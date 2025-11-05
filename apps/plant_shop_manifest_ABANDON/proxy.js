const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const morgan = require('morgan');

const TARGET = 'http://localhost:4250'; // Manifest écoute ici
const app = express();
app.use(morgan('dev'));

// Proxy : retire /api du chemin avant de rediriger vers Manifest
app.use(
  '/api',
  createProxyMiddleware({
    target: TARGET,
    changeOrigin: true,
    pathRewrite: {
      '^/api': '', // Retire /api du chemin
    },
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
    '🔄 Proxy sur http://localhost:4100 → Manifest http://localhost:4250'
  )
);
