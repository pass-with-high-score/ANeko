#!/usr/bin/env node
/**
 * Export strings that exist in values/strings.xml but are missing from one or
 * more values-<locale>/strings.xml files.
 *
 * Usage:
 *   node export_untranslated.cjs <res_dir> [-o out.csv] [--locales de,fr,ja]
 *
 * Writes locale,name,default_value rows — exactly the shape
 * import_translations.cjs reads back once a translated_value column is added.
 *
 * Strings marked translatable="false" are skipped, as are locales that have no
 * values-<locale> directory yet unless you name them with --locales.
 */

const fs = require('fs');
const path = require('path');
const { toCsv } = require('./csv.cjs');

/** Pull <string> entries out of a strings.xml without needing an XML parser. */
function readStrings(file) {
    if (!fs.existsSync(file)) return new Map();
    const content = fs.readFileSync(file, 'utf8');
    const out = new Map();
    const re = /<string(\s[^>]*?)?\sname="([^"]+)"([^>]*)>([\s\S]*?)<\/string>/g;
    let m;
    while ((m = re.exec(content)) !== null) {
        const attrs = (m[1] || '') + (m[3] || '');
        if (/translatable\s*=\s*"false"/.test(attrs)) continue;
        out.set(m[2], m[4]);
    }
    return out;
}

const args = process.argv.slice(2);
const positional = [];
let outPath = 'untranslated_strings.csv';
let onlyLocales = null;

for (let i = 0; i < args.length; i++) {
    if (args[i] === '-o' || args[i] === '--out') outPath = args[++i];
    else if (args[i] === '--locales') onlyLocales = args[++i].split(',').map(s => s.trim()).filter(Boolean);
    else positional.push(args[i]);
}

if (positional.length < 1) {
    console.log('Usage: node export_untranslated.cjs <res_dir> [-o out.csv] [--locales de,fr,ja]');
    process.exit(1);
}

const resDir = positional[0];
const defaults = readStrings(path.join(resDir, 'values', 'strings.xml'));
if (defaults.size === 0) {
    console.error(`No translatable strings found in ${path.join(resDir, 'values', 'strings.xml')}`);
    process.exit(1);
}

const locales = onlyLocales || fs.readdirSync(resDir)
    .filter(d => d.startsWith('values-') && fs.existsSync(path.join(resDir, d, 'strings.xml')))
    .map(d => d.slice('values-'.length))
    // Qualifiers like values-night or values-v31 are not locales.
    .filter(q => !/^(night|v\d+|land|port|sw\d+dp|w\d+dp|h\d+dp|.*dpi)$/.test(q));

const header = ['locale', 'name', 'default_value'];
const rows = [];
const summary = [];

for (const locale of locales) {
    const translated = readStrings(path.join(resDir, `values-${locale}`, 'strings.xml'));
    let missing = 0;
    for (const [name, value] of defaults) {
        if (!translated.has(name)) {
            rows.push({ locale, name, default_value: value });
            missing++;
        }
    }
    summary.push(`  ${locale}: ${missing} missing of ${defaults.size}`);
}

fs.writeFileSync(outPath, toCsv(header, rows));
console.log(summary.join('\n'));
console.log(`\nWrote ${rows.length} rows across ${locales.length} locales to ${outPath}`);
if (rows.length) {
    console.log('Add a "translated_value" column, fill it in, then run import_translations.cjs.');
}
