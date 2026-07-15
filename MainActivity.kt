package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.ActiveBlueBorder
import com.example.ui.theme.ActiveBlueGlass
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.CyberRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlowColor
import com.example.ui.theme.GridLineColor
import com.example.ui.theme.LightText
import com.example.ui.theme.MutedText
import com.example.ui.theme.MyApplicationTheme
import com.example.vpn.VpnConnectionState
import com.example.vpn.VpnProtocol
import com.example.vpn.VpnServer
import com.example.vpn.VpnViewModel
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

class VpnContextWrapper(private val base: Context) : android.content.ContextWrapper(base) {
    override fun getPackageName(): String {
        val pm = base.packageManager
        val packages = pm.getPackagesForUid(android.os.Process.myUid())
        if (packages != null && packages.isNotEmpty()) {
            val target = "com.aistudio.myanmarfreevpn.wqtkyz"
            for (p in packages) {
                if (p == target) {
                    return target
                }
            }
            return packages[0]
        }
        return super.getPackageName()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                    ) {
                        // Ambient Blue/Indigo Glowing orbs in background
                        AtmosphericGlowBackground()
                        
                        // Cyber layout grid lines
                        CyberGridBackground()
                        
                        VpnDashboardScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun AtmosphericGlowBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Glowing Orb Center-Left
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.CenterStart)
                .blur(90.dp)
                .background(GlowColor, shape = CircleShape)
        )
        // Glowing Orb Bottom-Right
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .blur(80.dp)
                .background(GlowColor.copy(alpha = 0.15f), shape = CircleShape)
        )
    }
}

