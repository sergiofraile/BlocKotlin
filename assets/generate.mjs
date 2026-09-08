// Regenerates the BlocKotlin brand assets (banner, logo mark, favicon, touch icon).
//
//   npm install --no-save sharp png-to-ico
//   node assets/generate.mjs assets/out
//
// Then copy from assets/out/:
//   banner.png, logo-mark.svg        -> assets/
//   logo-mark.svg (as logo-icon.svg) -> bloc/dokka/logo-icon.svg
//   favicon.ico, apple-touch-icon.png -> bloc/dokka/
//
// The logo mark and colours are defined inline below; edit BIRD / BARS / DEFS to tweak.

import sharp from "sharp";
import pngToIco from "png-to-ico";
import { writeFileSync } from "fs";

// ---- Shared iconography (100 x 100 local box, centered ~50,50) ----------

// Minimal gull in flight.
const BIRD = `<path fill="#fff" d="M12 54 C27 33 41 33 50 49 C59 33 73 33 88 54 C73 45 60 47 50 60 C40 47 27 45 12 54 Z"/>`;

// Three rounded bars = the state stream.
const BARS = `<g fill="#fff">
  <rect x="27" y="31" width="30" height="11" rx="5.5"/>
  <rect x="27" y="45" width="46" height="11" rx="5.5"/>
  <rect x="27" y="59" width="22" height="11" rx="5.5"/>
</g>`;

const DEFS = `
  <radialGradient id="orange" cx="0.32" cy="0.26" r="0.95">
    <stop offset="0" stop-color="#FF9D4D"/><stop offset="0.55" stop-color="#FF7A2F"/><stop offset="1" stop-color="#EF5F1B"/>
  </radialGradient>
  <radialGradient id="purple" cx="0.72" cy="0.28" r="1">
    <stop offset="0" stop-color="#A97BFF"/><stop offset="0.5" stop-color="#7F52FF"/><stop offset="1" stop-color="#B31FE0"/>
  </radialGradient>`;

// two-circle mark, drawn around a translate() origin
function markGroup({ r = 104, off = 74 } = {}) {
  return `
    <circle cx="${off}" cy="0" r="${r}" fill="url(#purple)"/>
    <g transform="translate(${off} 0) translate(-50 -50)">${BARS}</g>
    <circle cx="${-off}" cy="0" r="${r}" fill="url(#orange)"/>
    <g transform="translate(${-off} 0) translate(-50 -50)">${BIRD}</g>`;
}

function markSVG(size) {
  const vb = 372;
  return `<svg xmlns="http://www.w3.org/2000/svg"${size ? ` width="${size}" height="${size}"` : ""} viewBox="0 0 ${vb} ${vb}">
  <defs>${DEFS}</defs>
  <g transform="translate(${vb / 2} ${vb / 2})">${markGroup()}</g>
</svg>`;
}

function bannerSVG() {
  const W = 1024, H = 622;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">
  <defs>
    ${DEFS}
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#1b1611"/><stop offset="0.45" stop-color="#13131d"/><stop offset="1" stop-color="#0c0d15"/>
    </linearGradient>
    <radialGradient id="glowO" cx="0.5" cy="0.5" r="0.5">
      <stop offset="0" stop-color="#FF7A2F" stop-opacity="0.42"/><stop offset="1" stop-color="#FF7A2F" stop-opacity="0"/>
    </radialGradient>
    <radialGradient id="glowP" cx="0.5" cy="0.5" r="0.5">
      <stop offset="0" stop-color="#7F52FF" stop-opacity="0.38"/><stop offset="1" stop-color="#7F52FF" stop-opacity="0"/>
    </radialGradient>
    <linearGradient id="rule" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="#EF5F1B"/><stop offset="1" stop-color="#7F52FF"/>
    </linearGradient>
  </defs>
  <rect width="${W}" height="${H}" fill="url(#bg)"/>
  <ellipse cx="325" cy="245" rx="440" ry="370" fill="url(#glowO)"/>
  <ellipse cx="720" cy="245" rx="440" ry="370" fill="url(#glowP)"/>
  <g transform="translate(${W / 2} 236) scale(1.12)">${markGroup()}</g>
  <text x="${W / 2}" y="452" text-anchor="middle" font-family="'Helvetica Neue', Arial, sans-serif" font-size="94" font-weight="800" letter-spacing="-2.5">
    <tspan fill="#8E6BFF">Bloc</tspan><tspan fill="#FF7A2F">Kotlin</tspan>
  </text>
  <text x="${W / 2}" y="503" text-anchor="middle" font-family="'Helvetica Neue', Arial, sans-serif" font-size="25" font-weight="500" fill="#9aa0ad" letter-spacing="0.4">
    Predictable state management for Kotlin · Inspired by BLoC
  </text>
  <rect x="0" y="${H - 8}" width="${W}" height="8" fill="url(#rule)"/>
</svg>`;
}

// ---- Render ------------------------------------------------------------
const outDir = process.argv[2] || ".";
const markStr = markSVG();

writeFileSync(`${outDir}/logo-mark.svg`, markStr);
writeFileSync(`${outDir}/banner.svg`, bannerSVG());
await sharp(Buffer.from(bannerSVG())).png().toFile(`${outDir}/banner.png`);
await sharp(Buffer.from(markStr)).resize(1024, 1024).png().toFile(`${outDir}/mark-1024.png`);
await sharp(Buffer.from(markStr)).resize(180, 180).png().toFile(`${outDir}/apple-touch-icon.png`);
await sharp(Buffer.from(markStr)).resize(512, 512).png().toFile(`${outDir}/icon-512.png`);

const icoPngs = [];
for (const s of [16, 32, 48, 64]) icoPngs.push(await sharp(Buffer.from(markStr)).resize(s, s).png().toBuffer());
writeFileSync(`${outDir}/favicon.ico`, await pngToIco(icoPngs));

// tiny preview strip of the favicon sizes
const strip = `<svg xmlns="http://www.w3.org/2000/svg" width="240" height="80" viewBox="0 0 240 80"><rect width="240" height="80" fill="#eee"/>
${[16,24,32,48,64].map((s,i)=>`<image x="${10+i*46}" y="${40-s/2}" width="${s}" height="${s}" href="data:image/svg+xml;base64,${Buffer.from(markStr).toString("base64")}"/>`).join("")}</svg>`;
await sharp(Buffer.from(strip)).png().toFile(`${outDir}/favicon-preview.png`);

console.log("done ->", outDir);
