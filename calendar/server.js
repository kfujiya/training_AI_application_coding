import http from 'node:http';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';
import { execFile } from 'node:child_process';
import { createCalendarService } from './calendar.js';

export function createApp(service = createCalendarService()) {
  return http.createServer(async (req, res) => {
    const send = (code, body) => { res.writeHead(code, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' }); res.end(JSON.stringify(body)); };
    try {
      if (req.url === '/api/status' && req.method === 'GET') return send(200, service.status());
      if (req.url === '/api/refresh' && req.method === 'POST') {
        if (req.headers.origin && req.headers.origin !== `http://${req.headers.host}`) return send(403, { error: 'この操作はアプリ画面から実行してください。' });
        let body = '';
        for await (const chunk of req) {
          body += chunk;
          if (Buffer.byteLength(body) > 16384) return send(413, { error: '設定データが大きすぎます。' });
        }
        let settings;
        try { settings = JSON.parse(body); } catch { return send(400, { error: '設定データの形式が不正です。' }); }
        let result;
        try { result = await service.refresh(settings); } catch (error) { return send(400, { error: error.message }); }
        if (result.code === 429) res.setHeader('Retry-After', Math.ceil(result.body.remainingMs / 1000));
        return send(result.code, result.body);
      }
      const files = { '/': ['index.html', 'text/html'], '/app.js': ['app.js', 'text/javascript'], '/style.css': ['style.css', 'text/css'] };
      const file = files[req.url];
      if (!file || req.method !== 'GET') return send(404, { error: '見つかりません。' });
      const content = await readFile(new URL(`./public/${file[0]}`, import.meta.url));
      res.writeHead(200, { 'Content-Type': `${file[1]}; charset=utf-8`, 'Cache-Control': 'no-cache', 'X-Content-Type-Options': 'nosniff' });
      res.end(content);
    } catch { if (!res.headersSent) send(500, { error: '処理に失敗しました。時間をおいて再度お試しください。' }); else res.end(); }
  });
}

if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
  const server = createApp();
  const port = Number(process.env.PORT || 3000);
  server.on('error', error => { console.error(`起動できませんでした: ${error.message}`); process.exitCode = 1; });
  server.listen(port, '127.0.0.1', () => {
    const url = `http://127.0.0.1:${server.address().port}`;
    console.log(`週間カレンダー: ${url}\n停止: Ctrl+C（再起動すると取得制限がリセットされます）`);
    if (process.argv.includes('--open')) {
      execFile('rundll32.exe', ['url.dll,FileProtocolHandler', url], { windowsHide: true }, error => {
        if (error) console.error(`ブラウザを開けませんでした。上記のURLをブラウザで開いてください: ${error.message}`);
      });
    }
  });
}
