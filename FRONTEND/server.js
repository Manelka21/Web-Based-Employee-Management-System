// Zero-dependency static file server for local development.
//   node server.js            -> http://localhost:5173
//   PORT=3000 node server.js  -> http://localhost:3000
// The backend only accepts these two origins (CORS), so keep one of them.

const http = require("http");
const fs = require("fs");
const path = require("path");

const PORT = Number(process.env.PORT) || 5173;
const ROOT = __dirname;

const TYPES = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".ico": "image/x-icon",
  ".json": "application/json; charset=utf-8",
  ".webmanifest": "application/manifest+json",
};

const server = http.createServer((req, res) => {
  const urlPath = decodeURIComponent(new URL(req.url, `http://localhost:${PORT}`).pathname);
  let filePath = path.normalize(path.join(ROOT, urlPath));

  // Never serve anything outside this folder
  if (!filePath.startsWith(ROOT)) {
    res.writeHead(403).end("Forbidden");
    return;
  }

  fs.stat(filePath, (err, stat) => {
    if (!err && stat.isDirectory()) filePath = path.join(filePath, "index.html");
    fs.readFile(filePath, (readErr, data) => {
      if (readErr) {
        // Unknown paths fall back to the app (routing is hash-based, so this is just a safety net)
        if (!path.extname(urlPath)) {
          fs.readFile(path.join(ROOT, "index.html"), (e, html) => {
            if (e) return res.writeHead(500).end("index.html missing");
            res.writeHead(200, { "Content-Type": TYPES[".html"] }).end(html);
          });
          return;
        }
        res.writeHead(404).end("Not found");
        return;
      }
      res.writeHead(200, {
        "Content-Type": TYPES[path.extname(filePath).toLowerCase()] || "application/octet-stream",
        "Cache-Control": "no-cache",
      });
      res.end(data);
    });
  });
});

server.listen(PORT, () => {
  console.log(`LankaTech EMS frontend running at http://localhost:${PORT}`);
  console.log("API expected at http://localhost:8080/api (edit js/config.js to change)");
});
