const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const morgan = require('morgan');

const TARGET = 'http://localhost:4250/api'; // Manifest API écoute ici
const app = express();

// Debug middleware AVANT tout
app.use((req, res, next) => {
  console.log('⚡ Request received:', req.method, req.path);
  next();
});

app.use(morgan('dev'));

// Proxy : transforme /api/* en /endpoints/* pour les custom endpoints de Manifest
console.log('🔧 Setting up proxy middleware for /api');
app.use(
  createProxyMiddleware({
    target: TARGET,
    changeOrigin: true,
    logLevel: 'debug',
    // Filter: only proxy requests starting with /api
    filter: (pathname, req) => {
      const shouldProxy = pathname.startsWith('/api');
      console.log(`🔍 Filter check: ${pathname} → ${shouldProxy ? 'PROXY' : 'SKIP'}`);
      return shouldProxy;
    },
    pathRewrite: {
      '^/api': '/endpoints/api', // /api/auth/login -> /endpoints/api/auth/login
    },
    onProxyReq: (pReq, req) => {
      console.log('🔵 Proxy request:', req.method, req.path, '→', pReq.path);
      if (req.headers.cookie) pReq.setHeader('cookie', req.headers.cookie);
    },
    onProxyRes: (pRes, req, res) => {
      console.log('🟢 Proxy response:', pRes.statusCode);
      if (pRes.headers['set-cookie'])
        res.setHeader('set-cookie', pRes.headers['set-cookie']);
    },
    onError: (err, req, res) => {
      console.error('🔴 Proxy error:', err.message);
      res.status(500).json({ message: 'Proxy error', error: err.message });
    },
  })
);

app.listen(4100, () =>
  console.log(
    '🔄 Proxy sur http://localhost:4100 → Manifest http://localhost:4250'
  )
);
