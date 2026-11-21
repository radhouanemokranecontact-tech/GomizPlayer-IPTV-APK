package com.gomizplayer.iptv.ui.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gomizplayer.iptv.R
import com.gomizplayer.iptv.ui.kit.tvFocusRing
import com.gomizplayer.iptv.ui.theme.LocalGomizColors

/** Reusable yes/no confirmation dialog (Windows .modal style). */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    danger: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalGomizColors.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .width(440.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.panel)
                .border(1.dp, colors.line, RoundedCornerShape(22.dp))
                .padding(24.dp)
        ) {
            Text(
                text = title,
                color = colors.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                color = colors.muted,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss) {
                    Text("No", color = colors.muted, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (danger) Color(0xCCFF3D5A) else Color(0xFFFF3D5A))
                        .tvFocusRing(RoundedCornerShape(10.dp))
                        .clickable(onClick = onConfirm)
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = confirmLabel,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
