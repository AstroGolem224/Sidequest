# Sidequest Architecture

## Overview
Sidequest is a local-first Android app for turning visual chaos into structured action. Users capture or import images, the app extracts text and candidate tasks, and the user promotes those candidates into missions that appear in a prioritized dashboard and a more playful lobby view.

The v1 architecture is a modular monolith:

- `app`: app shell, navigation, DI bootstrap
- `core-ui`: design tokens and shared Compose components
- `core-data`: Room schema, repositories, processing pipeline, security and archive interfaces
- `feature-capture`: camera/import entry flow
- `feature-inbox`: review extracted candidates
- `feature-missions`: mission dashboard and actions
- `feature-lobby`: playful read-model over mission state
- `feature-search`: local text search
- `feature-settings`: privacy, provider and archive settings

## Design decisions
- Local-first baseline with no required account, backend or analytics.
- Optional provider integration for enhanced extraction through user-supplied API keys.
- Raw captures remain immutable source objects; all derived missions and facts retain provenance.
- Background work uses WorkManager so capture remains fast and processing can retry safely.

## Core flow
1. Capture or import an image.
2. Persist the image and enqueue processing.
3. OCR and rule-based extraction run in the background.
4. Candidate items land in Inbox for review.
5. Accepted candidates become missions.
6. Missions drive the Mission Control dashboard, Lobby, reminders and search.

## Data model
- `Capture`: immutable source image plus metadata.
- `CaptureAnalysis`: OCR output and processing summary.
- `ExtractedItem`: candidate task/fact/event with provenance.
- `Mission`: user-facing actionable unit.
- `Project` and `LifeArea`: grouping and presentation.
- `Reminder`: local reminder state.
- `ProviderConfig`: user-selected enhanced extraction provider metadata.

## Security model
- API keys are stored through a Keystore-backed service.
- App usage works without network connectivity or provider configuration.
- Export/import is manual and snapshot-based through Storage Access Framework.

## Near-term roadmap
- CameraX capture and gallery import
- ML Kit OCR and rule-based extraction
- Mission dashboard and reminders
- Local search
- Provider adapter scaffolding for OpenAI, Anthropic, NIM and OpenRouter
