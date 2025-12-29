# Visual Changes Guide

## Overview
This document describes the visual changes made to the RPG Classes v2 mod UI.

## 1. Inventory Screen - Fixed Crash + Skill Tree Button

### Before:
- Game crashed when opening inventory
- No button visible

### After:
- ✅ Inventory opens successfully
- "S" button visible next to recipe book button
- Clicking "S" opens skill tree for current class

**Location:** Left side of inventory screen, below recipe book button
**Button:** 20x20 pixels, labeled "S"

---

## 2. Pause/Escape Screen - New Stats Button

### Before:
- Stats dropdown only accessible via K keybind
- No visual indicator in pause menu

### After:
- ✅ "S" button visible in top-right corner
- Clicking button toggles stats dropdown panel
- Stats panel shows health, mana, damage, defense, etc.

**Location:** Top-right corner of pause screen
**Button:** 20x20 pixels, labeled "S"
**Position:** 25 pixels from right edge, 5 pixels from top

---

## 3. Class Selection Screen - Complete Visual Overhaul

### Before:
- Plain text title: "Select Your Class"
- Vertical list of class buttons (200x20 pixels)
- Basic selected class info at bottom
- "Placeholder" message visible
- No decorative elements

### After - Professional RPG Theme:

#### Title Section:
- **Title:** "SELECT YOUR CLASS" (golden color: #FFDD00)
- **Shadow effect:** Black shadow offset 2 pixels for depth
- **Subtitle:** "Choose wisely, for this will shape your destiny" (gray)

#### Main Panel:
- **Background:** Dark semi-transparent (alpha: DD)
- **Borders:** Gradient blue (#4488FF → #2244AA, 2px thick)
- **Corners:** Golden decorative corners (#FFDD00, 10px)
- **Size:** 400x350 pixels, centered on screen

#### Class Selection Grid:
- **Layout:** 2-column grid (configurable)
- **Card Size:** 180x80 pixels (much larger than before)
- **Spacing:** 10 pixels between cards
- **Position:** Starts 80 pixels from top

#### Selected Class Info Panel:
- **Background:** Dark semi-transparent (alpha: EE)
- **Borders:** Green (#55FF55 → #33AA33, 1px thick)
- **Size:** 380x50 pixels
- **Position:** 120 pixels from bottom

**Info Displayed:**
1. "Selected: [Class Name]" (green: #55FF55)
2. Class description (light gray: #CCCCCC)
3. Flavor text: "Forge your legend as a [Class]" (dark gray: #888888)

#### Buttons:
- **Confirm Button:** 150x25 pixels, 60 pixels from bottom
- **Close Button:** 100x20 pixels, 30 pixels from bottom
- Both centered horizontally

### Color Palette:
- **Golden/Yellow:** #FFDD00 (titles, corners)
- **Blue Gradient:** #4488FF → #2244AA (panel borders)
- **Green:** #55FF55 → #33AA33 (selection highlights)
- **Gray Text:** #AAAAAA (subtitle), #CCCCCC (description), #888888 (flavor)
- **Dark Backgrounds:** #DD000000, #EE000000 (semi-transparent)

### Layout Constants (All Configurable):
```
PANEL_WIDTH = 400
PANEL_HEIGHT = 350
PANEL_Y_OFFSET = 20
CARD_WIDTH = 180
CARD_HEIGHT = 80
CARD_SPACING = 10
GRID_COLUMNS = 2
GRID_START_Y = 80
CORNER_SIZE = 10
CORNER_COLOR = 0xFFFFDD00
CONFIRM_BUTTON_WIDTH = 150
CONFIRM_BUTTON_HEIGHT = 25
CONFIRM_BUTTON_BOTTOM_OFFSET = 60
CLOSE_BUTTON_WIDTH = 100
CLOSE_BUTTON_HEIGHT = 20
CLOSE_BUTTON_BOTTOM_OFFSET = 30
INFO_PANEL_BOTTOM_OFFSET = 120
```

---

## Technical Details

### Rendering Order:
1. Dark background (renderBackground)
2. Main panel background (dark fill)
3. Panel borders (gradient)
4. Super.render() - buttons and widgets
5. Title with shadow effect
6. Subtitle
7. Info panel (if class selected)
8. Decorative corners

### Cross-Platform Compatibility:
- All text uses plain ASCII characters (no emojis)
- Colors specified in ARGB hex format
- Compatible with all font renderers

### Performance:
- Efficient rendering using GuiGraphics fill operations
- Minimal string allocations with cached className variable
- No unnecessary object creation in render loop

---

## User Experience Improvements

### Inventory Screen:
- **Before:** Crash → frustration
- **After:** Smooth opening → skill tree access

### Pause Screen:
- **Before:** Hidden feature (keybind only)
- **After:** Visible button → discoverable feature

### Class Selection:
- **Before:** Utilitarian, placeholder appearance
- **After:** Polished, professional, RPG-themed experience
  - Clear visual hierarchy
  - Better readability
  - More engaging presentation
  - Easier class comparison
  - Professional aesthetic matching the mod theme
