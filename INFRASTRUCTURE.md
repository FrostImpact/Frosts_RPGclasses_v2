# Frost's RPG Classes v2 - Infrastructure Setup

This document describes the foundational infrastructure that has been set up for the mod.

## File Structure

```
rpgclasses_v2/
├── build.gradle                    # Gradle build configuration with NeoForge MDK
├── gradle.properties               # Mod metadata and version information
├── settings.gradle                 # Gradle settings
├── gradlew / gradlew.bat          # Gradle wrapper scripts
├── .gitignore                      # Excludes build artifacts and dependencies
└── src/main/
    ├── java/net/frostimpact/rpgclasses_v2/
    │   ├── RpgClassesMod.java                          # Main mod class
    │   ├── rpg/
    │   │   ├── ModAttachments.java                     # Data attachment registration
    │   │   ├── PlayerRPGData.java                      # Mana, cooldowns, class tracking
    │   │   └── stats/
    │   │       ├── PlayerStats.java                    # Stat modifier management
    │   │       ├── StatType.java                       # Enum of all stat types
    │   │       ├── StatModifier.java                   # Individual stat modifier
    │   │       └── StatsDropdownOverlay.java           # Stats UI overlay
    │   ├── combat/
    │   │   ├── CombatConfig.java                       # Combat system constants
    │   │   ├── CombatEventHandler.java                 # Combat event handling
    │   │   ├── ComboTracker.java                       # Combo system tracking
    │   │   └── slash/
    │   │       ├── SlashAnimation.java                 # Slash animation data
    │   │       └── SlashRenderer.java                  # Slash particle rendering
    │   ├── item/weapon/
    │   │   ├── MeleeWeaponItem.java                    # Base melee weapon class
    │   │   ├── ModWeapons.java                         # Weapon registry
    │   │   ├── WeaponStatHandler.java                  # Weapon stat application
    │   │   ├── WeaponStats.java                        # Weapon stat builder
    │   │   ├── WeaponType.java                         # Weapon type enum
    │   │   └── sword/
    │   │       ├── ShortswordItem.java                 # Shortsword implementation
    │   │       ├── LongswordItem.java                  # Longsword implementation
    │   │       └── ClaymoreItem.java                   # Claymore implementation
    │   ├── entity/
    │   │   └── EntityHealthBar.java                    # Entity health bar rendering
    │   ├── client/
    │   │   ├── event/
    │   │   │   └── ClientEvents.java                   # Client-side input handling
    │   │   └── overlay/
    │   │       ├── HealthBarOverlay.java               # Ornate health bar HUD
    │   │       └── ManaBarOverlay.java                 # Ornate mana bar HUD
    │   ├── networking/
    │   │   ├── ModMessages.java                        # Packet registration
    │   │   └── packet/
    │   │       ├── PacketSyncMana.java                 # Mana sync packet
    │   │       └── PacketSyncStats.java                # Stats sync packet
    │   └── event/
    │       └── ServerEvents.java                       # Server-side event handling
    └── resources/
        ├── META-INF/
        │   └── neoforge.mods.toml                      # Mod metadata file
        └── assets/rpgclasses_v2/
            ├── lang/
            │   └── en_us.json                          # English localization
            └── models/item/                            # Item models
```

## Components Implemented

### 1. Player Stats System
**Location:** `rpg/stats/`

- **StatType.java**: Enum defining all stat types:
  - MAX_HEALTH, HEALTH_REGEN, MAX_MANA, MANA_REGEN
  - DAMAGE, ATTACK_SPEED, DEFENSE, MOVE_SPEED
  - COOLDOWN_REDUCTION

- **StatModifier.java**: Represents a single stat modifier with:
  - Source ID (String)
  - Stat type (StatType enum)
  - Value (double, percent-based)
  - Duration (int ticks, -1 for permanent)
  - Codec and StreamCodec for serialization

- **PlayerStats.java**: Manages all stat modifiers for a player:
  - Add/remove modifiers by source and type
  - Calculate final stat values (sum of all modifiers)
  - Tick method to expire timed modifiers
  - Codec for NBT serialization

