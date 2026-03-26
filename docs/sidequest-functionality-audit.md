# Sidequest Functionality Audit

Date: 2026-03-26

## Scope
This audit compares the current implementation to [sidequest-architecture.md](C:/Users/matth/OneDrive/Dokumente/GitHub/Ambiguous/docs/sidequest-architecture.md), identifies real versus mocked functionality, and defines the next delivery phases needed to make the product operational rather than presentational.

## Executive Summary
- The app shell, local data model, capture pipeline, candidate review, mission board, reminders, search, and archive import/export all exist in working form.
- The strongest implemented loop is `capture/import -> OCR/extraction -> inbox review -> mission -> reminder`.
- The weakest areas are still `provider-backed enhancement`, `semantic retrieval quality`, `archive restore confidence`, `settings/provider UX`, and parts of the `profile` screen where presentation is ahead of operational value.
- Obvious mock/demo strings have been removed in this pass.
- A per-scan delete option is now implemented from capture detail.

## Architecture Check, Chapter By Chapter

### 1. Overview
Expected:
- local-first Android app
- modular monolith
- capture-first flow into missions and search

Current status:
- Implemented and aligned.
- Modules exist and are wired through Compose navigation and Hilt.
- The app is truly local-first and works without sign-in or backend.

Assessment:
- Healthy.
- No major architectural drift here.

### 2. Design Decisions
Expected:
- no account required
- optional provider integration
- immutable raw captures with provenance
- WorkManager background processing

Current status:
- Local-first and no account: implemented.
- Optional provider configuration exists, but provider capability is only partially mature.
- Raw captures remain the provenance anchor for extracted items and linked missions.
- WorkManager is used for capture processing and reminder delivery.

Assessment:
- Mostly implemented.
- Provider support is architecturally present but still product-incomplete.
- Provenance is good, but deleting a capture previously lacked a managed path; this pass adds that.

### 3. Core Flow
Expected:
1. capture/import
2. persist + enqueue
3. OCR + extraction
4. inbox review
5. mission promotion
6. dashboard/lobby/reminder/search consumption

Current status:
- `capture/import`: implemented
- `persist + enqueue`: implemented
- `OCR + extraction`: implemented, local-first, heuristic-heavy
- `inbox review`: implemented
- `mission promotion`: implemented
- `dashboard/reminders/search`: implemented
- `lobby`: implemented, but more as a derived dashboard than a must-have operational surface

Assessment:
- Core product loop works.
- The biggest functional risk is extraction quality, not flow wiring.
- Search is present but still more `good local lookup` than `strong second brain`.

### 4. Data Model
Expected:
- captures, analyses, extracted items, missions, reminders, provider config, knowledge nodes

Current status:
- `Capture`: implemented
- `CaptureAnalysis`: implemented
- `ExtractedItem`: implemented
- `Mission`: implemented
- `Reminder`: implemented
- `KnowledgeNode`: implemented
- `ProviderConfig`: effectively stored through secure prefs rather than a Room entity
- `Project` / `LifeArea`: schema exists but product usage is shallow

Assessment:
- Good enough for v1.
- `Project` and `LifeArea` are underused versus what the architecture suggests.
- There is no robust lifecycle policy yet for pruning stale data beyond manual delete/export/import.

### 5. Security Model
Expected:
- keystore-backed key storage
- no required connectivity
- manual snapshot import/export

Current status:
- secure provider key storage: implemented
- offline baseline: implemented
- snapshot export/import: implemented
- biometric app lock: implemented

Assessment:
- Strong relative to app maturity.
- Remaining weakness is not security architecture, but QA confidence around edge cases for restore and provider-failure UX.

### 6. Near-Term Roadmap
Expected in the architecture doc:
- CameraX capture and gallery import
- ML Kit OCR and rule-based extraction
- mission dashboard and reminders
- local search
- provider scaffolding

Current status:
- CameraX capture: implemented
- Gallery import: implemented
- OCR and extraction: implemented
- Dashboard and reminders: implemented
- Local search: implemented
- Provider scaffolding: implemented

Assessment:
- The old roadmap is largely obsolete because most listed items now exist.
- The problem has shifted from `missing features` to `depth, quality, and hardening`.

