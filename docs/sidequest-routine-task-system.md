# Sidequest Routine Task System

Date: 2026-03-26

## Purpose
Routine tasks are not ad-hoc quests. They are repeatable commitments with explicit planning fields, deterministic XP, and future verification hooks.

The intent is:
- keep repeatable habits out of the noisy capture inbox
- let users start from strong templates instead of blank forms
- make schedule, reward, and verification rules editable
- leave room for future integrations like capture verification and Google Fit / Health Connect

## What Is Implemented

### 1. Template Catalog
The app now ships with editable starter templates for:
- meditation
- sport
- stretch
- walk
- room reset
- kitchen reset
- garden round
- reading
- journaling
- sleep reset
- hydration reset
- plant care
- laundry
- shopping restock
- deep work sprint
- language practice
- meal prep
- finance review
- inbox zero
- social check-in

These are defaults, not locked content. Creating a plan from a template opens a full edit surface immediately.

### 2. Editable Plan Fields
Every routine plan can currently edit:
- title
- target label / what exactly is being done
- duration in minutes
- XP reward
- hour
- minute
- recurring on/off
- active on/off
- weekdays
- category
- trigger mode
- notes

### 3. Categories
The system uses the following top-level categories:
- `MIND`
- `BODY`
- `HOME`
- `OUTDOOR`
- `LIFE`

These are intentionally broad so the user can shape specific plans with the `targetLabel`.

### 4. Trigger Modes
Current trigger modes:
- `MANUAL`
- `CAPTURE`
- `FITNESS`

Interpretation:
- `MANUAL`: user explicitly completes the task
- `CAPTURE`: intended for tasks that could later be verified by a photo or scan
- `FITNESS`: intended for future fitness integrations

Current implementation status:
- the trigger mode is stored and editable
- completion is still user-confirmed for all three modes
- fitness and capture verification are planned seams, not yet automated

### 5. XP Rules
The XP model is intentionally conservative:
- XP is earned on completion only
- no XP is granted for creating or editing tasks
- template creation does not mint rewards
- completion count and last completion time are persisted

This keeps the system aligned with real work rather than task inflation.

## UX Model

### Planner Screen
The planner screen has two sections:
- template catalog
- user-created plans

The intended loop is:
1. choose a template
2. edit the details immediately
3. save the schedule
4. complete the routine when done
5. collect XP from actual completion only

### Detail Screen
The routine detail screen is the real control surface.

It is designed to answer:
- what is the task exactly
- when should it happen
- how often should it repeat
- how should completion be interpreted
- how much XP should it award

## Why This Structure Works

### It avoids capture overload
Capture-derived missions are reactive. Routine plans are proactive. Keeping them separate prevents the inbox from becoming a confused mix of habits and discovered tasks.

### It stays editable
A meditation plan and a kitchen reset plan have different verification expectations, durations, and recurrence rhythms. Hard-coded habits would become brittle very fast.

### It is integration-ready without fake automation
The app stores the trigger mode now, but does not pretend that Google Fit or capture verification already exist. That keeps the UX honest while preserving the architecture seam.

## Future Hooks

### Fitness
Likely next path:
- Google Fit or Health Connect read access
- map a routine plan to activity types or daily movement thresholds
- mark completion automatically or suggest completion

Good early candidates:
- sport
- walk
- stretch

### Capture Verification
Likely next path:
- attach a source capture to a routine completion event
- use local/provider analysis to detect before/after state changes
- support confirmable evidence rather than blind automation

Good early candidates:
- room reset
- kitchen reset
- garden round

### Mission Materialization
Possible later step:
- materialize today's active routine plans into a daily mission strip
- keep the source of truth in the routine planner, not duplicate logic in missions

## Current Gaps
- no automatic routine reminders yet
- no recurrence materialization into daily quest cards yet
- no verification adapters for `CAPTURE` or `FITNESS` yet
- no custom template authoring flow separate from editing a created plan

## Recommendation
The next implementation slice for this system should be:
1. local reminders for routine plans
2. clearer next-run summaries in the planner
3. optional completion verification hooks for `CAPTURE` and `FITNESS`
