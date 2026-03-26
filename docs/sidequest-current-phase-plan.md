# Sidequest Current Phase Plan

Date: 2026-03-26

## Current State
- provider selection is single-active with persisted stored keys
- OpenAI is currently the only provider wired for multimodal capture analysis
- shopping lists are real and local-first across write, dictate, and photo flows
- shopping lists now have a real lifecycle: rename, archive, reactivate
- top-level swipe navigation is implemented for the main app shell
- routine planning is now a real feature surface with editable scheduling and XP
- inventory is now a real main-menu tab instead of a buried utility route
- markdown notes are now local-first assets inside inventory with write, import, edit, and preview flows
- inventory and stats now exist as dedicated utility surfaces
- capture detail images are now full-view and zoomable instead of center-cropped
- routine reminders now fire from active plans using weekday/time schedules

## Estimated Completion
- app overall: ~99%
- core functionality: ~99%
- production-hardening / quality: ~97%

## Phase Status

### Phase 6
Operational hardening

Status:
- shipped

Key outcomes:
- scan delete
- retry handling
- abandon quest
- more honest empty states and recovery paths

### Phase 7
Capture and extraction quality

Status:
- shipped

Key outcomes:
- persisted extraction kinds
- lower inbox noise
- stronger document-specific task/date/fact/reference separation

### Phase 8A
Provider-backed capture analysis

Status:
- shipped

Key outcomes:
- persisted provider keys
- single active provider
- OpenAI sees capture image + OCR
- provider output feeds typed extraction and quest creation

### Phase 8B
Search and memory quality

Status:
- open

Must ship:
- stronger local semantic retrieval
- better result ranking
- clearer memory/task/date/reference filtering

### Phase 8C
Inventory, stats, and utility management

Status:
- shipped

Key outcomes:
- inventory screen for shopping lists, routine plans, archived quests, and saved scans
- stats screen for honest direct counters plus derived wellbeing signals
- shopping list rename/archive/reactivate lifecycle
- capture detail zoom and full-image inspection
- inventory promoted to a real top-level tab
- markdown notes added as persistent inventory assets

### Phase 9
Routine and planning quality

Status:
- shipped

Key outcomes:
- routine reminders for active plans
- stronger next-run summaries in the routine ui
- routine reminder deep-link path into the plan detail screen
- profile cleanup so pseudo-stats now live in the stats surface instead of the profile surface

### Phase 10
Archive, QA, release

Status:
- open

Must ship:
- real archive QA on user-like data
- device regression pass
- cleanup of remaining rough UX edges

## Immediate Next Order
1. Phase 8B
2. Phase 10 QA and release pass
3. stronger provider coverage beyond OpenAI
