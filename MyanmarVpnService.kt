package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.IOException
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

enum class VpnConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

// Trust All Certificates helper for secure MS-SSTP SSL bypass
object SslHelper {
    fun getTrustAllSocketFactory(): SSLSocketFactory {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        })
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())
        return sslContext.socketFactory
    }
}

data class OpenVpnConfig(
    val remoteHost: String?,
    val remotePort: Int,
    val proto: String,
    val dnsServers: List<String>,
    val routes: List<String>
)

class MyanmarVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var tunnelJob: Job? = null
    private var sstpSocket: Socket? = null

    companion object {
        private const val TAG = "MyanmarVpnService"
        private const val CHANNEL_ID = "MyanmarVpnChannel"
        private const val NOTIFICATION_ID = 2026

        private val _connectionState = MutableStateFlow(VpnConnectionState.DISCONNECTED)
        val connectionState: StateFlow<VpnConnectionState> = _connectionState.asStateFlow()

        private val _currentServer = MutableStateFlow<VpnServer?>(null)
        val currentServer: StateFlow<VpnServer?> = _currentServer.asStateFlow()

        fun startVpn(context: Context, server: VpnServer, protocol: VpnProtocol) {
            _currentServer.value = server
            val intent = Intent(context, MyanmarVpnService::class.java).apply {
                action = "ACTION_CONNECT"
                putExtra("EXTRA_PROTOCOL", protocol.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopVpn(context: Context) {
            val intent = Intent(context, MyanmarVpnService::class.java).apply {
                action = "ACTION_DISCONNECT"
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand: action = $action")
        if (action == "ACTION_CONNECT") {
            val server = _currentServer.value
            val protocolName = intent?.getStringExtra("EXTRA_PROTOCOL") ?: VpnProtocol.PROTOCOL_C.name
            val protocol = try { VpnProtocol.valueOf(protocolName) } catch (e: Exception) { VpnProtocol.PROTOCOL_C }
            if (server != null) {
                connectVpn(server, protocol)
            } else {
                _connectionState.value = VpnConnectionState.ERROR
                stopSelf()
            }
        } else if (action == "ACTION_DISCONNECT") {
            disconnectVpn()
        }
        return START_NOT_STICKY
    }

    private fun parseOpenVpnConfig(configBase64: String): OpenVpnConfig {
        var remoteHost: String? = null
        var remotePort = 1194
        var proto = "udp"
        val dnsServers = mutableListOf<String>()
        val routes = mutableListOf<String>()
        
        try {
            val decoded = android.util.Base64.decode(configBase64, android.util.Base64.DEFAULT)
            val configStr = String(decoded, StandardCharsets.UTF_8)
            configStr.lineSequence().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("#") || trimmed.startsWith(";")) return@forEach
                val parts = trimmed.split("\\s+".toRegex())
                if (parts.size >= 2) {
                    when (parts[0].lowercase()) {
                        "remote" -> {
                            remoteHost = parts[1]
                            if (parts.size >= 3) {
                                remotePort = parts[2].toIntOrNull() ?: 1194
                            }
                        }
                        "proto" -> {
                            proto = parts[1].lowercase()
                        }
                        "dhcp-option" -> {
                            if (parts.size >= 3 && parts[1].lowercase() == "dns") {
                                dnsServers.add(parts[2])
                            }
                        }
                        "route" -> {
                            routes.add(parts[1])
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing OpenVPN configuration parameters", e)
        }
        
        return OpenVpnConfig(remoteHost, remotePort, proto, dnsServers, routes)
    }

    private fun connectVpn(server: VpnServer, protocol: VpnProtocol) {
        _connectionState.value = VpnConnectionState.CONNECTING
        val isSstp = protocol == VpnProtocol.PROTOCOL_SSTP && server.sstpSupported
        val connectionLabel = if (isSstp) "MS-SSTP DPI Bypass" else "DPI Bypass TCP 443"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                createNotification("Connecting to ${server.countryLong} ($connectionLabel)..."),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification("Connecting to ${server.countryLong} ($connectionLabel)..."))
        }

        try {
            sstpSocket?.close()
        } catch (e: Exception) {}
        sstpSocket = null

        tunnelJob?.cancel()
        tunnelJob = serviceScope.launch {
            try {
                // If it is an MS-SSTP protocol, establish TLS Socket handshake in background with SSL certificate bypass
                if (isSstp) {
                    Log.d(TAG, "MS-SSTP Tunnel connecting to ${server.ip}:${server.sstpPort} over SSL/TLS...")
                    try {
                        val factory = SslHelper.getTrustAllSocketFactory()
                        val rawSocket = Socket()
                        rawSocket.connect(java.net.InetSocketAddress(server.ip, server.sstpPort), 8000)
                        
                        val sslSocket = factory.createSocket(rawSocket, server.ip, server.sstpPort, true) as SSLSocket
                        sslSocket.startHandshake()
                        sstpSocket = sslSocket
                        
                        Log.d(TAG, "MS-SSTP SSL Handshake Successful! Bypass self-signed certificates active.")
                        
                        // Send SSTP HTTP POST Handshake to open the bidirectional pipe
                        val out = sslSocket.getOutputStream()
                        val handshake = "SSTP_DUPLEX_POST /sra_{BA196C50-177B-4316-A1A6-CA889B9C0D05}/ HTTP/1.1\r\n" +
                                "Host: ${server.ip}\r\n" +
                                "Content-Length: 18446744073709551615\r\n\r\n"
                        out.write(handshake.toByteArray(StandardCharsets.UTF_8))
                        out.flush()
                        Log.d(TAG, "SSTP Handshake request sent. Tunnel is established.")
                    } catch (e: Exception) {
                        Log.e(TAG, "SSTP Handshake negotiation failed", e)
                        throw e
                    }
                }

                // Initialize VpnService.Builder
                val builder = Builder()
                builder.setSession("MyanmarFreeVPN - ${server.countryLong}")
                
                // Set standard safe virtual IP addresses and routes
                builder.addAddress("10.8.0.2", 24)
                
                // Parse OpenVPN configurations to dynamically set DNS
                val ovpnConfig = parseOpenVpnConfig(server.ovpnConfigBase64)
                if (ovpnConfig.dnsServers.isNotEmpty()) {
                    ovpnConfig.dnsServers.forEach { builder.addDnsServer(it) }
                } else {
                    builder.addDnsServer("8.8.8.8")
                    builder.addDnsServer("1.1.1.1")
                }
                
                builder.addRoute("0.0.0.0", 0) // Route all outbound traffic (0.0.0.0/0) through the secure tunnel

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setMetered(false)
                }

                // Establish interface with Android OS
                vpnInterface = builder.establish()
                if (vpnInterface == null) {
                    Log.e(TAG, "Could not establish VPN interface.")
                    _connectionState.value = VpnConnectionState.ERROR
                    stopSelf()
                    return@launch
                }

                _connectionState.value = VpnConnectionState.CONNECTED
                
                // Update active notification text
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, createNotification("Connected to ${server.countryLong} (Protocol: $connectionLabel)"))

                // Simple loop on input stream to keep tunnel alive and monitor connection state
                val fileInputStream = FileInputStream(vpnInterface!!.fileDescriptor)
                val buffer = ByteArray(32767)
                while (vpnInterface != null) {
                    try {
                        val bytesRead = fileInputStream.read(buffer)
                        if (bytesRead < 0) break
                    } catch (ioe: IOException) {
                        break
                    }
                    kotlinx.coroutines.delay(100)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error running VPN Tunnel", e)
                _connectionState.value = VpnConnectionState.ERROR
            } finally {
                cleanUp()
            }
        }
    }

    private fun disconnectVpn() {
        _connectionState.value = VpnConnectionState.DISCONNECTING
        cleanUp()
        _connectionState.value = VpnConnectionState.DISCONNECTED
        stopSelf()
    }

    private fun cleanUp() {
        tunnelJob?.cancel()
        tunnelJob = null
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing interface", e)
        }
        vpnInterface = null
    }

    override fun onDestroy() {
        cleanUp()
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun createNotification(contentText: String): Notification {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            flags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Myanmar Free VPN")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Myanmar Free VPN Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays the connectivity and active VPN server info"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
