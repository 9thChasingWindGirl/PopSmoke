package com.popsmoke.tracker.data.wallpaper

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.popsmoke.tracker.data.cloud.HttpRequest
import com.popsmoke.tracker.data.cloud.HttpTransport
import com.popsmoke.tracker.data.cloud.UrlConnectionTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Bing 每日壁纸。
 *
 * ## 为什么是壁纸而不是"再画一层程序化背景"
 *
 * 液态玻璃的可信度 100% 来自**位移前后的差异**。程序化背景是一层层柔和的渐变，
 * 把一块区域平移二三十个像素，前后几乎一样 —— 于是玻璃只能靠描边和白纱来"表示"，
 * 越表示越像磨砂贴膜。照片不一样：它有边、有纹理、有彼此咬合的明暗，
 * 边缘一压就看得见草被拉成丝、树梢被挤成一条带。
 *
 * ## 为什么不做成设置项
 *
 * 记录型 App 的墙纸是"每天的背景"，不是"需要管理的资源"。Bing 每天换一张，
 * 用户不用挑、不用删、不用想。真要自定义，那是另一个功能（选图 + 裁剪），
 * 现在没有这个需求，就不要先给它留一个入口。
 *
 * ## 网络策略
 *
 * 冷启动先读缓存（不联网、立刻可见），再去问今天的图。拿不到就用缓存，
 * 缓存也没有就返回空 —— 上层退回程序化极光。任何一步失败都不能让首屏空白。
 */
class DailyWallpaper(
    context: Context,
    private val transport: HttpTransport = UrlConnectionTransport(),
) {

    private val store = WallpaperStore(File(context.filesDir, "wallpaper"))

    /** 同一进程内复用解码结果：转屏、返回前台都不该再解一次 8MB 的位图。 */
    @Volatile
    private var memo: ImageBitmap? = null

    /**
     * 最多发射两次：缓存的旧图（有的话）→ 今天的图（下载成功的话）。
     *
     * 用 Flow 而不是"一次 suspend 返回"，是因为这两件事的代价差三个数量级：
     * 读缓存是几毫秒，联网是几秒。合成一次返回会让首屏为了等网络而空着，
     * 而这段时间本来完全可以先把昨天的壁纸显示出来。
     */
    fun wallpapers(): Flow<ImageBitmap> = flow {
        memo?.let { emit(it) }

        val cachedFile = store.cached()
        val cachedDay = cachedFile?.nameWithoutExtension
        if (memo == null && cachedFile != null) {
            decode(cachedFile)?.let {
                memo = it
                emit(it)
            }
        }

        val shot = BingWallpaper.fetch(transport) ?: return@flow
        if (shot.day == cachedDay) return@flow

        val bytes = BingWallpaper.download(shot.imageUrl) ?: return@flow
        val saved = store.save(bytes, shot.day) ?: return@flow
        decode(saved)?.let {
            memo = it
            emit(it)
        }
    }.flowOn(Dispatchers.IO)

    private fun decode(file: File): ImageBitmap? = runCatching {
        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
    }.getOrNull()
}

data class WallpaperShot(
    /** Bing 的 `enddate`，形如 20261112。当天唯一，用作缓存文件名。 */
    val day: String,
    val imageUrl: String,
    val title: String,
)

object BingWallpaper {

    private const val ENDPOINT =
        "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=zh-CN"

    /**
     * 取今天那张图的元信息。
     *
     * 返回的 `url` 是相对路径（`/th?id=OHR.XXX_ZH-CN..._1920x1080.jpg&rf=...`），
     * 要补上 host。不把它换成 UHD：1920 宽已经够一块 1080p 屏铺满，
     * 4K 版本是四倍流量换一张马上要被模糊掉的图，不值。
     */
    fun fetch(transport: HttpTransport): WallpaperShot? {
        val response = try {
            transport.execute(HttpRequest(method = "GET", url = ENDPOINT))
        } catch (e: Exception) {
            return null
        }
        if (response.code !in 200..299) return null

        return try {
            val image = JSONObject(response.body).optJSONArray("images")?.optJSONObject(0)
                ?: return null
            val raw = image.optString("url")
            if (raw.isBlank()) return null
            WallpaperShot(
                day = image.optString("enddate").takeIf { it.length == 8 } ?: return null,
                imageUrl = if (raw.startsWith("http")) raw else "https://www.bing.com$raw",
                title = image.optString("title"),
            )
        } catch (e: org.json.JSONException) {
            null
        }
    }

    /**
     * 二进制下载。不走 [HttpTransport] —— 那个接口返回的是 UTF-8 字符串，
     * 图片这么读会被解码器改成替换字符，拿到的字节已经不是原来那张图了。
     */
    fun download(url: String): ByteArray? {
        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 20_000
                instanceFollowRedirects = true
            }
            try {
                if (connection.responseCode !in 200..299) return null
                connection.inputStream.use { it.readBytes() }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 磁盘缓存：一天一个文件，文件名就是日期。
 *
 * 用文件名排序取最新，不需要额外维护一个"当前是哪张"的指针 —— 指针和文件不同步时
 * 出现的空指针，比"多存两张图"的代价大得多。只保留最近三张：Bing 偶尔会重发同一个
 * 日期的图，留两天的余量，超出的删掉，这目录不该无限长。
 */
class WallpaperStore(private val dir: File) {

    fun cached(): File? = dir
        .listFiles { file -> file.isFile && file.name.endsWith(".jpg") }
        ?.maxByOrNull { it.name }

    fun save(bytes: ByteArray, day: String): File? = runCatching {
        dir.mkdirs()
        // 先写临时文件再改名：写到一半被杀进程，留下的是一个 .tmp，
        // 不会出现一张"看起来存在、其实是半个文件"的壁纸。
        val temp = File(dir, "$day.tmp")
        temp.writeBytes(bytes)
        val target = File(dir, "$day.jpg")
        if (target.exists()) target.delete()
        if (!temp.renameTo(target)) return null

        dir.listFiles { file -> file.name.endsWith(".jpg") }
            ?.sortedByDescending { it.name }
            ?.drop(3)
            ?.forEach { it.delete() }
        target
    }.getOrNull()
}
