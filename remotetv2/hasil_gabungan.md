### File: ./app/src/main/java/com/tvhanan/data/network/AppLauncher.kt
```
package com.tvhanan.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Meluncurkan app Smart Hub via REST API /api/v2/applications/{appId},
 * BUKAN lewat WebSocket ms.channel.emit. Ditemukan lewat percobaan
 * langsung ke TV: endpoint WebSocket (ed.apps.launch) tidak direspons
 * sama sekali oleh firmware TV ini (N-series 2020 / Tizen 5.0), tapi
 * REST POST ke endpoint applications berhasil membuka app langsung.
 *
 * Endpoint ini tidak butuh token/pairing — sama seperti GET /api/v2/
 * untuk info TV, ini bagian dari REST API publik TV yang tidak melalui
 * jalur otorisasi WebSocket remote.control.
 */
object AppLauncher {
    private const val TAG = "AppLauncher"

    suspend fun launch(ip: String, appId: String): Boolean =
        request(ip, appId, "POST", "launch")

    suspend fun close(ip: String, appId: String): Boolean =
        request(ip, appId, "DELETE", "close")

    private suspend fun request(ip: String, appId: String, method: String, label: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("http://$ip:8001/api/v2/applications/$appId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = method
                connection.connectTimeout = 4000
                connection.readTimeout = 4000

                val responseCode = connection.responseCode
                connection.disconnect()

                Log.d(TAG, "$label($appId) responseCode=$responseCode")
                responseCode in 200..299
            } catch (e: Exception) {
                Log.e(TAG, "$label($appId) failed: ${e.message}")
                false
            }
        }
}```

### File: ./app/src/main/java/com/tvhanan/data/network/TvDiscoveryService.kt
```
package com.tvhanan.data.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.tvhanan.domain.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketTimeoutException

class TvDiscoveryService(private val context: Context) {

    companion object {
        private const val SSDP_ADDR = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val SSDP_TIMEOUT = 4000L
        private const val SCAN_TIMEOUT = 300
        private const val TAG = "TvDiscoveryService"
    }

    suspend fun discoverDevices(): List<TvDevice> = withContext(Dispatchers.IO) {
        val ssdpResults = discoverSSDP()
        if (ssdpResults.isNotEmpty()) return@withContext ssdpResults

        val subnet = getLocalIpPrefix() ?: return@withContext emptyList()
        scanSubnet(subnet)
    }

/**
     * Ambil info dasar TV (nama model, MAC wifi asli) lewat endpoint
     * HTTP /api/v2/ yang tidak butuh pairing/token sama sekali — berguna
     * untuk menampilkan nama TV yang sebenarnya di hasil scan, bukan
     * generik "Samsung TV". Dipanggil setelah port terbuka terdeteksi.
     */
    private suspend fun fetchDeviceInfo(ip: String): Pair<String, String?>? = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("http://$ip:8001/api/v2/")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                connection.disconnect()
                return@withContext null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = org.json.JSONObject(body)
            val device = json.optJSONObject("device") ?: return@withContext null
            val name = device.optString("name", "Samsung TV").removePrefix("[TV] ")
            val mac = device.optString("wifiMac", "").ifBlank { null }

            name to mac
        } catch (e: Exception) {
            Log.e(TAG, "fetchDeviceInfo failed for $ip: ${e.message}")
            null
        }
    }

    /**
     * Cek apakah sebuah host:port bisa dijangkau (TCP connect singkat).
     * Dipakai untuk "Hubungkan ulang TV" di Settings — verifikasi cepat
     * sebelum RemoteScreen mencoba membuka WebSocket sesungguhnya.
     */
    suspend fun isHostReachable(ip: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        isPortOpen(ip, port)
    }

    private suspend fun discoverSSDP(): List<TvDevice> {
        return withContext(Dispatchers.IO) {
            val multicastLock = acquireMulticastLock()

            try {
                val results = mutableListOf<TvDevice>()
                DatagramSocket().use { socket ->
                    socket.soTimeout = SSDP_TIMEOUT.toInt()

                    val ssdpRequest = buildString {
                        append("M-SEARCH * HTTP/1.1\r\n")
                        append("HOST: $SSDP_ADDR:$SSDP_PORT\r\n")
                        append("MAN: \"ssdp:discover\"\r\n")
                        append("ST: urn:samsung.com:device:RemoteControlReceiver:1\r\n")
                        append("MX: 3\r\n")
                        append("\r\n")
                    }

                    val sendPacket = DatagramPacket(
                        ssdpRequest.toByteArray(),
                        ssdpRequest.length,
                        InetAddress.getByName(SSDP_ADDR),
                        SSDP_PORT
                    )
                    socket.send(sendPacket)

                    val startTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startTime < SSDP_TIMEOUT) {
                        try {
                            val buffer = ByteArray(1024)
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet)

                            val response = String(packet.data, 0, packet.length)
                            val ip = parseLocationIp(response)
                            if (ip != null && !results.any { it.ipAddress == ip }) {
                                val info = fetchDeviceInfo(ip)
                                results.add(
                                    TvDevice(ipAddress = ip, name = info?.first ?: "Samsung TV", macAddress = info?.second)
                                )
                            }
                        } catch (_: SocketTimeoutException) {
                            break
                        } catch (_: Exception) {
                            continue
                        }
                    }
                }
                results
            } finally {
                releaseMulticastLock(multicastLock)
            }
        }
    }

    private suspend fun scanSubnet(prefix: String): List<TvDevice> = coroutineScope {
        val semaphore = Semaphore(50)
        (1..254).map { octet ->
            async {
                semaphore.withPermit {
                    val ip = "$prefix.$octet"
                    val openPort = when {
                        isPortOpen(ip, 8002) -> 8002
                        isPortOpen(ip, 8001) -> 8001
                        else -> null
                    }
                    if (openPort != null) {
                        val info = fetchDeviceInfo(ip)
                        TvDevice(
                            ipAddress = ip,
                            name = info?.first ?: "Samsung TV",
                            macAddress = info?.second,
                            port = openPort
                        )
                    } else {
                        null
                    }
                }
            }
        }.awaitAll().filterNotNull()
    }

    private fun isPortOpen(ip: String, port: Int): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), SCAN_TIMEOUT)
            socket.close()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getLocalIpPrefix(): String? {
    return try {
        // Berikan penanganan null-safety jika sistem mengembalikan nilai null saat tidak ada interface aktif
        val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
        
        // Filter interface aktif, lalu prioritaskan Wi-Fi (wlan0, wlan1, dst) di urutan pertama
        val activeInterfaces = interfaces
            .filter { !it.isLoopback && it.isUp }
            .sortedByDescending { it.name.startsWith("wlan") }

        for (networkInterface in activeInterfaces) {
            val addresses = networkInterface.inetAddresses.toList()
            for (addr in addresses) {
                if (addr is Inet4Address && !addr.isLoopbackAddress) {
                    val ip = addr.hostAddress ?: continue
                    return ip.substringBeforeLast(".") // Berhasil mengunci prefix jaringan Wi-Fi
                }
            }
        }
        null
    } catch (_: Exception) {
        null
    }
}

    private var acquiredLock: WifiManager.MulticastLock? = null

    private fun acquireMulticastLock(): WifiManager.MulticastLock? {
        return try {
            val wifi = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
            val lock = wifi.createMulticastLock("tvhanan_ssdp")
            lock.setReferenceCounted(false)
            lock.acquire()
            acquiredLock = lock
            lock
        } catch (_: Exception) {
            null
        }
    }

    private fun releaseMulticastLock(lock: WifiManager.MulticastLock?) {
        try {
            lock?.release()
            acquiredLock = null
        } catch (_: Exception) {
        }
    }

    private fun parseLocationIp(response: String): String? {
        val locationHeader = response.lines().firstOrNull {
            it.startsWith("LOCATION:", ignoreCase = true)
        } ?: return null

        val url = locationHeader.substringAfter(":").trim()
        return try {
            val host = InetAddress.getByName(
                url.removePrefix("http://").removePrefix("https://")
                    .substringBefore("/").substringBefore(":")
            )
            host.hostAddress
        } catch (_: Exception) {
            null
        }
    }
}
```

### File: ./app/src/main/java/com/tvhanan/data/network/WakeOnLanUtil.kt
```
package com.tvhanan.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Inet4Address
import java.net.NetworkInterface
import android.util.Log

object WakeOnLanUtil {

    private const val TAG = "WakeOnLanUtil"
    private val WOL_PORTS = listOf(9, 7)

    suspend fun sendWakeOnLan(macAddress: String, broadcastIp: String = "255.255.255.255"): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val macBytes = parseMacAddress(macAddress)
                val packetBytes = ByteArray(6 + 16 * macBytes.size)

                for (i in 0 until 6) {
                    packetBytes[i] = 0xFF.toByte()
                }
                for (i in 0 until 16) {
                    System.arraycopy(macBytes, 0, packetBytes, 6 + i * macBytes.size, macBytes.size)
                }

                val targetIp = if (broadcastIp == "255.255.255.255") {
                    getRealSubnetBroadcast()
                } else {
                    broadcastIp
                }

                val address = InetAddress.getByName(targetIp)
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    WOL_PORTS.forEach { port ->
                        val packet = DatagramPacket(packetBytes, packetBytes.size, address, port)
                        socket.send(packet)
                    }
                }
                Log.d(TAG, "Magic packet sent to $macAddress via $targetIp")
                true
            } catch (e: Exception) {
                Log.e(TAG, "sendWakeOnLan failed: ${e.message}")
                false
            }
        }

    suspend fun sendWakeOnLanWithRetry(
        macAddress: String,
        broadcastIp: String = "255.255.255.255",
        attempts: Int = 5,
        intervalMillis: Long = 2000
    ): Boolean = withContext(Dispatchers.IO) {
        repeat(attempts) { attempt ->
            val sent = sendWakeOnLan(macAddress, broadcastIp)
            if (sent) {
                Log.d(TAG, "Attempt ${attempt + 1}/$attempts sent")
                return@withContext true
            }
            if (attempt < attempts - 1) {
                kotlinx.coroutines.delay(intervalMillis)
            }
        }
        false
    }

    private fun getRealSubnetBroadcast(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
            for (networkInterface in interfaces) {
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                for (interfaceAddress in networkInterface.interfaceAddresses) {
                    val broadcast = interfaceAddress.broadcast
                    if (broadcast != null && broadcast is Inet4Address) {
                        return broadcast.hostAddress ?: "255.255.255.255"
                    }
                }
            }
        } catch (_: Exception) {}
        return "255.255.255.255"
    }

    private fun parseMacAddress(mac: String): ByteArray {
        val hex = mac.replace(":", "").replace("-", "").replace(" ", "").uppercase()
        if (hex.length != 12) {
            throw IllegalArgumentException("Alamat MAC harus terdiri dari 12 karakter heksadesimal")
        }
        return ByteArray(6) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
```

### File: ./app/src/main/java/com/tvhanan/data/network/SamsungKeyMapper.kt
```
package com.tvhanan.data.network

import com.tvhanan.domain.model.RemoteKey
import org.json.JSONObject

object SamsungKeyMapper {

    private const val REMOTE_CONTROL_CHANNEL = "ms.remote.control"

    fun createKeyPressPayload(key: RemoteKey): String {
        return JSONObject().apply {
            put("method", REMOTE_CONTROL_CHANNEL)
            put("params", JSONObject().apply {
                put("Cmd", "Click")
                put("DataOfCmd", key.keyCode)
                put("Option", "false")
                put("TypeOfRemote", "SendRemoteKey")
            })
        }.toString()
    }
}
```

