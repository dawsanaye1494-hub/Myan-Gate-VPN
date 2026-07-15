package com.example.vpn

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

enum class VpnProtocol {
    PROTOCOL_A, // UDP only
    PROTOCOL_B, // TCP Port 443 (DPI Bypass)
    PROTOCOL_C, // Smart Mode (TCP Port 443 with UDP fallback)
    PROTOCOL_SSTP // MS-SSTP Protocol (Port 443 SSL Tunnel)
}

class VpnViewModel : ViewModel() {

    private val repository = VpnRepository()

    private val _servers = MutableStateFlow<List<VpnServer>>(emptyList())
    val servers: StateFlow<List<VpnServer>> = _servers.asStateFlow()

    private val _selectedServer = MutableStateFlow<VpnServer?>(null)
    val selectedServer: StateFlow<VpnServer?> = _selectedServer.asStateFlow()

    private val _selectedProtocol = MutableStateFlow(VpnProtocol.PROTOCOL_C)
    val selectedProtocol: StateFlow<VpnProtocol> = _selectedProtocol.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Observe connection state directly from the system VPN Service
    val connectionState: StateFlow<VpnConnectionState> = MyanmarVpnService.connectionState

    // Observe current connected server directly from the system VPN Service
    val activeVpnServer: StateFlow<VpnServer?> = MyanmarVpnService.currentServer

    private var fallbackJob: Job? = null
    private var isFallbackAttempt = false

    init {
        loadServers()
    }

    fun loadServers() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val serverList = repository.fetchServers()
                _servers.value = serverList
                
