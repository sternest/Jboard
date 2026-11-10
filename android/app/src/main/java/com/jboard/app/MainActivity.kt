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
 var section by remember{mutableStateOf("Home")}
 val history=remember{mutableStateListOf<String>()}
 LaunchedEffect(Unit){try{val arr=JSONArray(prefs.getString("notes","[]"));for(i in 0 until arr.length()){val n=arr.getJSONObject(i);notes.add(Note(n.getInt("id"),n.getString("text"),n.optInt("x",20),n.optInt("y",20),n.optLong("color",0xFFFFE27A)))};nextId=(notes.maxOfOrNull{it.id}?:0)+1}catch(_:Exception){}}
 fun save(){val a=JSONArray();notes.forEach{a.put(JSONObject().put("id",it.id).put("text",it.text).put("x",it.x).put("y",it.y).put("color",it.color))};prefs.edit().putString("name",boardName).putString("notes",a.toString()).apply();history.add("Saved board");dirty=false}
 MaterialTheme(colorScheme=darkColorScheme(primary=Gold,background=Ink,surface=Board,onBackground=Cream,onSurface=Cream)){
 Scaffold(containerColor=Ink,topBar={Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){
 Box(Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(Gold),contentAlignment=Alignment.Center){Text("J",color=Ink,fontSize=32.sp,fontWeight=FontWeight.Black)}
 Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text("JBOARD",color=Cream,fontSize=20.sp,fontWeight=FontWeight.Black,letterSpacing=1.5.sp);Text("Your ideas, connected.",color=Pink,fontSize=11.sp)}
 TextButton(onClick={historyOpen=true}){Text("HISTORY",color=Pink,fontSize=10.sp)}
 }},floatingActionButton={FloatingActionButton(onClick={editing=-2;draft="";selected=-1},containerColor=Gold,contentColor=Ink){Text("+",fontSize=28.sp)}}){padding->
 Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=14.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
 Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){ listOf("Home","History","Files","Settings").forEach{tab-> FilterChip(selected=section==tab,onClick={section=tab;if(tab=="History")historyOpen=true},label={Text(tab)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Gold,selectedLabelColor=Ink,labelColor=Pink))} }
 OutlinedTextField(value=search,onValueChange={search=it},modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Search your board…")},leadingIcon={Text("⌕",color=Gold)},colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Cream,unfocusedTextColor=Cream,focusedBorderColor=Gold,unfocusedBorderColor=Color.DarkGray))
 OutlinedTextField(value=boardName,onValueChange={boardName=it;dirty=true},label={Text("Board name")},singleLine=true,modifier=Modifier.fillMaxWidth(),colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Cream,unfocusedTextColor=Cream,focusedBorderColor=Gold,unfocusedBorderColor=Color.DarkGray,focusedLabelColor=Gold,unfocusedLabelColor=Color.LightGray))
 Row(verticalAlignment=Alignment.CenterVertically){Text("IDEA BOARD",color=Gold,fontWeight=FontWeight.Bold,letterSpacing=2.sp);Spacer(Modifier.weight(1f));Text("${notes.count{it.text.contains(search,ignoreCase=true)}} MATCHING NOTES",color=Color.LightGray,fontSize=10.sp)}
 BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Board)){
 val maxX=maxWidth.value.toInt();val maxY=maxHeight.value.toInt()
 androidx.compose.foundation.Canvas(Modifier.fillMaxSize()){val step=30.dp.toPx();var y=step;while(y<size.height){var x=step;while(x<size.width){drawCircle(Color(0xFF35312B),1f,androidx.compose.ui.geometry.Offset(x,y));x+=step};y+=step}}
 if(notes.isEmpty()) Text("YOUR BOARD STARTS HERE\n\nTap + to create your first note",Modifier.align(Alignment.Center),color=Color.LightGray,fontSize=15.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center)\n notes.filter{it.text.contains(search,ignoreCase=true)}.forEach{n->Column(Modifier.offset{IntOffset(n.x,n.y)}.width(140.dp).clip(RoundedCornerShape(12.dp)).background(Color(n.color)).pointerInput(n.id){detectDragGestures(onDragStart={selected=n.id},onDrag={change,drag->change.consume();val dx=(drag.x/this.size.width.coerceAtLeast(1)*maxX).roundToInt();val dy=(drag.y/this.size.height.coerceAtLeast(1)*maxY).roundToInt();notes.replaceAll{q->if(q.id==n.id)q.copy(x=(q.x+dx).coerceIn(0,(maxX-145).coerceAtLeast(0)),y=(q.y+dy).coerceIn(0,(maxY-100).coerceAtLeast(0)))else q};dirty=true})}){
 Row(Modifier.fillMaxWidth().padding(start=8.dp,end=3.dp,top=4.dp),verticalAlignment=Alignment.CenterVertically){Text("NOTE ${n.id}",color=Ink.copy(alpha=.55f),fontSize=9.sp);Spacer(Modifier.weight(1f));TextButton(onClick={editing=n.id;draft=n.text;selected=n.id},contentPadding=PaddingValues(0.dp)){Text("✎",color=Ink)};TextButton(onClick={notes.removeAll{it.id==n.id};history.add("Deleted note");dirty=true},contentPadding=PaddingValues(0.dp)){Text("×",color=Ink)}}
 Text(n.text,Modifier.padding(9.dp),color=Ink,fontSize=13.sp,maxLines=7)
 }}
 Text("J",Modifier.align(Alignment.BottomEnd).padding(15.dp),color=Gold.copy(alpha=.18f),fontSize=70.sp,fontWeight=FontWeight.Black)
 Text("🦋",Modifier.align(Alignment.BottomStart).padding(15.dp),color=Pink.copy(alpha=.45f),fontSize=28.sp)
 }
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={draft="";editing=-2},modifier=Modifier.weight(1f),colors=ButtonDefaults.outlinedButtonColors(contentColor=Pink)){Text("+ NOTE")};OutlinedButton(onClick={historyOpen=true},modifier=Modifier.weight(1f),colors=ButtonDefaults.outlinedButtonColors(contentColor=Gold)){Text("ACTIVITY")};Button(onClick={save()},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=Pink,contentColor=Ink)){Text("SAVE",fontWeight=FontWeight.Bold)}}
 Text(if(dirty)"● UNSAVED CHANGES" else "✓ SAVED LOCALLY • PROTOTYPE",color=if(dirty)Gold else Color.Gray,fontSize=9.sp,letterSpacing=1.sp)
 }}
 if(editing!=-1)AlertDialog(onDismissRequest={editing=-1},containerColor=Board,title={Text(if(editing==-2)"New sticky note" else "Edit note",color=Gold)},text={BasicTextField(value=draft,onValueChange={draft=it},modifier=Modifier.fillMaxWidth().heightIn(min=100.dp).background(Color(0xFF333333),RoundedCornerShape(8.dp)).padding(12.dp),textStyle=MaterialTheme.typography.bodyLarge.copy(color=Cream),decorationBox={inner->Box{if(draft.isEmpty())Text("Write your thought…",color=Color.Gray);inner()}})},confirmButton={TextButton(onClick={if(editing==-2){notes.add(Note(nextId,draft,18+(notes.size%2)*150,25+(notes.size%4)*100,when(notes.size%3){0->0xFFFFE27A;1->0xFFFFA8C9;else->0xFFB8E3D0}));history.add("Added note");nextId++}else notes.replaceAll{if(it.id==editing)it.copy(text=draft)else it};editing=-1;dirty=true}){Text("DONE",color=Gold)}},dismissButton={TextButton(onClick={editing=-1}){Text("CANCEL",color=Pink)}})
 if(historyOpen)AlertDialog(onDismissRequest={historyOpen=false},containerColor=Board,title={Text("History",color=Gold)},text={Column(Modifier.heightIn(max=300.dp)){history.reversed().forEach{Text("• $it",color=Cream,modifier=Modifier.padding(4.dp))}}},confirmButton={TextButton(onClick={historyOpen=false}){Text("CLOSE",color=Pink)}})
 }
}
