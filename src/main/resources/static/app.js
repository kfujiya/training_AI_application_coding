const dailyRoot = document.querySelector('#daily');
const longRoot = document.querySelector('#long-term');
const dialog = document.querySelector('#settle-dialog');
let notes = [];
let settlingId = null;

const fmtDate = value => new Intl.DateTimeFormat('ja-JP', {year:'numeric',month:'long',day:'numeric',weekday:'short'}).format(new Date(`${value}T00:00:00`));
document.querySelector('#today').textContent = new Intl.DateTimeFormat('ja-JP', {year:'numeric',month:'long',day:'numeric',weekday:'short'}).format(new Date());

async function api(path, options={}) {
  const response = await fetch(`/api${path}`, {headers:{'Content-Type':'application/json'}, ...options});
  if (!response.ok) { const data = await response.json().catch(()=>({})); throw new Error(data.message || '処理に失敗しました'); }
  return response.json();
}
function toast(message) { const el=document.querySelector('#toast'); el.textContent=message; el.classList.add('show'); setTimeout(()=>el.classList.remove('show'),2400); }
function taskList(note) {
  if (!note.tasks.length) return '<p class="empty">まだタスクがありません。ひとつ書き留めてみましょう。</p>';
  return `<ul class="task-list">${note.tasks.map(task=>`<li class="task ${task.completed?'done':''}" data-task="${task.id}"><input class="task-check" type="checkbox" ${task.completed?'checked':''} aria-label="${task.completed?'未完了にする':'完了にする'}"><span class="task-title">${escapeHtml(task.title)}</span><button class="edit-task" type="button" aria-label="タスク名を編集">編集</button></li>`).join('')}</ul>`;
}
function escapeHtml(s) { const el=document.createElement('span'); el.textContent=s; return el.innerHTML; }
function renderNote(note) {
  const daily=note.type==='DAILY', pct=note.totalCount ? Math.round(note.completedCount*100/note.totalCount):0;
  if (note.settled) {
    const unfinished=note.tasks.filter(task=>!task.completed);
    return `<article class="note archived"><div class="note-header"><div><div class="note-date">${fmtDate(note.noteDate)} · 今日の記録</div><h3 class="note-title">一日のふりかえり</h3></div><span class="progress">${note.finalProgress}%</span></div><ul class="archive-tasks">${unfinished.map(task=>`<li class="archive-task incomplete"><span class="archive-task-title">${escapeHtml(task.title)}</span><span class="status-badge">未完了</span></li>`).join('') || '<li class="archive-empty">未完了タスクはありません</li>'}</ul></article>`;
  }
  return `<article class="note ${daily?'daily-note':'long-note'}" data-note="${note.id}"><div class="note-header"><div><div class="note-date">${fmtDate(note.noteDate)}${daily?' · DAILY':''}</div><h3 class="note-title">${daily?'今日のタスク':'長期タスク'}</h3></div>${daily?`<span class="progress">${note.completedCount}/${note.totalCount} · ${pct}%</span>`:''}</div>${daily?`<div class="progress-track"><div class="progress-fill" style="width:${pct}%"></div></div>`:''}${taskList(note)}<div class="task-entry"><input maxlength="300" placeholder="タスクを書き足す…"><button aria-label="追加">＋</button></div>${daily?'<button class="settle-btn">本日のタスク終了</button>':''}</article>`;
}
function render() {
  const daily=notes.filter(n=>n.type==='DAILY').sort((a,b)=>b.noteDate.localeCompare(a.noteDate));
  const activeDaily=daily.filter(n=>!n.settled), archived=daily.filter(n=>n.settled);
  dailyRoot.innerHTML=[...activeDaily,...archived].map(renderNote).join('') || '<p class="empty">日次付箋を準備しています…</p>';
  const longs=notes.filter(n=>n.type==='LONG_TERM');
  longRoot.innerHTML=longs.map(renderNote).join('') || '<p class="empty">長期付箋はまだありません。必要なときに追加できます。</p>';
  document.querySelectorAll('.note:not(.archived)').forEach(card=>{
    const id=Number(card.dataset.note);
    card.querySelectorAll('.task-check').forEach(input=>input.addEventListener('change',async()=>{const taskId=Number(input.closest('.task').dataset.task);try{await api(`/stickies/${id}/tasks/${taskId}`,{method:'PATCH'});await load();}catch(e){toast(e.message);}}));
    card.querySelectorAll('.edit-task').forEach(button=>button.addEventListener('click',()=>startTaskEdit(id,button.closest('.task'))));
    const add=async()=>{const input=card.querySelector('.task-entry input');if(!input.value.trim())return;try{await api(`/stickies/${id}/tasks`,{method:'POST',body:JSON.stringify({title:input.value})});await load();}catch(e){toast(e.message);}};
    card.querySelector('.task-entry button').addEventListener('click',add);
    card.querySelector('.task-entry input').addEventListener('keydown',e=>{if(e.key==='Enter')add();});
    card.querySelector('.settle-btn')?.addEventListener('click',()=>openSettle(id));
  });
}
function startTaskEdit(noteId, row) {
  const title=row.querySelector('.task-title');
  if (!title) return;
  const input=document.createElement('input');
  input.type='text'; input.className='task-edit-input'; input.maxLength=300;
  input.setAttribute('value',title.textContent); input.value=title.textContent;
  const save=document.createElement('button'); save.className='save-task'; save.type='button'; save.textContent='保存';
  const cancel=document.createElement('button'); cancel.className='cancel-task'; cancel.type='button'; cancel.textContent='取消';
  title.replaceWith(input); row.querySelector('.edit-task').hidden=true; row.append(save,cancel);
  input.focus(); input.select();
  const finish=()=>{input.replaceWith(title);row.querySelector('.edit-task').hidden=false;save.remove();cancel.remove();};
  cancel.addEventListener('click',finish);
  let saving=false;
  const persist=async()=>{
    if(saving)return;
    if(!input.value.trim()){toast('タスク名を入力してください');input.focus();return;}
    saving=true; save.disabled=true;
    try{await api(`/stickies/${noteId}/tasks/${row.dataset.task}`,{method:'PUT',body:JSON.stringify({title:input.value})});await load();toast('タスク名を更新しました');}
    catch(e){saving=false;save.disabled=false;toast(e.message);}
  };
  save.addEventListener('click',persist);
  input.addEventListener('keydown',e=>{if(e.key==='Enter'&&!e.isComposing){e.preventDefault();persist();}if(e.key==='Escape')finish();});
}
async function load(){try{notes=await api('/stickies');render();}catch(e){dailyRoot.innerHTML=`<p class="empty">データベースに接続できません。PostgreSQLの起動と接続設定を確認してください。<br>${escapeHtml(e.message)}</p>`;}}
function openSettle(id){settlingId=id;const n=notes.find(x=>x.id===id), pct=n.totalCount?Math.round(n.completedCount*100/n.totalCount):0;const unfinished=n.tasks.filter(t=>!t.completed);document.querySelector('#settle-content').innerHTML=`<p class="modal-date">作成日：${fmtDate(n.noteDate)}</p><div class="modal-progress">最終進捗　<span>${n.completedCount}/${n.totalCount} · ${pct}%</span></div><div class="unfinished"><strong>未完了のタスク</strong>${unfinished.length?`<ul>${unfinished.map(t=>`<li>${escapeHtml(t.title)}</li>`).join('')}</ul>`:'<p>未完了タスクはありません。</p>'}</div>`;dialog.showModal();}
document.querySelector('#confirm-settle').addEventListener('click',async e=>{e.preventDefault();try{await api(`/stickies/${settlingId}/settle`,{method:'POST'});dialog.close();await load();toast('今日の記録を保存しました');}catch(err){toast(err.message);}});
document.querySelector('#new-note').addEventListener('click',async()=>{try{await api('/stickies/long-term',{method:'POST'});await load();}catch(e){toast(e.message);}});
load();
