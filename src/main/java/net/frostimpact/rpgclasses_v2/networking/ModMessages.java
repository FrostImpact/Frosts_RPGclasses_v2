package net.frostimpact.rpgclasses_v2.networking;

import net.frostimpact.rpgclasses_v2.networking.packet.PacketAllocateSkillPoint;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketAllocateStatPoint;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSelectClass;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncCooldowns;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncMana;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncRPGData;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncSeekerCharges;
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
        
        registrar.playToClient(
                PacketSyncSeekerCharges.TYPE,
                PacketSyncSeekerCharges.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() != null) {
                            var rpgData = context.player().getData(ModAttachments.PLAYER_RPG);
                            rpgData.setSeekerCharges(packet.seekerCharges());
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
    
    public static void sendToPlayer(PacketSyncSeekerCharges packet, ServerPlayer player) {
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
        
        // Special handling for Hawkeye Seekers (slot 4) - mana cost is 5 * seeker charges
        if (currentClass.equalsIgnoreCase("hawkeye") && abilitySlot == 4) {
            int seekerCharges = rpgData.getSeekerCharges();
            if (seekerCharges == 0) {
                player.displayClientMessage(Component.literal("§eNo Seeker charges! Gain charges while airborne."), true);
                return;
            }
            manaCost = 5 * seekerCharges;
        }
        
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
                    case 1 -> { // Precise Shot - massive particle arrow with green release aura
                        // Deal high damage to enemies in a line
                        Vec3 lookVec = player.getLookAngle();
                        dealDamageInLine(player, 7.0f + damageBonus * 1.75f, lookVec, 30.0, 0.8);
                        // Spawn the massive particle arrow effect (no entity arrows)
                        spawnPreciseShotParticleArrow(level, playerPos.add(0, player.getEyeHeight(), 0), lookVec);
                        // Release aura visual - shows the power release moment
                        spawnPreciseShotChargeAura(level, playerPos);
                    }
                    case 2 -> { // Multi-Shot - 6 small particle lines that pierce enemies (hitscan)
                        Vec3 lookVec = player.getLookAngle();
                        // Fire 6 particle lines one after another (hitscan rays)
                        spawnMultiShotParticleRays(player, level, playerPos.add(0, player.getEyeHeight(), 0), lookVec, 
                                6, 2.5f + damageBonus * 0.6f, 25.0);
                    }
                    case 3 -> { // Escape - launch player opposite of look direction
                        Vec3 lookVec = player.getLookAngle();
                        // Launch player in the opposite direction
                        Vec3 escapeVec = new Vec3(-lookVec.x * 2.0, 0.5, -lookVec.z * 2.0);
                        player.setDeltaMovement(player.getDeltaMovement().add(escapeVec));
                        player.hurtMarked = true; // Force velocity sync
                        // Escape visual - particles at launch point and trail
                        spawnEscapeEffect(level, playerPos, lookVec.reverse());
                    }
                    case 4 -> { // Rain of Arrows - constant damage zone with particle arrows (visual only)
                        // Deal constant damage in area (not based on arrow positions)
                        dealDamageToNearbyEnemies(player, 6.0 + damageBonus * 1.5, 8.0);
                        applyEffectToNearbyEnemies(player, MobEffects.MOVEMENT_SLOWDOWN, 100, 1, 8.0);
                        // Rain of particle arrows - visual only (no entity arrows)
                        spawnRainOfParticleArrowsEffect(level, playerPos, 8.0, 30);
                    }
                }
            }
            case "hawkeye" -> {
                var rpgData = player.getData(ModAttachments.PLAYER_RPG);
                switch (slot) {
                    case 1 -> { // Glide - grants slow falling for 10 seconds
                        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
                        // Wind particles around player
                        spawnGlideEffect(level, playerPos);
                    }
                    case 2 -> { // Updraft - launch player upward with strong upward motion
                        // Launch player upward
                        Vec3 currentVelocity = player.getDeltaMovement();
                        player.setDeltaMovement(currentVelocity.x * 0.5, 1.2, currentVelocity.z * 0.5);
                        player.hurtMarked = true; // Force velocity sync
                        // Add brief slow falling after launch
                        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0));
                        // Add a seeker charge (aerial affinity)
                        rpgData.addSeekerCharge();
                        // Sync seeker charges to client
                        sendToPlayer(new PacketSyncSeekerCharges(rpgData.getSeekerCharges()), player);
                        // Updraft visual - particles going up
                        spawnUpdraftEffect(level, playerPos);
                    }
                    case 3 -> { // Vault - launch forward and lob a projectile
                        // Launch player forward in look direction
                        Vec3 lookVec = player.getLookAngle();
                        Vec3 launchVec = new Vec3(lookVec.x * 1.5, 0.4, lookVec.z * 1.5);
                        player.setDeltaMovement(player.getDeltaMovement().add(launchVec));
                        player.hurtMarked = true;
                        // Spawn a lobbed arrow projectile
                        spawnVaultProjectile(player, level, 1.5f, 4.0f + damageBonus * 0.5f);
                        // Add a seeker charge
                        rpgData.addSeekerCharge();
                        // Sync seeker charges to client
                        sendToPlayer(new PacketSyncSeekerCharges(rpgData.getSeekerCharges()), player);
                        // Vault visual - directional particles
                        spawnVaultEffect(level, playerPos, lookVec);
                    }
                    case 4 -> { // Seekers - fires homing projectiles based on seeker charges
                        int charges = rpgData.consumeSeekerCharges();
                        if (charges > 0) {
                            // Find nearest enemies and shoot seeker projectiles at them
                            spawnSeekerProjectiles(player, level, charges, 5.0f + damageBonus * 0.5f);
                            // Seeker visual
                            spawnSeekerEffect(level, playerPos, charges);
                            // Sync seeker charges to client (now 0)
                            sendToPlayer(new PacketSyncSeekerCharges(rpgData.getSeekerCharges()), player);
                        } else {
                            player.displayClientMessage(Component.literal("§eNo Seeker charges! Gain charges while airborne."), true);
                        }
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
    
    // ===== Ranger Ability Helper Methods =====
    
    /**
     * Spawn a single powerful arrow in the player's look direction
     */
    private static void spawnRangerArrow(ServerPlayer player, ServerLevel level, float velocity, float damage, boolean crit) {
        net.minecraft.world.entity.projectile.Arrow arrow = new net.minecraft.world.entity.projectile.Arrow(level, player, 
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, velocity, 0.5F);
        arrow.setBaseDamage(damage);
        arrow.setCritArrow(crit);
        arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
        level.addFreshEntity(arrow);
    }
    
    /**
     * Spawn multiple arrows in a fan spread pattern
     */
    private static void spawnMultiShotArrows(ServerPlayer player, ServerLevel level, float velocity, float damage, 
            int arrowCount, float spreadAngle) {
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        float halfSpread = spreadAngle / 2.0f;
        // Handle single arrow case to avoid division by zero
        float angleStep = arrowCount > 1 ? spreadAngle / (arrowCount - 1) : 0;
        
        for (int i = 0; i < arrowCount; i++) {
            float offsetYaw = arrowCount > 1 ? -halfSpread + (angleStep * i) : 0;
            net.minecraft.world.entity.projectile.Arrow arrow = new net.minecraft.world.entity.projectile.Arrow(level, player,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
            arrow.shootFromRotation(player, pitch, yaw + offsetYaw, 0.0F, velocity, 1.0F);
            arrow.setBaseDamage(damage);
            arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
            level.addFreshEntity(arrow);
        }
    }
    
    /**
     * Spawn trap visual effect on the ground
     */
    private static void spawnTrapEffect(ServerLevel level, Vec3 center, double radius) {
        // Spawn spore blossom particles in a circle pattern
        int count = 40;
        for (int i = 0; i < count; i++) {
            double angle = (double) i / count * 2 * Math.PI;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR,
                    x, center.y + 0.1, z, 2, 0.2, 0.1, 0.2, 0.01);
        }
        // Inner web/trap particles
        for (int i = 0; i < 20; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = RANDOM.nextDouble() * radius * 0.7;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ITEM_SLIME,
                    center.x + Math.cos(angle) * dist, center.y + 0.1, center.z + Math.sin(angle) * dist,
                    1, 0, 0, 0, 0);
        }
    }
    
    /**
     * Spawn rain of arrows visual effect - arrows falling from above
     */
    private static void spawnRainOfArrowsEffect(ServerPlayer player, ServerLevel level, double radius, int arrowCount) {
        Vec3 center = player.position();
        for (int i = 0; i < arrowCount; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = RANDOM.nextDouble() * radius;
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            double y = center.y + 8 + RANDOM.nextDouble() * 3; // Spawn high above
            
            net.minecraft.world.entity.projectile.Arrow arrow = new net.minecraft.world.entity.projectile.Arrow(level, player,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
            arrow.setPos(x, y, z);
            arrow.setOwner(player);
            // Shoot downward
            arrow.shoot(0, -1, 0, 2.0F, 3.0F);
            arrow.setBaseDamage(3.0);
            arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
            level.addFreshEntity(arrow);
        }
        // Particle rain effect
        for (int i = 0; i < 30; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = RANDOM.nextDouble() * radius;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                    center.x + Math.cos(angle) * dist, center.y + 4 + RANDOM.nextDouble() * 2, 
                    center.z + Math.sin(angle) * dist,
                    1, 0, -0.5, 0, 0.1);
        }
    }
    
    // ===== NEW Ranger Particle-Based Ability Methods =====
    
    /**
     * Spawn a massive dramatic particle arrow effect for Precise Shot
     * The arrow is made entirely of dust particles shaped like a real arrow
     * It moves slower with radiating particle circles around the projectile
     */
    private static void spawnPreciseShotParticleArrow(ServerLevel level, Vec3 start, Vec3 direction) {
        Vec3 normalizedDir = direction.normalize();
        double arrowLength = 35.0; // Extended range for more drama
        
        // Primary green color for Ranger theme
        float r = 0.15f, g = 0.85f, b = 0.25f;
        // Secondary brighter green for highlights
        float r2 = 0.3f, g2 = 1.0f, b2 = 0.4f;
        
        // ===== ARROW SHAFT - Dense dust particle core =====
        // Create the arrow shaft with layered particles for depth
        for (int layer = 0; layer < 3; layer++) {
            double layerOffset = layer * 0.08;
            for (int i = 0; i < 100; i++) {
                double progress = (double) i / 100.0 * (arrowLength - 3.0); // Leave room for arrowhead
                Vec3 pos = start.add(normalizedDir.scale(progress));
                
                // Vary particle positions slightly for thickness
                double offsetX = (RANDOM.nextDouble() - 0.5) * (0.15 + layerOffset);
                double offsetY = (RANDOM.nextDouble() - 0.5) * (0.15 + layerOffset);
                double offsetZ = (RANDOM.nextDouble() - 0.5) * (0.15 + layerOffset);
                
                // Arrow core particles (bright green dust)
                float size = 0.7f + RANDOM.nextFloat() * 0.25f;
                level.sendParticles(createDustParticle(r, g, b, size), 
                        pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ, 1, 0.02, 0.02, 0.02, 0);
            }
        }
        
        // ===== ARROW HEAD - Triangle/diamond shape made of particles =====
        Vec3 headStart = start.add(normalizedDir.scale(arrowLength - 3.0));
        Vec3 perpVec1 = getPerpendicular(normalizedDir);
        Vec3 perpVec2 = normalizedDir.cross(perpVec1).normalize();
        
        // Create triangular arrowhead
        double headLength = 3.0;
        for (int slice = 0; slice < 30; slice++) {
            double sliceProgress = (double) slice / 30.0;
            Vec3 sliceCenter = headStart.add(normalizedDir.scale(sliceProgress * headLength));
            double sliceRadius = (1.0 - sliceProgress) * 0.5; // Decreasing radius toward tip
            
            // Draw diamond cross-section at this slice
            int points = 8;
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI;
                double ox = Math.cos(angle) * sliceRadius;
                double oy = Math.sin(angle) * sliceRadius;
                Vec3 particlePos = sliceCenter.add(perpVec1.scale(ox)).add(perpVec2.scale(oy));
                
                float size = 0.8f - (float) sliceProgress * 0.3f;
                level.sendParticles(createDustParticle(r2, g2, b2, size),
                        particlePos.x, particlePos.y, particlePos.z, 1, 0.01, 0.01, 0.01, 0);
            }
        }
        
        // Arrow tip flash
        Vec3 arrowTip = start.add(normalizedDir.scale(arrowLength));
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                arrowTip.x, arrowTip.y, arrowTip.z, 1, 0, 0, 0, 0);
        level.sendParticles(createDustParticle(0.5f, 1.0f, 0.6f, 1.2f),
                arrowTip.x, arrowTip.y, arrowTip.z, 5, 0.1, 0.1, 0.1, 0);
        
        // ===== FLETCHING (Arrow feathers at back) =====
        Vec3 fletchStart = start;
        for (int feather = 0; feather < 4; feather++) {
            double featherAngle = (double) feather / 4.0 * 2 * Math.PI + Math.PI / 4;
            for (int i = 0; i < 10; i++) {
                double featherLength = 0.6 - (double) i / 10 * 0.4;
                Vec3 featherPos = fletchStart.add(normalizedDir.scale(i * 0.1));
                double ox = Math.cos(featherAngle) * featherLength;
                double oy = Math.sin(featherAngle) * featherLength;
                Vec3 particlePos = featherPos.add(perpVec1.scale(ox)).add(perpVec2.scale(oy));
                
                level.sendParticles(createDustParticle(0.2f, 0.7f, 0.25f, 0.4f),
                        particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
            }
        }
        
        // ===== RADIATING PARTICLE CIRCLES - Pulse outward along the path =====
        for (int ring = 0; ring < 12; ring++) {
            double ringProgress = (double) ring / 12.0 * arrowLength;
            Vec3 ringCenter = start.add(normalizedDir.scale(ringProgress));
            
            // Expanding ring effect - radius grows based on position along arrow
            double baseRadius = 0.4 + (ring % 3) * 0.25;
            int circlePoints = 16;
            for (int p = 0; p < circlePoints; p++) {
                double angle = (double) p / circlePoints * 2 * Math.PI;
                double ox = Math.cos(angle) * baseRadius;
                double oy = Math.sin(angle) * baseRadius;
                Vec3 circlePos = ringCenter.add(perpVec1.scale(ox)).add(perpVec2.scale(oy));
                
                float circleSize = 0.35f + RANDOM.nextFloat() * 0.15f;
                // Alternating colors for pulsing effect
                if (ring % 2 == 0) {
                    level.sendParticles(createDustParticle(0.3f, 1.0f, 0.4f, circleSize),
                            circlePos.x, circlePos.y, circlePos.z, 1, 0, 0, 0, 0);
                } else {
                    level.sendParticles(createDustParticle(0.2f, 0.8f, 0.3f, circleSize),
                            circlePos.x, circlePos.y, circlePos.z, 1, 0, 0, 0, 0);
                }
            }
            
            // Add END_ROD particles at cardinal points for extra glow
            for (int cardinal = 0; cardinal < 4; cardinal++) {
                double cardAngle = cardinal * Math.PI / 2;
                double ox = Math.cos(cardAngle) * baseRadius * 0.6;
                double oy = Math.sin(cardAngle) * baseRadius * 0.6;
                Vec3 glowPos = ringCenter.add(perpVec1.scale(ox)).add(perpVec2.scale(oy));
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        glowPos.x, glowPos.y, glowPos.z, 1, 0.01, 0.01, 0.01, 0);
            }
        }
        
        // ===== TRAIL EFFECT - Lingering particles behind =====
        for (int i = 0; i < 40; i++) {
            double trailProgress = RANDOM.nextDouble() * arrowLength * 0.7;
            Vec3 trailPos = start.add(normalizedDir.scale(trailProgress));
            double spreadX = (RANDOM.nextDouble() - 0.5) * 0.3;
            double spreadY = (RANDOM.nextDouble() - 0.5) * 0.3;
            double spreadZ = (RANDOM.nextDouble() - 0.5) * 0.3;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    trailPos.x + spreadX, trailPos.y + spreadY, trailPos.z + spreadZ,
                    1, 0, 0.05, 0, 0.01);
        }
        
        // Impact glow at end
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW,
                arrowTip.x, arrowTip.y, arrowTip.z, 20, 0.3, 0.3, 0.3, 0.02);
    }
    
    /**
     * Spawn the charging aura effect around the player for Precise Shot
     * Creates a SHAPED aura (hexagonal barrier) instead of random particles
     */
    private static void spawnPreciseShotChargeAura(ServerLevel level, Vec3 center) {
        // ===== HEXAGONAL SHAPED BARRIER =====
        // Create rotating hexagonal rings at different heights
        for (int ring = 0; ring < 3; ring++) {
            double ringY = center.y + 0.3 + ring * 0.8;
            double radius = 1.2 - ring * 0.15;
            double rotationOffset = ring * Math.PI / 6; // Offset each ring
            
            // Draw hexagon shape
            int sides = 6;
            for (int side = 0; side < sides; side++) {
                double angle1 = (double) side / sides * 2 * Math.PI + rotationOffset;
                double angle2 = (double) (side + 1) / sides * 2 * Math.PI + rotationOffset;
                
                double x1 = center.x + Math.cos(angle1) * radius;
                double z1 = center.z + Math.sin(angle1) * radius;
                double x2 = center.x + Math.cos(angle2) * radius;
                double z2 = center.z + Math.sin(angle2) * radius;
                
                // Draw line between vertices
                int lineParticles = 8;
                for (int p = 0; p < lineParticles; p++) {
                    double t = (double) p / lineParticles;
                    double x = x1 + (x2 - x1) * t;
                    double z = z1 + (z2 - z1) * t;
                    
                    level.sendParticles(createDustParticle(0.2f, 0.9f, 0.3f, 0.6f),
                            x, ringY, z, 1, 0.02, 0.02, 0.02, 0);
                }
                
                // Vertex glow
                level.sendParticles(createDustParticle(0.4f, 1.0f, 0.5f, 0.8f),
                        x1, ringY, z1, 2, 0.05, 0.05, 0.05, 0);
            }
        }
        
        // ===== VERTICAL CONNECTING LINES =====
        for (int v = 0; v < 6; v++) {
            double angle = (double) v / 6 * 2 * Math.PI;
            double x = center.x + Math.cos(angle) * 1.2;
            double z = center.z + Math.sin(angle) * 1.2;
            
            for (int h = 0; h < 12; h++) {
                double y = center.y + 0.3 + (double) h / 12 * 1.6;
                level.sendParticles(createDustParticle(0.25f, 0.85f, 0.35f, 0.4f),
                        x, y, z, 1, 0.01, 0.01, 0.01, 0);
            }
        }
        
        // ===== INNER DIAMOND SHAPE =====
        double innerRadius = 0.6;
        for (int side = 0; side < 4; side++) {
            double angle1 = side * Math.PI / 2;
            double angle2 = (side + 1) * Math.PI / 2;
            double y = center.y + 1.0;
            
            double x1 = center.x + Math.cos(angle1) * innerRadius;
            double z1 = center.z + Math.sin(angle1) * innerRadius;
            double x2 = center.x + Math.cos(angle2) * innerRadius;
            double z2 = center.z + Math.sin(angle2) * innerRadius;
            
            for (int p = 0; p < 6; p++) {
                double t = (double) p / 6;
                level.sendParticles(createDustParticle(0.35f, 1.0f, 0.45f, 0.5f),
                        x1 + (x2 - x1) * t, y, z1 + (z2 - z1) * t, 1, 0.01, 0.01, 0.01, 0);
            }
        }
        
        // ===== CENTRAL POWER CORE =====
        // Vertical beam in center
        for (int i = 0; i < 15; i++) {
            double y = center.y + (double) i / 15 * 2.5;
            level.sendParticles(createDustParticle(0.4f, 1.0f, 0.5f, 0.7f),
                    center.x, y, center.z, 1, 0.05, 0.05, 0.05, 0);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    center.x, y, center.z, 1, 0.03, 0.1, 0.03, 0);
        }
        
        // Spiraling enchantment particles
        for (int i = 0; i < 30; i++) {
            double angle = (double) i / 30 * 6 * Math.PI;
            double spiralRadius = 0.3 + (double) i / 30 * 0.5;
            double y = center.y + (double) i / 30 * 2.5;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    center.x + Math.cos(angle) * spiralRadius, y, center.z + Math.sin(angle) * spiralRadius,
                    1, 0, 0, 0, 0);
        }
        
        // Top crown effect
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y + 2.5, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW,
                center.x, center.y + 1.0, center.z, 25, 0.4, 0.7, 0.4, 0.02);
    }
    
    /**
     * Spawn 6 particle lines (hitscan rays) for Multi-Shot that pierce enemies
     */
    private static void spawnMultiShotParticleRays(ServerPlayer player, ServerLevel level, Vec3 start, Vec3 direction, 
            int rayCount, float damage, double range) {
        Vec3 normalizedDir = direction.normalize();
        float baseYaw = player.getYRot();
        float basePitch = player.getXRot();
        
        // Calculate spread angles for 6 rays
        float spreadAngle = 20.0f; // Total spread in degrees
        float halfSpread = spreadAngle / 2.0f;
        float angleStep = rayCount > 1 ? spreadAngle / (rayCount - 1) : 0;
        
        // Green ranger color theme
        float r = 0.3f, g = 0.85f, b = 0.35f;
        
        for (int ray = 0; ray < rayCount; ray++) {
            float yawOffset = rayCount > 1 ? -halfSpread + (angleStep * ray) : 0;
            
            // Calculate ray direction with spread
            // Note: -90 degree offset converts Minecraft's yaw (0=south, 90=west) to standard math coordinates
            double yawRad = Math.toRadians(-baseYaw - 90 + yawOffset);
            double pitchRad = Math.toRadians(-basePitch);
            Vec3 rayDir = new Vec3(
                    Math.cos(yawRad) * Math.cos(pitchRad),
                    Math.sin(pitchRad),
                    Math.sin(yawRad) * Math.cos(pitchRad)
            ).normalize();
            
            // Deal damage along the ray (piercing - hits all enemies in line)
            dealDamageInLine(player, damage, rayDir, range, 0.5);
            
            // Spawn particle line
            for (int i = 0; i < 25; i++) {
                double progress = (double) i / 25.0 * range;
                Vec3 pos = start.add(rayDir.scale(progress));
                
                // Thin green particle line
                float size = 0.25f + RANDOM.nextFloat() * 0.1f;
                level.sendParticles(createDustParticle(r, g, b, size),
                        pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0);
                
                // Occasional brighter particles
                if (i % 5 == 0) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                            pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
                }
            }
        }
        
        // Muzzle flash effect at start
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                start.x, start.y, start.z, 1, 0, 0, 0, 0);
        level.sendParticles(createDustParticle(0.5f, 1.0f, 0.5f, 0.7f),
                start.x, start.y, start.z, 10, 0.2, 0.2, 0.2, 0.05);
    }
    
    /**
     * Spawn the Escape ability visual effect
     */
    private static void spawnEscapeEffect(ServerLevel level, Vec3 center, Vec3 escapeDirection) {
        // Trail particles in escape direction
        for (int i = 0; i < 20; i++) {
            double progress = (double) i / 20;
            double x = center.x + escapeDirection.x * progress * 2;
            double y = center.y + 0.5 + progress * 0.3;
            double z = center.z + escapeDirection.z * progress * 2;
            
            // Green dash particles
            level.sendParticles(createDustParticle(0.3f, 0.8f, 0.4f, 0.4f),
                    x, y, z, 1, 0.1, 0.1, 0.1, 0);
        }
        
        // Burst at origin point
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                center.x, center.y + 0.5, center.z, 8, 0.3, 0.2, 0.3, 0.05);
        level.sendParticles(createDustParticle(0.2f, 0.9f, 0.3f, 0.6f),
                center.x, center.y + 0.5, center.z, 15, 0.4, 0.3, 0.4, 0.1);
        
        // Sweep effect
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                center.x, center.y + 1, center.z, 2, 0.3, 0.2, 0.3, 0);
    }
    
    /**
     * Spawn rain of particle arrows - ULTIMATE ability visual effect
     * Dramatically enhanced with massive coverage, impact waves, and afterglow
     * Damage is handled separately by dealDamageToNearbyEnemies in the calling method.
     */
    private static void spawnRainOfParticleArrowsEffect(ServerLevel level, Vec3 center, double radius, int arrowCount) {
        // Enhanced green ranger theme with brighter colors
        float r = 0.2f, g = 0.85f, b = 0.25f;
        float r2 = 0.35f, g2 = 1.0f, b2 = 0.4f;
        
        // ===== SKYWARD ACTIVATION BURST =====
        // Initial dramatic sky beam
        for (int i = 0; i < 25; i++) {
            double y = center.y + 5 + i * 0.5;
            level.sendParticles(createDustParticle(r2, g2, b2, 0.9f),
                    center.x, y, center.z, 3, 0.3, 0.1, 0.3, 0.01);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    center.x, y, center.z, 1, 0.1, 0, 0.1, 0);
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y + 15, center.z, 3, 0.5, 0.5, 0.5, 0);
        
        // ===== TARGETING ZONE INDICATOR =====
        // Expanding rings on ground showing damage area
        for (int ring = 0; ring < 5; ring++) {
            double ringRadius = radius * ((double) ring / 5);
            int points = (int) (ringRadius * 10) + 12;
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * ringRadius;
                double z = center.z + Math.sin(angle) * ringRadius;
                level.sendParticles(createDustParticle(r, g, b, 0.5f),
                        x, center.y + 0.05, z, 1, 0.05, 0, 0.05, 0);
            }
        }
        
        // Cross-hatch pattern on ground
        for (int line = 0; line < 8; line++) {
            double lineAngle = (double) line / 8 * Math.PI;
            for (int p = 0; p < 20; p++) {
                double dist = (double) p / 20 * radius;
                double x1 = center.x + Math.cos(lineAngle) * dist;
                double z1 = center.z + Math.sin(lineAngle) * dist;
                double x2 = center.x - Math.cos(lineAngle) * dist;
                double z2 = center.z - Math.sin(lineAngle) * dist;
                level.sendParticles(createDustParticle(r, g, b, 0.35f),
                        x1, center.y + 0.05, z1, 1, 0.02, 0, 0.02, 0);
                level.sendParticles(createDustParticle(r, g, b, 0.35f),
                        x2, center.y + 0.05, z2, 1, 0.02, 0, 0.02, 0);
            }
        }
        
        // ===== RAIN OF PARTICLE ARROWS - Dense coverage =====
        // Triple the arrow count for ultimate effect
        int totalArrows = arrowCount * 3;
        for (int arrow = 0; arrow < totalArrows; arrow++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = RANDOM.nextDouble() * radius;
            double startX = center.x + Math.cos(angle) * dist;
            double startZ = center.z + Math.sin(angle) * dist;
            double startY = center.y + 8 + RANDOM.nextDouble() * 6;
            
            // Slight angle variance for natural look
            double tiltX = (RANDOM.nextDouble() - 0.5) * 0.15;
            double tiltZ = (RANDOM.nextDouble() - 0.5) * 0.15;
            
            // Full arrow shape - shaft
            for (int i = 0; i < 12; i++) {
                double progress = (double) i / 12;
                double y = startY - progress * 1.2;
                double x = startX + tiltX * progress;
                double z = startZ + tiltZ * progress;
                
                float size = 0.45f - (float) progress * 0.2f;
                level.sendParticles(createDustParticle(r, g, b, size),
                        x + (RANDOM.nextDouble() - 0.5) * 0.08,
                        y,
                        z + (RANDOM.nextDouble() - 0.5) * 0.08,
                        1, 0, -0.4, 0, 0.08);
            }
            
            // Arrow head - brighter/larger
            level.sendParticles(createDustParticle(r2, g2, b2, 0.6f),
                    startX, startY, startZ, 2, 0.05, 0.05, 0.05, 0);
            
            // Every 3rd arrow gets extra glow
            if (arrow % 3 == 0) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        startX, startY, startZ, 1, 0, -0.3, 0, 0.05);
            }
        }
        
        // ===== IMPACT WAVES =====
        // Multiple expanding shockwave rings
        for (int wave = 0; wave < 4; wave++) {
            double waveRadius = radius * 0.3 + wave * (radius * 0.2);
            int wavePoints = (int) (waveRadius * 12);
            for (int p = 0; p < wavePoints; p++) {
                double angle = (double) p / wavePoints * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * waveRadius;
                double z = center.z + Math.sin(angle) * waveRadius;
                
                level.sendParticles(createDustParticle(r2, g2, b2, 0.55f),
                        x, center.y + 0.1 + wave * 0.3, z, 1, 0.08, 0.15, 0.08, 0.02);
            }
        }
        
        // ===== GROUND IMPACT PARTICLES =====
        // Dense scattered impact effect
        for (int i = 0; i < 80; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = RANDOM.nextDouble() * radius;
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            
            // Impact spark
            level.sendParticles(createDustParticle(0.35f, 0.95f, 0.4f, 0.5f),
                    x, center.y + 0.1, z, 1, 0.15, 0.2, 0.15, 0.03);
            
            // Debris/dust kick-up
            if (i % 3 == 0) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                        x, center.y + 0.1, z, 1, 0.1, 0.05, 0.1, 0.01);
            }
        }
        
        // ===== AFTERGLOW EFFECT =====
        // Central pillar of light
        for (int i = 0; i < 15; i++) {
            double y = center.y + (double) i / 15 * 4;
            double glowRadius = 0.5 - (double) i / 15 * 0.3;
            level.sendParticles(createDustParticle(0.4f, 1.0f, 0.5f, 0.6f),
                    center.x, y, center.z, 2, glowRadius, 0.1, glowRadius, 0.01);
        }
        
        // Massive ambient glow
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW,
                center.x, center.y + 1.5, center.z, 50, radius * 0.5, 1.0, radius * 0.5, 0.02);
        
        // Enchantment particles for magical feel
        for (int i = 0; i < 30; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = RANDOM.nextDouble() * radius * 0.8;
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            double y = center.y + RANDOM.nextDouble() * 2;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    x, y, z, 2, 0.1, 0.2, 0.1, 0.02);
        }
        
        // Final outer boundary ring
        spawnDustParticlesRing(level, center, radius, 0.4f, 1.0f, 0.5f);
        spawnDustParticlesRing(level, center, radius * 0.7, 0.3f, 0.9f, 0.4f);
    }
    
    /**
     * Deal damage to all enemies along a line (for hitscan/piercing abilities)
     */
    private static void dealDamageInLine(ServerPlayer player, float damage, Vec3 direction, double range, double width) {
        Vec3 start = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 end = start.add(direction.normalize().scale(range));
        
        // Create a bounding box that covers the entire line
        AABB lineBox = new AABB(
                Math.min(start.x, end.x) - width, Math.min(start.y, end.y) - width, Math.min(start.z, end.z) - width,
                Math.max(start.x, end.x) + width, Math.max(start.y, end.y) + width, Math.max(start.z, end.z) + width
        );
        
        List<Entity> entities = player.level().getEntities(player, lineBox,
                e -> e instanceof LivingEntity && e != player);
        
        Vec3 lineDir = direction.normalize();
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                // Check if entity is close enough to the line
                Vec3 entityPos = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
                Vec3 toEntity = entityPos.subtract(start);
                double projection = toEntity.dot(lineDir);
                
                // Only hit if entity is within the line range
                if (projection >= 0 && projection <= range) {
                    Vec3 closestPointOnLine = start.add(lineDir.scale(projection));
                    double distToLine = entityPos.distanceTo(closestPointOnLine);
                    
                    if (distToLine <= width + entity.getBbWidth() * 0.5) {
                        living.hurt(player.damageSources().playerAttack(player), damage);
                    }
                }
            }
        }
    }
    
    /**
     * Get a vector perpendicular to the given direction
     */
    private static Vec3 getPerpendicular(Vec3 direction) {
        if (Math.abs(direction.y) < 0.9) {
            return direction.cross(new Vec3(0, 1, 0)).normalize();
        } else {
            return direction.cross(new Vec3(1, 0, 0)).normalize();
        }
    }
    
    // ===== Hawkeye Ability Helper Methods =====
    
    /**
     * Spawn glide visual effect - ENHANCED wind particles with feather trail
     */
    private static void spawnGlideEffect(ServerLevel level, Vec3 center) {
        // Cyan/white color theme for wind
        float r = 0.7f, g = 0.9f, b = 1.0f;
        
        // ===== WING FORMATION - Feather-like spread =====
        for (int wing = 0; wing < 2; wing++) {
            double wingAngle = wing == 0 ? -Math.PI / 4 : Math.PI / 4;
            for (int feather = 0; feather < 12; feather++) {
                double featherAngle = wingAngle + (feather - 6) * 0.08;
                double featherLength = 1.5 - Math.abs(feather - 6) * 0.1;
                
                for (int p = 0; p < 8; p++) {
                    double dist = (double) p / 8 * featherLength;
                    double x = center.x + Math.cos(featherAngle) * dist;
                    double z = center.z + Math.sin(featherAngle) * dist;
                    double y = center.y + 1.2 - (double) p / 8 * 0.3;
                    
                    level.sendParticles(createDustParticle(r, g, b, 0.4f),
                            x, y, z, 1, 0.02, 0.02, 0.02, 0);
                }
            }
        }
        
        // ===== SPIRALING WIND CURRENTS =====
        for (int spiral = 0; spiral < 3; spiral++) {
            for (int i = 0; i < 25; i++) {
                double angle = (double) i / 25 * 4 * Math.PI + spiral * Math.PI * 2 / 3;
                double radius = 0.6 + (double) i / 25 * 0.5;
                double yOffset = (double) i / 25 * 2.5;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                        x, center.y + yOffset, z, 1, 0.05, 0.1, 0.05, 0.02);
            }
        }
        
        // ===== WIND STREAMS =====
        for (int stream = 0; stream < 8; stream++) {
            double streamAngle = (double) stream / 8 * 2 * Math.PI;
            for (int p = 0; p < 6; p++) {
                double dist = 0.8 + (double) p / 6 * 1.0;
                double x = center.x + Math.cos(streamAngle) * dist;
                double z = center.z + Math.sin(streamAngle) * dist;
                
                level.sendParticles(createDustParticle(0.8f, 0.95f, 1.0f, 0.35f),
                        x, center.y + 0.8, z, 1, 0.1, 0.1, 0.1, 0.01);
            }
        }
        
        // Sweep attacks for wind effect
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                center.x, center.y + 1, center.z, 5, 0.6, 0.4, 0.6, 0);
        
        // Central glow
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                center.x, center.y + 1.2, center.z, 10, 0.3, 0.4, 0.3, 0.01);
    }
    
    /**
     * Spawn updraft visual effect - ENHANCED dramatic vertical launch
     */
    private static void spawnUpdraftEffect(ServerLevel level, Vec3 center) {
        // Light blue wind theme
        float r = 0.6f, g = 0.85f, b = 1.0f;
        
        // ===== CENTRAL VORTEX COLUMN =====
        for (int ring = 0; ring < 8; ring++) {
            double ringY = center.y + ring * 0.6;
            double ringRadius = 1.0 - ring * 0.08;
            int points = 16;
            double rotation = ring * 0.4; // Each ring rotated
            
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI + rotation;
                double x = center.x + Math.cos(angle) * ringRadius;
                double z = center.z + Math.sin(angle) * ringRadius;
                
                level.sendParticles(createDustParticle(r, g, b, 0.5f),
                        x, ringY, z, 1, 0.05, 0.2, 0.05, 0.02);
            }
        }
        
        // ===== VERTICAL STREAMS =====
        for (int stream = 0; stream < 6; stream++) {
            double streamAngle = (double) stream / 6 * 2 * Math.PI;
            double streamRadius = 0.4;
            
            for (int i = 0; i < 20; i++) {
                double y = center.y + (double) i / 20 * 5;
                double spiralAngle = streamAngle + (double) i / 20 * Math.PI;
                double x = center.x + Math.cos(spiralAngle) * streamRadius;
                double z = center.z + Math.sin(spiralAngle) * streamRadius;
                
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                        x, y, z, 1, 0.03, 0.15, 0.03, 0.05);
            }
        }
        
        // ===== GROUND EXPLOSION =====
        // Expanding ring at base
        for (int ring = 0; ring < 3; ring++) {
            double radius = 0.5 + ring * 0.5;
            int points = (int) (radius * 12);
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                        x, center.y + 0.15, z, 2, 0.1, 0.05, 0.1, 0.02);
                level.sendParticles(createDustParticle(0.7f, 0.9f, 1.0f, 0.4f),
                        x, center.y + 0.2 + ring * 0.2, z, 1, 0.05, 0.1, 0.05, 0.01);
            }
        }
        
        // ===== TOP BURST =====
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y + 4, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                center.x, center.y + 3, center.z, 15, 0.3, 0.5, 0.3, 0.03);
        
        // Sweep effect at launch
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                center.x, center.y + 0.5, center.z, 4, 0.5, 0.2, 0.5, 0);
    }
    
    /**
     * Spawn vault visual effect - ENHANCED directional dash with trail
     */
    private static void spawnVaultEffect(ServerLevel level, Vec3 center, Vec3 direction) {
        // Golden/yellow energy theme
        float r = 1.0f, g = 0.85f, b = 0.4f;
        
        // ===== DASH TRAIL =====
        Vec3 normalizedDir = direction.normalize();
        for (int layer = 0; layer < 3; layer++) {
            for (int i = 0; i < 25; i++) {
                double progress = (double) i / 25;
                double trailLength = 3.0;
                Vec3 pos = center.add(normalizedDir.scale(progress * trailLength));
                
                // Spread based on layer
                double spread = layer * 0.15;
                double offsetX = (RANDOM.nextDouble() - 0.5) * spread;
                double offsetY = (RANDOM.nextDouble() - 0.5) * spread;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * spread;
                
                float size = 0.5f - (float) progress * 0.3f;
                level.sendParticles(createDustParticle(r, g, b, size),
                        pos.x + offsetX, pos.y + 1 + offsetY, pos.z + offsetZ,
                        1, 0.02, 0.02, 0.02, 0);
            }
        }
        
        // ===== END ROD STREAK =====
        for (int i = 0; i < 15; i++) {
            double progress = (double) i / 15;
            Vec3 pos = center.add(normalizedDir.scale(progress * 2.5));
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    pos.x, pos.y + 1, pos.z, 1, 0.05, 0.05, 0.05, 0);
        }
        
        // ===== LAUNCH BURST =====
        // Ring at launch point
        for (int p = 0; p < 16; p++) {
            double angle = (double) p / 16 * 2 * Math.PI;
            double x = center.x + Math.cos(angle) * 0.8;
            double z = center.z + Math.sin(angle) * 0.8;
            level.sendParticles(createDustParticle(1.0f, 0.9f, 0.5f, 0.5f),
                    x, center.y + 0.8, z, 1, 0.05, 0.05, 0.05, 0);
        }
        
        // Sweep effect
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                center.x, center.y + 1, center.z, 3, 0.4, 0.3, 0.4, 0);
        
        // Flash at start
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y + 1, center.z, 1, 0, 0, 0, 0);
    }
    
    /**
     * Spawn vault projectile - lobbed arrow with trailing particles
     */
    private static void spawnVaultProjectile(ServerPlayer player, ServerLevel level, float velocity, float damage) {
        Vec3 lookVec = player.getLookAngle();
        net.minecraft.world.entity.projectile.Arrow arrow = new net.minecraft.world.entity.projectile.Arrow(level, player,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
        // Lob the projectile (arc trajectory)
        arrow.shootFromRotation(player, player.getXRot() - 30, player.getYRot(), 0.0F, velocity, 1.0F);
        arrow.setBaseDamage(damage);
        arrow.setGlowingTag(true); // Make it visible
        arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
        level.addFreshEntity(arrow);
    }
    
    /**
     * Spawn seeker visual effect - ENHANCED dramatic seeker launch
     */
    private static void spawnSeekerEffect(ServerLevel level, Vec3 center, int charges) {
        // Purple/magenta energy theme for seekers
        float r = 0.7f, g = 0.3f, b = 1.0f;
        
        // ===== CHARGING RINGS - one per seeker =====
        for (int charge = 0; charge < charges; charge++) {
            double chargeAngle = (double) charge / charges * 2 * Math.PI;
            double chargeRadius = 1.2;
            double chargeX = center.x + Math.cos(chargeAngle) * chargeRadius;
            double chargeZ = center.z + Math.sin(chargeAngle) * chargeRadius;
            
            // Each seeker charge as a small ring
            for (int p = 0; p < 12; p++) {
                double angle = (double) p / 12 * 2 * Math.PI;
                double ringRadius = 0.3;
                double x = chargeX + Math.cos(angle) * ringRadius;
                double z = chargeZ + Math.sin(angle) * ringRadius;
                
                level.sendParticles(createDustParticle(r, g, b, 0.5f),
                        x, center.y + 1.5, z, 1, 0.02, 0.02, 0.02, 0);
            }
            
            // Center glow for each charge
            level.sendParticles(createDustParticle(0.9f, 0.5f, 1.0f, 0.7f),
                    chargeX, center.y + 1.5, chargeZ, 3, 0.1, 0.1, 0.1, 0);
        }
        
        // ===== CENTRAL CONVERGENCE BURST =====
        // All seekers launch from center
        for (int i = 0; i < charges * 8; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = RANDOM.nextDouble() * 0.5;
            level.sendParticles(createDustParticle(0.8f, 0.4f, 1.0f, 0.6f),
                    center.x + Math.cos(angle) * dist, center.y + 1.5,
                    center.z + Math.sin(angle) * dist, 1, 0.15, 0.15, 0.15, 0.03);
        }
        
        // ===== UPWARD SPIRAL =====
        for (int i = 0; i < 30; i++) {
            double angle = (double) i / 30 * 4 * Math.PI;
            double radius = 0.4 + (double) i / 30 * 0.5;
            double y = center.y + 1 + (double) i / 30 * 1.5;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    center.x + Math.cos(angle) * radius, y, center.z + Math.sin(angle) * radius,
                    2, 0, 0.1, 0, 0.02);
        }
        
        // Witch particles burst
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                center.x, center.y + 1.5, center.z, charges * 5, 0.4, 0.4, 0.4, 0.08);
        
        // Flash
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y + 2, center.z, 1, 0, 0, 0, 0);
    }
    
    /**
     * Spawn seeker projectiles - PARTICLE-BASED HOMING ENTITIES (not arrows!)
     * Seekers are now visual particle formations that home in on enemies
     */
    private static void spawnSeekerProjectiles(ServerPlayer player, ServerLevel level, int chargeCount, float damage) {
        // Find nearby enemies
        AABB searchBox = player.getBoundingBox().inflate(20.0); // Extended range
        List<Entity> entities = player.level().getEntities(player, searchBox,
                e -> e instanceof LivingEntity && e != player && isHostile(e));
        
        Vec3 playerPos = player.position().add(0, 1.5, 0);
        
        // Purple/magenta seeker theme
        float r = 0.75f, g = 0.35f, b = 1.0f;
        
        if (entities.isEmpty()) {
            // No enemies - fire seeking orbs in look direction that spread out
            Vec3 lookVec = player.getLookAngle();
            for (int i = 0; i < chargeCount; i++) {
                // Calculate spread direction
                float yawOffset = ((float) i / chargeCount - 0.5f) * 30f;
                float pitchOffset = (RANDOM.nextFloat() - 0.5f) * 10f;
                
                double yawRad = Math.toRadians(-player.getYRot() - 90 + yawOffset);
                double pitchRad = Math.toRadians(-player.getXRot() + pitchOffset);
                Vec3 seekerDir = new Vec3(
                        Math.cos(yawRad) * Math.cos(pitchRad),
                        Math.sin(pitchRad),
                        Math.sin(yawRad) * Math.cos(pitchRad)
                ).normalize();
                
                // Spawn particle seeker projectile path
                spawnParticleSeeker(level, playerPos, seekerDir, 20.0, r, g, b);
            }
        } else {
            // Target enemies with homing particle seekers
            for (int i = 0; i < chargeCount; i++) {
                Entity target = entities.get(i % entities.size());
                Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
                
                // Calculate curved path to target
                spawnHomingSeekerPath(player, level, playerPos, targetPos, target, damage, r, g, b);
            }
        }
    }
    
    /**
     * Spawn a particle-based seeker projectile traveling in a direction
     */
    private static void spawnParticleSeeker(ServerLevel level, Vec3 start, Vec3 direction, double range,
            float r, float g, float b) {
        Vec3 normalizedDir = direction.normalize();
        Vec3 perpVec1 = getPerpendicular(normalizedDir);
        Vec3 perpVec2 = normalizedDir.cross(perpVec1).normalize();
        
        // Seeker travels along the path
        for (int step = 0; step < 40; step++) {
            double progress = (double) step / 40.0;
            double distance = progress * range;
            Vec3 pos = start.add(normalizedDir.scale(distance));
            
            // ===== SEEKER CORE =====
            // Central bright orb
            level.sendParticles(createDustParticle(0.9f, 0.6f, 1.0f, 0.8f),
                    pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0);
            
            // ===== ORBITAL RING =====
            // Spinning particles around the core
            double orbitAngle = step * 0.5;
            for (int orbit = 0; orbit < 6; orbit++) {
                double angle = orbitAngle + (double) orbit / 6 * 2 * Math.PI;
                double orbitRadius = 0.25;
                double ox = Math.cos(angle) * orbitRadius;
                double oy = Math.sin(angle) * orbitRadius;
                Vec3 orbitPos = pos.add(perpVec1.scale(ox)).add(perpVec2.scale(oy));
                
                level.sendParticles(createDustParticle(r, g, b, 0.4f),
                        orbitPos.x, orbitPos.y, orbitPos.z, 1, 0, 0, 0, 0);
            }
            
            // ===== TRAIL =====
            if (step % 3 == 0) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                        pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.01);
            }
            if (step % 5 == 0) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0);
            }
        }
        
        // End burst
        Vec3 endPos = start.add(normalizedDir.scale(range));
        level.sendParticles(createDustParticle(1.0f, 0.7f, 1.0f, 1.0f),
                endPos.x, endPos.y, endPos.z, 8, 0.2, 0.2, 0.2, 0.05);
    }
    
    /**
     * Spawn a homing seeker path that curves toward the target and deals damage
     */
    private static void spawnHomingSeekerPath(ServerPlayer player, ServerLevel level, Vec3 start, Vec3 targetPos,
            Entity target, float damage, float r, float g, float b) {
        // Calculate curved path using quadratic bezier
        Vec3 toTarget = targetPos.subtract(start);
        double distance = toTarget.length();
        
        // Control point - offset to create arc
        Vec3 midPoint = start.add(toTarget.scale(0.5));
        double arcHeight = distance * 0.3 + 1.5;
        double arcSide = (RANDOM.nextDouble() - 0.5) * distance * 0.4;
        
        // Get perpendicular vectors for arc
        Vec3 forward = toTarget.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 side = forward.cross(up).normalize();
        
        Vec3 controlPoint = midPoint.add(0, arcHeight, 0).add(side.scale(arcSide));
        
        // Draw bezier curve path
        Vec3 perpVec1 = getPerpendicular(forward);
        Vec3 perpVec2 = forward.cross(perpVec1).normalize();
        
        for (int step = 0; step < 50; step++) {
            double t = (double) step / 50.0;
            
            // Quadratic bezier: B(t) = (1-t)²P0 + 2(1-t)tP1 + t²P2
            double oneMinusT = 1 - t;
            Vec3 pos = start.scale(oneMinusT * oneMinusT)
                    .add(controlPoint.scale(2 * oneMinusT * t))
                    .add(targetPos.scale(t * t));
            
            // ===== SEEKER CORE =====
            float coreSize = 0.7f + (float) Math.sin(t * Math.PI) * 0.3f; // Pulse
            level.sendParticles(createDustParticle(0.95f, 0.6f, 1.0f, coreSize),
                    pos.x, pos.y, pos.z, 2, 0.04, 0.04, 0.04, 0);
            
            // ===== SPINNING ORBITAL PARTICLES =====
            double orbitAngle = step * 0.6;
            for (int orbit = 0; orbit < 8; orbit++) {
                double angle = orbitAngle + (double) orbit / 8 * 2 * Math.PI;
                double orbitRadius = 0.2 + Math.sin(t * Math.PI * 2) * 0.1;
                
                // Calculate orbit position relative to current path direction
                Vec3 tangent = calculateBezierTangent(start, controlPoint, targetPos, t);
                Vec3 orbitPerp1 = getPerpendicular(tangent);
                Vec3 orbitPerp2 = tangent.cross(orbitPerp1).normalize();
                
                double ox = Math.cos(angle) * orbitRadius;
                double oy = Math.sin(angle) * orbitRadius;
                Vec3 orbitPos = pos.add(orbitPerp1.scale(ox)).add(orbitPerp2.scale(oy));
                
                level.sendParticles(createDustParticle(r, g, b, 0.35f),
                        orbitPos.x, orbitPos.y, orbitPos.z, 1, 0, 0, 0, 0);
            }
            
            // ===== TRAIL PARTICLES =====
            if (step % 2 == 0) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                        pos.x, pos.y, pos.z, 1, 0.03, 0.03, 0.03, 0.005);
            }
            if (step % 4 == 0) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        pos.x, pos.y, pos.z, 1, 0.01, 0.01, 0.01, 0);
            }
        }
        
        // ===== IMPACT EFFECT =====
        // Burst at target
        level.sendParticles(createDustParticle(1.0f, 0.7f, 1.0f, 1.0f),
                targetPos.x, targetPos.y, targetPos.z, 12, 0.3, 0.3, 0.3, 0.08);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                targetPos.x, targetPos.y, targetPos.z, 20, 0.4, 0.4, 0.4, 0.1);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                targetPos.x, targetPos.y, targetPos.z, 1, 0, 0, 0, 0);
        
        // Deal damage to target
        if (target instanceof LivingEntity living) {
            living.hurt(player.damageSources().playerAttack(player), damage);
        }
    }
    
    /**
     * Calculate tangent vector for bezier curve at parameter t
     */
    private static Vec3 calculateBezierTangent(Vec3 p0, Vec3 p1, Vec3 p2, double t) {
        // Derivative of quadratic bezier: B'(t) = 2(1-t)(P1-P0) + 2t(P2-P1)
        double oneMinusT = 1 - t;
        Vec3 tangent = p1.subtract(p0).scale(2 * oneMinusT)
                .add(p2.subtract(p1).scale(2 * t));
        return tangent.normalize();
    }
}