@Composable
fun CyberGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val step = 90f // grid lines distance

        // Vertical grid lines
        var x = 0f
        while (x < width) {
            drawLine(
                color = GridLineColor.copy(alpha = 0.12f),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
            x += step
        }

        // Horizontal grid lines
        var y = 0f
        while (y < height) {
            drawLine(
                color = GridLineColor.copy(alpha = 0.12f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            y += step
        }
    }
}

@Composable
fun VpnDashboardScreen(viewModel: VpnViewModel = viewModel()) {
    val context = LocalContext.current
    val servers by viewModel.servers.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val activeVpnServer by viewModel.activeVpnServer.collectAsState()
    val selectedProtocol by viewModel.selectedProtocol.collectAsState()
    var showServerListMenu by remember { mutableStateOf(false) }

    // Setup VPN Permission Launcher using modern Android APIs
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.toggleConnection(context)
        } else {
            Toast.makeText(context, "VPN Permission was denied. Connection cancelled.", Toast.LENGTH_LONG).show()
        }
    }

    if (showServerListMenu) {
        ServerListMenuScreen(
            servers = servers,
            selectedServer = selectedServer,
            activeServer = activeVpnServer,
            selectedProtocol = selectedProtocol,
            viewModel = viewModel,
            onBack = { showServerListMenu = false },
            onSelectServer = { server ->
                viewModel.selectAndConnect(context, server)
                showServerListMenu = false
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // App Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header brand logo icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1A3B82F6))
                        .border(1.dp, Color(0x4D3B82F6), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(2.dp, Color(0xFF60A5FA), CircleShape)
                    )
                }
                Column {
                    Text(
                        text = "M-GATE VPN",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "မြန်မာနိုင်ငံအတွက် အခမဲ့ ဝန်ဆောင်မှု (Free Service)",
                        color = CyberGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            
            // Reload Server List Button (Frosted Style)
            IconButton(
                onClick = { if (!isLoading) viewModel.loadServers() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurface)
                    .border(1.dp, GlassBorder, CircleShape)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = CyberCyan,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload Servers",
                        tint = CyberCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Error Display
        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberRed.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Error",
                        tint = CyberRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Circular Connection Trigger & Pulsing Animations
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(250.dp)
                .padding(12.dp)
        ) {
            // Infinite wave animation when connecting or connected
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = if (connectionState == VpnConnectionState.CONNECTED || connectionState == VpnConnectionState.CONNECTING) 1.25f else 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = if (connectionState == VpnConnectionState.CONNECTED || connectionState == VpnConnectionState.CONNECTING) 0.05f else 0.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            val colorAccent by animateColorAsState(
                targetValue = when (connectionState) {
                    VpnConnectionState.CONNECTED -> CyberGreen
                    VpnConnectionState.CONNECTING -> CyberOrange
                    VpnConnectionState.DISCONNECTING -> CyberOrange
                    VpnConnectionState.ERROR -> CyberRed
                    VpnConnectionState.DISCONNECTED -> CyberCyan
                },
                label = "color"
            )

            // Outer Pulsating Circle Wave
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(colorAccent.copy(alpha = pulseAlpha))
            )

            // Outer ring
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .border(1.dp, colorAccent.copy(alpha = 0.3f), CircleShape)
            )

            // Primary Inner Interactive Button (Blue-to-Indigo Gradient for Connect / Dynamic Glass for Disconnect)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(136.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = if (connectionState == VpnConnectionState.CONNECTED) {
                                listOf(CyberGreen, Color(0xFF047857))
                            } else {
                                listOf(Color(0xFF3B82F6), Color(0xFF4F46E5))
                            }
                        )
                    )
                    .border(4.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .clickable {
                        // Check Vpn permissions before attempting to connect
                        val vpnIntent = VpnService.prepare(VpnContextWrapper(context))
                        if (vpnIntent != null) {
                            vpnPermissionLauncher.launch(vpnIntent)
                        } else {
                            viewModel.toggleConnection(context)
                        }
                    }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = when (connectionState) {
                            VpnConnectionState.CONNECTED -> Icons.Default.Lock
                            else -> Icons.Default.LockOpen
                        },
                        contentDescription = "Lock",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (connectionState) {
                            VpnConnectionState.CONNECTED -> "CONNECTED"
                            VpnConnectionState.CONNECTING -> "CONNECTING"
                            VpnConnectionState.DISCONNECTING -> "CLOSING"
                            VpnConnectionState.ERROR -> "RETRY"
                            VpnConnectionState.DISCONNECTED -> "CONNECT"
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Connection State Label
        Text(
            text = when (connectionState) {
                VpnConnectionState.CONNECTED -> "လုံခြုံစွာ ချိတ်ဆက်ထားပြီးပါပြီ (Protected)"
                VpnConnectionState.CONNECTING -> "VPN လိုင်း ချိတ်ဆက်နေပါသည်..."
                VpnConnectionState.DISCONNECTING -> "ချိတ်ဆက်မှုကို ဖြတ်တောက်နေပါသည်..."
                VpnConnectionState.ERROR -> "ချိတ်ဆက်မှု အမှားအယွင်းရှိပါသည်။ ပြန်ကြိုးစားပါ။"
                VpnConnectionState.DISCONNECTED -> "ချိတ်ဆက်မှု မရှိသေးပါ။ လိုင်းဖွင့်ရန် ခလုတ်ကို နှိပ်ပါ။"
            },
            color = when (connectionState) {
                VpnConnectionState.CONNECTED -> CyberGreen
                VpnConnectionState.CONNECTING -> CyberOrange
                VpnConnectionState.ERROR -> CyberRed
                else -> Color.White.copy(alpha = 0.8f)
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Protocol Selection Panel (Segmented Control)
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "SELECT PROTOCOL (ပရိုတိုကော ရွေးချယ်ရန်)",
                    color = MutedText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val protocols = listOf(
                        Triple(VpnProtocol.PROTOCOL_C, "Smart Auto", "Bypass (TCP+UDP)"),
                        Triple(VpnProtocol.PROTOCOL_B, "TCP Mode", "Port 443 (Firewall)"),
                        Triple(VpnProtocol.PROTOCOL_SSTP, "MS-SSTP", "SSL Tunnel (DPI)"),
                        Triple(VpnProtocol.PROTOCOL_A, "UDP Mode", "UDP (Fast)")
                    )

                    protocols.forEach { (proto, title, subtitle) ->
                        val isSelected = selectedProtocol == proto
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF60A5FA).copy(alpha = 0.4f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.setProtocol(proto) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) Color.White else LightText,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                                Text(
                                    text = subtitle,
                                    color = if (isSelected) CyberCyan else MutedText,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected / Connected Server Details Panel (Styled as Frosted glass)
        val displayServer = activeVpnServer ?: selectedServer
        if (displayServer != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showServerListMenu = true }
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = getFlagEmoji(displayServer.countryShort),
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayServer.countryLong,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "IP: ${displayServer.ip}",
                                color = MutedText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Change Location",
                            tint = LightText,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Latency/Ping Metric
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SignalCellularAlt,
                                contentDescription = "Ping",
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = "LATENCY", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (displayServer.ping > 0) "${displayServer.ping} ms" else "Excellent",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Speed Metric
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed",
                                tint = CyberGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = "SPEED", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = formatSpeed(displayServer.speed),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Protocol Metric
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security",
                                tint = CyberOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = "PROTOCOL", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                val protoText = if (viewModel.isUdpServer(displayServer)) "OpenVPN (UDP)" else "OpenVPN (TCP)"
                                Text(text = protoText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showServerListMenu = true }
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌐",
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ဆာဗာ ရွေးချယ်ရန်",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap to choose a server location",
                            color = MutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Select Server",
                        tint = LightText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Server List Panel (Frosted Glass Effect: bg-white/5 backdrop-blur border-white/10)
        AnimatedVisibility(
            visible = connectionState == VpnConnectionState.CONNECTED && servers.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.weight(1f)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recommended Servers",
                            color = LightText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0x1A3B82F6), RoundedCornerShape(100.dp))
                                .border(1.dp, Color(0x4D3B82F6), RoundedCornerShape(100.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "LOW LATENCY",
                                color = Color(0xFF60A5FA),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredServers = remember(servers, selectedProtocol, displayServer) {
                        val rawList = when (selectedProtocol) {
                            VpnProtocol.PROTOCOL_A -> servers.filter { viewModel.isUdpServer(it) }
                            VpnProtocol.PROTOCOL_B -> servers.filter { it.isTcp443 }
                            VpnProtocol.PROTOCOL_SSTP -> servers.filter { it.sstpSupported }
                            VpnProtocol.PROTOCOL_C -> servers
                        }
                        rawList.filter { it.ip != displayServer?.ip }
                    }

                    if (filteredServers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "No Servers",
                                    tint = CyberOrange,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "ထို Protocol အတွက် Server မရနိုင်သေးပါ",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredServers) { server ->
                                ServerItemRow(
                                    server = server,
                                    onConnectClick = {
                                        viewModel.selectAndConnect(context, server)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // If loading servers on first open
        if (isLoading && servers.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
            CircularProgressIndicator(color = CyberCyan)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "ဆာဗာစာရင်းများကို ရယူနေပါသည်...", color = MutedText, fontSize = 12.sp)
            Spacer(modifier = Modifier.weight(1f))
        }
    } }
}

@Composable
fun ServerListMenuScreen(
    servers: List<VpnServer>,
    selectedServer: VpnServer?,
    activeServer: VpnServer?,
    selectedProtocol: VpnProtocol,
    viewModel: VpnViewModel,
    onBack: () -> Unit,
    onSelectServer: (VpnServer) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedCountries by remember { mutableStateOf(setOf<String>()) }

    val filteredServers = remember(servers, selectedProtocol, searchQuery) {
        val protocolFiltered = when (selectedProtocol) {
            VpnProtocol.PROTOCOL_A -> servers.filter { viewModel.isUdpServer(it) }
            VpnProtocol.PROTOCOL_B -> servers.filter { it.isTcp443 }
            VpnProtocol.PROTOCOL_SSTP -> servers.filter { it.sstpSupported }
            VpnProtocol.PROTOCOL_C -> servers
        }
        if (searchQuery.isBlank()) {
            protocolFiltered
        } else {
            protocolFiltered.filter {
                it.countryLong.contains(searchQuery, ignoreCase = true) ||
                it.countryShort.contains(searchQuery, ignoreCase = true) ||
                it.ip.contains(searchQuery)
            }
        }
    }

    // Auto expand search matches
    val autoExpanded = remember(searchQuery, filteredServers) {
        if (searchQuery.isNotBlank()) {
            filteredServers.map { it.countryLong }.toSet()
        } else {
            emptySet()
        }
    }
    val activeExpanded = expandedCountries + autoExpanded

    // Sort by score desc, speed desc, ping asc
    val recommendedServers = remember(servers, selectedProtocol) {
        val protocolFiltered = when (selectedProtocol) {
            VpnProtocol.PROTOCOL_A -> servers.filter { viewModel.isUdpServer(it) }
            VpnProtocol.PROTOCOL_B -> servers.filter { it.isTcp443 }
            VpnProtocol.PROTOCOL_SSTP -> servers.filter { it.sstpSupported }
            VpnProtocol.PROTOCOL_C -> servers
        }
        protocolFiltered.sortedWith(
            compareByDescending<VpnServer> { it.score }
                .thenByDescending { it.speed }
                .thenBy { if (it.ping > 0) it.ping else Long.MAX_VALUE }
        ).take(3)
    }

    val groupedServers = remember(filteredServers) {
        filteredServers.groupBy { it.countryLong }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurface)
                    .border(1.dp, GlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SELECT LOCATION",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "နိုင်ငံနှင့် ဆာဗာရွေးချယ်ရန်",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Refresh Button in Location Screen
            val isLoading by viewModel.isLoading.collectAsState()
            IconButton(
                onClick = { if (!isLoading) viewModel.loadServers() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurface)
                    .border(1.dp, GlassBorder, CircleShape)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = CyberCyan,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload Servers",
                        tint = CyberCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Location (နိုင်ငံအမည် ရှာရန်...)", color = MutedText, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = LightText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                disabledContainerColor = DarkSurface,
                focusedIndicatorColor = CyberCyan,
                unfocusedIndicatorColor = GlassBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recommended Section (Show only when searchQuery is empty)
            if (searchQuery.isBlank() && recommendedServers.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECOMMENDED (အကြံပြုထားသော)",
                                color = LightText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .background(CyberGreen.copy(alpha = 0.1f), RoundedCornerShape(100.dp))
                                    .border(1.dp, CyberGreen.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "BEST SPEED",
                                    color = CyberGreen,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recommendedServers.forEach { server ->
                                val isConnected = activeServer?.ip == server.ip
                                RecommendedServerRow(
                                    server = server,
                                    isConnected = isConnected,
                                    onClick = { onSelectServer(server) }
                                )
                            }
                        }
                    }
                }
            }

            // All Locations / Grouped Expandable Accordion List
            item {
                Text(
                    text = "ALL LOCATIONS (ဆာဗာအားလုံး)",
                    color = LightText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (groupedServers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "No results",
                                tint = CyberOrange,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ရှာဖွေထားသော နိုင်ငံ မတွေ့ရှိပါ",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                groupedServers.forEach { (countryName, serversInCountry) ->
                    val firstServer = serversInCountry.firstOrNull()
                    val countryShort = firstServer?.countryShort ?: ""
                    val isExpanded = activeExpanded.contains(countryName)

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (isExpanded) CyberCyan.copy(alpha = 0.3f) else GlassBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                // Accordion Header Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedCountries = if (isExpanded) {
                                                expandedCountries - countryName
                                            } else {
                                                expandedCountries + countryName
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = getFlagEmoji(countryShort),
                                            fontSize = 24.sp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "$countryName (${serversInCountry.size})",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = if (isExpanded) CyberCyan else LightText
                                    )
                                }

                                // Expanded Server List
                                if (isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0x06FFFFFF))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        serversInCountry.forEach { server ->
                                            val isConnected = activeServer?.ip == server.ip
                                            ServerRowItemDetail(
                                                server = server,
                                                isConnected = isConnected,
                                                onClick = { onSelectServer(server) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendedServerRow(
    server: VpnServer,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    val pingColor = when {
        server.ping <= 0 -> CyberGreen
        server.ping < 80 -> CyberGreen
        server.ping < 160 -> CyberCyan
        server.ping < 260 -> CyberOrange
        else -> CyberRed
    }

    val pingText = if (server.ping > 0) "${server.ping} ms" else "Fast"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0x1A10B981) else Color(0x08FFFFFF)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isConnected) CyberGreen.copy(alpha = 0.5f) else Color(0x0FFFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = getFlagEmoji(server.countryShort),
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = server.countryLong,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isConnected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(CyberGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(1.dp, CyberGreen.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "CONNECTED",
                                    color = CyberGreen,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "IP: ${server.ip} • Speed: ${formatSpeed(server.speed)}",
                        color = MutedText,
                        fontSize = 10.sp
                    )
                }
            }

            // Latency indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(pingColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .border(1.dp, pingColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SignalCellularAlt,
                    contentDescription = "Signal",
                    tint = pingColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = pingText,
                    color = pingColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ServerRowItemDetail(
    server: VpnServer,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    val pingColor = when {
        server.ping <= 0 -> CyberGreen
        server.ping < 80 -> CyberGreen
        server.ping < 160 -> CyberCyan
        server.ping < 260 -> CyberOrange
        else -> CyberRed
    }

    val pingText = if (server.ping > 0) "${server.ping} ms" else "Fast"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0x1A10B981) else Color(0x05FFFFFF)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isConnected) CyberGreen.copy(alpha = 0.4f) else Color(0x0AFFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "IP: ${server.ip}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isConnected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[CONNECTED]",
                            color = CyberGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Speed: ${formatSpeed(server.speed)}",
                        color = CyberCyan.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(text = "•", color = MutedText, fontSize = 10.sp)
                    Text(
                        text = "Score: ${server.score}",
                        color = MutedText,
                        fontSize = 10.sp
                    )
                }
            }

            // Connection Quality Wifi-style Signal Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(pingColor.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                    .border(1.dp, pingColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SignalCellularAlt,
                    contentDescription = "Signal Quality",
                    tint = pingColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = pingText,
                    color = pingColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ServerItemRow(server: VpnServer, onConnectClick: () -> Unit) {
    // Color code according to ping latency in ms
    val pingColor = when {
        server.ping <= 0 -> CyberGreen
        server.ping < 80 -> CyberGreen
        server.ping < 160 -> CyberCyan
        server.ping < 260 -> CyberOrange
        else -> CyberRed
    }

    val pingText = if (server.ping > 0) "${server.ping} ms" else "Fast"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x08FFFFFF)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0x0FFFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnectClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = getFlagEmoji(server.countryShort),
                    fontSize = 26.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = server.countryLong,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Proxy Gate",
                            color = MutedText,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "•",
                            color = MutedText,
                            fontSize = 10.sp
                        )
                        Text(
                            text = formatSpeed(server.speed),
                            color = CyberCyan.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Latency / connection quality indicator badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(pingColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .border(1.dp, pingColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SignalCellularAlt,
                    contentDescription = "Latency",
                    tint = pingColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = pingText,
                    color = pingColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Utility to translate country short code (like "US") to unicode regional indicator flag emoji
fun getFlagEmoji(countryShort: String): String {
    val cleanCode = countryShort.uppercase(Locale.ROOT).trim()
    if (cleanCode.length != 2) return "🌐"
    try {
        val firstChar = Character.codePointAt(cleanCode, 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(cleanCode, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    } catch (e: Exception) {
        return "🌐"
    }
}

// Format speeds nicely
fun formatSpeed(speedBytes: Long): String {
    if (speedBytes <= 0) return "High-Speed"
    val mbps = speedBytes.toDouble() / 1_000_000.0
    return if (mbps < 1.0) {
        val kbps = speedBytes.toDouble() / 1_000.0
        String.format(Locale.ROOT, "%.1f Kbps", kbps)
    } else {
        String.format(Locale.ROOT, "%.1f Mbps", mbps)
    }
}