### 2. Core Player RPG Data
**Location:** `rpg/PlayerRPGData.java`

Stores essential player data:
- Current and max mana (default 100)
- Ability cooldowns (Map<String, Integer>)
- Current class (default "NONE")

Key methods:
- `getMana()`, `setMana()`, `getMaxMana()`, `setMaxMana()`
- `useMana()`, `regenMana()` - with clamping
- `getAbilityCooldown()`, `setAbilityCooldown()`
- `tickCooldowns()` - auto-decrements and removes expired
- `clearAllCooldowns()`
- `getCurrentClass()`, `setCurrentClass()`

### 3. Mod Attachments
**Location:** `rpg/ModAttachments.java`

Registers NeoForge data attachments:
- `PLAYER_RPG` - PlayerRPGData attachment
- `PLAYER_STATS` - PlayerStats attachment

Uses DeferredRegister with serializable attachment types.

### 4. Networking
**Location:** `networking/`

Implements NeoForge CustomPacketPayload system:

- **PacketSyncMana**: Syncs mana and maxMana to client
- **PacketSyncStats**: Syncs full stat modifier list to client
- **ModMessages**: Registers packets and provides helper methods

### 5. Client Overlays
**Location:** `client/overlay/`

**HealthBarOverlay.java**:
- Position: Bottom-left (screenWidth/2 - 105, screenHeight - 50)
- Dimensions: 100x7 pixels
- Ornate bronze border (0xFF8b7355) with gold corners (0xFFd4af37)
- Dynamic gradient based on health:
  - High (>60%): Green gradient
  - Medium (30-60%): Yellow gradient
  - Low (<30%): Red gradient
- Pulsing red effect when health < 25%
- Centered health text display

**ManaBarOverlay.java**:
- Position: Bottom-right (screenWidth/2 + 5, screenHeight - 50)
- Dimensions: 100x7 pixels
- Blue-silver border (0xFF5577aa) with cyan corners (0xFF66ccff)
- Cyan-to-blue gradient fill (0xFF00bfff to 0xFF0066cc)
- Moving shimmer effect
- Pulsing blue effect when mana < 15%
- Centered mana text display

Both overlays registered above VanillaGuiLayers.FOOD_LEVEL.

**StatsDropdownOverlay.java**:
- Position: Top-right corner of screen
- Interactive dropdown menu showing all player stats
- Click button to toggle expansion
- Displays current values for all 9 stat types with color coding

### 6. Combat System
**Location:** `combat/`

**CombatConfig.java**:
- Centralized configuration for combat mechanics
- Base attack cooldown settings
- Combo system parameters (reset time, max combo count)
- Damage multipliers for combo finishers
- Particle effect settings

**CombatEventHandler.java**:
- Handles all melee weapon combat events
- Disables block breaking with melee weapons
- Disables vanilla jump crits for melee weapons
- Manages attack cooldowns with weapon speed multipliers
- Tracks combo progression per weapon type
- Applies damage bonuses from stats and combo finishers
- Spawns slash particle effects

**ComboTracker.java**:
- Tracks combo state for each player
- Weapon-type-specific combo counts (3-hit for shortswords, 4-hit for others)
- Auto-resets combos after timeout
- Thread-safe player data management

**slash/SlashAnimation.java & SlashRenderer.java**:
- Renders visual slash particle effects
- Different patterns for different weapon types and combo stages
- Bright yellow/gold particle colors
- Server-side particle spawning

### 7. Weapon System
**Location:** `item/weapon/`

**WeaponType.java**:
- Enum defining weapon types (SHORTSWORD, LONGSWORD, CLAYMORE)
- Each type has unique combo count and speed multiplier
- Shortsword: 3-hit combo, 0.6x speed (faster)
- Longsword: 4-hit combo, 1.0x speed (balanced)
- Claymore: 4-hit combo, 1.4x speed (slower)

**MeleeWeaponItem.java**:
- Base class for all melee weapons
- Integrates with combat system
- Prevents block breaking
- Applies custom attack damage and speed
- Abstract getWeaponType() method

