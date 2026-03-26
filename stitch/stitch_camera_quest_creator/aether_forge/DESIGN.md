```markdown
# Design System Specification: The Hero’s Journey

## 1. Overview & Creative North Star
The Creative North Star for this design system is **"The Astral Odyssey."** 

We are moving away from the "flat, sterile dashboard" of traditional productivity apps and leaning into a high-end, immersive RPG experience. The goal is to make life-tracking feel like a grand adventure. We achieve this through **Depth-First Design**: a philosophy where the UI is not a flat plane, but a series of floating, luminous artifacts suspended in a deep, cosmic void. 

By utilizing intentional asymmetry, overlapping "glass" containers, and high-contrast typography, we create a sense of momentum. This system rejects the rigid 12-column grid in favor of "Organic Quest-lines"—layouts where elements stack and overflow to guide the eye toward the next "reward."

---

## 2. Colors & Atmospheric Depth
Our palette is rooted in a "Cosmic Base" with "Magical Accents." We rely on light emission rather than physical borders to define space.

### The "No-Line" Rule
**Explicit Instruction:** Do not use 1px solid borders to section content. Boundaries must be defined solely through background color shifts. For example, a `surface_container_low` section should sit directly on a `background` base. The transition of color is the boundary.

### Surface Hierarchy & Nesting
Treat the UI as a series of nested magical artifacts.
*   **Base Layer:** `surface` (#0d0d16) – The infinite void.
*   **Primary Containers:** `surface_container` (#191923) – The main quest cards.
*   **Elevated Details:** `surface_bright` (#2b2b39) – Tooltips or active states.
*   **Nesting Logic:** Instead of a flat grid, place a `surface_container_highest` element inside a `surface_container_low` section to create "natural lift."

### The "Glass & Gradient" Rule
To capture the "magical" vibe, use **Glassmorphism** for floating elements (like navigation bars or modal popups). 
*   **Effect:** Apply `surface_variant` at 60% opacity with a `20px` backdrop-blur. 
*   **Signature Textures:** Use a linear gradient from `primary` (#d095ff) to `primary_container` (#c782ff) at a 135-degree angle for Hero CTAs. This creates a "glow" that feels alive, not static.

---

## 3. Typography
The typography system balances the whimsy of a game with the precision of a data-driven tracker.

*   **Display & Headlines (Plus Jakarta Sans):** Chosen for its modern, geometric clarity with a slight "tech-fantasy" feel. Use `display-lg` (3.5rem) for major milestones and level-ups. These should always feel "loud" and celebratory.
*   **Titles & Body (Manrope):** A highly functional, high-readability font. Use `title-lg` for quest names. Its balanced proportions keep the UI from feeling too "childish."
*   **Labels (Space Grotesk):** This is our "Data Layer." The monospace-adjacent feel of Space Grotesk should be used for stats, timers, and XP counts, providing a specialized "HUD" (Heads-Up Display) aesthetic.

---

## 4. Elevation & Depth
In this system, depth is "emitted," not "cast."

### The Layering Principle
Achieve hierarchy by stacking the `surface-container` tiers. 
*   **Example:** A Quest Card (`surface_container`) contains a Progress Bar (`surface_container_highest`). The contrast in tonal value creates the separation.

### Ambient Shadows & "Glow"
Standard black drop shadows are prohibited. When an element must "float" (e.g., a reward chest icon):
*   **Shadow Color:** Use a tinted version of the `primary` or `secondary` token at 8% opacity.
*   **Blur:** Use a massive `32px` to `48px` blur to simulate a soft, magical aura rather than a hard shadow.

### The "Ghost Border" Fallback
If accessibility requires a container edge, use a **Ghost Border**:
*   **Value:** `outline_variant` (#484751) at **15% opacity**. It should be felt, not seen.

---

## 5. Components

### Buttons (The "Action Orbs")
*   **Primary:** Solid `primary` (#d095ff) with `on_primary` (#490078) text. Apply a subtle outer glow using the `primary` color at 20% opacity.
*   **Secondary/Action:** `secondary` (#00e3fd) with `none` border. Use for "Side-Quests" or minor actions.
*   **Roundedness:** All buttons use `md` (1.5rem) or `full` (9999px) for a friendly, ergonomic feel.

### Progress Bars (The "Mana Gauges")
*   **Track:** `surface_container_highest` (#252532).
*   **Fill:** A gradient from `secondary` (#00e3fd) to `primary` (#d095ff).
*   **Animation:** Must include a subtle "pulse" or "shimmer" effect to indicate the quest is active.

### Cards & Lists
*   **Forbid dividers.** Use `spacing.6` (1.5rem) of vertical white space to separate list items. 
*   **Interactive Cards:** On hover, transition the background from `surface_container` to `surface_bright` and increase the `primary` outer glow.

### New Component: The "Loot Toast"
A specialized notification for rewards. Use `tertiary` (#ffe792) for the background with a glassmorphic blur. Position it with intentional asymmetry (e.g., 2rem from bottom, 3rem from right) to break the standard center-toast pattern.

---

## 6. Do’s and Don’ts

### Do:
*   **Use Asymmetry:** Offset icons or progress indicators to create a "dynamic flow."
*   **Embrace the Dark:** Ensure `background` (#0d0d16) remains the dominant surface to let the neon accents "pop."
*   **Layer Surfaces:** Use at least three tiers of `surface-container` colors in complex views to build architectural depth.

### Don't:
*   **Don't use 100% white text:** Use `on_surface` (#e7e4f1) for body text to reduce eye strain against the dark background.
*   **Don't use hard corners:** Every interactive element must have at least a `sm` (0.5rem) radius to maintain the playful RPG vibe.
*   **Don't use 1px dividers:** If you feel the need to separate two items, use a background color shift or increase the spacing scale. Lines are "barriers"; we want "flow."

---

## 7. Spacing & Rhythm
We utilize a **4px Base Grid**. Use `spacing.4` (1rem) for standard padding and `spacing.8` (2rem) for section breathing room. Because this is a "vibrant" system, don't be afraid of "wasteful" space—it creates the premium, editorial feel of a high-end game manual.```