package com.tvhanan.data.network

import android.util.Log
import com.tvhanan.data.local.TvPreferences
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLSession

class SslTrustManager(private val prefs: TvPreferences) {

    companion object {
        private const val TAG = "SslTrustManager"
    }

    private val cache = ConcurrentHashMap<String, String>()

    fun loadFingerprint(ip: String) {
        if (cache.containsKey(ip)) return
        val fp = prefs.getCertificateFingerprintSync(ip)
        if (fp != null) cache[ip] = fp
    }

    fun verifyOrTrust(hostname: String, session: SSLSession?): Boolean {
        if (session == null) return false
        return try {
            val chain = session.peerCertificateChain
            if (chain.isEmpty()) return false
            val leaf = chain[0] as X509Certificate
            val fingerprint = sha256Fingerprint(leaf)
            val stored = cache[hostname]

            if (stored == null) {
                Log.i(TAG, "TOFU: Trusting first certificate for $hostname")
                cache[hostname] = fingerprint
                prefs.saveCertificateFingerprintSync(hostname, fingerprint)
                true
            } else if (stored == fingerprint) {
                true
            } else {
                Log.w(TAG, "Certificate fingerprint mismatch for $hostname! " +
                    "Expected $stored, got $fingerprint")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Certificate verification failed for $hostname", e)
            false
        }
    }

    fun forget(ip: String) {
        cache.remove(ip)
        prefs.removeCertificateFingerprintSync(ip)
    }

    fun isTrusted(ip: String): Boolean = cache.containsKey(ip)

    private fun sha256Fingerprint(cert: X509Certificate): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(cert.encoded)
        return hash.joinToString("") { "%02X".format(it) }
    }
}