**WeaponStats.java**:
- Builder pattern for weapon stat bonuses
- Supports all stat types (damage, speed, health, etc.)
- Creates immutable weapon configurations

**WeaponStatHandler.java**:
- Manages equipping/unequipping weapon stat bonuses
- Tracks equipped weapons per player
- Automatically applies/removes stat modifiers
- Syncs changes to client

**ModWeapons.java**:
- Registry for all weapon items
- Defines 9 weapon variants (3 tiers × 3 types)
- Iron, Diamond, and Netherite tiers
- Shortswords, Longswords, and Claymores
- Legacy RPG sword compatibility

**sword/** package:
- Specific implementations for each weapon type
- ShortswordItem: Fast, mobile, 3-hit combo
- LongswordItem: Balanced, 4-hit combo
- ClaymoreItem: Heavy, tanky, 4-hit combo with higher damage

### 8. Entity Rendering
**Location:** `entity/EntityHealthBar.java`

- Renders health bars above living entities
- Billboard effect to always face camera
- Shows entity name and HP text
- Color-coded based on health percentage
- Configurable render distance (32 blocks)

### 9. Client Events
**Location:** `client/event/ClientEvents.java`

- Handles client-side input events
- Mouse click detection for stats dropdown
- Prevents UI interactions from affecting gameplay

### 10. Server Events
**Location:** `event/ServerEvents.java`

Handles server-side logic every tick:
- Ticks all player cooldowns (for both abilities and combat)
- Ticks stat modifier durations
- Ticks combat system (attack cooldowns, combo tracking)
- Manages weapon stat bonuses when equipment changes
- Mana regeneration (every 20 ticks = 1 second)
  - Base: 1 mana/second
  - Modified by MANA_REGEN stat
- Applies MOVE_SPEED stat to player movement attribute
- Cleans up player data on logout

### 11. Main Mod Class
**Location:** `RpgClassesMod.java`

Entry point with MOD_ID = "rpgclasses_v2":
- Registers attachments via ModAttachments.register()
- Registers networking via ModMessages.register()
- Registers weapons via ModWeapons.register()
- Registers client overlays (only on Dist.CLIENT)
  - HealthBarOverlay
  - ManaBarOverlay
  - StatsDropdownOverlay
- Registers ServerEvents on NeoForge.EVENT_BUS
- Registers CombatEventHandler on NeoForge.EVENT_BUS

### 12. Gradle Setup
**Build Configuration:**
- NeoForge MDK plugin version 1.0.21
- Minecraft 1.21
- NeoForge 21.0.167
- Java 21 toolchain
- Parchment mappings for better names

**Properties:**
- mod_id: rpgclasses_v2
- mod_name: Frost's RPG Classes v2
- mod_version: 1.0.0
- minecraft_version: 1.21

## Design Decisions

1. **Percent-based stats**: All stat modifiers use percentage values for flexibility and scalability
2. **Multiple modifier sources**: Stats system supports multiple modifiers from different sources (equipment, abilities, buffs)
3. **Efficient ticking**: Cooldowns and stat durations auto-expire, removing themselves when done
4. **Client-server sync**: Packets ensure mana and stats stay synchronized
5. **Ornate UI**: Health and mana bars use decorative frames matching the v1 aesthetic
6. **No potion effects**: Stats are applied directly to attributes where possible, avoiding potion effect pollution

## Next Steps for Development

This infrastructure is ready for:
1. Class implementations (Bladedancer, Juggernaut, etc.)
2. Ability systems using the cooldown management
3. Items and equipment that modify stats
4. Class selection GUI
5. Particle effects and visual feedback

## Building

To build the mod (requires access to maven.neoforged.net):
```bash
./gradlew build
```

To run the client:
```bash
./gradlew runClient
```

To run the server:
```bash
./gradlew runServer
```

## Notes

- All source code follows NeoForge 1.21 conventions
- Attachments use the new NeoForge data attachment system
- Networking uses CustomPacketPayload records
- Client code is properly isolated using Dist.CLIENT checks
- All serialization uses Codecs for NBT and StreamCodecs for network