### File: ./app/src/main/java/com/tvhanan/data/network/TvWebSocketClient.kt
```
package com.tvhanan.data.network

import android.util.Base64
import android.util.Log
import com.tvhanan.domain.model.ConnectionState
import com.tvhanan.domain.model.RemoteKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume

class TvWebSocketClient(
    private val sslTrustManager: SslTrustManager? = null
) {

    companion object {
        private const val TAG = "TvHanan"
    }

    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    })

    private val sslClient by lazy {
        try {
            val sslCtx = SSLContext.getInstance("TLS")
            sslCtx.init(null, trustAllCerts, SecureRandom())
            OkHttpClient.Builder()
                .pingInterval(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(8, TimeUnit.SECONDS)
                .sslSocketFactory(sslCtx.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { hostname, session ->
                    sslTrustManager?.verifyOrTrust(hostname, session) ?: true
                }
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "SSL client init error: ${e.message}")
            null
        }
    }

    private val plainClient by lazy {
        OkHttpClient.Builder()
            .pingInterval(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    private val webSocketRef = AtomicReference<WebSocket?>(null)
    private val connectionId = AtomicLong(0)
    private var currentToken: String? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _tokenReceived = MutableStateFlow<String?>(null)
    val tokenReceived: StateFlow<String?> = _tokenReceived.asStateFlow()

    private val appNameBase64: String by lazy {
        Base64.encodeToString("TvHanan".toByteArray(), Base64.NO_WRAP)
    }

    suspend fun connect(ip: String, port: Int, token: String? = null): Result<WebSocket> {
        val currentId = connectionId.incrementAndGet()
        disconnect()
        sslTrustManager?.loadFingerprint(ip)

        return suspendCancellableCoroutine { continuation ->
            currentToken = token
            _connectionState.value = ConnectionState.CONNECTING

            Log.d(TAG, "Connecting to $ip:$port...")

            val client = if (port == 8002) (sslClient ?: plainClient) else plainClient
            val request = Request.Builder()
                .url(buildUrl(ip, port, currentToken))
                .header("Origin", "https://localhost:$port")
                .build()

            try {
                val ws = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(ws: WebSocket, response: Response) {
                        if (connectionId.get() != currentId) return
                        Log.d(TAG, "Connected to $ip:$port")
                        _connectionState.value = ConnectionState.CONNECTED
                        if (continuation.isActive) {
                            continuation.resume(Result.success(ws))
                        }
                    }

                    override fun onMessage(ws: WebSocket, text: String) {
                        handleMessage(text)
                    }

                    override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                        if (connectionId.get() != currentId) return
                        val detail = t.message ?: t.javaClass.simpleName
                        Log.e(TAG, "Failed $ip:$port: $detail")
                        if (response != null) Log.e(TAG, "HTTP ${response.code}")

                        _connectionState.value = ConnectionState.ERROR

                        if (continuation.isActive) {
                            continuation.resume(Result.failure(t))
                        }
                    }

                    override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                        if (connectionId.get() != currentId) return
                        Log.d(TAG, "Closed: $code $reason")
                        webSocketRef.compareAndSet(ws, null)
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }
                })

                webSocketRef.set(ws)

                continuation.invokeOnCancellation {
                    if (connectionId.get() != currentId) return@invokeOnCancellation
                    ws.close(1001, "Cancelled")
                    webSocketRef.compareAndSet(ws, null)
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            } catch (e: Exception) {
                Log.e(TAG, "WS error: ${e.message}")
                _connectionState.value = ConnectionState.ERROR
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }
        }
    }

    suspend fun connectWithFallback(ip: String, token: String? = null): Result<WebSocket> {
        Log.d(TAG, "=== Connecting $ip sequentially ===")

        for (port in listOf(8002, 8001)) {
            Log.d(TAG, "Trying $ip:$port...")
            val result = connect(ip, port, token)
            if (result.isSuccess) {
                Log.d(TAG, "Success on $ip:$port!")
                return result
            }
            Log.e(TAG, "Failed $ip:$port: ${result.exceptionOrNull()?.message}")
        }
        return Result.failure(Exception("TV tidak merespon di port 8002 maupun 8001"))
    }

    fun sendKey(key: RemoteKey): Boolean {
        return try {
            val payload = SamsungKeyMapper.createKeyPressPayload(key)
            webSocketRef.get()?.send(payload) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Send error: ${e.message}")
            false
        }
    }

    fun disconnect() {
        webSocketRef.getAndSet(null)?.close(1000, "User disconnected")
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun buildUrl(ip: String, port: Int, token: String? = null): String {
        val scheme = if (port == 8002) "wss" else "ws"
        val base = "$scheme://$ip:$port/api/v2/channels/samsung.remote.control?name=$appNameBase64"
        return if (!token.isNullOrEmpty()) "$base&token=$token" else base
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            Log.d(TAG, "Event: ${json.optString("event")}")

            val newToken = json.optJSONObject("data")?.optString("token")

            if (!newToken.isNullOrEmpty() && currentToken != newToken) {
                currentToken = newToken
                _tokenReceived.value = newToken
                Log.d(TAG, "New Token saved: $newToken")
            }
        } catch (_: Exception) {}
    }
}
```

### File: ./app/src/main/java/com/tvhanan/data/network/SslTrustManager.kt
```
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
        try {
            val fp = prefs.getCertificateFingerprintSync(ip)
            if (fp != null) cache[ip] = fp
        } catch (e: Exception) {
            Log.e(TAG, "loadFingerprint failed for $ip", e)
        }
    }

    fun verifyOrTrust(hostname: String, session: SSLSession?): Boolean {
        if (session == null) {
            Log.w(TAG, "verifyOrTrust: null session for $hostname, allowing")
            return true
        }
        return try {
            val chain = session.peerCertificates
            if (chain.isEmpty()) {
                Log.w(TAG, "verifyOrTrust: empty cert chain for $hostname, allowing")
                return true
            }
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
            true
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
```

### File: ./app/src/main/java/com/tvhanan/data/local/TvPreferences.kt
```
package com.tvhanan.data.local

import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.catch
import java.io.IOException
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tvhanan.util.CryptoUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tv_settings")

class TvPreferences(private val context: Context) {

    private val cryptoUtil = CryptoUtil()

    // Helper untuk menangkap error I/O agar app tidak Force Close
    private val Flow<Preferences>.safeData: Flow<Preferences>
        get() = this.catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
    companion object {
        private val KEY_LAST_IP = stringPreferencesKey("last_ip")
        private val KEY_LAST_PORT = stringPreferencesKey("last_port")
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_MAC_ADDRESS = stringPreferencesKey("mac_address")

        private fun certFingerprintKey(ip: String) = stringPreferencesKey("cert_fp_$ip")
    }

    val lastIp: Flow<String?> = context.dataStore.data.safeData.map { it[KEY_LAST_IP] }
    val lastPort: Flow<String?> = context.dataStore.data.safeData.map { it[KEY_LAST_PORT] }
    val token: Flow<String?> = context.dataStore.data.safeData.map { it[KEY_TOKEN] }
    val macAddress: Flow<String?> = context.dataStore.data.safeData.map { it[KEY_MAC_ADDRESS] }

    suspend fun saveLastIp(ip: String) {
        context.dataStore.edit { it[KEY_LAST_IP] = ip }
    }

    suspend fun saveLastPort(port: String) {
        context.dataStore.edit { it[KEY_LAST_PORT] = port }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[KEY_TOKEN] = cryptoUtil.encrypt(token) }
    }

    suspend fun saveMacAddress(mac: String) {
        context.dataStore.edit { it[KEY_MAC_ADDRESS] = mac }
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.safeData.first()[KEY_TOKEN]?.let { cryptoUtil.decrypt(it) }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    // ===== Certificate Fingerprint (TOFU SSL) =====
    // Metode synchronous untuk dipanggil dari OkHttp thread (non-coroutine)

    fun getCertificateFingerprintSync(ip: String): String? = runBlocking(Dispatchers.IO) {
        context.dataStore.data.safeData.first()[certFingerprintKey(ip)]
    }

    fun saveCertificateFingerprintSync(ip: String, fingerprint: String) = runBlocking(Dispatchers.IO) {
        context.dataStore.edit { it[certFingerprintKey(ip)] = fingerprint }
    }

    fun removeCertificateFingerprintSync(ip: String) = runBlocking(Dispatchers.IO) {
        context.dataStore.edit { it.remove(certFingerprintKey(ip)) }
    }
}
```

### File: ./app/src/main/java/com/tvhanan/data/repository/TvRepositoryImpl.kt
```
package com.tvhanan.data.repository

import com.tvhanan.data.local.TvPreferences
import com.tvhanan.data.network.AppLauncher
import com.tvhanan.data.network.TvDiscoveryService
import com.tvhanan.data.network.TvWebSocketClient
import com.tvhanan.data.network.WakeOnLanUtil
import com.tvhanan.domain.model.ConnectionState
import com.tvhanan.domain.model.RemoteKey
import com.tvhanan.domain.model.TvDevice
import com.tvhanan.domain.repository.TvRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class TvRepositoryImpl(
    private val discoveryService: TvDiscoveryService,
    private val webSocketClient: TvWebSocketClient,
    private val preferences: TvPreferences
) : TvRepository {

    override val lastIp: Flow<String?> = preferences.lastIp
    override val lastPort: Flow<String?> = preferences.lastPort
    override val macAddress: Flow<String?> = preferences.macAddress

    override val connectionState: StateFlow<ConnectionState> = webSocketClient.connectionState
    override val tokenReceived: Flow<String?> = webSocketClient.tokenReceived

    override suspend fun getToken(): String? = preferences.getToken()

    override suspend fun saveToken(token: String) = preferences.saveToken(token)

    override suspend fun saveLastIp(ip: String) = preferences.saveLastIp(ip)

    override suspend fun saveLastPort(port: String) = preferences.saveLastPort(port)

    override suspend fun saveMacAddress(mac: String) = preferences.saveMacAddress(mac)

    override suspend fun clearPreferences() = preferences.clear()

    override suspend fun discoverDevices(): List<TvDevice> = discoveryService.discoverDevices()

    override suspend fun isHostReachable(ip: String, port: Int): Boolean =
        discoveryService.isHostReachable(ip, port)

    override suspend fun connectWithFallback(ip: String, token: String?): Result<Unit> {
        return webSocketClient.connectWithFallback(ip, token).map { }
    }

    override fun sendKey(key: RemoteKey): Boolean = webSocketClient.sendKey(key)

    override fun disconnect() = webSocketClient.disconnect()

    override suspend fun wakeOnLan(mac: String, broadcastIp: String): Boolean =
        WakeOnLanUtil.sendWakeOnLanWithRetry(mac, broadcastIp)

    override suspend fun launchApp(ip: String, appId: String): Boolean =
        AppLauncher.launch(ip, appId)

    override suspend fun closeApp(ip: String, appId: String): Boolean =
        AppLauncher.close(ip, appId)
}
```

### File: ./app/src/main/java/com/tvhanan/domain/model/RemoteKey.kt
```
package com.tvhanan.domain.model

enum class RemoteKey(val keyCode: String, val label: String) {
    POWER("KEY_POWER", "Power"),
    HOME("KEY_HOME", "Home"),
    BACK("KEY_RETURN", "Back"),

    DPAD_UP("KEY_UP", "\u25B2"),
    DPAD_DOWN("KEY_DOWN", "\u25BC"),
    DPAD_LEFT("KEY_LEFT", "\u25C0"),
    DPAD_RIGHT("KEY_RIGHT", "\u25B6"),
    ENTER("KEY_ENTER", "OK"),

    VOL_UP("KEY_VOLUP", "Vol+"),
    VOL_DOWN("KEY_VOLDOWN", "Vol-"),
    MUTE("KEY_MUTE", "Mute"),

    CH_UP("KEY_CHUP", "CH+"),
    CH_DOWN("KEY_CHDOWN", "CH-"),
    CH_LIST("KEY_CH_LIST", "CH List"),

    KEY_0("KEY_0", "0"),
    KEY_1("KEY_1", "1"),
    KEY_2("KEY_2", "2"),
    KEY_3("KEY_3", "3"),
    KEY_4("KEY_4", "4"),
    KEY_5("KEY_5", "5"),
    KEY_6("KEY_6", "6"),
    KEY_7("KEY_7", "7"),
    KEY_8("KEY_8", "8"),
    KEY_9("KEY_9", "9"),
    PRE_CH("KEY_PRECH", "Pre-CH"),

    RED("KEY_RED", "Red"),
    GREEN("KEY_GREEN", "Green"),
    YELLOW("KEY_YELLOW", "Yellow"),
    BLUE("KEY_BLUE", "Blue"),

    SOURCE("KEY_SOURCE", "Source"),
    HDMI("KEY_HDMI", "HDMI"),

    PLAY("KEY_PLAY", "Play"),
    PAUSE("KEY_PAUSE", "Pause"),
    STOP("KEY_STOP", "Stop"),
    REWIND("KEY_REWIND", "Rewind"),
    FAST_FORWARD("KEY_FF", "FF"),

    MENU("KEY_MENU", "Menu"),
    GUIDE("KEY_GUIDE", "Guide"),
    INFO("KEY_INFO", "Info"),
    EXIT("KEY_EXIT", "Exit")
}
```

