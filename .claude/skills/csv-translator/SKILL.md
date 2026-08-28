---
name: csv-translator
description: Specialized skill for translating large CSV files. It follows a multi-step process of planning, counting lines, splitting large files into manageable chunks, translating chunks, merging, and finally importing translations into Android resource files. Use when the user needs to translate a CSV file, especially large ones.
---

# CSV Translator Workflow

Translate large CSV files without blowing the context window, then import the
results into Android string resources.

All scripts live in `scripts/` and share `scripts/csv.cjs`, an RFC 4180 parser.
Fields keep their commas, quotes and newlines through every step.

## Workflow Steps

### 0. Discovery & Extraction (Optional)

- **Find untranslated strings:**
  `node scripts/export_untranslated.cjs <res_dir> [-o out.csv] [--locales de,fr]`
  Writes `locale,name,default_value` rows for everything in
  `values/strings.xml` that a `values-<locale>/strings.xml` is missing. Skips
  `translatable="false"` entries and non-locale qualifiers such as
  `values-night` and `values-v31`.
- **Find unused strings:** `node scripts/find_unused_strings.cjs <module_dir> [<extra_dir> ...]`
  Pass every module that might reference the strings — a string declared in
  `app` is often used from another module. Heuristic; names built at runtime
  cannot be detected, so review before deleting.

### 1. Planning & Analysis

- **Identify target:** which CSV, and which locales.
- **Size it up:** `wc -l` for a rough count.
- **Pick a chunking strategy** (see below).

### 2. Splitting

```
node scripts/split_csv.cjs <input> <output_prefix> --by locale     # one file per language
node scripts/split_csv.cjs <input> <output_prefix> <rows_per_chunk>
```

Prefer `--by locale` for Android exports. One chunk per language means each
translation pass sees a whole language at once, which keeps terminology
consistent — that matters more than an even row count. Fall back to
`<rows_per_chunk>` (100–200) when a single locale is still too large, and split
again if a chunk remains too big for one turn.

### 3. Translation

Add a `translated_value` column and fill it in.

- Preserve every format placeholder exactly: `%1$s`, `%2$d`, `%%`. You may
  reorder them when grammar requires it (`%2$d 個中 %1$d 個`), but the set must
  match — the import script enforces this.
- Keep the key column untouched and the row order stable; it makes review a
  clean diff.
- Reuse the terminology already in the locale. Look up how the target language
  already renders a referenced UI label before quoting it in help text, so
  "turn on Allow downgrade" points at the string the user actually sees.
- Write raw text, not Android escapes. Do **not** type `\'` or `\"` — the
  import script escapes for you, and pre-escaping double-escapes.
- **CSV quoting still applies.** A field containing a comma, a newline or a
  double quote must be wrapped in `"`, and a literal `"` inside it must be
  doubled: `"He said ""Allow downgrade"" here"`. Using the locale's own
  quotation marks (« », „ ", 「」) usually reads better anyway.

### 4. Merging

```
node scripts/merge_csv.cjs <output_path> <input1> <input2> ...
```

Keeps one header row and errors out on a column mismatch rather than silently
misaligning rows.

### 5. Importing (Android)

```
node scripts/import_translations.cjs <csv_path> <res_dir> [--dry-run] [--allow-placeholder-mismatch]
```

Requires `locale`, `name` and `translated_value` columns; uses `default_value`
for placeholder checking when present. Run `--dry-run` first to preview counts.

The import refuses to write anything if any row has an empty translation, a
duplicate `locale`+`name`, or placeholders that do not match the source. That
is deliberate — a partial import is worse than none. Pass
`--allow-placeholder-mismatch` only when the change is intentional.

On write it:
- maps locales to Android qualifiers (`pt-BR` → `values-pt-rBR`, `id` → `values-in`, `zh-Hans` → `values-b+zh+Hans`);
- replaces an existing entry in place at whatever indentation the file uses,
  preserving attributes like `formatted="false"`, instead of appending a
  duplicate key;
- escapes `'`, `"`, bare `&`, `<`, `>`, newlines and a leading `@`/`?`, while
  leaving existing entities such as `&amp;` alone.

### 6. Validation

- Re-run `export_untranslated.cjs`; it should report 0 missing.
- Re-running the import should report `0 added` and leave `git diff` clean.
- Check the XML parses and has no duplicate keys.
- Build: `./gradlew :app:processDebugResources` validates resources on their
  own, which is quicker than `assembleDebug` and isolates resource problems
  from unrelated compile errors.

### 7. Cleanup & Git

- Do not commit intermediate CSVs (exports, chunks, merged files). Delete them
  once the import is validated, or add them to `.gitignore`.
- Commit only the `strings.xml` changes.

## Guardrails

- **Preserve structure:** never change the column count or the meaning of key
  columns.
- **Encoding:** UTF-8 throughout.
- **No double-escaping:** raw text in the CSV, Android escapes applied on import.
- **Never invent keys:** the key set must match the source export exactly.