                // Smart select or update selected server
                if (serverList.isNotEmpty()) {
                    val active = activeVpnServer.value
                    if (active != null) {
                        // If we are currently active/connected, find the active server in the new list and update selected
                        val updatedActive = serverList.find { it.ip == active.ip }
                        if (updatedActive != null) {
                            _selectedServer.value = updatedActive
                        }
                    } else {
                        // If not connected, find if current selected is still in the list, otherwise choose the best one
                        val currentSel = _selectedServer.value
                        val stillExists = if (currentSel != null) serverList.find { it.ip == currentSel.ip } else null
                        if (stillExists != null) {
                            _selectedServer.value = stillExists
                        } else {
                            val best = selectBestServer(serverList, _selectedProtocol.value)
                            _selectedServer.value = best ?: serverList.firstOrNull()
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "အင်တာနက် သို့မဟုတ် API ချိတ်ဆက်မှု အဆင်မပြေပါ။ နောက်တစ်ကြိမ် ကြိုးစားကြည့်ပါ။ (Connection Error)"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun isUdpServer(server: VpnServer): Boolean {
        return try {
            val decoded = android.util.Base64.decode(server.ovpnConfigBase64, android.util.Base64.DEFAULT)
            val config = String(decoded, StandardCharsets.UTF_8).lowercase()
            config.contains("proto udp") || config.contains("udp")
        } catch (e: Exception) {
            false
        }
    }

    fun isTcpServer(server: VpnServer): Boolean {
        return try {
            val decoded = android.util.Base64.decode(server.ovpnConfigBase64, android.util.Base64.DEFAULT)
            val config = String(decoded, StandardCharsets.UTF_8).lowercase()
            config.contains("proto tcp") || config.contains("tcp")
        } catch (e: Exception) {
            false
        }
    }

    // Select the best server using the Score & Speed criteria:
    // Sort by Score descending, then Speed descending. Take the top 5, and randomly pick 1.
    fun selectBestServer(servers: List<VpnServer>, protocol: VpnProtocol): VpnServer? {
        val filtered = when (protocol) {
            VpnProtocol.PROTOCOL_A -> servers.filter { isUdpServer(it) }
            VpnProtocol.PROTOCOL_B -> servers.filter { it.isTcp443 }
            VpnProtocol.PROTOCOL_SSTP -> servers.filter { it.sstpSupported }
            VpnProtocol.PROTOCOL_C -> {
                val tcp443List = servers.filter { it.isTcp443 }
                if (tcp443List.isNotEmpty()) tcp443List else servers.filter { isUdpServer(it) }
            }
        }

        if (filtered.isEmpty()) return null

        // Sort by Score (highest first), then Speed (highest first)
        val sorted = filtered.sortedWith(
            compareByDescending<VpnServer> { it.score }
                .thenByDescending { it.speed }
        )

        // Extract top 5
        val top5 = sorted.take(5)

        // Select a random one from top 5
        return top5.randomOrNull()
    }

    fun setProtocol(protocol: VpnProtocol) {
        _selectedProtocol.value = protocol
        // Reset any existing fallback jobs or states
        fallbackJob?.cancel()
        isFallbackAttempt = false
        
        val best = selectBestServer(_servers.value, protocol)
        if (best != null) {
            _selectedServer.value = best
            _errorMessage.value = null
        } else {
            _selectedServer.value = null
            _errorMessage.value = "ထို Protocol အတွက် Server မရနိုင်သေးပါ"
        }
    }

    fun toggleConnection(context: Context) {
        val currentState = connectionState.value
        if (currentState == VpnConnectionState.CONNECTED || currentState == VpnConnectionState.CONNECTING) {
            disconnect(context)
        } else {
            isFallbackAttempt = false
            connect(context)
        }
    }

    private fun connect(context: Context) {
        val currentProtocol = _selectedProtocol.value
        val allServers = _servers.value

        if (allServers.isEmpty()) {
            _errorMessage.value = "ချိတ်ဆက်ရန် VPN Server မရှိသေးပါ။ ကျေးဇူးပြု၍ စာရင်းကို အရင်ဒေါင်းလုဒ်ဆွဲပါ။"
            return
        }

        // Under Protocol C, we first try TCP port 443 (Protocol B), and on fallback we try UDP (Protocol A)
        val protocolToConnect = if (currentProtocol == VpnProtocol.PROTOCOL_C) {
            if (isFallbackAttempt) VpnProtocol.PROTOCOL_A else VpnProtocol.PROTOCOL_B
        } else {
            currentProtocol
        }

        val targetServer = selectBestServer(allServers, protocolToConnect)
        if (targetServer == null) {
            if (currentProtocol == VpnProtocol.PROTOCOL_C && !isFallbackAttempt) {
                // Immediately fallback if no TCP port 443 servers exist
                isFallbackAttempt = true
                connect(context)
            } else {
                _errorMessage.value = "ထို Protocol အတွက် Server မရနိုင်သေးပါ"
            }
            return
        }

        _selectedServer.value = targetServer
        _errorMessage.value = null
        MyanmarVpnService.startVpn(context, targetServer, protocolToConnect)

        // Monitor connection for fallback if using Protocol C
        if (currentProtocol == VpnProtocol.PROTOCOL_C && !isFallbackAttempt) {
            monitorFallback(context, targetServer)
        }
    }

    private fun monitorFallback(context: Context, tcpServer: VpnServer) {
        fallbackJob?.cancel()
        fallbackJob = viewModelScope.launch {
            // Wait up to 6 seconds to verify if connection succeeds
            var timeoutSeconds = 6
            while (timeoutSeconds > 0) {
                delay(1000)
                val state = connectionState.value
                if (state == VpnConnectionState.CONNECTED) {
                    isFallbackAttempt = false
                    return@launch // Connected successfully!
                }
                if (state == VpnConnectionState.ERROR) {
                    break // Direct connection error
                }
                timeoutSeconds--
            }

            // If we didn't establish connection, trigger UDP fallback
            val state = connectionState.value
            if (state == VpnConnectionState.CONNECTING || state == VpnConnectionState.ERROR) {
                Log.d("VpnViewModel", "TCP 443 Connection timed out or failed. Switching to UDP fallback.")
                
                // Disconnect first
                MyanmarVpnService.stopVpn(context)
                delay(1000)
                
                // Trigger fallback attempt with UDP
                isFallbackAttempt = true
                _errorMessage.value = "TCP 443 ချိတ်ဆက်မှုမအောင်မြင်ပါ။ UDP သို့ အလိုအလျောက် ပြောင်းလဲချိတ်ဆက်နေပါသည်..."
                connect(context)
            }
        }
    }

    fun disconnect(context: Context) {
        fallbackJob?.cancel()
        isFallbackAttempt = false
        MyanmarVpnService.stopVpn(context)
    }

    fun selectAndConnect(context: Context, server: VpnServer) {
        fallbackJob?.cancel()
        isFallbackAttempt = false
        _selectedServer.value = server
        MyanmarVpnService.startVpn(context, server, _selectedProtocol.value)
    }
}
