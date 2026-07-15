package com.example.vpn

import java.io.BufferedReader
import java.io.StringReader

object VpnGateParser {
    
    // A robust CSV line splitter that correctly handles quoted values containing commas
    fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString())
                current.setLength(0)
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    private fun isTcp443Config(configBase64: String): Boolean {
        return try {
            val decoded = android.util.Base64.decode(configBase64, android.util.Base64.DEFAULT)
            val configStr = String(decoded, java.nio.charset.StandardCharsets.UTF_8).lowercase()
            (configStr.contains("proto tcp") || configStr.contains("tcp")) && 
            (configStr.contains("port 443") || configStr.contains(" 443"))
        } catch (e: Exception) {
            false
        }
    }

    fun parse(csvData: String): List<VpnServer> {
        val servers = mutableListOf<VpnServer>()
        val reader = BufferedReader(StringReader(csvData))
        var line: String?
        var headers: List<String>? = null

        while (reader.readLine().also { line = it } != null) {
            val currentLine = line?.trim() ?: continue
            if (currentLine.isEmpty()) continue
            
            // Robust Header Detection: Check if the line contains key VPNGate header names,
            // regardless of whether it starts with '*' or '#' or has no prefix at all.
            if (currentLine.contains("HostName", ignoreCase = true) &&
                currentLine.contains("IP", ignoreCase = true) &&
                currentLine.contains("OpenVPN_ConfigData_Base64", ignoreCase = true)) {
                
                val cleanLine = currentLine.trimStart('*', '#', ' ')
                headers = splitCsvLine(cleanLine).map { it.trim().trim('\"') }
                continue
            }

            // Skip comments or status lines that do not match the headers
            if (currentLine.startsWith("#")) {
                continue
            }

            if (headers == null) {
                continue
            }

            // Split columns safely respecting quotes
            val columns = splitCsvLine(currentLine).map { it.trim().trim('\"') }
            if (columns.size < 3) {
                continue
            }

            try {
                val hostNameIdx = headers.indexOfFirst { it.equals("HostName", ignoreCase = true) }
                val ipIdx = headers.indexOfFirst { it.equals("IP", ignoreCase = true) }
                val countryLongIdx = headers.indexOfFirst { it.equals("CountryLong", ignoreCase = true) }
                val countryShortIdx = headers.indexOfFirst { it.equals("CountryShort", ignoreCase = true) }
                val pingIdx = headers.indexOfFirst { it.equals("Ping", ignoreCase = true) }
                val speedIdx = headers.indexOfFirst { it.equals("Speed", ignoreCase = true) }
                val scoreIdx = headers.indexOfFirst { it.equals("Score", ignoreCase = true) }
                val configIdx = headers.indexOfFirst { it.equals("OpenVPN_ConfigData_Base64", ignoreCase = true) }
                val sstpIdx = headers.indexOfFirst { it.contains("SSTP", ignoreCase = true) || it.contains("MS-SSTP", ignoreCase = true) }

                if (hostNameIdx != -1 && ipIdx != -1 && configIdx != -1) {
                    val hostName = if (hostNameIdx < columns.size) columns[hostNameIdx] else ""
                    val ip = if (ipIdx < columns.size) columns[ipIdx] else ""
                    val countryLong = if (countryLongIdx != -1 && countryLongIdx < columns.size) columns[countryLongIdx] else "Unknown"
                    val countryShort = if (countryShortIdx != -1 && countryShortIdx < columns.size) columns[countryShortIdx] else "UN"
                    val pingStr = if (pingIdx != -1 && pingIdx < columns.size) columns[pingIdx] else "0"
                    val speedStr = if (speedIdx != -1 && speedIdx < columns.size) columns[speedIdx] else "0"
                    val scoreStr = if (scoreIdx != -1 && scoreIdx < columns.size) columns[scoreIdx] else "0"
                    val configBase64 = if (configIdx < columns.size) columns[configIdx] else ""
                    val sstpVal = if (sstpIdx != -1 && sstpIdx < columns.size) columns[sstpIdx] else ""

                    // Only add if we have valid hostname, IP and Base64 config
                    if (hostName.isNotEmpty() && ip.isNotEmpty() && configBase64.isNotEmpty()) {
                        val ping = pingStr.toLongOrNull() ?: 0L
                        val speed = speedStr.toLongOrNull() ?: 0L
                        val score = scoreStr.toLongOrNull() ?: 0L
                        
                        val isTcp443 = isTcp443Config(configBase64)
                        val sstpSupported = (sstpVal.isNotEmpty() && sstpVal != "0" && sstpVal != "-" && sstpVal != "None") || isTcp443
                        val sstpPort = 443

                        servers.add(
                            VpnServer(
                                hostName = hostName,
                                ip = ip,
                                countryLong = countryLong,
                                countryShort = countryShort,
                                ping = ping,
                                speed = speed,
                                ovpnConfigBase64 = configBase64,
                                score = score,
                                sstpSupported = sstpSupported,
                                sstpPort = sstpPort,
                                isTcp443 = isTcp443
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return servers
    }
}
