package com.gomizplayer.iptv.ui.toast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class ToastMessage(val id: Long, val text: String, val isError: Boolean = false)

object Toasts {
    private val queue = androidx.compose.runtime.mutableStateListOf<ToastMessage>()
    private var counter = 0L

    fun show(text: String, isError: Boolean = false) {
        counter += 1
        queue.add(ToastMessage(counter, text, isError))
        if (queue.size > 3) queue.removeAt(0)
    }

    fun list(): List<ToastMessage> = queue.toList()

    fun dismiss(id: Long) {
        queue.removeAll { it.id == id }
    }
}

@Composable
fun ToastHost(modifier: Modifier = Modifier) {
    val toasts = Toasts.list()
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(toasts.size) {
        while (toasts.isNotEmpty()) {
            delay(2600)
            tick = System.currentTimeMillis()
            toasts.firstOrNull()?.let { Toasts.dismiss(it.id) }
        }
    }

    Column(
        modifier = modifier
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.End
    ) {
        toasts.forEach { toast ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (toast.isError) Color(0xCCFF3D5A) else Color(0xCC12161F))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .background(if (toast.isError) Color.White else Color(0xFF38D39F))
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = toast.text,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
