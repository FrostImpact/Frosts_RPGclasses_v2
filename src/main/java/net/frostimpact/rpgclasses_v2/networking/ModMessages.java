package net.frostimpact.rpgclasses_v2.networking;

import net.frostimpact.rpgclasses_v2.networking.packet.PacketAllocateSkillPoint;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketAllocateStatPoint;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSelectClass;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncCooldowns;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncMana;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncRPGData;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncStats;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketUseAbility;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketResetStats;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatModifier;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import net.frostimpact.rpgclasses_v2.rpgclass.AbilityUtils;
import net.frostimpact.rpgclasses_v2.rpgclass.ClassRegistry;
import net.frostimpact.rpgclasses_v2.rpgclass.RPGClass;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

public class ModMessages {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModMessages.class);
    private static final Random RANDOM = new Random();
    
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModMessages::onRegisterPayloadHandler);
    }

    private static void onRegisterPayloadHandler(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("rpgclasses_v2");

        registrar.playToClient(
                PacketSyncMana.TYPE,
                PacketSyncMana.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() != null) {
                            var rpgData = context.player().getData(ModAttachments.PLAYER_RPG);
                            rpgData.setMana(packet.mana());
                            rpgData.setMaxMana(packet.maxMana());
                        }
                    });
                }
        );

        registrar.playToClient(
                PacketSyncStats.TYPE,
                PacketSyncStats.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {

                        if (context.player() != null) {
                            var stats = context.player().getData(ModAttachments.PLAYER_STATS);
                            stats.setModifiers(packet.modifiers());
                        }
                    });
                }
        );
        
        registrar.playToClient(
                PacketSyncCooldowns.TYPE,
                PacketSyncCooldowns.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() != null) {
                            var rpgData = context.player().getData(ModAttachments.PLAYER_RPG);
                            rpgData.setAllCooldowns(packet.cooldowns());
                        }
                    });
                }
        );

        registrar.playToServer(
                PacketAllocateStatPoint.TYPE,
                PacketAllocateStatPoint.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer serverPlayer) {
                            var rpgData = serverPlayer.getData(ModAttachments.PLAYER_RPG);
                            var stats = serverPlayer.getData(ModAttachments.PLAYER_STATS);

                            if (rpgData.useStatPoint()) {
                                // Find existing allocated stat modifier for this stat type
                                int currentValue = 0;
                                for (var modifier : stats.getModifiers()) {
                                    if (modifier.getStatType() == packet.statType() &&
                                            modifier.getSource().equals("allocated")) {
                                        currentValue = (int) modifier.getValue();
                                        // Remove the old modifier
                                        stats.removeModifier("allocated", packet.statType());
                                        break;
                                    }
                                }

                                // Add the updated modifier with +1 to the stat
                                stats.addModifier(new StatModifier(
                                        "allocated",
                                        packet.statType(),
                                        currentValue + 1,
                                        -1
                                ));

                                // Sync back to client
                                sendToPlayer(new PacketSyncStats(stats.getModifiers()), serverPlayer);
                                sendToPlayer(new PacketSyncRPGData(
                                        rpgData.getCurrentClass(),
                                        rpgData.getLevel(),
                                        rpgData.getClassLevel(),
                                        rpgData.getClassExperience(),
                                        rpgData.getAvailableStatPoints(),
                                        rpgData.getAvailableSkillPoints()
                                ), serverPlayer);
                            }
                        }
                    });
                }
        );
        
        // Register PacketSelectClass - handles class selection from client
        registrar.playToServer(
                PacketSelectClass.TYPE,
                PacketSelectClass.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer serverPlayer) {
                            String classId = packet.classId();
                            
                            // Validate the class exists
                            var optionalClass = ClassRegistry.getClass(classId);
                            if (optionalClass.isEmpty()) {
                                LOGGER.warn("Player {} tried to select invalid class: {}", 
                                        serverPlayer.getName().getString(), classId);
                                return;
                            }
                            
                            RPGClass rpgClass = optionalClass.get();
                            var rpgData = serverPlayer.getData(ModAttachments.PLAYER_RPG);
                            var stats = serverPlayer.getData(ModAttachments.PLAYER_STATS);
                            
                            String oldClass = rpgData.getCurrentClass();
                            
                            // Remove old class stats
                            stats.removeAllFromSource("class_" + oldClass.toLowerCase());
                            
                            // Set new class
                            rpgData.setCurrentClass(classId);
                            
                            // Apply new class base stats
                            for (var entry : rpgClass.getAllBaseStats().entrySet()) {
                                for (var modifier : entry.getValue()) {
                                    stats.addModifier(new StatModifier(
                                            "class_" + classId.toLowerCase(),
                                            entry.getKey(),
                                            modifier.getValue(),
                                            -1 // Permanent
                                    ));
                                }
                            }
                            
                            LOGGER.info("Player {} selected class: {} (was: {})", 
                                    serverPlayer.getName().getString(), classId, oldClass);
                            
                            // Sync all data to client
                            sendToPlayer(new PacketSyncStats(stats.getModifiers()), serverPlayer);
                            sendToPlayer(new PacketSyncRPGData(
                                    rpgData.getCurrentClass(),
                                    rpgData.getLevel(),
                                    rpgData.getClassLevel(),
                                    rpgData.getClassExperience(),
                                    rpgData.getAvailableStatPoints(),
                                    rpgData.getAvailableSkillPoints()
                            ), serverPlayer);
                        }
                    });
                }
        );
        
        // Register PacketSyncRPGData - syncs RPG data from server to client
        registrar.playToClient(
                PacketSyncRPGData.TYPE,
                PacketSyncRPGData.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() != null) {
                            var rpgData = context.player().getData(ModAttachments.PLAYER_RPG);
                            rpgData.setCurrentClass(packet.currentClass());
                            rpgData.setLevel(packet.level());
                            rpgData.setClassLevel(packet.classLevel());
                            rpgData.setClassExperience(packet.classExperience());
                            rpgData.setAvailableStatPoints(packet.availableStatPoints());
                            rpgData.setAvailableSkillPoints(packet.availableSkillPoints());
                        }
                    });
                }
        );
        
        // Register PacketAllocateSkillPoint - handles skill point allocation from client
        registrar.playToServer(
                PacketAllocateSkillPoint.TYPE,
                PacketAllocateSkillPoint.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer serverPlayer) {
                            var rpgData = serverPlayer.getData(ModAttachments.PLAYER_RPG);
                            
                            if (rpgData.useSkillPoint()) {
                                // TODO: Apply skill point to skill tree node
                                LOGGER.info("Player {} allocated skill point to {} in tree {}", 
                                        serverPlayer.getName().getString(), 
                                        packet.skillNodeId(), 
                                        packet.skillTreeId());
                                
                                // Sync back to client
                                sendToPlayer(new PacketSyncRPGData(
                                        rpgData.getCurrentClass(),
                                        rpgData.getLevel(),
                                        rpgData.getClassLevel(),
                                        rpgData.getClassExperience(),
                                        rpgData.getAvailableStatPoints(),
                                        rpgData.getAvailableSkillPoints()
                                ), serverPlayer);
                            }
                        }
                    });
                }
        );
        
        // Register PacketUseAbility - handles ability usage from client
        registrar.playToServer(
                PacketUseAbility.TYPE,
                PacketUseAbility.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer serverPlayer) {
                            executeAbility(serverPlayer, packet.abilitySlot());
                        }
                    });
                }
        );
        
        // Register PacketResetStats - handles stat reset from client
        registrar.playToServer(
                PacketResetStats.TYPE,
                PacketResetStats.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer serverPlayer) {
                            var rpgData = serverPlayer.getData(ModAttachments.PLAYER_RPG);
                            var stats = serverPlayer.getData(ModAttachments.PLAYER_STATS);
                            
                            // Count allocated stat points
                            int refundedPoints = 0;
                            for (var modifier : stats.getModifiers()) {
                                if (modifier.getSource().equals("allocated")) {
                                    refundedPoints += (int) modifier.getValue();
                                }
                            }
                            
                            // Remove all allocated modifiers
                            stats.removeAllFromSource("allocated");
                            
                            // Add back the points
                            rpgData.addStatPoints(refundedPoints);
                            
                            LOGGER.info("Player {} reset stats, refunded {} points", 
                                    serverPlayer.getName().getString(), refundedPoints);
                            
                            // Sync to client
                            sendToPlayer(new PacketSyncStats(stats.getModifiers()), serverPlayer);
                            sendToPlayer(new PacketSyncRPGData(
                                    rpgData.getCurrentClass(),
                                    rpgData.getLevel(),
                                    rpgData.getClassLevel(),
                                    rpgData.getClassExperience(),
                                    rpgData.getAvailableStatPoints(),
                                    rpgData.getAvailableSkillPoints()
                            ), serverPlayer);
                        }
                    });
                }
        );
    }

    public static void sendToPlayer(PacketSyncMana packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToPlayer(PacketSyncStats packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }
    
    public static void sendToPlayer(PacketSyncRPGData packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }
    
    public static void sendToPlayer(PacketSyncCooldowns packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToServer(PacketAllocateStatPoint packet) {
        PacketDistributor.sendToServer(packet);
    }
    
    public static void sendToServer(PacketSelectClass packet) {
        PacketDistributor.sendToServer(packet);
    }
    
    public static void sendToServer(PacketAllocateSkillPoint packet) {
        PacketDistributor.sendToServer(packet);
    }
    
    public static void sendToServer(PacketUseAbility packet) {
        PacketDistributor.sendToServer(packet);
    }
    
    public static void sendToServer(PacketResetStats packet) {
        PacketDistributor.sendToServer(packet);
    }
    
    /**
     * Execute an ability on the server side
     */
    private static void executeAbility(ServerPlayer player, int abilitySlot) {
        var rpgData = player.getData(ModAttachments.PLAYER_RPG);
        var stats = player.getData(ModAttachments.PLAYER_STATS);
        String currentClass = rpgData.getCurrentClass();
        
        if (currentClass == null || currentClass.equals("NONE")) {
            player.displayClientMessage(Component.literal("§cYou need to select a class first!"), true);
            return;
        }
        
        String abilityId = currentClass.toLowerCase() + "_ability_" + abilitySlot;
        int manaCost = AbilityUtils.getAbilityManaCost(currentClass, abilitySlot);
        int baseCooldownTicks = AbilityUtils.getAbilityCooldownTicks(currentClass, abilitySlot);
        String abilityName = AbilityUtils.getAbilityName(currentClass, abilitySlot);
        
        // Check cooldown
        int cooldown = rpgData.getAbilityCooldown(abilityId);
        if (cooldown > 0) {
            player.displayClientMessage(
                    Component.literal("§e" + abilityName + " §7is on cooldown (§c" + (cooldown / 20) + "s§7)"), true);
            return;
        }
        
        // Check mana
        if (rpgData.getMana() < manaCost) {
            player.displayClientMessage(
                    Component.literal("§9Not enough mana for §b" + abilityName + " §7(Need §3" + manaCost + "§7)"), true);
            return;
        }
        
        // Apply cooldown reduction
        int cooldownReduction = stats.getIntStatValue(StatType.COOLDOWN_REDUCTION);
        int adjustedCooldownTicks = (int) (baseCooldownTicks * (1.0 - cooldownReduction / 100.0));
        adjustedCooldownTicks = Math.max(adjustedCooldownTicks, 20); // Minimum 1 second cooldown
        
        // Use mana and set cooldown
        rpgData.useMana(manaCost);
        rpgData.setAbilityCooldown(abilityId, adjustedCooldownTicks);
        
        // Execute the ability effect
        performAbilityEffect(player, currentClass, abilitySlot, stats);
        
        // Sync mana and cooldowns to client
        sendToPlayer(new PacketSyncMana(rpgData.getMana(), rpgData.getMaxMana()), player);
        sendToPlayer(new PacketSyncCooldowns(rpgData.getAllCooldowns()), player);
        
        LOGGER.info("Player {} used ability {} (slot {})", player.getName().getString(), abilityName, abilitySlot);
        player.displayClientMessage(Component.literal("§a" + abilityName + " §7activated!"), true);
    }
    
    private static void performAbilityEffect(ServerPlayer player, String classId, int slot, 
            net.frostimpact.rpgclasses_v2.rpg.stats.PlayerStats stats) {
        int damageBonus = stats.getIntStatValue(StatType.DAMAGE);
        ServerLevel level = player.serverLevel();
        Vec3 playerPos = player.position();
        
        switch (classId.toLowerCase()) {
            case "warrior" -> {
                switch (slot) {
                    case 1 -> { // Power Strike - melee damage + slowness
                        dealDamageToNearbyEnemies(player, 6.0 + damageBonus * 1.5, 3.0);
                        applyEffectToNearbyEnemies(player, MobEffects.MOVEMENT_SLOWDOWN, 40, 2, 3.0);
                        // Dust particles - forward arc swing effect
                        spawnDustParticlesArc(level, playerPos, player.getYRot(), 3.0, 0.4f, 0.4f, 0.4f);
                    }
                    case 2 -> { // Battle Cry - damage and speed buff
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
                        // Burst of particles around player
                        spawnDustParticlesRing(level, playerPos, 2.0, 0.6f, 0.5f, 0.3f);
                    }
                    case 3 -> { // Whirlwind - AoE damage
                        dealDamageToNearbyEnemies(player, 4.0 + damageBonus, 4.0);
                        // Spinning ring of dust particles
                        spawnDustParticlesSpiral(level, playerPos, 4.0, 0.5f, 0.5f, 0.5f);
                    }
                    case 4 -> { // Berserker Rage - massive damage buff but take more damage
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1));
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1));
                        // Intense burst around player
                        spawnDustParticlesBurst(level, playerPos, 3.0, 0.7f, 0.3f, 0.3f, 30);
                    }
                }
            }
            case "mage" -> {
                switch (slot) {
                    case 1 -> { // Fireball - ranged fire damage
                        dealDamageToNearbyEnemies(player, 5.0 + damageBonus, 5.0);
                        setNearbyEnemiesOnFire(player, 5.0, 100);
                        // Orange/gray dust particles expanding outward
                        spawnDustParticlesBurst(level, playerPos.add(0, 1, 0), 5.0, 0.7f, 0.4f, 0.2f, 25);
                    }
                    case 2 -> { // Frost Nova - freeze nearby enemies
                        applyEffectToNearbyEnemies(player, MobEffects.MOVEMENT_SLOWDOWN, 100, 3, 4.0);
                        dealDamageToNearbyEnemies(player, 3.0 + damageBonus * 0.5, 4.0);
                        // Light gray/white dust particles ring
                        spawnDustParticlesRing(level, playerPos, 4.0, 0.8f, 0.85f, 0.9f);
                    }
                    case 3 -> { // Arcane Shield - absorption
                        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 2));
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
                        // Protective shell of particles
                        spawnDustParticlesShell(level, playerPos, 1.5, 0.5f, 0.5f, 0.7f);
                    }
                    case 4 -> { // Meteor Storm - massive AoE
                        dealDamageToNearbyEnemies(player, 10.0 + damageBonus * 2.0, 8.0);
                        setNearbyEnemiesOnFire(player, 8.0, 200);
                        // Large burst of particles
                        spawnDustParticlesBurst(level, playerPos, 8.0, 0.6f, 0.35f, 0.2f, 50);
                    }
                }
            }
            case "rogue" -> {
                switch (slot) {
                    case 1 -> { // Backstab - teleport behind + damage
                        dealDamageToNearbyEnemies(player, 8.0 + damageBonus * 2.0, 2.0);
                        // Quick directional particles
                        spawnDustParticlesLine(level, playerPos, player.getYRot(), 2.0, 0.3f, 0.3f, 0.35f);
                    }
                    case 2 -> { // Smoke Bomb - invisibility
                        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0));
                        applyEffectToNearbyEnemies(player, MobEffects.BLINDNESS, 60, 0, 4.0);
                        // Dark smoke particles
                        spawnDustParticlesBurst(level, playerPos, 4.0, 0.25f, 0.25f, 0.3f, 40);
                    }
                    case 3 -> { // Fan of Knives - damage with bleed
                        dealDamageToNearbyEnemies(player, 3.0 + damageBonus, 5.0);
                        applyEffectToNearbyEnemies(player, MobEffects.WITHER, 80, 0, 5.0);
                        // Outward spread of particles
                        spawnDustParticlesSpiral(level, playerPos, 5.0, 0.4f, 0.4f, 0.45f);
                    }
                    case 4 -> { // Shadow Dance - invisibility + crit
                        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 160, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 2));
                        // Dark swirling particles
                        spawnDustParticlesSpiral(level, playerPos, 2.0, 0.2f, 0.2f, 0.25f);
                    }
                }
            }
            case "ranger" -> {
                switch (slot) {
                    case 1 -> { // Precise Shot - high damage single target
                        dealDamageToNearbyEnemies(player, 7.0 + damageBonus * 1.75, 10.0);
                        // Long line of particles in look direction
                        spawnDustParticlesLine(level, playerPos.add(0, 1.5, 0), player.getYRot(), 10.0, 0.5f, 0.55f, 0.4f);
                    }
                    case 2 -> { // Multi-Shot - multiple projectiles
                        dealDamageToNearbyEnemies(player, 2.5 + damageBonus * 0.6, 8.0);
                        // Fan of particles
                        spawnDustParticlesFan(level, playerPos.add(0, 1.5, 0), player.getYRot(), 8.0, 0.5f, 0.5f, 0.4f);
                    }
                    case 3 -> { // Trap - root enemies
                        applyEffectToNearbyEnemies(player, MobEffects.MOVEMENT_SLOWDOWN, 100, 4, 3.0);
                        applyEffectToNearbyEnemies(player, MobEffects.POISON, 100, 1, 3.0);
                        // Ground particles
                        spawnDustParticlesGround(level, playerPos, 3.0, 0.4f, 0.5f, 0.35f);
                    }
                    case 4 -> { // Rain of Arrows - sustained AoE
                        dealDamageToNearbyEnemies(player, 6.0 + damageBonus * 1.5, 8.0);
                        applyEffectToNearbyEnemies(player, MobEffects.MOVEMENT_SLOWDOWN, 100, 1, 8.0);
                        // Falling particles from above
                        spawnDustParticlesRain(level, playerPos, 8.0, 0.55f, 0.55f, 0.45f);
                    }
                }
            }
            case "tank" -> {
                switch (slot) {
                    case 1 -> { // Shield Bash - damage + stun
                        dealDamageToNearbyEnemies(player, 4.0 + damageBonus, 2.0);
                        applyEffectToNearbyEnemies(player, MobEffects.MOVEMENT_SLOWDOWN, 30, 5, 2.0);
                        // Impact particles forward
                        spawnDustParticlesArc(level, playerPos, player.getYRot(), 2.0, 0.55f, 0.55f, 0.5f);
                    }
                    case 2 -> { // Taunt - force aggro + defense
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
                        applyEffectToNearbyEnemies(player, MobEffects.WEAKNESS, 100, 0, 6.0);
                        // Expanding ring
                        spawnDustParticlesRing(level, playerPos, 6.0, 0.6f, 0.5f, 0.4f);
                    }
                    case 3 -> { // Iron Skin - damage reduction
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 2));
                        // Protective shell particles
                        spawnDustParticlesShell(level, playerPos, 1.2, 0.6f, 0.6f, 0.55f);
                    }
                    case 4 -> { // Fortress - major damage reduction
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 3));
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2));
                        // Strong defensive particle shell
                        spawnDustParticlesShell(level, playerPos, 1.5, 0.7f, 0.65f, 0.55f);
                    }
                }
            }
            case "priest" -> {
                switch (slot) {
                    case 1 -> { // Holy Light - heal
                        player.heal(8.0f);
                        healNearbyAllies(player, 6.0f, 5.0);
                        // Light particles upward
                        spawnDustParticlesUpward(level, playerPos, 5.0, 0.85f, 0.85f, 0.7f);
                    }
                    case 2 -> { // Blessing - health buff
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
                        player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 600, 1));
                        // Gentle ring around player
                        spawnDustParticlesRing(level, playerPos, 2.0, 0.8f, 0.75f, 0.6f);
                    }
                    case 3 -> { // Smite - holy damage
                        dealDamageToNearbyEnemies(player, 6.0 + damageBonus, 6.0);
                        applyEffectToNearbyEnemies(player, MobEffects.WEAKNESS, 100, 1, 6.0);
                        // Bright burst
                        spawnDustParticlesBurst(level, playerPos, 6.0, 0.9f, 0.85f, 0.6f, 30);
                    }
                    case 4 -> { // Divine Intervention - massive heal + immunity
                        player.heal(20.0f);
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2));
                        healNearbyAllies(player, 20.0f, 8.0);
                        // Large healing burst
                        spawnDustParticlesBurst(level, playerPos, 8.0, 0.95f, 0.9f, 0.7f, 50);
                        spawnDustParticlesUpward(level, playerPos, 8.0, 0.9f, 0.85f, 0.65f);
                    }
                }
            }
            default -> {
                // Generic ability for unknown classes
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 100, 0));
                spawnDustParticlesBurst(level, playerPos, 2.0, 0.6f, 0.6f, 0.6f, 15);
            }
        }
    }
    
    // ===== Particle Helper Methods =====
    
    /**
     * Creates dust particle options with given RGB values (0-1 range)
     */
    private static DustParticleOptions createDustParticle(float r, float g, float b, float size) {
        // Add slight variation to make particles more interesting
        float rVar = r + (RANDOM.nextFloat() - 0.5f) * 0.15f;
        float gVar = g + (RANDOM.nextFloat() - 0.5f) * 0.15f;
        float bVar = b + (RANDOM.nextFloat() - 0.5f) * 0.15f;
        return new DustParticleOptions(new Vector3f(
            Math.max(0, Math.min(1, rVar)),
            Math.max(0, Math.min(1, gVar)),
            Math.max(0, Math.min(1, bVar))
        ), size);
    }
    
    /**
     * Spawn dust particles in a burst pattern around a position
     */
    private static void spawnDustParticlesBurst(ServerLevel level, Vec3 center, double radius, 
            float r, float g, float b, int count) {
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = RANDOM.nextDouble() * radius;
            double x = center.x + Math.cos(angle) * dist;
            double y = center.y + 0.5 + RANDOM.nextDouble() * 1.5;
            double z = center.z + Math.sin(angle) * dist;
            float size = 0.4f + RANDOM.nextFloat() * 0.3f;
            level.sendParticles(createDustParticle(r, g, b, size), x, y, z, 1, 0, 0, 0, 0);
        }
    }
    
    /**
     * Spawn dust particles in a ring pattern
     */
    private static void spawnDustParticlesRing(ServerLevel level, Vec3 center, double radius,
            float r, float g, float b) {
        int count = (int) (radius * 8);
        for (int i = 0; i < count; i++) {
            double angle = (double) i / count * 2 * Math.PI;
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + 0.5;
            double z = center.z + Math.sin(angle) * radius;
            float size = 0.5f + RANDOM.nextFloat() * 0.2f;
            level.sendParticles(createDustParticle(r, g, b, size), x, y, z, 1, 0, 0.1, 0, 0);
        }
    }
    
    /**
     * Spawn dust particles in a spiral pattern
     */
    private static void spawnDustParticlesSpiral(ServerLevel level, Vec3 center, double radius,
            float r, float g, float b) {
        int count = (int) (radius * 12);
        for (int i = 0; i < count; i++) {
            double progress = (double) i / count;
            double angle = progress * 4 * Math.PI;
            double dist = progress * radius;
            double x = center.x + Math.cos(angle) * dist;
            double y = center.y + 0.3 + progress * 1.5;
            double z = center.z + Math.sin(angle) * dist;
            float size = 0.4f + RANDOM.nextFloat() * 0.25f;
            level.sendParticles(createDustParticle(r, g, b, size), x, y, z, 1, 0, 0, 0, 0);
        }
    }
    
    /**
     * Spawn dust particles in an arc in front of player
     */
    private static void spawnDustParticlesArc(ServerLevel level, Vec3 center, float yaw, double radius,
            float r, float g, float b) {
        double baseAngle = Math.toRadians(-yaw + 90);
        int count = 15;
        for (int i = 0; i < count; i++) {
            double angleOffset = (i - count/2) * 0.15;
            double angle = baseAngle + angleOffset;
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + 0.8 + RANDOM.nextDouble() * 0.5;
            double z = center.z + Math.sin(angle) * radius;
            float size = 0.5f + RANDOM.nextFloat() * 0.3f;
            level.sendParticles(createDustParticle(r, g, b, size), x, y, z, 1, 0, 0, 0, 0);
        }
    }
    
    /**
     * Spawn dust particles in a protective shell around player
     */
    private static void spawnDustParticlesShell(ServerLevel level, Vec3 center, double radius,
            float r, float g, float b) {
        int count = 30;
        for (int i = 0; i < count; i++) {
            double phi = RANDOM.nextDouble() * Math.PI;
            double theta = RANDOM.nextDouble() * 2 * Math.PI;
            double x = center.x + Math.sin(phi) * Math.cos(theta) * radius;
            double y = center.y + 1.0 + Math.cos(phi) * radius;
            double z = center.z + Math.sin(phi) * Math.sin(theta) * radius;
            float size = 0.4f + RANDOM.nextFloat() * 0.2f;
            level.sendParticles(createDustParticle(r, g, b, size), x, y, z, 1, 0, 0, 0, 0);
        }
    }
    
    /**
     * Spawn dust particles in a line in the look direction
     */
    private static void spawnDustParticlesLine(ServerLevel level, Vec3 start, float yaw, double length,
            float r, float g, float b) {
        double angle = Math.toRadians(-yaw + 90);
        int count = (int) (length * 3);
        for (int i = 0; i < count; i++) {
            double progress = (double) i / count;
            double dist = progress * length;
            double x = start.x + Math.cos(angle) * dist + (RANDOM.nextDouble() - 0.5) * 0.3;
            double y = start.y + (RANDOM.nextDouble() - 0.5) * 0.3;
            double z = start.z + Math.sin(angle) * dist + (RANDOM.nextDouble() - 0.5) * 0.3;
            float size = 0.35f + RANDOM.nextFloat() * 0.2f;
            level.sendParticles(createDustParticle(r, g, b, size), x, y, z, 1, 0, 0, 0, 0);
        }
    }
    
    /**
     * Spawn dust particles in a fan pattern (multiple spread lines)
     */
    private static void spawnDustParticlesFan(ServerLevel level, Vec3 start, float yaw, double length,
            float r, float g, float b) {
        double baseAngle = Math.toRadians(-yaw + 90);
        int lines = 5;
        int particlesPerLine = 8;
        for (int line = 0; line < lines; line++) {
            double angleOffset = (line - lines/2) * 0.2;
            double angle = baseAngle + angleOffset;
            for (int i = 0; i < particlesPerLine; i++) {
                double progress = (double) i / particlesPerLine;
                double dist = progress * length;
                double x = start.x + Math.cos(angle) * dist;
                double y = start.y + (RANDOM.nextDouble() - 0.5) * 0.2;
                double z = start.z + Math.sin(angle) * dist;
                float size = 0.35f + RANDOM.nextFloat() * 0.2f;
                level.sendParticles(createDustParticle(r, g, b, size), x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }
    
    /**
     * Spawn dust particles on the ground
     */
    private static void spawnDustParticlesGround(ServerLevel level, Vec3 center, double radius,
            float r, float g, float b) {
        int count = (int) (radius * radius * 3);
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = RANDOM.nextDouble() * radius;
            double x = center.x + Math.cos(angle) * dist;
            double y = center.y + 0.1 + RANDOM.nextDouble() * 0.2;
            double z = center.z + Math.sin(angle) * dist;
            float size = 0.4f + RANDOM.nextFloat() * 0.25f;
            level.sendParticles(createDustParticle(r, g, b, size), x, y, z, 1, 0, 0.05, 0, 0);
        }
    }
    
    /**
     * Spawn dust particles falling from above (rain effect)
     */
    private static void spawnDustParticlesRain(ServerLevel level, Vec3 center, double radius,
            float r, float g, float b) {
        int count = (int) (radius * radius * 2);
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = RANDOM.nextDouble() * radius;
            double x = center.x + Math.cos(angle) * dist;
            double y = center.y + 3 + RANDOM.nextDouble() * 2;
            double z = center.z + Math.sin(angle) * dist;
            float size = 0.3f + RANDOM.nextFloat() * 0.2f;
            level.sendParticles(createDustParticle(r, g, b, size), x, y, z, 1, 0, -0.3, 0, 0.05);
        }
    }
    
    /**
     * Spawn dust particles moving upward (healing effect)
     */
    private static void spawnDustParticlesUpward(ServerLevel level, Vec3 center, double radius,
            float r, float g, float b) {
        int count = (int) (radius * 5);
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = RANDOM.nextDouble() * radius * 0.5;
            double x = center.x + Math.cos(angle) * dist;
            double y = center.y + RANDOM.nextDouble() * 0.5;
            double z = center.z + Math.sin(angle) * dist;
            float size = 0.4f + RANDOM.nextFloat() * 0.25f;
            level.sendParticles(createDustParticle(r, g, b, size), x, y, z, 1, 0, 0.2, 0, 0.05);
        }
    }
    
    private static void dealDamageToNearbyEnemies(ServerPlayer player, double damage, double range) {
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<Entity> entities = player.level().getEntities(player, searchBox, 
                e -> e instanceof LivingEntity && e != player);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                living.hurt(player.damageSources().playerAttack(player), (float) damage);
            }
        }
    }
    
    private static void applyEffectToNearbyEnemies(ServerPlayer player, 
            net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, 
            int duration, int amplifier, double range) {
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<Entity> entities = player.level().getEntities(player, searchBox, 
                e -> e instanceof LivingEntity && e != player);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(effect, duration, amplifier));
            }
        }
    }
    
    private static void setNearbyEnemiesOnFire(ServerPlayer player, double range, int fireTicks) {
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<Entity> entities = player.level().getEntities(player, searchBox, 
                e -> e instanceof LivingEntity && e != player);
        
        for (Entity entity : entities) {
            entity.setRemainingFireTicks(fireTicks);
        }
    }
    
    private static void healNearbyAllies(ServerPlayer player, float healAmount, double range) {
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<Entity> entities = player.level().getEntities(player, searchBox, 
                e -> e instanceof LivingEntity);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                // Heal players and passive mobs (allies)
                if (entity instanceof ServerPlayer || !isHostile(entity)) {
                    living.heal(healAmount);
                }
            }
        }
    }
    
    private static boolean isHostile(Entity entity) {
        return entity instanceof net.minecraft.world.entity.monster.Monster;
    }
}