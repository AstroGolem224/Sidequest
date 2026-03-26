# Sidequest Inventory And Stats

Date: 2026-03-26

## Why These Screens Exist

`Inventory` and `Stats` solve two different problems:

- `Inventory` is for persistent assets that should remain editable and reusable over time.
- `Stats` is for operational feedback derived from real completions, not decorative role-play numbers.

Without these screens, shopping lists, archived quests, routines, and scans become hard to find again, and the user's long-term behavior stays invisible.

## Inventory Model

### What belongs in Inventory now

1. Shopping lists
   - renameable
   - archivable
   - reactivatable
   - openable

2. Routine plans
   - editable
   - schedulable
   - openable

3. Archived quests
   - visible after abandonment/archive
   - reactivatable
   - openable

4. Saved scans
   - quickly reopenable from a single place
   - still deletable from capture detail

### Why shopping lists belong here

Shopping lists are not one-off missions. They are lightweight utility assets. Users often need to:

- rename them after creation
- archive them after a shopping run
- reactivate them for a weekly restock
- keep them available without polluting the active quest board

That makes them a better fit for `Inventory` than for `Quests`.

### Other inventory candidates worth adding later

1. Pantry staples
   - reusable checklists for things that should usually be in stock

2. Travel packs
   - standard reusable lists like cables, passport, chargers, medication

3. Maintenance kits
   - bike tools, garden tools, cleaning supplies, office reset kits

4. Document vault shortcuts
   - frequently referenced scans like IDs, contracts, warranty references

5. Health kits
   - medication restock lists, supplements, recovery items

## Stats Model

### Direct counters

The stats screen shows real raw counts from persisted data:

1. completed quests
2. completed routine sessions
3. checked shopping-list items
4. saved scans

These are not guessed values.

### Derived wellbeing metrics

The screen also shows four derived indicators:

1. `Physical Health`
2. `Mental Health`
3. `Home Order`
4. `Life Admin`

These are explicitly derived signals, not medical or psychological measurements.

### Derivation rules

Routine plans are the cleanest source because they already carry a category:

- `BODY` contributes to `Physical Health`
- `MIND` contributes to `Mental Health`
- `HOME` and `OUTDOOR` contribute to `Home Order`
- `LIFE` contributes to `Life Admin`

Completed quests are bucketed by keywords in title + description:

- physical examples: `run`, `walk`, `sport`, `workout`, `stretch`, `gym`, `garden`
- mental examples: `meditat`, `journal`, `read`, `study`, `reflect`, `breath`
- home examples: `clean`, `kitchen`, `room`, `desk`, `laundry`, `garden`, `reset`
- everything else defaults to `Life Admin`

### Score formula

Each metric starts with a baseline, then adds points per counted completion, then clamps into a stable UI range:

- `Physical Health = 28 + 11 * physical_count`
- `Mental Health = 32 + 10 * mental_count`
- `Home Order = 26 + 12 * home_count`
- `Life Admin = 24 + 11 * admin_count`

After that, each score is clamped to `18..100`.

This keeps the UI readable and avoids fake precision.

### Why this is acceptable

- the formulas are deterministic
- the categories are inspectable
- the system never pretends these are clinical truths
- the scores reward completion, not task creation

## Editing Rules

The system should stay editable wherever the user still owns the asset:

1. shopping lists
   - title editable
   - archive/reactivate editable

2. routine plans
   - already fully editable

3. archived quests
   - reactivatable instead of dead-ended

4. saved scans
   - deletable from capture detail

## Additional Routine Ideas

The routine template catalog now includes additional ideas beyond the first pass:

1. sleep reset
2. hydration reset
3. plant care
4. inbox zero
5. social check-in

These are intentionally broad starter templates, not locked product rules.

## Product Direction

The point of this pass is not to add more game dressing. It is to make Sidequest behave like a trustworthy personal operating system:

- scans go somewhere recoverable
- utility assets stay reusable
- recurring behavior gets its own planning layer
- user progress becomes measurable without lying
