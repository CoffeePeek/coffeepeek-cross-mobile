package com.coffeepeek.buildlogic

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** AGENTS.md body limit for a @Composable function body. */
const val COMPOSABLE_BODY_LIMIT = 40

/** AGENTS.md body limit for an ordinary (non-composable) function body. */
const val ORDINARY_BODY_LIMIT = 20

/**
 * Fails when a production function body exceeds the AGENTS.md limit unless the exact violation is
 * recorded in the baseline. The baseline freezes pre-existing debt; new or grown bodies fail.
 * Counts follow AGENTS.md: non-blank, non-comment body lines including nested blocks, excluding
 * the signature. Bodies without braces (single expressions) are not counted.
 */
@DisableCachingByDefault(because = "Reads live sources; caching would hide new violations")
abstract class VerifyFunctionBodyLimitsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceRoots: ConfigurableFileCollection

    @get:Internal
    abstract val rootDir: RegularFileProperty

    @get:InputFiles
    abstract val baselineFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val baselineFile = baselineFiles.files.singleOrNull()
            ?: throw GradleException("Exactly one baseline file is required for function body limits")
        val baseline = readBaseline(baselineFile)
        val violations = productionSources().flatMap { (relative, file) -> scanFile(relative, file.readText()) }
        val fresh = violations.filterNot { current ->
            baseline.any { record -> record.path == current.path && record.name == current.name && record.lines >= current.lines }
        }
        logger.lifecycle("Function body limits: ${violations.size} recorded, ${fresh.size} new.")
        if (fresh.isNotEmpty()) {
            val details = fresh.take(20).joinToString("\n") {
                "  ${it.path}: ${it.name}() — ${it.lines} body lines (limit ${it.limit}${if (it.composable) ", @Composable" else ""})"
            }
            val more = if (fresh.size > 20) "\n  … and ${fresh.size - 20} more" else ""
            throw GradleException(
                "Function body limit violations: ${fresh.size}. Bodies count non-blank, non-comment lines " +
                    "per AGENTS.md ($COMPOSABLE_BODY_LIMIT for @Composable, $ORDINARY_BODY_LIMIT otherwise):\n$details$more\n" +
                    "Extract cohesive helpers, or record deliberate debt in the baseline file " +
                    "${baselineFile.name} (path|name|lines)."
            )
        }
    }

    internal fun productionSources(): List<Pair<String, File>> =
        productionSources(sourceRoots, rootDir.get().asFile)
}

/** Unit, instrumented and UI test sources carry no production limit. */
internal val TEST_SOURCE_SEGMENTS =
    setOf("commonTest", "androidUnitTest", "androidInstrumentedTest", "androidTest", "test", "jsTest")

/** Shared walk so verify and generate always see identical sources. */
internal fun productionSources(roots: ConfigurableFileCollection, rootDir: File): List<Pair<String, File>> =
    roots.files.flatMap { root ->
        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && root.toPath().relativize(it.toPath()).none { part -> part.toString() in TEST_SOURCE_SEGMENTS } }
            .map { file -> rootDir.toPath().relativize(file.toPath()).joinToString("/") to file }
            .toList()
    }

/** Rewrites the baseline from the current sources: `relative/path/File.kt|functionName|lines`. */
@DisableCachingByDefault(because = "Reads live sources; the output must reflect the working tree")
abstract class GenerateFunctionBodyLimitsBaselineTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceRoots: ConfigurableFileCollection

    @get:Internal
    abstract val rootDir: RegularFileProperty

    @get:OutputFile
    abstract val baselineFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val output = productionSources(sourceRoots, rootDir.get().asFile)
            .flatMap { (relative, file) -> scanFile(relative, file.readText()) }
            .sortedBy { it.path }
        baselineFile.get().asFile.writeText(
            buildString {
                appendLine("# AGENTS.md function body limit debt; regenerate with :generateFunctionBodyLimitsBaseline")
                appendLine("# path|function|lines — shrink or remove entries by refactoring; do not extend by hand")
                output.forEach { appendLine("${it.path}|${it.name}|${it.lines}") }
            }
        )
        logger.lifecycle("Recorded ${output.size} function body limit violations in the baseline")
    }
}

