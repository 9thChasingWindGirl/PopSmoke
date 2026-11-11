package com.popsmoke.tracker.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 用 Android Keystore 里的 AES-GCM 密钥加密本地敏感数据。
 *
 * Web 版的做法是「用户设一个密码，用密码派生密钥加密 API 配置」，于是每次同步都要输一次密码。
 * 原生有 Keystore：密钥由系统生成、永不离开 TEE，App 只能"用"不能"读"。
 * 所以这里不需要密码，安全性不低于 Web 版，还省掉了"忘记密码 = 配置作废"这个坑。
 *
 * 密文格式：`base64(iv) + ":" + base64(ciphertext)`。IV 每次随机，绝不复用。
 */
object SecureStore {

    private const val KEY_ALIAS = "popsmoke-local-secret"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val body = Base64.encodeToString(cipherText, Base64.NO_WRAP)
        return "$iv:$body"
    }

    /** 解密失败返回 null —— 换设备、清数据、密钥失效都会走到这里，调用方按"未配置"处理。 */
    fun decrypt(blob: String): String? = runCatching {
        val parts = blob.split(":")
        if (parts.size != 2) return null
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val body = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
        String(cipher.doFinal(body), Charsets.UTF_8)
    }.getOrNull()
}

/** API 配置。字段与 Web 版 apiSettings 对齐，只保留原生需要的几项。 */
data class ApiConfig(
    val feishuUrl: String = "",
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    /** 飞书中继的可选写权限密钥，对应 Web 端的 X-Access-Key。 */
    val feishuKey: String = "",
) {
    val hasFeishu: Boolean get() = feishuUrl.isNotBlank()
    val hasSupabase: Boolean get() = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()
    val isEmpty: Boolean get() = !hasFeishu && !hasSupabase
}

/** API 配置的落盘：整段 JSON 加密后写文件。 */
class ApiConfigStore(private val baseDir: java.io.File) {

    private val file = java.io.File(baseDir, "api.enc")

    fun load(): ApiConfig {
        if (!file.exists()) return ApiConfig()
        val plain = SecureStore.decrypt(file.readText(Charsets.UTF_8)) ?: return ApiConfig()
        return runCatching {
            val json = org.json.JSONObject(plain)
            ApiConfig(
                feishuUrl = json.optString("feishuUrl", ""),
                supabaseUrl = json.optString("supabaseUrl", ""),
                supabaseAnonKey = json.optString("supabaseAnonKey", ""),
                feishuKey = json.optString("feishuKey", ""),
            )
        }.getOrElse { ApiConfig() }
    }

    fun save(config: ApiConfig) {
        if (config.isEmpty) {
            if (file.exists()) file.delete()
            return
        }
        val plain = org.json.JSONObject()
            .put("feishuUrl", config.feishuUrl)
            .put("supabaseUrl", config.supabaseUrl)
            .put("supabaseAnonKey", config.supabaseAnonKey)
            .put("feishuKey", config.feishuKey)
            .toString()
        baseDir.mkdirs()
        file.writeText(SecureStore.encrypt(plain), Charsets.UTF_8)
    }
}

/** 掩码显示：保留协议与域名首段，其余打码。API Key 一律全掩。 */
fun maskUrl(url: String): String {
    if (url.isBlank()) return "—"
    val host = url.substringAfter("://", url).substringBefore("/")
    val masked = if (host.length <= 6) "••••" else host.take(6) + "••••••"
    return masked
}

fun maskSecret(secret: String): String = if (secret.isBlank()) "—" else "••••••••••••"
