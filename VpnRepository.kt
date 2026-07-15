package com.example.vpn

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class VpnRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val fallbackServers = listOf(
        VpnServer(
            hostName = "public-vpn-jp.opengw.net",
            ip = "219.100.37.245",
            countryLong = "Tokyo, Japan",
            countryShort = "JP",
            ping = 45,
            speed = 42500000,
            ovpnConfigBase64 = "Y2xpZW50CmRldiB0dW4KcHJvdG8gdWRwCnJlbW90ZSAyMTkuMTAwLjM3LjI0NSAxMTk0Cg==",
            sstpSupported = true,
            sstpPort = 443,
            isTcp443 = true
        ),
        VpnServer(
            hostName = "public-vpn-kr.opengw.net",
            ip = "112.172.54.33",
            countryLong = "Seoul, South Korea",
            countryShort = "KR",
            ping = 62,
            speed = 18200000,
            ovpnConfigBase64 = "Y2xpZW50CmRldiB0dW4KcHJvdG8gdWRwCnJlbW90ZSAxMTIuMTcyLjU0LjMzIDExOTQKCg==",
            sstpSupported = false,
            sstpPort = 443,
            isTcp443 = false
        ),
        VpnServer(
            hostName = "public-vpn-sg.opengw.net",
            ip = "128.199.112.5",
            countryLong = "Jurong, Singapore",
            countryShort = "SG",
            ping = 28,
            speed = 55100000,
            ovpnConfigBase64 = "Y2xpZW50CmRldiB0dW4KcHJvdG8gdWRwCnJlbW90ZSAxMjguMTk5LjExMi41IDExOTQKCg==",
            sstpSupported = true,
            sstpPort = 443,
            isTcp443 = true
        ),
        VpnServer(
            hostName = "public-vpn-us.opengw.net",
            ip = "172.56.21.89",
            countryLong = "California, United States",
            countryShort = "US",
            ping = 120,
            speed = 31400000,
            ovpnConfigBase64 = "Y2xpZW50CmRldiB0dW4KcHJvdG8gdWRwCnJlbW90ZSAxNzIuNTYuMjEuODkgMTE5NAo=",
            sstpSupported = false,
            sstpPort = 443,
            isTcp443 = false
        )
    )

    suspend fun fetchServers(): List<VpnServer> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://vpngate-proxy.dawsanaye-1494.workers.dev/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Android VPN Client")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("VpnRepository", "API server returned error: ${response.code}. Using fallback servers.")
                    return@withContext fallbackServers
                }
                val bodyString = response.body?.string() ?: ""
                if (bodyString.isEmpty()) {
                    Log.w("VpnRepository", "API server returned empty body. Using fallback servers.")
                    return@withContext fallbackServers
                }
                val parsed = VpnGateParser.parse(bodyString)
                if (parsed.isEmpty()) {
                    Log.w("VpnRepository", "No valid servers found in API response. Using fallback servers.")
                    return@withContext fallbackServers
                }
                Log.d("VpnRepository", "Successfully fetched ${parsed.size} VPN servers from API.")
                parsed
            }
        } catch (e: Exception) {
            Log.e("VpnRepository", "Network error or API failure. Using fallback servers.", e)
            fallbackServers
        }
    }
}
