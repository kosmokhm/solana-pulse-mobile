package com.pulse.mobile.mwa

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

internal fun androidWalletSessionStore(context: Context): WalletSessionStore {
    // noBackupFilesDir is excluded from Android cloud backup AND device transfer.
    val file = AtomicFile(File(context.applicationContext.noBackupFilesDir, "mwa_session_v1.enc"))
    val blobs = object : SessionBlobStorage {
        override fun read(): ByteArray? {
            if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return null
            return file.openRead().use { input ->
                val bytes = input.readBytes()
                require(bytes.size <= 65536)
                bytes
            }
        }
        override fun write(value: ByteArray) {
            val output = file.startWrite()
            try { output.write(value); output.fd.sync(); file.finishWrite(output) }
            catch (e: Exception) { file.failWrite(output); throw e }
            // AtomicFile.finishWrite logs fsync errors on some Android versions; verify disk value.
            check(file.openRead().use { it.readBytes() }.contentEquals(value))
        }
    }
    val keys = SessionKeyProvider { create ->
        val alias = "pulse_mwa_session_aes_v1"
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        existing ?: run {
            check(create) { "Session encryption key is unavailable" }
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true).build())
            }.generateKey()
        }
    }
    return EncryptedWalletSessionStore(blobs, SessionCipher(keys))
}
