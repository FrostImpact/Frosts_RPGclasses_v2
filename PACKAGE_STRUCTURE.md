# Package Structure and Organization

This document explains the package organization of the Frost's RPG Classes v2 mod.

## Overview

The codebase is organized into logical packages based on functionality:

```
net.frostimpact.rpgclasses_v2/
├── rpg/                    # Core RPG mechanics
├── combat/                 # Combat system
├── item/weapon/            # Weapon items
├── client/                 # Client-side code
├── networking/             # Network packets
├── event/                  # Server event handlers
└── entity/                 # Entity-related code
```

## Package Descriptions

### `rpg/` - Core RPG Mechanics
Contains the foundational RPG systems:
- **ModAttachments.java** - Registration of data attachments
- **PlayerRPGData.java** - Player mana, cooldowns, and class data
- **stats/** - Player stat system
  - **StatType.java** - Enum of all stat types
  - **StatModifier.java** - Individual stat modifiers
  - **PlayerStats.java** - Stat aggregation and management
  - **StatsDropdownOverlay.java** - UI for viewing stats

### `combat/` - Combat System
Self-contained combat mechanics:
- **CombatConfig.java** - Combat constants and configuration
- **CombatEventHandler.java** - Event handling for melee combat
- **ComboTracker.java** - Combo system tracking
- **slash/** - Slash particle effects
  - **SlashAnimation.java** - Animation data
  - **SlashRenderer.java** - Particle rendering

### `item/weapon/` - Weapon Items
All weapon-related items:
- **WeaponType.java** - Weapon type definitions
- **MeleeWeaponItem.java** - Base melee weapon class
- **WeaponStats.java** - Weapon stat builder
- **WeaponStatHandler.java** - Equipment stat management
- **ModWeapons.java** - Weapon registry
- **sword/** - Sword implementations
  - **ShortswordItem.java** - Fast weapon
  - **LongswordItem.java** - Balanced weapon
  - **ClaymoreItem.java** - Heavy weapon

### `client/` - Client-Side Code
Client-only functionality:
- **event/** - Client event handlers
  - **ClientEvents.java** - Input handling
- **overlay/** - HUD overlays
  - **HealthBarOverlay.java** - Health bar
  - **ManaBarOverlay.java** - Mana bar

### `networking/` - Network Communication
Packet definitions and registration:
- **ModMessages.java** - Packet registry
- **packet/** - Packet implementations
  - **PacketSyncMana.java** - Mana synchronization
  - **PacketSyncStats.java** - Stats synchronization

### `event/` - Server Events
Server-side event handlers:
- **ServerEvents.java** - Main server tick events
  - Cooldown ticking
  - Stat duration management
  - Mana regeneration
  - Movement speed application

### `entity/` - Entity Code
Entity-related rendering and logic:
- **EntityHealthBar.java** - Overhead health bar rendering

## Design Principles

### Separation of Concerns
Each package has a single, well-defined responsibility:
- **rpg/** focuses on core RPG data and stats
- **combat/** handles all combat mechanics
- **item/** contains all item definitions

### No Cross-Dependencies Between Siblings
- **combat/** and **item/weapon/** are independent
- Both depend on **rpg/stats/** but not on each other
- This allows either system to be modified without affecting the other

### Clear Hierarchy
- Core systems (**rpg/**) are at the root level
- Specialized systems (**combat/**, **item/**) are separate
- Client code is isolated in **client/**
- Common infrastructure (**networking/**, **event/**) is accessible to all

## Migration Notes

This structure was reorganized from the original layout where combat and weapons were nested under `rpg/stats/`. The new structure:

1. **Moved** `rpg/stats/combat/` → `combat/`
2. **Moved** `rpg/stats/weapon/` → `item/weapon/`
3. **Kept** `rpg/stats/` for actual stat-related code

This reorganization improves:
- Code discoverability
- Logical grouping
- Package cohesion
- Future maintainability