/** One over-limit function: relative POSIX path, function name, counted body lines. */
data class FunctionBodyViolation(val path: String, val name: String, val lines: Int, val limit: Int, val composable: Boolean)

/** Reads `path|name|lines` baseline entries; malformed lines are skipped. */
fun readBaseline(file: File): List<FunctionBodyViolation> =
    file.readLines().mapNotNull { line ->
        val trimmed = line.trim().trimEnd(';')
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return@mapNotNull null
        val parts = trimmed.split('|')
        val count = parts.getOrNull(2)?.toIntOrNull() ?: return@mapNotNull null
        FunctionBodyViolation(path = parts[0], name = parts[1], lines = count, limit = 0, composable = false)
    }

/** Scans one Kotlin source; reports only functions whose body exceeds the limit. */
fun scanFile(path: String, source: String): List<FunctionBodyViolation> =
    functionBodyLineCounts(source).mapNotNull { (name, lines, composable) ->
        val limit = if (composable) COMPOSABLE_BODY_LIMIT else ORDINARY_BODY_LIMIT
        if (lines > limit) FunctionBodyViolation(path, name, lines, limit, composable) else null
    }

/** One counted function: name, body line count, composable flag. */
data class FunctionBodyLineCount(val name: String, val lines: Int, val composable: Boolean)

/**
 * Counts every braced function body: braced bodies directly and the braced part of expression
 * bodies (`= try {`, `= when {`, `= object : T {`). Braces inside strings and comments never count.
 * Nested functions are covered by their enclosing body and are not reported separately.
 */
fun functionBodyLineCounts(source: String): List<FunctionBodyLineCount> {
    val lines = source.lines()
    val result = mutableListOf<FunctionBodyLineCount>()
    var index = 0
    while (index < lines.size) {
        val bodyStart = findBodyStart(lines, index)
        if (bodyStart == null) { index++; continue }
        val name = FUN_NAME.find(codeOf(lines[index]).text)?.groupValues?.get(1)
        if (name == null) { index = bodyStart + 1; continue }
        val body = bracedBody(lines, bodyStart)
        if (body == null) { index = bodyStart + 1; continue }
        val (counted, bodyEnd) = body
        result += FunctionBodyLineCount(name, counted, isComposable(lines, index))
        index = bodyEnd + 1
    }
    return result
}

internal data class Code(val text: String, val inBlockComment: Boolean, val inTriple: Boolean)

/** Strips comment text and string-literal contents from one line, tracking cross-line states. */
internal fun codeOf(line: String, inBlockComment: Boolean = false, inTriple: Boolean = false): Code {
    var insideBlock = inBlockComment
    var insideTriple = inTriple
    val out = StringBuilder()
    var index = 0
    while (index < line.length) {
        if (insideBlock) {
            val end = line.indexOf("*/", index)
            if (end < 0) return Code(out.toString(), true, insideTriple)
            out.append(" ".repeat(end + 2 - index))
            index = end + 2
            insideBlock = false
            continue
        }
        if (insideTriple) {
            if (line.startsWith("\"\"\"", index)) {
                out.append("\"\"\"")
                index += 3
                insideTriple = false
                continue
            }
            index++
            continue
        }
        when {
            line.startsWith("//", index) -> return Code(out.toString(), false, insideTriple)
            line.startsWith("/*", index) -> { insideBlock = true; index += 2 }
            line.startsWith("\"\"\"", index) -> { insideTriple = true; out.append(' '); index += 3 }
            line[index] == '"' -> {
                var cursor = index + 1
                while (cursor < line.length && line[cursor] != '"') {
                    if (line[cursor] == '\\') cursor++
                    cursor++
                }
                out.append("\"\"")
                index = cursor + 1
            }
            line[index] == '\'' -> {
                var cursor = index + 1
                while (cursor < line.length && line[cursor] != '\'') {
                    if (line[cursor] == '\\') cursor++
                    cursor++
                }
                out.append("''")
                index = cursor + 1
            }
            else -> { out.append(line[index]); index++ }
        }
    }
    return Code(out.toString(), insideBlock, insideTriple)
}

