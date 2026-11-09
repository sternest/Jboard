package com.jboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

private val Ink = Color(0xFF111111)
private val Gold = Color(0xFFFFD329)
private val Pink = Color(0xFFFF7EB6)
private val Paper = Color(0xFFFFF6D9)
private val Cream = Color(0xFFFFFDF5)

class Note(var id:Int, var text:String, x:Float, y:Float, var color:Long) { var x by mutableFloatStateOf(x); var y by mutableFloatStateOf(y) }
data class Thread(val a:Int,val b:Int)

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { JboardApp() } }
}

@Composable
fun JboardApp() {
 val ctx= LocalContext.current
 val prefs= remember { ctx.getSharedPreferences("jboard",0) }
 val notes= remember { mutableStateListOf<Note>() }
 val threads= remember { mutableStateListOf<Thread>() }
 val history= remember { mutableStateListOf<String>() }
 var nextId by remember { mutableIntStateOf(1) }
 var selected by remember { mutableIntStateOf(-1) }
 var editing by remember { mutableIntStateOf(-1) }
 var draft by remember { mutableStateOf("") }
 var showHistory by remember { mutableStateOf(false) }
 var boardName by remember { mutableStateOf("My ideas") }
 var attachment by remember { mutableStateOf(prefs.getString("last_attachment","") ?: "") }
 val pickFile= rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
   if(uri!=null){
     try { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch(_:Exception) {}
     attachment=uri.toString()
     prefs.edit().putString("last_attachment",attachment).apply()
     history.add("Attached file: ${uri.lastPathSegment ?: "file"}")
     saveBoard(prefs,notes,threads,history,boardName)
   }
 }
 LaunchedEffect(Unit) {
   try {
    val root=JSONObject(prefs.getString("board","{}")!!)
    boardName=root.optString("name","My ideas")
    val ns=root.optJSONArray("notes") ?: JSONArray()
    for(i in 0 until ns.length()){val n=ns.getJSONObject(i);notes.add(Note(n.getInt("id"),n.getString("text"),n.getDouble("x").toFloat(),n.getDouble("y").toFloat(),n.optLong("color",0L)))}
    nextId=(notes.maxOfOrNull{it.id} ?: 0)+1
    val ts=root.optJSONArray("threads") ?: JSONArray()
    for(i in 0 until ts.length()){val t=ts.getJSONObject(i);threads.add(Thread(t.getInt("a"),t.getInt("b")))}
    val hs=root.optJSONArray("history") ?: JSONArray()
    for(i in 0 until hs.length()) history.add(hs.getString(i))
   } catch(_:Exception){}
 }
 fun persist(){saveBoard(prefs,notes,threads,history,boardName)}
 MaterialTheme(colorScheme=darkColorScheme(primary=Gold,background=Ink,surface=Color(0xFF222222),onPrimary=Ink,onBackground=Cream,onSurface=Cream)) {
  Scaffold(containerColor=Ink,topBar={Row(Modifier.fillMaxWidth().background(Ink).padding(horizontal=18.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){
   Text("J",color=Gold,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineLarge)
   Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)){Text("JBOARD",color=Cream,fontWeight=FontWeight.Bold);Text(boardName,color=Pink,style=MaterialTheme.typography.labelSmall)}
   TextButton(onClick={showHistory=true}){Text("HISTORY",color=Pink)}
   TextButton(onClick={pickFile.launch(arrayOf("*/*"))}){Text("＋ FILE",color=Gold)}
  }},floatingActionButton={FloatingActionButton(onClick={editing=-2;draft="";selected=-1},containerColor=Gold,contentColor=Ink){Text("+",style=MaterialTheme.typography.headlineMedium)}}) { pad ->
   Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=12.dp)) {
    Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
     Text("IDEA BOARD",color=Gold,fontWeight=FontWeight.Bold)
     Spacer(Modifier.weight(1f))
     Text("${notes.size} NOTES  •  ${threads.size} THREADS",color=Cream,style=MaterialTheme.typography.labelSmall)
    }
    Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF1B1B1B),RoundedCornerShape(18.dp)).border(1.dp,Color(0xFF3A3320),RoundedCornerShape(18.dp))) {
     Canvas(Modifier.fillMaxSize()) {
      // subtle dotted board texture
      val step=32.dp.toPx()
      var yy=step
      while(yy<size.height){var xx=step;while(xx<size.width){drawCircle(Color(0xFF34302A),1.2f,Offset(xx,yy));xx+=step};yy+=step}
      threads.forEach { t ->
       val a=notes.find{it.id==t.a};val b=notes.find{it.id==t.b}
       if(a!=null&&b!=null) { val start=Offset(a.x+75.dp.toPx(),a.y+55.dp.toPx());val end=Offset(b.x+75.dp.toPx(),b.y+55.dp.toPx()); val path=Path().apply{moveTo(start.x,start.y);cubicTo(start.x+45,start.y-45,end.x-45,end.y+45,end.x,end.y)};drawPath(path,Color(0xFFE84A58),style=androidx.compose.ui.graphics.drawscope.Stroke(width=3.dp.toPx())) }
      }
     }
     notes.forEach { n ->
      Column(Modifier.offset { IntOffset(n.x.roundToInt(),n.y.roundToInt()) }.width(150.dp).background(Color(n.color),RoundedCornerShape(8.dp)).border(if(selected==n.id) 2.dp else 0.dp,Gold,RoundedCornerShape(8.dp)).pointerInput(n.id){detectDragGestures(onDragStart={selected=n.id},onDrag={change,drag->change.consume();n.x=(n.x+drag.x).coerceIn(0f,500f);n.y=(n.y+drag.y).coerceIn(0f,900f)},onDragEnd={history.add("Moved note #${n.id}");persist()}) ) {
       Row(Modifier.fillMaxWidth().padding(start=9.dp,end=4.dp,top=5.dp),verticalAlignment=Alignment.CenterVertically){Text("NOTE ${n.id}",color=Ink.copy(alpha=.55f),style=MaterialTheme.typography.labelSmall);Spacer(Modifier.weight(1f));TextButton(onClick={selected=n.id;editing=n.id;draft=n.text},contentPadding=PaddingValues(0.dp)){Text("✎",color=Ink)};TextButton(onClick={notes.remove(n);threads.removeAll{it.a==n.id||it.b==n.id};history.add("Deleted note #${n.id}");persist()},contentPadding=PaddingValues(0.dp)){Text("×",color=Ink)}}
       Text(n.text.ifBlank{"Tap ✎ to write"},Modifier.padding(horizontal=10.dp,vertical=8.dp),color=Ink,fontWeight=FontWeight.Medium,maxLines=6)
      }
     }
     Text("J",Modifier.align(Alignment.BottomEnd).padding(14.dp),color=Gold.copy(alpha=.18f),fontWeight=FontWeight.Black,style=MaterialTheme.typography.displayLarge)
     Text("🦋",Modifier.align(Alignment.BottomStart).padding(14.dp),color=Pink.copy(alpha=.30f),style=MaterialTheme.typography.headlineLarge)
    }
    Row(Modifier.fillMaxWidth().padding(vertical=9.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
     OutlinedButton(onClick={if(selected>=0){val other=notes.firstOrNull{it.id!=selected};if(other!=null){threads.add(Thread(selected,other.id));history.add("Connected note #${selected} → #${other.id}");persist()}}},modifier=Modifier.weight(1f)){Text("CONNECT TO NEXT",color=Pink)}
     Button(onClick={persist();history.add("Saved board");persist()},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Ink)){Text("SAVE BOARD")}
    }
    if(attachment.isNotBlank()) Text("Attachment added • ${Uri.parse(attachment).lastPathSegment ?: "file"}",color=Pink,style=MaterialTheme.typography.labelSmall)
   }
  }
  if(editing!=-1) AlertDialog(onDismissRequest={editing=-1},containerColor=Color(0xFF252525),title={Text(if(editing==-2)"New sticky note" else "Edit note",color=Gold)},text={Column {BasicTextField(value=draft,onValueChange={draft=it},modifier=Modifier.fillMaxWidth().heightIn(min=100.dp).background(Color(0xFF333333),RoundedCornerShape(8.dp)).padding(12.dp),textStyle=MaterialTheme.typography.bodyLarge.copy(color=Cream),decorationBox={inner->Box{if(draft.isEmpty())Text("Write your thought…",color=Color.Gray);inner()}})}},confirmButton={TextButton(onClick={if(editing==-2){notes.add(Note(nextId,draft,24f+(notes.size%3)*165f,30f+(notes.size%5)*125f,when(notes.size%3){0->0xFFFFE27A;1->0xFFFFA8C9;else->0xFFB8E3D0}));history.add("Created note #$nextId");nextId++}else{notes.find{it.id==editing}?.text=draft;history.add("Edited note #$editing")};editing=-1;persist()}){Text("SAVE",color=Gold)}},dismissButton={TextButton(onClick={editing=-1}){Text("CANCEL",color=Pink)}})
  if(showHistory) AlertDialog(onDismissRequest={showHistory=false},containerColor=Color(0xFF252525),title={Text("Board history",color=Gold)},text={LazyColumn(Modifier.heightIn(max=360.dp)){items(history.reversed()){Text("• $it",color=Cream,modifier=Modifier.padding(vertical=5.dp))}}},confirmButton={TextButton(onClick={showHistory=false}){Text("DONE",color=Gold)}})
 }
}

private fun saveBoard(prefs:android.content.SharedPreferences,notes:List<Note>,threads:List<Thread>,history:List<String>,name:String){
 val root=JSONObject().put("name",name);val ns=JSONArray();notes.forEach{ns.put(JSONObject().put("id",it.id).put("text",it.text).put("x",it.x.toDouble()).put("y",it.y.toDouble()).put("color",it.color))};root.put("notes",ns)
 val ts=JSONArray();threads.forEach{ts.put(JSONObject().put("a",it.a).put("b",it.b))};root.put("threads",ts)
 val hs=JSONArray();history.takeLast(100).forEach{hs.put(it)};root.put("history",hs)
 prefs.edit().putString("board",root.toString()).apply()
}
