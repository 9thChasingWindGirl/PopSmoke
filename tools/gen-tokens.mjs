#!/usr/bin/env node
/**
 * 令牌生成器：design/tokens.json → Compose 令牌
 *
 * 用法：node tools/gen-tokens.mjs [--check]
 *   --check  只校验，不写文件（CI 用；一旦有人手改生成物就会失败）
 *
 * 设计约束：
 *   1. 页面代码里不允许出现裸数值，一律引用生成的常量。
 *   2. rem 单位的数值统一乘以 base.rem（手机端 14px），否则与 Web 版尺寸对不上。
 *   3. 生成物带 DO-NOT-EDIT 头，被手改会被 --check 拦下。
 */
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, '..');
const TOKENS = resolve(root, 'design/tokens.json');
const OUT = resolve(
  root,
  'android-native/app/src/main/java/com/popsmoke/tracker/design/PopTokens.kt'
);

const tokens = JSON.parse(readFileSync(TOKENS, 'utf8'));
const CHECK_ONLY = process.argv.includes('--check');

const REM = tokens.base.rem;

const num = (v) => {
  const rounded = Math.round(v * 1000) / 1000;
  return Number.isInteger(rounded) ? String(rounded) : String(rounded);
};
const dp = (v) => `${num(v)}.dp`;
const sp = (rem) => `${num(Math.round(rem * REM * 100) / 100)}.sp`;
const em = (v) => `${num(v)}.em`;
const pascal = (s) => s.charAt(0).toUpperCase() + s.slice(1);
const color = (hex) => `Color(0xFF${hex.replace('#', '').toUpperCase()})`;

const lines = [];
const out = (s = '') => lines.push(s);

out('// GENERATED FILE — DO NOT EDIT.');
out('// 由 design/tokens.json 经 tools/gen-tokens.mjs 生成。改动请改 JSON 后重新生成。');
out('// 校验：node tools/gen-tokens.mjs --check');
out('');
out('package com.popsmoke.tracker.design');
out('');
out('import androidx.compose.animation.core.CubicBezierEasing');
out('import androidx.compose.ui.graphics.Color');
out('import androidx.compose.ui.unit.dp');
out('import androidx.compose.ui.unit.em');
out('import androidx.compose.ui.unit.sp');
out('');
out(`/** 设计令牌 v${tokens.meta.version} · 来源 ${tokens.meta.source} */`);

// ---------- 颜色 ----------
out('object PopColor {');
for (const [group, value] of Object.entries(tokens.color)) {
  if (typeof value === 'string') {
    out(`    val ${pascal(group)} = ${color(value)}`);
    continue;
  }
  out(`    object ${pascal(group)} {`);
  for (const [k, v] of Object.entries(value)) {
    // 以 _ 开头的是给读 JSON 的人看的注释，不生成常量
    if (k.startsWith('_')) continue;
    out(`        val ${pascal(k)} = ${color(v)}`);
  }
  out('    }');
}
out('}');
out('');

// ---------- 主题预设 ----------
out('data class PopThemePreset(');
out('    val id: String,');
out('    val color: Color,');
out('    val names: Map<String, String>,');
out(') {');
out('    fun name(lang: String): String = names[lang] ?: names["en"] ?: id');
out('}');
out('');
out('object PopPresets {');
out('    val All: List<PopThemePreset> = listOf(');
for (const p of tokens.presets) {
  const names = Object.entries(p)
    .filter(([k]) => !['id', 'color'].includes(k))
    .map(([k, v]) => `"${k}" to "${v}"`)
    .join(', ');
  out(`        PopThemePreset("${p.id}", ${color(p.color)}, mapOf(${names})),`);
}
out('    )');
out('');
out('    fun byId(id: String): PopThemePreset? = All.firstOrNull { it.id == id }');
out('    fun byColor(c: Color): PopThemePreset? = All.firstOrNull { it.color == c }');
out('}');
out('');

// ---------- 描边 / 圆角 / 阴影 ----------
out('object PopStroke {');
for (const [k, v] of Object.entries(tokens.stroke)) out(`    val ${pascal(k)} = ${dp(v)}`);
out('}');
out('');
out('object PopRadius {');
for (const [k, v] of Object.entries(tokens.radius)) out(`    val ${pascal(k)} = ${dp(v)}`);
out('}');
out('');
out('/** 硬阴影偏移量，全部零模糊。用矩形平移实现，禁止用模糊阴影。 */');
out('object PopShadow {');
for (const [k, v] of Object.entries(tokens.shadow)) out(`    val ${pascal(k)} = ${dp(v)}`);
out('}');
out('');

