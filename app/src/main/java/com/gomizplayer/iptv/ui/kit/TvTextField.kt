package com.gomizplayer.iptv.ui.kit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** OutlinedTextField with TV-friendly D-pad traversal: Down/Up leaves the field. */
@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    password: Boolean = false,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        visualTransformation = if (password) androidx.compose.ui.text.input.PasswordVisualTransformation()
            else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedTextColor = androidx.compose.ui.graphics.Color.White,
            unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
            focusedBorderColor = androidx.compose.ui.graphics.Color(0xFFFF3D5A),
            unfocusedBorderColor = androidx.compose.ui.graphics.Color(0x55FFFFFF),
            focusedLabelColor = androidx.compose.ui.graphics.Color(0xFFFF3D5A),
            unfocusedLabelColor = androidx.compose.ui.graphics.Color(0xFF9AA6B8),
            focusedPlaceholderColor = androidx.compose.ui.graphics.Color(0xFF9AA6B8),
            unfocusedPlaceholderColor = androidx.compose.ui.graphics.Color(0xFF6B7686)
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                val down = event.key == Key.DirectionDown
                val up = event.key == Key.DirectionUp
                if ((down || up) && event.type == KeyEventType.KeyDown) {
                    focusManager.moveFocus(if (down) FocusDirection.Down else FocusDirection.Up)
                    true
                } else {
                    false
                }
            }
    )
}