### File: ./app/src/main/java/com/tvhanan/domain/model/AppShortcut.kt
```
package com.tvhanan.domain.model

/**
 * App ID Samsung Smart Hub. ID ini TIDAK resmi didokumentasikan Samsung
 * dan bisa berubah/berbeda per region & firmware — nilai di sini sudah
 * diverifikasi LANGSUNG dari TV (lewat ed.installedApp.get) untuk unit
 * Samsung UA32N4300 (N-series 2020, Tizen 5.0).
 *
 * Cara launch app TERBUKTI bekerja di firmware ini lewat REST API:
 *   POST  http://{ip}:8001/api/v2/applications/{appId}   -> buka app
 *   DELETE http://{ip}:8001/api/v2/applications/{appId}  -> tutup app
 * (lihat AppLauncher.kt)
 *
 * Catatan riset tambahan (belum dipakai, untuk referensi masa depan):
 * Launch app JUGA bisa lewat WebSocket ms.channel.emit/ed.apps.launch,
 * TAPI action_type harus disesuaikan dengan app_type masing-masing app
 * (didapat dari ed.installedApp.get):
 *   app_type 2 (kebanyakan app streaming: Netflix, YouTube, Prime, dst)
 *     -> action_type harus "DEEP_LINK"
 *   app_type 4 (system app seperti browser)
 *     -> action_type harus "NATIVE_LAUNCH"
 * Salah pasangan action_type/app_type menyebabkan TV diam tanpa respons
 * (bukan error, cuma diabaikan) — ini sumber kebingungan utama saat
 * awal mengembangkan fitur ini.
 */
enum class AppShortcut(val appId: String, val label: String) {
    NETFLIX("11101200001", "Netflix"),
    PRIME_VIDEO("3201512006785", "Prime Video"),
    YOUTUBE("111299001912", "YouTube")
}```

### File: ./app/src/main/java/com/tvhanan/domain/model/TvDevice.kt
```
package com.tvhanan.domain.model

data class TvDevice(
    val ipAddress: String,
    val name: String = "Samsung TV",
    val macAddress: String? = null,
    val port: Int = 8002, // Prioritaskan 8002 sebagai port bawaan
    val token: String? = null
)
```

### File: ./app/src/main/java/com/tvhanan/domain/model/ConnectionState.kt
```
package com.tvhanan.domain.model

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
```

### File: ./app/src/main/java/com/tvhanan/domain/repository/TvRepository.kt
```
package com.tvhanan.domain.repository

import com.tvhanan.domain.model.ConnectionState
import com.tvhanan.domain.model.RemoteKey
import com.tvhanan.domain.model.TvDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TvRepository {
    val lastIp: Flow<String?>
    val lastPort: Flow<String?>
    val macAddress: Flow<String?>

    val connectionState: StateFlow<ConnectionState>
    val tokenReceived: Flow<String?>

    suspend fun getToken(): String?
    suspend fun saveToken(token: String)
    suspend fun saveLastIp(ip: String)
    suspend fun saveLastPort(port: String)
    suspend fun saveMacAddress(mac: String)
    suspend fun clearPreferences()

    suspend fun discoverDevices(): List<TvDevice>
    suspend fun isHostReachable(ip: String, port: Int): Boolean

    suspend fun connectWithFallback(ip: String, token: String? = null): Result<Unit>
    fun sendKey(key: RemoteKey): Boolean
    fun disconnect()

    suspend fun wakeOnLan(mac: String, broadcastIp: String = "255.255.255.255"): Boolean
    suspend fun launchApp(ip: String, appId: String): Boolean
    suspend fun closeApp(ip: String, appId: String): Boolean
}
```

### File: ./app/src/main/java/com/tvhanan/MainActivity.kt
```
package com.tvhanan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.tvhanan.ui.navigation.TvRemoteNavGraph
import com.tvhanan.ui.theme.TvRemoteTheme
import com.tvhanan.util.HapticUtil

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        HapticUtil.init(applicationContext)

        val app = application as TvRemoteApp

        setContent {
            TvRemoteTheme {
                val navController = rememberNavController()
                TvRemoteNavGraph(
                    navController = navController,
                    serviceLocator = app.serviceLocator,
                    onExitApp = { finish() }
                )
            }
        }
    }
}
```

### File: ./app/src/main/java/com/tvhanan/ui/remote/RemoteViewModel.kt
```
package com.tvhanan.ui.remote

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhanan.domain.model.ConnectionState
import com.tvhanan.domain.model.RemoteKey
import com.tvhanan.domain.repository.TvRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class RemoteViewModel(
    private val ipAddress: String,
    private val port: Int = 8002,
    private val macAddress: String? = null,
    private val repository: TvRepository
) : ViewModel() {
    
    private var tokenObserverJob: kotlinx.coroutines.Job? = null
    private val isConnecting = AtomicBoolean(false)

    companion object {
        private const val TAG = "TvHanan"
    }

    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lastSavedToken = MutableStateFlow<String?>(null)
    
    private val _isMacAvailable = MutableStateFlow(false)
    val isMacAvailable: StateFlow<Boolean> = _isMacAvailable.asStateFlow()

    init {
        viewModelScope.launch {
            if (macAddress != null) {
                _isMacAvailable.value = true
            } else {
                repository.macAddress.collect { mac ->
                    _isMacAvailable.value = !mac.isNullOrBlank()
                }
            }
        }
    }
    
    fun connect() {
        if (!isConnecting.compareAndSet(false, true)) {
            Log.d(TAG, "connect() skipped: already connecting")
            return
        }
        Log.d(TAG, "connect() called for $ipAddress")
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val savedToken = repository.getToken()
                Log.d(TAG, "savedToken = ${if (savedToken == null) "null" else "exists"}")

                val result = repository.connectWithFallback(ipAddress, savedToken)

                if (result.isSuccess) {
                    Log.d(TAG, "Connection succeeded")
                    if (savedToken == null) {
                        tokenObserverJob?.cancel()
                        tokenObserverJob = launch {
                            val newToken = repository.tokenReceived
                                .filterNotNull()
                                .firstOrNull()
                            if (newToken != null) {
                                repository.saveToken(newToken)
                                _lastSavedToken.value = newToken
                                Log.d(TAG, "First token saved: $newToken")
                            }
                        }
                    } else {
                        _lastSavedToken.value = savedToken
                    }
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Gagal terhubung ke TV"
                    Log.e(TAG, "Connection failed: $errorMsg")
                    _errorMessage.value = errorMsg
                }
            } finally {
                isConnecting.set(false)
            }
        }
    }

    /**
     * Memantau token yang baru tersimpan (baik dari pairing baru maupun
     * yang sudah ada sebelumnya), memanggil [onNewToken] tiap kali nilainya
     * berubah dan tidak null. Dipakai NavGraph untuk sinkronisasi token ke
     * SettingsViewModel tanpa duplikasi logic penyimpanan token.
     */
    suspend fun observeNewToken(onNewToken: suspend (String) -> Unit) {
        _lastSavedToken.filterNotNull().collect { token ->
            onNewToken(token)
        }
    }

    /**
     * Mengirim key ke TV. Catatan: haptic feedback TIDAK dipicu di sini —
     * itu ditangani oleh komponen UI (HapticGlassButton) saat tombol
     * mulai ditekan (onPress), bukan di layer ViewModel. Ini menghindari
     * getar terpicu dua kali (sekali dari UI saat press, sekali lagi
     * dari sini) dan menjaga ViewModel tidak punya concern soal detail
     * presentasi seperti vibration.
     */
    fun sendKey(key: RemoteKey) {
        if (key == RemoteKey.POWER && connectionState.value != ConnectionState.CONNECTED) {
            wakeOnLan()
        } else {
            repository.sendKey(key)
        }
    }

    fun wakeOnLan() {
        viewModelScope.launch {
            val mac = macAddress ?: repository.macAddress.firstOrNull()
            if (!mac.isNullOrBlank()) {
                Log.d(TAG, "Mencoba menyalakan TV via WoL (dengan Retry) ke MAC: $mac")

                val success = repository.wakeOnLan(mac)

                if (success) {
                    _errorMessage.value = "TV sedang dinyalakan, mencoba menghubungkan kembali..."
                    kotlinx.coroutines.delay(8000)

                    repeat(4) { attempt ->
                        if (connectionState.value == ConnectionState.CONNECTED) return@repeat
                        Log.d(TAG, "Auto-reconnect setelah WOL, percobaan ke-${attempt + 1}")
                        connect()
                        while (isConnecting.get()) {
                            kotlinx.coroutines.delay(500)
                        }
                        if (connectionState.value != ConnectionState.CONNECTED && attempt < 3) {
                            kotlinx.coroutines.delay(4000)
                        }
                    }
                }
            } else {
                Log.e(TAG, "Gagal menjalankan WoL: Alamat MAC tidak ditemukan")
            }
        }
    }

    fun launchApp(appId: String) {
        viewModelScope.launch {
            val success = repository.launchApp(ipAddress, appId)
            Log.d(TAG, "launchApp($appId) success=$success")
        }
    }

    fun closeApp(appId: String) {
        viewModelScope.launch {
            val success = repository.closeApp(ipAddress, appId)
            Log.d(TAG, "closeApp($appId) success=$success")
        }
    }

    fun disconnect() {
        repository.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
```

