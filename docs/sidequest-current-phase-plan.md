# Sidequest Current Phase Plan

Date: 2026-03-26

## Current State
- provider selection is single-active with persisted stored keys
- OpenAI is currently the only provider wired for multimodal capture analysis
- shopping lists are real and local-first across write, dictate, and photo flows
- top-level swipe navigation is implemented for the main app shell
- routine planning is now a real feature surface with editable scheduling and XP

## Estimated Completion
- app overall: ~98%
- core functionality: ~99%
- production-hardening / quality: ~94%

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

### Phase 9
Routine and planning quality

Status:
- started

Must ship:
- routine reminders
- stronger next-run summaries
- archive confidence for routine data
- future verification seams for capture and fitness routines

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
2. Phase 9 reminders and verification seams
3. Phase 10 QA and release pass
