export const INTERVAL_MS = 3 * 60 * 60 * 1000;

export function dateInZone(date, timeZone) {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone, year: 'numeric', month: '2-digit', day: '2-digit'
  }).format(date);
}

export function weekDates(now, timeZone) {
  const day = new Date(`${dateInZone(now, timeZone)}T12:00:00Z`);
  day.setUTCDate(day.getUTCDate() - day.getUTCDay());
  return Array.from({ length: 7 }, (_, i) => {
    const date = new Date(day);
    date.setUTCDate(day.getUTCDate() + i);
    return date.toISOString().slice(0, 10);
  });
}

export function validateSettings(input) {
  if (!input || typeof input !== 'object') throw new Error('設定を確認してください。');
  const { name, latitude, longitude, timezone, countries } = input;
  if (typeof name !== 'string' || !name.trim() || name.length > 100) throw new Error('地域名を入力してください（100文字以内）。');
  if (typeof latitude !== 'number' || !Number.isFinite(latitude) || Math.abs(latitude) > 90) throw new Error('緯度は -90 ～ 90 で入力してください。');
  if (typeof longitude !== 'number' || !Number.isFinite(longitude) || Math.abs(longitude) > 180) throw new Error('経度は -180 ～ 180 で入力してください。');
  try {
    if (typeof timezone !== 'string' || !timezone) throw new Error();
    new Intl.DateTimeFormat('ja', { timeZone: timezone });
  } catch { throw new Error('有効なタイムゾーンを指定してください。例: Asia/Tokyo'); }
  if (!Array.isArray(countries) || countries.length > 250 || countries.some(code => typeof code !== 'string' || !/^[A-Z]{2}$/.test(code))) throw new Error('国は2文字の国コードで指定してください。');
  return { name: name.trim(), latitude, longitude, timezone, countries: [...new Set(['JP', ...countries])] };
}

async function jsonFrom(url, fetcher) {
  const response = await fetcher(url, { signal: AbortSignal.timeout(15000), headers: { Accept: 'application/json' } });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
}

export function createCalendarService({ fetcher = fetch, now = () => new Date(), monotonic = () => performance.now() } = {}) {
  let deadline = -Infinity;
  let busy = false;
  let data = null;
  let lastAttempt = null;
  function status() {
    const remainingMs = Math.max(0, Math.ceil(deadline - monotonic()));
    return { busy, remainingMs, nextAllowedAt: remainingMs ? new Date(now().getTime() + remainingMs).toISOString() : null, lastAttempt, data };
  }
  async function refresh(input) {
    const settings = validateSettings(input);
    if (busy || monotonic() < deadline) return { code: 429, body: { ...status(), error: '取得間隔は3時間です。次の取得までお待ちください。' } };
    busy = true;
    deadline = monotonic() + INTERVAL_MS;
    const requestedAt = now();
    lastAttempt = requestedAt.toISOString();
    const dates = weekDates(requestedAt, settings.timezone);
    const years = [...new Set(dates.map(date => date.slice(0, 4)))];
    const result = { settings, dates, fetchedAt: lastAttempt, weather: {}, holidays: [], errors: [], countries: null };
    const tasks = [
      async () => {
        try {
          const params = new URLSearchParams({ latitude: settings.latitude, longitude: settings.longitude, timezone: settings.timezone,
            daily: 'weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max', start_date: dates[0], end_date: dates[6] });
          const body = await jsonFrom(`https://api.open-meteo.com/v1/forecast?${params}`, fetcher);
          const daily = body.daily;
          if (!daily || !Array.isArray(daily.time) || !Array.isArray(daily.weather_code) || dates.some(date => !daily.time.includes(date))) throw new Error('週間データが不完全です');
          daily.time.forEach((date, i) => { result.weather[date] = { code: daily.weather_code[i], max: daily.temperature_2m_max?.[i] ?? null, min: daily.temperature_2m_min?.[i] ?? null, rain: daily.precipitation_probability_max?.[i] ?? null }; });
        } catch (error) { result.errors.push(`天気を取得できませんでした (${error.message})。`); }
      },
      async () => {
        try {
          const countries = await jsonFrom('https://date.nager.at/api/v3/AvailableCountries', fetcher);
          if (!Array.isArray(countries) || countries.some(c => !/^[A-Z]{2}$/.test(c.countryCode))) throw new Error('応答形式が不正です');
          result.countries = countries;
        } catch (error) { result.errors.push(`対応国一覧を取得できませんでした (${error.message})。祝日は国ごとに取得を試みています。`); }
      },
      ...settings.countries.flatMap(country => years.map(year => async () => {
        try {
          const holidays = await jsonFrom(`https://date.nager.at/api/v3/PublicHolidays/${year}/${country}`, fetcher);
          if (!Array.isArray(holidays) || holidays.some(h => typeof h.date !== 'string' || typeof h.localName !== 'string')) throw new Error('応答形式が不正です');
          result.holidays.push(...holidays.filter(h => dates.includes(h.date) && h.types?.includes('Public')).map(h => ({ date: h.date, name: h.localName, englishName: h.name, country, global: h.global, counties: h.counties })));
        } catch (error) { result.errors.push(`${country} (${year}年) の祝日を取得できませんでした (${error.message})。`); }
      }))
    ];
    let cursor = 0;
    try {
      await Promise.all(Array.from({ length: Math.min(6, tasks.length) }, async () => {
        while (cursor < tasks.length) { const task = tasks[cursor++]; await task(); }
      }));
      result.holidays.sort((a, b) => a.date.localeCompare(b.date) || a.country.localeCompare(b.country));
      data = result;
    } finally { busy = false; }
    return { code: 200, body: status() };
  }
  return { status, refresh };
}
