package com.jboard.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray

private val Ink = Color(0xFF111111)
private val BoardBlack = Color(0xFF191919)
private val JYellow = Color(0xFFFFD629)
private val JPink = Color(0xFFFF79B8)
private val Paper = Color(0xFFFFF4D6)
private val ThreadRed = Color(0xFFE84A4A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JboardApp() }
    }
}

@Composable
private fun JboardApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("jboard_local", 0) }
    fun readNotes(): List<String> = try {
        val a = JSONArray(prefs.getString("notes", "[]"))
        (0 until a.length()).map { a.getString(it) }
    } catch (_: Exception) { listOf("Welcome to Jboard", "Connect ideas with red threads") }
    var boardName by remember { mutableStateOf(prefs.getString("board", "My first board") ?: "My first board") }
    var noteText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(readNotes()) }
    var selected by remember { mutableStateOf(setOf<Int>()) }
    var history by remember { mutableStateOf(prefs.getString("history", "Board created") ?: "Board created") }
    var attachment by remember { mutableStateOf(prefs.getString("attachment", "") ?: "") }
    var saved by remember { mutableStateOf(true) }
    var editIndex by remember { mutableStateOf<Int?>(null) }
    var editText by remember { mutableStateOf("") }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            attachment = uri.toString()
            prefs.edit().putString("attachment", attachment).apply()
            history = "Attached file: ${uri.lastPathSegment ?: "file"}"
            prefs.edit().putString("history", history).apply()
        }
    }
    fun persist() {
        val arr = JSONArray()
        notes.forEach { arr.put(it) }
        prefs.edit().putString("notes", arr.toString()).putString("board", boardName).apply()
        saved = true
        history = "Saved board"
        prefs.edit().putString("history", history).apply()
    }
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).background(JYellow, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Text("J", color = Ink, fontSize = 31.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Jboard", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                        Text("Your ideas, connected.", color = JPink, fontSize = 12.sp)
                    }
                    Text(if (saved) "SAVED" else "EDITING", color = if (saved) JYellow else JPink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = boardName, onValueChange = { boardName = it; saved = false },
                    label = { Text("Board name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = JYellow, unfocusedBorderColor = Color.DarkGray, focusedLabelColor = JYellow, unfocusedLabelColor = Color.LightGray)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("IDEA BOARD", color = JYellow, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.weight(1f))
                    Text("✦", color = JPink, fontSize = 20.sp)
                    Text("  Jboard • Butterfly", color = Color.LightGray, fontSize = 10.sp)
                }
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth().background(BoardBlack, RoundedCornerShape(18.dp)).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(notes, key = { i, _ -> i }) { index, note ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = index in selected, onCheckedChange = { checked ->
                                selected = if (checked) selected + index else selected - index
                            }, colors = CheckboxDefaults.colors(checkedColor = JPink, uncheckedColor = Color.Gray))
                            if (index in selected || selected.size >= 2) {
                                Text("●", color = ThreadRed, fontSize = 11.sp)
                                Spacer(Modifier.width(5.dp))
                            }
                            Card(Modifier.weight(1f).clickable {
                                editIndex = index; editText = note
                            }, colors = CardDefaults.cardColors(containerColor = if (index % 2 == 0) Paper else JPink), shape = RoundedCornerShape(13.dp)) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(note, color = Ink, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                    Text("✎", color = Ink)
                                    TextButton(onClick = {
                                        notes = notes.filterIndexed { i, _ -> i != index }
                                        selected = emptySet(); saved = false
                                        history = "Deleted note"
                                    }) { Text("×", color = Ink) }
                                }
                            }
                        }
                    }
                    item {
                        if (selected.size == 2) Text("🔴 Thread connected between selected ideas", color = JPink, fontSize = 11.sp)
                    }
                }
                if (attachment.isNotBlank()) Text("📎 Attached: ${Uri.parse(attachment).lastPathSegment ?: "file"}", color = JPink, fontSize = 11.sp)
                OutlinedTextField(value = noteText, onValueChange = { noteText = it }, label = { Text("Write an idea…") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = JPink, unfocusedBorderColor = Color.DarkGray, focusedLabelColor = JPink, unfocusedLabelColor = Color.LightGray))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { if (noteText.isNotBlank()) { notes = notes + noteText.trim(); noteText = ""; saved = false; history = "Added note" } }, colors = ButtonDefaults.buttonColors(containerColor = JYellow, contentColor = Ink), modifier = Modifier.weight(1f)) { Text("+ Note", fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = JPink), modifier = Modifier.weight(1f)) { Text("Attach") }
                    Button(onClick = { persist() }, colors = ButtonDefaults.buttonColors(containerColor = JPink, contentColor = Ink), modifier = Modifier.weight(1f)) { Text("Save") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("History: $history", color = Color.Gray, fontSize = 10.sp)
                    Text("J ✦", color = JPink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text("Local prototype • collaboration and cloud sync not yet available", color = Color.Gray, fontSize = 9.sp)
            }
        }
    }
    if (editIndex != null) {
        AlertDialog(onDismissRequest = { editIndex = null }, title = { Text("Edit idea") }, text = {
            OutlinedTextField(value = editText, onValueChange = { editText = it }, label = { Text("Note") })
        }, confirmButton = {
            TextButton(onClick = { val i = editIndex; if (i != null && i in notes.indices) notes = notes.toMutableList().also { it[i] = editText }; editIndex = null; saved = false; history = "Edited note" }) { Text("Update") }
        }, dismissButton = { TextButton(onClick = { editIndex = null }) { Text("Cancel") } })
    }
}
