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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

private val Ink=Color(0xFF101010); private val Board=Color(0xFF1B1B1B)
private val Gold=Color(0xFFFFD629); private val Pink=Color(0xFFFF78B7)
private val Cream=Color(0xFFFFF4D8); private val Red=Color(0xFFE84A58)
private data class Note(val id:Int,val text:String,val x:Float,val y:Float,val color:Long)
private data class Thread(val a:Int,val b:Int)

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{JboardApp()}}}

@Composable private fun JboardApp(){
 val ctx=LocalContext.current;val prefs=remember{ctx.getSharedPreferences("jboard",0)}
 val notes=remember{mutableStateListOf<Note>()};val threads=remember{mutableStateListOf<Thread>()};val history=remember{mutableStateListOf<String>()}
 var nextId by remember{mutableIntStateOf(1)};var selected by remember{mutableIntStateOf(-1)};var editing by remember{mutableIntStateOf(-1)}
 var draft by remember{mutableStateOf("")};var boardName by remember{mutableStateOf("My ideas")};var showHistory by remember{mutableStateOf(false)}
 var attachment by remember{mutableStateOf(prefs.getString("last_attachment","")?: "")};var dirty by remember{mutableStateOf(false)}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){u:Uri?->if(u!=null){try{ctx.contentResolver.takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){};attachment=u.toString();history.add("Attached file");persist(prefs,notes,threads,history,boardName);dirty=false}}
 LaunchedEffect(Unit){try{val root=JSONObject(prefs.getString("board","{}")!!);boardName=root.optString("name","My ideas");val ns=root.optJSONArray("notes")?:JSONArray();for(i in 0 until ns.length()){val n=ns.getJSONObject(i);notes.add(Note(n.getInt("id"),n.optString("text"),n.optDouble("x",20.0).toFloat(),n.optDouble("y",20.0).toFloat(),n.optLong("color",0xFFFFE27A)))};nextId=(notes.maxOfOrNull{it.id}?:0)+1;val ts=root.optJSONArray("threads")?:JSONArray();for(i in 0 until ts.length()){val t=ts.getJSONObject(i);threads.add(Thread(t.getInt("a"),t.getInt("b")))};val hs=root.optJSONArray("history")?:JSONArray();for(i in 0 until hs.length())history.add(hs.getString(i))}catch(_:Exception){}}
 fun save(){history.add("Saved board");persist(prefs,notes,threads,history,boardName);dirty=false}
 MaterialTheme(colorScheme=darkColorScheme(primary=Gold,background=Ink,surface=Color(0xFF242424),onPrimary=Ink,onBackground=Cream,onSurface=Cream)){
 Scaffold(containerColor=Ink,topBar={Row(Modifier.fillMaxWidth().background(Ink).padding(horizontal=16.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){
 Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Gold),contentAlignment=Alignment.Center){Text("J",color=Ink,fontSize=32.sp,fontWeight=FontWeight.Black)}
 Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text("JBOARD",color=Cream,fontSize=20.sp,fontWeight=FontWeight.Black,letterSpacing=1.5.sp);Text("Your ideas, connected.",color=Pink,fontSize=11.sp)}
 TextButton(onClick={showHistory=true}){Text("HISTORY",color=Pink,fontSize=10.sp)}
 }},floatingActionButton={FloatingActionButton(onClick={editing=-2;draft="";selected=-1},containerColor=Gold,contentColor=Ink,shape=RoundedCornerShape(16.dp)){Text("+",fontSize=30.sp)}}){p->
 Column(Modifier.fillMaxSize().padding(p).padding(horizontal=14.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
 OutlinedTextField(boardName,{boardName=it;dirty=true},label={Text("Board name")},singleLine=true,modifier=Modifier.fillMaxWidth(),colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Cream,unfocusedTextColor=Cream,focusedBorderColor=Gold,unfocusedBorderColor=Color.DarkGray,focusedLabelColor=Gold,unfocusedLabelColor=Color.LightGray))
 Row(verticalAlignment=Alignment.CenterVertically){Text("IDEA BOARD",color=Gold,fontWeight=FontWeight.Bold,letterSpacing=2.sp);Spacer(Modifier.weight(1f));Text("${notes.size} NOTES • ${threads.size} THREADS",color=Color.LightGray,fontSize=10.sp)}
 BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Board)){
 val w=maxWidth.value;val h=maxHeight.value
 Canvas(Modifier.fillMaxSize()){val step=30.dp.toPx();var yy=step;while(yy<size.height){var xx=step;while(xx<size.width){drawCircle(Color(0xFF35312B),1.1f,Offset(xx,yy));xx+=step};yy+=step}
 threads.forEach{t->val a=notes.find{it.id==t.a};val b=notes.find{it.id==t.b};if(a!=null&&b!=null){val s=Offset((a.x+65).dp.toPx(),(a.y+42).dp.toPx());val e=Offset((b.x+65).dp.toPx(),(b.y+42).dp.toPx());val path=Path().apply{moveTo(s.x,s.y);cubicTo(s.x+40,s.y-40,e.x-40,e.y+40,e.x,e.y)};drawPath(path,Red,style=androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()));drawCircle(Red,4.dp.toPx(),s);drawCircle(Red,4.dp.toPx(),e)}} 
 notes.forEach{n->Column(Modifier.offset{IntOffset(n.x.roundToInt(),n.y.roundToInt())}.width(140.dp).clip(RoundedCornerShape(12.dp)).background(Color(n.color)).pointerInput(n.id){detectDragGestures(onDragStart={selected=n.id},onDrag={c,d->c.consume();val dx=d.x/ density;val dy=d.y/density;notes.replaceAll{q->if(q.id==n.id)q.copy(x=(q.x+dx).coerceIn(0f,(w-145).coerceAtLeast(0f)),y=(q.y+dy).coerceIn(0f,(h-100).coerceAtLeast(0f)))else q};dirty=true},onDragEnd={history.add("Moved note #${n.id}")}) ){
 Row(Modifier.fillMaxWidth().padding(start=8.dp,end=3.dp,top=3.dp),verticalAlignment=Alignment.CenterVertically){Text("NOTE ${n.id}",color=Ink.copy(alpha=.55f),fontSize=9.sp);Spacer(Modifier.weight(1f));TextButton(onClick={selected=n.id;editing=n.id;draft=n.text},contentPadding=PaddingValues(0.dp)){Text("✎",color=Ink)};TextButton(onClick={notes.removeAll{it.id==n.id};threads.removeAll{it.a==n.id||it.b==n.id};history.add("Deleted note");dirty=true},contentPadding=PaddingValues(0.dp)){Text("×",color=Ink)}}
 Text(n.text.ifBlank{"Tap edit to write"},Modifier.padding(9.dp),color=Ink,fontSize=13.sp,maxLines=6)
 }}
 Text("J",Modifier.align(Alignment.BottomEnd).padding(16.dp),color=Gold.copy(alpha=.24f),fontSize=70.sp,fontWeight=FontWeight.Black)
 Text("🦋",Modifier.align(Alignment.BottomStart).padding(15.dp),color=Pink.copy(alpha=.5f),fontSize=30.sp)
 Text("JBOARD • IDEAS IN MOTION",Modifier.align(Alignment.TopStart).padding(12.dp),color=Pink.copy(alpha=.45f),fontSize=8.sp,fontWeight=FontWeight.Bold)
 }
 if(selected>=0)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("NOTE #$selected SELECTED",color=Pink,fontSize=10.sp);Spacer(Modifier.weight(1f));TextButton(onClick={val other=notes.firstOrNull{it.id!=selected};if(other!=null){threads.add(Thread(selected,other.id));history.add("Connected notes");dirty=true}},enabled=notes.size>1){Text("CONNECT",color=Pink)}}
 if(attachment.isNotBlank())Text("📎 ${Uri.parse(attachment).lastPathSegment?: "Attachment"}",color=Pink,fontSize=10.sp,maxLines=1)
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={picker.launch(arrayOf("*/*"))},modifier=Modifier.weight(1f),colors=ButtonDefaults.outlinedButtonColors(contentColor=Pink)){Text("＋ ATTACH")};Button(onClick={save},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=Pink,contentColor=Ink)){Text("SAVE BOARD",fontWeight=FontWeight.Bold)}}
 Text(if(dirty)"● UNSAVED CHANGES" else "✓ SAVED LOCALLY • PROTOTYPE",color=if(dirty)Gold else Color.Gray,fontSize=9.sp,letterSpacing=1.sp)
 }}
 if(editing!=-1)AlertDialog(onDismissRequest={editing=-1},containerColor=Color(0xFF252525),title={Text(if(editing==-2)"New sticky note" else "Edit note",color=Gold)},text={BasicTextField(draft,{draft=it},Modifier.fillMaxWidth().heightIn(min=100.dp).background(Color(0xFF333333),RoundedCornerShape(8.dp)).padding(12.dp),textStyle=MaterialTheme.typography.bodyLarge.copy(color=Cream),decorationBox={inner->Box{if(draft.isEmpty())Text("Write your thought…",color=Color.Gray);inner()}})},confirmButton={TextButton(onClick={if(editing==-2){notes.add(Note(nextId,draft,22f+(notes.size%2)*150f,32f+(notes.size%4)*100f,when(notes.size%3){0->0xFFFFE27A;1->0xFFFFA8C9;else->0xFFB8E3D0}));history.add("Created note #$nextId");nextId++}else{notes.replaceAll{if(it.id==editing)it.copy(text=draft)else it};history.add("Edited note")};editing=-1;dirty=true}){Text("SAVE",color=Gold)}},dismissButton={TextButton(onClick={editing=-1}){Text("CANCEL",color=Pink)}})
 if(showHistory)AlertDialog(onDismissRequest={showHistory=false},containerColor=Color(0xFF252525),title={Text("Board history",color=Gold)},text={Column(Modifier.heightIn(max=350.dp)){history.takeLast(60).reversed().forEach{Text("• $it",color=Cream,modifier=Modifier.padding(vertical=4.dp))}}},confirmButton={TextButton(onClick={showHistory=false}){Text("DONE",color=Gold)}})
 }}
private fun persist(prefs:android.content.SharedPreferences,notes:List<Note>,threads:List<Thread>,history:List<String>,name:String){val root=JSONObject().put("name",name);val ns=JSONArray();notes.forEach{ns.put(JSONObject().put("id",it.id).put("text",it.text).put("x",it.x.toDouble()).put("y",it.y.toDouble()).put("color",it.color))};root.put("notes",ns);val ts=JSONArray();threads.forEach{ts.put(JSONObject().put("a",it.a).put("b",it.b))};root.put("threads",ts);val hs=JSONArray();history.takeLast(100).forEach{hs.put(it)};root.put("history",hs);prefs.edit().putString("board",root.toString()).apply()}
