# SORN Sample Files — govaifiles.org

These are 5 sample System of Records Notices (SORNs) from the Federal Register, parsed into structured JSON. They are a subset of ~3,500 SORNs in the full dataset, selected because they document AI/algorithmic systems clearly.

## The Files

| File | System | Agency |
|------|--------|--------|
| `2012-12396.json` | Automated Targeting System (ATS) | DHS/CBP |
| `2025-13609.json` | Search, Arrest & Seizure Records | DHS/ICE |
| `2022-14789.json` | Electronic System for Travel Authorization (ESTA) / Traveler Verification | DHS/CBP |
| `2016-25206.json` | Border Patrol Enforcement Records | DHS/CBP |
| `2024-08473.json` | Automated License Plate Recognition (ALPR) | Presidio Trust |

---

## JSON Structure

Each file has three top-level keys:

```
{
  "preamble": { ... },           // front matter: agency, summary, dates
  "supplementary_info": { ... }, // background narrative, signature
  "system_of_records": { ... },  // the core Privacy Act fields
  "metadata": { ... }            // source file, conversion timestamp
}
```

---

## Most Important Fields (for display)

### Identity / Header
- **`preamble.subject`** — the actual system name. *Use this, not `title`.* The Federal Register title field is almost always the generic string "Privacy Act of 1974; System of Records" — `subject` has the real name (e.g., "DHS/CBP-006 Automated Targeting System").
- **`preamble.agency.name`** — publishing agency (e.g., "DEPARTMENT OF HOMELAND SECURITY")
- **`preamble.sub_agency`** — sub-agency or bureau (e.g., "U.S. Customs and Border Protection")
- **`preamble.action`** — New / Modified / Rescinded
- **`preamble.dates`** — effective/publication dates

### What the system does
- **`preamble.summary`** — short overview; good for a card/list view
- **`system_of_records.purpose`** — stated purpose of the system
- **`system_of_records.authority`** — legal authority (statutes cited)

### Who it affects / what it collects
- **`system_of_records.categories_of_individuals`** — who the records are about
- **`system_of_records.categories_of_records`** — what data is collected
- **`system_of_records.record_source_categories`** — where the data comes from

### Sharing and retention
- **`system_of_records.routine_uses`** — who the data can be shared with (often a long list)
- **`system_of_records.retention_and_disposal`** — how long records are kept

### Lower priority (useful but secondary)
- `system_of_records.safeguards`
- `system_of_records.access_procedures`
- `system_of_records.system_location`
- `supplementary_info.background` — detailed narrative; verbose but substantive

---

## Data Quality Notes

**Expect missing fields.** Not every SORN fills in every field. Some fields will be `null`, an empty string, or a list with one vague entry. This is a real feature of the data — disclosure quality varies a lot by agency and year. The site should handle missing fields gracefully (omit the section or show a "not provided" label).

**`routine_uses` is often a list.** It may contain 10–20 permitted disclosure categories. Each entry is a string. Worth rendering as a bulleted list.

**`supplementary_info.background`** is long prose. It's often the most substantive description of how the system actually works — but it's not structured, just a block of text.

**Encoding artifacts.** You may see `â` or similar — these are UTF-8 encoding artifacts from the original Federal Register XML (em-dashes, curly quotes). Worth cleaning on ingest.

---

## What a full record looks like

The ATS SORN (`2012-12396.json`) is a good reference — it's one of the more complete and substantive records in the dataset. The ICE Search/Arrest record (`2025-13609.json`) is the largest and most recent, with explicit mentions of facial recognition algorithms.

---

## Full Dataset

The full dataset contains ~3,500 SORNs spanning 2010–2026. We've identified ~280 with AI-related terms and a subset with explicit facial recognition / biometric system documentation. Happy to share the full JSON directory or a CSV index of all records.
