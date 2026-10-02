# AniFlow — Canonical Domain Glossary

> **Enforcing STEP 15 Section 95 & 96 (Single Source of Truth & Domain Vocabulary).**  
> *These terms are canonical across Code, Database Schemas, UI, and Documentation.*

---

## 1. Core Domain Entities

| Term | Definition | Disallowed Synonyms |
| :--- | :--- | :--- |
| **Anime** | The parent intellectual property or series identity (e.g. *Attack on Titan*). | Show, Series, Cartoon |
| **Season** | A numbered sequential or special season grouping under an Anime (e.g. *Season 4*). | Part (unless official title), Arc |
| **Episode** | A distinct numbered narrative installment or OVA/special under a Season. | Chapter, Part |
| **Release** | An indexed distribution package published on a provider containing media. | Torrent, Item, Link, Post |
| **Provider** | An external indexing service providing searchable anime releases (e.g. *Nyaa*). | Tracker, Scraper, Site |
| **Uploader** | The external user account that submitted the release to the provider. | Submitter, User |
| **Release Group**| The fansub or encode team responsible for encoding and subtitling (e.g. *SubsPlease*). | Subber, Ripper |
| **Batch** | A single release package encompassing an entire season, cour, or episode range (e.g. *01–12*). | Pack, Bundle, Complete |

---

## 2. Download & Runtime Terminology

| Term | Definition | Disallowed Synonyms |
| :--- | :--- | :--- |
| **Download Plan** | A pre-flight blueprint validating storage capacity, network rules, and duplicate status before queuing. | Staging, Prep |
| **Download Task** | A stateful runtime execution unit managing the streaming and verification of a release. | Job, Download, Thread |
| **Download Queue**| The prioritized fifo/priority scheduler managing concurrent task slots. | Pool, List |
| **Partial File** | An incomplete temporary file on disk with a `.part` or `.tmp` extension undergoing transfer. | Temp file, Broken file |
| **Atomic Move** | The immediate filesystem rename of a verified `.part` file to its final destination path. | Copy-paste, Move |

---

## 3. Storage & Library Terminology

| Term | Definition | Disallowed Synonyms |
| :--- | :--- | :--- |
| **Storage Target**| An authorized filesystem directory managed via Android Storage Access Framework (SAF). | Directory, Path, Folder |
| **Library Item** | A recognized Anime series tracked in the user's permanent local media library. | Entry, Record |
| **Library File** | A physical media file verified and indexed within a specific Library Item's folder. | Video, File |
| **Coverage Map** | A matrix displaying the completeness of a season (`Available`, `Missing`, `Downloading`, `Upgrade`). | Checklist, Grid |

---

## 4. Control Plane & Automation Terminology

| Term | Definition | Disallowed Synonyms |
| :--- | :--- | :--- |
| **Download Profile**| A saved user preference template specifying desired resolution, video codec, and audio channels. | Preset, Setting |
| **Rule** | A declarative boolean expression (AST) evaluated against discovered releases to trigger automated actions. | Filter, Script, Trigger |
| **Action** | An executable domain operation resulting from a satisfied rule (e.g. `AutoDownload`, `Notify`). | Command, Step |
| **Execution ID** | A unique correlation identifier tracking an automation cascade to prevent recursive loops. | Correlation ID, Tag |
| **Saved Search** | A persistent search query and filter set monitored for new airing release updates. | Alert, Watcher, Monitor |
