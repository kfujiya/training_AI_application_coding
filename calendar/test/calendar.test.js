import test from 'node:test';
import assert from 'node:assert/strict';
import { createCalendarService, INTERVAL_MS, validateSettings, weekDates } from '../calendar.js';
import { createApp } from '../server.js';

const settings = { name: '東京', latitude: 35.6762, longitude: 139.6503, timezone: 'Asia/Tokyo', countries: ['JP', 'US', 'JP'] };
const instant = new Date('2026-12-31T03:00:00Z');
const dates = weekDates(instant, 'Asia/Tokyo');
function fakeFetch(calls, failWeather = false) {
  return async url => {
    calls.push(String(url));
    if (String(url).includes('open-meteo')) {
      if (failWeather) throw new Error('network unavailable');
      return { ok: true, json: async () => ({ daily: { time: dates, weather_code: dates.map(() => 0), temperature_2m_max: dates.map(() => 15), temperature_2m_min: dates.map(() => 5), precipitation_probability_max: dates.map(() => 10) } }) };
    }
    if (String(url).endsWith('AvailableCountries')) return { ok: true, json: async () => [{ countryCode: 'JP', name: 'Japan' }, { countryCode: 'US', name: 'United States' }] };
    const country = String(url).split('/').at(-1);
    const year = String(url).split('/').at(-2);
    return { ok: true, json: async () => year === '2027' ? [
      { date: '2027-01-01', localName: country === 'JP' ? '元日' : "New Year's Day", countryCode: country, global: true, types: ['Public'] },
      { date: '2027-01-02', localName: 'Bank only', countryCode: country, types: ['Bank'] },
      { date: '2027-02-01', localName: 'Outside week', countryCode: country, types: ['Public'] }
    ] : [] };
  };
}

test('Sunday start, Saturday end, leap day and time zone boundaries', () => {
  assert.deepEqual(dates, ['2026-12-27', '2026-12-28', '2026-12-29', '2026-12-30', '2026-12-31', '2027-01-01', '2027-01-02']);
  assert.equal(weekDates(new Date('2026-10-24T16:00:00Z'), 'Asia/Tokyo')[0], '2026-10-25');
  assert.equal(weekDates(new Date('2026-10-24T16:00:00Z'), 'America/New_York')[0], '2026-10-18');
  assert.ok(weekDates(new Date('2028-03-01T00:00:00Z'), 'Asia/Tokyo').includes('2028-02-29'));
});

test('settings always include Japan, deduplicate, and reject invalid data', () => {
  assert.deepEqual(validateSettings({ ...settings, countries: ['US', 'US'] }).countries, ['JP', 'US']);
  for (const change of [{ latitude: 91 }, { longitude: -181 }, { latitude: null }, { timezone: 'invalid' }, { countries: ['../US'] }, { name: '  ' }]) {
    assert.throws(() => validateSettings({ ...settings, ...change }));
  }
});

test('no automatic external calls; merges both years and countries; weather covers Sunday through Saturday', async () => {
  const calls = [];
  const service = createCalendarService({ fetcher: fakeFetch(calls), now: () => instant });
  service.status();
  assert.equal(calls.length, 0);
  const { code, body } = await service.refresh(settings);
  assert.equal(code, 200);
  assert.equal(calls.length, 6);
  assert.equal(body.data.holidays.length, 2);
  assert.deepEqual(body.data.holidays.map(h => h.country), ['JP', 'US']);
  assert.equal(Object.keys(body.data.weather).length, 7);
  const weatherUrl = new URL(calls.find(url => url.includes('open-meteo')));
  assert.equal(weatherUrl.searchParams.get('start_date'), '2026-12-27');
  assert.equal(weatherUrl.searchParams.get('end_date'), '2027-01-02');
  assert.deepEqual(body.data.errors, []);
});

test('three hour server cooldown survives settings changes and ignores wall clock changes; restart resets it', async () => {
  let mono = 100;
  let clock = instant;
  const calls = [];
  const options = { fetcher: fakeFetch(calls), now: () => clock, monotonic: () => mono };
  const service = createCalendarService(options);
  await service.refresh(settings);
  assert.equal(service.status().remainingMs, INTERVAL_MS);
  clock = new Date('2030-01-01T00:00:00Z');
  mono += INTERVAL_MS - 1;
  assert.equal((await service.refresh({ ...settings, name: '大阪' })).code, 429);
  assert.equal(calls.length, 6);
  assert.equal(service.status().remainingMs, 1);
  mono += 1;
  assert.equal((await service.refresh(settings)).code, 200);
  assert.equal(createCalendarService(options).status().remainingMs, 0);
});

test('simultaneous requests are rejected before any duplicate external call', async () => {
  let release;
  const gate = new Promise(resolve => { release = resolve; });
  const calls = [];
  const baseFetch = fakeFetch(calls);
  const service = createCalendarService({ fetcher: async (...args) => { await gate; return baseFetch(...args); }, now: () => instant });
  const first = service.refresh(settings);
  assert.equal(service.status().busy, true);
  assert.equal((await service.refresh(settings)).code, 429);
  release();
  await first;
  assert.equal(calls.length, 6);
  assert.equal(service.status().busy, false);
});

test('partial and total external failures preserve cooldown and expose unavailable data', async () => {
  const service = createCalendarService({ fetcher: fakeFetch([], true), now: () => instant });
  const { body } = await service.refresh(settings);
  assert.deepEqual(body.data.weather, {});
  assert.equal(body.data.holidays.length, 2);
  assert.equal(body.data.errors.length, 1);
  assert.equal((await service.refresh(settings)).code, 429);
  const offline = createCalendarService({ fetcher: async () => { throw new Error('offline'); }, now: () => instant });
  const result = await offline.refresh(settings);
  assert.equal(result.body.data.errors.length, 6);
  assert.equal(result.body.busy, false);
  assert.ok(result.body.remainingMs > 0);
});

test('invalid settings do not consume cooldown', async () => {
  const service = createCalendarService();
  await assert.rejects(service.refresh({ ...settings, latitude: 200 }));
  assert.equal(service.status().remainingMs, 0);
});

test('HTTP routes share cooldown across clients; static files and bad requests', async t => {
  const app = createApp(createCalendarService({ fetcher: fakeFetch([]), now: () => instant }));
  await new Promise(resolve => app.listen(0, '127.0.0.1', resolve));
  t.after(() => new Promise(resolve => app.close(resolve)));
  const base = `http://127.0.0.1:${app.address().port}`;
  for (const path of ['/', '/app.js', '/style.css', '/api/status']) assert.equal((await fetch(base + path)).status, 200);
  assert.equal((await fetch(base + '/server.js')).status, 404);
  assert.equal((await fetch(base + '/api/refresh', { method: 'POST', body: 'bad json' })).status, 400);
  assert.equal((await fetch(base + '/api/refresh', { method: 'POST', headers: { Origin: 'https://external.example' }, body: JSON.stringify(settings) })).status, 403);
  const first = await fetch(base + '/api/refresh', { method: 'POST', body: JSON.stringify(settings) });
  assert.equal(first.status, 200);
  const second = await fetch(base + '/api/refresh', { method: 'POST', body: JSON.stringify(settings) });
  assert.equal(second.status, 429);
  assert.ok(Number(second.headers.get('Retry-After')) > 0);
  assert.ok((await (await fetch(base + '/api/status')).json()).remainingMs > 0);
});
