package com.jboard.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

private val Ink = Color(0xFF111111)
private val BoardBlack = Color(0xFF191919)
private val JYellow = Color(0xFFFFD629)
private val JPink = Color(0xFFFF79B8)
private val Paper = Color(0xFFFFF4D6)
private val ThreadRed = Color(0xFFE84A4A)

private data class BoardNote(val id: Long, val text: String, val x: Float, val y: Float)

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
    fun readNotes(): List<BoardNote> = try {
        val a = JSONArray(prefs.getString("notes_v2", "[]"))
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            BoardNote(o.optLong("id", i.toLong()), o.optString("text"), o.optDouble("x", 12.0).toFloat(), o.optDouble("y", 12.0 + i * 100).toFloat())
        }
    } catch (_: Exception) { listOf(BoardNote(1,"Welcome to Jboard",12f,12f), BoardNote(2,"Drag notes. Select two to connect.",12f,120f)) }
    var boardName by remember { mutableStateOf(prefs.getString("board", "My first board") ?: "My first board") }
    var noteText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(readNotes()) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var history by remember { mutableStateOf(prefs.getString("history", "Board created") ?: "Board created") }
    var attachment by remember { mutableStateOf(prefs.getString("attachment", "") ?: "") }
    var saved by remember { mutableStateOf(true) }
    var editId by remember { mutableStateOf<Long?>(null) }
    var editText by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            attachment = uri.toString()
            prefs.edit().putString("attachment", attachment).putString("history", "Attached file").apply()
            history = "Attached file: ${uri.lastPathSegment ?: "file"}"
        }
    }
    fun persist() {
        val arr = JSONArray()
        notes.forEach { n -> arr.put(JSONObject().put("id",n.id).put("text",n.text).put("x",n.x).put("y",n.y)) }
        prefs.edit().putString("notes_v2", arr.toString()).putString("board",boardName).putString("history",history).apply()
        saved = true; history = "Saved board"
    }
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).background(JYellow,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center) {
                        Text("J",color=Ink,fontSize=31.sp,fontWeight=FontWeight.Black)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Jboard",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Black)
                        Text("Your ideas, connected.",color=JPink,fontSize=12.sp)
                    }
                    Column(horizontalAlignment=Alignment.End) {
                        Text(if(saved) "SAVED" else "EDITING",color=if(saved) JYellow else JPink,fontSize=10.sp,fontWeight=FontWeight.Bold)
                        Text("🦋",fontSize=18.sp)
                    }
                }
                OutlinedTextField(value=boardName,onValueChange={boardName=it;saved=false},label={Text("Board name")},singleLine=true,modifier=Modifier.fillMaxWidth(),
                    colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Color.White,unfocusedTextColor=Color.White,focusedBorderColor=JYellow,unfocusedBorderColor=Color.DarkGray,focusedLabelColor=JYellow,unfocusedLabelColor=Color.LightGray))
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("IDEA BOARD",color=JYellow,fontWeight=FontWeight.Bold,letterSpacing=2.sp)
                    Spacer(Modifier.weight(1f))
                    Text("J ✦ Butterfly",color=JPink,fontSize=11.sp)
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().background(BoardBlack,RoundedCornerShape(18.dp))) {
                    val maxW=maxWidth.value
                    val maxH=maxHeight.value
                    Canvas(Modifier.fillMaxSize()) {
                        val selectedNotes=notes.filter { it.id in selected }
                        if(selectedNotes.size>=2) {
                            val a=selectedNotes[0]; val b=selectedNotes[1]
                            drawLine(ThreadRed,Offset((a.x+65).dp.toPx(),(a.y+45).dp.toPx()),Offset((b.x+65).dp.toPx(),(b.y+45).dp.toPx()),strokeWidth=4.dp.toPx())
                            drawCircle(ThreadRed,6.dp.toPx(),Offset((a.x+65).dp.toPx(),(a.y+45).dp.toPx()))
                            drawCircle(ThreadRed,6.dp.toPx(),Offset((b.x+65).dp.toPx(),(b.y+45).dp.toPx()))
                        }
                    }
                    notes.forEach { note ->
                        val noteColor=if(note.id%2L==0L) JPink else Paper
                        Card(Modifier.offset { IntOffset(note.x.dp.roundToPx(),note.y.dp.roundToPx()) }
                            .width(140.dp).shadow(5.dp,RoundedCornerShape(12.dp))
                            .pointerInput(note.id) {
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    notes=notes.map { n -> if(n.id==note.id) n.copy(x=(n.x+drag.x/ density).coerceIn(0f,(maxW-145).coerceAtLeast(0f)),y=(n.y+drag.y/density).coerceIn(0f,(maxH-100).coerceAtLeast(0f))) else n }
                                    saved=false
                                }
                            },
                            colors=CardDefaults.cardColors(containerColor=noteColor),shape=RoundedCornerShape(12.dp)) {
                            Column(Modifier.padding(9.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment=Alignment.CenterVertically) {
                                    Checkbox(checked=note.id in selected,onCheckedChange={checked->selected=if(checked) selected+note.id else selected-note.id},
                                        colors=CheckboxDefaults.colors(checkedColor=ThreadRed,uncheckedColor=Ink))
                                    Spacer(Modifier.weight(1f))
                                    Text("✎",color=Ink,modifier=Modifier.clickable { editId=note.id; editText=note.text })
                                    TextButton(onClick={notes=notes.filterNot{it.id==note.id};selected=selected-note.id;saved=false;history="Deleted note"}) { Text("×",color=Ink) }
                                }
                                Text(note.text,color=Ink,fontWeight=FontWeight.Medium,fontSize=13.sp)
                            }
                        }
                    }
                    Text("JBOARD  •  🦋",Modifier.align(Alignment.BottomEnd).padding(10.dp),color=JPink,fontSize=10.sp,fontWeight=FontWeight.Bold)
                }
                if(selected.size==2) Text("🔴 Red thread connects selected ideas",color=JPink,fontSize=10.sp)
                if(attachment.isNotBlank()) Text("📎 ${Uri.parse(attachment).lastPathSegment ?: "Attached file"}",color=JPink,fontSize=10.sp)
                OutlinedTextField(value=noteText,onValueChange={noteText=it},label={Text("Write an idea…")},modifier=Modifier.fillMaxWidth(),
                    colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Color.White,unfocusedTextColor=Color.White,focusedBorderColor=JPink,unfocusedBorderColor=Color.DarkGray,focusedLabelColor=JPink,unfocusedLabelColor=Color.LightGray))
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.fillMaxWidth()) {
                    Button(onClick={if(noteText.isNotBlank()){val id=(notes.maxOfOrNull{it.id}?:0L)+1;notes=notes+BoardNote(id,noteText.trim(),12f,12f);noteText="";saved=false;history="Added note"}},colors=ButtonDefaults.buttonColors(containerColor=JYellow,contentColor=Ink),modifier=Modifier.weight(1f)){Text("+ Note")}
                    OutlinedButton(onClick={picker.launch(arrayOf("*/*"))},colors=ButtonDefaults.outlinedButtonColors(contentColor=JPink),modifier=Modifier.weight(1f)){Text("Attach")}
                    Button(onClick={persist()},colors=ButtonDefaults.buttonColors(containerColor=JPink,contentColor=Ink),modifier=Modifier.weight(1f)){Text("Save")}
                }
                Text("History: $history  •  Local prototype",color=Color.Gray,fontSize=10.sp)
            }
        }
    }
    if(editId!=null) AlertDialog(onDismissRequest={editId=null},title={Text("Edit note")},text={OutlinedTextField(value=editText,onValueChange={editText=it})},confirmButton={TextButton(onClick={notes=notes.map{if(it.id==editId)it.copy(text=editText)else it};editId=null;saved=false;history="Edited note"){Text("Update")}},dismissButton={TextButton(onClick={editId=null}){Text("Cancel")}})
}
