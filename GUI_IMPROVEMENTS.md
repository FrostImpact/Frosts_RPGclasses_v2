# GUI Improvements Documentation

## Overview
This document details all the improvements made to the RPG Classes v2 GUI system, including the skill tree, class selection, and stat displays.

## 1. Class System Enhancements

### 6 Main Archetypes Added
The following main classes are now available:
1. **Warrior** - Strong melee fighter with high health and damage
2. **Mage** - Powerful spellcaster with high mana and magical abilities
3. **Rogue** - Swift and agile fighter with high speed and critical hits
4. **Ranger** - Skilled archer who excels at ranged combat
5. **Tank** - Heavily armored defender with high health and defense
6. **Priest** - Holy healer with powerful support abilities

### 12 Subclasses Added (2 per archetype)

#### Warrior Subclasses (Unlock at Level 10)
- **Berserker** - Rage-fueled warrior trading defense for overwhelming offense
- **Paladin** - Holy warrior who protects allies and smites evil

#### Mage Subclasses (Unlock at Level 10)
- **Pyromancer** - Specializes in destructive fire magic
- **Frost Mage** - Controls ice and slows enemies

#### Rogue Subclasses (Unlock at Level 10)
- **Assassin** - Focused on critical strikes and stealth
- **Shadow Dancer** - Manipulates shadows for mobility

#### Ranger Subclasses (Unlock at Level 10)
- **Marksman** - Unmatched accuracy and precision
- **Beast Master** - Commands animal companions

#### Tank Subclasses (Unlock at Level 10)
- **Guardian** - Defensive tank who protects allies
- **Juggernaut** - Unstoppable force that crushes enemies

#### Priest Subclasses (Unlock at Level 10)
- **Cleric** - Focused on healing and support
- **Templar** - Warrior priest who fights on the front lines

## 2. Class Leveling System

### PlayerRPGData Enhancements
Added new fields:
- `classLevel` - The player's current class level (starts at 1)
- `classExperience` - Experience points for class progression
- `addClassExperience(int amount)` - Method to add XP and handle level ups
  - Requires 100 XP per level (scales with level: level * 100)
  - Automatically handles level ups when enough XP is gained

## 3. Class Selection Screen Improvements

### Visual Enhancements
- **3-column grid layout** instead of 2-column for better organization
- **Fancy card-based design** with:
  - Colored backgrounds based on class type (red for warrior, blue for mage, etc.)
  - Shadow effects for depth
  - Gradient backgrounds (darker at bottom)
  - Thicker borders when hovered (3px vs 2px)
  - Animated hover effects with visual indicators

