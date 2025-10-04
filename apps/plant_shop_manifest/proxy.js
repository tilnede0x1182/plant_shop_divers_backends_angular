const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const morgan = require('morgan');

const TARGET = 'http://localhost:4120'; // Manifest écoute ici
const app = express();
app.use(morgan('dev'));

function rewritePath(path) {
  // Cas /api/auth/...
  if (path === '/api/auth/login') return '/api/auth/users/login';
  if (path === '/api/auth/register') return '/api/auth/users/signup';
  if (path === '/api/auth/logout') return '/api/auth/users/logout';
  if (path === '/api/auth/me') return '/api/auth/users/me';

  // Cas front natif (/auth/...)
  if (path.startsWith('/auth/')) {
    const map = {
      '/auth/login': '/api/auth/users/login',
      '/auth/register': '/api/auth/users/signup',
      '/auth/logout': '/api/auth/users/logout',
      '/auth/me': '/api/auth/users/me',
    };
    return map[path] || path;
  }

  // Admin
  if (path.startsWith('/admin/')) {
    return '/api' + path.slice('/admin'.length);
  }

  // Tout le reste
  if (path.startsWith('/api/')) {
    // si c’est déjà /api/auth/... ils auront été pris ci-dessus
    return path;
  }
  return '/api' + path;
}


app.use(
  '*',
  createProxyMiddleware({
    target: TARGET,
    changeOrigin: true,
    pathRewrite: (_, req) => rewritePath(req.originalUrl),
    onProxyReq: (pReq, req) => {
      if (req.headers.cookie) pReq.setHeader('cookie', req.headers.cookie);
    },
    onProxyRes: (pRes, req, res) => {
      if (pRes.headers['set-cookie'])
        res.setHeader('set-cookie', pRes.headers['set-cookie']);
    },
  })
);

app.listen(4100, () =>
  console.log(
    'Proxy sur http://localhost:4100  →  Manifest http://localhost:4120'
  )
);
