# Quick Reference - GUI Implementation

## New/Modified Files Overview

### Core Data Classes

#### `PlayerRPGData.java` - MODIFIED
- Added `classLevel` and `classExperience` fields
- Added `addClassExperience(int)` method for XP and level ups
- XP required: `level * 100` per level

#### `RPGClass.java` - MODIFIED
- Added `iconPath`, `isSubclass`, `parentClassId`, `requiredLevel` fields
- Supports both main classes and subclasses

#### `SkillNode.java` - MODIFIED
- Added `x`, `y`, `iconPath` fields for tree positioning
- Constructor overload for positioned nodes

### Registry Classes

#### `ClassRegistry.java` - MODIFIED
- Added `getMainClasses()` and `getSubclasses()` methods
- 6 main classes: Warrior, Mage, Rogue, Ranger, Tank, Priest
- 12 subclasses: 2 per main class, unlock at level 10

#### `SkillTreeRegistry.java` - MODIFIED
- 6 complete skill trees with 4-5 skills each
- Tree structure with X,Y positioning
- Added Ranger, Tank, Priest trees

### GUI Screens

#### `ClassSelectionScreen.java` - MAJOR REWRITE
- 3-column grid of fancy class cards (120x140px)
- Class-specific colors with gradients
- Hover tooltips and effects
- Opens SubclassSelectionScreen on click

#### `SubclassSelectionScreen.java` - NEW FILE
- Shows 2 subclasses per parent
- Purple theme, level-gated selection
- Back button to main selection

#### `SkillTreeScreen.java` - COMPLETE REWRITE
- Black background with tree layout
- 40x40 nodes with connections
- Rich tooltips on hover
- Class level display

#### `StatsDropdownOverlay.java` - MODIFIED
- Golden gradient border
- 8x8 pixel icons for each stat
- Color-coded display

## Key Features

### Class System
- 6 main classes with unique colors and stats
- 12 subclasses (2 per main class)
- Level 10 requirement for subclasses
- Class leveling with XP progression

### Skill Trees
- Tree structure with positioned nodes
- Black background
- Connection lines show prerequisites
- Hover tooltips with full details
- Color-coded locked/unlocked states

### Visual Polish
- Class-specific color schemes
- Gradient borders and backgrounds
- Drop shadows throughout
- Hover effects (thicker borders, white highlights)
- Icon placeholders ready for textures

## Code Statistics
- Files Modified: 8
- Files Created: 4
- Lines Added: ~1,300
- Net Change: ~1,120 lines

## Testing
1. Open Class Selection Book
2. See 6 colorful classes
3. Click to open subclass screen
4. Open skill tree to see tree layout
5. Hover for tooltips
6. Toggle stats panel for icons

## What's Next
- Add network packets for class application
- Implement skill point allocation
- Add actual skill effects
- Create PNG icon textures