private val FUN_NAME = Regex("""\bfun\s+(?:<[^<>]*>\s+)?(?:[\w?.<>]*\.)?(\w+)\s*\(""")

private fun isComposable(lines: List<String>, start: Int): Boolean {
    val sameLine = lines[start].substringBefore("fun").trim()
    if (sameLine.startsWith("@") && sameLine.contains("@Composable")) return true
    // Annotations attach contiguously; a blank line breaks the chain to an earlier function.
    for (back in 1..4) {
        val previous = lines.getOrNull(start - back)?.trim() ?: break
        if (previous.startsWith("//")) continue
        if (previous.contains("@Composable")) return true
        if (previous.isBlank() || !previous.startsWith("@")) break
    }
    return false
}

/**
 * Finds the line where a function declared on [start] opens its braced body: the first `{` after
 * the parameter list closes, directly or after a top-level `=` (expression body). Returns null
 * when the next 60 lines never open one, or when an unrelated declaration intervenes.
 */
private fun findBodyStart(lines: List<String>, start: Int): Int? {
    if (FUN_NAME.find(codeOf(lines[start]).text) == null) return null
    var parenDepth = 0
    var sawEquals = false
    var block = false
    var triple = false
    for (index in start until minOf(start + 60, lines.size)) {
        val (text, nextBlock, nextTriple) = codeOf(lines[index], block, triple)
        block = nextBlock
        triple = nextTriple
        if (index > start && parenDepth == 0 && !block && !triple) {
            val trimmed = text.trim()
            // An intervening declaration before any body brace means this is not a function body here.
            if (trimmed.startsWith("fun ") && (!sawEquals || FUN_NAME.containsMatchIn(text))) return null
            if (!sawEquals && (trimmed.startsWith("class ") || trimmed.startsWith("object "))) return null
            // An expression body that ends without ever opening a brace has no counted body.
            if (sawEquals && trimmed.isNotEmpty() && trimmed.last() !in EXPRESSION_CONTINUATION && !trimmed.startsWith(".")) {
                val next = lines.getOrNull(index + 1)?.trim() ?: ""
                if (!next.startsWith(".") && !next.startsWith("?.") && !next.startsWith("\"\"\"")) return null
            }
        }
        for (ch in text) {
            when {
                ch == '(' -> parenDepth++
                ch == ')' -> parenDepth--
                ch == '=' && parenDepth == 0 && !sawEquals -> sawEquals = true
                ch == '{' && parenDepth == 0 -> return index
            }
        }
    }
    return null
}

private val EXPRESSION_CONTINUATION = setOf('=', '(', ',', '{', '+', '*', ':', '?', '-', '>', '|', '&')

/** Counts non-blank non-comment body lines and returns them with the closing brace's line. */
private fun bracedBody(lines: List<String>, bodyLine: Int): Pair<Int, Int>? {
    var depth = 0
    var block = false
    var triple = false
    var count = 0
    for (index in bodyLine until lines.size) {
        val (text, nextBlock, nextTriple) = codeOf(lines[index], block, triple)
        block = nextBlock
        triple = nextTriple
        for (ch in text) {
            if (ch == '{') depth++ else if (ch == '}') depth--
        }
        // Bare brace-only lines are structure, not body content.
        val bare = text.trim()
        if (index > bodyLine && bare.isNotEmpty() && bare.any { it != '{' && it != '}' }) count++
        if (depth <= 0) return count to index
    }
    return null
}
