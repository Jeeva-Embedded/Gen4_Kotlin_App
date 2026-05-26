package com.gen4.spinning.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.gen4.spinning.core.logging.listLogFiles
import com.gen4.spinning.ui.components.GradientAppBar
import com.gen4.spinning.ui.theme.SpinColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var files by remember { mutableStateOf(listLogFiles(context)) }
    var deleteTarget by remember { mutableStateOf<File?>(null) }

    deleteTarget?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Log?") },
            text = { Text(file.name) },
            confirmButton = {
                TextButton(onClick = {
                    file.delete()
                    files = listLogFiles(context)
                    deleteTarget = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }

    Scaffold(
        topBar = {
            GradientAppBar(
                title = "Log Files",
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
            )
        },
        containerColor = Color.White,
    ) { padding ->
        val saveDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)?.absolutePath
            ?: context.filesDir.absolutePath
        if (files.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No log files yet.\nEnable logging from the Options tab.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Save location:\n$saveDir",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                item {
                    Text(
                        text = "Saved to: $saveDir",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    HorizontalDivider()
                }
                items(files) { file ->
                    LogFileRow(
                        file = file,
                        onShare = { shareFile(context, file) },
                        onDelete = { deleteTarget = file },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun LogFileRow(file: File, onShare: () -> Unit, onDelete: () -> Unit) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val sizeKb = (file.length() / 1024).coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(file.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                text = "${sdf.format(Date(file.lastModified()))}  •  $sizeKb KB",
                fontSize = 12.sp,
                color = Color.Gray,
            )
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Default.Share, contentDescription = "Share", tint = SpinColors.Blue)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
        }
    }
}

private fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Log"))
}
