---
name: Obsidian Telemetry
colors:
  surface: '#111417'
  surface-dim: '#111417'
  surface-bright: '#37393d'
  surface-container-lowest: '#0b0e11'
  surface-container-low: '#191c1f'
  surface-container: '#1d2023'
  surface-container-high: '#272a2e'
  surface-container-highest: '#323538'
  on-surface: '#e1e2e7'
  on-surface-variant: '#b9cac4'
  inverse-surface: '#e1e2e7'
  inverse-on-surface: '#2e3134'
  outline: '#84948f'
  outline-variant: '#3b4a45'
  surface-tint: '#00dfbe'
  primary: '#70ffe0'
  on-primary: '#00382e'
  primary-container: '#00e5c3'
  on-primary-container: '#006152'
  inverse-primary: '#006b5a'
  secondary: '#ffb86b'
  on-secondary: '#492900'
  secondary-container: '#ed9000'
  on-secondary-container: '#583300'
  tertiary: '#8fffa3'
  on-tertiary: '#003915'
  tertiary-container: '#50e67a'
  on-tertiary-container: '#00632a'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#42fdda'
  primary-fixed-dim: '#00dfbe'
  on-primary-fixed: '#00201a'
  on-primary-fixed-variant: '#005143'
  secondary-fixed: '#ffdcbc'
  secondary-fixed-dim: '#ffb86b'
  on-secondary-fixed: '#2c1700'
  on-secondary-fixed-variant: '#683d00'
  tertiary-fixed: '#6bff8f'
  tertiary-fixed-dim: '#4ae176'
  on-tertiary-fixed: '#002109'
  on-tertiary-fixed-variant: '#005321'
  background: '#111417'
  on-background: '#e1e2e7'
  surface-variant: '#323538'
typography:
  display-lg:
    fontFamily: JetBrains Mono
    fontSize: 56px
    fontWeight: '700'
    lineHeight: 60px
    letterSpacing: -0.04em
  display-lg-mobile:
    fontFamily: JetBrains Mono
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.03em
  display-md:
    fontFamily: JetBrains Mono
    fontSize: 36px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Space Grotesk
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-telemetry-lg:
    fontFamily: JetBrains Mono
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.02em
  label-telemetry-md:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.04em
  label-telemetry-sm:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '600'
    lineHeight: 12px
    letterSpacing: 0.08em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-lg: 1.5rem
  margin: 1rem
  margin-md: 1.5rem
  margin-lg: 2.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system synthesizes high-performance automotive cockpit instrumentation with the refined tactile hierarchy of Material 3 Expressive. Engineered for riders, drivers, and precision telemetry enthusiasts, the UI channels the technical discipline of high-end superbike digital clusters (Ducati Corse, BMW Motorrad TFTs) filtered through the sparse, transparent industrial elegance of Nothing OS.

The aesthetic balance combines:
- **Precision Instrumentation:** Monospaced data streams, razor-sharp segment lines, and hyper-structured tabular readouts designed for split-second glanceability at high velocity.
- **Material 3 Expressive Foundations:** Expressive container hierarchies, dynamic tonal layering, and deliberate tactile pill controls that temper raw industrial brutalism into an intuitive, polished digital surface.
- **Cockpit Dark Mode:** Deep, light-absorbing obsidian foundations that eliminate visual glare during night conditions while allowing high-chroma electric status indicators to pop with functional authority.

## Colors

The color architecture is built strictly on deep, light-absorbing obsidian backdrops with luminous, purposeful semantic accents that simulate automotive heads-up displays and physical instrument clusters.

- **Primary (`#00E5C3` - Electric Cyan-Teal):** The core telemetry focal point. Applied to primary speedometers, active route vectors, peak state gauges, interactive toggles, and primary callouts.
- **Secondary (`#FF9F1C` - Racing Amber):** Critical threshold, alert, and high-RPM zone indicators. Communicates performance thresholds, torque slip warnings, and thermal limits without triggering red critical stop states.
- **Tertiary (`#22C55E` - Live Emerald):** Real-time connectivity and optimal health indicators. Reserved for live Bluetooth/CAN-bus communication channels, optimal tire temp ranges, and continuous sync pulses.
- **Neutral Surface Palette:**
  - Base Obsidian Canvas: `#0B0E11`
  - Elevated Bento Cards: `#14191F`
  - Deep Wells & Inset Displays: `#080A0D`
  - Structural Ghost Borders: `#232B35`
  - High-Contrast Text / Readouts: `#F1F5F9`
  - Muted Technical Labels: `#64748B`

## Typography

The typographic hierarchy enforces absolute demarcation between functional metadata, structural headings, and live numerical streams:

- **Telemetry Readouts (`JetBrains Mono`):** All real-time telemetry metrics—such as km/h, RPM, tire pressure, voltage, lean angle, and lap timers—must use tabular numbers (`tnum`) to eliminate spatial jitter when figures cycle rapidly.
- **Structural Headings (`Space Grotesk`):** Clean, mechanical sans-serif with subtle geometric quirks that reinforce an advanced vehicular HUD context.
- **Interface Copy & Metadata (`Plus Jakarta Sans`):** Balanced, modern, highly legible sans-serif for secondary descriptions, settings labels, and contextual telemetry tooltips.
- **Telemetry Micro-Labels:** Formatted in uppercase monospace (`label-telemetry-sm`) with expanded tracking (`letterSpacing: 0.08em`) to mimic physical stamped instrumentation switches and race-spec avionics.

## Layout & Spacing

The layout is built around a dynamic telemetry bento-grid structure that prioritizes glanceable modularity across screen sizes.