### File: ./app/src/main/java/com/tvhanan/ui/remote/RemoteScreen.kt
```
package com.tvhanan.ui.remote

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvhanan.domain.model.ConnectionState
import com.tvhanan.domain.model.RemoteKey
import com.tvhanan.ui.components.DpadRing
import com.tvhanan.ui.components.HapticGlassButton
import com.tvhanan.ui.components.HapticGlassLabelButton
import com.tvhanan.ui.components.MeshGradientBackground
import com.tvhanan.ui.components.ZoneLabel
import com.tvhanan.ui.theme.*
import com.tvhanan.util.HapticUtil

/**
 * Layar remote utama. Dibagi 9 zona sesuai preview yang disepakati,
 * ditampilkan via LazyColumn + stickyHeader bawaan Compose Foundation
 * supaya status bar + tombol Settings selalu terlihat saat scroll panjang ke bawah.
 *
 * @param scaleFactor faktor skala ukuran tombol, berasal dari SettingsViewModel.
 */

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun RemoteScreen(
    viewModel: RemoteViewModel,
    onOpenSettings: () -> Unit,
    scaleFactor: Float = 1f,
    keepScreenOn: Boolean = true,
    hapticEnabled: Boolean = true,           // Parameter baru untuk sinkronisasi getar
    meshBackgroundEnabled: Boolean = true     // Parameter baru untuk sinkronisasi aurora
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isMacAvailable by viewModel.isMacAvailable.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // SINKRONISASI GETAR: Hubungkan setelan dinamis haptic ke utility getar
    LaunchedEffect(hapticEnabled) {
        HapticUtil.isEnabled = hapticEnabled
    }

    // Set Flag Window agar layar HP tidak meredup/mati 
    DisposableEffect(keepScreenOn) {
        val window = context.findActivity()?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.connect()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnect()
        }
    }

    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize().background(BgBase)) {
        if (meshBackgroundEnabled) {
            MeshGradientBackground(modifier = Modifier.fillMaxSize())
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy((18 * scaleFactor).dp)
        ) {
            item(key = "header") {
                RemoteHeaderBar(
                    connectionState = connectionState,
                    isMacAvailable = isMacAvailable,
                    onSettingsClick = onOpenSettings
                )
            }

            if (connectionState == ConnectionState.ERROR || connectionState == ConnectionState.DISCONNECTED) {
                if (isMacAvailable) {
                    item(key = "standby_banner") {
                        StandbyBanner(
                            onWakeClick = { viewModel.wakeOnLan() },
                            scaleFactor = scaleFactor
                        )
                    }
                } else {
                    errorMessage?.let { message ->
                        item(key = "error_banner") { ErrorBanner(message = message, onRetry = { viewModel.connect() }) }
                    }
                }
            }

            item(key = "power_row") { PowerSourceSleepRow(viewModel, scaleFactor) }

            item(key = "navigation") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ZoneLabel("Navigasi", accentColor = NavAccent)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        DpadRing(
                            onUp = { viewModel.sendKey(RemoteKey.DPAD_UP) },
                            onDown = { viewModel.sendKey(RemoteKey.DPAD_DOWN) },
                            onLeft = { viewModel.sendKey(RemoteKey.DPAD_LEFT) },
                            onRight = { viewModel.sendKey(RemoteKey.DPAD_RIGHT) },
                            onOk = { viewModel.sendKey(RemoteKey.ENTER) },
                            size = (216 * scaleFactor).dp
                        )
                    }
                    BackHomeExitRow(viewModel, scaleFactor)
                }
            }

            item(key = "volume_channel") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZoneLabel("Volume & Channel", accentColor = NavAccent2)
                    VolumeChannelSection(viewModel, scaleFactor)
                }
            }

            item(key = "numpad") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneLabel("Angka", accentColor = AccentWarn)
                    NumpadGrid(viewModel, scaleFactor)
                }
            }

            item(key = "color_keys") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneLabel(
                        "Smart Hub Color Keys",
                        accentBrush = Brush.horizontalGradient(listOf(ColorKeyRed, ColorKeyGreen))
                    )
                    ColorKeysRow(viewModel, scaleFactor)
                }
            }

            item(key = "media") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneLabel("Media", accentColor = MediaAccent)
                    MediaTransportRow(viewModel, scaleFactor)
                }
            }

            item(key = "menu_info") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneLabel("Menu & Info", accentColor = TextDim)
                    MenuInfoGrid(viewModel, scaleFactor)
                }
            }

            item(key = "app_shortcuts") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneLabel(
                        "App Pintasan",
                        accentBrush = Brush.horizontalGradient(listOf(NetflixRed, PrimeBlue))
                    )
                    AppShortcutsRow(viewModel, scaleFactor)
                }
            }
        }
    }
}

@Composable
private fun RemoteHeaderBar(
    connectionState: ConnectionState, 
    isMacAvailable: Boolean,
    onSettingsClick: () -> Unit
) {
    val (label, color) = when {
        connectionState == ConnectionState.CONNECTED -> "Connected" to ConnectedColor
        connectionState == ConnectionState.CONNECTING -> "Menghubungkan..." to ConnectingColor
        isMacAvailable && (connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR) -> "Siaga (Standby)" to ConnectingColor
        connectionState == ConnectionState.DISCONNECTED -> "Terputus" to DisconnectedColor
        else -> "Error" to DisconnectedColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgBase.copy(alpha = 0.78f), RoundedCornerShape(999.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(999.dp))
            .padding(start = 14.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.size(width = 8.dp, height = 1.dp))
        Text(text = label, color = TextDim, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "SAMSUNG · N4300",
            color = TextPrimary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )

        Spacer(modifier = Modifier.size(width = 10.dp, height = 1.dp))

        HapticGlassButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(34.dp),
            shape = CircleShape
        ) {
            Text("⚙", color = TextDim, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DisconnectedColor.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .border(1.dp, DisconnectedColor.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(text = message, color = DisconnectedColor, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(10.dp))
        HapticGlassButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Coba Lagi", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StandbyBanner(onWakeClick: () -> Unit, scaleFactor: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavAccent2.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .border(1.dp, NavAccent2.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⏻", color = NavAccent2, fontSize = (28 * scaleFactor).sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "TV dalam Mode Siaga (Standby)",
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tekan tombol daya merah di bawah atau tombol di bawah ini untuk menyalakan TV.",
            color = TextDim,
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        HapticGlassButton(
            onClick = onWakeClick,
            modifier = Modifier.fillMaxWidth().height((44 * scaleFactor).dp),
            gradientColors = listOf(NavAccent.copy(alpha = 0.20f), NavAccent2.copy(alpha = 0.16f)),
            borderColor = NavAccent.copy(alpha = 0.35f)
        ) {
            Text("Nyalakan TV (WOL)", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PowerSourceSleepRow(viewModel: RemoteViewModel, scaleFactor: Float) {
    val height = (60 * scaleFactor).dp
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HapticGlassButton(
            onClick = { viewModel.sendKey(RemoteKey.POWER) },
            modifier = Modifier.weight(1f).height(height),
            gradientColors = listOf(PowerGradientStart.copy(alpha = 0.24f), PowerGradientEnd.copy(alpha = 0.16f)),
            borderColor = PowerGradientStart.copy(alpha = 0.38f)
        ) {
            // Menggunakan Simbol Daya IEC ⏻ (Unicode Power)
            Text("⏻", color = Color(0xFFFFB199), style = MaterialTheme.typography.titleLarge, fontSize = (22 * scaleFactor).sp)
        }
        HapticGlassButton(
            onClick = { viewModel.sendKey(RemoteKey.SOURCE) },
            modifier = Modifier.weight(1f).height(height)
        ) {
            // Menggunakan Simbol Input Berputar/Siklus ⇥
            Text("⇥", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontSize = (20 * scaleFactor).sp)
        }
        HapticGlassButton(
            onClick = { viewModel.sendKey(RemoteKey.HDMI) },
            modifier = Modifier.weight(1f).height(height)
        ) {
            // Menggunakan Simbol Steker/Konektor Kabel 🔌
            Text("🔌", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontSize = (18 * scaleFactor).sp)
        }
    }
}

@Composable
private fun BackHomeExitRow(viewModel: RemoteViewModel, scaleFactor: Float) {
    val height = (50 * scaleFactor).dp
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HapticGlassLabelButton(
            label = "↶", // Menggunakan Kurva Putar Balik untuk 'Back'
            onClick = { viewModel.sendKey(RemoteKey.BACK) },
            modifier = Modifier.weight(1f).height(height),
            fontSize = (20 * scaleFactor).sp
        )
        HapticGlassLabelButton(
            label = "⌂", // Menggunakan Simbol Rumah Klasik untuk 'Home'
            onClick = { viewModel.sendKey(RemoteKey.HOME) },
            modifier = Modifier.weight(1f).height(height),
            fontSize = (22 * scaleFactor).sp
        )
        HapticGlassLabelButton(
            label = "✕", // Menggunakan Cross Ramping untuk 'Exit'
            onClick = { viewModel.sendKey(RemoteKey.EXIT) },
            modifier = Modifier.weight(1f).height(height),
            fontSize = (18 * scaleFactor).sp
        )
    }
}

private data class PillCell(
    val label: String, 
    val isSymbol: Boolean, 
    val autoRepeat: Boolean = false, // Default bernilai false
    val onClick: () -> Unit
)

@Composable
private fun VolumeChannelSection(viewModel: RemoteViewModel, scaleFactor: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PillRow(
            scaleFactor = scaleFactor,
            cells = listOf(
                PillCell("−", isSymbol = true, autoRepeat = true) { viewModel.sendKey(RemoteKey.VOL_DOWN) },
                PillCell("🔊", isSymbol = true) { },
                PillCell("+", isSymbol = true, autoRepeat = true) { viewModel.sendKey(RemoteKey.VOL_UP) },
                PillCell("🔇", isSymbol = true) { viewModel.sendKey(RemoteKey.MUTE) }
            )
        )
        PillRow(
            scaleFactor = scaleFactor,
            cells = listOf(
                PillCell("−", isSymbol = true, autoRepeat = true) { viewModel.sendKey(RemoteKey.CH_DOWN) },
                PillCell("📺", isSymbol = true) { },
                PillCell("+", isSymbol = true, autoRepeat = true) { viewModel.sendKey(RemoteKey.CH_UP) },
                PillCell("☰", isSymbol = true) { viewModel.sendKey(RemoteKey.CH_LIST) }
            )
        )
        HapticGlassLabelButton(
            label = "⇄",
            onClick = { viewModel.sendKey(RemoteKey.PRE_CH) },
            modifier = Modifier.fillMaxWidth().height((46 * scaleFactor).dp),
            fontSize = (20 * scaleFactor).sp
        )
    }
}

@Composable
private fun PillRow(cells: List<PillCell>, scaleFactor: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height((54 * scaleFactor).dp)
            .background(Color.Transparent, RoundedCornerShape(18.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp)),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        cells.forEachIndexed { index, cell ->
            val shape = when (index) {
                0 -> RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                cells.lastIndex -> RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
                else -> RoundedCornerShape(0.dp)
            }
            HapticGlassButton(
                onClick = cell.onClick,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = shape,
                autoRepeat = cell.autoRepeat, // SALURKAN AUTO-REPEAT DI SINI
                borderColor = Color.Transparent
            ) {
                Text(
                    text = cell.label,
                    color = if (cell.isSymbol) TextPrimary else TextDim,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun NumpadGrid(viewModel: RemoteViewModel, scaleFactor: Float) {
    val rows = listOf(
        listOf(RemoteKey.KEY_1, RemoteKey.KEY_2, RemoteKey.KEY_3),
        listOf(RemoteKey.KEY_4, RemoteKey.KEY_5, RemoteKey.KEY_6),
        listOf(RemoteKey.KEY_7, RemoteKey.KEY_8, RemoteKey.KEY_9),
        listOf(RemoteKey.KEY_0)
    )
    val keyHeight = (54 * scaleFactor).dp
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    HapticGlassLabelButton(
                        label = key.label,
                        onClick = { viewModel.sendKey(key) },
                        modifier = Modifier.weight(1f).height(keyHeight),
                        fontSize = 19.sp
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ColorKeysRow(viewModel: RemoteViewModel, scaleFactor: Float) {
    val height = (46 * scaleFactor).dp
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ColorKeyButton("A", ColorKeyRed, Modifier.weight(1f), height) { viewModel.sendKey(RemoteKey.RED) }
        ColorKeyButton("B", ColorKeyGreen, Modifier.weight(1f), height) { viewModel.sendKey(RemoteKey.GREEN) }
        ColorKeyButton("C", ColorKeyYellow, Modifier.weight(1f), height) { viewModel.sendKey(RemoteKey.YELLOW) }
        ColorKeyButton("D", ColorKeyBlue, Modifier.weight(1f), height) { viewModel.sendKey(RemoteKey.BLUE) }
    }
}

@Composable
private fun ColorKeyButton(label: String, color: Color, modifier: Modifier, height: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    HapticGlassButton(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(14.dp),
        gradientColors = listOf(color.copy(alpha = 0.20f), color.copy(alpha = 0.07f)),
        borderColor = color.copy(alpha = 0.35f),
        contentColor = color
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun MediaTransportRow(viewModel: RemoteViewModel, scaleFactor: Float) {
    val buttons = listOf(
        // Triple berisi: Icon, Perintah, dan status Auto-Repeat (true/false)
        Triple("\u23EA", { viewModel.sendKey(RemoteKey.REWIND) }, true),      // Rewind -> TRUE
        Triple("\u25B6", { viewModel.sendKey(RemoteKey.PLAY) }, false),
        Triple("\u23F8", { viewModel.sendKey(RemoteKey.PAUSE) }, false),
        Triple("\u23F9", { viewModel.sendKey(RemoteKey.STOP) }, false),
        Triple("\u23E9", { viewModel.sendKey(RemoteKey.FAST_FORWARD) }, true) // Fast Forward -> TRUE
    )
    val height = (52 * scaleFactor).dp
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        buttons.forEach { (icon, action, autoRepeat) ->
            HapticGlassButton(
                onClick = action,
                modifier = Modifier.weight(1f).height(height),
                shape = RoundedCornerShape(15.dp),
                autoRepeat = autoRepeat, // SALURKAN AUTO-REPEAT DI SINI
                gradientColors = listOf(MediaAccent.copy(alpha = 0.14f), MediaAccent2.copy(alpha = 0.10f)),
                borderColor = MediaAccent.copy(alpha = 0.25f)
            ) {
                Text(icon, color = MediaAccent, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun MenuInfoGrid(viewModel: RemoteViewModel, scaleFactor: Float) {
    val items = listOf(
        Triple("☰", RemoteKey.MENU, "☰"),  // Menu -> List Rata Kiri
        Triple("📅", RemoteKey.GUIDE, "📅"), // Guide -> Kalender Agenda
        Triple("ℹ", RemoteKey.INFO, "ℹ")   // Info -> Huruf i Informasi
    )
    val height = (58 * scaleFactor).dp
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        items.forEach { (label, key, icon) ->
            HapticGlassButton(
                onClick = { viewModel.sendKey(key) },
                modifier = Modifier.weight(1f).height(height),
                shape = RoundedCornerShape(16.dp)
            ) {
                // Tampilan bersih terpusat tanpa sub-label teks di bawahnya
                Text(
                    text = icon,
                    color = TextPrimary.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = (20 * scaleFactor).sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppShortcutsRow(viewModel: RemoteViewModel, scaleFactor: Float) {
    val height = (54 * scaleFactor).dp
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AppShortcutButton(
            label = "NETFLIX",
            color = NetflixRed,
            modifier = Modifier.weight(1f),
            height = height,
            onLaunch = { viewModel.launchApp(com.tvhanan.domain.model.AppShortcut.NETFLIX.appId) },
            onClose = { viewModel.closeApp(com.tvhanan.domain.model.AppShortcut.NETFLIX.appId) }
        )
        AppShortcutButton(
            label = "PRIME",
            color = PrimeBlue,
            modifier = Modifier.weight(1f),
            height = height,
            onLaunch = { viewModel.launchApp(com.tvhanan.domain.model.AppShortcut.PRIME_VIDEO.appId) },
            onClose = { viewModel.closeApp(com.tvhanan.domain.model.AppShortcut.PRIME_VIDEO.appId) }
        )
        AppShortcutButton(
            label = "YOUTUBE",
            color = YoutubeRed,
            modifier = Modifier.weight(1f),
            height = height,
            onLaunch = { viewModel.launchApp(com.tvhanan.domain.model.AppShortcut.YOUTUBE.appId) },
            onClose = { viewModel.closeApp(com.tvhanan.domain.model.AppShortcut.YOUTUBE.appId) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppShortcutButton(
    label: String,
    color: Color,
    modifier: Modifier,
    height: androidx.compose.ui.unit.Dp,
    onLaunch: () -> Unit,
    onClose: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(height)
            .background(
                Brush.linearGradient(listOf(color.copy(alpha = 0.18f), color.copy(alpha = 0.06f))),
                RoundedCornerShape(16.dp)
            )
            .border(1.dp, color.copy(alpha = 0.32f), RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    HapticUtil.tick()
                    onLaunch()
                },
                onLongClick = {
                    HapticUtil.tick()
                    onClose()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}```

