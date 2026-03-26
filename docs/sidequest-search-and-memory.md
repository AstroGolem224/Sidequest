# Sidequest Search And Memory

Date: 2026-03-26

## Goal
Turn `Search` into a real local memory surface instead of a thin OCR lookup box.

## What Ships In Phase 8B
- semantic retrieval over persisted scans, extracted intel, quests, and markdown notes
- one local ranking pass with exact, title, semantic, and related matching labels
- user-facing filters for:
  - `All`
  - `Tasks`
  - `Dates`
  - `References`
  - `Facts`
  - `Notes`
  - `Scans`
- direct recovery actions from search results:
  - `Open quest`
  - `Source scan`
  - `Open note`
  - `Recover from scan`

## Local Search Strategy
The app stays local-first. There is no cloud retrieval dependency.

The semantic pass builds a local search corpus from:
- `knowledge_nodes` for raw scan memory
- `extracted_items` for typed intel
- `missions` for actionable quest recovery
- `notes` for markdown memory

Each document is ranked with:
- exact phrase boosts
- title token overlap
- expanded semantic token overlap
- trigram similarity for non-exact phrasing
- type-intent boosts such as `expiry -> date`, `reference -> reference`, `todo -> task`
- freshness weighting
- source quality weighting

## Why This Is Better Than The Old Search
Before this pass, search was effectively:
- FTS on capture nodes
- token-overlap fallback on the same nodes

That meant:
- notes were invisible
- quests were invisible
- extracted dates, references, and facts were weakly recoverable
- non-exact user phrasing was unreliable

After this pass:
- notes are searchable
- quests are searchable
- extracted intel is filterable by kind
- search can recover intent from phrasing that does not exactly match OCR text

## Operational Rules
- if a quest already exists for a task-like capture item, search prefers the quest result instead of duplicating the candidate
- dismissed extracted items are excluded from search
- raw scan search remains available for memory recovery even when no quest was created

## Remaining Future Work
- optional persisted local vector index if archives grow large enough
- better multilingual synonym expansion
- re-ranking from real archive telemetry once enough user data exists
