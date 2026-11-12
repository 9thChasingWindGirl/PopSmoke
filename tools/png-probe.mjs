#!/usr/bin/env node
/**
 * 极简 PNG 探针：不用第三方依赖，读取像素做客观校验。
 * 用法：
 *   node tools/png-probe.mjs <file.png> --at 12,12 --row 300
 *
 * 为什么需要它：截图"看着像"没有意义，配色/边界/阴影偏移必须是可测量的。
 */
import { readFileSync } from 'node:fs';
import { inflateSync } from 'node:zlib';

export function decodePng(path) {
  const buf = readFileSync(path);
  if (buf.readUInt32BE(0) !== 0x89504e47) throw new Error('not a png');

  let offset = 8;
  let width = 0;
  let height = 0;
  let bitDepth = 0;
  let colorType = 0;
  let interlace = 0;
  const idat = [];

  while (offset < buf.length) {
    const length = buf.readUInt32BE(offset);
    const type = buf.toString('ascii', offset + 4, offset + 8);
    const data = buf.subarray(offset + 8, offset + 8 + length);
    if (type === 'IHDR') {
      width = data.readUInt32BE(0);
      height = data.readUInt32BE(4);
      bitDepth = data[8];
      colorType = data[9];
      interlace = data[12];
    } else if (type === 'IDAT') {
      idat.push(data);
    } else if (type === 'IEND') {
      break;
    }
    offset += 12 + length;
  }

  if (bitDepth !== 8) throw new Error(`unsupported bit depth ${bitDepth}`);
  if (interlace !== 0) throw new Error('interlaced png unsupported');
  const channels = colorType === 6 ? 4 : colorType === 2 ? 3 : colorType === 0 ? 1 : 0;
  if (!channels) throw new Error(`unsupported color type ${colorType}`);

  const raw = inflateSync(Buffer.concat(idat));
  const stride = width * channels;
  const out = Buffer.alloc(width * height * 4);
  let prev = Buffer.alloc(stride);
  let pos = 0;

  for (let y = 0; y < height; y++) {
    const filter = raw[pos++];
    const line = Buffer.from(raw.subarray(pos, pos + stride));
    pos += stride;

    for (let i = 0; i < stride; i++) {
      const a = i >= channels ? line[i - channels] : 0;
      const b = prev[i];
      const c = i >= channels ? prev[i - channels] : 0;
      switch (filter) {
        case 1: line[i] = (line[i] + a) & 0xff; break;
        case 2: line[i] = (line[i] + b) & 0xff; break;
        case 3: line[i] = (line[i] + ((a + b) >> 1)) & 0xff; break;
        case 4: {
          const p = a + b - c;
          const pa = Math.abs(p - a);
          const pb = Math.abs(p - b);
          const pc = Math.abs(p - c);
          const pred = pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
          line[i] = (line[i] + pred) & 0xff;
          break;
        }
        default: break;
      }
    }

    for (let x = 0; x < width; x++) {
      const s = x * channels;
      const d = (y * width + x) * 4;
      out[d] = line[s];
      out[d + 1] = channels >= 3 ? line[s + 1] : line[s];
      out[d + 2] = channels >= 3 ? line[s + 2] : line[s];
      out[d + 3] = channels === 4 ? line[s + 3] : 255;
    }
    prev = line;
  }

  return { width, height, data: out };
}

export function pixelAt(img, x, y) {
  const d = (y * img.width + x) * 4;
  return [img.data[d], img.data[d + 1], img.data[d + 2], img.data[d + 3]];
}

export const hex = ([r, g, b]) =>
  '#' + [r, g, b].map((v) => v.toString(16).padStart(2, '0').toUpperCase()).join('');

if (process.argv[1] && process.argv[1].endsWith('png-probe.mjs')) {
  const args = process.argv.slice(2);
  const file = args[0];
  if (!file) {
    console.error('usage: node tools/png-probe.mjs <file.png> [--at x,y]... [--row y]');
    process.exit(1);
  }
  const img = decodePng(file);
  console.log(`${file}\n  ${img.width} x ${img.height}`);

  for (let i = 1; i < args.length; i++) {
    if (args[i] === '--at') {
      const [x, y] = args[++i].split(',').map(Number);
      console.log(`  (${x},${y}) = ${hex(pixelAt(img, x, y))} a=${pixelAt(img, x, y)[3]}`);
    } else if (args[i] === '--row') {
      const y = Number(args[++i]);
      let prev = '';
      const runs = [];
      for (let x = 0; x < img.width; x++) {
        const h = hex(pixelAt(img, x, y));
        if (h !== prev) {
          runs.push(`${x}:${h}`);
          prev = h;
        }
      }
      console.log(`  row ${y}: ${runs.join(' ')}`);
    } else if (args[i] === '--col') {
      // 竖向扫描：元素高度、间距、偏移只能靠这一列一条条读出来
      const x = Number(args[++i]);
      let prev = '';
      const runs = [];
      for (let y = 0; y < img.height; y++) {
        const h = hex(pixelAt(img, x, y));
        if (h !== prev) {
          runs.push(`${y}:${h}`);
          prev = h;
        }
      }
      console.log(`  col ${x}: ${runs.join(' ')}`);
    }
  }
}