### File: ./app/src/main/java/com/tvhanan/ui/components/DpadRing.kt
```
package com.tvhanan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tvhanan.ui.theme.NavAccent
import com.tvhanan.ui.theme.NavAccent2
import com.tvhanan.ui.theme.TextPrimary

/**
 * D-pad dengan cincin conic-gradient teal->biru mengelilingi 4 tombol
 * arah + tombol OK kaca di tengah. Ini elemen signature yang membedakan
 * dari D-pad Material/CircleShape solid biasa.
 *
 * Catatan: di Compose, Text di dalam Box(contentAlignment = Center)
 * otomatis center secara akurat — bug optical-centering yang sempat
 * terjadi di versi HTML/CSS preview (akibat letter-spacing) tidak
 * relevan di sini.
 */
private enum class DpadDirection { UP, DOWN, LEFT, RIGHT }

@Composable
fun DpadRing(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 216.dp
) {
    val sweepBrush = remember {
        Brush.sweepGradient(
            listOf(
                NavAccent.copy(alpha = 0.55f),
                NavAccent2.copy(alpha = 0.55f),
                NavAccent.copy(alpha = 0.55f)
            )
        )
    }
    val radialBrush = remember {
        Brush.radialGradient(
            listOf(Color(0xFF14161C), Color(0xFF0D0E12))
        )
    }
    Box(
        modifier = modifier
            .size(size)
            .background(brush = sweepBrush, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size - 6.dp)
                .background(brush = radialBrush, shape = CircleShape)
        ) {
            DpadArrowZone(onClick = onUp, alignment = Alignment.TopCenter, direction = DpadDirection.UP)
            DpadArrowZone(onClick = onDown, alignment = Alignment.BottomCenter, direction = DpadDirection.DOWN)
            DpadArrowZone(onClick = onLeft, alignment = Alignment.CenterStart, direction = DpadDirection.LEFT)
            DpadArrowZone(onClick = onRight, alignment = Alignment.CenterEnd, direction = DpadDirection.RIGHT)

            HapticGlassButton(
                onClick = onOk,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(74.dp),
                shape = CircleShape,
                gradientColors = listOf(
                    NavAccent.copy(alpha = 0.20f),
                    NavAccent2.copy(alpha = 0.18f)
                ),
                borderColor = NavAccent.copy(alpha = 0.45f)
            ) {
                Text(
                    text = "OK",
                    color = TextPrimary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.DpadArrowZone(
    onClick: () -> Unit,
    alignment: Alignment,
    direction: DpadDirection
) {
    val edgePadding = 6.dp
    val zoneModifier = when (direction) {
        DpadDirection.UP -> Modifier.align(alignment).padding(top = edgePadding)
        DpadDirection.DOWN -> Modifier.align(alignment).padding(bottom = edgePadding)
        DpadDirection.LEFT -> Modifier.align(alignment).padding(start = edgePadding)
        DpadDirection.RIGHT -> Modifier.align(alignment).padding(end = edgePadding)
    }

    HapticGlassButton(
        onClick = onClick,
        modifier = zoneModifier.size(50.dp),
        shape = RoundedCornerShape(15.dp),
        autoRepeat = true, // AKTIFKAN AUTO-REPEAT DI SINI
        borderColor = NavAccent.copy(alpha = 0.08f)
    ) {
        Text(
            text = when (direction) {
                DpadDirection.UP -> "\u25B2"
                DpadDirection.DOWN -> "\u25BC"
                DpadDirection.LEFT -> "\u25C0"
                DpadDirection.RIGHT -> "\u25B6"
            },
            color = TextPrimary.copy(alpha = 0.85f),
            style = MaterialTheme.typography.titleMedium
        )
    }
}
```

### File: ./app/src/main/java/com/tvhanan/ui/components/MeshBackground.kt
```
package com.tvhanan.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.tvhanan.ui.theme.BgBase
import com.tvhanan.ui.theme.MeshBlob1
import com.tvhanan.ui.theme.MeshBlob2
import com.tvhanan.ui.theme.MeshBlob3
import com.tvhanan.ui.theme.MeshBlob4

/**
 * Background "aurora" statis — beberapa radial gradient blob yang
 * di-blend di atas base gelap. Sengaja TIDAK animasi dan TIDAK
 * memakai BlurEffect/backdrop-blur real-time, karena:
 *
 * 1. Canvas ini hanya digambar ulang saat ukurannya berubah (rotasi
 *    layar/resize), bukan setiap frame atau setiap recomposition lain
 *    di layar (klik tombol dsb tidak memicu redraw blob ini).
 * 2. BlurEffect real-time mahal di GPU low-end (penting untuk target
 *    Android 9 / device lawas) — di sini efek "kaca" disimulasikan
 *    lewat alpha pada komponen GlassButton, bukan blur sungguhan.
 *
 * Dipasang sebagai layer paling belakang (di belakang konten scroll),
 * mengisi seluruh layar via Modifier.fillMaxSize() dari pemanggil.
 */
@Composable
fun MeshGradientBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(BgBase).graphicsLayer()) {
        drawMeshBlob(
            color = MeshBlob1,
            center = Offset(size.width * 0.08f, size.height * 0.05f),
            radius = size.width * 0.62f,
            alpha = 0.65f
        )
        drawMeshBlob(
            color = MeshBlob2,
            center = Offset(size.width * 0.96f, size.height * 0.24f),
            radius = size.width * 0.58f,
            alpha = 0.55f
        )
        drawMeshBlob(
            color = MeshBlob3,
            center = Offset(size.width * 0.10f, size.height * 0.56f),
            radius = size.width * 0.62f,
            alpha = 0.48f
        )
        drawMeshBlob(
            color = MeshBlob4,
            center = Offset(size.width * 0.92f, size.height * 0.74f),
            radius = size.width * 0.58f,
            alpha = 0.50f
        )
    }
}

private fun DrawScope.drawMeshBlob(color: Color, center: Offset, radius: Float, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}
```

### File: ./app/src/main/java/com/tvhanan/ui/components/ZoneLabel.kt
```
package com.tvhanan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tvhanan.ui.theme.TextFaint

/**
 * Label kecil di atas tiap zona tombol (mis. "Navigasi", "Volume & Channel"),
 * dengan garis warna pendek di samping kiri sebagai penanda visual zona —
 * supaya mata bisa langsung kenali zona tanpa membaca teksnya secara penuh.
 */
@Composable
fun ZoneLabel(
    text: String,
    accentColor: Color? = null,
    accentBrush: Brush? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dashModifier = Modifier
            .size(width = 14.dp, height = 2.dp)
            .let { base ->
                when {
                    accentBrush != null -> base.background(accentBrush, RoundedCornerShape(2.dp))
                    accentColor != null -> base.background(accentColor, RoundedCornerShape(2.dp))
                    else -> base.background(TextFaint, RoundedCornerShape(2.dp))
                }
            }

        Box(modifier = dashModifier)
        Spacer(modifier = Modifier.size(width = 8.dp, height = 1.dp))
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextFaint
        )
    }
}
```

### File: ./app/src/main/java/com/tvhanan/ui/components/HapticGlassButton.kt
```
package com.tvhanan.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvhanan.ui.theme.GlassBorder
import com.tvhanan.ui.theme.TextPrimary
import com.tvhanan.util.HapticUtil

/**
 * GlassButton + pemicu haptic otomatis saat tombol mulai ditekan
 * (bukan saat onClick/dilepas — supaya getar terasa instan, sesuai
 * keputusan desain awal: haptic di onPress, bukan onRelease).
 *
 * Dipakai untuk semua tombol remote fisik (D-pad, angka, power, dst)
 * supaya pemanggilan di RemoteScreen tidak perlu menulis ulang logic
 * HapticUtil.tick() di setiap tempat.
 */
@Composable
fun HapticGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    gradientColors: List<Color>? = null,
    borderColor: Color = GlassBorder,
    contentColor: Color = TextPrimary,
    enabled: Boolean = true,
    autoRepeat: Boolean = false, // Tambahkan parameter di sini
    content: @Composable () -> Unit
) {
    GlassButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        gradientColors = gradientColors,
        borderColor = borderColor,
        contentColor = contentColor,
        enabled = enabled,
        autoRepeat = autoRepeat, // Salurkan ke GlassButton
        onPressedChange = { pressed -> if (pressed) HapticUtil.tick() },
        content = content
    )
}

@Composable
fun HapticGlassLabelButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    gradientColors: List<Color>? = null,
    borderColor: Color = GlassBorder,
    contentColor: Color = TextPrimary,
    autoRepeat: Boolean = false, // Tambahkan parameter di sini
    fontSize: TextUnit = 17.sp
) {
    HapticGlassButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        gradientColors = gradientColors,
        borderColor = borderColor,
        contentColor = contentColor,
        autoRepeat = autoRepeat // Salurkan ke HapticGlassButton
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
```

### File: ./app/src/main/java/com/tvhanan/ui/components/GlassButton.kt
```
package com.tvhanan.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.tvhanan.ui.theme.GlassBorder
import com.tvhanan.ui.theme.GlassBorderStrong
import com.tvhanan.ui.theme.GlassSurface
import com.tvhanan.ui.theme.GlassSurfacePressed
import com.tvhanan.ui.theme.TextPrimary

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    gradientColors: List<Color>? = null,
    borderColor: Color = GlassBorder,
    contentColor: Color = TextPrimary,
    enabled: Boolean = true,
    autoRepeat: Boolean = false,
    onPressedChange: ((Boolean) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        onPressedChange?.invoke(isPressed)
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 70, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "glassButtonScale"
    )

    val backgroundModifier = if (gradientColors != null) {
        Modifier.background(Brush.linearGradient(gradientColors), shape)
    } else {
        Modifier.background(if (isPressed) GlassSurfacePressed else GlassSurface, shape)
    }

    val clickModifier = if (autoRepeat) {
        Modifier.repeatingClickable(
            interactionSource = interactionSource,
            enabled = enabled,
            onClick = onClick
        )
    } else {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
    }

    Box(
        modifier = modifier
            .then(clickModifier)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(backgroundModifier)
            .border(1.dp, if (isPressed) GlassBorderStrong else borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

fun Modifier.repeatingClickable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = this.pointerInput(interactionSource, enabled) {
    if (!enabled) return@pointerInput
    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val press = PressInteraction.Press(down.position)

            launch {
                interactionSource.emit(press)
            }

            val repeatJob = launch {
                onClick()
                delay(300)
                while (true) {
                    onClick()
                    delay(100)
                }
            }

            val up = waitForUpOrCancellation()

            repeatJob.cancel()

            launch {
                if (up != null) {
                    interactionSource.emit(PressInteraction.Release(press))
                } else {
                    interactionSource.emit(PressInteraction.Cancel(press))
                }
            }
        }
    }
}
```

