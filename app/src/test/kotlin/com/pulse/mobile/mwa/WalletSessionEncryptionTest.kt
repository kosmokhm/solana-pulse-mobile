package com.pulse.mobile.mwa

import org.junit.Assert.*
import org.junit.Test
import javax.crypto.KeyGenerator

class WalletSessionEncryptionTest {
    private class Blobs : SessionBlobStorage {
        var bytes: ByteArray? = null
        var failWrite = false
        override fun read() = bytes?.copyOf()
        override fun write(value: ByteArray) { check(!failWrite);bytes = value.copyOf() }
    }
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private fun cipher() = SessionCipher(SessionKeyProvider { key })
    private fun record() = WalletSession("synthetic-long-secret-token", "1".repeat(32), "https://mock-wallet.invalid")
    private fun rejects(action: () -> Unit) {
        try { action();fail("Expected storage rejection") } catch (_: Exception) { }
    }
    @Test fun encryptedSessionRoundTripAcrossFreshStoreInstance() {
        val blobs = Blobs();EncryptedWalletSessionStore(blobs,cipher()).save(record())
        val restored = EncryptedWalletSessionStore(blobs,cipher()).load()!!
        assertEquals(record().token,restored.token);assertEquals(record().address,restored.address)
        assertEquals(record().walletUri,restored.walletUri)
        assertFalse(String(blobs.bytes!!,Charsets.ISO_8859_1).contains(record().token!!))
        assertFalse(String(blobs.bytes!!,Charsets.ISO_8859_1).contains(record().address))
    }
    @Test fun tokenRotationUsesFreshNonceAndReplacesOldToken() {
        val blobs = Blobs();val store = EncryptedWalletSessionStore(blobs,cipher());store.save(record())
        val old = blobs.bytes!!.copyOf();store.save(WalletSession("new-secret-token",record().address,record().walletUri))
        assertFalse(old.copyOfRange(1,13).contentEquals(blobs.bytes!!.copyOfRange(1,13)))
        assertEquals("new-secret-token",store.load()!!.token)
    }
    @Test fun tamperedCiphertextIsRejected() {
        val blobs = Blobs();val store = EncryptedWalletSessionStore(blobs,cipher());store.save(record())
        blobs.bytes!![blobs.bytes!!.lastIndex] = (blobs.bytes!!.last().toInt() xor 1).toByte()
        rejects { store.load() }
    }
    @Test fun differentKeystoreKeyCannotDecryptOldSession() {
        val blobs = Blobs();EncryptedWalletSessionStore(blobs,cipher()).save(record())
        val other = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        rejects { EncryptedWalletSessionStore(blobs,SessionCipher(SessionKeyProvider { other })).load() }
    }
    @Test fun clearNeedsNoDecryptionAndPreventsNextColdStartRestore() {
        val blobs = Blobs();EncryptedWalletSessionStore(blobs,cipher()).save(record())
        val broken = EncryptedWalletSessionStore(blobs,SessionCipher(SessionKeyProvider { error("Keystore unavailable") }))
        broken.clear();assertNull(broken.load());assertArrayEquals(byteArrayOf(0),blobs.bytes)
    }
    @Test fun failedAtomicWriteKeepsPreviousEncryptedRecord() {
        val blobs = Blobs();val store = EncryptedWalletSessionStore(blobs,cipher());store.save(record());blobs.failWrite = true
        rejects { store.save(WalletSession("new",record().address,null)) }
        assertEquals(record().token,store.load()!!.token)
    }
    @Test fun absentEncryptionKeyDoesNotWritePlaintextFallback() {
        val blobs = Blobs();val store = EncryptedWalletSessionStore(blobs,SessionCipher(SessionKeyProvider { error("Unavailable") }))
        rejects { store.save(record()) };assertNull(blobs.bytes)
    }
    @Test fun expiredMetadataIsEncryptedAndCarriesNoUsableToken() {
        val blobs = Blobs();val store = EncryptedWalletSessionStore(blobs,cipher())
        store.save(WalletSession(null,record().address,record().walletUri))
        assertNull(store.load()!!.token);assertEquals(record().address,store.load()!!.address)
    }
}
