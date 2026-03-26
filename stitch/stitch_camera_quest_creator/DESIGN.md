# Sidequest Stitch Alignment

## Selected Direction
Sidequest should use a **hybrid visual system**:

- `clean_quest_creator_v2` defines the **capture screen** and the overall cinematic camera-first posture.
- `clean_dashboard_quest_log_v2` defines the **dashboard / quest log** surfaces, spacing, and shell chrome.
- `clean_quest_brief_v2` defines the **mission detail** hierarchy.
- `clean_stats_inventory_v2` defines the **profile / lobby** screen.
- `aurelian_void/DESIGN.md` provides the **primary shell language** for the shipped Android app.
- `aether_forge/DESIGN.md` remains a secondary variant and should inform celebratory or reward-heavy moments, not the base shell.

The result is not "game UI pasted onto productivity." It is a **premium quest OS** where capture, review, missions, search, and profile all share one warm amber-on-charcoal language.

## Product Mapping

### 1. Dashboard
Maps to the current mission board.

- Purpose: show `level`, `gp`, `xp progress`, and the active quest log.
- Real app behavior: this screen still owns mission triage, completion, and quick open actions.
- Visual source: `clean_dashboard_quest_log_v2`.

### 2. Quests
Maps to the current inbox / review queue.

- Purpose: review AI-generated candidate quests before they enter the system.
- Real app behavior: this is where the user exercises veto power.
- Visual direction: same shell as dashboard, but framed as a staging hangar / quest intake.

### 3. Create
Maps to the current capture flow.

- Purpose: scan the physical world and convert it into structured quests, facts, dates, or references.
- Real app behavior: camera-first with import fallback, review, and source capture drill-down.
- Visual source: `clean_quest_creator_v2`.

### 4. Profile
Maps to the current lobby.

- Purpose: summarize progress, streak, attributes, and collectible rewards.
- Real app behavior: uses live mission data to derive progress and "inventory" surfaces.
- Visual source: `clean_stats_inventory_v2`.

### 5. Mission Detail
Maps to the current mission brief screen.

- Purpose: show briefing, rewards, timing, and source capture context.
- Real app behavior: due date, reminder, priority, and action controls remain fully editable.
- Visual source: `clean_quest_brief_v2`.

## Functional Extensions To The Design

The stitch package is mostly visual. Sidequest needs additional product rules so the Android app remains useful:

### Capture Classification
Every processed capture must land in exactly one of these buckets:

- `quest_candidate`
- `fact`
- `date`
- `reference`
- `no_actionable_output`

Only `quest_candidate` items appear in the Quests intake queue by default.

### Rewards Layer
The design shows `XP` and `GP`, so the app needs deterministic reward rules:

- Completing a mission grants XP derived from `priority`, `urgency`, and `difficulty hints`.
- GP is a softer reward derived from completion count and mission weight.
- Capture and review do **not** award XP. Completion does.

### Top Bar Utility
Because the stitched design uses a clean 4-tab shell, the app must move utility routes out of the bottom nav:

- `Search` becomes a top-bar action.
- `Settings` becomes a top-bar action.
- `Inbox review` remains a primary tab because it is part of the core action loop.

### Capture Review Drawer
The design implies a post-scan quest card. In the Android app this should become:

- a bottom drawer for fresh candidate review
- with `promote`, `dismiss`, and `open source intel`
- while the separate Quests tab remains the backlog / recovery surface
- while processing is still running, the same drawer can show `analyzing objective` instead of a dead end
- if a scan produces only `fact / date / reference` intel, the drawer must explicitly say `intel archived` rather than pretending there were no results

### Profile / Inventory Semantics
The profile design shows loot and attributes. In Sidequest those should map to real state:

- attributes are derived from live mission distribution and completion history
- loot cards represent streaks, milestones, and capture achievements
- profile never becomes a second task source of truth

### Mission Brief Semantics
The quest brief screen in stitch needs to stay operational, not decorative:

- the hero can use the blurred source capture when Sidequest has one
- `briefing` remains directly editable
- `potential spoils` are derived from deterministic XP/GP rules
- `vital stats` are where due date, reminder, difficulty, and estimated duration live
- `evidence window` is the primary way back to the source capture detail
- `forfeit` maps to archive, not deletion

### Quest Intake Semantics
The stitched Quests screen needs one direct trust path:

- every candidate card must support `open intel` so the user can inspect source context before promotion
- dismissing a candidate is first-class, fast, and one tap away

## Implementation Phases

### Phase 1
- Adopt the `Solar Observatory` amber shell
- Move to a 4-tab bottom nav
- Add a shared top bar with brand + GP + utility actions
- Restyle Dashboard, Create, and Profile to match stitch surfaces

### Phase 2
- Restyle Quests intake and Mission Detail
- Add reward toasts and better completion feedback
- Add capture review drawer behavior after a fresh scan
- Add direct `open intel` access from Quests and Mission Brief

### Phase 3
- Replace generic Android iconography with custom pictograms
- Add stronger motion design, shimmer, and subtle reward animation
- Tighten device-level spacing and gesture feel

## Implementation Status

- `Phase 1`: complete
  - amber shell, 4-tab nav, top-bar utilities, and stitched dashboard/create/profile baseline are now landed
- `Phase 2`: complete
  - quests intake, mission brief, post-scan review drawer, direct intel access, and visible completion/review feedback are landed
- `Phase 3`: complete
  - custom pictograms, reward banners, subtle progress motion, and tighter device-level spacing/affordances are now in the app

## Next Delivery Phases

### Phase 4
- Upgrade `Profile` into a stronger stats + inventory surface derived from live mission state
- Add visible reward banners / completion feedback on the mission board
- Surface overload guidance when too many quests are active
  - status: shipped in the current working tree

### Phase 5
- Add richer motion to capture review, quest completion, and profile unlocks
- Tighten iconography, chip styling, and spacing on real devices
- Improve capture-detail discoverability from more surfaces
  - status: shipped in the current working tree

## Guardrails

- No hard white-on-black dashboard look.
- No HUD clutter unless it serves the scan flow.
- No fake RPG systems disconnected from mission truth.
- No bottom-nav sprawl beyond the 4 stitched destinations.
- AI always suggests; the user always confirms or dismisses.
