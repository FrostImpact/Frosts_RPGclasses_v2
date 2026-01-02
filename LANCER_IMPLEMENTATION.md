# Lancer Specialization Implementation

## Overview
The Lancer is a new warrior specialization that replaces the Paladin class. It features a momentum-based combat system where the player gains power through movement and velocity.

## Core Mechanics

### Momentum System
- **Range**: 0-100, calculated from player velocity
- **Display**: Bar above health/mana bars (pale yellow theme)
- **Calculation**: Based on horizontal velocity, max at sprinting speed (~0.3 blocks/tick)
- **Visual**: Gradient from pale yellow to bright yellow, pulsing white when empowered

### Sprint Speed Boost
- **Activation**: After 1.5 seconds of continuous sprinting
- **Effect**: Gradually increases movement speed from 0 to +50 (additive)
- **Duration**: Builds over 2 seconds (40 ticks) after initial 1.5s delay
- **Reset**: Stops sprinting resets boost immediately

### Empowered Attack
- **Trigger**: When momentum reaches 100%
- **Effect**: Next melee attack deals +50% bonus damage
- **Visual**: Yellow crit particles and damage notification
- **Reset**: Consumed on use

## Abilities

### Ability 1: Piercing Charge (Z)
- **Cooldown**: 15 seconds
- **Mana Cost**: 30 MP
- **Requirements**: Momentum > 50%
- **Mechanics**:
  - Forces player into sprint mode
  - Spawn spear-like yellow aura in front
  - Deal momentum-based damage to enemies sprinted through
  - If damage < 20% of target HP: Stop and deal full momentum damage
  - Wall collision or 8s timeout: End charge and apply cooldown
  - Can be cancelled by reactivating ability
- **Cooldown Applied**: Only when charge ends (not when started)

### Ability 2: Leap (X)
- **Cooldown**: 5 seconds
- **Mana Cost**: 15 MP
- **Effect**: Launch forward with high velocity
  - Forward velocity: 1.5 blocks/tick
  - Upward velocity: 0.4 blocks/tick
- **Visual**: Yellow particle burst

### Ability 3: Lunge (C)
- **Cooldown**: 6 seconds
- **Mana Cost**: 15 MP
- **Mechanics**:
  - Horizontal dash (no vertical velocity): 1.2 blocks/tick
  - Damage calculation: min(20, momentum/10 + damage_stat)
  - If no enemy hit: Reset Leap cooldown
- **Visual**: Yellow particle line + sweep attack particles

### Ability 4: Comet (V)
- **Cooldown**: 25 seconds
- **Mana Cost**: 50 MP
- **Mechanics**:
  - Convert all velocity to strong downward force
  - On ground impact: Deal shockwave damage in 6 block radius
  - Damage calculation: (momentum/5) + (damage_stat * 2)
  - Remove all momentum on impact
- **Visual**: Yellow shockwave rings + explosion

## Base Stats
- **Move Speed**: +10 (percentage-based)
- **Damage**: +6 (additive)
- **Attack Speed**: +10 (percentage-based)

## Visual Theme
All effects use pale yellow colors (RGB: 1.0, 1.0, 0.2-0.4) to represent the "crit" theme mentioned in requirements.

## Implementation Details

### Files Modified
1. **ClassRegistry.java**: Replaced Paladin with Lancer class definition
2. **PlayerRPGData.java**: Added momentum fields and Piercing Charge state tracking
3. **MomentumBarOverlay.java**: New overlay for momentum visualization
4. **RpgClassesMod.java**: Registered momentum overlay
5. **ServerEvents.java**: Momentum calculation, speed boost, and empowered attack
6. **ModMessages.java**: All four ability implementations + update logic
7. **AbilityUtils.java**: Lancer ability metadata (names, costs, cooldowns)

### Key Systems
- Momentum is calculated every tick based on player velocity
- Sprint speed boost applied via temporary stat modifier
- Piercing Charge uses persistent data for state tracking
- Comet impact detected when player lands (onGround check)
- All abilities follow existing patterns from Berserker/Ravager

## Testing Recommendations
1. Test momentum calculation at various speeds (walking, sprinting, falling)
2. Verify momentum bar updates smoothly
3. Test sprint speed boost timing (1.5s + 2s ramp)
4. Test empowered attack trigger at max momentum
5. Test Piercing Charge:
   - Low momentum rejection
   - Enemy collision damage
   - Large enemy stop condition
   - Wall collision detection
   - Timeout after 8 seconds
   - Cancel via reactivation
6. Test Lunge cooldown reset when no enemy hit
7. Test Comet ground impact detection
8. Verify all particle effects are pale yellow
9. Test ability interactions (e.g., Leap → Lunge → miss → Leap again)

## Known Limitations
- Turning speed reduction during Piercing Charge is partially implemented (needs rotation constraint)
- Crit chance stat not implemented (requirement mentions "crit" but only as theme)
- Icon texture needs to be added at `rpgclasses_v2:textures/gui/icons/lancer.png`

## Color Scheme
- Primary: Pale Yellow (0xFFFF99)
- Highlight: Bright Yellow (0xFFFF00)
- Particles: RGB(1.0, 1.0, 0.2-0.4)
- Text: §e (yellow) and §6 (gold)
