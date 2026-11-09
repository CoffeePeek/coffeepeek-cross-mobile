package com.coffeepeek.buildlogic

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VerifyFunctionBodyLimitsTaskTest {
    @Test fun bracedBodyCountsNonBlankNonCommentLinesAndIgnoresSignature() {
        val source = """
            @Composable
            fun Big() {           // signature line is never counted
                val a = 1

                // comment does not count
                run {             // nested braces stay in the same body
                    val b = 2
                }
            }
        """.trimIndent()
        assertEquals(3, functionBodyLineCounts(source).single().lines)
    }

    @Test fun composableIsDetectedFromAnnotationsWithinFourLines() {
        val composable = "@Composable\nprivate fun WithAnnotation() {\n    x\n}\n"
        val plain = "@ReadOnly\n@Composable\nfun Stacked() {\n    x\n}\n"
        val ordinary = "@PublishedApi\ninternal fun Annotated() {\n    x\n}\n"
        assertEquals(listOf(true, true, false), listOf(composable, plain, ordinary).map { src ->
            functionBodyLineCounts(src).single().composable
        })
    }

    @Test fun bracesInsideStringsAndCommentsNeverCount() {
        val triple = "\"\"\""
        val source = """
            fun Tricky() {
                val json = "{ not a brace }"
                val text = $triple
                    multi { line }
                $triple
                // lone { brace in comment
                /* block { brace */
                val ch = '}'
                val ok = 1
            }
        """.trimIndent()
        assertEquals(5, functionBodyLineCounts(source).single().lines)
    }

    @Test fun expressionBodiesCountTheirBracedPart() {
        val tryBody = "fun Load(): Int = try {\n    step()\n} catch (e: Exception) {\n    0\n}\n"
        val whenBody = "fun Pick(x: Int) = when (x) {\n    1 -> a\n    else -> b\n}\n"
        val simple = "fun Sum() = 1 + 2\n\nfun Next() {\n    val a = 1\n}\n"
        assertEquals(3, functionBodyLineCounts(tryBody).single().lines)
        assertEquals(2, functionBodyLineCounts(whenBody).single().lines)
        // The brace-less expression must not swallow the next function's body.
        assertEquals(listOf("Next"), functionBodyLineCounts(simple).map { it.name })
    }

    @Test fun signatureSpanningLinesStillFindsTheBody() {
        val source = """
            fun Long(
                first: Int,
                second: String,
            ): Result<Unit> {
                val x = first
                return Result.success(x)
            }
        """.trimIndent() + "\n"
        assertEquals(2, functionBodyLineCounts(source).single().lines)
    }

    @Test fun nestedFunctionsAreCoveredByTheEnclosingBodyOnly() {
        val source = """
            fun Outer() {
                fun inner() {
                    val a = 1
                }
                val b = 2
            }
        """.trimIndent() + "\n"
        assertEquals(listOf("Outer"), functionBodyLineCounts(source).map { it.name })
    }

    @Test fun onlyOverLimitFunctionsAreReported() {
        val composable = "@Composable\nfun Small() {\n" + List(20) { "    line$it()" }.joinToString("\n") + "\n}\n"
        val ordinary = "fun Big() {\n" + List(21) { "    line$it()" }.joinToString("\n") + "\n}\n"
        val violations = scanFile("Test.kt", composable + ordinary)
        assertEquals(listOf("Big"), violations.map { it.name })
        assertEquals(21, violations.single().lines)
        assertEquals(ORDINARY_BODY_LIMIT, violations.single().limit)
    }

    @Test fun baselineMatchingIgnoresShrunkenBodiesAndRejectsGrownOnes() {
        val file = File.createTempFile("baseline", ".txt").apply {
            writeText("# comment\nfeature/shop/impl/ui/Shop.kt|ShopCard|50;\ncore/Other.kt|X|25\nbroken-line\n")
        }
        val baseline = readBaseline(file)
        assertEquals(listOf("ShopCard", "X"), baseline.map { it.name })
        assertEquals(listOf(50, 25), baseline.map { it.lines })
        file.delete()
    }

    @Test fun codeOfKeepsCrossLineStatesSeparate() {
        // Block comment swallows braces; the following code still counts.
        assertEquals("{", codeOf("  /* { } */ {").text.trim())
        assertNullStringsInTriple()
    }

    @Test fun tripleStringStateSurvivesAcrossLines() {
        val first = codeOf("val s = \"\"\"{", inTriple = false)
        assertTrue(first.inTriple)
        val second = codeOf("still { inside }", inTriple = first.inTriple)
        assertTrue(second.text.isBlank())
        val third = codeOf("\"\"\".trim()", inTriple = second.inTriple)
        assertTrue(!third.inTriple)
    }

    private fun assertNullStringsInTriple() {
        val result = codeOf("val quote = \"}\"", inBlockComment = false, inTriple = false)
        assertEquals(0, result.text.count { it == '{' } - result.text.count { it == '}' })
    }
}