### File: ./app/src/main/java/com/tvhanan/ui/settings/SettingsViewModel.kt
```
package com.tvhanan.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhanan.domain.model.TvDevice
import com.tvhanan.domain.repository.TvRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Status sebuah aksi koneksi (reconnect/scan) yang ditampilkan sbg modal di SettingsScreen. */
sealed interface ConnectionActionState {
    data object Idle : ConnectionActionState
    data object Loading : ConnectionActionState
    data class ReconnectSuccess(val device: TvDevice) : ConnectionActionState
    data class ScanResult(val devices: List<TvDevice>) : ConnectionActionState
    data class Failed(val message: String) : ConnectionActionState
}

/**
 * Preferensi tampilan remote. Nilai [remoteSize] dikonsumsi oleh
 * RemoteScreen lewat [NavGraph] sebagai [scaleFactor], yang
 * mengalikan ukuran dp tombol-tombol remote.
 */
data class RemoteUiPreferences(
    val hapticEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val meshBackgroundEnabled: Boolean = true,
    val remoteSize: RemoteSize = RemoteSize.FIT
)

enum class RemoteSize(val scaleFactor: Float) {
    COMPACT(0.86f),
    FIT(1.0f),
    LARGE(1.14f)
}

class SettingsViewModel(
    private val repository: TvRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val _tvDevice = MutableStateFlow<TvDevice?>(null)
    val tvDevice: StateFlow<TvDevice?> = _tvDevice.asStateFlow()

    private val _isActuallyConnected = MutableStateFlow(false)
    val isActuallyConnected: StateFlow<Boolean> = _isActuallyConnected.asStateFlow()

    private val _uiPreferences = MutableStateFlow(RemoteUiPreferences())
    val uiPreferences: StateFlow<RemoteUiPreferences> = _uiPreferences.asStateFlow()

    private val _actionState = MutableStateFlow<ConnectionActionState>(ConnectionActionState.Idle)
    val actionState: StateFlow<ConnectionActionState> = _actionState.asStateFlow()

    init {
        loadCurrentDevice()
    }

    private fun loadCurrentDevice() {
        viewModelScope.launch {
            val ip = repository.lastIp.first()
            val port = repository.lastPort.first()?.toIntOrNull() ?: 8002
            val mac = repository.macAddress.first()
            if (ip != null) {
                _tvDevice.value = TvDevice(ipAddress = ip, port = port, macAddress = mac)
            }
        }
    }

/** Dipanggil RemoteScreen begitu IP/port/mac aktif diketahui, supaya
     * TvInfoCard di Settings langsung akurat tanpa menunggu DataStore. */
    fun setActiveDevice(ipAddress: String, port: Int, macAddress: String?, token: String? = null, isConnected: Boolean = false) {
        _tvDevice.value = TvDevice(ipAddress = ipAddress, port = port, macAddress = macAddress, token = token)
        _isActuallyConnected.value = isConnected
    }

    fun setHapticEnabled(enabled: Boolean) {
        _uiPreferences.value = _uiPreferences.value.copy(hapticEnabled = enabled)
    }

    fun setKeepScreenOn(enabled: Boolean) {
        _uiPreferences.value = _uiPreferences.value.copy(keepScreenOn = enabled)
    }

    fun setMeshBackgroundEnabled(enabled: Boolean) {
        _uiPreferences.value = _uiPreferences.value.copy(meshBackgroundEnabled = enabled)
    }

    fun setRemoteSize(size: RemoteSize) {
        _uiPreferences.value = _uiPreferences.value.copy(remoteSize = size)
    }

    fun reconnect() {
        val device = _tvDevice.value ?: return
        viewModelScope.launch {
            _actionState.value = ConnectionActionState.Loading
            try {
                val reachable = repository.isHostReachable(device.ipAddress, device.port)
                _actionState.value = if (reachable) {
                    ConnectionActionState.ReconnectSuccess(device)
                } else {
                    ConnectionActionState.Failed("TV tidak merespons di ${device.ipAddress}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reconnect probe failed", e)
                _actionState.value = ConnectionActionState.Failed(e.message ?: "Gagal menghubungkan ulang")
            }
        }
    }

    fun scanForOtherTvs() {
        viewModelScope.launch {
            _actionState.value = ConnectionActionState.Loading
            try {
                val devices = repository.discoverDevices()
                _actionState.value = ConnectionActionState.ScanResult(devices)
            } catch (e: Exception) {
                Log.e(TAG, "Scan failed", e)
                _actionState.value = ConnectionActionState.Failed(e.message ?: "Gagal memindai jaringan")
            }
        }
    }

    fun forgetTv() {
        viewModelScope.launch {
            repository.clearPreferences()
            _tvDevice.value = null
            _actionState.value = ConnectionActionState.Idle
        }
    }

    fun wakeTv() {
        val device = _tvDevice.value ?: return
        val mac = device.macAddress ?: return
        viewModelScope.launch {
            repository.wakeOnLan(mac)
        }
    }

    fun resetActionState() {
        _actionState.value = ConnectionActionState.Idle
    }
}
```

### File: ./app/src/main/java/com/tvhanan/ui/settings/SettingsScreen.kt
```
package com.tvhanan.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.unit.dp
import com.tvhanan.domain.model.TvDevice
import com.tvhanan.ui.components.HapticGlassButton
import com.tvhanan.ui.components.MeshGradientBackground
import com.tvhanan.ui.components.ZoneLabel
import com.tvhanan.ui.theme.ConnectedColor
import com.tvhanan.ui.theme.DisconnectedColor
import com.tvhanan.ui.theme.GlassBorder
import com.tvhanan.ui.theme.GlassSurface
import com.tvhanan.ui.theme.NavAccent
import com.tvhanan.ui.theme.NavAccent2
import com.tvhanan.ui.theme.TextDim
import com.tvhanan.ui.theme.TextFaint
import com.tvhanan.ui.theme.TextPrimary
import com.tvhanan.ui.theme.BgBase

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onManualConnect: (String) -> Unit,
    onForgetAndExitToScan: () -> Unit,
    onExitApp: () -> Unit
) {
    val device by viewModel.tvDevice.collectAsStateWithLifecycle()
    val prefs by viewModel.uiPreferences.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    var showManualDialog by remember { mutableStateOf(false) }
    var showForgetDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showActionDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(BgBase)) {
        // Sinkronisasi efek aurora latar belakang halaman Pengaturan
        if (prefs.meshBackgroundEnabled) {
            MeshGradientBackground(modifier = Modifier.fillMaxSize())
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { SettingsHeaderBar(onBack) }

            item {
                val isConnected by viewModel.isActuallyConnected.collectAsStateWithLifecycle()
                TvInfoCard(device, isConnected)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneLabel("Koneksi", accentColor = NavAccent)
                    SettingsGroup {
                        SettingsRow(
                            title = "Hubungkan ulang TV",
                            description = "Cari ulang & sambungkan ke TV ini",
                            onClick = {
                                pendingAction = "reconnect"
                                showActionDialog = true
                                viewModel.reconnect()
                            }
                        )
                        SettingsRow(
                            title = "Pindai TV lain",
                            description = "Tambahkan TV baru di jaringan",
                            onClick = {
                                pendingAction = "scan"
                                showActionDialog = true
                                viewModel.scanForOtherTvs()
                            }
                        )
                        SettingsRow(
                            title = "Sambungkan manual",
                            description = "Masukkan IP TV secara langsung",
                            onClick = { showManualDialog = true }
                        )
                        if (device?.macAddress != null) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            SettingsRow(
                                title = "Nyalakan TV (WOL)",
                                description = "Bisa butuh beberapa menit setelah TV dimatikan",
                                onClick = {
                                    viewModel.wakeTv()
                                    // Tampilkan pesan melayang (Toast) instan sebagai feedback visual
                                    android.widget.Toast.makeText(
                                        context, 
                                        "Sinyal bangun (WOL) telah dikirim ke TV", 
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                        SettingsRow(
                            title = "Lupakan TV ini",
                            description = "Hapus token & data koneksi tersimpan",
                            isLast = true,
                            onClick = { showForgetDialog = true }
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneLabel("Tampilan & Pengalaman", accentColor = NavAccent2)
                    SettingsGroup {
                        SettingsToggleRow(
                            title = "Getar saat tombol ditekan",
                            description = "Haptic feedback tiap tap",
                            checked = prefs.hapticEnabled,
                            onCheckedChange = viewModel::setHapticEnabled
                        )
                        SettingsToggleRow(
                            title = "Tetap terang di tangan",
                            description = "Cegah layar HP redup saat dipakai",
                            checked = prefs.keepScreenOn,
                            onCheckedChange = viewModel::setKeepScreenOn
                        )
                        SettingsToggleRow(
                            title = "Latar belakang dinamis",
                            description = "Efek aurora di background app",
                            checked = prefs.meshBackgroundEnabled,
                            onCheckedChange = viewModel::setMeshBackgroundEnabled,
                            isLast = true
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneLabel("Ukuran Tampilan Remote", accentColor = TextDim)
                    SizeSelector(
                        current = prefs.remoteSize,
                        onSelect = viewModel::setRemoteSize
                    )
                    Text(
                        text = "Sesuaikan ukuran tombol remote agar pas dengan layar HP kamu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextFaint,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneLabel("Lainnya", accentColor = TextFaint)
                    SettingsGroup {
                        SettingsRow(
                            title = "Tentang aplikasi",
                            description = "Versi, lisensi, dan info build",
                            trailingText = "v1.0.0",
                            onClick = {}
                        )
                        SettingsRow(
                            title = "Keluar dari aplikasi",
                            description = "Tutup remote sepenuhnya",
                            isDanger = true,
                            isLast = true,
                            onClick = { showExitDialog = true }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "TV Remote · versi 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextFaint,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    if (showManualDialog) {
        ManualIpDialog(
            onDismiss = { showManualDialog = false },
            onConfirm = { typedIp ->
                showManualDialog = false
                if (typedIp.isNotBlank()) {
                    onManualConnect(typedIp.trim())
                }
            }
        )
    }

    if (showForgetDialog) {
        ConfirmDialog(
            title = "Lupakan TV ini?",
            description = "Token & data koneksi akan dihapus. Kamu perlu menyetujui ulang permintaan koneksi di TV saat menyambung lagi.",
            confirmLabel = "Lupakan",
            isDanger = true,
            onDismiss = { showForgetDialog = false },
            onConfirm = {
                showForgetDialog = false
                viewModel.forgetTv()
                onForgetAndExitToScan()
            }
        )
    }

    if (showExitDialog) {
        ConfirmDialog(
            title = "Keluar dari aplikasi?",
            description = "Remote akan ditutup sepenuhnya. Koneksi ke TV tetap tersimpan untuk dipakai lagi nanti.",
            confirmLabel = "Keluar",
            isDanger = true,
            onDismiss = { showExitDialog = false },
            onConfirm = {
                showExitDialog = false
                onExitApp()
            }
        )
    }

    if (showActionDialog) {
        ActionProgressDialog(
            actionState = actionState,
            onDismiss = {
                showActionDialog = false
                viewModel.resetActionState()
            }
        )
    }
}

@Composable
private fun SettingsHeaderBar(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HapticGlassButton(
            onClick = onBack,
            modifier = Modifier.size(38.dp),
            shape = CircleShape
        ) {
            Text("\u2190", color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.size(width = 12.dp, height = 1.dp))
        Text("Pengaturan", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
    }
}

@Composable
private fun TvInfoCard(device: TvDevice?, isConnected: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(NavAccent.copy(alpha = 0.10f), NavAccent2.copy(alpha = 0.07f))
                ),
                RoundedCornerShape(22.dp)
            )
            .border(1.dp, NavAccent.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(GlassSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("\uD83D\uDCFA", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.size(width = 14.dp, height = 1.dp))
            Column {
                Text(
                    text = device?.name ?: "Belum ada TV tersambung",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "N-Series · Tizen 5.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
            }
        }

        if (device != null) {
            val statusColor = if (isConnected) ConnectedColor else DisconnectedColor
            val statusLabel = if (isConnected) "Tersambung" else "Tidak tersambung"
            Row(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                Spacer(modifier = Modifier.size(width = 6.dp, height = 1.dp))
                Text(statusLabel, color = statusColor, style = MaterialTheme.typography.bodySmall)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetaItem("Alamat IP", device.ipAddress, Modifier.weight(1f))
                    MetaItem("Port", "${device.port}", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetaItem("Alamat MAC", device.macAddress ?: "Tidak diketahui", Modifier.weight(1f))
                    
                    // Logika tampilan status token yang lebih cerdas
                    val tokenStatus = when {
                        device.token != null -> "Ya"
                        device.port == 8001 -> "Tidak perlu" // TV lawas / port 8001 tidak butuh token
                        else -> "Belum"
                    }
                    MetaItem("Token", tokenStatus, Modifier.weight(1f))
                }
            }
        } else {
            Text(
                text = "Hubungkan ke TV lewat menu Koneksi di bawah.",
                style = MaterialTheme.typography.bodySmall,
                color = TextDim
            )
        }
    }
}

@Composable
private fun MetaItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextFaint)
        Spacer(modifier = Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassSurface, RoundedCornerShape(18.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    trailingText: String? = null,
    isDanger: Boolean = false,
    isLast: Boolean = false
) {
    Column {
        HapticGlassButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            borderColor = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDanger) DisconnectedColor.copy(alpha = 0.9f) else TextPrimary
                    )
                    Text(description, style = MaterialTheme.typography.bodySmall, color = TextFaint)
                }
                if (trailingText != null) {
                    Text(trailingText, style = MaterialTheme.typography.bodyMedium, color = TextDim)
                }
            }
        }
        if (!isLast) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GlassBorder)
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isLast: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextFaint)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NavAccent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = TextFaint
                )
            )
        }
        if (!isLast) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlassBorder))
        }
    }
}

@Composable
private fun SizeSelector(current: RemoteSize, onSelect: (RemoteSize) -> Unit) {
    SettingsGroup {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SizeChip("Kompak", RemoteSize.COMPACT, current, onSelect, Modifier.weight(1f))
            SizeChip("Pas di layar", RemoteSize.FIT, current, onSelect, Modifier.weight(1f))
            SizeChip("Besar", RemoteSize.LARGE, current, onSelect, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SizeChip(
    label: String,
    value: RemoteSize,
    current: RemoteSize,
    onSelect: (RemoteSize) -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = current == value
    HapticGlassButton(
        onClick = { onSelect(value) },
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(999.dp),
        gradientColors = if (isActive) listOf(NavAccent.copy(alpha = 0.22f), NavAccent2.copy(alpha = 0.18f)) else null,
        borderColor = if (isActive) NavAccent.copy(alpha = 0.4f) else GlassBorder
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isActive) TextPrimary else TextDim
        )
    }
}

@Composable
private fun ManualIpDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var ip by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sambungkan manual") },
        text = {
            Column {
                Text(
                    "Masukkan alamat IP TV yang terlihat di Menu > Network > Network Status pada TV.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    placeholder = { Text("192.168.1.42") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            HapticGlassButton(onClick = { onConfirm(ip) }, modifier = Modifier.height(40.dp)) {
                Text("Sambungkan", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            }
        },
        dismissButton = {
            HapticGlassButton(onClick = onDismiss, modifier = Modifier.height(40.dp), borderColor = Color.Transparent) {
                Text("Batal", color = TextDim, style = MaterialTheme.typography.bodyMedium)
            }
        }
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    description: String,
    confirmLabel: String,
    isDanger: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(description, style = MaterialTheme.typography.bodyMedium, color = TextDim) },
        confirmButton = {
            HapticGlassButton(
                onClick = onConfirm,
                modifier = Modifier.height(40.dp),
                gradientColors = if (isDanger) listOf(Color(0xFFFF5A5A), Color(0xFFFF3D7A)) else null
            ) {
                Text(confirmLabel, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        },
        dismissButton = {
            HapticGlassButton(onClick = onDismiss, modifier = Modifier.height(40.dp), borderColor = Color.Transparent) {
                Text("Batal", color = TextDim, style = MaterialTheme.typography.bodyMedium)
            }
        }
    )
}

@Composable
private fun ActionProgressDialog(actionState: ConnectionActionState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (actionState) {
                    is ConnectionActionState.Loading -> "Memproses…"
                    is ConnectionActionState.ReconnectSuccess -> "Berhasil terhubung"
                    is ConnectionActionState.ScanResult -> "${actionState.devices.size} TV ditemukan"
                    is ConnectionActionState.Failed -> "Gagal"
                    ConnectionActionState.Idle -> "Tidak ada aksi"
                }
            )
        },
        text = {
            when (actionState) {
                is ConnectionActionState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = NavAccent)
                        Spacer(modifier = Modifier.size(width = 10.dp, height = 1.dp))
                        Text("Menghubungi TV di jaringan…", color = TextDim, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is ConnectionActionState.ReconnectSuccess -> {
                    Text(
                        "${actionState.device.name} siap dikendalikan.",
                        color = TextDim,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is ConnectionActionState.ScanResult -> {
                    if (actionState.devices.isEmpty()) {
                        Text("Tidak ada TV ditemukan di jaringan ini.", color = TextDim, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Column {
                            actionState.devices.forEach { d ->
                                Text("${d.name} — ${d.ipAddress}", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
                is ConnectionActionState.Failed -> {
                    Text(actionState.message, color = DisconnectedColor, style = MaterialTheme.typography.bodyMedium)
                }
                ConnectionActionState.Idle -> {
                    Text(
                        "Belum ada TV yang tersambung untuk dihubungkan ulang. Coba pindai atau sambungkan manual dahulu.",
                        color = TextDim,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            HapticGlassButton(onClick = onDismiss, modifier = Modifier.height(40.dp)) {
                Text("Tutup", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    )
}
```

