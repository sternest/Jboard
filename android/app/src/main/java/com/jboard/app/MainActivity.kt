package com.jboard.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    var boardName by remember { mutableStateOf("My first board") }
    var noteText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(listOf("Welcome to Jboard", "Connect ideas with red threads", "Tap + to add a note")) }
    var saved by remember { mutableStateOf(true) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(46.dp).background(JYellow, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("J", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Jboard", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Your ideas, connected.", color = JPink, fontSize = 12.sp)
                    }
                    Text(if (saved) "Saved" else "Unsaved", color = if (saved) JYellow else JPink, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = boardName,
                    onValueChange = { boardName = it; saved = false },
                    label = { Text("Board name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = JYellow,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedLabelColor = JYellow,
                        unfocusedLabelColor = Color.LightGray
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("BOARD", color = JYellow, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.weight(1f))
                    Text("✦", color = JPink, fontSize = 20.sp)
                    Text("  Butterfly", color = Color.Gray, fontSize = 11.sp)
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .background(BoardBlack, RoundedCornerShape(20.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notes.withIndex().toList(), key = { it.index }) { (index, note) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (index > 0) {
                                Box(Modifier.width(18.dp).height(2.dp).background(ThreadRed))
                                Text("●", color = ThreadRed, fontSize = 10.sp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = if (index % 2 == 0) Paper else JPink),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(note, color = Ink, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                    TextButton(onClick = {
                                        notes = notes.filterIndexed { i, _ -> i != index }
                                        saved = false
                                    }) { Text("×", color = Ink) }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Write an idea…") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = JPink,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedLabelColor = JPink,
                        unfocusedLabelColor = Color.LightGray
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (noteText.isNotBlank()) {
                                notes = notes + noteText.trim()
                                noteText = ""
                                saved = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JYellow, contentColor = Ink),
                        modifier = Modifier.weight(1f)
                    ) { Text("+ Add note", fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { saved = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JPink),
                        modifier = Modifier.weight(1f)
                    ) { Text("Save board") }
                }
                Text(
                    "Prototype • Local board only • Sharing and cloud sync coming later",
                    color = Color.Gray,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
