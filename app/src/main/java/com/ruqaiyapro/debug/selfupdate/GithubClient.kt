package com.ruqaiyapro.debug.selfupdate

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/** Minimal GitHub REST client: commit files, watch Actions runs, download the built APK. */
class GithubClient(private val token: String) {

    data class RunInfo(val id: Long, val number: Int, val status: String, val conclusion: String?, val url: String)

    private data class Resp(val code: Int, val body: String)

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun builder(url: String): Request.Builder = Request.Builder().url(url)
        .header("Authorization", "Bearer $token")
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")

    private fun call(method: String, url: String, json: String? = null): Resp {
        val b = builder(url)
        if (method == "PUT") b.put((json ?: "{}").toRequestBody("application/json".toMediaType())) else b.get()
        http.newCall(b.build()).execute().use { r ->
            return Resp(r.code, r.body?.string().orEmpty())
        }
    }

    fun commitFile(repo: String, branch: String, path: String, content: String, message: String): Pair<Boolean, String> {
        val url = "https://api.github.com/repos/$repo/contents/$path"
        val existing = call("GET", "$url?ref=$branch")
        val sha = if (existing.code == 200) JSONObject(existing.body).optString("sha") else ""
        val body = JSONObject()
            .put("message", message)
            .put("content", Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
            .put("branch", branch)
        if (sha.isNotEmpty()) body.put("sha", sha)
        val r = call("PUT", url, body.toString())
        return Pair(r.code == 200 || r.code == 201, if (r.code in 200..201) "ok" else "HTTP ${r.code}: ${r.body.take(200)}")
    }

    private fun parseRun(o: JSONObject): RunInfo = RunInfo(
        id = o.getLong("id"),
        number = o.optInt("run_number"),
        status = o.optString("status"),
        conclusion = if (o.isNull("conclusion")) null else o.optString("conclusion"),
        url = o.optString("html_url")
    )

    fun latestRun(repo: String, branch: String, onlySuccess: Boolean = false): RunInfo? {
        val extra = if (onlySuccess) "&status=success" else ""
        val r = call("GET", "https://api.github.com/repos/$repo/actions/runs?branch=$branch&per_page=1$extra")
        if (r.code != 200) return null
        val arr = JSONObject(r.body).optJSONArray("workflow_runs") ?: return null
        if (arr.length() == 0) return null
        return parseRun(arr.getJSONObject(0))
    }

    /** Downloads the first .apk inside the newest artifact of the given run. */
    fun downloadApk(repo: String, runId: Long, dest: File): Boolean {
        val list = call("GET", "https://api.github.com/repos/$repo/actions/runs/$runId/artifacts")
        if (list.code != 200) return false
        val arts = JSONObject(list.body).optJSONArray("artifacts") ?: return false
        if (arts.length() == 0) return false
        val zipUrl = arts.getJSONObject(0).getString("archive_download_url")
        dest.parentFile?.mkdirs()
        http.newCall(builder(zipUrl).get().build()).execute().use { resp ->
            if (!resp.isSuccessful) return false
            val stream = resp.body?.byteStream() ?: return false
            ZipInputStream(stream).use { zin ->
                var entry = zin.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(".apk")) {
                        dest.outputStream().use { out -> zin.copyTo(out) }
                        return true
                    }
                    entry = zin.nextEntry
                }
            }
        }
        return false
    }
}
