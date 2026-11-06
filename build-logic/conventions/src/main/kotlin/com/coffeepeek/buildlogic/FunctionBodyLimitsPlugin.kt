package com.coffeepeek.buildlogic

import com.coffeepeek.buildlogic.GenerateFunctionBodyLimitsBaselineTask
import com.coffeepeek.buildlogic.VerifyFunctionBodyLimitsTask
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Root-project enforcement of the AGENTS.md function body limits (40 for @Composable, 20
 * otherwise). Existing debt is frozen in `function-body-limits-baseline.conf`; new or grown
 * violations fail `verifyFunctionBodyLimits`. `generateFunctionBodyLimitsBaseline` rewrites the
 * debt file after a refactor shrank recorded bodies.
 */
class FunctionBodyLimitsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val root = project.rootDir
        val moduleSources = project.files()
        moduleSources.setFrom(Modules.all.map { path -> root.resolve(path.removePrefix(":").replace(':', '/')).resolve("src") })

        project.tasks.register("verifyFunctionBodyLimits", VerifyFunctionBodyLimitsTask::class.java).configure {
            sourceRoots.setFrom(moduleSources)
            rootDir.set(root)
            baselineFiles.from(root.resolve(BASELINE))
        }
        project.tasks.register("generateFunctionBodyLimitsBaseline", GenerateFunctionBodyLimitsBaselineTask::class.java).configure {
            sourceRoots.setFrom(moduleSources)
            rootDir.set(root)
            baselineFile.set(root.resolve(BASELINE))
        }
    }

    private companion object {
        const val BASELINE = "function-body-limits-baseline.conf"
    }
}
