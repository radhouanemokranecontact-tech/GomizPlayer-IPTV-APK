package com.gomizplayer.iptv.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gomizplayer.iptv.core.secrets.PinStore
import com.gomizplayer.iptv.ui.theme.LocalGomizColors

/**
 * Lock screen shown when app lock is enabled. Returns via [onUnlocked] once the
 * correct PIN is entered (or biometric succeeds).
 */
@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalGomizColors.current
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val pinLength = 4

    fun submit() {
        if (entered.length < pinLength) return
        if (PinStore.verify(context, entered)) {
            onUnlocked()
        } else {
            error = "Incorrect PIN"
            entered = ""
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GomizPlayer", color = colors.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text("Enter PIN to unlock", color = colors.muted, fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(pinLength) { i ->
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (i < entered.length) colors.accent else colors.panel2)
                            .border(1.dp, colors.line, RoundedCornerShape(8.dp))
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            error?.let {
                Text(it, color = Color(0xFFFF8A8A), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Number pad
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                keys.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { k ->
                            if (k.isEmpty()) {
                                Spacer(Modifier.size(72.dp))
                            } else {
                                Box(
                                    Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.panel)
                                        .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                                        .clickable {
                                            when (k) {
                                                "⌫" -> if (entered.isNotEmpty()) entered = entered.dropLast(1)
                                                else -> if (entered.length < pinLength) {
                                                    entered += k
                                                    if (entered.length == pinLength) submit()
                                                }
                                            }
                                            error = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(k, color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
