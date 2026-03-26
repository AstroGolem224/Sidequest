# Sidequest Inventory, Avatar, Theme, and AI-First Pass

## Scope
This pass closes five UX and platform gaps:

1. inventory search and section cleanup
2. profile avatar selection
3. stats hierarchy cleanup
4. ai-first capture processing
5. stitched theme color switching

## Inventory
The inventory screen now acts as a searchable asset vault instead of a flat dump.

- A top-level search field filters notes, shopping lists, routine plans, archived quests, and scans.
- Section entrypoints for `Shopping Lists` and `Routine Tasks` now live inside their own sections instead of competing with the inventory hero card.
- The inventory header shows section counts so growth remains legible.
- The default scan lane stays capped when no query is active to reduce visual overload.

### Follow-up idea if inventory volume grows further
If the number of notes and assets grows beyond a few hundred items, the next step should be a second filtering layer:

- filter chips for `notes`, `lists`, `routines`, `archived`, `scans`
- sort mode toggle for `recent`, `alphabetical`, `active first`
- optional collapsible sections

## Profile Avatar
The profile hero now supports a local avatar image.

- Users can pick an image from the device photo picker.
- The image is copied into app-private storage so it survives picker permission loss.
- The avatar is rendered as a circular image inside the existing profile ring.
- Users can remove the avatar and fall back to the default profile glyph.

## Stats Screen
The stats screen now leads with the actual wellbeing bars instead of duplicating them.

- `Physical Health`, `Mental Health`, `Home Order`, and `Life Admin` stay at the top.
- Raw counters like completed quests, routine sessions, checked items, and saved scans moved below.
- The duplicate wellbeing pill row was removed from the summary block.

## AI-First Capture
Capture processing can now run in an image-first mode before local OCR.

### Behavior
- A new `AI-first` switch lives directly on the capture screen.
- When enabled and an AI provider is active, the provider sees the image first with no OCR context.
- After that, local OCR still runs and the outputs are merged.
- The provider prompt now explicitly allows scene-based quest suggestions, such as cleanup or reset tasks, when the image shows disorder rather than text.

### Why this matters
This is the missing bridge between:

- document captures that need reading
- scene captures that need interpretation
- messy environment captures that should become cleanup quests

## Theme Switching
Settings now expose a theme switcher that only changes colors, not layout.

### Theme sources
The palettes are derived from the stitch theme documents in:

- `stitch/Themes/DESIGN_Solar.md`
- `stitch/Themes/DESIGN_Aether.md`
- `stitch/Themes/DESIGN_Frost.md`
- `stitch/Themes/DESIGN_Hearth.md`

### Applied rule
Only the color tokens were ported. Layout, spacing, motion, and component structure stay tied to the existing Sidequest UI.

## Persistence
The following user-facing settings now persist through the security/preferences service:

- active theme preset
- ai-first capture enabled state
- biometric lock enabled state
- avatar image path
- provider keys and active provider

## Validation
Validated with:

```powershell
.\gradlew.bat :feature-lobby:compileDebugKotlin :feature-capture:compileDebugKotlin :feature-settings:compileDebugKotlin :app:assembleDebug
.\gradlew.bat testDebugUnitTest
adb install -r -d "$env:LOCALAPPDATA\SidequestBuild\app\outputs\apk\debug\app-debug.apk"
```
