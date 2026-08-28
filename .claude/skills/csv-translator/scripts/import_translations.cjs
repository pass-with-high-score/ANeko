#!/usr/bin/env node
/**
 * Import translated strings from a CSV into Android values-<locale>/strings.xml.
 *
 * Usage: node import_translations.cjs <csv_path> <res_dir> [--dry-run] [--allow-placeholder-mismatch]
 *
 * The CSV needs "locale", "name" and "translated_value" columns. A
 * "default_value" column is optional; when present it is used to check that
 * the translation kept the same format placeholders.
 *
 * Values in the CSV are raw text. Android escaping (\', \", &amp;, leading @)
 * is applied here, so do not pre-escape anything in the CSV.
 */

const fs = require('fs');
const path = require('path');
const { parseCsvRecords } = require('./csv.cjs');

/** Map a BCP 47 style tag onto an Android resource qualifier. */
function androidLocale(locale) {
    const tag = locale.trim().replace(/_/g, '-');
    // Legacy codes Android still expects.
    const legacy = { id: 'in', he: 'iw', yi: 'ji' };
    const parts = tag.split('-');
    const lang = legacy[parts[0].toLowerCase()] || parts[0].toLowerCase();
    if (parts.length === 1) return lang;
    // Already in Android's region form (pt-rBR), keep it.
    if (/^r[A-Z]{2}$/.test(parts[1])) return `${lang}-${parts[1]}`;
    if (/^[A-Za-z]{2}$/.test(parts[1])) return `${lang}-r${parts[1].toUpperCase()}`;
    // Script or BCP47 extension (zh-Hans) needs the b+ form.
    return `b+${[lang, ...parts.slice(1)].join('+')}`;
}

/** Escape raw text for use as an Android string resource value. */
function escapeAndroidString(value) {
    let s = String(value);
    // Escape bare ampersands but leave existing entities (&amp; &#39; &#x27;) alone.
    s = s.replace(/&(?!(?:[a-zA-Z][a-zA-Z0-9]*|#\d+|#x[0-9a-fA-F]+);)/g, '&amp;');
    s = s.replace(/</g, '&lt;').replace(/>/g, '&gt;');
    s = s.replace(/'/g, "\\'").replace(/"/g, '\\"');
    s = s.replace(/\r\n|\r|\n/g, '\\n').replace(/\t/g, '\\t');
    // A leading @ or ? would be read as a resource reference.
    s = s.replace(/^([@?])/, '\\$1');
    return s;
}

/** Format placeholders, as a sorted multiset, for comparing source vs translation. */
function placeholders(value) {
    const found = String(value).match(/%(?:\d+\$)?[-+ 0#,(]*\d*(?:\.\d+)?[a-zA-Z]|%%/g) || [];
    return found.sort();
}

function escapeRegExp(s) {
    return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/** Detect the indentation used by <string> entries in a file. */
function detectIndent(fileContent) {
    const m = fileContent.match(/^([ \t]+)<string\b/m);
    return m ? m[1] : '    ';
}

function importTranslations(csvPath, resDir, opts) {
    const { header, records } = parseCsvRecords(fs.readFileSync(csvPath, 'utf8'));

    for (const required of ['locale', 'name', 'translated_value']) {
        if (!header.includes(required)) {
            console.error(`CSV must have a "${required}" column. Found: ${header.join(', ')}`);
            process.exit(1);
        }
    }
    const hasDefault = header.includes('default_value');

    const byLocale = new Map();
    const problems = [];
    const seen = new Set();

    for (const rec of records) {
        const locale = rec.locale.trim();
        const name = rec.name.trim();
        const value = rec.translated_value;

        if (!locale || !name) {
            problems.push(`line ${rec.__line}: missing locale or name`);
            continue;
        }
        if (value === undefined || value.trim() === '') {
            problems.push(`line ${rec.__line}: ${locale}/${name} has an empty translation`);
            continue;
        }
        const key = `${locale}\u0000${name}`;
        if (seen.has(key)) {
            problems.push(`line ${rec.__line}: ${locale}/${name} is duplicated`);
            continue;
        }
        seen.add(key);

        if (hasDefault && rec.default_value) {
            const want = placeholders(rec.default_value).join(' ');
            const got = placeholders(value).join(' ');
            if (want !== got) {
                const msg = `line ${rec.__line}: ${locale}/${name} placeholders differ — source [${want}] vs translation [${got}]`;
                if (opts.allowPlaceholderMismatch) console.warn(`warning: ${msg}`);
                else problems.push(msg);
            }
        }

        if (!byLocale.has(locale)) byLocale.set(locale, []);
        byLocale.get(locale).push({ name, value });
    }

    if (problems.length) {
        console.error(`Refusing to import, ${problems.length} problem(s) found:`);
        problems.forEach(p => console.error(`  ${p}`));
        console.error('Fix the CSV, or pass --allow-placeholder-mismatch if the placeholder change is intended.');
        process.exit(1);
    }

    let totalAdded = 0;
    let totalReplaced = 0;

    for (const [locale, items] of byLocale) {
        const targetDir = path.join(resDir, `values-${androidLocale(locale)}`);
        const targetFile = path.join(targetDir, 'strings.xml');

        let fileContent;
        if (fs.existsSync(targetFile)) {
            fileContent = fs.readFileSync(targetFile, 'utf8');
        } else {
            fileContent = '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n</resources>\n';
        }
        if (!/<\/resources>/.test(fileContent)) {
            console.error(`${targetFile} has no </resources> close tag, skipping.`);
            continue;
        }

        const indent = detectIndent(fileContent);
        let added = 0;
        let replaced = 0;

        for (const item of items) {
            const escaped = escapeAndroidString(item.value);
            // Match at any indentation, and keep whatever attributes the entry already had.
            const existing = new RegExp(
                `([ \\t]*)<string(\\s[^>]*?)?\\sname="${escapeRegExp(item.name)}"([^>]*)>[\\s\\S]*?</string>`
            );
            const m = fileContent.match(existing);
            if (m) {
                const before = m[2] || '';
                const after = m[3] || '';
                fileContent = fileContent.replace(
                    existing,
                    `${m[1]}<string${before} name="${item.name}"${after}>${escaped}</string>`
                );
                replaced++;
            } else {
                fileContent = fileContent.replace(
                    /([ \t]*)<\/resources>/,
                    `${indent}<string name="${item.name}">${escaped}</string>\n$1</resources>`
                );
                added++;
            }
        }

        if (!opts.dryRun) {
            fs.mkdirSync(targetDir, { recursive: true });
            fs.writeFileSync(targetFile, fileContent);
        }
        totalAdded += added;
        totalReplaced += replaced;
        console.log(`${opts.dryRun ? '[dry-run] ' : ''}${targetFile}: ${added} added, ${replaced} replaced`);
    }

    console.log(`\n${opts.dryRun ? '[dry-run] ' : ''}${byLocale.size} locale(s), ${totalAdded} added, ${totalReplaced} replaced.`);
}

const args = process.argv.slice(2);
const opts = {
    dryRun: args.includes('--dry-run'),
    allowPlaceholderMismatch: args.includes('--allow-placeholder-mismatch'),
};
const positional = args.filter(a => !a.startsWith('--'));
if (positional.length < 2) {
    console.log('Usage: node import_translations.cjs <csv_path> <res_dir> [--dry-run] [--allow-placeholder-mismatch]');
    process.exit(1);
}

importTranslations(positional[0], positional[1], opts);
