package com.popsmoke.tracker.data.cloud

import com.popsmoke.tracker.data.SecureStore
import org.json.JSONObject
import java.io.File

data class CloudUser(val id: String, val email: String)

/**
 * 一次登录会话。`expiresAt` 用绝对毫秒时间而不是 expires_in，
 * 因为后者只有落盘那一刻有意义 —— 存成相对值，冷启动时就变成了"还剩几秒"。
 */
data class CloudSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val user: CloudUser,
) {
    fun isFresh(now: Long, marginMs: Long = 60_000): Boolean = now < expiresAt - marginMs
}

/**
 * 会话落盘。抽接口是为了单测：JVM 里没有 Android Keystore，
 * 用 [InMemorySessionStore] 就能把整套登录流程跑通。
 */
interface SessionStore {
    fun load(): CloudSession?
    fun save(session: CloudSession)
    fun clear()
}

class InMemorySessionStore(private var session: CloudSession? = null) : SessionStore {
    override fun load(): CloudSession? = session
    override fun save(session: CloudSession) {
        this.session = session
    }

    override fun clear() {
        session = null
    }
}

/**
 * 真机用的实现：走 [SecureStore] 的 Keystore AES-GCM。
 *
 * access_token 是短期票据，refresh_token 才是长期凭据（Supabase 默认不过期直到被吊销）。
 * 它本质上等同于密码，所以不能像 API 配置那样明文放 SharedPreferences。
 * 解密失败（换机、重装、系统密钥失效）一律按"未登录"处理，不弹异常。
 */
class EncryptedSessionStore(private val baseDir: File) : SessionStore {

    private val file = File(baseDir, "session.enc")

    override fun load(): CloudSession? {
        if (!file.exists()) return null
        val plain = SecureStore.decrypt(file.readText(Charsets.UTF_8)) ?: return null
        return runCatching {
            val json = JSONObject(plain)
            CloudSession(
                accessToken = json.optString("accessToken"),
                refreshToken = json.optString("refreshToken"),
                expiresAt = json.optLong("expiresAt"),
                user = CloudUser(
                    id = json.optString("userId"),
                    email = json.optString("email"),
                ),
            )
        }.getOrNull()?.takeIf { it.accessToken.isNotBlank() && it.user.id.isNotBlank() }
    }

    override fun save(session: CloudSession) {
        val plain = JSONObject()
            .put("accessToken", session.accessToken)
            .put("refreshToken", session.refreshToken)
            .put("expiresAt", session.expiresAt)
            .put("userId", session.user.id)
            .put("email", session.user.email)
            .toString()
        baseDir.mkdirs()
        file.writeText(SecureStore.encrypt(plain), Charsets.UTF_8)
    }

    override fun clear() {
        if (file.exists()) file.delete()
    }
}