- **Grid Architecture:** 
  - **Mobile / Portrait Cockpit (360px - 599px):** 4-column fluid layout with `margin: 1rem` and `gutter: 1rem`. Bento cards span full width (4 cols) or half width (2 cols) for synchronized telemetry gauges (e.g., front/rear tire PSI side-by-side).
  - **Tablet / Landscape Dash Mount (600px - 1023px):** 8-column layout with `margin: 1.5rem` and `gutter: 1rem`. Speedometer and primary RPM sweep dominate a central 4-column block, flanked by dual 2-column diagnostic tiles.
  - **Desktop / Ultra-Wide Console (1024px+):** 12-column layout with `margin: 2.5rem` and `gutter-lg: 1.5rem`. Layout scales fluidly to automotive aspect ratios (such as 16:9 or 21:9 in-dash displays).
- **Spatial Rhythm:** Tight inner padding (`space-md`) inside telemetry cards ensures maximum display area for high-contrast gauges. Modular tiles maintain a clean `space-lg` gap between functional clusters.

## Elevation & Depth

Visual hierarchy does not use diffuse, cloudy drop shadows; instead, it relies on strict tonal layering, crisp micro-borders, and deliberate glow fields typical of active optoelectronic panels:

- **Surface Tiers:**
  - **Level 0 (Canvas):** `#0B0E11` deep background.
  - **Level 1 (Telemetry Bento Modules):** `#14191F` with a continuous `1px solid #232B35` perimeter border.
  - **Level 2 (Active Focus & Insets):** `#1C232B` for pressed states, recessed gauge tracks, and inset slider paths.
  - **Level 3 (Floating HUD / Modals):** `#14191F` at 85% opacity with `backdrop-filter: blur(16px)` and high-contrast `1px solid #333F4E` edge highlight.
- **Instrument Glow:** Focused primary and secondary elements (active toggle switches, critical RPM band limits, status beacons) utilize a localized neon bloom (`0 0 12px rgba(0, 229, 195, 0.35)` or `0 0 12px rgba(255, 159, 28, 0.35)`) to signal live system activity.

## Shapes

The shape hierarchy combines aerodynamic curvature with engineered industrial corners:

- **Bento Telemetry Cards:** Defined with `rounded-xl` (1rem / 16px) to maintain structured modular alignment without sharp industrial edges.
- **Floating Pill Controls:** Interactive navigation docks, telemetry filters, and mode selectors utilize full pill caps (`rounded-full` / `9999px`) to maintain tactile ergonomics.
- **Micro Chips & Badges:** Use `rounded-md` (0.375rem / 6px) to ground them as technical chips and physical surface indicators.
- **Recessed Gauge Wells:** Interior cutouts for track telemetry and linear sliders follow `rounded-lg` (0.75rem / 12px) to nest symmetrically inside parent cards.

## Components

### Buttons
- **Primary Telemetry Trigger:** Solid `#00E5C3` background with `#080A0D` bold monospace text. Pressed state drops brightness to `#00B89D`. Includes subtle glow aura on focus.
- **Secondary / HUD Button:** Dark surface `#14191F` with `1px solid #232B35` border and `#F1F5F9` text. Hover state brightens border to `#00E5C3`.
- **Destructive / Reset Button:** `#1F1416` surface with `1px solid #E11D48` border and `#FB7185` text.

### Floating Pill Tab Navigation
- Positioned floating at the bottom center or side docking zone.
- Built using an ultra-low-profile pill container (`#14191F` with 80% opacity, `backdrop-filter: blur(20px)`, and `1px solid #232B35`).
- Active tab pill shifts to `#232B35` with an electric cyan `#00E5C3` icon and micro status dot.
- Inactive tabs display at `#64748B` with immediate color transition on hover/touch.

### Telemetry Bento Cards
- `#14191F` background, `1px solid #232B35` structural perimeter border.
- Header row contains the `label-telemetry-sm` uppercase title on the left and an optional status pill (e.g., "LIVE", "CAN-OK") on the right.
- Central area features oversized `display-lg` tabular numerals with metric units (`KM/H`, `RPM`, `V`) locked in muted secondary scale next to the value.

### Chips & Telemetry Tags
- **Bluetooth Status Chip:** `#14191F` base, `1px solid #232B35`, featuring a pulsing `6px` circular badge illuminated in `#22C55E` accompanied by `label-telemetry-sm` text.
- **Threshold Zone Chip:** Outlined badge with `#FF9F1C` text and `rgba(255, 159, 28, 0.12)` fill for high thermal/RPM warnings.

### Checkboxes, Switches & Radios
- **Cockpit Toggle Switch:** Recessed pill track (`#080A0D`, `1px solid #232B35`) with an illuminated circular thumb. When active, track edge and thumb illuminate in `#00E5C3`.
- **Radio & Checkboxes:** Squared-off `rounded-md` selection targets with inset check states utilizing `#00E5C3` fills.

### Input Fields
- Dark well background (`#080A0D`), `1px solid #232B35`, `rounded-lg` corners.
- Active focus state changes border to `#00E5C3` with zero outer ring bloom, keeping edges sharp.
- Placeholder text in `#64748B`, user entry text rendered in high-contrast `#F1F5F9` using `JetBrains Mono`.

### Specialized Cockpit Gauges (Additional)
- **Linear Bar-Graph Strips:** Segmented RPM and throttle readouts built with 2px gap intervals between tick marks. Fills dynamically shift from Electric Cyan (`#00E5C3`) across low-to-mid bands, stepping directly into Racing Amber (`#FF9F1C`) in the top 15% range.