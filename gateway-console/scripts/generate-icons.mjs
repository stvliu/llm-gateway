import { Resvg } from '@resvg/resvg-js';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const publicDir = path.join(__dirname, '..', 'public');
const assetsDir = path.join(__dirname, '..', 'src', 'assets', 'images');

// 从文件读取 SVG 内容
const svgPath = path.join(assetsDir, 'codingas-logo-transparent.svg');
const svgContent = fs.readFileSync(svgPath, 'utf-8');

// 需要生成的尺寸
const sizes = [
  { name: 'favicon-16x16.png', size: 16 },
  { name: 'favicon-32x32.png', size: 32 },
  { name: 'favicon-48x48.png', size: 48 },
  { name: 'favicon-64x64.png', size: 64 },
  { name: 'favicon-128x128.png', size: 128 },
  { name: 'favicon-192x192.png', size: 192 },
  { name: 'favicon-512x512.png', size: 512 },
  { name: 'favicon-180x180.png', size: 180 },
  { name: 'apple-touch-icon.png', size: 180 },
];

function generatePng(svgString, size) {
  const opts = {
    fitTo: {
      mode: 'width',
      value: size,
    },
  };
  const resvg = new Resvg(svgString, opts);
  const pngData = resvg.render();
  return pngData.asPng();
}

async function generateIcons() {
  console.log('开始生成图标...');

  // 确保 public 目录存在
  if (!fs.existsSync(publicDir)) {
    fs.mkdirSync(publicDir, { recursive: true });
  }

  for (const { name, size } of sizes) {
    const outputPath = path.join(publicDir, name);
    const pngBuffer = generatePng(svgContent, size);
    fs.writeFileSync(outputPath, pngBuffer);
    console.log(`生成: ${name} (${size}x${size})`);
  }

  // 生成 favicon.ico (使用 256x256 PNG)
  const icoPath = path.join(publicDir, 'favicon.ico');
  const icoPngBuffer = generatePng(svgContent, 256);
  fs.writeFileSync(icoPath, icoPngBuffer);
  console.log('生成: favicon.ico (256x256)');

  console.log('全部完成!');
}

generateIcons().catch(console.error);
