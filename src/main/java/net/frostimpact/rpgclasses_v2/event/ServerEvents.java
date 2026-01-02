package net.frostimpact.rpgclasses_v2.event;

import net.frostimpact.rpgclasses_v2.networking.ModMessages;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncCooldowns;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncMana;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncRPGData;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncSeekerCharges;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncStats;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerEvents.class);
    private static final int MANA_REGEN_INTERVAL = 20; // Regen every second (20 ticks)
    private static final int SEEKER_CHARGE_INTERVAL = 40; // Gain seeker charge every 2 seconds while airborne
    private int tickCounter = 0;

    // Track last known stats for each player to avoid recalculating every tick
    private final Map<UUID, Double> lastMoveSpeedStats = new HashMap<>();
    private final Map<UUID, Double> lastAttackSpeedStats = new HashMap<>();
    private final Map<UUID, Integer> lastMaxHealthStats = new HashMap<>();
    private final Map<UUID, Integer> lastDefenseStats = new HashMap<>();
    private final Map<UUID, Integer> lastDamageStats = new HashMap<>();
    
    // Track last known player level for stat point awards
    private final Map<UUID, Integer> lastPlayerLevels = new HashMap<>();
    
    // Track airborne ticks for seeker charge restoration
    private final Map<UUID, Integer> airborneTickCounter = new HashMap<>();

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Pre event) {
        tickCounter++;
        
        // Tick timed ability effects (Rain of Arrows, Seeker projectiles)
        ModMessages.tickTimedEffects();
        
        // Get any server level for global updates
        if (!event.getServer().getAllLevels().iterator().hasNext()) {
            return;
        }
        net.minecraft.server.level.ServerLevel serverLevel = event.getServer().getAllLevels().iterator().next();
        
        // Update status effects for Ravager (BLEED, GRIEVOUS WOUNDS)
        ModMessages.updateStatusEffects(serverLevel);
        
        // Update Warrior Leaps
        ModMessages.updateWarriorLeaps(serverLevel);
        
        // Update Lancer Piercing Charge and Comet impacts
        ModMessages.updateLancerAbilities(serverLevel);
        
        // Update Ravager Heartstoppers
        ModMessages.updateRavagerHeartstoppers(serverLevel);
        
        // Update Tearing Hook pulls
        ModMessages.updateTearingHookPulls(serverLevel);
        
        // Update Berserker RAGE system
        ModMessages.updateBerserkerRage(serverLevel);

        event.getServer().getPlayerList().getPlayers().forEach(player -> {
            // Tick cooldowns
            var rpgData = player.getData(ModAttachments.PLAYER_RPG);
            rpgData.tickCooldowns();

            // Tick stat modifiers
            var stats = player.getData(ModAttachments.PLAYER_STATS);
            stats.tick();
            
            // HAWKEYE AERIAL AFFINITY & GLIDE PASSIVE: Restore seeker charges while mid-air + auto-apply Slow Falling
            if (rpgData.getCurrentClass() != null && rpgData.getCurrentClass().equalsIgnoreCase("hawkeye")) {
                if (!player.onGround() && !player.isInWater() && !player.isInLava()) {
                    // Player is airborne
                    int airTicks = airborneTickCounter.getOrDefault(player.getUUID(), 0) + 1;
                    airborneTickCounter.put(player.getUUID(), airTicks);
                    
                    // GLIDE PASSIVE: Auto-apply Slow Falling I while airborne
                    if (!player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING)) {
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.SLOW_FALLING, 40, 0, false, false));
                    }
                    
                    // Every SEEKER_CHARGE_INTERVAL ticks while airborne, add a seeker charge
                    if (airTicks % SEEKER_CHARGE_INTERVAL == 0) {
                        int oldCharges = rpgData.getSeekerCharges();
                        rpgData.addSeekerCharge();
                        int newCharges = rpgData.getSeekerCharges();
                        
                        if (newCharges > oldCharges) {
                            // Sync to client
                            ModMessages.sendToPlayer(new PacketSyncSeekerCharges(newCharges), player);
                            LOGGER.debug("Hawkeye {} gained aerial seeker charge, now has {}", 
                                    player.getName().getString(), newCharges);
                        }
                    }
                } else {
                    // Player is grounded - reset counter
                    airborneTickCounter.put(player.getUUID(), 0);
                }
            }
            
            // BEAST MASTER BEAST BOND PASSIVE: +5% damage per active beast companion (max 3 stacks = +15%)
            if (rpgData.getCurrentClass() != null && rpgData.getCurrentClass().equalsIgnoreCase("beastmaster")) {
                // Count active summoned beasts nearby (within 30 blocks)
                int activeBeastCount = countNearbyBeastCompanions(player);
                
                // Remove old beast bond modifier
                stats.removeModifier("beast_bond", net.frostimpact.rpgclasses_v2.rpg.stats.StatType.DAMAGE);
                
                // Apply new beast bond modifier if beasts are present
                if (activeBeastCount > 0) {
                    int maxStacks = 3;
                    int effectiveBeastCount = Math.min(activeBeastCount, maxStacks);
                    int damageBonus = effectiveBeastCount * 5; // 5% per beast, max 15%
                    
                    stats.addModifier(new net.frostimpact.rpgclasses_v2.rpg.stats.StatModifier(
                            "beast_bond",
                            net.frostimpact.rpgclasses_v2.rpg.stats.StatType.DAMAGE,
                            damageBonus,
                            -1 // Permanent until removed
                    ));
                }
                
                // EAGLE SPECIAL ABILITY: Periodically swoop and mark enemies
                handleEagleScoutAbility(player);
            }
            
            // MARKSMAN FOCUS MODE: Mana drain (3 mana/sec) and slow falling mid-air
            if (rpgData.getCurrentClass() != null && rpgData.getCurrentClass().equalsIgnoreCase("marksman")) {
                if (rpgData.isInFocusMode()) {
                    // Drain mana every 20 ticks (1 second) - 3 mana per second
                    if (tickCounter % 20 == 0) {
                        int currentMana = rpgData.getMana();
                        if (currentMana >= 3) {
                            rpgData.useMana(3);
                            ModMessages.sendToPlayer(
                                    new net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncMana(
                                            rpgData.getMana(), rpgData.getMaxMana()),
                                    player
                            );
                        } else {
                            // Out of mana - exit FOCUS mode
                            rpgData.setInFocusMode(false);
                            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                    "§cOut of mana! FOCUS mode disabled."), true);
                        }
                    }
                    
                    // Apply slow falling if mid-air (like FOCUS mid-air feature)
                    if (!player.onGround() && !player.isInWater() && !player.isInLava()) {
                        if (!player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING)) {
                            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.SLOW_FALLING, 40, 0, false, false));
                        }
                    }
                }
            }
            
            // LANCER MOMENTUM SYSTEM: Calculate momentum based on velocity and apply sprint speed boost
            if (rpgData.getCurrentClass() != null && rpgData.getCurrentClass().equalsIgnoreCase("lancer")) {
                // Calculate momentum from velocity (0-100 scale)
                net.minecraft.world.phys.Vec3 velocity = player.getDeltaMovement();
                double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                
                // Max momentum at ~0.3 blocks/tick (sprinting speed)
                float momentum = (float) Math.min(100.0, (horizontalSpeed / 0.3) * 100.0);
                rpgData.setMomentum(momentum);
                
                // Track sprint time for speed boost
                if (player.isSprinting()) {
                    if (rpgData.getSprintStartTime() == 0) {
                        rpgData.setSprintStartTime(player.level().getGameTime());
                    }
                    
                    long sprintDuration = player.level().getGameTime() - rpgData.getSprintStartTime();
                    // After 1.5s (30 ticks) of sprinting, apply gradual speed boost up to +50
                    if (sprintDuration >= 30) {
                        // Gradual increase over next 2s (40 ticks), maxing at +50
                        long boostTicks = sprintDuration - 30;
                        float speedBoost = Math.min(50.0f, (boostTicks / 40.0f) * 50.0f);
                        
                        // Remove old momentum speed modifier
                        stats.removeModifier("lancer_momentum_speed", net.frostimpact.rpgclasses_v2.rpg.stats.StatType.MOVE_SPEED);
                        
                        // Apply new momentum speed modifier
                        if (speedBoost > 0) {
                            stats.addModifier(new net.frostimpact.rpgclasses_v2.rpg.stats.StatModifier(
                                    "lancer_momentum_speed",
                                    net.frostimpact.rpgclasses_v2.rpg.stats.StatType.MOVE_SPEED,
                                    speedBoost,
                                    -1 // Permanent until removed
                            ));
                        }
                    }
                } else {
                    // Not sprinting - reset sprint timer and remove speed boost
                    rpgData.setSprintStartTime(0);
                    stats.removeModifier("lancer_momentum_speed", net.frostimpact.rpgclasses_v2.rpg.stats.StatType.MOVE_SPEED);
                }
                
                // Check if momentum is at max (100) and set empowered attack flag
                if (momentum >= 100.0f && !rpgData.isEmpoweredAttack()) {
                    rpgData.setEmpoweredAttack(true);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "§e§lEMPOWERED! §7Your next melee attack is empowered!"), true);
                }
            }

            // Mana regeneration and cooldown sync
            if (tickCounter % MANA_REGEN_INTERVAL == 0) {
                int manaRegenBonus = stats.getIntStatValue(StatType.MANA_REGEN);
                int baseRegen = 1;
                int regenAmount = baseRegen + manaRegenBonus;

                int oldMana = rpgData.getMana();
                rpgData.regenMana(regenAmount);

                // Sync if mana changed
                if (rpgData.getMana() != oldMana) {
                    ModMessages.sendToPlayer(
                            new PacketSyncMana(rpgData.getMana(), rpgData.getMaxMana()),
                            player
                    );
                }
                
                // Sync cooldowns periodically
                ModMessages.sendToPlayer(new PacketSyncCooldowns(rpgData.getAllCooldowns()), player);
            }

            // Apply movement speed modifier only if it changed
            double currentSpeedStat = stats.getPercentageStatValue(StatType.MOVE_SPEED);

            Double lastSpeedStat = lastMoveSpeedStats.get(player.getUUID());
            if (lastSpeedStat == null || !lastSpeedStat.equals(currentSpeedStat)) {
                applyMovementSpeed(player, currentSpeedStat);
                lastMoveSpeedStats.put(player.getUUID(), currentSpeedStat);
            }
            
            // Apply attack speed modifier only if it changed
            double currentAttackSpeedStat = stats.getPercentageStatValue(StatType.ATTACK_SPEED);
            Double lastAttackSpeedStat = lastAttackSpeedStats.get(player.getUUID());
            if (lastAttackSpeedStat == null || !lastAttackSpeedStat.equals(currentAttackSpeedStat)) {
                applyAttackSpeed(player, currentAttackSpeedStat);
                lastAttackSpeedStats.put(player.getUUID(), currentAttackSpeedStat);
            }
            
            // Apply max health modifier only if it changed
            int currentMaxHealthStat = stats.getIntStatValue(StatType.MAX_HEALTH);
            Integer lastMaxHealthStat = lastMaxHealthStats.get(player.getUUID());
            if (lastMaxHealthStat == null || !lastMaxHealthStat.equals(currentMaxHealthStat)) {
                applyMaxHealth(player, currentMaxHealthStat);
                lastMaxHealthStats.put(player.getUUID(), currentMaxHealthStat);
            }
            
            // Apply defense modifier only if it changed
            int currentDefenseStat = stats.getIntStatValue(StatType.DEFENSE);
            Integer lastDefenseStat = lastDefenseStats.get(player.getUUID());
            if (lastDefenseStat == null || !lastDefenseStat.equals(currentDefenseStat)) {
                applyDefense(player, currentDefenseStat);
                lastDefenseStats.put(player.getUUID(), currentDefenseStat);
            }
            
            // Apply damage modifier only if it changed
            int currentDamageStat = stats.getIntStatValue(StatType.DAMAGE);
            Integer lastDamageStat = lastDamageStats.get(player.getUUID());
            if (lastDamageStat == null || !lastDamageStat.equals(currentDamageStat)) {
                applyDamage(player, currentDamageStat);
                lastDamageStats.put(player.getUUID(), currentDamageStat);
            }
        });
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerUUID = player.getUUID();
            lastMoveSpeedStats.remove(playerUUID);
            lastAttackSpeedStats.remove(playerUUID);
            lastMaxHealthStats.remove(playerUUID);
            lastDefenseStats.remove(playerUUID);
            lastDamageStats.remove(playerUUID);
            lastPlayerLevels.remove(playerUUID);
            airborneTickCounter.remove(playerUUID);
            // Clean up any active Rain of Arrows effects for this player
            ModMessages.getActiveRainEffects().remove(playerUUID);
        }
    }
    
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Sync all RPG data to client on login
            var rpgData = player.getData(ModAttachments.PLAYER_RPG);
            var stats = player.getData(ModAttachments.PLAYER_STATS);
            
            ModMessages.sendToPlayer(new PacketSyncMana(rpgData.getMana(), rpgData.getMaxMana()), player);
            ModMessages.sendToPlayer(new PacketSyncStats(stats.getModifiers()), player);
            ModMessages.sendToPlayer(new PacketSyncRPGData(
                    rpgData.getCurrentClass(),
                    rpgData.getLevel(),
                    rpgData.getClassLevel(),
                    rpgData.getClassExperience(),
                    rpgData.getAvailableStatPoints(),
                    rpgData.getAvailableSkillPoints()
            ), player);
            // Sync seeker charges for Hawkeye class
            ModMessages.sendToPlayer(new PacketSyncSeekerCharges(rpgData.getSeekerCharges()), player);
            
            // Sync RAGE data for Berserker class
            if (rpgData.getCurrentClass() != null && rpgData.getCurrentClass().equalsIgnoreCase("berserker")) {
                ModMessages.sendToPlayer(new net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncRage(
                        rpgData.getRage(),
                        rpgData.isEnraged(),
                        rpgData.isEnhancedEnraged(),
                        rpgData.isExhausted(),
                        rpgData.getAxeThrowCharges()
                ), player);
            }
            
            // Initialize last level tracking
            lastPlayerLevels.put(player.getUUID(), player.experienceLevel);
            
            LOGGER.debug("Synced RPG data for player {} on login", player.getName().getString());
        }
    }
    
    @SubscribeEvent
    public void onPlayerLevelChange(PlayerXpEvent.LevelChange event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            int newLevel = player.experienceLevel + event.getLevels();
            int oldLevel = player.experienceLevel;
            
            // Only award points when gaining levels (not losing)
            if (newLevel > oldLevel) {
                var rpgData = player.getData(ModAttachments.PLAYER_RPG);
                
                // Award 1 stat point per level gained
                int levelsGained = newLevel - oldLevel;
                rpgData.addStatPoints(levelsGained);
                
                // Update the stored level
                rpgData.setLevel(newLevel);
                
                LOGGER.info("Player {} gained {} level(s), now has {} stat points", 
                        player.getName().getString(), levelsGained, rpgData.getAvailableStatPoints());
                
                // Sync to client
                ModMessages.sendToPlayer(new PacketSyncRPGData(
                        rpgData.getCurrentClass(),
                        rpgData.getLevel(),
                        rpgData.getClassLevel(),
                        rpgData.getClassExperience(),
                        rpgData.getAvailableStatPoints(),
                        rpgData.getAvailableSkillPoints()
                ), player);
            }
        }
    }

    private void applyMovementSpeed(ServerPlayer player, double speedModifier) {
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            double baseSpeed = 0.1;
            double newSpeed = baseSpeed * (1.0 + speedModifier / 100.0);

            LOGGER.debug("Applying MOVE_SPEED to player {}: modifier={}%, baseSpeed={}, newSpeed={}",
                    player.getName().getString(), speedModifier, baseSpeed, newSpeed);

            speedAttribute.setBaseValue(newSpeed);
        }
    }
    
    private void applyAttackSpeed(ServerPlayer player, double attackSpeedModifier) {
        AttributeInstance attackSpeedAttribute = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeedAttribute != null) {
            // Check if player is Ravager - they cannot gain attack speed bonuses
            var rpgData = player.getData(ModAttachments.PLAYER_RPG);
            if (rpgData.getCurrentClass() != null && rpgData.getCurrentClass().equalsIgnoreCase("ravager")) {
                // Ravagers don't get attack speed bonus - it's converted to BLEED duration
                double baseAttackSpeed = 4.0;
                attackSpeedAttribute.setBaseValue(baseAttackSpeed);
                return;
            }
            
            double baseAttackSpeed = 4.0; // Minecraft's base attack speed
            double newAttackSpeed = baseAttackSpeed * (1.0 + attackSpeedModifier / 100.0);

            LOGGER.debug("Applying ATTACK_SPEED to player {}: modifier={}%, baseSpeed={}, newSpeed={}",
                    player.getName().getString(), attackSpeedModifier, baseAttackSpeed, newAttackSpeed);

            attackSpeedAttribute.setBaseValue(newAttackSpeed);
        }
    }
    
    private void applyMaxHealth(ServerPlayer player, int healthBonus) {
        AttributeInstance maxHealthAttribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            double baseMaxHealth = 20.0; // Minecraft's base max health
            double newMaxHealth = baseMaxHealth + healthBonus;

            LOGGER.debug("Applying MAX_HEALTH to player {}: bonus={}, baseHealth={}, newHealth={}",
                    player.getName().getString(), healthBonus, baseMaxHealth, newMaxHealth);

            maxHealthAttribute.setBaseValue(newMaxHealth);
            
            // Heal the player if their health is now below the new max
            if (player.getHealth() > newMaxHealth) {
                player.setHealth((float) newMaxHealth);
            }
        }
    }
    
    private void applyDefense(ServerPlayer player, int defenseBonus) {
        AttributeInstance armorAttribute = player.getAttribute(Attributes.ARMOR);
        if (armorAttribute != null) {
            double baseArmor = 0.0; // Minecraft's base armor without equipment
            double newArmor = baseArmor + defenseBonus;

            LOGGER.debug("Applying DEFENSE to player {}: bonus={}, baseArmor={}, newArmor={}",
                    player.getName().getString(), defenseBonus, baseArmor, newArmor);

            armorAttribute.setBaseValue(newArmor);
        }
    }
    
    private void applyDamage(ServerPlayer player, int damageBonus) {
        AttributeInstance attackDamageAttribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamageAttribute != null) {
            double baseAttackDamage = 1.0; // Minecraft's base attack damage
            double newAttackDamage = baseAttackDamage + damageBonus;

            LOGGER.debug("Applying DAMAGE to player {}: bonus={}, baseDamage={}, newDamage={}",
                    player.getName().getString(), damageBonus, baseAttackDamage, newAttackDamage);

            attackDamageAttribute.setBaseValue(newAttackDamage);
        }
    }
    
    /**
     * Count nearby beast companions summoned by the player (for Beast Bond passive)
     */
    private int countNearbyBeastCompanions(ServerPlayer player) {
        double searchRadius = 30.0;
        net.minecraft.world.phys.AABB searchBox = player.getBoundingBox().inflate(searchRadius);
        
        java.util.List<net.minecraft.world.entity.Entity> entities = player.level().getEntities(player, searchBox,
                e -> e instanceof net.minecraft.world.entity.Mob);
        
        int count = 0;
        for (net.minecraft.world.entity.Entity entity : entities) {
            // Check if entity is marked as a summoned beast
            if (entity.getPersistentData().getBoolean("rpgclasses_summoned_beast")) {
                // Verify owner matches (if owner is stored)
                if (entity.getPersistentData().contains("rpgclasses_owner")) {
                    java.util.UUID ownerUUID = entity.getPersistentData().getUUID("rpgclasses_owner");
                    if (ownerUUID.equals(player.getUUID()) && entity.isAlive()) {
                        count++;
                    }
                } else if (entity instanceof net.minecraft.world.entity.TamableAnimal tameable) {
                    // For tameable animals (wolves), check if owned by player
                    if (tameable.getOwnerUUID() != null && tameable.getOwnerUUID().equals(player.getUUID()) && 
                            tameable.getPersistentData().getBoolean("rpgclasses_summoned_beast")) {
                        count++;
                    }
                }
            }
        }
        
        return count;
    }
    
    /**
     * Handle eagle scout special ability - swoops down and marks/damages enemies
     */
    private void handleEagleScoutAbility(ServerPlayer player) {
        // Find nearby eagle scouts summoned by this player
        double searchRadius = 40.0;
        net.minecraft.world.phys.AABB searchBox = player.getBoundingBox().inflate(searchRadius);
        
        java.util.List<net.minecraft.world.entity.Entity> entities = player.level().getEntities(player, searchBox,
                e -> e instanceof net.minecraft.world.entity.animal.allay.Allay);
        
        for (net.minecraft.world.entity.Entity entity : entities) {
            if (entity instanceof net.minecraft.world.entity.animal.allay.Allay eagle) {
                // Check if this is an eagle scout
                if (!eagle.getPersistentData().getBoolean("rpgclasses_eagle_scout")) {
                    continue;
                }
                
                // Verify ownership
                if (!eagle.getPersistentData().contains("rpgclasses_owner")) {
                    continue;
                }
                java.util.UUID ownerUUID = eagle.getPersistentData().getUUID("rpgclasses_owner");
                if (!ownerUUID.equals(player.getUUID())) {
                    continue;
                }
                
                // Check if it's time for a swoop (every 5 seconds)
                long currentTime = player.level().getGameTime();
                long lastSwoop = eagle.getPersistentData().getLong("rpgclasses_last_swoop");
                if (currentTime - lastSwoop < 100) { // 100 ticks = 5 seconds
                    continue;
                }
                
                // Find nearby enemies to mark
                net.minecraft.world.phys.AABB eagleBox = eagle.getBoundingBox().inflate(15.0);
                java.util.List<net.minecraft.world.entity.Entity> nearbyEnemies = player.level().getEntities(eagle, eagleBox,
                        e -> e instanceof net.minecraft.world.entity.monster.Monster && 
                             e instanceof net.minecraft.world.entity.LivingEntity);
                
                if (!nearbyEnemies.isEmpty()) {
                    // Pick closest enemy
                    net.minecraft.world.entity.Entity closestEnemy = null;
                    double closestDist = Double.MAX_VALUE;
                    for (net.minecraft.world.entity.Entity enemy : nearbyEnemies) {
                        double dist = eagle.position().distanceTo(enemy.position());
                        if (dist < closestDist) {
                            closestDist = dist;
                            closestEnemy = enemy;
                        }
                    }
                    
                    if (closestEnemy instanceof net.minecraft.world.entity.LivingEntity living) {
                        // Apply glowing effect (mark enemy)
                        living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.GLOWING, 100, 0)); // 5 seconds
                        
                        // Deal minor swoop damage
                        living.hurt(player.damageSources().mobAttack(eagle), 3.0f);
                        
                        // Spawn swoop visual effect
                        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            serverLevel.sendParticles(
                                    net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                                    living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(),
                                    3, 0.3, 0.3, 0.3, 0);
                            serverLevel.sendParticles(
                                    net.minecraft.core.particles.ParticleTypes.CLOUD,
                                    living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(),
                                    5, 0.2, 0.2, 0.2, 0.05);
                        }
                        
                        // Update last swoop time
                        eagle.getPersistentData().putLong("rpgclasses_last_swoop", currentTime);
                        
                        LOGGER.debug("Eagle scout swooped on enemy for player {}", player.getName().getString());
                    }
                }
            }
        }
    }
    
    /**
     * Suppress death messages for summoned beasts to prevent chat spam
     * This is handled by marking entities with Silent:1b NBT tag
     */
    @SubscribeEvent
    public void onLivingDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.Mob mob) {
            // Check if this is a summoned beast
            if (mob.getPersistentData().getBoolean("rpgclasses_summoned_beast")) {
                // Set the entity to silent to suppress death message
                // This is the proper way to prevent death messages in Minecraft
                mob.setSilent(true);
                
                LOGGER.debug("Suppressed death message for summoned beast: {}", 
                        mob.getType().getDescription().getString());
            }
        }
    }
    
    /**
     * Handle Ravager passive - Jagged Blade
     * Applies BLEED on normal attacks
     * Converts attack speed bonuses to BLEED duration
     * 
     * Also handles Berserker RAGE gain and lifesteal
     */
    @SubscribeEvent
    public void onPlayerAttack(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        // Check if damage source is a player
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            var rpgData = player.getData(ModAttachments.PLAYER_RPG);
            String currentClass = rpgData.getCurrentClass();
            
            // Ravager BLEED passive
            if (currentClass != null && currentClass.equalsIgnoreCase("ravager")) {
                // Apply BLEED to target
                if (event.getEntity() instanceof net.minecraft.world.entity.LivingEntity target) {
                    // Calculate BLEED duration based on attack speed
                    var stats = player.getData(ModAttachments.PLAYER_STATS);
                    int attackSpeedBonus = stats.getIntStatValue(StatType.ATTACK_SPEED);
                    
                    // Base BLEED duration is 3 seconds (60 ticks)
                    // Each point of attack speed adds 0.1 seconds (2 ticks)
                    int bleedDuration = 60 + (attackSpeedBonus * 2);
                    
                    // Apply BLEED effect
                    ModMessages.applyBleed(target, player, bleedDuration);
                    
                    LOGGER.debug("Ravager {} applied BLEED for {} ticks (attack speed bonus: {})", 
                            player.getName().getString(), bleedDuration, attackSpeedBonus);
                }
            }
            
            // Berserker RAGE gain and lifesteal
            if (currentClass != null && currentClass.equalsIgnoreCase("berserker")) {
                float damageDealt = event.getNewDamage();
                
                // Add RAGE from damage dealt (5% of damage)
                ModMessages.addRageFromDamageDealt(player, damageDealt);
                
                // Apply lifesteal if enraged (5% of damage)
                ModMessages.applyBerserkerLifesteal(player, damageDealt);
            }
        }
    }
    
    /**
     * Handle Berserker RAGE gain from taking damage
     * Also handles Unbound Carnage immortality
     * Also prevents fall damage for Warrior Leap
     */
    @SubscribeEvent
    public void onPlayerTakeDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Prevent fall damage after Warrior Leap
            if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
                if (player.getPersistentData().getBoolean("warrior_leap_no_fall_damage")) {
                    event.setCanceled(true);
                    player.getPersistentData().remove("warrior_leap_no_fall_damage");
                    return;
                }
            }
            
            var rpgData = player.getData(ModAttachments.PLAYER_RPG);
            String currentClass = rpgData.getCurrentClass();
            
            if (currentClass != null && currentClass.equalsIgnoreCase("berserker")) {
                // Add RAGE from taking damage (10 per hit, 3s cooldown)
                if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    ModMessages.addRageFromDamageTaken(player, serverLevel);
                }
                
                // Handle Unbound Carnage immortality
                if (rpgData.isEnhancedEnraged()) {
                    // Check if damage would kill the player
                    float incomingDamage = event.getNewDamage();
                    if (player.getHealth() - incomingDamage <= 0) {
                        // Set damage to bring player to 1 HP instead of killing
                        float newDamage = player.getHealth() - 1.0f;
                        if (newDamage < 0) newDamage = 0;
                        event.setNewDamage(newDamage);
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§6§lUNBOUND CARNAGE §cblocks lethal damage!"), true);
                    }
                }
            }
        }
    }
}