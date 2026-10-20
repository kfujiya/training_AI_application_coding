const $ = id => document.getElementById(id);
const presets = {
  tokyo: ['東京', 35.6762, 139.6503, 'Asia/Tokyo'], osaka: ['大阪', 34.6937, 135.5023, 'Asia/Tokyo'],
  sapporo: ['札幌', 43.0618, 141.3545, 'Asia/Tokyo'], sendai: ['仙台', 38.2682, 140.8694, 'Asia/Tokyo'],
  nagoya: ['名古屋', 35.1815, 136.9066, 'Asia/Tokyo'], fukuoka: ['福岡', 33.5904, 130.4017, 'Asia/Tokyo'],
  naha: ['那覇', 26.2124, 127.6809, 'Asia/Tokyo'], london: ['ロンドン', 51.5074, -0.1278, 'Europe/London'],
  newyork: ['ニューヨーク', 40.7128, -74.006, 'America/New_York']
};
const defaultSettings = { name: '東京', latitude: 35.6762, longitude: 139.6503, timezone: 'Asia/Tokyo', countries: ['JP'] };
let settings = { ...defaultSettings };
try {
  const saved = JSON.parse(localStorage.getItem('weekly-calendar-settings'));
  if (saved && typeof saved.name === 'string' && Number.isFinite(saved.latitude) && Math.abs(saved.latitude) <= 90 && Number.isFinite(saved.longitude) && Math.abs(saved.longitude) <= 180 && Array.isArray(saved.countries) && saved.countries.every(c => /^[A-Z]{2}$/.test(c)) && saved.timezone) {
    new Intl.DateTimeFormat('ja', { timeZone: saved.timezone });
    settings = { ...saved, countries: [...new Set(['JP', ...saved.countries])] };
  }
} catch { /* 保存領域が使えない場合は初期設定で起動する */ }
let state = { data: null, busy: false, remainingMs: 0 };
let blockedUntil = 0;
let refreshing = false;
let serverReady = false;
let draftCountries = new Set();
let supportedCountries = null;
let connectionNotice = '';
let localMessage = '';
const names = new Intl.DisplayNames(['ja'], { type: 'region' });
const countryName = code => names.of(code) || code;
const starterCodes = 'JP US GB DE FR KR CN TW HK AU NZ CA IT ES PT AT BE CH CZ DK FI GR HU IE IS MX NL NO PL RO SE SG SK SI BR AR CL CO ZA TR UA'.split(' ');
const weekdays = ['日', '月', '火', '水', '木', '金', '土'];
const escapeHtml = value => String(value).replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]));
function dateInZone(timezone) { return new Intl.DateTimeFormat('en-CA', { timeZone: timezone, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date()); }
function datesForWeek(timezone) {
  const date = new Date(`${dateInZone(timezone)}T12:00:00Z`);
  date.setUTCDate(date.getUTCDate() - date.getUTCDay());
  return Array.from({ length: 7 }, (_, i) => { const day = new Date(date); day.setUTCDate(day.getUTCDate() + i); return day.toISOString().slice(0, 10); });
}
function formatDate(date) { const [year, month, day] = date.split('-'); return `${year}年${Number(month)}月${Number(day)}日`; }
function formatTime(value) { return new Date(value).toLocaleString('ja-JP', { timeZone: activeSettings().timezone, hour12: false }); }
function activeSettings() { return state.data?.settings || settings; }
function sameSettings(a, b) { return a.name === b.name && a.latitude === b.latitude && a.longitude === b.longitude && a.timezone === b.timezone && [...a.countries].sort().join() === [...b.countries].sort().join(); }
function describeWeather(code) {
  if (code === null || code === undefined) return ['—', '天気データなし'];
  if (code === 0) return ['☀️', '晴れ'];
  if (code === 1) return ['🌤️', 'おおむね晴れ'];
  if (code === 2) return ['⛅', '晴れ時々曇り'];
  if (code === 3) return ['☁️', '曇り'];
  if ([45, 48].includes(code)) return ['🌫️', '霧'];
  if ([51, 53, 55, 56, 57].includes(code)) return ['🌦️', '霧雨'];
  if ([61, 63, 65, 66, 67, 80, 81, 82].includes(code)) return ['🌧️', '雨'];
  if ([71, 73, 75, 77, 85, 86].includes(code)) return ['🌨️', '雪'];
  if ([95, 96, 99].includes(code)) return ['⛈️', '雷雨'];
  return ['—', '天気コード不明'];
}
function value(number, suffix = '') { return Number.isFinite(number) ? `${Math.round(number)}${suffix}` : '—'; }
function holidayHtml(holiday) {
  const region = holiday.global === false ? `一部地域${holiday.counties?.length ? `: ${holiday.counties.join(', ')}` : ''}` : '';
  return `<div class="holiday ${holiday.country !== 'JP' ? 'foreign' : ''}"><span class="holiday-country">${escapeHtml(countryName(holiday.country))}${region ? ` · ${escapeHtml(region)}` : ''}</span>${escapeHtml(holiday.name)}</div>`;
}
function renderNotice() {
  const messages = [];
  if (connectionNotice) messages.push(connectionNotice);
  if (localMessage) messages.push(localMessage);
  if (refreshing || state.busy) messages.push('天気と祝日を取得しています…');
  if (state.data) {
    if (!sameSettings(settings, state.data.settings)) messages.push(`設定を変更しました。現在は取得済みの「${state.data.settings.name}」の情報を表示しています。次回取得時に新しい設定を反映します。`);
    if (datesForWeek(activeSettings().timezone)[0] !== state.data.dates[0]) messages.push('週が切り替わりました。「情報取得」を押して今週の情報を取得してください。');
    messages.push(...state.data.errors);
    if (state.data.errors.length) messages.push('取得できなかった項目は未取得として表示しています。再取得は3時間後に可能です。');
  }
  $('notice').textContent = messages.join('\n');
  $('notice').className = `notice${messages.length ? ' visible' : ''}${connectionNotice || state.data?.errors.length ? ' warning' : ''}`;
}
function render() {
  const selected = activeSettings();
  const dates = datesForWeek(selected.timezone);
  const today = dateInZone(selected.timezone);
  const data = state.data;
  $('location-name').textContent = selected.name;
  $('timezone').textContent = selected.timezone;
  $('week-label').textContent = `${formatDate(dates[0])} − ${formatDate(dates[6])}`;
  $('country-label').textContent = `日曜日始まり · ${selected.countries.map(countryName).join(' / ')}`;
  $('calendar').innerHTML = dates.map((date, i) => {
    const weather = data?.weather[date];
    const holidays = data?.holidays.filter(h => h.date === date) || [];
    const [icon, description] = describeWeather(weather?.code);
    const holidayFailed = !data || !data.dates.includes(date) || data.errors.some(e => e.includes('の祝日を取得できません'));
    return `<article class="day ${i === 0 ? 'sunday' : i === 6 ? 'saturday' : ''} ${date === today ? 'today' : ''}" aria-label="${formatDate(date)} ${weekdays[i]}曜日${date === today ? ' 今日' : ''}">
      <div class="day-name">${weekdays[i]}曜日${date === today ? '<span class="today-tag">今日</span>' : ''}</div>
      <div class="date"><time datetime="${date}">${Number(date.slice(8))}</time><small>${Number(date.slice(5, 7))}月</small></div>
      <div class="weather">${weather ? `<span class="weather-icon" aria-hidden="true">${icon}</span><div class="weather-name">${description}</div><div class="temperature"><span class="high" aria-label="最高気温">${value(weather.max, '°')}</span><span class="separator">/</span><span class="low" aria-label="最低気温">${value(weather.min, '°')}</span></div><div class="rain">降水確率 ${value(weather.rain, '%')}</div>` : '<div class="empty-weather">☁<br>天気は未取得</div>'}</div>
      <div class="day-holidays">${holidays.map(holidayHtml).join('')}${!holidays.length || holidayFailed ? `<div class="no-holiday">${holidayFailed ? '祝日は未取得あり' : '祝日なし'}</div>` : ''}</div></article>`;
  }).join('');
  const holidays = data?.holidays.filter(h => dates.includes(h.date)) || [];
  $('holiday-list').innerHTML = holidays.length ? holidays.map(h => `<div class="holiday-row"><time datetime="${h.date}">${Number(h.date.slice(5, 7))}/${Number(h.date.slice(8))}</time><div>${escapeHtml(h.name)} <small>${escapeHtml(countryName(h.country))}${h.global === false ? ' · 一部地域' : ''}</small></div></div>`).join('') : escapeHtml(!data ? '「情報取得」を押すと、この週の祝日を表示します。' : !data.dates.includes(dates[0]) ? '今週の祝日は未取得です。' : data.errors.some(e => e.includes('の祝日を取得できません')) ? '祝日を取得できなかった国があります。上のメッセージを確認してください。' : '選択した国に今週の祝日はありません。');
  $('last-updated').textContent = data ? `最終取得: ${formatTime(data.fetchedAt)} (${selected.timezone})` : '未取得';
  renderNotice();
  tick();
}
function tick() {
  const seconds = Math.max(0, Math.ceil((blockedUntil - performance.now()) / 1000));
  $('refresh').disabled = !serverReady || refreshing || state.busy || seconds > 0;
  $('refresh').textContent = refreshing || state.busy ? '取得中…' : '↻ 情報取得';
  $('cooldown').textContent = seconds > 0 ? `次の取得まで ${String(Math.floor(seconds / 3600)).padStart(2, '0')}:${String(Math.floor(seconds / 60) % 60).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}` : !serverReady ? 'サーバーへの接続を確認中' : 'ボタンを押すと最新情報を取得します';
}
function applyStatus(body) {
  state = body;
  blockedUntil = performance.now() + body.remainingMs;
  if (body.data?.countries) supportedCountries = body.data.countries.map(c => c.countryCode);
  render();
}
async function syncStatus() {
  if (refreshing) return;
  try {
    const response = await fetch('/api/status', { signal: AbortSignal.timeout(10000), cache: 'no-store' });
    if (!response.ok) throw new Error();
    const body = await response.json();
    serverReady = true;
    connectionNotice = '';
    if (!refreshing) applyStatus(body);
  } catch {
    serverReady = false;
    connectionNotice = 'サーバーに接続できません。起動状態を確認してください。接続は定期的に再確認します。';
    render();
  }
}
$('refresh').addEventListener('click', async () => {
  if ($('refresh').disabled) return;
  refreshing = true;
  localMessage = '';
  render();
  try {
    const response = await fetch('/api/refresh', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(settings) });
    const body = await response.json();
    if (!response.ok && response.status !== 429) throw new Error(body.error || '取得に失敗しました。');
    refreshing = false;
    connectionNotice = '';
    if (response.status === 429) localMessage = body.error;
    applyStatus(body);
  } catch (error) {
    refreshing = false;
    connectionNotice = `取得結果を確認できませんでした: ${error.message}`;
    await syncStatus();
    localMessage = `取得結果を確認できませんでした: ${error.message}。表示中の情報と取得制限を確認してください。`;
    render();
  }
});
function fillRegion(region) {
  $('region-name').value = region.name;
  $('latitude').value = region.latitude;
  $('longitude').value = region.longitude;
  $('region-timezone').value = region.timezone;
}
function renderCountries() {
  const filter = $('country-search').value.trim().toLocaleLowerCase();
  const codes = [...new Set(['JP', ...(supportedCountries || starterCodes), ...draftCountries])];
  codes.sort((a, b) => a === 'JP' ? -1 : b === 'JP' ? 1 : countryName(a).localeCompare(countryName(b), 'ja'));
  $('countries').innerHTML = codes.filter(code => `${code} ${countryName(code)}`.toLocaleLowerCase().includes(filter)).map(code => `<label class="country-choice"><input type="checkbox" value="${code}" ${draftCountries.has(code) ? 'checked' : ''} ${code === 'JP' ? 'disabled' : ''}>${escapeHtml(countryName(code))} (${code})</label>`).join('') || '<p class="field-help">該当する国はありません。</p>';
  $('country-hint').textContent = supportedCountries ? 'Nager.Dateの対応国一覧です。日本は常に取得対象です。' : '初回取得前は主要国を表示します。未対応の国は取得時にエラーを表示します。初回取得後に対応国一覧を反映します。';
}
$('countries').addEventListener('change', event => { if (event.target.checked) draftCountries.add(event.target.value); else draftCountries.delete(event.target.value); });
$('country-search').addEventListener('input', renderCountries);
$('settings-open').addEventListener('click', () => {
  fillRegion(settings);
  $('preset').value = Object.entries(presets).find(([, p]) => p[0] === settings.name && p[1] === settings.latitude && p[2] === settings.longitude && p[3] === settings.timezone)?.[0] || 'custom';
  draftCountries = new Set(settings.countries);
  $('country-search').value = '';
  $('settings-error').textContent = '';
  renderCountries();
  $('settings-dialog').showModal();
});
$('preset').addEventListener('change', () => {
  const preset = presets[$('preset').value];
  if (preset) fillRegion({ name: preset[0], latitude: preset[1], longitude: preset[2], timezone: preset[3] });
});
['region-name', 'latitude', 'longitude', 'region-timezone'].forEach(id => $(id).addEventListener('input', () => { $('preset').value = 'custom'; }));
['settings-close', 'settings-cancel'].forEach(id => $(id).addEventListener('click', () => $('settings-dialog').close()));
$('settings-form').addEventListener('submit', event => {
  event.preventDefault();
  const timezone = $('region-timezone').value.trim();
  try { new Intl.DateTimeFormat('ja', { timeZone: timezone }); } catch { $('settings-error').textContent = '有効なタイムゾーンを入力してください。例: Asia/Tokyo'; return; }
  if (!$('region-name').value.trim()) { $('settings-error').textContent = '地域名を入力してください。'; return; }
  settings = { name: $('region-name').value.trim(), latitude: Number($('latitude').value), longitude: Number($('longitude').value), timezone, countries: [...draftCountries] };
  try { localStorage.setItem('weekly-calendar-settings', JSON.stringify(settings)); localMessage = '設定を保存しました。次の「情報取得」で反映します。'; }
  catch { localMessage = '設定を変更しました。ブラウザの保存領域が使えないため、再読み込みすると設定が戻ります。'; }
  $('settings-dialog').close();
  render();
});
document.addEventListener('visibilitychange', () => { if (!document.hidden) syncStatus(); });
setInterval(tick, 1000);
setInterval(() => { render(); syncStatus(); }, 30000);
render();
syncStatus();