### File: ./app/src/main/java/com/tvhanan/ui/navigation/NavGraph.kt
```
package com.tvhanan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvhanan.di.ServiceLocator
import com.tvhanan.ui.settings.SettingsViewModel
import com.tvhanan.domain.model.TvDevice
import com.tvhanan.ui.manual.ManualConnectScreen
import com.tvhanan.ui.remote.RemoteScreen
import com.tvhanan.ui.remote.RemoteViewModel
import com.tvhanan.ui.scan.ScanScreen
import com.tvhanan.ui.scan.ScanViewModel
import com.tvhanan.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.tvhanan.domain.model.ConnectionState

object Routes {
    const val SCAN = "scan"
    const val MANUAL = "manual"
    const val REMOTE = "remote/{ip}/{port}?mac={mac}"
    const val SETTINGS = "settings"

    fun remoteRoute(device: TvDevice) =
        "remote/${device.ipAddress}/${device.port}?mac=${device.macAddress ?: ""}"
    fun remoteRoute(ip: String, port: Int = 8002, mac: String? = null) =
        "remote/$ip/$port?mac=${mac ?: ""}"
}

/**
 * Navigasi grafis utama aplikasi remote TV.
 */
@Composable
fun TvRemoteNavGraph(
    navController: NavHostController,
    serviceLocator: ServiceLocator,
    onExitApp: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(serviceLocator.repository) as T
            }
        }
    )

    val startRoute by androidx.compose.runtime.produceState<String?>(initialValue = null) {
        val savedIp = serviceLocator.repository.lastIp.first()
        value = if (savedIp != null) {
            val savedPort = serviceLocator.repository.lastPort.first()?.toIntOrNull() ?: 8002
            val savedMac = serviceLocator.repository.macAddress.first()
            Routes.remoteRoute(savedIp, savedPort, savedMac)
        } else {
            Routes.SCAN
        }
    }

    if (startRoute == null) return

    NavHost(navController = navController, startDestination = startRoute!!) {
        composable(Routes.SCAN) {
            val viewModel: ScanViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return ScanViewModel(serviceLocator.repository) as T
                    }
                }
            )
            ScanScreen(
                viewModel = viewModel,
                onDeviceSelected = { device ->
                    scope.launch {
                        serviceLocator.repository.saveLastIp(device.ipAddress)
                        serviceLocator.repository.saveLastPort(device.port.toString())
                        device.macAddress?.let { serviceLocator.repository.saveMacAddress(it) }
                        navController.navigate(Routes.remoteRoute(device))
                    }
                },
                onManualConnect = {
                    navController.navigate(Routes.MANUAL)
                }
            )
        }

        composable(Routes.MANUAL) {
            ManualConnectScreen(
                onConnect = { device ->
                    scope.launch {
                        serviceLocator.repository.saveLastIp(device.ipAddress)
                        serviceLocator.repository.saveLastPort(device.port.toString())
                        device.macAddress?.let { serviceLocator.repository.saveMacAddress(it) }
                        navController.navigate(Routes.remoteRoute(device)) {
                            popUpTo(Routes.SCAN)
                        }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.REMOTE,
            arguments = listOf(
                navArgument("ip") { type = NavType.StringType },
                navArgument("port") { type = NavType.IntType; defaultValue = 8002 },
                navArgument("mac") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val ip = backStackEntry.arguments?.getString("ip") ?: return@composable
            val port = backStackEntry.arguments?.getInt("port") ?: 8002
            val mac = backStackEntry.arguments?.getString("mac")?.ifBlank { null }

            val viewModel: RemoteViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return RemoteViewModel(ip, port, mac, serviceLocator.repository) as T
                    }
                }
            )

            val connectionStateForSync by viewModel.connectionState.collectAsStateWithLifecycle()

            androidx.compose.runtime.LaunchedEffect(ip, port, connectionStateForSync) {
                val mac = serviceLocator.repository.macAddress.first()
                val token = serviceLocator.repository.getToken()
                settingsViewModel.setActiveDevice(
                    ipAddress = ip,
                    port = port,
                    macAddress = mac,
                    token = token,
                    isConnected = connectionStateForSync == ConnectionState.CONNECTED
                )
            }

            androidx.compose.runtime.LaunchedEffect(ip, port) {
                viewModel.observeNewToken { newToken ->
                    val mac = serviceLocator.repository.macAddress.first()
                    settingsViewModel.setActiveDevice(
                        ipAddress = ip,
                        port = port,
                        macAddress = mac,
                        token = newToken,
                        isConnected = true
                    )
                }
            }

            val uiPrefs by settingsViewModel.uiPreferences.collectAsStateWithLifecycle()

            RemoteScreen(
                viewModel = viewModel,
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS) {
                        launchSingleTop = true // Mencegah penumpukan layar ganda pada double-tap cepat
                    }
                },
                scaleFactor = uiPrefs.remoteSize.scaleFactor,
                keepScreenOn = uiPrefs.keepScreenOn,
                hapticEnabled = uiPrefs.hapticEnabled,                 // Salurkan status getar
                meshBackgroundEnabled = uiPrefs.meshBackgroundEnabled   // Salurkan status latar belakang aurora
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onManualConnect = { typedIp ->
                    navController.navigate(Routes.remoteRoute(typedIp)) {
                        popUpTo(Routes.SCAN)
                    }
                },
                onForgetAndExitToScan = {
                    navController.navigate(Routes.SCAN) {
                        popUpTo(0)
                    }
                },
                onExitApp = onExitApp
            )
        }
    }
}```

### File: ./app/src/main/java/com/tvhanan/ui/scan/ScanScreen.kt
```
package com.tvhanan.ui.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tvhanan.domain.model.TvDevice
import com.tvhanan.ui.components.HapticGlassButton
import com.tvhanan.ui.components.MeshGradientBackground
import com.tvhanan.ui.theme.DisconnectedColor
import com.tvhanan.ui.theme.GlassBorder
import com.tvhanan.ui.theme.NavAccent
import com.tvhanan.ui.theme.NavAccent2
import com.tvhanan.ui.theme.TextDim
import com.tvhanan.ui.theme.TextFaint
import com.tvhanan.ui.theme.TextPrimary

@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onDeviceSelected: (TvDevice) -> Unit,
    onManualConnect: () -> Unit
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val lastIp by viewModel.lastIp.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        MeshGradientBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            Text(
                text = "Cari TV Samsung",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pastikan HP dan TV terhubung ke Wi-Fi yang sama.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextDim
            )

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HapticGlassButton(
                    onClick = { viewModel.startScan() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    gradientColors = listOf(NavAccent.copy(alpha = 0.20f), NavAccent2.copy(alpha = 0.16f)),
                    borderColor = NavAccent.copy(alpha = 0.4f)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = NavAccent
                        )
                    } else {
                        Text("Cari TV", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                HapticGlassButton(
                    onClick = onManualConnect,
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text("Manual", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (error != null) {
                Text(
                    text = error!!,
                    color = DisconnectedColor,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (lastIp != null && devices.isEmpty() && !isScanning) {
                Text(
                    text = "Terakhir terhubung: $lastIp",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextFaint
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(devices) { device ->
                    TvDeviceCard(
                        device = device,
                        onClick = {
                            viewModel.savePreferredDevice(device)
                            onDeviceSelected(device)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvDeviceCard(
    device: TvDevice,
    onClick: () -> Unit
) {
    HapticGlassButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(NavAccent.copy(alpha = 0.12f), CircleShape)
                        .border(1.dp, NavAccent.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\uD83D\uDCFA", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.size(width = 12.dp, height = 1.dp))
                Column {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = device.ipAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                }
            }

            Text(
                text = "\u2192",
                style = MaterialTheme.typography.titleLarge,
                color = NavAccent
            )
        }
    }
}
```

### File: ./app/src/main/java/com/tvhanan/ui/scan/ScanViewModel.kt
```
package com.tvhanan.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhanan.domain.model.TvDevice
import com.tvhanan.domain.repository.TvRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ScanViewModel(
    private val repository: TvRepository
) : ViewModel() {

    private val _devices = MutableStateFlow<List<TvDevice>>(emptyList())
    val devices: StateFlow<List<TvDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _lastIp = MutableStateFlow<String?>(null)
    val lastIp: StateFlow<String?> = _lastIp.asStateFlow()

    init {
        viewModelScope.launch {
            _lastIp.value = repository.lastIp.first()
        }
    }

    fun startScan() {
        viewModelScope.launch {
            _isScanning.value = true
            _error.value = null

            try {
                val found = repository.discoverDevices()
                _devices.value = found
                if (found.isEmpty()) {
                    _error.value = "TV tidak ditemukan. Coba koneksi manual."
                }
            } catch (e: Exception) {
                _error.value = "Gagal scan: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun savePreferredDevice(device: TvDevice) {
        viewModelScope.launch {
            repository.saveLastIp(device.ipAddress)
            repository.saveLastPort(device.port.toString())
            device.macAddress?.let { repository.saveMacAddress(it) }
        }
    }
}
```

