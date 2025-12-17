# Particle Effects System

This folder contains utilities and examples for creating particle effects using vector math.

## Files

### VectorMath.java
Core utility class with mathematical functions for creating particle patterns:
- **Rotation functions**: `rotateAroundX()`, `rotateAroundY()`, `rotateAroundZ()`
- **Pattern generators**: 
  - `createCircle()` - Generate points in a circular pattern
  - `createSpiral()` - Generate points in a spiral pattern
  - `createSphere()` - Generate points on a sphere surface
  - `createWave()` - Generate points along a wave
- **Interpolation**: 
  - `lerp()` - Linear interpolation
  - `bezier()` - Cubic bezier curves

### ParticleAnimator.java
Helper class for spawning particles using the patterns from VectorMath:
- `spawnCircle()` - Spawn particles in a circle
- `spawnSpiral()` - Spawn particles in a spiral
- `spawnSphere()` - Spawn particles in a sphere (static or exploding)
- `spawnWave()` - Spawn particles in a wave pattern
- `spawnBezierCurve()` - Spawn particles along a bezier curve

### ExampleParticleEffects.java
Ready-to-use example effects demonstrating common patterns:
- `magicCircle()` - Magic circle on the ground
- `risingSpiral()` - Spiral rising upward
- `explosionSphere()` - Explosion effect
- `energyWave()` - Wave of energy
- `teleportEffect()` - Teleportation effect
- `healingAura()` - Healing aura around player
- `slashArc()` - Melee attack slash arc
- `protectiveBarrier()` - Protective barrier shield

## Usage Example

```java
// In your event handler or ability code:
if (level instanceof ServerLevel serverLevel) {
    Vec3 playerPos = player.position();
    
    // Spawn a magic circle
    ExampleParticleEffects.magicCircle(serverLevel, playerPos, 2.0);
    
    // Or create a custom effect
    Vec3[] points = VectorMath.createCircle(playerPos, 1.5, 20, 0);
    for (Vec3 point : points) {
        serverLevel.sendParticles(ParticleTypes.FLAME, 
            point.x, point.y, point.z, 0, 0, 0.1, 0, 1.0);
    }
}
```

## Tips for Tinkering

1. **Start with examples**: Modify the parameters in ExampleParticleEffects to see how they change
2. **Combine patterns**: Use multiple particle types and patterns together
3. **Animate over time**: Use `level.getGameTime()` to create animated effects
4. **Experiment with particle types**: Try different `ParticleTypes` (FLAME, ENCHANT, PORTAL, etc.)
5. **Adjust parameters**: Play with radius, particle count, speed, and rotation values

## Available Particle Types (Partial List)
- `ParticleTypes.FLAME` - Fire particles
- `ParticleTypes.SMOKE` - Smoke
- `ParticleTypes.ENCHANT` - Enchanting table particles
- `ParticleTypes.PORTAL` - Purple portal particles
- `ParticleTypes.ELECTRIC_SPARK` - Lightning sparks
- `ParticleTypes.SOUL_FIRE_FLAME` - Blue soul fire
- `ParticleTypes.END_ROD` - White end rod particles
- `ParticleTypes.GLOW` - Glowing particles
- `ParticleTypes.HAPPY_VILLAGER` - Green happy particles

## Creating New Effects

To create a new effect:
1. Use VectorMath to generate point positions
2. Loop through the points and spawn particles
3. Add velocity to make particles move
4. Combine multiple patterns for complex effects
