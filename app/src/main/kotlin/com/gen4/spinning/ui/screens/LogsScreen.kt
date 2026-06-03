package com.gen4.spinning.ui.screens

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.graphics.Brush
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

private val MACHINE_ORDER = listOf("DrawFrame", "BlowCard", "Flyer", "Ring")
private val MACHINE_DISPLAY = mapOf(
    "DrawFrame" to "Draw Frame",
    "BlowCard" to "Blow Card",
    "Flyer" to "Flyer Frame",
    "Ring" to "Ring Doubler",
)

private fun machineKey(file: File): String =
    file.nameWithoutExtension.substringBefore("_").ifEmpty { "Other" }

private fun groupByMachine(files: List<File>): List<Pair<String, List<File>>> {
    val grouped = files.groupBy { machineKey(it) }
        .mapValues { (_, v) -> v.sortedByDescending { it.lastModified() } }
    val result = mutableListOf<Pair<String, List<File>>>()
    for (key in MACHINE_ORDER) grouped[key]?.let { result.add(key to it) }
    for ((key, value) in grouped) if (key !in MACHINE_ORDER) result.add(key to value)
    return result
}

private fun fileDateDisplay(file: File): String = try {
    val parts = file.nameWithoutExtension.split("_")
    // filename format: Machine_yyyyMMdd_HHmmss
    val parsed = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        .parse("${parts[1]}${parts[2]}")
    SimpleDateFormat("dd MMM yyyy  •  HH:mm:ss", Locale.getDefault()).format(parsed!!)
} catch (_: Exception) {
    SimpleDateFormat("dd MMM yyyy  •  HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified()))
}

@OptIn(ExperimentalFoundationApi::class)
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
        if (files.isEmpty()) {
            val saveDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
                ?.absolutePath ?: context.filesDir.absolutePath
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
            val sections = remember(files) { groupByMachine(files) }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                item {
                    Text(
                        text = "Tap file to open  •  ↓ Save to Downloads",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 8.dp),
                    )
                }
                sections.forEach { (key, machineFiles) ->
                    val displayName = MACHINE_DISPLAY[key] ?: key
                    val count = machineFiles.size
                    stickyHeader(key = key) {
                        MachineSectionHeader(
                            name = displayName,
                            count = count,
                        )
                    }
                    items(machineFiles, key = { it.absolutePath }) { file ->
                        LogFileRow(
                            file = file,
                            onOpen = { openFile(context, file) },
                            onShare = { shareFile(context, file) },
                            onDownload = { saveToDownloads(context, file) },
                            onDelete = { deleteTarget = file },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    item(key = "${key}_spacer") { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MachineSectionHeader(name: String, count: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(SpinColors.Blue, SpinColors.LightGreen)
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (count == 1) "1 file" else "$count files",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun LogFileRow(
    file: File,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    val sizeKb = (file.length() / 1024).coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileDateDisplay(file),
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF1A1A1A),
            )
            Text(
                text = "$sizeKb KB",
                fontSize = 12.sp,
                color = Color.Gray,
            )
        }
        IconButton(onClick = onDownload) {
            Icon(Icons.Default.Download, contentDescription = "Save to Downloads", tint = SpinColors.LightGreen)
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Default.Share, contentDescription = "Share", tint = SpinColors.Blue)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
        }
    }
}

private fun openFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "text/csv")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        val fallback = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.ms-excel")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(fallback)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No app found to open CSV files. Install Excel or similar.", Toast.LENGTH_LONG).show()
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

private fun saveToDownloads(context: Context, file: File) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                Toast.makeText(context, "Saved to Downloads: ${file.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to save to Downloads", Toast.LENGTH_SHORT).show()
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.mkdirs()
            val dest = File(downloadsDir, file.name)
            file.copyTo(dest, overwrite = true)
            Toast.makeText(context, "Saved to Downloads: ${file.name}", Toast.LENGTH_SHORT).show()
        }
    } catch (_: Exception) {
        Toast.makeText(context, "Failed to save to Downloads", Toast.LENGTH_SHORT).show()
    }
}
