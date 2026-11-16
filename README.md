# Zotero for Android

Zotero is a personal research assistant and reference management application rebuilt for Android using **Kotlin**, **Jetpack Compose (Material 3)**, and **Room Database**.

## Key Features

- **My Library & Item Inspector**: Browse, search, filter, and sort scholarly sources (Journal Articles, Books, Conference Papers, Preprints, Theses, Reports, and Web Pages).
- **Add by Identifier (DOI / ISBN / arXiv)**: Resolve bibliographic metadata via Crossref and built-in identifier translators.
- **Hierarchical Collections & System Views**: Organize references across custom collections, Starred items, Unfiled items, Duplicate detection, and Trash.
- **Colored Tag Selector**: Assign Zotero color swatches to tags for visual scanning and one-tap filtering.
- **PDF Highlights & Research Notes**: Record page-indexed PDF highlight passages with Zotero highlight colors, add marginalia comments, and synthesize highlights directly into child research notes.
- **CSL Citation & Bibliography Studio**: Format in-text citations and full bibliographies in APA 7th, MLA 9th, Chicago 17th, IEEE, Harvard, BibTeX (`.bib`), and RIS (`.ris`).

## Tech Stack

- **UI**: Jetpack Compose + Material Design 3 + Locally bundled Google Fonts (`Libre Baskerville`, `Plus Jakarta Sans`, `JetBrains Mono`)
- **Architecture**: MVVM (`ZoteroViewModel` + `StateFlow` + `ZoteroRepository`)
- **Persistence**: Room SQLite Database (`ZoteroDatabase` + KSP)
