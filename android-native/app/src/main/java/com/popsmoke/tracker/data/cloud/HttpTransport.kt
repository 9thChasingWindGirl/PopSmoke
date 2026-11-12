package com.popsmoke.tracker.data.cloud

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * 一次 HTTP 调用的最小描述。刻意不用 OkHttp/Retrofit：
 * 这一批只有 7 个端点、请求体全是 JSON、没有上传下载，多一个依赖就多一份体积和更新负担。
 *
 * 抽成接口的唯一目的是测试：单测里换掉实现就能断言"发了什么请求"，
 * 不需要 MockWebServer，也不需要联网。
 */
data class HttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
)

data class HttpResponse(val code: Int, val body: String)

interface HttpTransport {
    /** 网络失败（DNS、超时、断网）抛 [IOException]；HTTP 4xx/5xx 正常返回，由上层判读。 */
    fun execute(request: HttpRequest): HttpResponse
}

class UrlConnectionTransport(
    private val connectTimeoutMs: Int = 12_000,
    private val readTimeoutMs: Int = 15_000,
) : HttpTransport {

    override fun execute(request: HttpRequest): HttpResponse {
        val connection = (URL(request.url).openConnection() as HttpURLConnection).apply {
            requestMethod = request.method
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            instanceFollowRedirects = true
            request.headers.forEach { (key, value) -> setRequestProperty(key, value) }
            if (request.body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        try {
            request.body?.let { payload ->
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return HttpResponse(code, text)
        } finally {
            connection.disconnect()
        }
    }
}
