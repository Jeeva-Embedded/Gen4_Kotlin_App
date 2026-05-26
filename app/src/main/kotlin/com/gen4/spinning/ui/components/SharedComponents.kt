package com.gen4.spinning.ui.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gen4.spinning.ui.theme.SpinColors

@Composable
fun GradientAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(SpinColors.Blue, SpinColors.LightGreen),
                )
            )
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (navigationIcon != null) navigationIcon()
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = if (navigationIcon != null) 4.dp else 12.dp),
            )
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
            Row { actions() }
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (enabled) Brush.horizontalGradient(
                    colors = listOf(SpinColors.Blue, SpinColors.LightGreen),
                ) else Brush.horizontalGradient(listOf(Color.Gray, Color.Gray))
            ),
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatusBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF5F5F5),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = label, fontSize = 12.sp, color = Color.Gray)
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
fun SensorIndicator(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(if (active) SpinColors.LightGreen else Color.Red)
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(text = label, fontSize = 14.sp)
            Text(
                text = if (active) "Open" else "Blocked",
                fontSize = 11.sp,
                color = if (active) SpinColors.LightGreen else Color.Red,
            )
        }
    }
}

@Composable
fun FieldInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String = "",
    numeric: Boolean = true,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = if (suffix.isNotEmpty()) ({ Text(suffix) }) else null,
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        singleLine = true,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun SaveBanner(
    message: String,
    success: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(if (success) SpinColors.LightGreen else Color.Red)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = message, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SettingsToolbar(
    onRead: () -> Unit,
    onAll: (() -> Unit)?,
    onSave: () -> Unit,
    onPid: () -> Unit,
    onLimits: (() -> Unit)?,
    saveEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolbarIconButton(icon = Icons.Default.Refresh, label = "Input",  tint = SpinColors.Blue,       onClick = onRead)
        if (onAll != null) {
            ToolbarIconButton(icon = Icons.Default.History, label = "Default", tint = SpinColors.Blue, onClick = onAll)
        }
        ToolbarIconButton(icon = Icons.Default.Save,    label = "Save",   tint = SpinColors.LightGreen, onClick = onSave, enabled = saveEnabled)
        ToolbarIconButton(icon = Icons.Default.Tune,    label = "PID",    tint = SpinColors.Blue,       onClick = onPid)
        if (onLimits != null) {
            ToolbarIconButton(icon = Icons.Default.Search, label = "Parameters", tint = Color.Gray, onClick = onLimits)
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(icon, contentDescription = label, tint = if (enabled) tint else Color.LightGray)
        }
        Text(text = label, fontSize = 10.sp, color = if (enabled) tint else Color.LightGray)
    }
}

@Composable
fun DisconnectedScreen(
    message: String = "Device disconnected",
    onReconnect: (() -> Unit)? = null,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (onReconnect != null) {
            GradientButton(text = "Reconnect", onClick = onReconnect, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }
        TextButton(onClick = onBack) {
            Text("Back to machine selection", color = SpinColors.Blue)
        }
    }
}
