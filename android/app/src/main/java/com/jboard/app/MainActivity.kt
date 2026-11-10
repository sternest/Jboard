package com.jboard.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

private val Ink=Color(0xFF101010)
private val Board=Color(0xFF1B1B1B)
private val Gold=Color(0xFFFFD629)
private val Pink=Color(0xFFFF78B7)
private val Cream=Color(0xFFFFF4D8)
private val NeonPink=Color(0xFFFF168B)
private data class Note(val id:Int,val text:String,val x:Int,val y:Int,val color:Long)

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{JboardApp()}}}

@Composable private fun JboardApp(){
 val context=LocalContext.current
 val prefs=remember{context.getSharedPreferences("jboard",Context.MODE_PRIVATE)}
 var boardName by remember{mutableStateOf(prefs.getString("name","My ideas")?:"My ideas")}
 val notes= remember{ mutableStateListOf<Note>() }
 var nextId by remember{mutableIntStateOf(1)}
 var selected by remember{mutableIntStateOf(-1)}
 var draft by remember{mutableStateOf("")}
 var editing by remember{mutableIntStateOf(-1)}
 var dirty by remember{mutableStateOf(false)}
 var historyOpen by remember{mutableStateOf(false)}
 var search by remember{mutableStateOf("")}
 var drawMode by remember{mutableStateOf(false)}
 val strokes=remember{mutableStateListOf<MutableList<Offset>>()}
 var activeStroke by remember{mutableStateOf<List<Offset>?>(null)}
 var section by remember{mutableStateOf("Home")}
 val history=remember{mutableStateListOf<String>()}
 fun recordActivity(message:String){history.add(message);while(history.size>100)history.removeAt(0);prefs.edit().putString("history",JSONArray().apply{history.forEach{put(it)}}.toString()).apply()}
 LaunchedEffect(Unit){try{val arr=JSONArray(prefs.getString("notes","[]"));for(i in 0 until arr.length()){val n=arr.getJSONObject(i);notes.add(Note(n.getInt("id"),n.getString("text"),n.optInt("x",20),n.optInt("y",20),n.optLong("color",0xFFFFE27A)))};nextId=(notes.maxOfOrNull{it.id}?:0)+1}catch(_:Exception){};try{val h=JSONArray(prefs.getString("history","[]"));for(i in 0 until h.length())history.add(h.getString(i))}catch(_:Exception){};try{val saved=JSONArray(prefs.getString("strokes","[]"));for(i in 0 until saved.length()){val arr=saved.getJSONArray(i);val stroke=mutableListOf<Offset>();for(j in 0 until arr.length()){val p=arr.getJSONObject(j);stroke.add(Offset(p.getDouble("x").toFloat(),p.getDouble("y").toFloat()))};if(stroke.isNotEmpty())strokes.add(stroke)}}catch(_:Exception){}}
 fun save(){val a=JSONArray();notes.forEach{a.put(JSONObject().put("id",it.id).put("text",it.text).put("x",it.x).put("y",it.y).put("color",it.color))};prefs.edit().putString("name",boardName).putString("notes",a.toString()).putString("strokes",JSONArray().apply{strokes.forEach{stroke->put(JSONArray().apply{stroke.forEach{put(JSONObject().put("x",it.x).put("y",it.y))}})}}.toString()).apply();recordActivity("Saved board");dirty=false}
 MaterialTheme(colorScheme=darkColorScheme(primary=Gold,background=Ink,surface=Board,onBackground=Cream,onSurface=Cream)){
 Scaffold(containerColor=Ink,topBar={Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){
 Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Gold),contentAlignment=Alignment.Center){Text("J",color=Ink,fontSize=32.sp,fontWeight=FontWeight.Black)}
 Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text("JBOARD",color=Cream,fontSize=20.sp,fontWeight=FontWeight.Black,letterSpacing=1.5.sp);Text("Your ideas, connected.",color=Pink,fontSize=11.sp,letterSpacing=0.5.sp)}
 TextButton(onClick={historyOpen=true}){Text("HISTORY",color=Pink,fontSize=10.sp)}
 }},floatingActionButton={FloatingActionButton(onClick={editing=-2;draft="";selected=-1},containerColor=Gold,contentColor=Ink){Text("+",fontSize=28.sp)}}){padding->
 Column(Modifier.fillMaxSize().background(Ink).padding(padding).padding(horizontal=14.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
 Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF17120D)).padding(6.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically){ listOf("⌂" to "Home","◷" to "History","♧" to "Collaborators","▱" to "Files","⚙" to "Settings").forEach{(glyph,tab)-> Surface(onClick={section=tab;if(tab=="History")historyOpen=true},shape=RoundedCornerShape(11.dp),color=if(section==tab)Color(0xFF4A3510)else Color.Transparent,border=if(section==tab)androidx.compose.foundation.BorderStroke(1.dp,Gold.copy(alpha=.65f))else null){Row(Modifier.padding(horizontal=9.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Text(glyph,color=Gold,fontSize=16.sp);Spacer(Modifier.width(5.dp));Text(tab,color=if(section==tab)Gold else Color.LightGray,fontSize=10.sp,fontWeight=FontWeight.SemiBold)}}} }
 OutlinedTextField(value=search,onValueChange={search=it},modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Search your board…")},leadingIcon={Text("⌕",color=Gold)},colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Cream,unfocusedTextColor=Cream,focusedBorderColor=Gold,unfocusedBorderColor=Color.DarkGray))
 OutlinedTextField(value=boardName,onValueChange={boardName=it;dirty=true},label={Text("Board name")},singleLine=true,modifier=Modifier.fillMaxWidth(),colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Cream,unfocusedTextColor=Cream,focusedBorderColor=Gold,unfocusedBorderColor=Color.DarkGray,focusedLabelColor=Gold,unfocusedLabelColor=Color.LightGray))
 Row(verticalAlignment=Alignment.CenterVertically){Text("IDEA BOARD",color=Gold,fontWeight=FontWeight.Bold,letterSpacing=2.sp);Spacer(Modifier.weight(1f));Text("${notes.count{it.text.contains(search,ignoreCase=true)}} MATCHING NOTES",color=Color.LightGray,fontSize=10.sp)}
 BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFF281A12)).padding(2.dp).background(Color(0xFF090707),RoundedCornerShape(21.dp))){
 val maxX=maxWidth.value.toInt();val maxY=maxHeight.value.toInt()
 androidx.compose.foundation.Canvas(Modifier.fillMaxSize().pointerInput(drawMode){if(drawMode)detectDragGestures(onDragStart={p->activeStroke=listOf(Offset(p.x/size.width,p.y/size.height))},onDrag={change,_->change.consume();activeStroke=activeStroke.orEmpty()+Offset(change.position.x/size.width,change.position.y/size.height)},onDragEnd={activeStroke?.let{strokes.add(it.toMutableList());recordActivity("Drew thread");dirty=true};activeStroke=null},onDragCancel={activeStroke=null})}){
 val step=30.dp.toPx();var y=step;while(y<size.height){var x=step;while(x<size.width){drawCircle(Color(0xFF3A2A1B),1f,Offset(x,y));x+=step};y+=step}
 drawRoundRect(Color(0x66FFD629),style=Stroke(1.5.dp.toPx()),cornerRadius=androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()))
 val visible=notes.filter{it.text.contains(search,ignoreCase=true)}
 strokes.forEach{stroke->if(stroke.size>1){val path=Path().apply{moveTo(stroke.first().x*size.width,stroke.first().y*size.height);stroke.drop(1).forEach{lineTo(it.x*size.width,it.y*size.height)}};drawPath(path,NeonPink,style=Stroke(width=3.dp.toPx()))}}
 activeStroke?.let{stroke->if(stroke.size>1){val path=Path().apply{moveTo(stroke.first().x*size.width,stroke.first().y*size.height);stroke.drop(1).forEach{lineTo(it.x*size.width,it.y*size.height)}};drawPath(path,Pink,style=Stroke(width=3.dp.toPx()))}}
 }
 if(notes.isEmpty()) Text("YOUR BOARD STARTS HERE\n\nTap + to create your first note",Modifier.align(Alignment.Center),color=Color.LightGray,fontSize=15.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center) 
 notes.filter{it.text.contains(search,ignoreCase=true)}.forEach{n->Column(Modifier.offset{IntOffset(n.x,n.y)}.width(140.dp).clip(RoundedCornerShape(12.dp)).background(Color(n.color)).pointerInput(n.id,drawMode){if(!drawMode)detectDragGestures(onDragStart={selected=n.id},onDrag={change,drag->change.consume();val dx=drag.x.roundToInt();val dy=drag.y.roundToInt();notes.replaceAll{q->if(q.id==n.id)q.copy(x=(q.x+dx).coerceIn(0,(maxX-145).coerceAtLeast(0)),y=(q.y+dy).coerceIn(0,(maxY-100).coerceAtLeast(0)))else q};dirty=true})}){
 Row(Modifier.fillMaxWidth().padding(start=8.dp,end=3.dp,top=4.dp),verticalAlignment=Alignment.CenterVertically){Text("NOTE ${n.id}",color=Ink.copy(alpha=.55f),fontSize=9.sp);Spacer(Modifier.weight(1f));TextButton(onClick={editing=n.id;draft=n.text;selected=n.id},contentPadding=PaddingValues(0.dp)){Text("✎",color=Ink)};TextButton(onClick={notes.removeAll{it.id==n.id};recordActivity("Deleted note");dirty=true},contentPadding=PaddingValues(0.dp)){Text("×",color=Ink)}}
 Text(n.text,Modifier.padding(9.dp),color=Ink,fontSize=13.sp,maxLines=7)
 }}
 Text("J",Modifier.align(Alignment.BottomEnd).padding(15.dp),color=Gold.copy(alpha=.12f),fontSize=96.sp,fontWeight=FontWeight.Black)
 Text("🦋",Modifier.align(Alignment.CenterEnd).padding(end=8.dp),color=Pink.copy(alpha=.18f),fontSize=150.sp)
 }
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={drawMode=!drawMode;activeStroke=null},modifier=Modifier.weight(1f),colors=ButtonDefaults.outlinedButtonColors(contentColor=if(drawMode)Gold else Pink)){Text(if(drawMode)"DRAW THREAD: ON" else "✎ THREAD")};OutlinedButton(onClick={draft="";editing=-2},modifier=Modifier.weight(1f),colors=ButtonDefaults.outlinedButtonColors(contentColor=Pink)){Text("+ NOTE")};OutlinedButton(onClick={historyOpen=true},modifier=Modifier.weight(1f),colors=ButtonDefaults.outlinedButtonColors(contentColor=Gold)){Text("ACTIVITY")};Button(onClick={save()},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=Pink,contentColor=Ink)){Text("SAVE",fontWeight=FontWeight.Bold)}}
 Text(if(dirty)"● UNSAVED CHANGES" else "✓ SAVED LOCALLY • PROTOTYPE",color=if(dirty)Gold else Color.Gray,fontSize=9.sp,letterSpacing=1.sp)
 }}
 if(editing!=-1)AlertDialog(onDismissRequest={editing=-1},containerColor=Board,title={Text(if(editing==-2)"New sticky note" else "Edit note",color=Gold)},text={BasicTextField(value=draft,onValueChange={draft=it},modifier=Modifier.fillMaxWidth().heightIn(min=100.dp).background(Color(0xFF333333),RoundedCornerShape(8.dp)).padding(12.dp),textStyle=MaterialTheme.typography.bodyLarge.copy(color=Cream),decorationBox={inner->Box{if(draft.isEmpty())Text("Write your thought…",color=Color.Gray);inner()}})},confirmButton={TextButton(onClick={if(editing==-2){notes.add(Note(nextId,draft,18+(notes.size%2)*150,25+(notes.size%4)*100,when(notes.size%3){0->0xFFFFE27A;1->0xFFFFA8C9;else->0xFFB8E3D0}));recordActivity("Added note");nextId++}else notes.replaceAll{if(it.id==editing)it.copy(text=draft)else it};editing=-1;dirty=true}){Text("DONE",color=Gold)}},dismissButton={TextButton(onClick={editing=-1}){Text("CANCEL",color=Pink)}})
 if(historyOpen)AlertDialog(onDismissRequest={historyOpen=false},containerColor=Board,title={Text("History",color=Gold)},text={Column(Modifier.heightIn(max=300.dp)){history.reversed().forEach{Text("• $it",color=Cream,modifier=Modifier.padding(4.dp))}}},confirmButton={Row{TextButton(onClick={history.clear();prefs.edit().putString("history","[]").apply();historyOpen=false}){Text("CLEAR",color=Pink)};TextButton(onClick={historyOpen=false}){Text("CLOSE",color=Gold)}}})
 }
}
