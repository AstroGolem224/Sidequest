# Design System Document

## 1. Creative North Star: "The Solar Observatory"
This design system moves away from the cold, clinical tropes of traditional sci-fi. Instead, it embraces a "Solar Observatory" aesthetic—an interface that feels like high-end aerospace technology operating within the warmth of a star’s corona. We prioritize **Atmospheric Depth** over flat grids and **Intentional Asymmetry** over rigid templates. The goal is a digital experience that feels bespoke, cinematic, and whisper-quiet.

**The signature look is achieved by:**
*   **Tonal Layering:** Using shadows and surface shifts instead of lines.
*   **Luminous Accents:** Using ambers and golds to guide the eye like light through a lens.
*   **Negative Space as a Luxury:** Utilizing the spacing scale to ensure the UI feels expansive, never cluttered.

---

## 2. Colors & Surface Philosophy
The palette is anchored in deep charcoals (`surface`) with an energetic core of warm ambers (`primary`) and soft golds (`secondary`).

### The "No-Line" Rule
**Explicit Instruction:** Do not use 1px solid borders to define sections. Boundaries must be defined solely through background color shifts. For example, a `surface-container-low` section should sit directly against a `surface` background. The shift in value provides the containment; the eye does not need a line to perceive a edge.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers. We use the Material tiers to create "nested" depth:
*   **Base Layer:** `surface` (#131313) or `surface-container-lowest` (#0e0e0e) for the deep background.
*   **Interactive Cards:** `surface-container` (#201f1f) or `surface-variant` (#353534).
*   **Elevated Overlays:** `surface-bright` (#393939) for high-priority floating elements.

### The Glass & Gradient Rule
To achieve the "cool sci-fi" feel, use **Glassmorphism** for floating panels (e.g., sidebars or modals). 
*   **Formula:** Background: `rgba(32, 31, 31, 0.6)` (`surface-container`) + `backdrop-filter: blur(20px)`.
*   **Signature Gradients:** For primary CTAs, transition from `primary` (#ffd79b) to `primary-container` (#ffb300) at a 135-degree angle. This adds "soul" and mimics the glow of an amber display.

---

## 3. Typography: Modern Editorial
We use **Plus Jakarta Sans** for its geometric clarity and modern "tech-humanist" feel.

*   **Display Scale (`display-lg` to `display-sm`):** Reserved for high-impact hero moments. Use these sparingly with wide tracking (-0.02em) to create an authoritative, editorial look.
*   **Headline & Title:** Use `headline-md` for section starts. The transition from a `headline-sm` (1.5rem) to a `body-lg` (1rem) creates a sophisticated hierarchy that feels more "magazine" than "dashboard."
*   **Functional Labels:** `label-md` and `label-sm` should be used for metadata. Pair them with `primary` or `secondary` colors to highlight key data points within the charcoal environment.

---

## 4. Elevation & Depth
Depth is not a "drop shadow"; it is an **Ambient Bloom**.

*   **The Layering Principle:** Stacking tiers is the primary method of organization. Place a `surface-container-highest` card on a `surface-container-low` section to create natural lift.
*   **Ambient Shadows:** If an element must float (e.g., a dropdown), use a massive blur. 
    *   *Shadow:* `0 24px 48px rgba(0, 0, 0, 0.5)`. The shadow should feel like a soft occlusion of light, not a hard edge.
*   **The Ghost Border Fallback:** If accessibility requires a border, use the `outline-variant` token at **15% opacity**. High-contrast, 100% opaque borders are strictly forbidden.
*   **Refined Borders:** On glass panels, use a top-weighted inner stroke of `outline` (#9e8e78) at 20% opacity to mimic the "catch-light" on the edge of a glass pane.

---

## 5. Components

### Buttons
*   **Primary:** Gradient fill (`primary` to `primary-container`), no border, `DEFAULT` (0.25rem) radius. Text color: `on-primary`.
*   **Secondary:** Ghost style. Transparent background with a `Ghost Border` and `primary` text.
*   **Tertiary:** Text-only with `label-md` styling, using `primary` color for the label.

### Input Fields
*   **Base:** Background `surface-container-low`, `Ghost Border` (15% opacity `outline-variant`).
*   **Active State:** The border opacity increases to 40%, and a 2px "glow" (outer shadow) of `primary` color is applied with a 12px blur.
*   **Labels:** Always use `label-md` positioned above the field, never inside.

### Cards & Lists
*   **No Dividers:** Forbid the use of line dividers. Use `spacing-6` (2rem) of vertical whitespace to separate list items. 
*   **Hover State:** Shift the background from `surface-container` to `surface-container-high`. Do not move the element; the color shift is enough.

### The "Solar" Progress Bar (Custom Component)
A thick, `surface-container-highest` track with a `primary` fill that features a soft outer glow (`box-shadow: 0 0 8px #ffd79b`) to simulate a glowing fiber-optic filament.

---

## 6. Do’s and Don’ts

### Do
*   **Use Asymmetry:** Place a large `display-md` title off-center to create a bespoke, high-end feel.
*   **Lean into the Spacing Scale:** Use `spacing-12` (4rem) between major sections to let the "cool" aesthetic breathe.
*   **Tone-on-Tone:** Use `on-surface-variant` for secondary text to keep the contrast sophisticated and easy on the eyes.

### Don’t
*   **Don't use HUD clutter:** Avoid chevrons, brackets, or "techy" corners unless they serve a direct functional purpose.
*   **Don't use pure white:** The brightest color should be `on-surface` (#e5e2e1). Pure #FFFFFF will break the warm, sophisticated atmosphere.
*   **Don't use sharp corners:** While we are "sci-fi," we are "sophisticated." Use the `DEFAULT` (0.25rem) or `md` (0.375rem) roundedness to keep the interface feeling premium and tactile.