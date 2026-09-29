const state = { tab: 'books', books: [], records: [] };
const statusLabels = { '00': '未読', '01': '読書中', '03': '完読', '09': '貸与中' };
const $ = (id) => document.getElementById(id);

async function load() {
  const [books, records] = await Promise.all([fetch('api/books'), fetch('api/records')]);
  if (!books.ok || !records.ok) throw new Error('データを読み込めませんでした');
  state.books = await books.json(); state.records = await records.json(); render();
}
function render() {
  $('bookCount').textContent = state.books.length; $('recordCount').textContent = state.records.length;
  $('booksGrid').innerHTML = state.books.length ? state.books.map(book => `<article class="book-card"><div><span class="status">${statusLabels[book.readingStatus] || book.readingStatus}</span><h3>${esc(book.title)}</h3><p>${esc(book.author)}${book.volume ? `・${book.volume}巻` : ''}</p></div><p>${book.pageCount}ページ　<span>${esc(book.isbn)}</span></p></article>`).join('') : '<p class="empty">まだ書籍がありません。</p>';
  $('recordsList').innerHTML = state.records.length ? state.records.map(r => `<article class="record"><div class="record-date">${r.readingDate}</div><div><h3>${esc(r.title)}</h3><p>${r.pagesRead}ページ${r.completed ? ' ・ 完読' : ''}${r.comment ? `　${esc(r.comment)}` : ''}</p></div><button class="delete" data-id="${r.id}">削除</button></article>`).join('') : '<p class="empty">まだ読書記録がありません。</p>';
  document.querySelectorAll('.delete').forEach(button => button.addEventListener('click', () => removeRecord(button.dataset.id)));
}
function esc(value) { return String(value ?? '').replace(/[&<>'"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c])); }
function setFieldsEnabled(container, enabled) {
  container.querySelectorAll('input, select, textarea').forEach(field => { field.disabled = !enabled; });
}
function openCreate() {
  $('createForm').reset(); $('formError').textContent = ''; const recordMode = state.tab === 'records';
  $('modalTitle').textContent = recordMode ? '読書記録を登録' : '書籍を登録'; $('bookFields').classList.toggle('hidden', recordMode); $('recordFields').classList.toggle('hidden', !recordMode); setFieldsEnabled($('bookFields'), !recordMode); setFieldsEnabled($('recordFields'), recordMode); $('submitCreate').disabled = recordMode && !state.books.length;
  $('recordIsbn').innerHTML = state.books.map(b => `<option value="${esc(b.isbn)}">${esc(b.title)} (${esc(b.isbn)})</option>`).join(''); if (recordMode) document.querySelector('[name="readingDate"]').value = new Date().toISOString().slice(0, 10); $('createDialog').showModal();
}
async function save(event) { event.preventDefault(); const recordMode = state.tab === 'records'; const form = new FormData($('createForm')); const data = new URLSearchParams();
  if (recordMode) { ['recordIsbn','readingDate','pagesRead','comment'].forEach(k => data.set(k === 'recordIsbn' ? 'isbn' : k, form.get(k) || '')); data.set('completed', form.has('completed') ? 'true' : 'false'); } else { ['isbn','title','author','volume','translator','pageCount','readingStatus'].forEach(k => data.set(k, form.get(k) || '')); }
  const response = await fetch(`api/${recordMode ? 'records' : 'books'}`, { method: 'POST', headers: {'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'}, body: data }); if (!response.ok) { const e = await response.json(); $('formError').textContent = e.error || '登録に失敗しました'; return; } $('createDialog').close(); await load(); }
async function removeRecord(id) { if (!confirm('この読書記録を削除しますか？')) return; const response = await fetch(`api/records/${id}`, {method:'DELETE'}); if (!response.ok) { alert('削除に失敗しました'); return; } await load(); }
document.querySelectorAll('.tab').forEach(tab => tab.addEventListener('click', () => { state.tab = tab.dataset.tab; document.querySelectorAll('.tab').forEach(t => t.classList.toggle('active', t === tab)); $('booksPanel').classList.toggle('hidden', state.tab !== 'books'); $('recordsPanel').classList.toggle('hidden', state.tab !== 'records'); }));
$('openCreate').addEventListener('click', openCreate); $('closeCreate').addEventListener('click', () => $('createDialog').close()); $('cancelCreate').addEventListener('click', () => $('createDialog').close()); $('createForm').addEventListener('submit', save);
load().catch(error => { $('booksGrid').innerHTML = `<p class="empty">${esc(error.message)}</p>`; });