### File: ./app/src/main/java/com/tvhanan/ui/theme/Type.kt
```
package com.tvhanan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Skala tipografi dijaga minim (cuma beberapa ukuran) sesuai prinsip
 * desain awal: hierarki jelas tanpa banyak variasi ukuran.
 *
 * Catatan font: secara default menggunakan font sistem (Roboto) supaya
 * tidak menambah dependency/asset font custom yang bisa menambah risiko
 * build error. Kalau ingin font custom (Space Grotesk utk display,
 * Inter utk body) tinggal taruh file .ttf di res/font/ dan ganti
 * fontFamily di TextStyle masing-masing.
 */
val RemoteTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp
    )
)
```

### File: ./app/src/main/java/com/tvhanan/ui/theme/Theme.kt
```
package com.tvhanan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Dark-mode-only secara sengaja: app ini dipakai sambil menonton TV,
 * biasanya di ruangan gelap. Tidak ada dynamicColor/light theme supaya
 * identitas warna (mesh gradient + aksen teal/oranye) konsisten dan
 * tidak berubah ikut wallpaper user.
 */
private val TvDarkColorScheme = darkColorScheme(
    primary = NavAccent,
    secondary = NavAccent2,
    tertiary = AccentWarn,
    background = BgBase,
    surface = BgBase,
    surfaceVariant = GlassSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextDim,
    error = DisconnectedColor
)

@Composable
fun TvRemoteTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvDarkColorScheme,
        typography = RemoteTypography,
        content = content
    )
}
```

### File: ./app/src/main/java/com/tvhanan/ui/theme/Color.kt
```
package com.tvhanan.ui.theme

import androidx.compose.ui.graphics.Color

// ===== Base =====
val BgBase = Color(0xFF0D0E12)

// ===== Glass surface tokens (pengganti DpadGray/DarkSurface solid) =====
val GlassSurface = Color(0x0FFFFFFF)        // white alpha ~6%
val GlassSurfacePressed = Color(0x24FFFFFF) // white alpha ~14%
val GlassBorder = Color(0x1FFFFFFF)         // white alpha ~12%
val GlassBorderStrong = Color(0x3DFFFFFF)   // white alpha ~24%

// ===== Text =====
val TextPrimary = Color(0xFFF2F3F5)
val TextDim = Color(0x80F2F3F5)   // alpha ~50%
val TextFaint = Color(0x52F2F3F5) // alpha ~32%

// ===== Power gradient (pengganti PowerRed flat) =====
val PowerGradientStart = Color(0xFFFF6B4A)
val PowerGradientEnd = Color(0xFFFF3D7A)

// ===== Navigation / D-pad accent gradient (signature element) =====
val NavAccent = Color(0xFF3DD9C4)  // teal
val NavAccent2 = Color(0xFF2E8FFF) // blue

// ===== Media transport accent =====
val MediaAccent = Color(0xFFC99BFF)
val MediaAccent2 = Color(0xFF6E7BFF)

// ===== Status =====
val ConnectedColor = Color(0xFF3DD9C4)
val ConnectingColor = Color(0xFFFFC857)
val DisconnectedColor = Color(0xFFFF5A5A)

// ===== Smart Hub color keys (representasi warna asli remote fisik) =====
val ColorKeyRed = Color(0xFFFF8A8A)
val ColorKeyGreen = Color(0xFF7CE8A4)
val ColorKeyYellow = Color(0xFFFFD98C)
val ColorKeyBlue = Color(0xFF8FC0FF)

// ===== Mesh gradient blobs — dipakai sekali oleh MeshBackground =====
val MeshBlob1 = Color(0xFF1E2A4A) // biru tua keunguan
val MeshBlob2 = Color(0xFF3A1F3D) // ungu-marun gelap
val MeshBlob3 = Color(0xFF16313A) // teal gelap
val MeshBlob4 = Color(0xFF2E1E46) // ungu tambahan

// ===== Warning/accent angka =====
val AccentWarn = Color(0xFFFFC857)

// ===== App shortcut brand colors (dipakai subtle, alpha rendah) =====
val NetflixRed = Color(0xFFFF6B6B)
val PrimeBlue = Color(0xFF6FD2F2)
val YoutubeRed = Color(0xFFFF8080)
```

### File: ./app/src/main/java/com/tvhanan/ui/manual/ManualConnectScreen.kt
```
package com.tvhanan.ui.manual

import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tvhanan.domain.model.TvDevice
import com.tvhanan.ui.components.HapticGlassButton
import com.tvhanan.ui.components.MeshGradientBackground
import com.tvhanan.ui.theme.DisconnectedColor
import com.tvhanan.ui.theme.NavAccent
import com.tvhanan.ui.theme.NavAccent2
import com.tvhanan.ui.theme.TextDim
import com.tvhanan.ui.theme.TextFaint
import com.tvhanan.ui.theme.TextPrimary

@Composable
fun ManualConnectScreen(
    onConnect: (TvDevice) -> Unit,
    onBack: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current // 1. Deklarasikan
    var ipAddress by remember { mutableStateOf("") }
    var macAddress by remember { mutableStateOf("") }
    var ipError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        MeshGradientBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            Text(
                text = "Koneksi Manual",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Lihat IP TV di Menu > Network > Network Status pada TV.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextDim
            )

            Spacer(modifier = Modifier.height(24.dp))

            val glassFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NavAccent.copy(alpha = 0.5f),
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = NavAccent,
                focusedLabelColor = NavAccent,
                unfocusedLabelColor = TextFaint,
                focusedPlaceholderColor = TextFaint,
                unfocusedPlaceholderColor = TextFaint
            )

            OutlinedTextField(
                value = ipAddress,
                onValueChange = {
                    ipAddress = it
                    ipError = null
                },
                label = { Text("IP Address TV") },
                placeholder = { Text("192.168.1.100") },
                isError = ipError != null,
                supportingText = ipError?.let { msg -> { Text(msg, color = DisconnectedColor) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = glassFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = macAddress,
                onValueChange = { macAddress = it },
                label = { Text("MAC Address (opsional)") },
                placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                singleLine = true,
                colors = glassFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "MAC diperlukan untuk Wake-on-LAN",
                style = MaterialTheme.typography.bodySmall,
                color = TextFaint
            )

            Spacer(modifier = Modifier.height(28.dp))

            HapticGlassButton(
                onClick = {
                    keyboardController?.hide() // 2. Tutup keyboard sebelum eksekusi aksi
                    if (validateIp(ipAddress)) {
                        val cleanMac = macAddress.trim().ifEmpty { null }
                        onConnect(
                            TvDevice(
                                ipAddress = ipAddress.trim(),
                                macAddress = cleanMac
                            )
                        )
                    } else {
                        ipError = "Format IP tidak valid"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                gradientColors = listOf(NavAccent.copy(alpha = 0.22f), NavAccent2.copy(alpha = 0.18f)),
                borderColor = NavAccent.copy(alpha = 0.4f),
                enabled = ipAddress.isNotBlank()
            ) {
                Text("Hubungkan", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            HapticGlassButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Kembali", color = TextDim, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun validateIp(ip: String): Boolean {
    // Gunakan Regex murni agar tidak memblokir Main Thread atau memicu DNS Lookup
    val ipRegex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$".toRegex()
    return ip.trim().matches(ipRegex)
}
```

### File: ./app/src/main/java/com/tvhanan/TvRemoteApp.kt
```
package com.tvhanan

import android.app.Application
import com.tvhanan.di.ServiceLocator
import com.tvhanan.util.HapticUtil

class TvRemoteApp : Application() {

    lateinit var serviceLocator: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        serviceLocator = ServiceLocator(this)
        HapticUtil.init(this)
    }
}
```

### File: ./app/src/main/java/com/tvhanan/di/ServiceLocator.kt
```
package com.tvhanan.di

import android.content.Context
import com.tvhanan.data.local.TvPreferences
import com.tvhanan.data.network.SslTrustManager
import com.tvhanan.data.network.TvDiscoveryService
import com.tvhanan.data.network.TvWebSocketClient
import com.tvhanan.data.repository.TvRepositoryImpl
import com.tvhanan.domain.repository.TvRepository

class ServiceLocator(context: Context) {

    val preferences: TvPreferences by lazy { TvPreferences(context) }
    val discoveryService: TvDiscoveryService by lazy { TvDiscoveryService(context) }
    val sslTrustManager: SslTrustManager by lazy { SslTrustManager(preferences) }
    val webSocketClient: TvWebSocketClient by lazy { TvWebSocketClient(sslTrustManager) }
    val repository: TvRepository by lazy {
        TvRepositoryImpl(discoveryService, webSocketClient, preferences)
    }

}
```

### File: ./app/src/main/java/com/tvhanan/util/CryptoUtil.kt
```
package com.tvhanan.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoUtil {

    private companion object {
        private const val KEY_ALIAS = "tvhanan_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }

    private val secretKey: SecretKey by lazy {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            val keyGenerator = javax.crypto.KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }

    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, secretKey)
        }
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(ciphertext: String): String? {
        return try {
            val decoded = Base64.decode(ciphertext, Base64.NO_WRAP)
            val iv = decoded.copyOfRange(0, 12)
            val encrypted = decoded.copyOfRange(12, decoded.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            }
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
```

### File: ./app/src/main/java/com/tvhanan/util/HapticUtil.kt
```
package com.tvhanan.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticUtil {

    private var vibrator: Vibrator? = null
    private var appContext: Context? = null
    private var toastShown: Boolean = false
    var isEnabled: Boolean = true

    fun init(context: Context) {
        appContext = context.applicationContext
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun tick() {
        val v = vibrator ?: return
        if (!isEnabled) return

        appContext?.let { ctx ->
            val systemHaptic = android.provider.Settings.System.getInt(
                ctx.contentResolver,
                android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED, 1
            )
            if (systemHaptic == 0 && !toastShown) {
                android.widget.Toast.makeText(ctx, "Aktifkan 'Getar saat disentuh' di Pengaturan HP untuk efek getar", android.widget.Toast.LENGTH_LONG).show()
                toastShown = true
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(50, 255)
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(50)
        }
    }
}
```

### File: ./app/src/main/AndroidManifest.xml
```
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:name=".TvRemoteApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.TvHanan"
        android:networkSecurityConfig="@xml/network_security_config">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.TvHanan">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### File: ./app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
```
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

### File: ./app/src/main/res/drawable/ic_launcher_background.xml
```
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#1A1A2E"
        android:pathData="M0,0h108v108H0z" />
</vector>
```

### File: ./app/src/main/res/drawable/ic_launcher_foreground.xml
```
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M44,28h20v52H44z" />
    <path
        android:fillColor="#2196F3"
        android:pathData="M50,32h8v8H50z" />
    <path
        android:fillColor="#2196F3"
        android:pathData="M48,46h12v4H48z" />
    <path
        android:fillColor="#2196F3"
        android:pathData="M48,54h12v4H48z" />
    <path
        android:fillColor="#2196F3"
        android:pathData="M48,62h12v4H48z" />
</vector>
```

### File: ./app/src/main/res/xml/network_security_config.xml
```
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

### File: ./app/src/main/res/values/strings.xml
```
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">TvHanan</string>
</resources>
```

### File: ./app/src/main/res/values/themes.xml
```
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.TvHanan" parent="android:Theme.Material.NoActionBar">
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
    </style>
</resources>
```

### File: ./app/build.gradle.kts
```
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.tvhanan"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tvhanan"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.okhttp)
    implementation(libs.datastore)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.core.ktx)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling.preview)
}
```

### File: ./app/proguard-rules.pro
```
# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Samsung Remote Protocol
-keep class com.tvhanan.data.network.** { *; }
```

### File: ./build.gradle.kts
```
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}
```

### File: ./gradle.properties
```
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

### File: ./gradle/libs.versions.toml
```
[versions]
agp = "8.9.0"
kotlin = "2.0.21"
composeBom = "2024.12.01"
okhttp = "4.12.0"
datastore = "1.1.1"
coroutines = "1.9.0"
coreKtx = "1.15.0"
activityCompose = "1.9.3"
navigationCompose = "2.8.5"
lifecycle = "2.8.7"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
datastore = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

### File: ./settings.gradle.kts
```
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TvHanan"
include(":app")
```

### File: ./.gitignore
```
*.iml
.gradle
/local.properties
/.idea
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
```

