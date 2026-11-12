(()=>{'use strict';const $=s=>document.querySelector(s),$$=s=>[...document.querySelectorAll(s)];const DB='jboard-web-v1',STORE='attachments';let db,board={id:'default',name:'My ideas',notes:[],files:[],threads:[]},projects=[],history=[],pan={x:0,y:0},zoom=1,draw=false,editing=null,drag=null,threadDraft=null,installPrompt=null,fileUrls=new Map();const boardEl=$('#board'),itemsEl=$('#items'),svg=$('#threads');const uid=()=>Math.random().toString(36).slice(2,10);function openDB(){return new Promise((resolve,reject)=>{const req=indexedDB.open(DB,1);req.onupgradeneeded=()=>req.result.createObjectStore(STORE,{keyPath:'id'});req.onsuccess=()=>{db=req.result;resolve(db)};req.onerror=()=>reject(req.error)})}function putFile(record){return new Promise((res,rej)=>{const tx=db.transaction(STORE,'readwrite');tx.objectStore(STORE).put(record);tx.oncomplete=res;tx.onerror=()=>rej(tx.error)})}function getFile(id){return new Promise((res,rej)=>{const r=db.transaction(STORE).objectStore(STORE).get(id);r.onsuccess=()=>res(r.result);r.onerror=()=>rej(r.error)})}function log(msg){history.unshift({msg,at:new Date().toISOString()});history=history.slice(0,150);localStorage.setItem('jb-history',JSON.stringify(history));renderHistory()}function persist(){board.name=$('#boardName').value.trim()||'Untitled board';localStorage.setItem('jb-board-'+board.id,JSON.stringify(board));localStorage.setItem('jb-projects',JSON.stringify(projects));$('#boardName').value=board.name;renderProjects()}function load(){try{projects=JSON.parse(localStorage.getItem('jb-projects')||'[]');history=JSON.parse(localStorage.getItem('jb-history')||'[]')}catch{}if(!projects.length){projects=[{id:'default',name:'My ideas'}];localStorage.setItem('jb-projects',JSON.stringify(projects))}const active=localStorage.getItem('jb-active')||projects[0].id;switchProject(active,false);renderHistory()}function switchProject(id,record=true){const meta=projects.find(p=>p.id===id)||projects[0];board={id:meta.id,name:meta.name,notes:[],files:[],threads:[]};try{const saved=JSON.parse(localStorage.getItem('jb-board-'+id)||'null');if(saved)board={...board,...saved}}catch{}$('#boardName').value=board.name;localStorage.setItem('jb-active',id);pan={x:0,y:0};zoom=1;renderBoard();renderFiles();renderProjects();if(record)log('Opened project '+board.name)}function worldToScreen(x,y){return{x:x*zoom+pan.x,y:y*zoom+pan.y}}function screenToWorld(x,y){return{x:(x-pan.x)/zoom,y:(y-pan.y)/zoom}}function place(el,x,y){const p=worldToScreen(x,y);el.style.transform='translate('+p.x+'px,'+p.y+'px) scale('+zoom+')';el.style.transformOrigin='top left'}async function renderBoard(){
 itemsEl.innerHTML='';
 board.notes.forEach(note=>{
  const el=document.createElement('article');el.className='note '+(note.color||'');el.dataset.id=note.id;place(el,note.x,note.y);
  const head=document.createElement('div');head.className='note-head';
  const label=document.createElement('span');label.textContent='IDEA NOTE';
  const actions=document.createElement('div');actions.className='note-actions';
  const edit=document.createElement('button');edit.type='button';edit.textContent='Edit';edit.title='Edit note';edit.onclick=e=>{e.stopPropagation();openNote(note.id)};
  const del=document.createElement('button');del.type='button';del.textContent='×';del.title='Delete note';del.onclick=e=>{e.stopPropagation();board.notes=board.notes.filter(n=>n.id!==note.id);board.threads=board.threads.filter(t=>t.a!==note.id&&t.b!==note.id);renderBoard();persist();log('Deleted a note')};
  actions.append(edit,del);head.append(label,actions);
  const body=document.createElement('div');body.className='note-body';body.textContent=note.text;
  el.append(head,body);el.addEventListener('pointerdown',e=>startNoteDrag(e,note,el));itemsEl.append(el);
 });
 for(const file of board.files){
  const el=document.createElement('article');el.className='file-chip';el.dataset.id=file.id;place(el,file.x,file.y);
  const thumb=document.createElement('div');thumb.className='file-thumb';
  const info=document.createElement('div');info.className='file-info';
  const kind=document.createElement('b');kind.textContent=file.type?.startsWith('image/')?'IMAGE':file.type?.startsWith('video/')?'VIDEO':file.type==='application/pdf'?'PDF':'FILE';
  const name=document.createElement('small');name.textContent=file.name;info.append(kind,name);
  const actions=document.createElement('div');actions.className='file-actions';
  const open=document.createElement('button');open.type='button';open.textContent='Open';open.onclick=e=>{e.stopPropagation();previewFile(file.id)};
  const remove=document.createElement('button');remove.type='button';remove.textContent='×';remove.title='Remove attachment';remove.onclick=e=>{e.stopPropagation();board.files=board.files.filter(x=>x.id!==file.id);renderBoard();renderFiles();persist();log('Removed '+file.name)};
  actions.append(open,remove);el.append(thumb,info,actions);
  const stored=await getFile(file.id).catch(()=>null);
  if(stored?.blob){
   if(stored.type?.startsWith('image/')){
    const url=fileUrls.get(file.id)||URL.createObjectURL(stored.blob);fileUrls.set(file.id,url);
    const img=document.createElement('img');img.src=url;img.alt=file.name;img.loading='lazy';thumb.append(img);
   }else{
    const symbol=document.createElement('span');symbol.className='file-kind-icon';symbol.textContent=stored.type?.startsWith('video/')?'▶':stored.type==='application/pdf'?'PDF':'▤';thumb.append(symbol);
   }
  }else{const symbol=document.createElement('span');symbol.className='file-kind-icon';symbol.textContent='!';thumb.append(symbol);name.textContent=file.name+' · file data unavailable'}
  el.addEventListener('pointerdown',e=>startFileDrag(e,file,el));itemsEl.append(el);
 }
 drawThreads();boardEl.style.backgroundPosition=pan.x+'px '+pan.y+'px';
 $('#boardHint').classList.toggle('hidden',board.notes.length+board.files.length>0);
 $('#zoomValue').textContent=Math.round(zoom*100)+'%';
 $('#modeStatus').textContent=draw?'Connect mode: drag from one note to another':'Drag empty space to move around';
}
function drawThreads(){svg.innerHTML='';board.threads.forEach(t=>{const a=board.notes.find(n=>n.id===t.a),b=board.notes.find(n=>n.id===t.b);if(!a||!b)return;const p1=worldToScreen(a.x+110,a.y+76),p2=worldToScreen(b.x+110,b.y+76);const path=document.createElementNS('http://www.w3.org/2000/svg','path');path.setAttribute('d','M '+p1.x+' '+p1.y+' C '+(p1.x+60*zoom)+' '+p1.y+', '+(p2.x-60*zoom)+' '+p2.y+', '+p2.x+' '+p2.y);svg.append(path)});if(threadDraft){const p=threadDraft.points;const path=document.createElementNS('http://www.w3.org/2000/svg','path');path.setAttribute('d','M '+p[0].x+' '+p[0].y+' L '+p[1].x+' '+p[1].y);svg.append(path)}}function startNoteDrag(e,note,el){if(draw){e.preventDefault();e.stopPropagation();threadDraft={from:note.id,points:[{x:e.clientX-boardEl.getBoundingClientRect().left,y:e.clientY-boardEl.getBoundingClientRect().top},{x:e.clientX-boardEl.getBoundingClientRect().left,y:e.clientY-boardEl.getBoundingClientRect().top}]};el.setPointerCapture(e.pointerId);const move=ev=>{const r=boardEl.getBoundingClientRect();threadDraft.points[1]={x:ev.clientX-r.left,y:ev.clientY-r.top};drawThreads()};const end=ev=>{el.removeEventListener('pointermove',move);el.removeEventListener('pointerup',end);el.removeEventListener('pointercancel',end);const target=document.elementFromPoint(ev.clientX,ev.clientY)?.closest('.note');if(target&&target.dataset.id!==note.id){board.threads.push({id:uid(),a:note.id,b:target.dataset.id});persist();log('Connected two ideas')}threadDraft=null;drawThreads()};el.addEventListener('pointermove',move);el.addEventListener('pointerup',end);el.addEventListener('pointercancel',end);return}if(e.target.closest('button'))return;e.preventDefault();e.stopPropagation();const start={x:e.clientX,y:e.clientY,nx:note.x,ny:note.y};el.setPointerCapture(e.pointerId);function move(ev){note.x=start.nx+(ev.clientX-start.x)/zoom;note.y=start.ny+(ev.clientY-start.y)/zoom;place(el,note.x,note.y);drawThreads()}function end(){el.removeEventListener('pointermove',move);el.removeEventListener('pointerup',end);el.removeEventListener('pointercancel',end);persist();log('Moved a note')}el.addEventListener('pointermove',move);el.addEventListener('pointerup',end);el.addEventListener('pointercancel',end)}function startFileDrag(e,file,el){if(e.target.closest('button,a'))return;e.preventDefault();e.stopPropagation();const start={x:e.clientX,y:e.clientY,nx:file.x,ny:file.y};el.setPointerCapture(e.pointerId);function move(ev){file.x=start.nx+(ev.clientX-start.x)/zoom;file.y=start.ny+(ev.clientY-start.y)/zoom;place(el,file.x,file.y)}function end(){el.removeEventListener('pointermove',move);el.removeEventListener('pointerup',end);persist()}el.addEventListener('pointermove',move);el.addEventListener('pointerup',end)}boardEl.addEventListener('pointerdown',e=>{
 if(e.target.closest('.note,.file-chip,button,input,textarea,a'))return;
 if(draw)return;
 e.preventDefault();drag={pointerId:e.pointerId,x:e.clientX,y:e.clientY,px:pan.x,py:pan.y};
 try{boardEl.setPointerCapture(e.pointerId)}catch{}
 boardEl.classList.add('is-panning');
});
boardEl.addEventListener('pointermove',e=>{
 if(!drag||drag.pointerId!==e.pointerId)return;
 pan.x=drag.px+(e.clientX-drag.x);pan.y=drag.py+(e.clientY-drag.y);
 boardEl.style.backgroundPosition=pan.x+'px '+pan.y+'px';
 itemsEl.querySelectorAll('.note,.file-chip').forEach(el=>{
  const item=board.notes.find(n=>n.id===el.dataset.id)||board.files.find(f=>f.id===el.dataset.id);
  if(item)place(el,item.x,item.y);
 });
 drawThreads();
});
function stopBoardPan(e){if(drag&&(!e||drag.pointerId===e.pointerId)){drag=null;boardEl.classList.remove('is-panning')}}
boardEl.addEventListener('pointerup',stopBoardPan);boardEl.addEventListener('pointercancel',stopBoardPan);boardEl.addEventListener('lostpointercapture',stopBoardPan);boardEl.addEventListener('wheel',e=>{if(e.ctrlKey||e.metaKey){e.preventDefault();zoom=Math.min(2.2,Math.max(.45,zoom*(e.deltaY<0?1.08:.92)));renderBoard()}},{passive:false});function openNote(id=null){editing=id;$('#noteDialogTitle').textContent=id?'Edit note':'New note';$('#noteText').value=id?board.notes.find(n=>n.id===id)?.text||'':'';$('#noteSubmit').textContent=id?'Save note':'Add note';$('#noteDialog').showModal();$('#noteText').focus()}$('#noteForm').addEventListener('submit',e=>{if(e.submitter?.value==='cancel')return;e.preventDefault();const text=$('#noteText').value.trim();if(!text)return;if(editing){const n=board.notes.find(n=>n.id===editing);if(n)n.text=text;log('Edited a note')}else{const center=screenToWorld(boardEl.clientWidth/2-80,boardEl.clientHeight/2-55);board.notes.push({id:uid(),text,x:center.x+(Math.random()-.5)*70,y:center.y+(Math.random()-.5)*70,color:['','pink','blue','green'][board.notes.length%4]});log('Added a note')}$('#noteDialog').close();renderBoard();persist()});$('#addNote').onclick=()=>openNote();$('#addFile').onclick=()=>$('#fileInput').click();$('#filesAdd').onclick=()=>$('#fileInput').click();$('#fileInput').addEventListener('change',async e=>{
 const chosen=[...e.target.files];let added=0;
 for(const file of chosen){
  try{
   const id=uid();await putFile({id,name:file.name,type:file.type,size:file.size,blob:file});
   const columns=Math.max(1,Math.floor((boardEl.clientWidth-24)/250));const p=screenToWorld(24+(board.files.length%columns)*250,24+Math.floor(board.files.length/columns)*190);
   board.files.push({id,name:file.name,type:file.type,size:file.size,x:p.x,y:p.y});added++;log('Attached '+file.name);
  }catch(err){console.error('Attachment save failed',err);alert('Could not save '+file.name+'. Check available browser storage and try again.')}
 }
 await renderBoard();renderFiles();persist();e.target.value='';
 if(added)$('#modeStatus').textContent=added+' attachment'+(added===1?'':'s')+' added to the board';
});async function previewFile(id){
 const f=await getFile(id);if(!f?.blob){alert('This attachment is no longer available in this browser.');return}
 const url=URL.createObjectURL(f.blob),area=$('#filePreview');area.innerHTML='';
 if(f.type?.startsWith('image/')){const img=document.createElement('img');img.src=url;img.alt=f.name;area.append(img)}
 else if(f.type?.startsWith('video/')){const v=document.createElement('video');v.src=url;v.controls=true;v.playsInline=true;area.append(v)}
 else if(f.type==='application/pdf'||f.name.toLowerCase().endsWith('.pdf')){const frame=document.createElement('iframe');frame.src=url;frame.title=f.name;frame.className='pdf-preview';area.append(frame)}
 else{const p=document.createElement('p');p.textContent=f.name+' · '+formatBytes(f.size);area.append(p)}
 const download=document.createElement('a');download.href=url;download.download=f.name;download.className='download-file';download.textContent='Download '+f.name;area.append(download);
 $('#fileDialog').showModal();$('#closeFileDialog').onclick=()=>{$('#fileDialog').close();URL.revokeObjectURL(url)};
}
async function renderFiles(){
 const root=$('#fileList');root.innerHTML='';
 if(!board.files.length){root.innerHTML='<p class="muted">No attachments on this board yet.</p>';return}
 for(const f of board.files){
  const el=document.createElement('div');el.className='file-card';
  const symbol=document.createElement('div');symbol.className='file-symbol';
  const info=document.createElement('div');info.style.minWidth='0';
  const name=document.createElement('strong');name.textContent=f.name;
  const meta=document.createElement('small');meta.textContent=formatBytes(f.size);
  const open=document.createElement('button');open.className='quiet';open.textContent='Open';open.onclick=()=>previewFile(f.id);
  const stored=await getFile(f.id).catch(()=>null);
  if(stored?.blob&&stored.type?.startsWith('image/')){
   const url=fileUrls.get(f.id)||URL.createObjectURL(stored.blob);fileUrls.set(f.id,url);
   const img=document.createElement('img');img.className='file-list-thumb';img.src=url;img.alt=f.name;symbol.append(img);
  }else{symbol.textContent=stored?.type==='application/pdf'?'PDF':stored?.type?.startsWith('video/')?'▶':'▤'}
  info.append(name,meta);el.append(symbol,info,open);root.append(el);
 }
}
function renderProjects(){const root=$('#projectList');if(!root)return;root.innerHTML='';projects.forEach(p=>{const el=document.createElement('div');el.className='project-card';const strong=document.createElement('strong');strong.textContent=p.name;const small=document.createElement('small');small.textContent=p.id===board.id?'Currently open':'Open project';el.append(strong,small);el.onclick=()=>{persist();switchProject(p.id)};root.append(el)})}function renderHistory(){const root=$('#historyList');if(!root)return;root.innerHTML='';if(!history.length){root.innerHTML='<p class="muted">No activity yet.</p>';return}history.forEach(h=>{const row=document.createElement('div');row.className='history-row';const dot=document.createElement('i');dot.className='history-dot';const div=document.createElement('div');const msg=document.createElement('div');msg.textContent=h.msg;const date=document.createElement('small');date.textContent=new Date(h.at).toLocaleString();div.append(msg,date);row.append(dot,div);root.append(row)})}$('#newProject').onclick=()=>$('#projectDialog').showModal();$('#projectForm').addEventListener('submit',e=>{if(e.submitter?.value==='cancel')return;e.preventDefault();const name=$('#projectName').value.trim();if(!name)return;persist();const p={id:uid(),name};projects.push(p);$('#projectDialog').close();$('#projectName').value='';switchProject(p.id);log('Created project '+name)});$$('.tab').forEach(b=>b.onclick=()=>{$$('.tab').forEach(x=>x.classList.toggle('active',x===b));$$('.panel').forEach(x=>x.classList.remove('active'));$('#'+b.dataset.panel+'Panel').classList.add('active');if(b.dataset.panel==='files')renderFiles();if(b.dataset.panel==='projects')renderProjects();if(b.dataset.panel==='history')renderHistory()});$('#drawMode').onclick=()=>{draw=!draw;$('#drawMode').classList.toggle('active',draw);$('#modeStatus').textContent=draw?'Connect mode: drag from one note to another':'Drag empty space to move around'};$('#saveBtn').onclick=()=>{persist();log('Saved '+board.name)};$('#boardName').addEventListener('change',()=>{const p=projects.find(p=>p.id===board.id);if(p)p.name=$('#boardName').value;persist()});$('#centerBoard').onclick=()=>{pan={x:0,y:0};zoom=1;renderBoard()};$('#zoomIn').onclick=()=>{zoom=Math.min(2.2,zoom*1.15);renderBoard()};$('#zoomOut').onclick=()=>{zoom=Math.max(.45,zoom/1.15);renderBoard()};$('#exportBtn').onclick=()=>{persist();const data={format:'jboard-export',version:1,board,history};const blob=new Blob([JSON.stringify(data,null,2)],{type:'application/json'});const a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download=(board.name.replace(/[^a-z0-9-_]/gi,'_')||'jboard')+'.jboard.json';a.click();URL.revokeObjectURL(a.href)};window.addEventListener('beforeinstallprompt',e=>{e.preventDefault();installPrompt=e;$('#installBtn').classList.remove('hidden')});$('#installBtn').onclick=async()=>{if(installPrompt){installPrompt.prompt();await installPrompt.userChoice;installPrompt=null;$('#installBtn').classList.add('hidden')}};if('serviceWorker'in navigator)window.addEventListener('load',()=>navigator.serviceWorker.register('./sw.js').catch(()=>{}));(async()=>{try{await openDB();load()}catch(err){console.error(err);load();$('#modeStatus').textContent='Browser storage unavailable — export your work regularly'}})();function formatBytes(n){if(n<1024)return n+' B';if(n<1048576)return(n/1024).toFixed(1)+' KB';return(n/1048576).toFixed(1)+' MB'}})();