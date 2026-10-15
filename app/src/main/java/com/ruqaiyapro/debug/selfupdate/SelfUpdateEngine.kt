package com.ruqaiyapro.debug.selfupdate

import com.ruqaiyapro.debug.brain.BrainManager
import com.ruqaiyapro.debug.feedback.FeedbackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Self-Update flow that really works on a phone:
 *   prompt -> AI writes Kotlin files -> commit to GitHub -> GitHub Actions builds -> app downloads the APK.
 * (Gradle cannot run on the phone itself, so GitHub does the build.)
 */
class SelfUpdateEngine(
    private val brain: BrainManager,
    private val feedback: FeedbackManager
) {

    data class GeneratedFile(val path: String, val content: String)
    data class GenResult(val files: List<GeneratedFile>, val rawText: String, val provider: String)

    companion object {
        const val ALLOWED_PREFIX = "app/src/main/"
    }

    suspend fun generate(userPrompt: String, onLog: (String) -> Unit): GenResult? {
        onLog("Prompt analyze korchi...")
        val fb = feedback.getPromptContextPrefix()
        val system = """
            You are an Android engineer for the project RuqaiyaPro (package com.ruqaiyapro.debug, Kotlin, Jetpack Compose + Material3, Room, CameraX, OkHttp, minSdk 26, targetSdk 30).
            Write complete, compilable Kotlin. Do not use RenderScript. Do not add FOREGROUND_SERVICE_MICROPHONE.
            Only create or modify files under app/src/main/ . Return ONLY file blocks in exactly this format:
            === FILE: app/src/main/java/com/ruqaiyapro/debug/features/Example.kt ===
            <full file content>
            === END FILE ===
            $fb
        """.trimIndent()

        onLog("AI (${brain.getSelectedProvider().displayName}) ke code likhte bolchi...")
        val r = brain.generate(userPrompt, system)
        if (!r.success) {
            onLog("AI fail: ${r.error ?: r.text}")
            return null
        }
        val files = parseFiles(r.text)
        onLog("AI ${r.latencyMs}ms-e ${files.size} ta file dilo.")
        return GenResult(files, r.text, r.provider.displayName)
    }

    fun parseFiles(text: String): List<GeneratedFile> {
        val regex = Regex("=== FILE: (.*?) ===\\r?\\n([\\s\\S]*?)=== END FILE ===")
        val out = mutableListOf<GeneratedFile>()
        for (m in regex.findAll(text)) {
            val path = m.groupValues[1].trim()
            if (!path.startsWith(ALLOWED_PREFIX) || path.contains("..")) continue // safety: only app/src/main
            var code = m.groupValues[2]
            code = code.lines().filterNot { it.trim().startsWith("```") }.joinToString("\n")
            out.add(GeneratedFile(path, LogcatRegexRepair.repairKotlinSource(code)))
        }
        return out
    }

    suspend fun commitAndWatch(
        repo: String, branch: String, token: String,
        files: List<GeneratedFile>, message: String, onLog: (String) -> Unit
    ): GithubClient.RunInfo? = withContext(Dispatchers.IO) {
        val gh = GithubClient(token)
        val before = try { gh.latestRun(repo, branch)?.id } catch (e: Exception) { null }

        for (f in files) {
            val (ok, info) = gh.commitFile(repo, branch, f.path, f.content, message)
            onLog(if (ok) "Commit OK: ${f.path}" else "Commit FAIL: ${f.path} -> $info")
            if (!ok) return@withContext null
        }
        onLog("GitHub Actions build shuru hobe, opekkha korchi...")
        delay(12_000)
        var last: GithubClient.RunInfo? = null
        repeat(50) {
            val run = try { gh.latestRun(repo, branch) } catch (e: Exception) { null }
            if (run != null && run.id != before) {
                if (last?.status != run.status || last?.id != run.id) onLog("Build #${run.number}: ${run.status}")
                last = run
                if (run.status == "completed" && run.conclusion != "cancelled") {
                    onLog("Build shesh: ${run.conclusion}")
                    return@withContext run
                }
            }
            delay(15_000)
        }
        onLog("Timeout: Actions page-e check koro.")
        last
    }

    suspend fun downloadLatestApk(repo: String, branch: String, token: String, dest: File, onLog: (String) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val gh = GithubClient(token)
                val run = gh.latestRun(repo, branch, onlySuccess = true)
                if (run == null) {
                    onLog("Kono successful build paini.")
                    return@withContext false
                }
                onLog("Build #${run.number} er APK download korchi...")
                val ok = gh.downloadApk(repo, run.id, dest)
                onLog(if (ok) "APK ready (${dest.length() / 1024} KB)" else "APK download hoy ni (artifact expired/token permission?)")
                ok
            } catch (e: Exception) {
                onLog("Download error: ${e.message}")
                false
            }
        }
}