## Current Functional Inventory

### Implemented and Real
- camera capture
- gallery import
- local OCR and candidate extraction
- candidate classification into task/date/reference/fact
- inbox review and dismissal
- mission promotion
- mission editing for description, due date, reminder, and priority
- local reminder scheduling and notification deep links
- search across stored intel
- manual archive export/import
- biometric lock
- per-scan delete from capture detail

### Implemented but Quality-Limited
- extraction heuristics
- candidate confidence quality
- search relevance
- provider-enhanced extraction
- profile/lobby usefulness
- archive restore confidence and conflict handling

### Implemented but Still Presentation-Heavy
- parts of the profile screen
- some reward/gamification surfaces
- some top-level chrome metrics

### Not Fully Landed
- truly strong semantic retrieval
- provider-agnostic enhanced extraction flow that feels production-ready
- scan management tooling beyond single-item delete
- comprehensive error recovery UI for failed processing
- robust archive validation reporting
- test coverage for critical flows

## Demo / Mockup Cleanup Performed In This Pass
- removed hardcoded faux labels like `Field Scan` / `Recovered Intel`
- removed fake profile identity text like `ASTRAEA_VOID`
- replaced several fantasy rank labels with neutral operational labels
- added real per-scan delete behavior instead of leaving scans as permanent artifacts

## New Delivery Phases

### Phase 6 — Operational Hardening
Goal:
- make every current screen honest and reliable

Work:
- replace remaining decorative/fantasy status copy with operational language where it obscures meaning
- tighten failed-processing recovery flows
- add scan delete affordances from more than one entry point
- add safe empty states for missions, profile, search, and settings
- verify no screen depends on fabricated derived values for core decisions

Exit criteria:
- no major screen exposes fake identity, fake ranking, or fake source labels
- failed scans can be understood and handled without guesswork
- scan deletion works consistently and does not orphan visible junk
Status:
- shipped in the current working tree

### Phase 7 — Capture & Extraction Quality
Goal:
- improve the correctness of what gets extracted

Work:
- separate `task extraction`, `fact extraction`, `date extraction`, and `reference extraction` into distinct pipelines
- improve OCR cleanup and dedupe
- add document-type-specific extraction rules
- only produce task candidates when actionability is actually present
- expose extraction confidence and reasoning more clearly in review

Exit criteria:
- inbox noise drops materially
- non-task documents stop polluting the task queue
- extracted dates and references are consistently useful
Status:
- shipped in the current working tree

### Phase 8 — Search & Memory Quality
Goal:
- turn search from lookup into useful recall

Work:
- replace token-overlap fallback with a real local semantic index
- rank search results by source quality and extraction type
- allow filtering by `tasks`, `dates`, `references`, `facts`
- support direct search-to-capture and search-to-mission recovery flows

Exit criteria:
- non-exact phrasing still retrieves the right capture
- result lists distinguish action items from archived memory

Status:
- shipped in the current working tree

### Phase 9 — Provider & Settings Completion
Goal:
- finish the optional cloud-enhancement boundary

Work:
- harden provider adapters and timeouts
- expose provider health and last-error states in settings
- support provider selection per enhancement path
- keep all provider use strictly optional and non-blocking

Exit criteria:
- provider configuration is understandable
- provider failure never blocks local operation
- enhanced extraction improves quality when configured

### Phase 10 — Archive, QA, Release
Goal:
- make the app releasable

Work:
- test snapshot import/export on real devices and real files
- add regression coverage around delete, restore, and reminder flows
- run device bug bash across camera, notification, biometric, import/export, and large-history scenarios
- trim visual polish that still hurts clarity

Exit criteria:
- restore works on real user data
- key flows pass repeatable device QA
- no major dead-end or mock-only interaction remains

## Recommended Immediate Order
1. Phase 8
2. Phase 9
3. Phase 10

## Notes For The Next Implementation Pass
- Keep the app local-first.
- Favor clarity over role-play language in all operational screens.
- Do not reintroduce fake profile identity or fake ranking systems.
- If a value is shown, it should either come from real persisted data or be clearly marked as derived.
