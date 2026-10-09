package com.pulse.mobile.mwa

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal interface SessionBlobStorage {
    fun read(): ByteArray?
    /** Must atomically replace the previous value and acknowledge durable success. */
    fun write(value: ByteArray)
}
internal fun interface SessionKeyProvider { fun key(create: Boolean): SecretKey }

/** AES-256/GCM; a fresh provider-generated nonce per write, with version/domain-bound AAD. */
internal class SessionCipher(private val keys: SessionKeyProvider) {
    private val aad = "com.pulse.mobile/mwa-session/v1".toByteArray(Charsets.UTF_8)
    fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keys.key(true))
        cipher.updateAAD(aad)
        check(cipher.iv.size == 12)
        return byteArrayOf(1) + cipher.iv + cipher.doFinal(plain)
    }
    fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size >= 30 && blob.size <= 65536 && blob[0] == 1.toByte())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keys.key(false), GCMParameterSpec(128, blob.copyOfRange(1, 13)))
        cipher.updateAAD(aad)
        return cipher.doFinal(blob.copyOfRange(13, blob.size))
    }
}

internal class EncryptedWalletSessionStore(
    private val blobs: SessionBlobStorage, private val cipher: SessionCipher
) : WalletSessionStore {
    @Synchronized override fun load(): WalletSession? {
        val blob = blobs.read() ?: return null
        if (blob.contentEquals(byteArrayOf(0))) return null // Explicit, durable Disconnect tombstone.
        val plain = cipher.decrypt(blob)
        try {
            return DataInputStream(ByteArrayInputStream(plain)).use {
                require(it.readInt() == 1)
                require(it.readUTF() == "solana:devnet") // Same chain as existing SDK default/identity.
                val token = it.readUTF().ifBlank { null }
                val address = it.readUTF()
                val uri = it.readUTF().ifBlank { null }
                require(address.length in 32..44 && it.available() == 0)
                WalletSession(token, address, uri)
            }
        } finally { plain.fill(0) }
    }
    @Synchronized override fun save(session: WalletSession) {
        require(session.address.length in 32..44)
        require((session.token?.length ?: 0) <= 16384 && (session.walletUri?.length ?: 0) <= 4096)
        val plain = ByteArrayOutputStream().also { bytes ->
            DataOutputStream(bytes).use {
                it.writeInt(1); it.writeUTF("solana:devnet")
                it.writeUTF(session.token.orEmpty()); it.writeUTF(session.address); it.writeUTF(session.walletUri.orEmpty())
            }
        }.toByteArray()
        try { blobs.write(cipher.encrypt(plain)) } finally { plain.fill(0) }
    }
    @Synchronized override fun clear() {
        // No decryption/Keystore needed to forget an unreadable session. Never touches demo data.
        blobs.write(byteArrayOf(0))
    }
}