### Class-Specific Colors
- Warrior: Red (#DD4444)
- Mage: Blue (#4444DD)
- Rogue: Green (#44DD44)
- Ranger: Green (#44DD44)
- Tank: Yellow (#DDDD44)
- Priest: Light Yellow (#FFFFAA)

### Interactive Features
- **Hover tooltips** - Detailed class information appears in bottom panel when hovering
- **Subclass indicators** - Shows "+X specs" for classes with subclasses
- **Click to navigate** - Clicking a main class opens the subclass selection screen
- **Word-wrapped descriptions** - Long descriptions automatically wrap to fit panel

### RPGClass Enhancements
New properties added:
- `iconPath` - Path to class icon texture (placeholder system ready)
- `isSubclass` - Boolean flag to distinguish main classes from subclasses
- `parentClassId` - ID of the parent class (for subclasses)
- `requiredLevel` - Minimum class level needed to unlock (10 for subclasses)

## 4. Subclass Selection Screen (NEW)

A completely new screen for choosing specializations:

### Features
- Shows all subclasses for the selected parent class
- Displays player's current class level
- **Level-gated selection** - Subclasses locked until level 10
  - Locked buttons show required level
  - Unlocked at level 10+
- Purple/violet color scheme to distinguish from main class selection
- Back button to return to main class selection
- Real-time tooltip showing subclass description on hover

## 5. Skill Tree Screen - Complete Overhaul

### Black Background
- Solid black background (#000000) for professional appearance
- Better contrast for skill nodes and text

### Tree-like Structure
- **Positioned nodes** with X, Y coordinates
  - Root nodes at top (Y=0)
  - Branches spread horizontally (X coordinates)
  - Progressive tiers moving down (Y=1, Y=2, etc.)
- **Connection lines** between prerequisite skills
  - Lines drawn from parent to child nodes
  - Visual representation of skill dependencies

### Skill Node Enhancements

#### SkillNode Class Updates
- Added `x` and `y` position fields
- Added `iconPath` for future texture support
- Constructor overload supporting positions and icons

#### Visual Design
- **40x40 pixel nodes** with proper spacing (80 pixels between nodes)
- **Color-coded by state**:
  - Unlocked: Blue (#4488FF) with light blue border
  - Locked: Dark gray (#444444) with gray border
  - Hovered: White border (3px thickness)
- **Shadow effects** for depth
- **Icon placeholders** - Colored squares with patterns based on skill type:
  - Offensive skills: Red
  - Defensive skills: Green
  - Magic skills: Blue
  - Hybrid skills: Purple

#### Interactive Tooltips
Comprehensive tooltips on hover showing:
- **Skill name** (yellow, bold)
- **Description** (word-wrapped to 200px width)
- **Max level** - How many times it can be upgraded
- **Point cost** - Skill points needed per level
- **Required level** - Class level needed to unlock
- **Prerequisites** - List of required skills

Tooltip features:
- Smart positioning to avoid screen edges
- Bordered with purple/violet theme (#AA44FF)
- Formatted with Minecraft color codes (§7 for gray, §e for yellow, etc.)

### Class Level Display
- Shows current class level in top-left: "Class Level: X"
- Class level displayed in title: "Warrior Skills - Level X"
- Instructions in bottom-right corner

### Skill Trees for All 6 Classes
Each class has a unique skill tree with 4-5 skills in a branching structure:

#### Warrior Tree
- Power Strike (root) → Branches to Toughness, Battle Cry, and Whirlwind

#### Mage Tree  
- Spell Power (root) → Branches to Mana Pool, Mana Regen, and Fireball

#### Rogue Tree
- Agility (root) → Branches to Critical Eye, Evasion, and Shadow Step

#### Ranger Tree
- Precision (root) → Branches to Rapid Fire and Tracking

#### Tank Tree
- Iron Skin (root) → Branches to Shield Wall and Taunt

#### Priest Tree
- Divine Blessing (root) → Branches to Holy Light and Resurrection

## 6. Stats Dropdown Overlay Improvements

### Icon System
- **8x8 pixel icons** next to each stat
- Color-coded to match stat type:
  - Health: Red (#FF5555)
  - Mana: Cyan (#55FFFF)
  - Damage: Orange (#FFAA00)
  - Defense: Light Blue (#00AAFF)
  - Move Speed: Green (#55FF55)
  - Attack Speed: Pink (#FF55FF)
  - Cooldown: Purple (#AA55FF)
  - Health Regen: Light Red (#FF8888)
  - Mana Regen: Light Cyan (#88FFFF)

### Icon Design
- Colored square background
- White border on top and left
- Gray border on bottom and right (3D effect)
- White center dot for detail

### Enhanced Border
- Changed from gray to golden gradient
  - Top/Left: Gold (#FFDD00)
  - Bottom/Right: Dark gold (#AA8800)

## 7. General Visual Improvements

### Consistent Styling
- All panels use dark backgrounds (#DD000000 or #EE000000)
- Consistent border thickness (2-3px)
- Shadow effects on all major elements
- Hover effects throughout

### Typography
- Title shadows for depth (3 layers)
- Gold color for important titles (#FFDD00)
- Gray for subtitles and hints (#AAAAAA, #CCCCCC)
- White for main content

### Color Scheme
- Primary: Gold/Yellow (#FFDD00)
- Secondary: Purple/Violet (#AA44FF)
- Accents: Class-specific colors
- Background: Pure black or dark gray

## 8. Technical Implementation Details

### Class Registry Methods
- `getMainClasses()` - Returns only non-subclass classes
- `getSubclasses(parentClassId)` - Returns subclasses for a parent

### Navigation Flow
1. Player opens Class Selection Screen
2. Clicks on a main class (e.g., Warrior)
3. If subclasses exist, opens Subclass Selection Screen
4. Shows available subclasses with level requirements
5. Player selects subclass if level requirement met

### Data Persistence
All new fields are properly serialized in PlayerRPGData codec:
- `classLevel` - Integer field
- `classExperience` - Integer field

## 9. Future Enhancement Opportunities

### Icons
Currently using colored placeholder icons. Ready to accept PNG textures at:
- Class icons: `rpgclasses_v2:textures/gui/icons/{classname}.png`
- Skill icons: Can be added via `SkillNode.iconPath` field

### Functionality
The visual foundation is complete. Still needed:
- Packet system to actually apply class selection to player
- Skill point allocation system
- Skill activation/effects implementation
- Experience gain from combat/quests

### Additional Polish
- Animation effects for transitions
- Sound effects for selections
- Particle effects for skill previews
- More detailed tooltips with stat bonuses

## Summary

This update transforms the basic placeholder GUIs into a professional, visually appealing system with:
- ✅ 6 main classes + 12 subclasses
- ✅ Class leveling with XP progression
- ✅ Beautiful tree-structured skill trees with tooltips
- ✅ Fancy class selection with hover effects
- ✅ Icon system throughout all GUIs
- ✅ Consistent, polished visual design
- ✅ Level-gated subclass progression
- ✅ Comprehensive information display

The GUI system is now production-ready and provides an excellent foundation for the gameplay mechanics to be implemented on top of it.
