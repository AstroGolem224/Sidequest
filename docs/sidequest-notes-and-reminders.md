# Sidequest Notes And Routine Reminders

Date: 2026-03-26

## Why This Pass Exists

Two gaps were still obvious:

1. routine plans had schedules but no user-visible reminder behavior
2. inventory was missing a real notes system even though local-first markdown fits the product directly

This pass closes both gaps.

## Routine Reminder Model

### What now happens

- active routine plans schedule a reminder based on their stored weekday and time
- changing a plan reschedules the next reminder
- deleting or deactivating a plan cancels its reminder
- completing a plan re-arms the next reminder instead of leaving the plan stale

### Deep-link behavior

Routine reminder notifications open the app directly into the matching routine plan detail screen.

### Scheduling rules

- if weekdays are set, the reminder targets the next matching weekday
- if no weekdays are set, the reminder targets the next future occurrence of the stored time
- non-recurring plans only schedule the next eligible occurrence and do not project indefinitely

## Notes Model

### Supported flows

1. create a blank markdown note
2. import a markdown or text file into a local note
3. edit title and markdown body
4. preview headings, bullets, quotes, and code fences
5. delete a note

### Storage rules

- notes are fully local-first
- imported notes keep a `sourceLabel` so the origin stays visible
- notes are included in archive export/import

### Why notes live in inventory

Notes are durable assets, not transient intake and not mission cards. Putting them into `Inventory` avoids polluting the quest loop while keeping them close to other reusable assets like shopping lists and routine plans.

## UX Decisions

### Inventory as a main tab

Inventory is now a top-level destination in the main shell. That is intentional:

- shopping lists are operational assets
- notes are durable knowledge assets
- routine plans are reusable system assets
- archived quests and saved scans are recovery assets

That cluster belongs at top level, not buried under profile.

### Profile cleanup

The old pseudo-stat blocks in profile are removed. The profile keeps identity, level, queue load, and milestones. Measurement moved into the `Stats` surface where it is easier to read honestly.

## Future Follow-up

1. render richer markdown elements like links and checklists
2. allow converting note checklist items into quests or shopping items
3. optionally verify some routine completions through capture or fitness integrations