// ---------- 间距 ----------
out('object PopSpace {');
for (const [k, v] of Object.entries(tokens.space)) out(`    val S${k} = ${dp(v * REM)}`);
out('}');
out('');

// ---------- 字体 ----------
out('object PopTypeScale {');
for (const [k, v] of Object.entries(tokens.type.size)) out(`    val ${pascal(k)} = ${sp(v)}`);
out('}');
out('');
// 行高单列一个对象：Web 版每档字号自带 Tailwind 的配套行高（text-xl = 1.75rem），
// 和字号不是同一个倍数。Compose 默认按字体度量算，会比 Web 矮 10%~15%，
// 一屏下来就是几十 dp 的位置偏差，所以必须显式写进去。
out('object PopLineHeight {');
for (const [k, v] of Object.entries(tokens.type.lineHeight)) {
  if (k.startsWith('_')) continue;
  out(`    val ${pascal(k)} = ${sp(v)}`);
}
out('    fun of(size: androidx.compose.ui.unit.TextUnit) = when (size) {');
for (const k of Object.keys(tokens.type.lineHeight)) {
  if (k.startsWith('_')) continue;
  out(`        PopTypeScale.${pascal(k)} -> ${pascal(k)}`);
}
out('        else -> androidx.compose.ui.unit.TextUnit.Unspecified');
out('    }');
out('}');
out('');
out('object PopWeight {');
for (const [k, v] of Object.entries(tokens.type.weight)) out(`    const val ${pascal(k)} = ${v}`);
out('}');
out('');
out('object PopTracking {');
for (const [k, v] of Object.entries(tokens.type.tracking)) out(`    val ${pascal(k)} = ${em(v)}`);
out('}');
out('');

// ---------- 图标 / 版式 ----------
out('object PopIconSize {');
for (const [k, v] of Object.entries(tokens.icon)) out(`    val ${pascal(k)} = ${dp(v * REM)}`);
out('}');
out('');
out('object PopLayout {');
for (const [k, v] of Object.entries(tokens.layout)) {
  // 键名后缀决定单位：*Rem → 乘基准字号；*Px → 1:1 转 dp
  if (k.endsWith('Rem')) {
    out(`    val ${pascal(k.slice(0, -3))} = ${dp(v * REM)}`);
  } else if (k.endsWith('Px')) {
    out(`    val ${pascal(k.slice(0, -2))} = ${dp(v)}`);
  } else {
    throw new Error(`layout.${k} 缺少单位后缀（Rem / Px）`);
  }
}
out('}');
out('');

// ---------- 液态玻璃 ----------
// 后缀即单位：*Alpha → 0-1 透明度，*Px → dp，*Ms → 毫秒整数，其余按倍数生成 Float。
if (tokens.glass) {
  out('/** 液态玻璃主题参数。后缀即单位：Alpha 为透明度，Px 为 dp，Ms 为毫秒，其余为倍数。 */');
  out('object PopGlass {');
  for (const [k, v] of Object.entries(tokens.glass)) {
    if (k.startsWith('_')) continue;
    const name = pascal(k);
    if (k.endsWith('Px')) out(`    val ${name.slice(0, -2)} = ${dp(v)}`);
    else if (k.endsWith('Ms')) out(`    const val ${name.slice(0, -2)} = ${v}`);
    // Alpha 后缀保留：ShadowAlpha / ShadowLift 放一起才不会看错
    else if (k.endsWith('Alpha')) out(`    const val ${name} = ${num(v)}f`);
    else out(`    const val ${name} = ${num(v)}f`);
  }
  out('}');
  out('');
}

// ---------- 动效 ----------
out('object PopMotion {');
for (const [k, v] of Object.entries(tokens.motion)) {
  if (k === 'easing') continue;
  out(`    const val ${pascal(k)} = ${v}`);
}
out('');
for (const [k, v] of Object.entries(tokens.motion.easing)) {
  const f = v.map((x) => `${num(x)}f`).join(', ');
  out(`    val ${pascal(k)} = CubicBezierEasing(${f})`);
}
out('}');
out('');

const output = lines.join('\n');

if (CHECK_ONLY) {
  const current = existsSync(OUT) ? readFileSync(OUT, 'utf8') : '';
  if (current !== output) {
    console.error('✖ 令牌生成物与 design/tokens.json 不一致。请运行：node tools/gen-tokens.mjs');
    process.exit(1);
  }
  console.log('✔ 令牌生成物一致');
  process.exit(0);
}

writeFileSync(OUT, output, 'utf8');
console.log(`✔ 已生成 ${OUT.replace(root + '/', '')}（${output.split('\n').length} 行）`);
