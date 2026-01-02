package net.frostimpact.rpgclasses_v2.networking;

import net.frostimpact.rpgclasses_v2.networking.packet.PacketAllocateSkillPoint;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketAllocateStatPoint;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSelectClass;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncCooldowns;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncMana;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncRage;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncRPGData;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncSeekerCharges;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncStats;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketUseAbility;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketResetStats;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketMarksmanFocusMode;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncSkillTreeData;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketResetSkillTree;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketRequestSkillTreeData;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatModifier;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import net.frostimpact.rpgclasses_v2.rpgclass.AbilityUtils;
import net.frostimpact.rpgclasses_v2.rpgclass.ClassRegistry;
import net.frostimpact.rpgclasses_v2.rpgclass.RPGClass;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.UUID;

public class ModMessages {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModMessages.class);
    private static final Random RANDOM = new Random();
    private static final AtomicInteger whirlwindCounter = new AtomicInteger(0);
    
    // Ability constants
    private static final int ULTIMATE_ARROW_MULTIPLIER = 3;
    private static final double SEEKER_SEARCH_RANGE = 20.0;
    private static final int RAIN_OF_ARROWS_DURATION_TICKS = 120; // 6 seconds at 20 ticks/second
    private static final int SEEKER_PROJECTILE_SPEED_TICKS = 3; // Update every 3 ticks for medium speed
    private static final double SEEKER_NO_TARGET_RANGE = 20.0; // Distance seekers travel when no target found
    private static final double SEEKER_STRAIGHT_FLIGHT_RANGE = 30.0; // Distance seekers fly straight when target dies
    
    // Marksman ability constants
    private static final float SNIPE_PROJECTILE_SPEED = 0.6f; // Slow moving for dramatic effect
    private static final int SNIPE_MAX_FLIGHT_TICKS = 200; // 10 seconds max flight time
    private static final float HEADSHOT_BASE_CHARGE_MULTIPLIER = 1.5f; // 50% bonus damage (charge system not fully implemented)
    private static final int HEADSHOT_MAX_CHARGE_TICKS = 100; // 5 seconds for full 100% bonus
    private static final float MARK_DAMAGE_BONUS = 1.3f; // 30% bonus damage to marked targets
    private static final int MARK_DURATION_TICKS = 140; // 7 seconds
    
    // Berserker ability constants
    private static final int AXE_THROW_DELAY_TICKS = 30; // 1.5 seconds between axe throws
    private static final int AXE_MAX_OUT_TICKS = 40; // 2 seconds before axe returns
    private static final float RAGE_GAIN_PERCENT = 0.05f; // 5% of damage dealt
    private static final float LIFESTEAL_PERCENT = 0.05f; // 5% of damage dealt while enraged
    
    // Active timed effects for Rain of Arrows
    private static final Map<UUID, RainOfArrowsEffect> activeRainEffects = new ConcurrentHashMap<>();
    // Active seeker projectiles (homing missiles)
    private static final List<SeekerProjectile> activeSeekers = new ArrayList<>();
    // Active Marksman Snipe projectiles (slow moving)
    private static final List<SnipeProjectile> activeSnipeProjectiles = new ArrayList<>();
    // Marksman Headshot charging state
    private static final Map<UUID, HeadshotCharge> activeHeadshotCharges = new ConcurrentHashMap<>();
    // Marksman marked enemies (30% more damage)
    private static final Map<UUID, MarkedEnemy> markedEnemies = new ConcurrentHashMap<>();
    // Warrior Whirlwind hit tracking - persists across scheduled tasks
    private static final Map<String, Map<UUID, Integer>> whirlwindHitCounts = new ConcurrentHashMap<>();
    // Berserker Frenzy slash tracking - persists across scheduled tasks
    private static final Map<UUID, Integer> frenzySlashCounts = new ConcurrentHashMap<>();
    // Ravager Heartstopper boss bars
    private static final Map<UUID, ServerBossEvent> heartstopperBossBars = new ConcurrentHashMap<>();
    
    /**
     * Data class for Rain of Arrows timed effect
     */
    public static class RainOfArrowsEffect {
        public final ServerPlayer player;
        public final ServerLevel level;
        public final Vec3 center;
        public final double radius;
        public final float damage;
        public int ticksRemaining;
        
        public RainOfArrowsEffect(ServerPlayer player, ServerLevel level, Vec3 center, double radius, float damage, int duration) {
            this.player = player;
            this.level = level;
            this.center = center;
            this.radius = radius;
            this.damage = damage;
            this.ticksRemaining = duration;
        }
    }
    
    /**
     * Data class for Seeker homing projectiles
     */
    public static class SeekerProjectile {
        public final ServerPlayer owner;
        public final ServerLevel level;
        public Vec3 position;
        public LivingEntity target;
        public final float damage;
        public int ticksAlive;
        public final int maxTicks;
        public final float speed;
        
        public SeekerProjectile(ServerPlayer owner, ServerLevel level, Vec3 startPos, LivingEntity target, float damage) {
            this.owner = owner;
            this.level = level;
            this.position = startPos;
            this.target = target;
            this.damage = damage;
            this.ticksAlive = 0;
            this.maxTicks = 100; // 5 seconds max flight time
            this.speed = 0.8f; // Medium speed
        }
    }
    
    /**
     * Data class for Marksman Snipe projectile (slow moving, high damage)
     */
    public static class SnipeProjectile {
        public final ServerPlayer owner;
        public final ServerLevel level;
        public Vec3 position;
        public final Vec3 direction;
        public final float damage;
        public int ticksAlive;
        public final int maxTicks;
        public final float speed;
        
        public SnipeProjectile(ServerPlayer owner, ServerLevel level, Vec3 startPos, Vec3 direction, float damage) {
            this.owner = owner;
            this.level = level;
            this.position = startPos;
            this.direction = direction.normalize();
            this.damage = damage;
            this.ticksAlive = 0;
            this.maxTicks = SNIPE_MAX_FLIGHT_TICKS;
            this.speed = SNIPE_PROJECTILE_SPEED;
        }
    }
    
    /**
     * Data class for Marksman Headshot charging
     */
    public static class HeadshotCharge {
        public final ServerPlayer owner;
        public final ServerLevel level;
        public LivingEntity target;
        public int chargeTime;
        public final int maxChargeTime;
        public final float baseDamage;
        
        public HeadshotCharge(ServerPlayer owner, ServerLevel level, LivingEntity target, float baseDamage) {
            this.owner = owner;
            this.level = level;
            this.target = target;
            this.chargeTime = 0;
            this.maxChargeTime = HEADSHOT_MAX_CHARGE_TICKS;
            this.baseDamage = baseDamage;
        }
        
        public float getDamageMultiplier() {
            return 1.0f + ((float) chargeTime / maxChargeTime); // 1.0 to 2.0 (100% bonus max)
        }
    }
    
    /**
     * Data class for Marksman marked enemies
     */
    public static class MarkedEnemy {
        public final UUID ownerUUID;
        public final LivingEntity target;
        public int ticksRemaining;
        
        public MarkedEnemy(UUID ownerUUID, LivingEntity target, int duration) {
            this.ownerUUID = ownerUUID;
            this.target = target;
            this.ticksRemaining = duration;
        }
    }
    
    /**
     * Called every server tick to update timed effects
     */
    public static void tickTimedEffects() {
        // Update Rain of Arrows effects
        Iterator<Map.Entry<UUID, RainOfArrowsEffect>> rainIterator = activeRainEffects.entrySet().iterator();
        while (rainIterator.hasNext()) {
            Map.Entry<UUID, RainOfArrowsEffect> entry = rainIterator.next();
            RainOfArrowsEffect effect = entry.getValue();
            effect.ticksRemaining--;
            
            // Every 4 ticks, spawn particle arrows and deal damage
            if (effect.ticksRemaining % 4 == 0) {
                spawnRainOfArrowsTickEffect(effect);
            }
            
            // Every 20 ticks (1 second), deal damage
            if (effect.ticksRemaining % 20 == 0) {
                dealRainOfArrowsDamage(effect);
            }
            
            if (effect.ticksRemaining <= 0) {
                // Spawn final impact effect
                spawnRainOfArrowsFinalEffect(effect);
                rainIterator.remove();
            }
        }
        
        // Update Seeker projectiles
        Iterator<SeekerProjectile> seekerIterator = activeSeekers.iterator();
        while (seekerIterator.hasNext()) {
            SeekerProjectile seeker = seekerIterator.next();
            seeker.ticksAlive++;
            
            // Update seeker position and check for hit
            if (!updateSeekerProjectile(seeker)) {
                seekerIterator.remove();
            }
        }
        
        // Update Marksman Snipe projectiles
        Iterator<SnipeProjectile> snipeIterator = activeSnipeProjectiles.iterator();
        while (snipeIterator.hasNext()) {
            SnipeProjectile snipe = snipeIterator.next();
            snipe.ticksAlive++;
            
            // Update snipe position and check for hit
            if (!updateSnipeProjectile(snipe)) {
                snipeIterator.remove();
            }
        }
        
        // Update Marksman Headshot charges
        Iterator<Map.Entry<UUID, HeadshotCharge>> headshotIterator = activeHeadshotCharges.entrySet().iterator();
        while (headshotIterator.hasNext()) {
            Map.Entry<UUID, HeadshotCharge> entry = headshotIterator.next();
            HeadshotCharge charge = entry.getValue();
            
            // Update charge state
            if (!updateHeadshotCharge(charge)) {
                headshotIterator.remove();
            }
        }
        
        // Update Marksman marked enemies
        Iterator<Map.Entry<UUID, MarkedEnemy>> markedIterator = markedEnemies.entrySet().iterator();
        while (markedIterator.hasNext()) {
            Map.Entry<UUID, MarkedEnemy> entry = markedIterator.next();
            MarkedEnemy marked = entry.getValue();
            marked.ticksRemaining--;
            
            // Spawn mark visual every 10 ticks
            if (marked.ticksRemaining % 10 == 0 && marked.target.isAlive()) {
                spawnMarkVisual(marked);
            }
            
            if (marked.ticksRemaining <= 0 || !marked.target.isAlive()) {
                markedIterator.remove();
            }
        }
        
        // Update Ranger Large Piercing Arrows
        Iterator<LargePiercingArrow> piercingIterator = activeLargePiercingArrows.iterator();
        while (piercingIterator.hasNext()) {
            LargePiercingArrow arrow = piercingIterator.next();
            arrow.ticksAlive++;
            
            // Update arrow position and check for hits
            if (!updateLargePiercingArrow(arrow)) {
                piercingIterator.remove();
            }
        }
        
        // Update Heavy Cleave projectiles
        Iterator<HeavyCleaveProjectile> cleaveIterator = activeHeavyCleaveProjectiles.iterator();
        while (cleaveIterator.hasNext()) {
            HeavyCleaveProjectile proj = cleaveIterator.next();
            proj.ticksAlive++;
            
            if (!updateHeavyCleaveProjectile(proj)) {
                cleaveIterator.remove();
            }
        }
        
        // Update Rupture projectiles
        Iterator<RuptureProjectile> ruptureIterator = activeRuptureProjectiles.iterator();
        while (ruptureIterator.hasNext()) {
            RuptureProjectile proj = ruptureIterator.next();
            proj.ticksAlive++;
            
            if (!updateRuptureProjectile(proj)) {
                ruptureIterator.remove();
            }
        }
    }
    
    /**
     * Get the active rain effects map for external access
     */
    public static Map<UUID, RainOfArrowsEffect> getActiveRainEffects() {
        return activeRainEffects;
    }
    
    /**
     * Get the active seekers list for external access
     */
    public static List<SeekerProjectile> getActiveSeekers() {
        return activeSeekers;
    }
    
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
        
        registrar.playToClient(
                PacketSyncRage.TYPE,
                PacketSyncRage.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() != null) {
                            var rpgData = context.player().getData(ModAttachments.PLAYER_RPG);
                            rpgData.setRage(packet.rage());
                            rpgData.setEnraged(packet.enraged());
                            rpgData.setEnhancedEnraged(packet.enhancedEnraged());
                            rpgData.setExhausted(packet.exhausted());
                            rpgData.setAxeThrowCharges(packet.axeThrowCharges());
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
                            
                            // Initialize Berserker-specific data
                            if (classId.equalsIgnoreCase("berserker")) {
                                rpgData.setAxeThrowCharges(2); // Start with 2 charges
                                rpgData.setRage(0); // Start with 0 rage
                            }
                            
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
                            
                            // Validate the skill tree and node exist
                            var optionalTree = net.frostimpact.rpgclasses_v2.skilltree.SkillTreeRegistry.getSkillTree(packet.skillTreeId());
                            if (optionalTree.isEmpty()) {
                                LOGGER.warn("Player {} tried to allocate to invalid tree: {}", 
                                        serverPlayer.getName().getString(), packet.skillTreeId());
                                return;
                            }
                            
                            var skillTree = optionalTree.get();
                            var optionalNode = skillTree.getNode(packet.skillNodeId());
                            if (optionalNode.isEmpty()) {
                                LOGGER.warn("Player {} tried to allocate to invalid node: {} in tree {}", 
                                        serverPlayer.getName().getString(), packet.skillNodeId(), packet.skillTreeId());
                                return;
                            }
                            
                            var node = optionalNode.get();
                            
                            // Check if player has available skill points
                            if (!rpgData.useSkillPoint()) {
                                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                        "§cNo skill points available!"), true);
                                return;
                            }
                            
                            // Get current allocated level
                            int currentLevel = rpgData.getSkillNodeLevel(packet.skillTreeId(), packet.skillNodeId());
                            
                            // Check if already maxed
                            if (currentLevel >= node.getMaxLevel()) {
                                // Refund the point
                                rpgData.addSkillPoints(1);
                                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                        "§cSkill already maxed out!"), true);
                                return;
                            }
                            
                            // Check player level requirement
                            if (rpgData.getClassLevel() < node.getRequiredLevel()) {
                                // Refund the point
                                rpgData.addSkillPoints(1);
                                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                        "§cRequires class level " + node.getRequiredLevel() + "!"), true);
                                return;
                            }
                            
                            // Check prerequisites
                            for (String reqId : node.getRequirements()) {
                                if (rpgData.getSkillNodeLevel(packet.skillTreeId(), reqId) < 1) {
                                    // Refund the point
                                    rpgData.addSkillPoints(1);
                                    serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                            "§cPrerequisites not met!"), true);
                                    return;
                                }
                            }
                            
                            // All checks passed - allocate the point
                            rpgData.incrementSkillNodeLevel(packet.skillTreeId(), packet.skillNodeId(), node.getMaxLevel());
                            
                            LOGGER.info("Player {} allocated skill point to {} in tree {} (now level {}/{})", 
                                    serverPlayer.getName().getString(), 
                                    packet.skillNodeId(), 
                                    packet.skillTreeId(),
                                    currentLevel + 1,
                                    node.getMaxLevel());
                            
                            // Sync back to client
                            sendToPlayer(new PacketSyncRPGData(
                                    rpgData.getCurrentClass(),
                                    rpgData.getLevel(),
                                    rpgData.getClassLevel(),
                                    rpgData.getClassExperience(),
                                    rpgData.getAvailableStatPoints(),
                                    rpgData.getAvailableSkillPoints()
                            ), serverPlayer);
                            
                            // Send updated skill tree allocations
                            sendToPlayer(new PacketSyncSkillTreeData(rpgData.getAllSkillTreeAllocations()), serverPlayer);
                            
                            // TODO: Apply skill node effects/bonuses
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
        
        // Register PacketMarksmanFocusMode - handles Marksman FOCUS mode state from client
        registrar.playToServer(
                PacketMarksmanFocusMode.TYPE,
                PacketMarksmanFocusMode.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer serverPlayer) {
                            var rpgData = serverPlayer.getData(ModAttachments.PLAYER_RPG);
                            
                            // Only allow Marksman class to enter FOCUS mode
                            if (!rpgData.getCurrentClass().equalsIgnoreCase("marksman")) {
                                return;
                            }
                            
                            rpgData.setInFocusMode(packet.inFocusMode());
                            
                            LOGGER.debug("Marksman {} {} FOCUS mode", 
                                    serverPlayer.getName().getString(), 
                                    packet.inFocusMode() ? "entered" : "exited");
                        }
                    });
                }
        );
        
        // Register PacketRequestSkillTreeData - client requests skill tree data sync
        registrar.playToServer(
                PacketRequestSkillTreeData.TYPE,
                PacketRequestSkillTreeData.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer serverPlayer) {
                            var rpgData = serverPlayer.getData(ModAttachments.PLAYER_RPG);
                            sendToPlayer(new PacketSyncSkillTreeData(rpgData.getAllSkillTreeAllocations()), serverPlayer);
                            LOGGER.debug("Synced skill tree data to player {}", serverPlayer.getName().getString());
                        }
                    });
                }
        );
        
        // Register PacketResetSkillTree - handles skill tree reset from client
        registrar.playToServer(
                PacketResetSkillTree.TYPE,
                PacketResetSkillTree.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer serverPlayer) {
                            var rpgData = serverPlayer.getData(ModAttachments.PLAYER_RPG);
                            
                            // Reset the specified tree and refund points
                            int refundedPoints = rpgData.resetSkillTree(packet.skillTreeId());
                            rpgData.addSkillPoints(refundedPoints);
                            
                            LOGGER.info("Player {} reset skill tree {}, refunded {} points", 
                                    serverPlayer.getName().getString(), packet.skillTreeId(), refundedPoints);
                            
                            // Sync back to client
                            sendToPlayer(new PacketSyncRPGData(
                                    rpgData.getCurrentClass(),
                                    rpgData.getLevel(),
                                    rpgData.getClassLevel(),
                                    rpgData.getClassExperience(),
                                    rpgData.getAvailableStatPoints(),
                                    rpgData.getAvailableSkillPoints()
                            ), serverPlayer);
                            
                            // Send updated skill tree allocations
                            sendToPlayer(new PacketSyncSkillTreeData(rpgData.getAllSkillTreeAllocations()), serverPlayer);
                            
                            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                    "§aSkill tree reset! Refunded " + refundedPoints + " points."), true);
                        }
                    });
                }
        );
        
        // Register PacketSyncSkillTreeData - syncs skill tree data from server to client
        registrar.playToClient(
                PacketSyncSkillTreeData.TYPE,
                PacketSyncSkillTreeData.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        // This is handled on client side by SkillTreeScreen
                        LOGGER.debug("Received skill tree data sync");
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
    
    public static void sendToPlayer(PacketSyncRage packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }
    
    public static void sendToPlayer(PacketSyncSkillTreeData packet, ServerPlayer player) {
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
    
    public static void sendToServer(PacketMarksmanFocusMode packet) {
        PacketDistributor.sendToServer(packet);
    }
    
    public static void sendToServer(PacketRequestSkillTreeData packet) {
        PacketDistributor.sendToServer(packet);
    }
    
    public static void sendToServer(PacketResetSkillTree packet) {
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
        
        // Special handling for Marksman Snipe (slot 1) in FOCUS mode - reduced cost and cooldown
        if (currentClass.equalsIgnoreCase("marksman") && abilitySlot == 1 && rpgData.isInFocusMode()) {
            manaCost = 5; // Reduced from 10 to 5
            baseCooldownTicks = 30; // Reduced from 60 (3s) to 30 (1.5s)
        }
        
        // Special handling for Hawkeye Seekers (slot 4) - mana cost is 5 * seeker charges
        if (currentClass.equalsIgnoreCase("hawkeye") && abilitySlot == 4) {
            int seekerCharges = rpgData.getSeekerCharges();
            if (seekerCharges == 0) {
                player.displayClientMessage(Component.literal("§eNo Seeker charges! Gain charges while airborne."), true);
                return;
            }
            manaCost = 5 * seekerCharges;
        }
        
        // Special handling for Berserker Axe Throw (slot 1) - uses charge system instead of cooldown
        boolean isBerserkerAxeThrow = currentClass.equalsIgnoreCase("berserker") && abilitySlot == 1;
        
        // Check cooldown (skip for Berserker Axe Throw - uses charge system)
        if (!isBerserkerAxeThrow) {
            int cooldown = rpgData.getAbilityCooldown(abilityId);
            if (cooldown > 0) {
                player.displayClientMessage(
                        Component.literal("§e" + abilityName + " §7is on cooldown (§c" + (cooldown / 20) + "s§7)"), true);
                return;
            }
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
        
        // Use mana and set cooldown (skip cooldown for Berserker Axe Throw - uses charge system)
        rpgData.useMana(manaCost);
        if (!isBerserkerAxeThrow) {
            rpgData.setAbilityCooldown(abilityId, adjustedCooldownTicks);
        }
        
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
                    case 1 -> { // Heavy Cleave - 120° arc attack
                        boolean isSneaking = player.isCrouching();
                        if (isSneaking) {
                            // Shift-cast: Projectile slash dealing 50% damage
                            Vec3 lookVec = player.getLookAngle();
                            Vec3 startPos = playerPos.add(0, player.getEyeHeight(), 0);
                            float projectileDamage = (float) (player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 0.5);
                            spawnHeavyCleaveProjectile(player, level, startPos, lookVec, projectileDamage + damageBonus * 0.5f, player.getYRot());
                            // Red projectile launch effect
                            spawnDustParticlesBurst(level, startPos, 1.5, 1.0f, 0.0f, 0.0f, 15);
                            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                                    startPos.x, startPos.y, startPos.z, 10, 0.3, 0.3, 0.3, 0.1);
                        } else {
                            // Normal: 120° arc dealing 110% damage
                            float baseDamage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                            dealDamageInArc(player, (baseDamage * 1.1f) + damageBonus * 1.1f, 5.0, 120.0);
                            // Red arc swing effect
                            spawnHeavyCleaveArcEffect(level, playerPos.add(0, 1, 0), player.getYRot(), 5.0);
                        }
                    }
                    case 2 -> { // Battle Cry - buff for 6 seconds
                        // 25% damage increase (Strength I = 3 damage = ~25% for default player)
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0));
                        // 15% attack speed increase (Haste I)
                        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 0));
                        // Red particle spheres radiating from player
                        spawnBattleCryEffect(level, playerPos.add(0, 1, 0));
                    }
                    case 3 -> { // Whirlwind - 30% damage per hit, max 8 hits over 3 seconds
                        float baseDamage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                        // Increased to 8 hits over 3 seconds for an actual whirlwind feel
                        dealWhirlwindDamage(player, level, (baseDamage * 0.3f) + damageBonus * 0.3f, 4.0, 8);
                        // Red spinning effect with continuous animation - 3 seconds
                        spawnWhirlwindEffect(level, playerPos.add(0, 1, 0), 4.0, player.getYRot());
                    }
                    case 4 -> { // Leap - after 3s in air, target location within 10 blocks, 200% damage, 2s slow
                        Vec3 lookVec = player.getLookAngle();
                        Vec3 horizontalLook = new Vec3(lookVec.x, 0, lookVec.z).normalize();
                        
                        // Launch player upward for 3 second air time
                        double upwardVelocity = 1.2; // Strong upward launch
                        double forwardVelocity = 0.3; // Minimal forward movement initially
                        
                        Vec3 velocity = new Vec3(
                                horizontalLook.x * forwardVelocity,
                                upwardVelocity,
                                horizontalLook.z * forwardVelocity
                        );
                        
                        player.setDeltaMovement(velocity);
                        player.hurtMarked = true;
                        
                        // Mark player for leap landing effect
                        player.getPersistentData().putBoolean("warrior_leaping", true);
                        player.getPersistentData().putLong("warrior_leap_time", level.getGameTime());
                        
                        // Store damage for landing (200% of player's damage)
                        float baseDamage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                        player.getPersistentData().putFloat("warrior_leap_damage", baseDamage * 2.0f + damageBonus * 2.0f);
                        
                        // Store look direction for targeting during air time
                        player.getPersistentData().putFloat("warrior_leap_yaw", player.getYRot());
                        player.getPersistentData().putFloat("warrior_leap_pitch", player.getXRot());
                        
                        // Apply slow falling during air time (3 seconds = 60 ticks)
                        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0));
                        
                        // Launch particles with lighter red
                        spawnLeapLaunchEffect(level, playerPos);
                        
                        player.displayClientMessage(Component.literal("§c§lLEAP! §7Target location with your view..."), true);
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
                    case 1 -> { // Piercing Shot - LARGE slow-moving arrow projectile (3x size) with dust circles
                        Vec3 lookVec = player.getLookAngle();
                        Vec3 startPos = playerPos.add(0, player.getEyeHeight(), 0);
                        float damage = 12.0f + damageBonus * 2.0f;
                        
                        // Create large piercing arrow projectile
                        spawnLargePiercingArrowProjectile(player, level, startPos, lookVec, damage);
                        
                        // Launch effect - green ranger theme
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                                startPos.x, startPos.y, startPos.z, 2, 0.3, 0.3, 0.3, 0);
                        spawnDustParticlesBurst(level, startPos, 2.0, 0.2f, 0.85f, 0.3f, 15);
                    }
                    case 2 -> { // Spread Shot - Fixed direction based on player look angle
                        Vec3 lookVec = player.getLookAngle();
                        Vec3 startPos = playerPos.add(0, player.getEyeHeight(), 0);
                        float yRot = player.getYRot();
                        float damage = 3.5f + damageBonus * 0.75f;
                        
                        // Spawn multiple arrows in a fan spread pattern FORWARD in look direction
                        spawnMultiShotArrowsFixed(player, level, startPos, lookVec, yRot, damage);
                        
                        // Dramatic launch effect
                        spawnMultiShotLaunchEffect(level, startPos, lookVec);
                    }
                    case 3 -> { // Escape - launch player opposite of look direction
                        Vec3 lookVec = player.getLookAngle();
                        // Launch player in the opposite direction with MORE force
                        Vec3 escapeVec = new Vec3(-lookVec.x * 2.5, 0.7, -lookVec.z * 2.5);
                        player.setDeltaMovement(player.getDeltaMovement().add(escapeVec));
                        player.hurtMarked = true; // Force velocity sync
                        // Enhanced escape visual - particles at launch point and trail
                        spawnEscapeEffect(level, playerPos, lookVec.reverse());
                    }
                    case 4 -> { // Arrow Rain - Target location based on where player is looking (raycast)
                        float damage = 4.0f + damageBonus * 1.0f;
                        double rainRadius = 6.0;
                        
                        // Find target position where player is looking
                        Vec3 targetPos = getPlayerLookTargetPosition(player, 50.0);
                        if (targetPos == null) {
                            // Fallback to player position if no valid target found
                            targetPos = playerPos;
                        }
                        
                        // Register timed effect at target location
                        RainOfArrowsEffect effect = new RainOfArrowsEffect(
                                player, level, targetPos, rainRadius, damage, RAIN_OF_ARROWS_DURATION_TICKS
                        );
                        activeRainEffects.put(player.getUUID(), effect);
                        
                        // Initial activation effect with DEFINED CIRCLE (not clouds!)
                        spawnRainOfArrowsActivationEffect(level, targetPos, rainRadius);
                        
                        // Apply slowdown to enemies in the zone

                    }
                }
            }
            case "hawkeye" -> {
                var rpgData = player.getData(ModAttachments.PLAYER_RPG);
                switch (slot) {
                    case 1 -> { // Vault - launch forward and throw a turtle scute entity (MOVED from slot 3)
                        // Launch player forward in look direction with MORE force
                        Vec3 lookVec = player.getLookAngle();
                        Vec3 launchVec = new Vec3(lookVec.x * 2.0, 0.5, lookVec.z * 2.0);
                        player.setDeltaMovement(player.getDeltaMovement().add(launchVec));
                        player.hurtMarked = true;
                        float projectileDamage = 6.0f + damageBonus * 0.7f;
                        // Spawn turtle scute projectile entity that deals damage
                        spawnVaultTurtleScuteProjectile(player, level, lookVec, projectileDamage);
                        // Add a seeker charge
                        rpgData.addSeekerCharge();
                        // Sync seeker charges to client
                        sendToPlayer(new PacketSyncSeekerCharges(rpgData.getSeekerCharges()), player);
                        // Enhanced vault visual - directional particles
                        spawnVaultEffect(level, playerPos, lookVec);
                        // Additional sweep attack visuals
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                                playerPos.x + lookVec.x, playerPos.y + 1, playerPos.z + lookVec.z, 3, 0.3, 0.2, 0.3, 0);
                    }
                    case 2 -> { // Updraft - ENHANCED launch with more dramatic effects
                        // Launch player upward with STRONGER force
                        Vec3 currentVelocity = player.getDeltaMovement();
                        player.setDeltaMovement(currentVelocity.x * 0.5, 1.5, currentVelocity.z * 0.5);
                        player.hurtMarked = true; // Force velocity sync
                        // Add longer slow falling after launch
                        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0));
                        // Add a seeker charge (aerial affinity)
                        rpgData.addSeekerCharge();
                        // Sync seeker charges to client
                        sendToPlayer(new PacketSyncSeekerCharges(rpgData.getSeekerCharges()), player);
                        // Enhanced updraft visual - more dramatic particles going up
                        spawnUpdraftEffect(level, playerPos);
                        // Additional flash
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                                playerPos.x, playerPos.y, playerPos.z, 2, 0, 0, 0, 0);
                    }
                    case 3 -> { // (Reserved slot - GLIDE is now a passive)
                        player.displayClientMessage(Component.literal("§eGLIDE is now a passive ability - automatically activates when airborne!"), true);
                    }
                    case 4 -> { // Seekers - MOVING PROJECTILES that home on targets (not hitscan)
                        int charges = rpgData.consumeSeekerCharges();
                        if (charges > 0) {
                            float damage = 6.0f + damageBonus * 0.6f;
                            // Find enemies in player's vision and spawn MOVING seeker projectiles
                            spawnMovingSeekerProjectiles(player, level, charges, damage);
                            // Enhanced seeker launch visual
                            spawnSeekerLaunchEffect(level, playerPos, charges);
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
            case "marksman" -> {
                var rpgData = player.getData(ModAttachments.PLAYER_RPG);
                switch (slot) {
                case 1 -> { // Snipe - HITSCAN (instant bullet travel) with subtle dust gradient targeting line
                        Vec3 lookVec = player.getLookAngle();
                        Vec3 startPos = playerPos.add(0, player.getEyeHeight(), 0);
                        float baseDamage = 20.0f + damageBonus * 3.0f; // High damage
                        
                        // Find target using raycast (hitscan - instant damage)
                        LivingEntity target = findTargetInSight(player, 80.0); // Long range
                        
                        if (target != null) {
                            // Apply mark bonus if target is marked
                            float finalDamage = baseDamage;
                            if (isMarkedEnemy(target)) {
                                finalDamage *= MARK_DAMAGE_BONUS;
                            }
                            
                            // Instant damage (hitscan)
                            target.hurt(player.damageSources().playerAttack(player), finalDamage);
                            
                            // Spawn subtle targeting line with dust gradient (no flash, no lava)
                            spawnSnipeHitscanEffect(level, startPos, target.position().add(0, target.getBbHeight() * 0.5, 0));
                            
                            player.displayClientMessage(Component.literal("§c§l⌖ SNIPE HIT! §r§7(" + String.format("%.1f", finalDamage) + " damage)"), true);
                        } else {
                            // No target found - show targeting line in look direction
                            Vec3 endPos = startPos.add(lookVec.scale(80.0));
                            spawnSnipeHitscanEffect(level, startPos, endPos);
                            player.displayClientMessage(Component.literal("§eSnipe missed - no target in sight!"), true);
                        }
                    }
                    case 2 -> { // Mark - mark the closest enemy in line of sight, deal 30% more damage, 7s duration
                        LivingEntity target = findTargetInSight(player, 50.0);
                        if (target != null) {
                            // Create mark effect using constant duration
                            MarkedEnemy mark = new MarkedEnemy(player.getUUID(), target, MARK_DURATION_TICKS);
                            markedEnemies.put(target.getUUID(), mark);
                            
                            // Apply visual effects
                            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, MARK_DURATION_TICKS, 0));
                            
                            // Spawn mark visual
                            spawnMarkAppliedEffect(level, target.position(), target);
                            
                            player.displayClientMessage(Component.literal("§c§l✦ MARKED! §r§7(30% bonus damage, 7s)"), true);
                        } else {
                            player.displayClientMessage(Component.literal("§eNo valid target in sight!"), true);
                        }
                    }
                    case 3 -> { // Grapple Hook - throw hook and launch toward that direction
                        Vec3 lookVec = player.getLookAngle();
                        
                        // Find grapple point (either a block or entity in range)
                        double grappleRange = 25.0;
                        Vec3 grappleTarget = findGrapplePoint(player, lookVec, grappleRange);
                        
                        if (grappleTarget != null) {
                            // Calculate launch direction toward grapple point
                            Vec3 launchDir = grappleTarget.subtract(playerPos).normalize();
                            double distance = playerPos.distanceTo(grappleTarget);
                            double launchStrength = Math.min(distance * 0.12, 3.5); // Scale with distance
                            
                            // Launch player toward grapple point
                            Vec3 launchVec = new Vec3(launchDir.x * launchStrength, 
                                                       Math.max(launchDir.y * launchStrength, 0.4), 
                                                       launchDir.z * launchStrength);
                            player.setDeltaMovement(launchVec);
                            player.hurtMarked = true;
                            
                            // Spawn grapple visual effect - rope/chain particles
                            spawnGrappleEffect(level, playerPos, grappleTarget);
                            
                            player.displayClientMessage(Component.literal("§a§l↗ GRAPPLE!"), true);
                        } else {
                            player.displayClientMessage(Component.literal("§eNo grapple point in range!"), true);
                        }
                    }
                    case 4 -> { // Headshot - COMPLETE REWORK with lock-on, rooting, charging, and cooldown reset on kill
                        // Check if already charging - if so, this is a release/fire
                        HeadshotCharge existingCharge = activeHeadshotCharges.get(player.getUUID());
                        
                        if (existingCharge != null) {
                            // RELEASE/FIRE - player pressed ability again
                            LivingEntity target = existingCharge.target;
                            
                            if (target != null && target.isAlive()) {
                                float baseDamage = existingCharge.baseDamage;
                                float damageMultiplier = existingCharge.getDamageMultiplier();
                                
                                // Check for marked enemy bonus
                                float markBonus = isMarkedEnemy(target) ? MARK_DAMAGE_BONUS : 1.0f;
                                
                                // Apply damage with charge multiplier
                                float finalDamage = baseDamage * markBonus * damageMultiplier;
                                
                                // Deal hitscan damage
                                target.hurt(player.damageSources().playerAttack(player), finalDamage);
                                
                                // Check if target was killed
                                boolean targetKilled = !target.isAlive() || target.getHealth() <= 0;
                                
                                // Spawn headshot hitscan visual
                                Vec3 startPos = playerPos.add(0, player.getEyeHeight(), 0);
                                spawnHeadshotEffect(level, startPos, target.position().add(0, target.getBbHeight() * 0.8, 0));
                                
                                if (targetKilled) {
                                    // Reset all other cooldowns on kill
                                    String ability1Id = "marksman_ability_1";
                                    String ability2Id = "marksman_ability_2";
                                    String ability3Id = "marksman_ability_3";
                                    rpgData.setAbilityCooldown(ability1Id, 0);
                                    rpgData.setAbilityCooldown(ability2Id, 0);
                                    rpgData.setAbilityCooldown(ability3Id, 0);
                                    
                                    // Speed buff on kill
                                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
                                    
                                    // Sync cooldowns to client
                                    sendToPlayer(new PacketSyncCooldowns(rpgData.getAllCooldowns()), player);
                                    
                                    player.displayClientMessage(Component.literal("§c§l☠ HEADSHOT KILL! §r§a(All cooldowns reset! +" + String.format("%.0f", (damageMultiplier - 1.0f) * 100) + "% damage)"), true);
                                } else {
                                    player.displayClientMessage(Component.literal("§c§l☠ HEADSHOT! §r§7(+" + String.format("%.0f", (damageMultiplier - 1.0f) * 100) + "% damage: " + String.format("%.1f", finalDamage) + ")"), true);
                                }
                            } else {
                                player.displayClientMessage(Component.literal("§eTarget lost!"), true);
                            }
                            
                            // Remove charge state and un-root player
                            activeHeadshotCharges.remove(player.getUUID());
                            player.setDeltaMovement(player.getDeltaMovement()); // Restore movement
                            
                        } else {
                            // START CHARGING - Find and lock onto nearest enemy
                            LivingEntity target = findNearestEnemy(player, 60.0);
                            
                            if (target != null) {
                                float baseDamage = 15.0f + damageBonus * 2.0f;
                                
                                // Start charging state
                                HeadshotCharge charge = new HeadshotCharge(player, level, target, baseDamage);
                                activeHeadshotCharges.put(player.getUUID(), charge);
                                
                                // Root player (set movement to zero and apply slowness)
                                player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
                                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 255, false, false));
                                
                                player.displayClientMessage(Component.literal("§c§l⌖ CHARGING HEADSHOT... §r§7(Press again to fire)"), true);
                            } else {
                                player.displayClientMessage(Component.literal("§eNo valid target for headshot!"), true);
                            }
                        }
                    }
                }
            }
            case "beastmaster" -> {
                switch (slot) {
                    case 1 -> { // Wolf Pack - SUMMON ACTUAL FRIENDLY WOLVES that attack enemies!
                        // Summon 3 tamed wolves that attack nearby enemies
                        int wolvesSummoned = summonFriendlyWolves(player, level, playerPos, 3);
                        
                        // Epic wolf summoning visual effect
                        spawnWolfSummonEffect(level, playerPos);
                        
                        if (wolvesSummoned > 0) {
                            player.displayClientMessage(Component.literal("§6§l" + wolvesSummoned + " WOLVES SUMMONED!"), true);
                        } else {
                            player.displayClientMessage(Component.literal("§6Wolf spirits guide you!"), true);
                        }
                    }
                    case 2 -> { // Bear Companion - SUMMON A FRIENDLY BEAR (iron golem reskinned)
                        // Summon a strong bear companion
                        boolean bearSummoned = summonFriendlyBear(player, level, playerPos);
                        
                        // Also give player defensive buffs
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
                        
                        // Epic bear summoning effect
                        spawnBearSummonEffect(level, playerPos);
                        
                        if (bearSummoned) {
                            player.displayClientMessage(Component.literal("§6§lBEAR COMPANION SUMMONED!"), true);
                        } else {
                            player.displayClientMessage(Component.literal("§6Bear spirit empowers you!"), true);
                        }
                    }
                    case 3 -> { // Eagle Companion - SUMMON A PARROT that scouts and marks enemies
                        // Summon a parrot companion
                        boolean eagleSummoned = summonFriendlyEagle(player, level, playerPos);
                        
                        // Grant enhanced vision
                        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
                        // Mark all enemies in large range with glowing
                        applyEffectToNearbyEnemies(player, MobEffects.GLOWING, 300, 0, 25.0);
                        
                        // Epic eagle summoning effect
                        spawnEagleSummonEffect(level, playerPos);
                        
                        if (eagleSummoned) {
                            player.displayClientMessage(Component.literal("§b§lEAGLE COMPANION SUMMONED!"), true);
                        } else {
                            player.displayClientMessage(Component.literal("§bEagle spirit grants vision!"), true);
                        }
                    }
                    case 4 -> { // Beast Stampede - 3 SYNCHRONIZED LINES of beasts charging forward!
                        Vec3 lookVec = player.getLookAngle();
                        float damage = 8.0f + damageBonus * 1.5f;
                        Vec3 stampedDir = new Vec3(lookVec.x, 0, lookVec.z).normalize();
                        
                        // Summon 3 lines of beasts (9-12 total beasts in synchronized formation)
                        int beastsSummoned = summonStampedBeasts(player, level, playerPos, stampedDir, 12);
                        
                        // Deal damage in a wide line in front of player
                        dealDamageInWideSwath(player, damage, stampedDir, 15.0, 4.0);
                        
                        // Apply knockback effect
                        applyKnockbackInLine(player, stampedDir, 15.0, 4.0, 1.5);
                        
                        // Epic stampede effect
                        spawnBeastStampedeEffect(level, playerPos, stampedDir);
                        player.displayClientMessage(Component.literal("§c§l⚡ BEAST STAMPEDE ⚡"), true);
                    }
                }
            }
            case "ravager" -> {
                switch (slot) {
                    case 1 -> { // Tearing Hook - stun and pull mechanic
                        Vec3 lookVec = player.getLookAngle();
                        Vec3 startPos = playerPos.add(0, player.getEyeHeight(), 0);
                        boolean isSneaking = player.isCrouching();
                        
                        // Find target enemy in look direction
                        LivingEntity target = findEnemyInLookDirection(player, lookVec, 15.0);
                        
                        if (target != null) {
                            // Stun the enemy
                            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 10));
                            
                            // Chain visual effect to target
                            spawnTearingHookChain(level, startPos, target.position().add(0, target.getBbHeight() * 0.5, 0), false);
                            
                            // Schedule pull after short duration (20 ticks = 1 second)
                            scheduleTearingHookPull(player, target, isSneaking, level.getGameTime() + 20);
                        } else {
                            // No target found - still show hook extending in look direction
                            Vec3 endPos = startPos.add(lookVec.scale(15.0));
                            spawnTearingHookChain(level, startPos, endPos, false);
                            player.displayClientMessage(Component.literal("§cNo target found!"), true);
                        }
                    }
                    case 2 -> { // Razor - arc attack with GRIEVOUS WOUNDS
                        float baseDamage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                        // Deal damage only in arc in front of player
                        applyRazorDamageInArc(player, level, baseDamage + damageBonus, 4.0, player.getYRot());
                        // Dark red arc slash effect with animated blades
                        spawnRazorEffect(level, playerPos.add(0, 1, 0), 4.0, player.getYRot());
                    }
                    case 3 -> { // Rupture - sticky sword projectile
                        Vec3 lookVec = player.getLookAngle();
                        Vec3 startPos = playerPos.add(0, player.getEyeHeight(), 0);
                        float baseDamage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                        spawnRuptureProjectile(player, level, startPos, lookVec, baseDamage * 0.8f + damageBonus * 0.8f);
                        // Dark red launch effect
                        spawnDustParticlesBurst(level, startPos, 1.5, 0.6f, 0.0f, 0.0f, 15);
                    }
                    case 4 -> { // Heartstopper - charge and slam (tracks current position)
                        // Mark player as charging
                        player.getPersistentData().putBoolean("ravager_heartstopper_charging", true);
                        player.getPersistentData().putLong("ravager_heartstopper_start", level.getGameTime());
                        float baseDamage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                        player.getPersistentData().putFloat("ravager_heartstopper_damage", baseDamage * 2.0f + damageBonus * 2.0f);
                        
                        // Slow the player significantly
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 10));
                        
                        player.displayClientMessage(Component.literal("§c§lCHARGING HEARTSTOPPER..."), true);
                    }
                }
            }
            case "berserker" -> {
                var rpgData = player.getData(ModAttachments.PLAYER_RPG);
                switch (slot) {
                    case 1 -> { // Axe Throw - throwing axe that returns
                        // Check charge cooldown (1.5s delay between uses)
                        long currentTime = level.getGameTime();
                        if (currentTime - rpgData.getLastAxeThrowTime() < AXE_THROW_DELAY_TICKS) {
                            player.displayClientMessage(Component.literal("§eAxe Throw is on delay!"), true);
                            return;
                        }
                        
                        // Check charges
                        if (rpgData.getAxeThrowCharges() <= 0) {
                            player.displayClientMessage(Component.literal("§cNo Axe Throw charges!"), true);
                            return;
                        }
                        
                        rpgData.useAxeThrowCharge();
                        rpgData.setLastAxeThrowTime(currentTime);
                        
                        Vec3 lookVec = player.getLookAngle();
                        Vec3 startPos = playerPos.add(0, player.getEyeHeight() - 0.5, 0); // Chest level, not eye level
                        float baseDamage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                        float axeDamage = baseDamage * 0.6f + damageBonus * 0.6f;
                        
                        // Spawn axe projectile
                        spawnAxeThrowProjectile(player, level, startPos, lookVec, axeDamage);
                        
                        // Orange launch effect
                        spawnDustParticlesBurst(level, startPos, 1.0, 1.0f, 0.5f, 0.0f, 10);
                        
                        player.displayClientMessage(Component.literal("§6Axe Throw! §7(" + rpgData.getAxeThrowCharges() + " charges left)"), true);
                    }
                    case 2 -> { // Blood Oath - sacrifice HP for RAGE
                        float maxHealth = player.getMaxHealth();
                        float currentHealth = player.getHealth();
                        float healthThreshold = maxHealth * 0.3f;
                        
                        // Check if below 30% HP
                        if (currentHealth <= healthThreshold) {
                            player.displayClientMessage(Component.literal("§cCannot use Blood Oath below 30% HP!"), true);
                            return;
                        }
                        
                        // Sacrifice 20% of total HP
                        float sacrifice = maxHealth * 0.2f;
                        player.hurt(player.damageSources().magic(), sacrifice);
                        
                        // Gain 30 RAGE
                        rpgData.addRage(30);
                        syncRageToClient(player);
                        
                        // Blood visual effect
                        spawnBloodOathEffect(level, playerPos);
                        
                        player.displayClientMessage(Component.literal("§4Blood Oath! §c-" + String.format("%.0f", sacrifice) + " HP §6+30 RAGE"), true);
                    }
                    case 3 -> { // Frenzy - multiple slashes, consumes RAGE if shifting
                        boolean isSneaking = player.isCrouching();
                        float baseDamage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                        float slashDamage = baseDamage * 0.2f + damageBonus * 0.2f;
                        
                        // Base 8 slashes at 0 rage over 3s
                        int baseSlashes = 8;
                        int extraSlashes = 0;
                        
                        if (isSneaking) {
                            // Consumes RAGE for extra slashes (5 RAGE per extra slash)
                            int currentRage = rpgData.getRage();
                            extraSlashes = currentRage / 5;
                            rpgData.setRage(0); // Consume all RAGE
                            syncRageToClient(player);
                        }
                        
                        int totalSlashes = baseSlashes + extraSlashes;
                        
                        // Schedule slashes over 3 seconds
                        spawnFrenzySlashes(player, level, playerPos, slashDamage, totalSlashes, isSneaking);
                        
                        player.displayClientMessage(Component.literal("§6FRENZY! §7(" + totalSlashes + " slashes)"), true);
                    }
                    case 4 -> { // Unbound Carnage - enhanced enraged state
                        // Check if already in enhanced enraged or enraged state
                        if (rpgData.isEnhancedEnraged()) {
                            player.displayClientMessage(Component.literal("§cAlready in Unbound Carnage!"), true);
                            return;
                        }
                        
                        // Fill RAGE to 100 and enter enhanced enraged state
                        rpgData.setRage(100);
                        rpgData.setEnraged(false); // Switch from normal enraged
                        rpgData.setEnhancedEnraged(true);
                        rpgData.setEnhancedEnragedEndTime(level.getGameTime() + 200); // 10 seconds
                        syncRageToClient(player);
                        
                        // Apply enhanced buffs: +35% speed, +35% damage
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2)); // Stronger speed
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 2)); // Stronger damage
                        
                        // Visual effect
                        spawnUnboundCarnageEffect(level, playerPos);
                        
                        player.displayClientMessage(Component.literal("§6§l⚡ UNBOUND CARNAGE! ⚡"), true);
                    }
                }
            }
            case "lancer" -> {
                var rpgData = player.getData(ModAttachments.PLAYER_RPG);
                switch (slot) {
                    case 1 -> { // Piercing Charge - forward sprint with momentum damage
                        float momentum = rpgData.getMomentum();
                        
                        // Check momentum requirement (> 50%)
                        if (momentum < 50.0f) {
                            player.displayClientMessage(Component.literal("§eNeed at least 50% momentum for Piercing Charge!"), true);
                            return;
                        }
                        
                        // Toggle Piercing Charge on/off
                        if (rpgData.isInPiercingCharge()) {
                            // Cancel Piercing Charge
                            rpgData.setInPiercingCharge(false);
                            player.displayClientMessage(Component.literal("§ePiercing Charge §7cancelled!"), true);
                        } else {
                            // Start Piercing Charge
                            rpgData.setInPiercingCharge(true);
                            rpgData.setPiercingChargeStartTime(level.getGameTime());
                            
                            // Store damage based on momentum
                            float baseDamage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                            rpgData.setPiercingChargeDamage(momentum * 0.5f + baseDamage);
                            
                            // Force player to sprint forward
                            player.setSprinting(true);
                            
                            // Yellow charging effect
                            spawnDustParticlesBurst(level, playerPos.add(0, 1, 0), 2.0, 1.0f, 1.0f, 0.4f, 20);
                            
                            player.displayClientMessage(Component.literal("§e§lPIERCING CHARGE! §7Sprint through enemies!"), true);
                        }
                    }
                    case 2 -> { // Leap - forward launch with velocity
                        Vec3 lookVec = player.getLookAngle();
                        Vec3 horizontalLook = new Vec3(lookVec.x, 0, lookVec.z).normalize();
                        
                        // Launch player forward with large velocity
                        double forwardVelocity = 1.5;
                        double upwardVelocity = 0.4;
                        
                        Vec3 velocity = new Vec3(
                                horizontalLook.x * forwardVelocity,
                                upwardVelocity,
                                horizontalLook.z * forwardVelocity
                        );
                        
                        player.setDeltaMovement(velocity);
                        player.hurtMarked = true;
                        
                        // Yellow leap effect
                        spawnDustParticlesBurst(level, playerPos, 2.0, 1.0f, 1.0f, 0.4f, 25);
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                                playerPos.x, playerPos.y + 0.5, playerPos.z, 15, 0.3, 0.3, 0.3, 0.1);
                    }
                    case 3 -> { // Lunge - horizontal lunge with momentum damage
                        float momentum = rpgData.getMomentum();
                        Vec3 lookVec = player.getLookAngle();
                        Vec3 horizontalLook = new Vec3(lookVec.x, 0, lookVec.z).normalize();
                        
                        // Lunge forward (no vertical velocity)
                        double lungeVelocity = 1.2;
                        Vec3 velocity = new Vec3(
                                horizontalLook.x * lungeVelocity,
                                0,
                                horizontalLook.z * lungeVelocity
                        );
                        
                        player.setDeltaMovement(velocity);
                        player.hurtMarked = true;
                        
                        // Calculate damage: momentum/10 + damage stat (max 20)
                        float lungeDamage = Math.min(20.0f, (momentum / 10.0f) + damageBonus);
                        
                        // Deal damage to nearby enemies
                        boolean hitEnemy = dealDamageToNearbyEnemiesWithCheck(player, lungeDamage, 3.0);
                        
                        // If no enemy hit, reset Leap cooldown
                        if (!hitEnemy) {
                            rpgData.setAbilityCooldown("lancer_ability_2", 0);
                            player.displayClientMessage(Component.literal("§aLeap cooldown reset!"), true);
                        }
                        
                        // Yellow lunge effect
                        spawnDustParticlesLine(level, playerPos, player.getYRot(), 3.0, 1.0f, 1.0f, 0.4f);
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                                playerPos.x + horizontalLook.x * 2, playerPos.y + 1, playerPos.z + horizontalLook.z * 2,
                                3, 0.3, 0.3, 0.3, 0);
                    }
                    case 4 -> { // Comet - convert velocity to downward, shockwave on impact
                        float momentum = rpgData.getMomentum();
                        
                        // Store comet state for impact detection
                        player.getPersistentData().putBoolean("lancer_comet_active", true);
                        
                        // Calculate impact damage: momentum/5 + damage stat * 2 (higher than lunge)
                        float cometDamage = (momentum / 5.0f) + (damageBonus * 2.0f);
                        player.getPersistentData().putFloat("lancer_comet_damage", cometDamage);
                        
                        // Convert all velocity to downward
                        Vec3 currentVelocity = player.getDeltaMovement();
                        double totalSpeed = Math.sqrt(
                                currentVelocity.x * currentVelocity.x +
                                currentVelocity.y * currentVelocity.y +
                                currentVelocity.z * currentVelocity.z
                        );
                        
                        // Apply strong downward velocity
                        player.setDeltaMovement(0, -Math.max(2.0, totalSpeed * 1.5), 0);
                        player.hurtMarked = true;
                        
                        // Remove all momentum
                        rpgData.setMomentum(0);
                        
                        // Yellow diving effect
                        spawnDustParticlesBurst(level, playerPos, 3.0, 1.0f, 1.0f, 0.2f, 30);
                        
                        player.displayClientMessage(Component.literal("§e§lCOMET DIVE!"), true);
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
    public static DustParticleOptions createDustParticle(float r, float g, float b, float size) {
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
    
    /**
     * Deal damage to nearby enemies and return whether any enemies were hit
     */
    private static boolean dealDamageToNearbyEnemiesWithCheck(ServerPlayer player, double damage, double range) {
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<Entity> entities = player.level().getEntities(player, searchBox, 
                e -> e instanceof LivingEntity && e != player);
        
        boolean hitAny = false;
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                living.hurt(player.damageSources().playerAttack(player), (float) damage);
                hitAny = true;
            }
        }
        return hitAny;
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
        int totalArrows = arrowCount * ULTIMATE_ARROW_MULTIPLIER;
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
        // Lime green energy theme (0.5, 1.0, 0.2 is lime green)
        float lime_r = 0.5f, lime_g = 1.0f, lime_b = 0.2f;
        
        // ===== LIME GREEN SPIRAL RISING UPWARD =====
        Vec3 normalizedDir = direction.normalize();
        int spiralPoints = 40;
        for (int i = 0; i < spiralPoints; i++) {
            double progress = (double) i / spiralPoints;
            
            // Spiral parameters
            double spiralAngle = progress * 4 * Math.PI; // 2 full rotations
            double spiralRadius = 0.6 + progress * 0.4; // Expanding radius
            double height = progress * 3.0; // Rising 3 blocks
            
            // Calculate spiral position
            double x = center.x + Math.cos(spiralAngle) * spiralRadius;
            double y = center.y + height;
            double z = center.z + Math.sin(spiralAngle) * spiralRadius;
            
            // Gradient from lime to lighter green
            float g_value = lime_g - (float) progress * 0.2f; // 1.0 → 0.8
            float b_value = lime_b + (float) progress * 0.3f; // 0.2 → 0.5
            
            level.sendParticles(createDustParticle(lime_r, g_value, b_value, 0.9f),
                    x, y, z, 2, 0.05, 0.05, 0.05, 0.01);
        }
        
        // ===== DIRECTIONAL DASH TRAIL =====
        for (int i = 0; i < 20; i++) {
            double progress = (double) i / 20;
            double trailLength = 2.5;
            Vec3 pos = center.add(normalizedDir.scale(progress * trailLength));
            
            // Lime green trail matching spiral
            level.sendParticles(createDustParticle(lime_r, lime_g, lime_b, 0.7f),
                    pos.x, pos.y + 1.0, pos.z, 3, 0.15, 0.15, 0.15, 0.02);
        }
        
        // ===== END ROD CORE (for highlight) =====
        for (int i = 0; i < 12; i++) {
            double progress = (double) i / 12;
            Vec3 pos = center.add(normalizedDir.scale(progress * 2.0));
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    pos.x, pos.y + 1, pos.z, 1, 0.05, 0.05, 0.05, 0);
        }
        
        // ===== LAUNCH BURST - LIME GREEN CIRCLE =====
        for (int ring = 0; ring < 2; ring++) {
            double radius = 0.6 + ring * 0.3;
            for (int p = 0; p < 16; p++) {
                double angle = (double) p / 16 * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                level.sendParticles(createDustParticle(lime_r, lime_g, lime_b, 0.8f),
                        x, center.y + 0.5, z, 1, 0.03, 0.03, 0.03, 0);
            }
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
        AABB searchBox = player.getBoundingBox().inflate(SEEKER_SEARCH_RANGE);
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
    
    // ===== NEW RANGER ABILITY METHODS =====
    
    /**
     * Spawn a MASSIVE Precise Shot arrow projectile - 3x bigger, slower, more dramatic
     */
    private static void spawnPreciseShotArrowProjectile(ServerPlayer player, ServerLevel level, Vec3 start, Vec3 direction, float damage) {
        // Create an arrow entity with enhanced properties
        net.minecraft.world.entity.projectile.Arrow arrow = new net.minecraft.world.entity.projectile.Arrow(level, player,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
        
        // Shoot slower for more dramatic effect
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.0F, 0.1F);
        arrow.setBaseDamage(damage);
        arrow.setCritArrow(true);
        arrow.setGlowingTag(true); // Make it glow for visibility
        arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
        level.addFreshEntity(arrow);
        
        // Spawn MASSIVE 3x particle trail alongside the arrow
        Vec3 normalizedDir = direction.normalize();
        Vec3 perpVec1 = getPerpendicular(normalizedDir);
        Vec3 perpVec2 = normalizedDir.cross(perpVec1).normalize();
        
        // 3x larger particles following the path
        double arrowLength = 40.0;
        for (int i = 0; i < 120; i++) { // More particles for larger effect
            double progress = (double) i / 120.0 * arrowLength;
            Vec3 pos = start.add(normalizedDir.scale(progress));
            
            // Main shaft - 3x thicker
            for (int layer = 0; layer < 3; layer++) {
                double spread = layer * 0.25; // 3x wider spread
                level.sendParticles(createDustParticle(0.2f, 0.9f, 0.3f, 1.2f), // Larger particles
                        pos.x + (RANDOM.nextDouble() - 0.5) * spread,
                        pos.y + (RANDOM.nextDouble() - 0.5) * spread,
                        pos.z + (RANDOM.nextDouble() - 0.5) * spread,
                        1, 0.03, 0.03, 0.03, 0);
            }
        }
        
        // 3x larger radiating circles along the path
        for (int ring = 0; ring < 18; ring++) { // More rings
            double ringProgress = (double) ring / 18.0 * arrowLength;
            Vec3 ringCenter = start.add(normalizedDir.scale(ringProgress));
            
            // 3x larger radius for circles
            double baseRadius = 1.2 + (ring % 3) * 0.75; // 3x bigger base radius
            int circlePoints = 24; // More points for smoother circles
            for (int p = 0; p < circlePoints; p++) {
                double angle = (double) p / circlePoints * 2 * Math.PI;
                double ox = Math.cos(angle) * baseRadius;
                double oy = Math.sin(angle) * baseRadius;
                Vec3 circlePos = ringCenter.add(perpVec1.scale(ox)).add(perpVec2.scale(oy));
                
                float circleSize = 0.6f + RANDOM.nextFloat() * 0.3f; // Larger circle particles
                level.sendParticles(createDustParticle(0.3f, 1.0f, 0.4f, circleSize),
                        circlePos.x, circlePos.y, circlePos.z, 1, 0, 0, 0, 0);
            }
            
            // END_ROD particles at cardinal points - more dramatic
            for (int cardinal = 0; cardinal < 8; cardinal++) {
                double cardAngle = cardinal * Math.PI / 4;
                double ox = Math.cos(cardAngle) * baseRadius * 0.7;
                double oy = Math.sin(cardAngle) * baseRadius * 0.7;
                Vec3 glowPos = ringCenter.add(perpVec1.scale(ox)).add(perpVec2.scale(oy));
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        glowPos.x, glowPos.y, glowPos.z, 1, 0.02, 0.02, 0.02, 0);
            }
        }
        
        // Impact flash at end
        Vec3 endPos = start.add(normalizedDir.scale(arrowLength));
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                endPos.x, endPos.y, endPos.z, 3, 0.5, 0.5, 0.5, 0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW,
                endPos.x, endPos.y, endPos.z, 30, 0.8, 0.8, 0.8, 0.05);
    }
    
    /**
     * Spawn Multi-Shot arrow projectiles - actual arrows shot from player in spread pattern
     */
    private static void spawnMultiShotArrowProjectiles(ServerPlayer player, ServerLevel level, Vec3 start, Vec3 direction,
            int arrowCount, float damage, float velocity, float spreadAngle) {
        float baseYaw = player.getYRot();
        float basePitch = player.getXRot();
        float halfSpread = spreadAngle / 2.0f;
        float angleStep = arrowCount > 1 ? spreadAngle / (arrowCount - 1) : 0;
        
        for (int i = 0; i < arrowCount; i++) {
            float yawOffset = arrowCount > 1 ? -halfSpread + (angleStep * i) : 0;
            
            net.minecraft.world.entity.projectile.Arrow arrow = new net.minecraft.world.entity.projectile.Arrow(level, player,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
            arrow.shootFromRotation(player, basePitch, baseYaw + yawOffset, 0.0F, velocity, 1.5F);
            arrow.setBaseDamage(damage);
            arrow.setGlowingTag(true);
            arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
            level.addFreshEntity(arrow);
            
            // Individual arrow trail particles
            double yawRad = Math.toRadians(-baseYaw - 90 + yawOffset);
            double pitchRad = Math.toRadians(-basePitch);
            Vec3 arrowDir = new Vec3(
                    Math.cos(yawRad) * Math.cos(pitchRad),
                    Math.sin(pitchRad),
                    Math.sin(yawRad) * Math.cos(pitchRad)
            ).normalize();
            
            // Trail particles for each arrow
            for (int p = 0; p < 15; p++) {
                double dist = (double) p / 15 * 10.0;
                Vec3 pos = start.add(arrowDir.scale(dist));
                level.sendParticles(createDustParticle(0.3f, 0.85f, 0.35f, 0.4f),
                        pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0);
            }
        }
    }
    
    /**
     * Spawn dramatic launch effect for Multi-Shot
     */
    private static void spawnMultiShotLaunchEffect(ServerLevel level, Vec3 center, Vec3 direction) {
        // Flash at launch point
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y, center.z, 2, 0.2, 0.2, 0.2, 0);
        
        // Green burst particles
        for (int i = 0; i < 20; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = RANDOM.nextDouble() * 0.5;
            level.sendParticles(createDustParticle(0.3f, 0.9f, 0.4f, 0.6f),
                    center.x + Math.cos(angle) * dist,
                    center.y + (RANDOM.nextDouble() - 0.5) * 0.3,
                    center.z + Math.sin(angle) * dist,
                    1, 0.1, 0.1, 0.1, 0.05);
        }
        
        // Sweep attack visual
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                center.x + direction.x * 0.5, center.y, center.z + direction.z * 0.5,
                2, 0.3, 0.2, 0.3, 0);
    }
    
    /**
     * Spawn Rain of Arrows activation effect - SMALLER with DEFINED CIRCLE (not clouds!)
     */
    private static void spawnRainOfArrowsActivationEffect(ServerLevel level, Vec3 center, double radius) {
        // DEFINED BOUNDARY CIRCLE - bright golden ring on ground (NOT CLOUDS!)
        for (int ring = 0; ring < 4; ring++) {
            double ringRadius = radius * ((double) (ring + 1) / 4);
            int points = (int) (ringRadius * 20) + 20;
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * ringRadius;
                double z = center.z + Math.sin(angle) * ringRadius;
                
                // Bright defined circle particles - golden/orange
                level.sendParticles(createDustParticle(1.0f, 0.8f, 0.2f, 0.9f),
                        x, center.y + 0.05, z, 2, 0.02, 0, 0.02, 0);
            }
        }
        
        // OUTER BOUNDARY - extra thick defined edge
        int outerPoints = (int) (radius * 24);
        for (int p = 0; p < outerPoints; p++) {
            double angle = (double) p / outerPoints * 2 * Math.PI;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            
            // Thick outer boundary
            level.sendParticles(createDustParticle(1.0f, 0.6f, 0.1f, 1.0f),
                    x, center.y + 0.08, z, 3, 0.03, 0, 0.03, 0);
            // Secondary glow
            level.sendParticles(createDustParticle(1.0f, 0.9f, 0.4f, 0.6f),
                    x, center.y + 0.2, z, 1, 0.05, 0.05, 0.05, 0);
        }
        
        // Cross pattern in center for targeting
        for (int i = -8; i <= 8; i++) {
            double offset = i * (radius / 8);
            level.sendParticles(createDustParticle(1.0f, 0.7f, 0.15f, 0.7f),
                    center.x + offset, center.y + 0.1, center.z, 2, 0.03, 0, 0.03, 0);
            level.sendParticles(createDustParticle(1.0f, 0.7f, 0.15f, 0.7f),
                    center.x, center.y + 0.1, center.z + offset, 2, 0.03, 0, 0.03, 0);
        }
        
        // Upward targeting beam (short, not obscuring)
        for (int i = 0; i < 20; i++) {
            double y = center.y + 2 + i * 0.4;
            level.sendParticles(createDustParticle(1.0f, 0.85f, 0.3f, 0.6f),
                    center.x, y, center.z, 2, 0.15, 0.08, 0.15, 0.01);
        }
        
        // Activation flash
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y + 3, center.z, 3, 0.3, 0.3, 0.3, 0);
    }
    
    /**
     * Spawn per-tick effect for Rain of Arrows - MORE VISIBLE arrows that TOUCH THE GROUND
     */
    private static void spawnRainOfArrowsTickEffect(RainOfArrowsEffect effect) {
        ServerLevel level = effect.level;
        Vec3 center = effect.center;
        double radius = effect.radius;
        
        // MORE VISIBLE energy bolts raining down and HITTING THE GROUND
        int boltsPerTick = 12; // More visible projectiles
        for (int bolt = 0; bolt < boltsPerTick; bolt++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = RANDOM.nextDouble() * radius;
            double groundX = center.x + Math.cos(angle) * dist;
            double groundZ = center.z + Math.sin(angle) * dist;
            double startY = center.y + 8 + RANDOM.nextDouble() * 4;
            
            // FALLING ENERGY BOLT - from sky to ground
            for (int i = 0; i < 16; i++) {
                double progress = (double) i / 16.0;
                double y = startY - progress * (startY - center.y);
                
                // Main bolt - bright and visible
                float brightness = 1.0f - (float) progress * 0.3f;
                level.sendParticles(createDustParticle(0.3f * brightness, 0.95f * brightness, 0.4f * brightness, 0.8f),
                        groundX + (RANDOM.nextDouble() - 0.5) * 0.05,
                        y,
                        groundZ + (RANDOM.nextDouble() - 0.5) * 0.05,
                        1, 0, -0.3, 0, 0.05);
                
                // Glow trail
                if (i % 3 == 0) {
                    level.sendParticles(createDustParticle(0.5f, 1.0f, 0.6f, 0.5f),
                            groundX, y, groundZ, 1, 0.03, 0.03, 0.03, 0);
                }
            }
            
            // GROUND IMPACT - particles TOUCHING THE GROUND
            level.sendParticles(createDustParticle(0.4f, 1.0f, 0.5f, 0.9f),
                    groundX, center.y + 0.05, groundZ, 3, 0.1, 0.02, 0.1, 0.03);
            
            // Impact spark on ground
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                    groundX, center.y + 0.1, groundZ, 1, 0.08, 0.02, 0.08, 0.02);
        }
        
        // PERSISTENT GROUND BOUNDARY - keep the circle visible during effect
        if (effect.ticksRemaining % 8 == 0) {
            int boundaryPoints = (int) (radius * 16);
            for (int p = 0; p < boundaryPoints; p++) {
                double boundaryAngle = (double) p / boundaryPoints * 2 * Math.PI;
                double bx = center.x + Math.cos(boundaryAngle) * radius;
                double bz = center.z + Math.sin(boundaryAngle) * radius;
                
                level.sendParticles(createDustParticle(1.0f, 0.75f, 0.2f, 0.6f),
                        bx, center.y + 0.05, bz, 1, 0.02, 0, 0.02, 0);
            }
        }
        
        // Additional ground impact ripples
        for (int i = 0; i < 4; i++) {
            double impactAngle = RANDOM.nextDouble() * 2 * Math.PI;
            double impactDist = RANDOM.nextDouble() * radius;
            double impactX = center.x + Math.cos(impactAngle) * impactDist;
            double impactZ = center.z + Math.sin(impactAngle) * impactDist;
            
            // Small impact ring on ground
            for (int ring = 0; ring < 6; ring++) {
                double ringAngle = ring * Math.PI / 3;
                double ringRadius = 0.2;
                level.sendParticles(createDustParticle(0.35f, 0.9f, 0.4f, 0.5f),
                        impactX + Math.cos(ringAngle) * ringRadius,
                        center.y + 0.08,
                        impactZ + Math.sin(ringAngle) * ringRadius,
                        1, 0.02, 0, 0.02, 0);
            }
        }
    }
    
    /**
     * Deal damage for Rain of Arrows (called every 20 ticks)
     */
    private static void dealRainOfArrowsDamage(RainOfArrowsEffect effect) {
        AABB damageBox = new AABB(
                effect.center.x - effect.radius, effect.center.y - 1, effect.center.z - effect.radius,
                effect.center.x + effect.radius, effect.center.y + 3, effect.center.z + effect.radius
        );
        
        List<Entity> entities = effect.level.getEntities(effect.player, damageBox,
                e -> e instanceof LivingEntity && e != effect.player);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                living.hurt(effect.player.damageSources().playerAttack(effect.player), effect.damage);
            }
        }
    }
    
    /**
     * Spawn final effect when Rain of Arrows ends
     */
    private static void spawnRainOfArrowsFinalEffect(RainOfArrowsEffect effect) {
        ServerLevel level = effect.level;
        Vec3 center = effect.center;
        double radius = effect.radius;
        
        // Final shockwave
        for (int ring = 0; ring < 5; ring++) {
            double ringRadius = radius * (ring + 1) / 5;
            int points = (int) (ringRadius * 14);
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * ringRadius;
                double z = center.z + Math.sin(angle) * ringRadius;
                
                level.sendParticles(createDustParticle(0.4f, 1.0f, 0.5f, 0.7f),
                        x, center.y + 0.3 + ring * 0.2, z, 1, 0.1, 0.2, 0.1, 0.03);
            }
        }
        
        // Final flash
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y + 1, center.z, 3, 0.5, 0.5, 0.5, 0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW,
                center.x, center.y + 1, center.z, 40, radius * 0.5, 1.0, radius * 0.5, 0.03);
    }
    
    // ===== NEW HAWKEYE ABILITY METHODS =====
    
    /**
     * Spawn Vault projectile - A large glowing projectile with scute-like visual appearance.
     * Uses a ThrownTrident entity as the base since it provides good arc trajectory and damage mechanics.
     * The actual visual is enhanced with cyan/teal particles to represent a turtle scute shell projectile.
     */
    private static void spawnVaultScuteProjectile(ServerPlayer player, ServerLevel level, float velocity, float damage) {
        Vec3 lookVec = player.getLookAngle();
        
        // Using ThrownTrident as the projectile base - provides good physics for a lobbed heavy projectile.
        // The visual appearance is enhanced with scute-colored particles to match the intended design.
        net.minecraft.world.entity.projectile.ThrownTrident projectile = new net.minecraft.world.entity.projectile.ThrownTrident(
                level, player, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TRIDENT));
        
        // Shoot at an arc (lob)
        projectile.shootFromRotation(player, player.getXRot() - 25, player.getYRot(), 0.0F, velocity, 2.0F);
        projectile.setBaseDamage(damage);
        projectile.setGlowingTag(true);
        projectile.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
        level.addFreshEntity(projectile);
        
        // Spawn cyan/teal particle trail to visually represent a scute shell projectile
        Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 lobDir = new Vec3(lookVec.x, lookVec.y + 0.5, lookVec.z).normalize();
        
        // Spawn cyan/teal particles to represent scute
        for (int i = 0; i < 25; i++) {
            double progress = (double) i / 25;
            double dist = progress * 8.0;
            double arcY = Math.sin(progress * Math.PI) * 3.0; // Arc trajectory
            Vec3 pos = startPos.add(
                    lobDir.x * dist,
                    lobDir.y * dist + arcY,
                    lobDir.z * dist
            );
            
            // Teal/cyan color for scute
            level.sendParticles(createDustParticle(0.3f, 0.85f, 0.8f, 0.8f),
                    pos.x, pos.y, pos.z, 2, 0.15, 0.15, 0.15, 0.02);
            
            // Shell-like hexagonal particles
            if (i % 3 == 0) {
                for (int hex = 0; hex < 6; hex++) {
                    double hexAngle = (double) hex / 6 * 2 * Math.PI;
                    level.sendParticles(createDustParticle(0.2f, 0.75f, 0.7f, 0.5f),
                            pos.x + Math.cos(hexAngle) * 0.3,
                            pos.y + Math.sin(hexAngle) * 0.3,
                            pos.z + (RANDOM.nextDouble() - 0.5) * 0.2,
                            1, 0.05, 0.05, 0.05, 0);
                }
            }
        }
        
        // Launch flash
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                startPos.x, startPos.y, startPos.z, 1, 0, 0, 0, 0);
    }
    
    /**
     * Spawn MOVING seeker projectiles that home on targets in player's vision
     */
    private static void spawnMovingSeekerProjectiles(ServerPlayer player, ServerLevel level, int chargeCount, float damage) {
        // Find enemies in player's field of view
        Vec3 playerPos = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 lookVec = player.getLookAngle();
        
        AABB searchBox = player.getBoundingBox().inflate(SEEKER_SEARCH_RANGE);
        List<Entity> allEntities = player.level().getEntities(player, searchBox,
                e -> e instanceof LivingEntity && e != player && isHostile(e));
        
        // Filter to enemies in player's vision (within 60 degree cone)
        List<LivingEntity> visibleEnemies = new ArrayList<>();
        for (Entity entity : allEntities) {
            Vec3 toEntity = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(playerPos).normalize();
            double dot = lookVec.dot(toEntity);
            if (dot > 0.5) { // Within ~60 degree cone
                visibleEnemies.add((LivingEntity) entity);
            }
        }
        
        // Spawn seeker projectiles as timed moving entities
        for (int i = 0; i < chargeCount; i++) {
            LivingEntity target = null;
            if (!visibleEnemies.isEmpty()) {
                target = visibleEnemies.get(i % visibleEnemies.size());
            }
            
            // Create seeker projectile data
            SeekerProjectile seeker = new SeekerProjectile(player, level, playerPos, target, damage);
            
            // If no target, set a default direction
            if (target == null) {
                // Calculate spread direction
                float yawOffset = ((float) i / chargeCount - 0.5f) * 40f;
                double yawRad = Math.toRadians(-player.getYRot() - 90 + yawOffset);
                double pitchRad = Math.toRadians(-player.getXRot());
                Vec3 seekerDir = new Vec3(
                        Math.cos(yawRad) * Math.cos(pitchRad),
                        Math.sin(pitchRad),
                        Math.sin(yawRad) * Math.cos(pitchRad)
                ).normalize();
                // Store direction as a "fake target position" at the defined no-target range
                seeker.position = playerPos.add(seekerDir.scale(SEEKER_NO_TARGET_RANGE));
            }
            
            activeSeekers.add(seeker);
        }
    }
    
    /**
     * Update a seeker projectile - returns false if it should be removed
     */
    private static boolean updateSeekerProjectile(SeekerProjectile seeker) {
        // Check if max time exceeded
        if (seeker.ticksAlive >= seeker.maxTicks) {
            return false;
        }
        
        // Calculate target position
        Vec3 targetPos;
        if (seeker.target != null && seeker.target.isAlive()) {
            targetPos = seeker.target.position().add(0, seeker.target.getBbHeight() * 0.5, 0);
        } else {
            // No target or target dead - fly straight using the defined range constant
            targetPos = seeker.position.add(seeker.owner.getLookAngle().scale(SEEKER_STRAIGHT_FLIGHT_RANGE));
        }
        
        // Move toward target
        Vec3 toTarget = targetPos.subtract(seeker.position);
        double distance = toTarget.length();
        
        if (distance < 1.0 && seeker.target != null && seeker.target.isAlive()) {
            // Hit the target
            seeker.target.hurt(seeker.owner.damageSources().playerAttack(seeker.owner), seeker.damage);
            
            // Impact effect - cyan/green to match Hawkeye theme
            seeker.level.sendParticles(createDustParticle(0.3f, 1.0f, 0.85f, 1.2f),
                    seeker.position.x, seeker.position.y, seeker.position.z, 15, 0.3, 0.3, 0.3, 0.1);
            seeker.level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                    seeker.position.x, seeker.position.y, seeker.position.z, 1, 0, 0, 0, 0);
            return false;
        }
        
        // Move seeker
        Vec3 moveDir = toTarget.normalize();
        seeker.position = seeker.position.add(moveDir.scale(seeker.speed));
        
        // Spawn seeker visual particles
        if (seeker.ticksAlive % SEEKER_PROJECTILE_SPEED_TICKS == 0) {
            // Core orb - cyan/green to match Hawkeye theme
            seeker.level.sendParticles(createDustParticle(0.3f, 0.95f, 0.85f, 0.9f),
                    seeker.position.x, seeker.position.y, seeker.position.z, 3, 0.08, 0.08, 0.08, 0);
            
            // Orbital particles
            Vec3 perpVec1 = getPerpendicular(moveDir);
            Vec3 perpVec2 = moveDir.cross(perpVec1).normalize();
            double orbitAngle = seeker.ticksAlive * 0.8;
            for (int orbit = 0; orbit < 6; orbit++) {
                double angle = orbitAngle + (double) orbit / 6 * 2 * Math.PI;
                double orbitRadius = 0.3;
                double ox = Math.cos(angle) * orbitRadius;
                double oy = Math.sin(angle) * orbitRadius;
                Vec3 orbitPos = seeker.position.add(perpVec1.scale(ox)).add(perpVec2.scale(oy));
                
                seeker.level.sendParticles(createDustParticle(0.25f, 0.85f, 0.75f, 0.4f),
                        orbitPos.x, orbitPos.y, orbitPos.z, 1, 0, 0, 0, 0);
            }
            
            // Trail - use END_ROD to match Hawkeye theme
            seeker.level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    seeker.position.x, seeker.position.y, seeker.position.z, 1, 0.02, 0.02, 0.02, 0.01);
        }
        
        return true;
    }
    
    /**
     * Spawn enhanced seeker launch effect
     */
    private static void spawnSeekerLaunchEffect(ServerLevel level, Vec3 center, int charges) {
        // Central power-up effect - cyan/green to match Hawkeye theme
        for (int ring = 0; ring < charges; ring++) {
            double ringY = center.y + 1.2 + ring * 0.3;
            double radius = 0.8 + ring * 0.2;
            
            for (int p = 0; p < 12; p++) {
                double angle = (double) p / 12 * 2 * Math.PI + ring * 0.5;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                
                level.sendParticles(createDustParticle(0.3f, 0.9f, 0.8f, 0.6f),
                        x, ringY, z, 1, 0.03, 0.03, 0.03, 0);
            }
        }
        
        // Burst - use END_ROD and GLOW to match Hawkeye theme
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y + 1.5, center.z, 2, 0.2, 0.2, 0.2, 0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                center.x, center.y + 1.5, center.z, charges * 8, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW,
                center.x, center.y + 1.5, center.z, charges * 5, 0.4, 0.4, 0.4, 0.08);
    }
    
    // ===== MARKSMAN ABILITY METHODS =====
    
    /**
     * Spawn Marksman Steady Shot - high damage, fast, accurate
     */
    private static void spawnMarksmanSteadyShot(ServerPlayer player, ServerLevel level, Vec3 start, Vec3 direction, float damage) {
        // Create a high-velocity arrow
        net.minecraft.world.entity.projectile.Arrow arrow = new net.minecraft.world.entity.projectile.Arrow(level, player,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 4.0F, 0.0F); // Very fast, perfect accuracy
        arrow.setBaseDamage(damage);
        arrow.setCritArrow(true);
        arrow.setGlowingTag(true);
        arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
        level.addFreshEntity(arrow);
        
        // Orange/red precision trail
        Vec3 normalizedDir = direction.normalize();
        for (int i = 0; i < 60; i++) {
            double dist = (double) i / 60 * 50.0;
            Vec3 pos = start.add(normalizedDir.scale(dist));
            
            level.sendParticles(createDustParticle(1.0f, 0.6f, 0.2f, 0.4f),
                    pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0);
        }
        
        // Muzzle flash
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                start.x, start.y, start.z, 1, 0, 0, 0, 0);
    }
    
    /**
     * Spawn scope effect for Steady Shot
     */
    private static void spawnSteadyShotScopeEffect(ServerLevel level, Vec3 center, Vec3 direction) {
        // Crosshair particles in front of player
        Vec3 crosshairPos = center.add(0, 1.5, 0).add(direction.scale(2));
        Vec3 perpVec1 = getPerpendicular(direction);
        Vec3 perpVec2 = direction.cross(perpVec1).normalize();
        
        // Horizontal line
        for (int i = -5; i <= 5; i++) {
            Vec3 pos = crosshairPos.add(perpVec1.scale(i * 0.1));
            level.sendParticles(createDustParticle(1.0f, 0.5f, 0.2f, 0.3f),
                    pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
        
        // Vertical line
        for (int i = -5; i <= 5; i++) {
            Vec3 pos = crosshairPos.add(perpVec2.scale(i * 0.1));
            level.sendParticles(createDustParticle(1.0f, 0.5f, 0.2f, 0.3f),
                    pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
    }
    
    /**
     * Spawn piercing arrow for Marksman
     */
    private static void spawnPiercingArrow(ServerPlayer player, ServerLevel level, Vec3 start, Vec3 direction, float damage, double range) {
        // Create arrow with max pierce level
        net.minecraft.world.entity.projectile.Arrow arrow = new net.minecraft.world.entity.projectile.Arrow(level, player,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.5F, 0.5F);
        arrow.setBaseDamage(damage);
        arrow.setCritArrow(true);
        arrow.setGlowingTag(true);
        arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
        level.addFreshEntity(arrow);
        
        // Deal damage to all enemies in line
        dealDamageInLine(player, damage, direction, range, 1.0);
    }
    
    /**
     * Spawn piercing trail effect
     */
    private static void spawnPiercingTrailEffect(ServerLevel level, Vec3 start, Vec3 direction) {
        Vec3 normalizedDir = direction.normalize();
        
        for (int i = 0; i < 80; i++) {
            double dist = (double) i / 80 * 50.0;
            Vec3 pos = start.add(normalizedDir.scale(dist));
            
            // Orange/yellow piercing trail
            level.sendParticles(createDustParticle(1.0f, 0.7f, 0.3f, 0.5f),
                    pos.x + (RANDOM.nextDouble() - 0.5) * 0.1,
                    pos.y + (RANDOM.nextDouble() - 0.5) * 0.1,
                    pos.z + (RANDOM.nextDouble() - 0.5) * 0.1,
                    1, 0, 0, 0, 0);
            
            // END_ROD particles at intervals
            if (i % 10 == 0) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0);
            }
        }
    }
    
    /**
     * Find target in player's line of sight
     */
    private static LivingEntity findTargetInSight(ServerPlayer player, double range) {
        Vec3 start = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 direction = player.getLookAngle();
        Vec3 end = start.add(direction.scale(range));
        
        AABB searchBox = new AABB(
                Math.min(start.x, end.x) - 2, Math.min(start.y, end.y) - 2, Math.min(start.z, end.z) - 2,
                Math.max(start.x, end.x) + 2, Math.max(start.y, end.y) + 2, Math.max(start.z, end.z) + 2
        );
        
        List<Entity> entities = player.level().getEntities(player, searchBox,
                e -> e instanceof LivingEntity && e != player);
        
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                Vec3 entityPos = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
                Vec3 toEntity = entityPos.subtract(start);
                double projection = toEntity.dot(direction);
                
                if (projection > 0 && projection < range) {
                    Vec3 closestPointOnLine = start.add(direction.scale(projection));
                    double distToLine = entityPos.distanceTo(closestPointOnLine);
                    
                    if (distToLine < entity.getBbWidth() + 1.0) { // Within entity width + margin
                        if (projection < closestDist) {
                            closestDist = projection;
                            closest = living;
                        }
                    }
                }
            }
        }
        
        return closest;
    }
    
    /**
     * Spawn Mark Target effect
     */
    private static void spawnMarkTargetEffect(ServerLevel level, Vec3 center, Entity target) {
        // Red targeting ring around enemy
        double radius = target.getBbWidth() + 0.5;
        for (int ring = 0; ring < 3; ring++) {
            double y = center.y + ring * 0.5;
            for (int p = 0; p < 16; p++) {
                double angle = (double) p / 16 * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                
                level.sendParticles(createDustParticle(1.0f, 0.3f, 0.2f, 0.5f),
                        x, y, z, 1, 0.02, 0.02, 0.02, 0);
            }
        }
        
        // Vertical beam on target
        for (int i = 0; i < 15; i++) {
            level.sendParticles(createDustParticle(1.0f, 0.4f, 0.3f, 0.4f),
                    center.x, center.y + i * 0.2, center.z, 1, 0.05, 0.05, 0.05, 0);
        }
    }
    
    /**
     * Spawn Headshot impact effect
     */
    private static void spawnHeadshotEffect(ServerLevel level, Vec3 center) {
        // Massive red burst
        level.sendParticles(createDustParticle(1.0f, 0.2f, 0.1f, 1.2f),
                center.x, center.y, center.z, 25, 0.4, 0.4, 0.4, 0.15);
        
        // Flash
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y, center.z, 3, 0.2, 0.2, 0.2, 0);
        
        // Crit particles
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                center.x, center.y, center.z, 30, 0.5, 0.5, 0.5, 0.3);
        
        // Crosshair effect
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 side = new Vec3(1, 0, 0);
        for (int i = -3; i <= 3; i++) {
            level.sendParticles(createDustParticle(1.0f, 0.0f, 0.0f, 0.6f),
                    center.x + i * 0.2, center.y, center.z, 1, 0, 0, 0, 0);
            level.sendParticles(createDustParticle(1.0f, 0.0f, 0.0f, 0.6f),
                    center.x, center.y + i * 0.2, center.z, 1, 0, 0, 0, 0);
        }
    }
    
    // ===== BEASTMASTER ABILITY METHODS =====
    
    /**
     * Spawn Wolf Attack effect
     */
    private static void spawnWolfAttackEffect(ServerLevel level, Vec3 center) {
        // Gray/white wolf attack particles
        for (int i = 0; i < 15; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = RANDOM.nextDouble() * 0.8;
            level.sendParticles(createDustParticle(0.6f, 0.6f, 0.6f, 0.5f),
                    center.x + Math.cos(angle) * dist,
                    center.y + 0.5 + RANDOM.nextDouble() * 0.5,
                    center.z + Math.sin(angle) * dist,
                    1, 0.1, 0.1, 0.1, 0.05);
        }
        
        // Claw slash visual
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                center.x, center.y + 0.8, center.z, 3, 0.3, 0.2, 0.3, 0);
        
        // Damage indicator
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                center.x, center.y + 1, center.z, 8, 0.3, 0.3, 0.3, 0.1);
    }
    
    /**
     * Spawn Wolf Howl effect
     */
    private static void spawnWolfHowlEffect(ServerLevel level, Vec3 center) {
        // Sound wave rings expanding outward
        for (int ring = 0; ring < 4; ring++) {
            double radius = 1.0 + ring * 0.8;
            for (int p = 0; p < 20; p++) {
                double angle = (double) p / 20 * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                
                level.sendParticles(createDustParticle(0.5f, 0.5f, 0.55f, 0.4f),
                        x, center.y + 1.5 - ring * 0.2, z, 1, 0.05, 0.05, 0.05, 0);
            }
        }
        
        // Upward howl particles
        for (int i = 0; i < 20; i++) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    center.x + (RANDOM.nextDouble() - 0.5) * 0.5,
                    center.y + 1.5 + i * 0.15,
                    center.z + (RANDOM.nextDouble() - 0.5) * 0.5,
                    1, 0.1, 0.15, 0.1, 0.02);
        }
    }
    
    /**
     * Spawn Bear Strength effect
     */
    private static void spawnBearStrengthEffect(ServerLevel level, Vec3 center) {
        // Brown/earthy protective particles
        for (int i = 0; i < 30; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = RANDOM.nextDouble() * 1.5;
            double y = center.y + RANDOM.nextDouble() * 2;
            
            level.sendParticles(createDustParticle(0.55f, 0.35f, 0.2f, 0.6f),
                    center.x + Math.cos(angle) * dist, y, center.z + Math.sin(angle) * dist,
                    1, 0.1, 0.1, 0.1, 0.03);
        }
        
        // Strength burst
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y + 1, center.z, 1, 0, 0, 0, 0);
        
        // Ground stomp effect
        for (int ring = 0; ring < 3; ring++) {
            double radius = 0.5 + ring * 0.5;
            for (int p = 0; p < 12; p++) {
                double angle = (double) p / 12 * 2 * Math.PI;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                        center.x + Math.cos(angle) * radius,
                        center.y + 0.1,
                        center.z + Math.sin(angle) * radius,
                        1, 0.05, 0.02, 0.05, 0.01);
            }
        }
    }
    
    /**
     * Spawn Eagle Eye effect
     */
    private static void spawnEagleEyeEffect(ServerLevel level, Vec3 center) {
        // Expanding detection rings
        for (int ring = 0; ring < 6; ring++) {
            double radius = ring * 3.0 + 2.0;
            int points = (int) (radius * 6);
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                
                level.sendParticles(createDustParticle(0.4f, 0.7f, 1.0f, 0.4f),
                        x, center.y + 2 + ring * 0.5, z, 1, 0.05, 0.05, 0.05, 0);
            }
        }
        
        // Central eye visual
        for (int i = 0; i < 10; i++) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    center.x, center.y + 2 + i * 0.3, center.z, 1, 0.05, 0.05, 0.05, 0);
        }
        
        // Flash
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y + 3, center.z, 2, 0.3, 0.3, 0.3, 0);
    }
    
    /**
     * Deal damage in a wide swath (for stampede)
     */
    private static void dealDamageInWideSwath(ServerPlayer player, float damage, Vec3 direction, double length, double width) {
        Vec3 start = player.position();
        Vec3 end = start.add(direction.scale(length));
        
        AABB damageBox = new AABB(
                Math.min(start.x, end.x) - width, start.y - 1, Math.min(start.z, end.z) - width,
                Math.max(start.x, end.x) + width, start.y + 3, Math.max(start.z, end.z) + width
        );
        
        List<Entity> entities = player.level().getEntities(player, damageBox,
                e -> e instanceof LivingEntity && e != player);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                // Check if entity is within the swath
                Vec3 toEntity = entity.position().subtract(start);
                double projection = toEntity.dot(direction);
                
                if (projection >= 0 && projection <= length) {
                    Vec3 closestPoint = start.add(direction.scale(projection));
                    double lateralDist = entity.position().distanceTo(closestPoint);
                    
                    if (lateralDist <= width + entity.getBbWidth() * 0.5) {
                        living.hurt(player.damageSources().playerAttack(player), damage);
                    }
                }
            }
        }
    }
    
    /**
     * Apply knockback to enemies in a line
     */
    private static void applyKnockbackInLine(ServerPlayer player, Vec3 direction, double length, double width, double strength) {
        Vec3 start = player.position();
        Vec3 end = start.add(direction.scale(length));
        
        AABB searchBox = new AABB(
                Math.min(start.x, end.x) - width, start.y - 1, Math.min(start.z, end.z) - width,
                Math.max(start.x, end.x) + width, start.y + 3, Math.max(start.z, end.z) + width
        );
        
        List<Entity> entities = player.level().getEntities(player, searchBox,
                e -> e instanceof LivingEntity && e != player);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                Vec3 knockbackDir = direction.add(0, 0.3, 0).normalize();
                living.setDeltaMovement(living.getDeltaMovement().add(knockbackDir.scale(strength)));
                living.hurtMarked = true;
            }
        }
    }
    
    /**
     * Spawn Stampede effect
     */
    private static void spawnStampedeEffect(ServerLevel level, Vec3 center, Vec3 direction) {
        // Dust cloud along the stampede path
        for (int dist = 0; dist < 15; dist++) {
            Vec3 pos = center.add(direction.scale(dist));
            
            // Width of stampede
            for (int w = -2; w <= 2; w++) {
                Vec3 perpDir = direction.cross(new Vec3(0, 1, 0)).normalize();
                Vec3 particlePos = pos.add(perpDir.scale(w));
                
                // Dust clouds
                level.sendParticles(createDustParticle(0.55f, 0.4f, 0.25f, 0.7f),
                        particlePos.x + (RANDOM.nextDouble() - 0.5) * 0.5,
                        particlePos.y + 0.3 + RANDOM.nextDouble() * 0.5,
                        particlePos.z + (RANDOM.nextDouble() - 0.5) * 0.5,
                        2, 0.2, 0.15, 0.2, 0.03);
                
                // Ground impact particles
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                        particlePos.x, particlePos.y + 0.1, particlePos.z,
                        1, 0.1, 0.02, 0.1, 0.01);
            }
            
            // Beast silhouette particles (dark shapes)
            if (dist % 3 == 0) {
                for (int h = 0; h < 5; h++) {
                    level.sendParticles(createDustParticle(0.3f, 0.25f, 0.2f, 0.8f),
                            pos.x + (RANDOM.nextDouble() - 0.5) * 2,
                            pos.y + 0.5 + h * 0.3,
                            pos.z + (RANDOM.nextDouble() - 0.5) * 2,
                            1, 0.1, 0.1, 0.1, 0.02);
                }
            }
        }
        
        // Shockwave at start
        for (int ring = 0; ring < 4; ring++) {
            double radius = 1.0 + ring * 1.0;
            int points = (int) (radius * 8);
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                
                level.sendParticles(createDustParticle(0.5f, 0.35f, 0.2f, 0.5f),
                        x, center.y + 0.2 + ring * 0.15, z, 1, 0.05, 0.1, 0.05, 0.02);
            }
        }
    }
    
    // ===== NEW PARTICLE-ONLY RANGER ABILITIES =====
    
    /**
     * Spawn Precise Shot ENERGY BEAM - particle-only, no arrows!
     */
    private static void spawnPreciseShotEnergyBeam(ServerLevel level, Vec3 start, Vec3 direction, LivingEntity target) {
        Vec3 normalizedDir = direction.normalize();
        Vec3 endPos = target != null ? target.position().add(0, target.getBbHeight() * 0.5, 0) : start.add(normalizedDir.scale(50.0));
        double distance = start.distanceTo(endPos);
        
        Vec3 perpVec1 = getPerpendicular(normalizedDir);
        Vec3 perpVec2 = normalizedDir.cross(perpVec1).normalize();
        
        // Main energy beam core - bright green/cyan
        for (int i = 0; i < 150; i++) {
            double progress = (double) i / 150.0;
            Vec3 pos = start.add(normalizedDir.scale(progress * distance));
            
            // Core beam - bright
            level.sendParticles(createDustParticle(0.3f, 1.0f, 0.5f, 1.0f),
                    pos.x, pos.y, pos.z, 2, 0.02, 0.02, 0.02, 0);
            
            // Outer glow
            if (i % 3 == 0) {
                level.sendParticles(createDustParticle(0.2f, 0.9f, 0.4f, 0.6f),
                        pos.x + (RANDOM.nextDouble() - 0.5) * 0.15,
                        pos.y + (RANDOM.nextDouble() - 0.5) * 0.15,
                        pos.z + (RANDOM.nextDouble() - 0.5) * 0.15,
                        1, 0.05, 0.05, 0.05, 0);
            }
        }
        
        // Spiraling energy rings along beam
        for (int ring = 0; ring < 20; ring++) {
            double ringProgress = (double) ring / 20.0;
            Vec3 ringCenter = start.add(normalizedDir.scale(ringProgress * distance));
            double ringRadius = 0.3 + Math.sin(ringProgress * Math.PI * 3) * 0.2;
            
            for (int p = 0; p < 8; p++) {
                double angle = ((double) p / 8 + ringProgress * 2) * 2 * Math.PI;
                double ox = Math.cos(angle) * ringRadius;
                double oy = Math.sin(angle) * ringRadius;
                Vec3 ringPos = ringCenter.add(perpVec1.scale(ox)).add(perpVec2.scale(oy));
                
                level.sendParticles(createDustParticle(0.4f, 1.0f, 0.6f, 0.5f),
                        ringPos.x, ringPos.y, ringPos.z, 1, 0, 0, 0, 0);
            }
        }
        
        // END_ROD particles for extra glow
        for (int i = 0; i < 30; i++) {
            double progress = RANDOM.nextDouble();
            Vec3 pos = start.add(normalizedDir.scale(progress * distance));
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0);
        }
        
        // Impact burst at end
        level.sendParticles(createDustParticle(0.5f, 1.0f, 0.7f, 1.2f),
                endPos.x, endPos.y, endPos.z, 25, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                endPos.x, endPos.y, endPos.z, 3, 0.3, 0.3, 0.3, 0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW,
                endPos.x, endPos.y, endPos.z, 20, 0.5, 0.5, 0.5, 0.05);
    }
    
    /**
     * Spawn Multi-Shot PARTICLE SPREAD - particle-only fan attack, no arrows!
     */
    private static void spawnMultiShotParticleSpread(ServerLevel level, Vec3 start, Vec3 direction, float playerYaw) {
        double baseAngle = Math.toRadians(-playerYaw + 90);
        int beamCount = 7;
        double spreadAngle = Math.toRadians(40);
        
        for (int beam = 0; beam < beamCount; beam++) {
            double beamAngle = baseAngle + (beam - beamCount / 2) * (spreadAngle / beamCount);
            Vec3 beamDir = new Vec3(Math.cos(beamAngle), direction.y * 0.5, Math.sin(beamAngle)).normalize();
            
            // Each beam particle trail
            for (int i = 0; i < 50; i++) {
                double dist = (double) i / 50.0 * 25.0;
                Vec3 pos = start.add(beamDir.scale(dist));
                
                // Main beam
                float intensity = 1.0f - (float) i / 50 * 0.5f;
                level.sendParticles(createDustParticle(0.3f * intensity, 0.9f * intensity, 0.4f * intensity, 0.5f),
                        pos.x + (RANDOM.nextDouble() - 0.5) * 0.1,
                        pos.y + (RANDOM.nextDouble() - 0.5) * 0.1,
                        pos.z + (RANDOM.nextDouble() - 0.5) * 0.1,
                        1, 0.03, 0.03, 0.03, 0);
                
                // Sparkle effect
                if (i % 8 == 0) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                            pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0);
                }
            }
        }
        
        // Central burst at origin
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                start.x, start.y, start.z, 2, 0.1, 0.1, 0.1, 0);
    }
    
    /**
     * Deal damage to enemies in a cone
     */
    private static void dealDamageInCone(ServerPlayer player, float damage, Vec3 direction, double range, double angleDegrees) {
        Vec3 playerPos = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 normalizedDir = new Vec3(direction.x, 0, direction.z).normalize();
        double cosAngle = Math.cos(Math.toRadians(angleDegrees / 2));
        
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<Entity> entities = player.level().getEntities(player, searchBox,
                e -> e instanceof LivingEntity && e != player);
        
        for (Entity entity : entities) {
            Vec3 toEntity = entity.position().subtract(playerPos).normalize();
            Vec3 toEntityHorizontal = new Vec3(toEntity.x, 0, toEntity.z).normalize();
            double dot = normalizedDir.dot(toEntityHorizontal);
            double distance = entity.position().distanceTo(playerPos);
            
            if (dot > cosAngle && distance <= range) {
                if (entity instanceof LivingEntity living) {
                    living.hurt(player.damageSources().playerAttack(player), damage);
                }
            }
        }
    }
    
    // ===== UPDATED RAIN OF ARROWS - SMALLER, MORE VISIBLE, DEFINED CIRCLE =====
    
    /**
     * Spawn Rain of Arrows activation effect - SMALLER radius with DEFINED CIRCLE (not clouds!)
     */
    private static void spawnRainOfArrowsActivationEffectNew(ServerLevel level, Vec3 center, double radius) {
        // DEFINED CIRCLE on ground - multiple bright rings
        for (int ring = 0; ring < 4; ring++) {
            double ringRadius = radius * ((double) (ring + 1) / 4);
            int points = (int) (ringRadius * 16);
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * ringRadius;
                double z = center.z + Math.sin(angle) * ringRadius;
                
                // Bright golden circle particles
                level.sendParticles(createDustParticle(1.0f, 0.85f, 0.3f, 0.8f),
                        x, center.y + 0.1, z, 1, 0.02, 0, 0.02, 0);
            }
        }
        
        // Cross pattern in center
        for (int i = -6; i <= 6; i++) {
            double offset = i * (radius / 6);
            // Horizontal line
            level.sendParticles(createDustParticle(1.0f, 0.8f, 0.2f, 0.6f),
                    center.x + offset, center.y + 0.15, center.z, 2, 0.05, 0, 0.05, 0);
            // Vertical line
            level.sendParticles(createDustParticle(1.0f, 0.8f, 0.2f, 0.6f),
                    center.x, center.y + 0.15, center.z + offset, 2, 0.05, 0, 0.05, 0);
        }
        
        // Upward beam to indicate zone
        for (int i = 0; i < 30; i++) {
            double y = center.y + 2 + i * 0.5;
            level.sendParticles(createDustParticle(1.0f, 0.9f, 0.4f, 0.7f),
                    center.x, y, center.z, 2, 0.2, 0.1, 0.2, 0.01);
        }
        
        // Flash
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y + 5, center.z, 3, 0.5, 0.5, 0.5, 0);
    }
    
    // ===== NEW MARKSMAN ABILITIES - COOLER VISUALS =====
    
    /**
     * Spawn SNIPER LASER BEAM - super cool long range particle beam
     */
    private static void spawnSniperLaserBeam(ServerLevel level, Vec3 start, Vec3 direction, LivingEntity target) {
        Vec3 normalizedDir = direction.normalize();
        Vec3 endPos = target != null ? target.position().add(0, target.getBbHeight() * 0.5, 0) : start.add(normalizedDir.scale(60.0));
        double distance = start.distanceTo(endPos);
        
        Vec3 perpVec1 = getPerpendicular(normalizedDir);
        Vec3 perpVec2 = normalizedDir.cross(perpVec1).normalize();
        
        // Main laser core - bright orange/red
        for (int i = 0; i < 180; i++) {
            double progress = (double) i / 180.0;
            Vec3 pos = start.add(normalizedDir.scale(progress * distance));
            
            // Intense core
            level.sendParticles(createDustParticle(1.0f, 0.5f, 0.1f, 0.9f),
                    pos.x, pos.y, pos.z, 2, 0.01, 0.01, 0.01, 0);
            
            // Outer glow - orange
            if (i % 2 == 0) {
                level.sendParticles(createDustParticle(1.0f, 0.6f, 0.2f, 0.5f),
                        pos.x + (RANDOM.nextDouble() - 0.5) * 0.1,
                        pos.y + (RANDOM.nextDouble() - 0.5) * 0.1,
                        pos.z + (RANDOM.nextDouble() - 0.5) * 0.1,
                        1, 0.02, 0.02, 0.02, 0);
            }
        }
        
        // Tracer effect - fast moving particles
        for (int tracer = 0; tracer < 10; tracer++) {
            double tracerProgress = RANDOM.nextDouble();
            Vec3 tracerPos = start.add(normalizedDir.scale(tracerProgress * distance));
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                    tracerPos.x, tracerPos.y, tracerPos.z, 1, 0.05, 0.05, 0.05, 0.2);
        }
        
        // Scope line effect at start
        for (int i = 0; i < 5; i++) {
            Vec3 pos = start.add(normalizedDir.scale(i * 0.3));
            level.sendParticles(createDustParticle(1.0f, 0.3f, 0.1f, 1.0f),
                    pos.x, pos.y, pos.z, 3, 0.01, 0.01, 0.01, 0);
        }
        
        // Impact burst
        if (target != null) {
            level.sendParticles(createDustParticle(1.0f, 0.4f, 0.1f, 1.2f),
                    endPos.x, endPos.y, endPos.z, 30, 0.4, 0.4, 0.4, 0.12);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                    endPos.x, endPos.y, endPos.z, 2, 0.2, 0.2, 0.2, 0);
        }
        
        // Muzzle flash
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                start.x, start.y, start.z, 1, 0, 0, 0, 0);
    }
    
    /**
     * Spawn RAILGUN BEAM - piercing particle effect
     */
    private static void spawnRailgunBeam(ServerLevel level, Vec3 start, Vec3 direction) {
        Vec3 normalizedDir = direction.normalize();
        double beamLength = 55.0;
        
        Vec3 perpVec1 = getPerpendicular(normalizedDir);
        Vec3 perpVec2 = normalizedDir.cross(perpVec1).normalize();
        
        // Main railgun beam - electric blue/cyan
        for (int i = 0; i < 200; i++) {
            double progress = (double) i / 200.0;
            Vec3 pos = start.add(normalizedDir.scale(progress * beamLength));
            
            // Core beam - bright cyan
            level.sendParticles(createDustParticle(0.3f, 0.9f, 1.0f, 0.8f),
                    pos.x, pos.y, pos.z, 2, 0.015, 0.015, 0.015, 0);
            
            // Electric arcs - zig-zag pattern
            if (i % 4 == 0) {
                double arcOffset = (RANDOM.nextDouble() - 0.5) * 0.4;
                Vec3 arcPos = pos.add(perpVec1.scale(arcOffset)).add(perpVec2.scale(arcOffset));
                level.sendParticles(createDustParticle(0.5f, 0.95f, 1.0f, 0.4f),
                        arcPos.x, arcPos.y, arcPos.z, 1, 0.05, 0.05, 0.05, 0);
            }
        }
        
        // Spiral energy around beam
        for (int spiral = 0; spiral < 40; spiral++) {
            double progress = (double) spiral / 40.0;
            double spiralAngle = progress * 8 * Math.PI;
            double spiralRadius = 0.25;
            Vec3 spiralCenter = start.add(normalizedDir.scale(progress * beamLength));
            double ox = Math.cos(spiralAngle) * spiralRadius;
            double oy = Math.sin(spiralAngle) * spiralRadius;
            Vec3 spiralPos = spiralCenter.add(perpVec1.scale(ox)).add(perpVec2.scale(oy));
            
            level.sendParticles(createDustParticle(0.4f, 0.85f, 1.0f, 0.5f),
                    spiralPos.x, spiralPos.y, spiralPos.z, 1, 0.02, 0.02, 0.02, 0);
        }
        
        // Electric sparks
        for (int i = 0; i < 25; i++) {
            double sparkProgress = RANDOM.nextDouble();
            Vec3 sparkPos = start.add(normalizedDir.scale(sparkProgress * beamLength));
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    sparkPos.x + (RANDOM.nextDouble() - 0.5) * 0.3,
                    sparkPos.y + (RANDOM.nextDouble() - 0.5) * 0.3,
                    sparkPos.z + (RANDOM.nextDouble() - 0.5) * 0.3,
                    1, 0.05, 0.05, 0.05, 0.1);
        }
        
        // End shockwave
        Vec3 endPos = start.add(normalizedDir.scale(beamLength));
        for (int ring = 0; ring < 3; ring++) {
            double radius = 0.5 + ring * 0.3;
            for (int p = 0; p < 12; p++) {
                double angle = (double) p / 12 * 2 * Math.PI;
                double ox = Math.cos(angle) * radius;
                double oy = Math.sin(angle) * radius;
                Vec3 ringPos = endPos.add(perpVec1.scale(ox)).add(perpVec2.scale(oy));
                level.sendParticles(createDustParticle(0.4f, 0.9f, 1.0f, 0.6f),
                        ringPos.x, ringPos.y, ringPos.z, 1, 0.03, 0.03, 0.03, 0);
            }
        }
        
        // Muzzle flash
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                start.x, start.y, start.z, 2, 0.1, 0.1, 0.1, 0);
    }
    
    /**
     * Spawn Hunter's Mark effect - epic targeting visuals
     */
    private static void spawnHuntersMarkEffect(ServerLevel level, Vec3 center, Entity target) {
        double targetWidth = target != null ? target.getBbWidth() : 1.0;
        double targetHeight = target != null ? target.getBbHeight() : 2.0;
        
        // Targeting reticle rings around target - red
        for (int ring = 0; ring < 5; ring++) {
            double radius = targetWidth + 0.5 + ring * 0.3;
            double y = center.y + targetHeight * 0.5 + (ring - 2) * 0.4;
            int points = 20;
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                
                level.sendParticles(createDustParticle(1.0f, 0.2f, 0.1f, 0.6f),
                        x, y, z, 1, 0.02, 0.02, 0.02, 0);
            }
        }
        
        // Crosshair on target
        double crosshairSize = targetWidth * 2;
        for (int i = -8; i <= 8; i++) {
            double offset = i * (crosshairSize / 8);
            double y = center.y + targetHeight * 0.5;
            // Horizontal
            level.sendParticles(createDustParticle(1.0f, 0.3f, 0.2f, 0.5f),
                    center.x + offset, y, center.z, 1, 0.01, 0.01, 0.01, 0);
            // Vertical (on ground plane)
            level.sendParticles(createDustParticle(1.0f, 0.3f, 0.2f, 0.5f),
                    center.x, y, center.z + offset, 1, 0.01, 0.01, 0.01, 0);
        }
        
        // Vertical beam on target
        for (int i = 0; i < 20; i++) {
            level.sendParticles(createDustParticle(1.0f, 0.4f, 0.2f, 0.4f),
                    center.x, center.y + i * 0.3, center.z, 1, 0.05, 0.05, 0.05, 0);
        }
        
        // Lock-on sparkle burst
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                center.x, center.y + targetHeight * 0.5, center.z, 20, 0.5, 0.5, 0.5, 0.15);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y + targetHeight * 0.5, center.z, 1, 0, 0, 0, 0);
    }
    
    /**
     * Spawn Execution Shot effect - dramatic kill shot visuals
     */
    private static void spawnExecutionShotEffect(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start).normalize();
        double distance = start.distanceTo(end);
        
        Vec3 perpVec1 = getPerpendicular(direction);
        Vec3 perpVec2 = direction.cross(perpVec1).normalize();
        
        // Main execution beam - deep red/crimson
        for (int i = 0; i < 100; i++) {
            double progress = (double) i / 100.0;
            Vec3 pos = start.add(direction.scale(progress * distance));
            
            // Crimson core
            level.sendParticles(createDustParticle(0.9f, 0.1f, 0.1f, 1.0f),
                    pos.x, pos.y, pos.z, 2, 0.02, 0.02, 0.02, 0);
            
            // Dark outer glow
            if (i % 2 == 0) {
                level.sendParticles(createDustParticle(0.7f, 0.1f, 0.15f, 0.6f),
                        pos.x + (RANDOM.nextDouble() - 0.5) * 0.15,
                        pos.y + (RANDOM.nextDouble() - 0.5) * 0.15,
                        pos.z + (RANDOM.nextDouble() - 0.5) * 0.15,
                        1, 0.03, 0.03, 0.03, 0);
            }
        }
        
        // MASSIVE impact explosion at target
        level.sendParticles(createDustParticle(1.0f, 0.1f, 0.05f, 1.5f),
                end.x, end.y, end.z, 50, 0.8, 0.8, 0.8, 0.2);
        
        // Skull crossbones pattern (using particles)
        // Horizontal line
        for (int i = -4; i <= 4; i++) {
            level.sendParticles(createDustParticle(0.2f, 0.2f, 0.2f, 0.8f),
                    end.x + i * 0.15, end.y, end.z, 2, 0.02, 0.02, 0.02, 0);
        }
        // Diagonal crosses
        for (int i = -3; i <= 3; i++) {
            level.sendParticles(createDustParticle(0.2f, 0.2f, 0.2f, 0.8f),
                    end.x + i * 0.12, end.y + 0.2, end.z + i * 0.12, 1, 0.02, 0.02, 0.02, 0);
            level.sendParticles(createDustParticle(0.2f, 0.2f, 0.2f, 0.8f),
                    end.x + i * 0.12, end.y + 0.2, end.z - i * 0.12, 1, 0.02, 0.02, 0.02, 0);
        }
        
        // Multiple flash effects
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                end.x, end.y, end.z, 5, 0.5, 0.5, 0.5, 0);
        
        // Crit particles for extra drama
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                end.x, end.y, end.z, 40, 0.6, 0.6, 0.6, 0.3);
        
        // Enchant particles swirling
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                end.x, end.y, end.z, 30, 0.5, 0.5, 0.5, 0.2);
    }
    
    // ===== NEW HAWKEYE VAULT ABILITY =====
    
    /**
     * Spawn Vault Turtle Scute Projectile - throws a turtle scute entity that deals damage on impact
     */
    private static void spawnVaultTurtleScuteProjectile(ServerPlayer player, ServerLevel level, Vec3 direction, float damage) {
        Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);
        
        // Create a turtle scute item entity
        net.minecraft.world.item.ItemStack scuteStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TURTLE_SCUTE);
        net.minecraft.world.entity.item.ItemEntity scuteEntity = new net.minecraft.world.entity.item.ItemEntity(
                level, startPos.x, startPos.y, startPos.z, scuteStack);
        
        // Set velocity in the look direction with arc (lobbed trajectory)
        Vec3 lobDir = new Vec3(direction.x * 1.2, direction.y + 0.4, direction.z * 1.2);
        scuteEntity.setDeltaMovement(lobDir);
        scuteEntity.setPickUpDelay(200); // Can't be picked up for 10 seconds
        scuteEntity.setGlowingTag(true); // Make it glow so it's visible
        
        // Store damage info using custom tag for tracking
        scuteEntity.getPersistentData().putFloat("rpgclasses_vault_damage", damage);
        scuteEntity.getPersistentData().putUUID("rpgclasses_vault_owner", player.getUUID());
        
        level.addFreshEntity(scuteEntity);
        
        // Spawn launch effect - cyan/teal energy trail matching Hawkeye theme
        level.sendParticles(createDustParticle(0.3f, 0.9f, 0.85f, 1.0f),
                startPos.x, startPos.y, startPos.z, 10, 0.2, 0.2, 0.2, 0.05);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                startPos.x, startPos.y, startPos.z, 5, 0.1, 0.1, 0.1, 0.03);
        
        // Schedule damage check - deal damage to nearby enemies when scute lands
        // We'll deal damage to enemies in range after a short delay (simulating impact)
        double maxDist = 12.0;
        AABB searchBox = player.getBoundingBox().inflate(maxDist);
        List<Entity> entities = player.level().getEntities(player, searchBox,
                e -> e instanceof LivingEntity && e != player);
        
        // Find target in look direction
        LivingEntity target = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity entity : entities) {
            Vec3 toEntity = entity.position().subtract(startPos);
            double horizontalDist = Math.sqrt(toEntity.x * toEntity.x + toEntity.z * toEntity.z);
            if (horizontalDist < maxDist && horizontalDist < closestDist) {
                Vec3 horizontalDir = new Vec3(direction.x, 0, direction.z).normalize();
                Vec3 toEntityHorizontal = new Vec3(toEntity.x, 0, toEntity.z).normalize();
                if (horizontalDir.dot(toEntityHorizontal) > 0.3) {
                    closestDist = horizontalDist;
                    target = (LivingEntity) entity;
                }
            }
        }
        
        // Deal damage if target found (immediate hitscan damage for gameplay responsiveness)
        if (target != null) {
            target.hurt(player.damageSources().playerAttack(player), damage);
            // Impact effect at target
            level.sendParticles(createDustParticle(0.4f, 1.0f, 0.9f, 1.0f),
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    20, 0.4, 0.4, 0.4, 0.1);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    2, 0.2, 0.2, 0.2, 0);
        }
    }
    
    // ===== BEAST MASTER MOB SUMMONING =====
    
    /**
     * Summon friendly wolves that attack nearby enemies - with unique leather armor and colored collars
     */
    private static int summonFriendlyWolves(ServerPlayer player, ServerLevel level, Vec3 center, int count) {
        int summoned = 0;
        
        // Different collar colors for variety
        net.minecraft.world.item.DyeColor[] collarColors = {
            net.minecraft.world.item.DyeColor.RED,
            net.minecraft.world.item.DyeColor.ORANGE,
            net.minecraft.world.item.DyeColor.YELLOW
        };
        
        for (int i = 0; i < count; i++) {
            double angle = (double) i / count * 2 * Math.PI;
            double spawnDist = 2.0;
            double x = center.x + Math.cos(angle) * spawnDist;
            double z = center.z + Math.sin(angle) * spawnDist;
            
            // Create wolf entity
            net.minecraft.world.entity.animal.Wolf wolf = new net.minecraft.world.entity.animal.Wolf(
                    net.minecraft.world.entity.EntityType.WOLF, level);
            wolf.setPos(x, center.y, z);
            wolf.setTame(true, false);
            wolf.setOwnerUUID(player.getUUID());
            
            // Make each wolf unique with different collar colors

            
            // Set wolf to be aggressive (angry mode)
            wolf.setRemainingPersistentAngerTime(400); // Stay angry for 20 seconds
            
            // Make wolf aggressive toward nearby enemies
            AABB searchBox = wolf.getBoundingBox().inflate(15.0);
            List<Entity> enemies = level.getEntities(wolf, searchBox,
                    e -> e instanceof net.minecraft.world.entity.monster.Monster);
            
            if (!enemies.isEmpty()) {
                Entity target = enemies.get(RANDOM.nextInt(enemies.size()));
                if (target instanceof LivingEntity living) {
                    wolf.setTarget(living);
                }
            }
            
            // Give wolf temporary strength and speed - more powerful than normal wolves
            wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 2));
            wolf.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 2));
            wolf.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400, 0));
            wolf.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0));
            
            // Mark as summoned beast
            wolf.getPersistentData().putBoolean("rpgclasses_summoned_beast", true);
            wolf.getPersistentData().putLong("rpgclasses_summon_time", level.getGameTime());
            
            if (level.addFreshEntity(wolf)) {
                summoned++;
                
                // Spawn effect at wolf location - orange/red beast master theme
                level.sendParticles(createDustParticle(0.9f, 0.5f, 0.3f, 1.0f),
                        x, center.y + 0.5, z, 15, 0.3, 0.3, 0.3, 0.1);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                        x, center.y, z, 8, 0.3, 0.5, 0.3, 0.02);
            }
        }
        
        return summoned;
    }
    
    /**
     * Summon a friendly bear (using Polar Bear - tanky beast companion)
     */
    private static boolean summonFriendlyBear(ServerPlayer player, ServerLevel level, Vec3 center) {
        // Use Polar Bear for the bear companion - fits the beast master theme better
        net.minecraft.world.entity.animal.PolarBear bear = new net.minecraft.world.entity.animal.PolarBear(
                net.minecraft.world.entity.EntityType.POLAR_BEAR, level);
        
        double spawnX = center.x + (RANDOM.nextDouble() - 0.5) * 2;
        double spawnZ = center.z + (RANDOM.nextDouble() - 0.5) * 2;
        bear.setPos(spawnX, center.y, spawnZ);
        
        // Make the bear a baby (cuter and more unique) or adult for power - alternate
        if (RANDOM.nextBoolean()) {
            bear.setBaby(false); // Adult bear for power
        }
        
        // Give bear powerful buffs to make it strong
        bear.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 2));
        bear.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 2));
        bear.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0));
        bear.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
        bear.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 600, 2)); // Extra tanky
        bear.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
        
        // Mark as summoned beast and make it persistent
        bear.getPersistentData().putBoolean("rpgclasses_summoned_beast", true);
        bear.getPersistentData().putLong("rpgclasses_summon_time", level.getGameTime());
        bear.getPersistentData().putUUID("rpgclasses_owner", player.getUUID());
        
        // Make bear angry so it attacks
        bear.setRemainingPersistentAngerTime(600);
        
        // Find and target nearby enemy
        AABB searchBox = bear.getBoundingBox().inflate(20.0);
        List<Entity> enemies = level.getEntities(bear, searchBox,
                e -> e instanceof net.minecraft.world.entity.monster.Monster);
        
        if (!enemies.isEmpty()) {
            Entity target = enemies.get(RANDOM.nextInt(enemies.size()));
            if (target instanceof LivingEntity living) {
                bear.setTarget(living);
                bear.setPersistentAngerTarget(target.getUUID());
            }
        }
        
        if (level.addFreshEntity(bear)) {
            // Epic spawn effect - brown/earthy for bear theme
            level.sendParticles(createDustParticle(0.6f, 0.4f, 0.25f, 1.2f),
                    spawnX, center.y + 1, spawnZ, 30, 0.5, 0.8, 0.5, 0.15);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                    spawnX, center.y + 1, spawnZ, 2, 0.3, 0.3, 0.3, 0);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    spawnX, center.y, spawnZ, 15, 0.4, 0.2, 0.4, 0.02);
            // Ground shake effect
            for (int ring = 0; ring < 3; ring++) {
                double radius = 1.0 + ring * 0.5;
                for (int p = 0; p < 12; p++) {
                    double angle = (double) p / 12 * 2 * Math.PI;
                    level.sendParticles(createDustParticle(0.5f, 0.35f, 0.2f, 0.6f),
                            spawnX + Math.cos(angle) * radius, center.y + 0.1, spawnZ + Math.sin(angle) * radius,
                            1, 0.05, 0.02, 0.05, 0.02);
                }
            }
            return true;
        }
        return false;
    }
    
    /**
     * Summon a friendly eagle (using Allay - flying scout that follows player)
     * Eagle provides Night Vision and marks enemies with Glowing + has special swooping ability
     */
    private static boolean summonFriendlyEagle(ServerPlayer player, ServerLevel level, Vec3 center) {
        // Use Allay as the "eagle" - flying creature that follows player and glows
        net.minecraft.world.entity.animal.allay.Allay eagle = new net.minecraft.world.entity.animal.allay.Allay(
                net.minecraft.world.entity.EntityType.ALLAY, level);
        
        double spawnX = center.x;
        double spawnY = center.y + 2.0;
        double spawnZ = center.z;
        eagle.setPos(spawnX, spawnY, spawnZ);
        
        // Give eagle buffs and make it visible
        eagle.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0));
        eagle.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 2));
        eagle.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0)); // Unique - regenerates
        
        // Mark as summoned beast with special eagle ability flag
        eagle.getPersistentData().putBoolean("rpgclasses_summoned_beast", true);
        eagle.getPersistentData().putBoolean("rpgclasses_eagle_scout", true); // Special eagle marker
        eagle.getPersistentData().putLong("rpgclasses_summon_time", level.getGameTime());
        eagle.getPersistentData().putUUID("rpgclasses_owner", player.getUUID());
        eagle.getPersistentData().putLong("rpgclasses_last_swoop", 0L); // Track last swoop time
        
        // Give the allay a special item to hold (feather for eagle theme)
        net.minecraft.world.item.ItemStack featherStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FEATHER);
        eagle.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, featherStack);
        
        if (level.addFreshEntity(eagle)) {
            // Epic spawn effect - cyan/white for sky theme
            level.sendParticles(createDustParticle(0.5f, 0.85f, 1.0f, 0.8f),
                    spawnX, spawnY, spawnZ, 25, 0.5, 0.5, 0.5, 0.15);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    spawnX, spawnY, spawnZ, 12, 0.4, 0.3, 0.4, 0.04);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    spawnX, spawnY, spawnZ, 8, 0.3, 0.3, 0.3, 0.03);
            
            // Feather/wing trail effect
            for (int i = 0; i < 10; i++) {
                double angle = i * Math.PI / 5;
                double wingRadius = 0.8;
                level.sendParticles(createDustParticle(0.9f, 0.95f, 1.0f, 0.6f),
                        spawnX + Math.cos(angle) * wingRadius,
                        spawnY + 0.2,
                        spawnZ + Math.sin(angle) * wingRadius,
                        2, 0.1, 0.1, 0.1, 0.02);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Summon stampeding beasts in 3 SYNCHRONIZED LINES moving in formation
     */
    private static int summonStampedBeasts(ServerPlayer player, ServerLevel level, Vec3 center, Vec3 direction, int totalCount) {
        int summoned = 0;
        Vec3 perpDir = direction.cross(new Vec3(0, 1, 0)).normalize();
        
        // Collar colors for wolves
        net.minecraft.world.item.DyeColor[] collarColors = {
            net.minecraft.world.item.DyeColor.RED,
            net.minecraft.world.item.DyeColor.ORANGE,
            net.minecraft.world.item.DyeColor.BLACK,
            net.minecraft.world.item.DyeColor.BROWN,
            net.minecraft.world.item.DyeColor.GRAY
        };
        
        // Create 3 lines: left, center, right
        int beastsPerLine = Math.max(3, (totalCount + 2) / 3); // Ensure all beasts are distributed
        double lineSpacing = 2.5; // Distance between left-center and center-right lines
        
        // Left line offset
        Vec3 leftLineOffset = perpDir.scale(-lineSpacing);
        // Center line offset
        Vec3 centerLineOffset = new Vec3(0, 0, 0);
        // Right line offset
        Vec3 rightLineOffset = perpDir.scale(lineSpacing);
        
        // Spawn left line
        summoned += spawnStampedeLine(player, level, center.add(leftLineOffset), direction, beastsPerLine, collarColors, 0);
        // Spawn center line
        summoned += spawnStampedeLine(player, level, center.add(centerLineOffset), direction, beastsPerLine, collarColors, 1);
        // Spawn right line
        summoned += spawnStampedeLine(player, level, center.add(rightLineOffset), direction, beastsPerLine, collarColors, 2);
        
        return summoned;
    }
    
    /**
     * Spawn a single line of stampeding beasts in formation
     */
    private static int spawnStampedeLine(ServerPlayer player, ServerLevel level, Vec3 lineStart, Vec3 direction, 
            int count, net.minecraft.world.item.DyeColor[] collarColors, int lineIndex) {
        int summoned = 0;
        
        for (int i = 0; i < count; i++) {
            // Stagger beasts along the line (depth spacing)
            double depthOffset = i * 1.2;
            Vec3 spawnPos = lineStart.add(direction.scale(depthOffset));
            double spawnX = spawnPos.x;
            double spawnZ = spawnPos.z;
            
            // Determine beast type based on line - each line gets a specific type for consistency
            net.minecraft.world.entity.Mob beast = null;
            
            if (lineIndex == 0) {
                // Left line: Wolves
                net.minecraft.world.entity.animal.Wolf wolf = new net.minecraft.world.entity.animal.Wolf(
                        net.minecraft.world.entity.EntityType.WOLF, level);
                wolf.setPos(spawnX, lineStart.y, spawnZ);
                wolf.setTame(true, false);
                wolf.setOwnerUUID(player.getUUID());

                wolf.setRemainingPersistentAngerTime(200);
                wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 2));
                wolf.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2));
                wolf.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                beast = wolf;
            } else if (lineIndex == 1) {
                // Center line: Goats (they ram!)
                net.minecraft.world.entity.animal.goat.Goat goat = new net.minecraft.world.entity.animal.goat.Goat(
                        net.minecraft.world.entity.EntityType.GOAT, level);
                goat.setPos(spawnX, lineStart.y, spawnZ);
                goat.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 3));
                goat.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2));
                goat.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                beast = goat;
            } else {
                // Right line: Rabbits (fast hoppers)
                net.minecraft.world.entity.animal.Rabbit rabbit = new net.minecraft.world.entity.animal.Rabbit(
                        net.minecraft.world.entity.EntityType.RABBIT, level);
                rabbit.setPos(spawnX, lineStart.y, spawnZ);
                rabbit.setVariant(net.minecraft.world.entity.animal.Rabbit.Variant.values()[i % 6]);
                rabbit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 4));
                rabbit.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 2));
                rabbit.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                beast = rabbit;
            }
            
            if (beast != null) {
                // Mark as stampede beast
                beast.getPersistentData().putBoolean("rpgclasses_stampede_beast", true);
                beast.getPersistentData().putInt("rpgclasses_stampede_line", lineIndex);
                
                // Synchronized movement - all beasts in same line move at same speed
                double STAMPEDE_SPEED_VARIATION = 0.2; // Speed difference per line for visual variety
                double baseSpeed = 1.5 + (lineIndex * STAMPEDE_SPEED_VARIATION);
                beast.setDeltaMovement(direction.scale(baseSpeed).add(0, 0.3, 0));
                beast.hurtMarked = true;
                
                if (level.addFreshEntity(beast)) {
                    summoned++;
                }
            }
        }
        
        return summoned;
    }
    
    // ===== BEAST MASTER VISUAL EFFECTS =====
    
    /**
     * Spawn wolf summoning effect
     */
    private static void spawnWolfSummonEffect(ServerLevel level, Vec3 center) {
        // Spirit circle on ground
        for (int ring = 0; ring < 3; ring++) {
            double radius = 1.5 + ring * 0.5;
            int points = 24;
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                
                level.sendParticles(createDustParticle(0.5f, 0.5f, 0.55f, 0.6f),
                        x, center.y + 0.1, z, 1, 0.03, 0, 0.03, 0);
            }
        }
        
        // Wolf spirit trails rising up
        for (int wolf = 0; wolf < 3; wolf++) {
            double angle = wolf * 2 * Math.PI / 3;
            double dist = 1.5;
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            
            for (int i = 0; i < 15; i++) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                        x, center.y + i * 0.15, z, 1, 0.1, 0.1, 0.1, 0.01);
            }
        }
        
        // Central howl effect
        for (int i = 0; i < 20; i++) {
            level.sendParticles(createDustParticle(0.6f, 0.6f, 0.65f, 0.5f),
                    center.x + (RANDOM.nextDouble() - 0.5) * 0.5,
                    center.y + 1 + i * 0.1,
                    center.z + (RANDOM.nextDouble() - 0.5) * 0.5,
                    1, 0.1, 0.1, 0.1, 0.02);
        }
        
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y + 1, center.z, 2, 0.3, 0.3, 0.3, 0);
    }
    
    /**
     * Spawn bear summoning effect
     */
    private static void spawnBearSummonEffect(ServerLevel level, Vec3 center) {
        // Ground tremor effect
        for (int ring = 0; ring < 5; ring++) {
            double radius = 0.5 + ring * 0.5;
            int points = 16;
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                
                level.sendParticles(createDustParticle(0.55f, 0.35f, 0.2f, 0.7f),
                        x, center.y + 0.1 + ring * 0.1, z, 1, 0.05, 0.1, 0.05, 0.02);
            }
        }
        
        // Earthy eruption
        for (int i = 0; i < 30; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = RANDOM.nextDouble() * 1.5;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    center.x + Math.cos(angle) * dist,
                    center.y + RANDOM.nextDouble() * 1.5,
                    center.z + Math.sin(angle) * dist,
                    1, 0.1, 0.2, 0.1, 0.01);
        }
        
        // Powerful stomp effect
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                center.x, center.y + 0.5, center.z, 1, 0, 0, 0, 0);
    }
    
    /**
     * Spawn eagle summoning effect
     */
    private static void spawnEagleSummonEffect(ServerLevel level, Vec3 center) {
        // Upward wind spiral
        for (int i = 0; i < 40; i++) {
            double progress = (double) i / 40;
            double spiralAngle = progress * 4 * Math.PI;
            double spiralRadius = 0.5 + progress * 1.5;
            double x = center.x + Math.cos(spiralAngle) * spiralRadius;
            double y = center.y + progress * 5;
            double z = center.z + Math.sin(spiralAngle) * spiralRadius;
            
            level.sendParticles(createDustParticle(0.5f, 0.75f, 1.0f, 0.6f),
                    x, y, z, 1, 0.05, 0.1, 0.05, 0.02);
        }
        
        // Feather cloud
        for (int i = 0; i < 15; i++) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    center.x + (RANDOM.nextDouble() - 0.5) * 2,
                    center.y + 2 + RANDOM.nextDouble() * 2,
                    center.z + (RANDOM.nextDouble() - 0.5) * 2,
                    1, 0.1, 0.1, 0.1, 0.02);
        }
        
        // Detection pulse expanding outward
        for (int ring = 0; ring < 6; ring++) {
            double radius = ring * 3.0;
            int points = (int) (radius * 6);
            if (points < 8) points = 8;
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                
                level.sendParticles(createDustParticle(0.4f, 0.7f, 1.0f, 0.4f),
                        x, center.y + 2, z, 1, 0.05, 0.05, 0.05, 0);
            }
        }
        
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y + 3, center.z, 2, 0.5, 0.5, 0.5, 0);
    }
    
    /**
     * Spawn beast stampede effect - epic charging visuals
     */
    private static void spawnBeastStampedeEffect(ServerLevel level, Vec3 center, Vec3 direction) {
        Vec3 perpDir = direction.cross(new Vec3(0, 1, 0)).normalize();
        
        // Massive dust cloud along stampede path
        for (int dist = 0; dist < 18; dist++) {
            Vec3 pos = center.add(direction.scale(dist));
            
            // Wide dust trail
            for (int w = -3; w <= 3; w++) {
                Vec3 particlePos = pos.add(perpDir.scale(w * 0.8));
                
                // Thick dust clouds
                level.sendParticles(createDustParticle(0.6f, 0.45f, 0.3f, 0.9f),
                        particlePos.x + (RANDOM.nextDouble() - 0.5) * 0.5,
                        particlePos.y + 0.3 + RANDOM.nextDouble() * 0.8,
                        particlePos.z + (RANDOM.nextDouble() - 0.5) * 0.5,
                        3, 0.25, 0.2, 0.25, 0.04);
                
                // Ground impact
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                        particlePos.x, particlePos.y + 0.1, particlePos.z,
                        1, 0.15, 0.02, 0.15, 0.02);
            }
            
            // Beast spirit silhouettes
            if (dist % 2 == 0) {
                for (int h = 0; h < 6; h++) {
                    double sideOffset = (RANDOM.nextDouble() - 0.5) * 4;
                    Vec3 spiritPos = pos.add(perpDir.scale(sideOffset));
                    level.sendParticles(createDustParticle(0.3f, 0.25f, 0.2f, 0.9f),
                            spiritPos.x,
                            spiritPos.y + 0.4 + h * 0.25,
                            spiritPos.z,
                            1, 0.15, 0.1, 0.15, 0.03);
                }
            }
        }
        
        // Powerful shockwave at origin
        for (int ring = 0; ring < 5; ring++) {
            double radius = 1.0 + ring * 1.2;
            int points = (int) (radius * 10);
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                
                level.sendParticles(createDustParticle(0.55f, 0.4f, 0.25f, 0.6f),
                        x, center.y + 0.3 + ring * 0.2, z, 1, 0.08, 0.12, 0.08, 0.03);
            }
        }
        
        // Thunder effect for epic impact
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y + 1, center.z, 3, 0.5, 0.5, 0.5, 0);
        
        // Explosion at start
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                center.x + direction.x, center.y + 0.5, center.z + direction.z, 1, 0, 0, 0, 0);
    }
    
    // ===== NEW MARKSMAN HELPER METHODS =====
    
    /**
     * Find the nearest enemy to the player (not just in sight, but closest overall)
     */
    private static LivingEntity findNearestEnemy(ServerPlayer player, double maxRange) {
        AABB searchBox = player.getBoundingBox().inflate(maxRange);
        List<Entity> entities = player.level().getEntities(player, searchBox,
                e -> e instanceof LivingEntity && !(e instanceof ServerPlayer) && e != player);
        
        LivingEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living && living.isAlive()) {
                double dist = player.position().distanceTo(entity.position());
                if (dist < nearestDist && dist <= maxRange) {
                    nearestDist = dist;
                    nearest = living;
                }
            }
        }
        
        return nearest;
    }
    
    /**
     * Spawn RED LASER SIGHT between player and target (particle line with pulse effect)
     */
    private static void spawnHeadshotLaserSight(ServerLevel level, Vec3 start, Vec3 end, int chargeTime, int maxChargeTime) {
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        direction = direction.normalize();
        
        // Calculate intensity based on charge (pulses faster as charge increases)
        float chargePercent = (float) chargeTime / maxChargeTime;
        float intensity = 0.6f + (chargePercent * 0.4f); // 0.6 to 1.0
        
        // Red laser line (intense red gradient)
        int particleCount = (int) (distance * 5); // Dense line
        for (int i = 0; i < particleCount; i++) {
            double progress = (double) i / particleCount;
            Vec3 pos = start.add(direction.scale(progress * distance));
            
            // Core red laser (brighter as charge increases)
            level.sendParticles(createDustParticle(1.0f, 0.05f, 0.05f, intensity),
                    pos.x, pos.y, pos.z, 1, 0.01, 0.01, 0.01, 0);
            
            // Pulse effect - outer red glow
            if (i % 3 == 0) {
                level.sendParticles(createDustParticle(0.9f, 0.1f, 0.1f, intensity * 0.5f),
                        pos.x + (RANDOM.nextDouble() - 0.5) * 0.05,
                        pos.y + (RANDOM.nextDouble() - 0.5) * 0.05,
                        pos.z + (RANDOM.nextDouble() - 0.5) * 0.05,
                        1, 0.02, 0.02, 0.02, 0);
            }
        }
        
        // Targeting reticle at target head (intensifies with charge)
        double reticleRadius = 0.3;
        int reticlePoints = 12;
        for (int i = 0; i < reticlePoints; i++) {
            double angle = (double) i / reticlePoints * 2 * Math.PI;
            double x = end.x + Math.cos(angle) * reticleRadius;
            double z = end.z + Math.sin(angle) * reticleRadius;
            
            level.sendParticles(createDustParticle(1.0f, 0.1f, 0.1f, intensity),
                    x, end.y, z, 1, 0.01, 0.01, 0.01, 0);
        }
    }
    
    /**
     * Check if an entity is marked
     */
    private static boolean isMarkedEnemy(LivingEntity entity) {
        return markedEnemies.containsKey(entity.getUUID());
    }
    
    /**
     * Find a grapple point (block or entity) in the look direction
     */
    private static Vec3 findGrapplePoint(ServerPlayer player, Vec3 direction, double maxRange) {
        Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 endPos = startPos.add(direction.scale(maxRange));
        
        // Raycast for blocks
        var blockHit = player.level().clip(new net.minecraft.world.level.ClipContext(
                startPos, endPos,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                player));
        
        if (blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return blockHit.getLocation();
        }
        
        // If no block, check for entities
        AABB searchBox = player.getBoundingBox().inflate(maxRange);
        List<Entity> entities = player.level().getEntities(player, searchBox,
                e -> e instanceof LivingEntity && e != player);
        
        double closestDist = Double.MAX_VALUE;
        Vec3 closestPoint = null;
        
        for (Entity entity : entities) {
            Vec3 toEntity = entity.position().subtract(startPos);
            double dot = direction.dot(toEntity.normalize());
            if (dot > 0.8) { // Must be roughly in look direction
                double dist = toEntity.length();
                if (dist < closestDist && dist < maxRange) {
                    closestDist = dist;
                    closestPoint = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
                }
            }
        }
        
        return closestPoint;
    }
    
    /**
     * Update Snipe projectile - returns false when it should be removed
     */
    private static boolean updateSnipeProjectile(SnipeProjectile snipe) {
        if (snipe.ticksAlive >= snipe.maxTicks) {
            return false;
        }
        
        // Move projectile
        snipe.position = snipe.position.add(snipe.direction.scale(snipe.speed));
        
        // Check for entity collision
        AABB hitbox = new AABB(
                snipe.position.x - 0.5, snipe.position.y - 0.5, snipe.position.z - 0.5,
                snipe.position.x + 0.5, snipe.position.y + 0.5, snipe.position.z + 0.5);
        
        List<Entity> entities = snipe.level.getEntities(snipe.owner, hitbox,
                e -> e instanceof LivingEntity && e != snipe.owner);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                // Apply mark bonus if target is marked
                float finalDamage = snipe.damage;
                if (isMarkedEnemy(living)) {
                    finalDamage *= MARK_DAMAGE_BONUS;
                }
                
                living.hurt(snipe.owner.damageSources().playerAttack(snipe.owner), finalDamage);
                
                // Impact effect
                spawnSnipeImpactEffect(snipe.level, snipe.position);
                return false;
            }
        }
        
        // Check for block collision
        var blockHit = snipe.level.clip(new net.minecraft.world.level.ClipContext(
                snipe.position.subtract(snipe.direction.scale(0.5)),
                snipe.position,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                snipe.owner));
        
        if (blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            spawnSnipeImpactEffect(snipe.level, blockHit.getLocation());
            return false;
        }
        
        // Spawn trail particles
        if (snipe.ticksAlive % 2 == 0) {
            spawnSnipeTrailParticle(snipe.level, snipe.position, snipe.direction);
        }
        
        return true;
    }
    
    /**
     * Update Headshot charge - shows laser sight, roots player, increases charge
     * Returns false when charge should be removed (target dies, max charge, etc.)
     */
    private static boolean updateHeadshotCharge(HeadshotCharge charge) {
        // Check if target is still alive
        if (charge.target == null || !charge.target.isAlive()) {
            charge.owner.displayClientMessage(Component.literal("§eTarget lost!"), true);
            // Un-root player
            charge.owner.setDeltaMovement(charge.owner.getDeltaMovement());
            return false;
        }
        
        // Check if player is still valid
        if (!charge.owner.isAlive()) {
            return false;
        }
        
        // Increase charge time
        charge.chargeTime++;
        
        // Root player (prevent movement)
        charge.owner.setDeltaMovement(charge.owner.getDeltaMovement().x * 0.0, charge.owner.getDeltaMovement().y, charge.owner.getDeltaMovement().z * 0.0);
        
        // Show RED LASER SIGHT between player and target
        Vec3 playerEyePos = charge.owner.position().add(0, charge.owner.getEyeHeight(), 0);
        Vec3 targetHeadPos = charge.target.position().add(0, charge.target.getBbHeight() * 0.8, 0);
        spawnHeadshotLaserSight(charge.level, playerEyePos, targetHeadPos, charge.chargeTime, charge.maxChargeTime);
        
        // Show charge indication particles at player
        if (charge.chargeTime % 5 == 0) {
            float chargePercent = (float) charge.chargeTime / charge.maxChargeTime;
            int particleCount = (int) (chargePercent * 20); // Growing particles
            charge.level.sendParticles(createDustParticle(1.0f, 0.1f, 0.1f, 0.8f),
                    charge.owner.getX(), charge.owner.getY() + 1.0, charge.owner.getZ(),
                    particleCount, 0.3, 0.3, 0.3, 0.05);
        }
        
        // Auto-fire at max charge
        if (charge.chargeTime >= charge.maxChargeTime) {
            // Max charge reached - auto fire
            float baseDamage = charge.baseDamage;
            float damageMultiplier = charge.getDamageMultiplier(); // Should be 2.0 (100% bonus)
            
            // Check for marked enemy bonus
            float markBonus = isMarkedEnemy(charge.target) ? MARK_DAMAGE_BONUS : 1.0f;
            
            // Apply damage with full charge multiplier
            float finalDamage = baseDamage * markBonus * damageMultiplier;
            
            // Deal hitscan damage
            charge.target.hurt(charge.owner.damageSources().playerAttack(charge.owner), finalDamage);
            
            // Check if target was killed
            boolean targetKilled = !charge.target.isAlive() || charge.target.getHealth() <= 0;
            
            // Spawn headshot hitscan visual
            spawnHeadshotEffect(charge.level, playerEyePos, targetHeadPos);
            
            if (targetKilled) {
                // Reset all other cooldowns on kill
                var rpgData = charge.owner.getData(ModAttachments.PLAYER_RPG);
                String ability1Id = "marksman_ability_1";
                String ability2Id = "marksman_ability_2";
                String ability3Id = "marksman_ability_3";
                rpgData.setAbilityCooldown(ability1Id, 0);
                rpgData.setAbilityCooldown(ability2Id, 0);
                rpgData.setAbilityCooldown(ability3Id, 0);
                
                // Speed buff on kill
                charge.owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
                
                // Sync cooldowns to client
                sendToPlayer(new PacketSyncCooldowns(rpgData.getAllCooldowns()), charge.owner);
                
                charge.owner.displayClientMessage(Component.literal("§c§l☠ HEADSHOT KILL! §r§a(All cooldowns reset! +100% damage)"), true);
            } else {
                charge.owner.displayClientMessage(Component.literal("§c§l☠ FULL CHARGE HEADSHOT! §r§7(+100% damage: " + String.format("%.1f", finalDamage) + ")"), true);
            }
            
            return false; // Remove charge
        }
        
        // Continue charging
        return true;
    }
    
    /**
     * Spawn mark visual effect (periodic)
     */
    private static void spawnMarkVisual(MarkedEnemy marked) {
        if (marked.target.level() instanceof ServerLevel level) {
            Vec3 pos = marked.target.position();
            double height = marked.target.getBbHeight();
            
            // Red targeting ring around marked enemy
            double radius = marked.target.getBbWidth() + 0.3;
            for (int p = 0; p < 12; p++) {
                double angle = (double) p / 12 * 2 * Math.PI;
                double x = pos.x + Math.cos(angle) * radius;
                double z = pos.z + Math.sin(angle) * radius;
                
                level.sendParticles(createDustParticle(1.0f, 0.2f, 0.2f, 0.5f),
                        x, pos.y + height * 0.5, z, 1, 0.02, 0.02, 0.02, 0);
            }
            
            // Red particles above head
            level.sendParticles(createDustParticle(1.0f, 0.3f, 0.2f, 0.6f),
                    pos.x, pos.y + height + 0.3, pos.z, 2, 0.1, 0.1, 0.1, 0.01);
        }
    }
    
    /**
     * Spawn Snipe hitscan effect - subtle dust gradient targeting line (NO lava, NO flash)
     */
    private static void spawnSnipeHitscanEffect(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        direction = direction.normalize();
        
        // Subtle targeting line with dust gradient (orange → red → dark red)
        int particleCount = (int) (distance * 3); // Smooth line
        for (int i = 0; i < particleCount; i++) {
            double progress = (double) i / particleCount;
            Vec3 pos = start.add(direction.scale(progress * distance));
            
            // Gradient from orange to dark red along the line
            float r = 1.0f;
            float g = 0.5f - (float) progress * 0.4f; // 0.5 → 0.1
            float b = 0.1f - (float) progress * 0.05f; // 0.1 → 0.05
            
            level.sendParticles(createDustParticle(r, g, b, 0.4f), // Subtle opacity
                    pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0);
        }
        
        // Impact point - subtle glow (if target hit)
        if (distance < 80.0) { // Only if we hit something close
            level.sendParticles(createDustParticle(1.0f, 0.2f, 0.1f, 0.8f),
                    end.x, end.y, end.z, 8, 0.2, 0.2, 0.2, 0.05);
        }
    }
    
    /**
     * Spawn Snipe launch effect (DEPRECATED - now using hitscan)
     */
    private static void spawnSnipeLaunchEffect(ServerLevel level, Vec3 start, Vec3 direction) {
        // Orange/red muzzle flash
        level.sendParticles(createDustParticle(1.0f, 0.5f, 0.1f, 1.2f),
                start.x, start.y, start.z, 20, 0.15, 0.15, 0.15, 0.05);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                start.x, start.y, start.z, 2, 0.1, 0.1, 0.1, 0);
        
        // Initial projectile beam (short)
        for (int i = 0; i < 10; i++) {
            Vec3 pos = start.add(direction.scale(i * 0.3));
            level.sendParticles(createDustParticle(1.0f, 0.6f, 0.2f, 0.8f),
                    pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0);
        }
    }
    
    /**
     * Spawn Snipe trail particle
     */
    private static void spawnSnipeTrailParticle(ServerLevel level, Vec3 position, Vec3 direction) {
        // Glowing orange/red core
        level.sendParticles(createDustParticle(1.0f, 0.5f, 0.15f, 0.9f),
                position.x, position.y, position.z, 3, 0.08, 0.08, 0.08, 0);
        
        // Fire trail
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME,
                position.x, position.y, position.z, 1, 0.02, 0.02, 0.02, 0.01);
        
        // Crit sparkles
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                position.x, position.y, position.z, 2, 0.1, 0.1, 0.1, 0.02);
    }
    
    /**
     * Spawn Snipe impact effect
     */
    private static void spawnSnipeImpactEffect(ServerLevel level, Vec3 position) {
        // Large explosion of orange/red particles
        level.sendParticles(createDustParticle(1.0f, 0.4f, 0.1f, 1.2f),
                position.x, position.y, position.z, 30, 0.5, 0.5, 0.5, 0.15);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                position.x, position.y, position.z, 3, 0.3, 0.3, 0.3, 0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                position.x, position.y, position.z, 15, 0.3, 0.3, 0.3, 0.05);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,
                position.x, position.y, position.z, 5, 0.2, 0.2, 0.2, 0);
    }
    
    /**
     * Spawn Mark applied effect
     */
    private static void spawnMarkAppliedEffect(ServerLevel level, Vec3 position, Entity target) {
        double targetHeight = target.getBbHeight();
        double targetWidth = target.getBbWidth();
        
        // Red targeting reticle
        for (int ring = 0; ring < 3; ring++) {
            double radius = targetWidth + 0.5 + ring * 0.2;
            for (int p = 0; p < 16; p++) {
                double angle = (double) p / 16 * 2 * Math.PI;
                double x = position.x + Math.cos(angle) * radius;
                double z = position.z + Math.sin(angle) * radius;
                
                level.sendParticles(createDustParticle(1.0f, 0.15f, 0.1f, 0.7f),
                        x, position.y + targetHeight * 0.5, z, 1, 0.02, 0.02, 0.02, 0);
            }
        }
        
        // Crosshair
        for (int i = -5; i <= 5; i++) {
            double offset = i * 0.3;
            // Horizontal
            level.sendParticles(createDustParticle(1.0f, 0.2f, 0.15f, 0.5f),
                    position.x + offset, position.y + targetHeight * 0.5, position.z,
                    1, 0.01, 0.01, 0.01, 0);
            // Vertical
            level.sendParticles(createDustParticle(1.0f, 0.2f, 0.15f, 0.5f),
                    position.x, position.y + targetHeight * 0.5, position.z + offset,
                    1, 0.01, 0.01, 0.01, 0);
        }
        
        // Lock-on flash
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                position.x, position.y + targetHeight * 0.5, position.z, 2, 0.1, 0.1, 0.1, 0);
    }
    
    /**
     * Spawn Grapple effect - brown→gray arc with connecting points
     */
    private static void spawnGrappleEffect(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        direction = direction.normalize();
        
        // Create arcing path (parabola) for more dynamic visual
        int pointCount = (int) (distance * 5);
        for (int i = 0; i < pointCount; i++) {
            double progress = (double) i / pointCount;
            
            // Parabolic arc calculation
            double arcHeight = distance * 0.15; // Arc peaks at 15% of distance
            double y_offset = 4 * arcHeight * progress * (1 - progress);
            
            Vec3 pos = start.add(direction.scale(progress * distance));
            pos = pos.add(0, y_offset, 0);
            
            // Gradient from brown to gray along the rope
            float brown_r = 0.6f - (float) progress * 0.2f; // 0.6 → 0.4
            float brown_g = 0.4f - (float) progress * 0.1f; // 0.4 → 0.3
            float brown_b = 0.25f + (float) progress * 0.15f; // 0.25 → 0.4
            
            // Main chain/rope with gradient
            level.sendParticles(createDustParticle(brown_r, brown_g, brown_b, 0.8f),
                    pos.x, pos.y, pos.z, 2, 0.03, 0.03, 0.03, 0);
            
            // Connection points (nodes) every few particles
            if (i % 5 == 0) {
                level.sendParticles(createDustParticle(0.5f, 0.5f, 0.55f, 1.0f), // Gray nodes
                        pos.x, pos.y, pos.z, 3, 0.05, 0.05, 0.05, 0.01);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0);
            }
        }
        
        // Hook impact at end - metallic gray burst
        level.sendParticles(createDustParticle(0.5f, 0.5f, 0.55f, 1.2f),
                end.x, end.y, end.z, 20, 0.4, 0.4, 0.4, 0.1);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                end.x, end.y, end.z, 15, 0.3, 0.3, 0.3, 0.15);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                end.x, end.y, end.z, 1, 0.1, 0.1, 0.1, 0);
        
        // Launch effect at start - brown dust cloud
        level.sendParticles(createDustParticle(0.6f, 0.4f, 0.25f, 0.9f),
                start.x, start.y, start.z, 12, 0.25, 0.25, 0.25, 0.03);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                start.x, start.y, start.z, 8, 0.2, 0.2, 0.2, 0.02);
    }
    
    /**
     * Spawn Headshot effect
     */
    private static void spawnHeadshotEffect(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start).normalize();
        double distance = start.distanceTo(end);
        
        // Red laser sight beam
        for (int i = 0; i < 100; i++) {
            double progress = (double) i / 100.0;
            Vec3 pos = start.add(direction.scale(progress * distance));
            
            // Intense red core
            level.sendParticles(createDustParticle(1.0f, 0.1f, 0.1f, 0.8f),
                    pos.x, pos.y, pos.z, 2, 0.015, 0.015, 0.015, 0);
            
            // Darker red outer
            if (i % 3 == 0) {
                level.sendParticles(createDustParticle(0.8f, 0.1f, 0.1f, 0.5f),
                        pos.x + (RANDOM.nextDouble() - 0.5) * 0.1,
                        pos.y + (RANDOM.nextDouble() - 0.5) * 0.1,
                        pos.z + (RANDOM.nextDouble() - 0.5) * 0.1,
                        1, 0.02, 0.02, 0.02, 0);
            }
        }
        
        // Massive impact at head
        level.sendParticles(createDustParticle(1.0f, 0.15f, 0.1f, 1.5f),
                end.x, end.y, end.z, 40, 0.6, 0.6, 0.6, 0.2);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                end.x, end.y, end.z, 5, 0.4, 0.4, 0.4, 0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                end.x, end.y, end.z, 30, 0.5, 0.5, 0.5, 0.25);
        
        // Skull indicator (using dark particles in X pattern)
        for (int i = -3; i <= 3; i++) {
            double offset = i * 0.12;
            level.sendParticles(createDustParticle(0.1f, 0.1f, 0.1f, 0.9f),
                    end.x + offset, end.y + 0.3, end.z + offset, 1, 0.02, 0.02, 0.02, 0);
            level.sendParticles(createDustParticle(0.1f, 0.1f, 0.1f, 0.9f),
                    end.x + offset, end.y + 0.3, end.z - offset, 1, 0.02, 0.02, 0.02, 0);
        }
        
        // Muzzle flash at start
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                start.x, start.y, start.z, 2, 0.1, 0.1, 0.1, 0);
    }
    
    // ===== NEW RANGER HELPER METHODS =====
    
    /**
     * Get the position where the player is looking (raycast for blocks up to maxRange)
     */
    private static Vec3 getPlayerLookTargetPosition(ServerPlayer player, double maxRange) {
        Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = startPos.add(lookVec.scale(maxRange));
        
        // Raycast for blocks
        var blockHit = player.level().clip(new net.minecraft.world.level.ClipContext(
                startPos, endPos,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                player));
        
        if (blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return blockHit.getLocation();
        }
        
        return null; // No valid block found
    }
    
    /**
     * Apply effect to nearby enemies at a specific position (not centered on player)
     */

    /**
    private static void applyEffectToNearbyEnemiesAtPosition(ServerLevel level, Vec3 center,
            net.minecraft.world.effect.MobEffect effect, int duration, int amplifier, double radius) {
        AABB searchBox = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);

        List<Entity> entities = level.getEntities((Entity) null, searchBox,
                e -> e instanceof LivingEntity && !(e instanceof ServerPlayer));

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                double dist = entity.position().distanceTo(center);
                if (dist <= radius) {
                    living.addEffect(new MobEffectInstance(effect, duration, amplifier));
                }
            }
        }
    }

    
    /**
     * Spawn large piercing arrow projectile with dust circles (3x normal size)
     */
    private static void spawnLargePiercingArrowProjectile(ServerPlayer player, ServerLevel level, 
            Vec3 startPos, Vec3 direction, float damage) {
        // Create a custom piercing arrow projectile that tracks and deals damage
        LargePiercingArrow arrow = new LargePiercingArrow(player, level, startPos, direction, damage);
        activeLargePiercingArrows.add(arrow);
        
        // Initial spawn effect - green ranger theme
        level.sendParticles(createDustParticle(0.2f, 0.85f, 0.25f, 1.0f),
                startPos.x, startPos.y, startPos.z, 20, 0.3, 0.3, 0.3, 0.1);
    }
    
    /**
     * Spawn multiple arrows in a fan spread pattern in the direction player is looking (FIXED)
     */
    private static void spawnMultiShotArrowsFixed(ServerPlayer player, ServerLevel level, 
            Vec3 startPos, Vec3 lookVec, float yRot, float damage) {
        int arrowCount = 5;
        float spreadAngle = 25.0f; // Total spread angle
        
        for (int i = 0; i < arrowCount; i++) {
            // Calculate angle offset from center (-12.5 to +12.5 degrees for 5 arrows)
            float angleOffset = (i - (arrowCount - 1) / 2.0f) * (spreadAngle / (arrowCount - 1));
            float arrowYRot = yRot + angleOffset;
            
            // Convert angle to direction vector
            double yRotRad = Math.toRadians(arrowYRot);
            double xPitch = lookVec.y; // Maintain vertical angle from look vector
            double horizontalLength = Math.cos(Math.asin(xPitch));
            
            Vec3 arrowDirection = new Vec3(
                    -Math.sin(yRotRad) * horizontalLength,
                    xPitch,
                    Math.cos(yRotRad) * horizontalLength
            ).normalize();
            
            // Spawn arrow entity
            net.minecraft.world.entity.projectile.Arrow arrow =
                    new net.minecraft.world.entity.projectile.Arrow(net.minecraft.world.entity.EntityType.ARROW, level);
            arrow.setPos(startPos.x, startPos.y, startPos.z);
            arrow.shoot(arrowDirection.x, arrowDirection.y, arrowDirection.z, 2.5f, 0.0f);
            arrow.setBaseDamage(damage);
            arrow.setCritArrow(false);
            arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
            level.addFreshEntity(arrow);
            
            // Spawn particle trail for each arrow
            level.sendParticles(createDustParticle(0.3f, 0.9f, 0.3f, 0.6f),
                    startPos.x, startPos.y, startPos.z, 3, 0.1, 0.1, 0.1, 0.05);
        }
    }
    
    // Data class for Large Piercing Arrow projectile
    public static class LargePiercingArrow {
        public final ServerPlayer owner;
        public final ServerLevel level;
        public Vec3 position;
        public final Vec3 direction;
        public final float damage;
        public int ticksAlive;
        public final int maxTicks;
        public final float speed;
        public final List<UUID> hitEntities; // Track which entities we've already hit
        
        public LargePiercingArrow(ServerPlayer owner, ServerLevel level, Vec3 startPos, Vec3 direction, float damage) {
            this.owner = owner;
            this.level = level;
            this.position = startPos;
            this.direction = direction.normalize();
            this.damage = damage;
            this.ticksAlive = 0;
            this.maxTicks = 100; // 5 seconds max flight time
            this.speed = 1.2f; // 3x faster (was 0.4f)
            this.hitEntities = new ArrayList<>();
        }
    }
    
    // Active large piercing arrows list
    private static final List<LargePiercingArrow> activeLargePiercingArrows = new ArrayList<>();
    
    /**
     * Update large piercing arrow projectile - returns false when it should be removed
     */
    private static boolean updateLargePiercingArrow(LargePiercingArrow arrow) {
        if (arrow.ticksAlive >= arrow.maxTicks) {
            return false;
        }
        
        // Move projectile
        arrow.position = arrow.position.add(arrow.direction.scale(arrow.speed));
        
        // Check for entity collision (pierce through multiple) - 3x bigger hitbox
        AABB hitbox = new AABB(
                arrow.position.x - 2.4, arrow.position.y - 2.4, arrow.position.z - 2.4,
                arrow.position.x + 2.4, arrow.position.y + 2.4, arrow.position.z + 2.4);
        
        List<Entity> entities = arrow.level.getEntities(arrow.owner, hitbox,
                e -> e instanceof LivingEntity && e != arrow.owner);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living && !arrow.hitEntities.contains(entity.getUUID())) {
                living.hurt(arrow.owner.damageSources().playerAttack(arrow.owner), arrow.damage);
                arrow.hitEntities.add(entity.getUUID());
                
                // Impact particles
                arrow.level.sendParticles(createDustParticle(0.3f, 0.9f, 0.3f, 1.0f),
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                        15, 0.3, 0.3, 0.3, 0.1);
            }
        }
        
        // Check for block collision
        var blockHit = arrow.level.clip(new net.minecraft.world.level.ClipContext(
                arrow.position.subtract(arrow.direction.scale(0.5)),
                arrow.position,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                arrow.owner));
        
        if (blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            // Impact effect - bright green ranger theme
            arrow.level.sendParticles(createDustParticle(0.2f, 0.9f, 0.3f, 1.2f),
                    blockHit.getLocation().x, blockHit.getLocation().y, blockHit.getLocation().z,
                    25, 0.4, 0.4, 0.4, 0.15);
            return false;
        }
        
        // Spawn trail particles - LARGE arrow (3x size) with elongated gradient dust trail
        if (arrow.ticksAlive % 1 == 0) { // Every tick
            // Main arrow trail with gradient (green → light green) - ranger theme
            arrow.level.sendParticles(createDustParticle(0.15f, 0.9f, 0.25f, 1.2f), // Bright green
                    arrow.position.x, arrow.position.y, arrow.position.z, 8, 0.45, 0.45, 0.45, 0.03);
            arrow.level.sendParticles(createDustParticle(0.25f, 1.0f, 0.35f, 1.0f), // Light green
                    arrow.position.x, arrow.position.y, arrow.position.z, 5, 0.3, 0.3, 0.3, 0.02);
            
            // Radiating dust circles around the arrow (3x larger)
            double circleRadius = 2.4; // 3x larger circles (was 0.8)
            int circlePoints = 16; // More points for smoother circle
            for (int i = 0; i < circlePoints; i++) {
                double angle = (double) i / circlePoints * 2 * Math.PI;
                Vec3 perpVec1 = new Vec3(
                        Math.cos(angle) * circleRadius,
                        Math.sin(angle) * circleRadius,
                        0
                );
                Vec3 circlePos = arrow.position.add(perpVec1);
                // Gradient from green to yellow-green
                arrow.level.sendParticles(createDustParticle(0.2f, 0.85f, 0.3f, 0.7f),
                        circlePos.x, circlePos.y, circlePos.z, 2, 0.1, 0.1, 0.1, 0);
            }
            
            // Side wisps extending outward (elongated trail)
            for (int side = -1; side <= 1; side += 2) {
                Vec3 perpVec = arrow.direction.cross(new Vec3(0, 1, 0)).normalize();
                Vec3 wispPos = arrow.position.add(perpVec.scale(side * 1.5));
                arrow.level.sendParticles(createDustParticle(0.3f, 0.95f, 0.4f, 0.6f),
                        wispPos.x, wispPos.y, wispPos.z, 3, 0.2, 0.2, 0.2, 0.02);
            }
            
            // Core glow (END_ROD for highlight)
            arrow.level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    arrow.position.x, arrow.position.y, arrow.position.z, 3, 0.3, 0.3, 0.3, 0.02);
        }
        
        return true;
    }
    
    // ===== WARRIOR ABILITY HELPER METHODS =====
    
    /**
     * Data class for Heavy Cleave projectile
     */
    public static class HeavyCleaveProjectile {
        public final ServerPlayer owner;
        public final ServerLevel level;
        public Vec3 position;
        public final Vec3 direction;
        public final float damage;
        public int ticksAlive;
        public final int maxTicks;
        public final float speed;
        public final List<UUID> hitEntities;
        public final float yaw; // Store yaw for arc visualization
        
        public HeavyCleaveProjectile(ServerPlayer owner, ServerLevel level, Vec3 startPos, Vec3 direction, float damage, float yaw) {
            this.owner = owner;
            this.level = level;
            this.position = startPos;
            this.direction = direction.normalize();
            this.damage = damage;
            this.ticksAlive = 0;
            this.maxTicks = 60; // 3 seconds
            this.speed = 1.0f;
            this.hitEntities = new ArrayList<>();
            this.yaw = yaw;
        }
    }
    
    private static final List<HeavyCleaveProjectile> activeHeavyCleaveProjectiles = new ArrayList<>();
    
    /**
     * Spawn Heavy Cleave projectile
     */
    private static void spawnHeavyCleaveProjectile(ServerPlayer player, ServerLevel level, Vec3 startPos, Vec3 direction, float damage, float yaw) {
        HeavyCleaveProjectile projectile = new HeavyCleaveProjectile(player, level, startPos, direction, damage, yaw);
        activeHeavyCleaveProjectiles.add(projectile);
    }
    
    /**
     * Update Heavy Cleave projectile
     */
    private static boolean updateHeavyCleaveProjectile(HeavyCleaveProjectile proj) {
        if (proj.ticksAlive >= proj.maxTicks) {
            return false;
        }
        
        proj.position = proj.position.add(proj.direction.scale(proj.speed));
        proj.ticksAlive++;
        
        // Check for entity collision
        AABB hitbox = new AABB(
                proj.position.x - 1.5, proj.position.y - 1.5, proj.position.z - 1.5,
                proj.position.x + 1.5, proj.position.y + 1.5, proj.position.z + 1.5);
        
        List<Entity> entities = proj.level.getEntities(proj.owner, hitbox,
                e -> e instanceof LivingEntity && e != proj.owner);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living && !proj.hitEntities.contains(entity.getUUID())) {
                living.hurt(proj.owner.damageSources().playerAttack(proj.owner), proj.damage);
                proj.hitEntities.add(entity.getUUID());
                
                // Impact particles - lighter red
                proj.level.sendParticles(createDustParticle(1.0f, 0.2f, 0.2f, 1.0f),
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                        15, 0.3, 0.3, 0.3, 0.1);
                proj.level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                        10, 0.2, 0.2, 0.2, 0.1);
            }
        }
        
        // Check for block collision
        var blockHit = proj.level.clip(new net.minecraft.world.level.ClipContext(
                proj.position.subtract(proj.direction.scale(0.5)),
                proj.position,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                proj.owner));
        
        if (blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            proj.level.sendParticles(createDustParticle(1.0f, 0.2f, 0.2f, 1.2f),
                    blockHit.getLocation().x, blockHit.getLocation().y, blockHit.getLocation().z,
                    20, 0.4, 0.4, 0.4, 0.15);
            return false;
        }
        
        // Spawn arc-shaped particles instead of bullet trail
        if (proj.ticksAlive % 1 == 0) {
            // Create an arc shape that travels with the projectile
            int arcParticles = 15;
            double arcDegrees = 90; // Narrower arc for projectile
            double halfArc = Math.toRadians(arcDegrees / 2.0);
            double yawRad = Math.toRadians(proj.yaw);
            
            for (int i = 0; i < arcParticles; i++) {
                double angle = -halfArc + (i / (double) arcParticles) * (2 * halfArc);
                double finalAngle = yawRad + angle + Math.PI / 2;
                double dist = 1.5 * (0.7 + RANDOM.nextDouble() * 0.3);
                double x = proj.position.x + Math.cos(finalAngle) * dist;
                double z = proj.position.z + Math.sin(finalAngle) * dist;
                
                // Lighter red color for projectile arc
                proj.level.sendParticles(createDustParticle(1.0f, 0.2f, 0.2f, 0.8f),
                        x, proj.position.y, z, 1, 0.05, 0.05, 0.05, 0.01);
            }
            
            // Add some crit particles for visual effect
            proj.level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                    proj.position.x, proj.position.y, proj.position.z, 2, 0.3, 0.2, 0.3, 0.02);
        }
        
        return true;
    }
    
    /**
     * Deal damage in an arc (120 degrees)
     */
    private static void dealDamageInArc(ServerPlayer player, float damage, double range, double arcDegrees) {
        Vec3 lookVec = player.getLookAngle();
        double yaw = Math.atan2(-lookVec.x, lookVec.z);
        double halfArc = Math.toRadians(arcDegrees / 2.0);
        
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<Entity> entities = player.level().getEntities(player, searchBox,
                e -> e instanceof LivingEntity && e != player);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                Vec3 toEntity = entity.position().subtract(player.position()).normalize();
                double entityAngle = Math.atan2(-toEntity.x, toEntity.z);
                double angleDiff = Math.abs(entityAngle - yaw);
                
                // Normalize angle difference
                if (angleDiff > Math.PI) {
                    angleDiff = 2 * Math.PI - angleDiff;
                }
                
                if (angleDiff <= halfArc && player.position().distanceTo(entity.position()) <= range) {
                    living.hurt(player.damageSources().playerAttack(player), damage);
                }
            }
        }
    }
    
    /**
     * Spawn Heavy Cleave arc effect
     */
    private static void spawnHeavyCleaveArcEffect(ServerLevel level, Vec3 center, float yaw, double radius) {
        int particles = 30;
        double arcDegrees = 120;
        double halfArc = Math.toRadians(arcDegrees / 2.0);
        double yawRad = Math.toRadians(yaw);
        
        // Add animation by spawning particles in waves
        for (int wave = 0; wave < 3; wave++) {
            final int waveIndex = wave;
            level.getServer().tell(new net.minecraft.server.TickTask(
                    level.getServer().getTickCount() + wave * 2, // Stagger waves
                    () -> {
                        double waveRadius = radius * (0.5 + waveIndex * 0.25);
                        for (int i = 0; i < particles; i++) {
                            double angle = -halfArc + (i / (double) particles) * (2 * halfArc);
                            double finalAngle = yawRad + angle + Math.PI / 2;
                            double dist = waveRadius * (0.8 + RANDOM.nextDouble() * 0.2);
                            double x = center.x + Math.cos(finalAngle) * dist;
                            double z = center.z + Math.sin(finalAngle) * dist;
                            
                            // Lighter red color - use slightly lighter shade
                            level.sendParticles(createDustParticle(1.0f, 0.2f, 0.2f, 0.9f),
                                    x, center.y + waveIndex * 0.2, z, 2, 0.1, 0.1, 0.1, 0.02);
                            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                                    x, center.y + waveIndex * 0.2, z, 1, 0.1, 0.1, 0.1, 0.01);
                        }
                    }
            ));
        }
    }
    
    /**
     * Spawn Battle Cry effect - focused on form, not quantity
     */
    private static void spawnBattleCryEffect(ServerLevel level, Vec3 center) {
        // Initial flash
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                center.x, center.y, center.z, 1, 0, 0, 0, 0);
        
        // Three expanding ring waves - clean, defined shape
        for (int wave = 0; wave < 3; wave++) {
            final int waveIndex = wave;
            level.getServer().tell(new net.minecraft.server.TickTask(
                    level.getServer().getTickCount() + wave * 4,
                    () -> {
                        double radius = 1.0 + waveIndex * 1.0;
                        int points = 12; // Reduced for cleaner shape
                        for (int p = 0; p < points; p++) {
                            double angle = (double) p / points * 2 * Math.PI;
                            double x = center.x + Math.cos(angle) * radius;
                            double z = center.z + Math.sin(angle) * radius;
                            
                            // Single clean particle per point
                            level.sendParticles(createDustParticle(1.0f, 0.2f, 0.2f, 1.2f),
                                    x, center.y + 0.1, z, 1, 0, 0, 0, 0);
                        }
                    }
            ));
        }
        
        // Single upward column effect
        for (int i = 0; i < 8; i++) {
            double y = center.y + i * 0.35;
            level.sendParticles(createDustParticle(1.0f, 0.3f, 0.1f, 1.0f),
                    center.x, y, center.z, 2, 0.15, 0.05, 0.15, 0.01);
        }
        
        // Crit burst for visual impact
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                center.x, center.y + 1, center.z, 8, 0.5, 0.3, 0.5, 0.15);
    }
    
    /**
     * Deal Whirlwind damage - 30% damage per hit, over 3 seconds
     */
    private static void dealWhirlwindDamage(ServerPlayer player, ServerLevel level, float damagePerHit, double range, int maxHits) {
        // Generate unique key for this whirlwind instance using player UUID + atomic counter
        String whirlwindKey = player.getUUID().toString() + "_" + whirlwindCounter.incrementAndGet();
        Map<UUID, Integer> hitCounts = new ConcurrentHashMap<>();
        whirlwindHitCounts.put(whirlwindKey, hitCounts);
        
        // Schedule hits over 3 seconds (60 ticks)
        int ticksPerHit = 60 / maxHits;
        for (int hit = 0; hit < maxHits; hit++) {
            final int hitIndex = hit;
            // Schedule each hit with a delay
            level.getServer().tell(new net.minecraft.server.TickTask(
                    level.getServer().getTickCount() + hit * ticksPerHit,
                    () -> {
                        if (!player.isAlive()) {
                            // Cleanup if player dies
                            whirlwindHitCounts.remove(whirlwindKey);
                            return;
                        }
                        
                        // Refresh entity list on each tick to catch entities entering the area
                        AABB searchBox = player.getBoundingBox().inflate(range);
                        List<Entity> entities = player.level().getEntities(player, searchBox,
                                e -> e instanceof LivingEntity && e != player);
                        
                        Map<UUID, Integer> persistentHitCounts = whirlwindHitCounts.get(whirlwindKey);
                        if (persistentHitCounts == null) return;
                        
                        for (Entity entity : entities) {
                            if (entity instanceof LivingEntity living && living.isAlive()) {
                                double dist = player.position().distanceTo(entity.position());
                                if (dist <= range) {
                                    UUID entityId = entity.getUUID();
                                    int currentHits = persistentHitCounts.getOrDefault(entityId, 0);
                                    if (currentHits < maxHits) {
                                        living.hurt(player.damageSources().playerAttack(player), damagePerHit);
                                        persistentHitCounts.put(entityId, currentHits + 1);
                                    }
                                }
                            }
                        }
                        
                        // Cleanup on final hit
                        if (hitIndex == maxHits - 1) {
                            whirlwindHitCounts.remove(whirlwindKey);
                        }
                    }
            ));
        }
    }
    
    /**
     * Spawn Whirlwind effect - lasts 3 seconds like an actual whirlwind
     */
    private static void spawnWhirlwindEffect(ServerLevel level, Vec3 center, double radius, float yaw) {
        // Continuous spinning animation over 3 seconds (60 ticks)
        int totalTicks = 60;
        double yawRad = Math.toRadians(yaw);
        
        for (int tick = 0; tick < totalTicks; tick++) {
            final int tickIndex = tick;
            level.getServer().tell(new net.minecraft.server.TickTask(
                    level.getServer().getTickCount() + tick,
                    () -> {
                        double spinProgress = (double) tickIndex / totalTicks;
                        double spinAngle = spinProgress * 12 * Math.PI; // 6 full rotations over 3 seconds
                        
                        // Spawn slash arcs at different angles during spin
                        for (int slash = 0; slash < 2; slash++) {
                            double slashAngle = spinAngle + slash * Math.PI;
                            int arcParticles = 8; // Reduced for cleaner visuals
                            double arcDegrees = 90;
                            double halfArc = Math.toRadians(arcDegrees / 2.0);
                            
                            for (int i = 0; i < arcParticles; i++) {
                                double angle = -halfArc + (i / (double) arcParticles) * (2 * halfArc);
                                double finalAngle = slashAngle + angle;
                                double dist = radius * (0.7 + RANDOM.nextDouble() * 0.3);
                                double x = center.x + Math.cos(finalAngle) * dist;
                                double z = center.z + Math.sin(finalAngle) * dist;
                                double y = center.y + (RANDOM.nextDouble() - 0.5) * 0.8;
                                
                                // Lighter red slashes
                                level.sendParticles(createDustParticle(1.0f, 0.2f, 0.2f, 0.9f),
                                        x, y, z, 1, 0.05, 0.05, 0.05, 0.01);
                            }
                        }
                        
                        // Add spinning ring effect - more spread out
                        if (tickIndex % 2 == 0) {
                            int ringPoints = 12;
                            for (int p = 0; p < ringPoints; p++) {
                                double angle = (double) p / ringPoints * 2 * Math.PI + spinAngle;
                                double x = center.x + Math.cos(angle) * radius * 0.9;
                                double z = center.z + Math.sin(angle) * radius * 0.9;
                                
                                level.sendParticles(createDustParticle(1.0f, 0.15f, 0.15f, 0.8f),
                                        x, center.y, z, 1, 0.03, 0.03, 0.03, 0.01);
                            }
                        }
                        
                        // Add SWEEP_ATTACK particles for slash effect - every ~8 ticks
                        if (tickIndex % 8 == 0) {
                            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                                    center.x, center.y, center.z, 2, 0.5, 0.3, 0.5, 0);
                        }
                    }
            ));
        }
    }
    
    /**
     * Spawn Leap launch effect
     */
    private static void spawnLeapLaunchEffect(ServerLevel level, Vec3 center) {
        // Ground impact with lighter red
        for (int ring = 0; ring < 4; ring++) {
            double radius = 1.0 + ring * 0.5;
            int points = 16;
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                
                level.sendParticles(createDustParticle(1.0f, 0.2f, 0.2f, 0.7f),
                        x, center.y + 0.1, z, 2, 0.05, 0.02, 0.05, 0.01);
            }
        }
        
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                center.x, center.y + 0.5, center.z, 1, 0, 0, 0, 0);
    }
    
    // ===== RAVAGER ABILITY HELPER METHODS AND DATA STRUCTURES =====
    
    /**
     * Status effect tracking
     */
    public static class BleedEffect {
        public final UUID targetId;
        public final UUID ownerId;
        public int duration; // in ticks
        
        public BleedEffect(UUID targetId, UUID ownerId, int duration) {
            this.targetId = targetId;
            this.ownerId = ownerId;
            this.duration = duration;
        }
    }
    
    public static class GrievousWoundsEffect {
        public final UUID targetId;
        public final UUID ownerId;
        public int stacks;
        
        public GrievousWoundsEffect(UUID targetId, UUID ownerId, int stacks) {
            this.targetId = targetId;
            this.ownerId = ownerId;
            this.stacks = Math.min(stacks, 5); // Max 5 stacks
        }
        
        public void addStack() {
            this.stacks = Math.min(this.stacks + 1, 5);
        }
    }
    
    private static final Map<UUID, BleedEffect> activeBleedEffects = new ConcurrentHashMap<>();
    private static final Map<UUID, GrievousWoundsEffect> activeGrievousWounds = new ConcurrentHashMap<>();
    
    /**
     * Tearing Hook pull data
     */
    public static class TearingHookPull {
        public final ServerPlayer owner;
        public final LivingEntity target;
        public final boolean pullPlayerToTarget;
        public final long executeAt;
        
        public TearingHookPull(ServerPlayer owner, LivingEntity target, boolean pullPlayerToTarget, long executeAt) {
            this.owner = owner;
            this.target = target;
            this.pullPlayerToTarget = pullPlayerToTarget;
            this.executeAt = executeAt;
        }
    }
    
    private static final List<TearingHookPull> scheduledHookPulls = new ArrayList<>();
    
    /**
     * Rupture projectile data
     */
    public static class RuptureProjectile {
        public final ServerPlayer owner;
        public final ServerLevel level;
        public Vec3 position;
        public final Vec3 direction;
        public final float damage;
        public int ticksAlive;
        public final int maxTicks;
        public final float speed;
        public LivingEntity stuckEntity;
        public boolean stuck;
        public int stuckTime;
        
        public RuptureProjectile(ServerPlayer owner, ServerLevel level, Vec3 startPos, Vec3 direction, float damage) {
            this.owner = owner;
            this.level = level;
            this.position = startPos;
            this.direction = direction.normalize();
            this.damage = damage;
            this.ticksAlive = 0;
            this.maxTicks = 100;
            this.speed = 0.8f;
            this.stuck = false;
            this.stuckTime = 0;
        }
    }
    
    private static final List<RuptureProjectile> activeRuptureProjectiles = new ArrayList<>();
    
    /**
     * Find enemy in look direction
     */
    private static LivingEntity findEnemyInLookDirection(ServerPlayer player, Vec3 direction, double maxRange) {
        Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);
        AABB searchBox = player.getBoundingBox().inflate(maxRange);
        List<Entity> entities = player.level().getEntities(player, searchBox,
                e -> e instanceof LivingEntity && e != player);
        
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                Vec3 toEntity = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(startPos);
                double dot = direction.normalize().dot(toEntity.normalize());
                if (dot > 0.9) { // Must be very close to look direction
                    double dist = toEntity.length();
                    if (dist < closestDist && dist <= maxRange) {
                        closestDist = dist;
                        closest = living;
                    }
                }
            }
        }
        
        return closest;
    }
    
    /**
     * Schedule Tearing Hook pull
     */
    private static void scheduleTearingHookPull(ServerPlayer player, LivingEntity target, boolean pullPlayerToTarget, long executeAt) {
        scheduledHookPulls.add(new TearingHookPull(player, target, pullPlayerToTarget, executeAt));
    }
    
    /**
     * Spawn Tearing Hook chain visual
     */
    private static void spawnTearingHookChain(ServerLevel level, Vec3 start, Vec3 end, boolean isRed) {
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        direction = direction.normalize();
        
        int particleCount = (int) (distance * 5);
        for (int i = 0; i < particleCount; i++) {
            double progress = (double) i / particleCount;
            Vec3 pos = start.add(direction.scale(progress * distance));
            
            if (isRed) {
                level.sendParticles(createDustParticle(0.8f, 0.0f, 0.0f, 0.8f),
                        pos.x, pos.y, pos.z, 2, 0.03, 0.03, 0.03, 0);
            } else {
                level.sendParticles(createDustParticle(0.5f, 0.5f, 0.5f, 0.7f),
                        pos.x, pos.y, pos.z, 2, 0.03, 0.03, 0.03, 0);
            }
        }
    }
    
    /**
     * Apply BLEED effect to entity
     */
    public static void applyBleed(LivingEntity target, ServerPlayer owner, int durationTicks) {
        // Check if target already has GRIEVOUS WOUNDS
        if (activeGrievousWounds.containsKey(target.getUUID())) {
            return; // Cannot apply BLEED if has GRIEVOUS WOUNDS
        }
        
        BleedEffect bleed = new BleedEffect(target.getUUID(), owner.getUUID(), durationTicks);
        activeBleedEffects.put(target.getUUID(), bleed);
    }
    
    /**
     * Apply GRIEVOUS WOUNDS effect to entity
     */
    private static void applyGrievousWounds(LivingEntity target, ServerPlayer owner, int stacks) {
        UUID targetId = target.getUUID();
        
        // Remove BLEED if present and add extra stack
        if (activeBleedEffects.containsKey(targetId)) {
            activeBleedEffects.remove(targetId);
            stacks++; // Extra stack for removing BLEED
        }
        
        if (activeGrievousWounds.containsKey(targetId)) {
            activeGrievousWounds.get(targetId).addStack();
        } else {
            activeGrievousWounds.put(targetId, new GrievousWoundsEffect(targetId, owner.getUUID(), stacks));
        }
    }
    
    /**
     * Apply Razor damage with GRIEVOUS WOUNDS in 180 degree arc
     */
    private static void applyRazorDamageInArc(ServerPlayer player, ServerLevel level, float damage, double range, float yaw) {
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<Entity> entities = player.level().getEntities(player, searchBox,
                e -> e instanceof LivingEntity && e != player);
        
        double yawRad = Math.toRadians(yaw);
        double arcDegrees = 180; // 180 degree arc as specified
        double halfArc = Math.toRadians(arcDegrees / 2.0);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                Vec3 toEntity = entity.position().subtract(player.position());
                double dist = toEntity.length();
                if (dist <= range) {
                    // Check if entity is within arc
                    double angleToEntity = Math.atan2(toEntity.z, toEntity.x);
                    double playerAngle = yawRad + Math.PI / 2;
                    double angleDiff = Math.abs(angleToEntity - playerAngle);
                    
                    // Normalize angle difference to [0, PI]
                    while (angleDiff > Math.PI) angleDiff = Math.abs(angleDiff - 2 * Math.PI);
                    
                    if (angleDiff <= halfArc) {
                        living.hurt(player.damageSources().playerAttack(player), damage);
                        applyGrievousWounds(living, player, 1);
                    }
                }
            }
        }
    }
    
    /**
     * Spawn Razor effect - 180 degree arc attack with animated blades
     */
    private static void spawnRazorEffect(ServerLevel level, Vec3 center, double radius, float yaw) {
        double yawRad = Math.toRadians(yaw);
        double arcDegrees = 180; // 180 degree arc
        double halfArc = Math.toRadians(arcDegrees / 2.0);
        
        // Animated blade slashes over time
        for (int wave = 0; wave < 4; wave++) {
            final int waveIndex = wave;
            level.getServer().tell(new net.minecraft.server.TickTask(
                    level.getServer().getTickCount() + wave * 2,
                    () -> {
                        double waveRadius = radius * (0.6 + waveIndex * 0.15);
                        
                        // Arc slash
                        int arcParticles = 25;
                        for (int i = 0; i < arcParticles; i++) {
                            double angle = -halfArc + (i / (double) arcParticles) * (2 * halfArc);
                            double finalAngle = yawRad + angle + Math.PI / 2;
                            double dist = waveRadius * (0.8 + RANDOM.nextDouble() * 0.2);
                            double x = center.x + Math.cos(finalAngle) * dist;
                            double z = center.z + Math.sin(finalAngle) * dist;
                            double y = center.y + (RANDOM.nextDouble() - 0.5) * 0.6 + waveIndex * 0.1;
                            
                            // Dark red with blood effect
                            level.sendParticles(createDustParticle(0.7f, 0.0f, 0.0f, 1.1f),
                                    x, y, z, 2, 0.08, 0.08, 0.08, 0.02);
                        }
                        
                        // Add sweeping attack particles
                        if (waveIndex % 2 == 0) {
                            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                                    center.x, center.y, center.z, 2, 0.5, 0.2, 0.5, 0);
                        }
                    }
            ));
        }
        
        // Blood spray particles
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                net.minecraft.core.particles.ParticleTypes.BLOCK, 
                net.minecraft.world.level.block.Blocks.RED_CONCRETE.defaultBlockState()),
                center.x, center.y + 1, center.z, 30, 0.8, 0.4, 0.8, 0.15);
        
        // Crimson spore particles for extra effect
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIMSON_SPORE,
                center.x, center.y + 0.5, center.z, 20, 1.0, 0.3, 1.0, 0.05);
    }
    
    /**
     * Spawn Rupture projectile
     */
    private static void spawnRuptureProjectile(ServerPlayer player, ServerLevel level, Vec3 startPos, Vec3 direction, float damage) {
        RuptureProjectile projectile = new RuptureProjectile(player, level, startPos, direction, damage);
        activeRuptureProjectiles.add(projectile);
    }
    
    /**
     * Update Rupture projectile
     */
    private static boolean updateRuptureProjectile(RuptureProjectile proj) {
        proj.ticksAlive++;
        
        if (proj.stuck) {
            proj.stuckTime++;
            
            // After 2.5 seconds (50 ticks), explode
            if (proj.stuckTime >= 50) {
                // Explode effect
                spawnRuptureExplosion(proj.level, proj.position, proj.owner, proj.damage * 0.25f);
                return false;
            }
            
            // Update position to follow stuck entity
            if (proj.stuckEntity != null && proj.stuckEntity.isAlive()) {
                proj.position = proj.stuckEntity.position().add(0, proj.stuckEntity.getBbHeight() * 0.5, 0);
                
                // Spawn stuck blade visual with actual blade particles
                if (proj.stuckTime % 3 == 0) {
                    // Draw a blade shape using particles
                    for (int i = 0; i < 5; i++) {
                        double offsetY = i * 0.15;
                        proj.level.sendParticles(createDustParticle(0.7f, 0.0f, 0.0f, 1.2f),
                                proj.position.x, proj.position.y - offsetY, proj.position.z, 
                                1, 0.03, 0.03, 0.03, 0);
                    }
                    // Blade edge particles (iron block for metal look)
                    proj.level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                            net.minecraft.core.particles.ParticleTypes.BLOCK,
                            net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState()),
                            proj.position.x, proj.position.y, proj.position.z, 2, 0.1, 0.1, 0.1, 0);
                }
                
                // Pulsing effect as explosion approaches
                if (proj.stuckTime > 30 && proj.stuckTime % 5 == 0) {
                    proj.level.sendParticles(createDustParticle(0.8f, 0.0f, 0.0f, 1.5f),
                            proj.position.x, proj.position.y, proj.position.z, 8, 0.3, 0.3, 0.3, 0.08);
                }
            } else {
                // Entity died, explode early
                spawnRuptureExplosion(proj.level, proj.position, proj.owner, proj.damage * 0.25f);
                return false;
            }
            
            return true;
        }
        
        if (proj.ticksAlive >= proj.maxTicks) {
            return false;
        }
        
        proj.position = proj.position.add(proj.direction.scale(proj.speed));
        
        // Check for entity collision
        AABB hitbox = new AABB(
                proj.position.x - 0.5, proj.position.y - 0.5, proj.position.z - 0.5,
                proj.position.x + 0.5, proj.position.y + 0.5, proj.position.z + 0.5);
        
        List<Entity> entities = proj.level.getEntities(proj.owner, hitbox,
                e -> e instanceof LivingEntity && e != proj.owner);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                // Get GRIEVOUS WOUNDS stacks for damage scaling
                int gwStacks = 0;
                if (activeGrievousWounds.containsKey(entity.getUUID())) {
                    gwStacks = activeGrievousWounds.get(entity.getUUID()).stacks;
                }
                
                float finalDamage = proj.damage * (1.0f + gwStacks * 0.3f);
                living.hurt(proj.owner.damageSources().playerAttack(proj.owner), finalDamage);
                
                // Stick to entity
                proj.stuck = true;
                proj.stuckEntity = living;
                proj.stuckTime = 0;
                
                // Impact effect - show blade sticking
                proj.level.sendParticles(createDustParticle(0.7f, 0.0f, 0.0f, 1.2f),
                        proj.position.x, proj.position.y, proj.position.z, 15, 0.3, 0.3, 0.3, 0.1);
                proj.level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                        proj.position.x, proj.position.y, proj.position.z, 1, 0, 0, 0, 0);
                
                return true;
            }
        }
        
        // Check for block collision
        var blockHit = proj.level.clip(new net.minecraft.world.level.ClipContext(
                proj.position.subtract(proj.direction.scale(0.5)),
                proj.position,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                proj.owner));
        
        if (blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            proj.level.sendParticles(createDustParticle(0.7f, 0.0f, 0.0f, 1.2f),
                    blockHit.getLocation().x, blockHit.getLocation().y, blockHit.getLocation().z,
                    20, 0.4, 0.4, 0.4, 0.15);
            return false;
        }
        
        // Spawn sword-shaped trail particles (actual sword visual in flight)
        if (proj.ticksAlive % 2 == 0) {
            // Draw a larger sword blade shape with handle
            // Blade - vertical line of particles
            for (int i = 0; i < 6; i++) {
                double offsetY = i * 0.15 - 0.3;
                float size = (i < 5) ? 0.8f : 0.5f; // Tip is smaller
                proj.level.sendParticles(createDustParticle(0.8f, 0.1f, 0.1f, size),
                        proj.position.x, proj.position.y + offsetY, proj.position.z, 
                        1, 0.03, 0.02, 0.03, 0);
            }
            // Handle/hilt
            proj.level.sendParticles(createDustParticle(0.4f, 0.3f, 0.2f, 0.6f),
                    proj.position.x, proj.position.y - 0.45, proj.position.z, 
                    2, 0.05, 0.02, 0.05, 0);
            // Cross guard
            proj.level.sendParticles(createDustParticle(0.5f, 0.4f, 0.2f, 0.5f),
                    proj.position.x + 0.1, proj.position.y - 0.35, proj.position.z, 
                    1, 0.01, 0.01, 0.01, 0);
            proj.level.sendParticles(createDustParticle(0.5f, 0.4f, 0.2f, 0.5f),
                    proj.position.x - 0.1, proj.position.y - 0.35, proj.position.z, 
                    1, 0.01, 0.01, 0.01, 0);
            // Metal blade edge particles - larger
            proj.level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                    net.minecraft.core.particles.ParticleTypes.BLOCK,
                    net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState()),
                    proj.position.x, proj.position.y + 0.2, proj.position.z, 3, 0.1, 0.2, 0.1, 0.02);
            // Blood trail behind sword
            proj.level.sendParticles(createDustParticle(0.6f, 0.0f, 0.0f, 0.7f),
                    proj.position.x - proj.direction.x * 0.5, proj.position.y, proj.position.z - proj.direction.z * 0.5, 
                    3, 0.1, 0.1, 0.1, 0.02);
        }
        
        // Rotating spin effect every tick
        double spinAngle = (proj.ticksAlive * 0.5) % (2 * Math.PI);
        double spinRadius = 0.3;
        double spinX = proj.position.x + Math.cos(spinAngle) * spinRadius;
        double spinZ = proj.position.z + Math.sin(spinAngle) * spinRadius;
        proj.level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                spinX, proj.position.y, spinZ, 1, 0.02, 0.02, 0.02, 0);
        
        return true;
    }
    
    /**
     * Spawn Rupture explosion - bigger effects
     */
    private static void spawnRuptureExplosion(ServerLevel level, Vec3 center, ServerPlayer owner, float damage) {
        double radius = 4.0; // Increased radius
        
        // Deal damage in small AOE
        AABB searchBox = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        
        List<Entity> entities = level.getEntities(owner, searchBox,
                e -> e instanceof LivingEntity && e != owner);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                double dist = center.distanceTo(entity.position());
                if (dist <= radius) {
                    // Get GRIEVOUS WOUNDS stacks for damage scaling
                    int gwStacks = 0;
                    if (activeGrievousWounds.containsKey(entity.getUUID())) {
                        gwStacks = activeGrievousWounds.get(entity.getUUID()).stacks;
                    }
                    
                    float finalDamage = damage * (1.0f + gwStacks * 0.3f);
                    living.hurt(owner.damageSources().playerAttack(owner), finalDamage);
                }
            }
        }
        
        // Bigger explosion visual with multiple explosions
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y, center.z, 1, 0, 0, 0, 0);
        
        // Multiple smaller explosions around
        for (int i = 0; i < 4; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * 2;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 2;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                    center.x + offsetX, center.y, center.z + offsetZ, 1, 0, 0, 0, 0);
        }
        
        // Large blood burst
        level.sendParticles(createDustParticle(0.8f, 0.0f, 0.0f, 1.8f),
                center.x, center.y, center.z, 60, 1.2, 1.0, 1.2, 0.25);
        
        // Blood concrete particles
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                net.minecraft.core.particles.ParticleTypes.BLOCK,
                net.minecraft.world.level.block.Blocks.RED_CONCRETE.defaultBlockState()),
                center.x, center.y, center.z, 50, 1.0, 0.8, 1.0, 0.2);
        
        // Iron/metal shrapnel from sword
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                net.minecraft.core.particles.ParticleTypes.BLOCK,
                net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState()),
                center.x, center.y, center.z, 20, 0.8, 0.6, 0.8, 0.15);
        
        // Crit particles
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                center.x, center.y + 0.5, center.z, 25, 1.0, 0.5, 1.0, 0.3);
    }
    
    /**
     * Update status effects (called every tick)
     */
    public static void updateStatusEffects(ServerLevel level) {
        // Update BLEED effects
        Iterator<Map.Entry<UUID, BleedEffect>> bleedIterator = activeBleedEffects.entrySet().iterator();
        while (bleedIterator.hasNext()) {
            Map.Entry<UUID, BleedEffect> entry = bleedIterator.next();
            BleedEffect bleed = entry.getValue();
            bleed.duration--;
            
            // Find entity and deal damage every second
            if (bleed.duration % 20 == 0) {
                Entity entity = level.getEntity(bleed.targetId);
                if (entity instanceof LivingEntity living && living.isAlive()) {
                    living.hurt(level.damageSources().magic(), 1.0f);
                    
                    // Bleed visual
                    level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                            net.minecraft.core.particles.ParticleTypes.BLOCK,
                            net.minecraft.world.level.block.Blocks.RED_CONCRETE.defaultBlockState()),
                            living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(),
                            5, 0.2, 0.2, 0.2, 0.1);
                }
            }
            
            if (bleed.duration <= 0) {
                bleedIterator.remove();
            }
        }
    }
    
    /**
     * Update Leap landing detection - 3s air time, then crash down at targeted location
     */
    public static void updateWarriorLeaps(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (player.getPersistentData().getBoolean("warrior_leaping")) {
                long leapTime = player.getPersistentData().getLong("warrior_leap_time");
                long currentTime = level.getGameTime();
                long airTime = currentTime - leapTime;
                
                // After 3 seconds (60 ticks) in air, launch towards targeted location
                if (airTime >= 60) {
                    // Calculate target position based on current look direction (max 10 blocks)
                    Vec3 lookVec = player.getLookAngle();
                    Vec3 horizontalLook = new Vec3(lookVec.x, 0, lookVec.z).normalize();
                    Vec3 targetPos = player.position().add(horizontalLook.scale(10.0));
                    
                    // Find ground level at target position
                    int targetGroundY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, 
                            (int) targetPos.x, (int) targetPos.z);
                    targetPos = new Vec3(targetPos.x, targetGroundY, targetPos.z);
                    
                    // Calculate launch velocity towards target
                    Vec3 currentPos = player.position();
                    Vec3 toTarget = targetPos.subtract(currentPos).normalize();
                    
                    // Launch player towards target with strong velocity
                    double launchSpeed = 2.5; // Fast horizontal launch
                    double downwardSpeed = -1.5; // Downward component
                    player.setDeltaMovement(toTarget.x * launchSpeed, downwardSpeed, toTarget.z * launchSpeed);
                    player.hurtMarked = true;
                    
                    // Mark to prevent fall damage
                    player.getPersistentData().putBoolean("warrior_leap_no_fall_damage", true);
                    
                    // Deal 200% damage and slow for 2s at impact
                    float damage = player.getPersistentData().getFloat("warrior_leap_damage");
                    dealDamageToNearbyEnemies(player, damage, 5.0);
                    applyEffectToNearbyEnemies(player, MobEffects.MOVEMENT_SLOWDOWN, 40, 1, 5.0); // 2 seconds slow
                    
                    // Landing effect with lighter red
                    spawnLeapLandingEffect(level, player.position());
                    
                    // Clear flags
                    player.getPersistentData().remove("warrior_leaping");
                    player.getPersistentData().remove("warrior_leap_time");
                    player.getPersistentData().remove("warrior_leap_damage");
                    player.getPersistentData().remove("warrior_leap_yaw");
                    player.getPersistentData().remove("warrior_leap_pitch");
                    player.getPersistentData().remove("warrior_leap_no_fall_damage");
                    
                    // Remove slow falling
                    player.removeEffect(MobEffects.SLOW_FALLING);
                    
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c§l💥 CRASH DOWN!"), true);
                } else if (player.onGround() && airTime > 10) {
                    // If player lands early (after at least 0.5s), still do crash effect
                    float damage = player.getPersistentData().getFloat("warrior_leap_damage");
                    dealDamageToNearbyEnemies(player, damage, 5.0);
                    applyEffectToNearbyEnemies(player, MobEffects.MOVEMENT_SLOWDOWN, 40, 1, 5.0);
                    spawnLeapLandingEffect(level, player.position());
                    
                    // Clear flags
                    player.getPersistentData().remove("warrior_leaping");
                    player.getPersistentData().remove("warrior_leap_time");
                    player.getPersistentData().remove("warrior_leap_damage");
                    player.getPersistentData().remove("warrior_leap_yaw");
                    player.getPersistentData().remove("warrior_leap_pitch");
                    player.getPersistentData().remove("warrior_leap_no_fall_damage");
                    player.removeEffect(MobEffects.SLOW_FALLING);
                }
            }
        }
    }
    
    /**
     * Spawn Leap landing effect
     */
    private static void spawnLeapLandingEffect(ServerLevel level, Vec3 center) {
        // Ground shockwave with lighter red
        for (int ring = 0; ring < 6; ring++) {
            double radius = 1.0 + ring * 0.8;
            int points = 24;
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                
                level.sendParticles(createDustParticle(1.0f, 0.2f, 0.2f, 0.8f),
                        x, center.y + 0.1, z, 3, 0.05, 0.02, 0.05, 0.02);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                        x, center.y + 0.1, z, 2, 0.05, 0.02, 0.05, 0.01);
            }
        }
        
        // Central explosion
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                center.x, center.y + 0.5, center.z, 3, 0.5, 0.2, 0.5, 0);
        
        // Upward burst with lighter red
        level.sendParticles(createDustParticle(1.0f, 0.2f, 0.2f, 1.2f),
                center.x, center.y, center.z, 50, 0.5, 0.3, 0.5, 0.2);
    }
    
    /**
     * Update Lancer abilities (Piercing Charge and Comet impact)
     */
    public static void updateLancerAbilities(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            var rpgData = player.getData(ModAttachments.PLAYER_RPG);
            
            // Handle Piercing Charge
            if (rpgData.isInPiercingCharge()) {
                long chargeTime = level.getGameTime() - rpgData.getPiercingChargeStartTime();
                
                // Check timeout (8 seconds = 160 ticks)
                if (chargeTime >= 160) {
                    rpgData.setInPiercingCharge(false);
                    rpgData.setAbilityCooldown("lancer_ability_1", 300); // 15s cooldown
                    sendToPlayer(new PacketSyncCooldowns(rpgData.getAllCooldowns()), player);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "§ePiercing Charge ended!"), true);
                    continue;
                }
                
                // Check wall collision (no velocity)
                Vec3 velocity = player.getDeltaMovement();
                double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                if (horizontalSpeed < 0.05) {
                    // Hit a wall - stop charging
                    rpgData.setInPiercingCharge(false);
                    rpgData.setAbilityCooldown("lancer_ability_1", 300); // 15s cooldown
                    sendToPlayer(new PacketSyncCooldowns(rpgData.getAllCooldowns()), player);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "§cPiercing Charge §7stopped by wall!"), true);
                    
                    // Yellow wall impact effect
                    spawnDustParticlesBurst(level, player.position(), 2.0, 1.0f, 1.0f, 0.2f, 20);
                    continue;
                }
                
                // Spawn spear-like aura visual in front
                Vec3 lookVec = player.getLookAngle();
                Vec3 spearTip = player.position().add(0, player.getEyeHeight(), 0).add(lookVec.scale(2.0));
                level.sendParticles(createDustParticle(1.0f, 1.0f, 0.3f, 0.8f),
                        spearTip.x, spearTip.y, spearTip.z, 3, 0.1, 0.1, 0.1, 0);
                
                // Check for enemies hit
                AABB hitBox = player.getBoundingBox().inflate(2.0);
                List<Entity> entities = level.getEntities(player, hitBox, 
                        e -> e instanceof LivingEntity && e != player);
                
                for (Entity entity : entities) {
                    if (entity instanceof LivingEntity living) {
                        float momentum = rpgData.getMomentum();
                        float pierceDamage = rpgData.getPiercingChargeDamage();
                        
                        // Deal momentum-based damage
                        living.hurt(player.damageSources().playerAttack(player), pierceDamage * 0.5f);
                        
                        // Check if damage was less than 20% of target's HP
                        float targetHealthPercent = living.getHealth() / living.getMaxHealth();
                        if (pierceDamage * 0.5f < living.getMaxHealth() * 0.2f) {
                            // Stop and deal larger damage
                            rpgData.setInPiercingCharge(false);
                            living.hurt(player.damageSources().playerAttack(player), pierceDamage);
                            rpgData.setAbilityCooldown("lancer_ability_1", 300); // 15s cooldown
                            sendToPlayer(new PacketSyncCooldowns(rpgData.getAllCooldowns()), player);
                            
                            // Stop player
                            player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
                            player.hurtMarked = true;
                            
                            // Yellow impact effect
                            spawnDustParticlesBurst(level, living.position(), 2.0, 1.0f, 1.0f, 0.2f, 25);
                            
                            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                    "§e§lCRITICAL PIERCE!"), true);
                            break;
                        }
                    }
                }
                
                // Reduce turning speed by applying look direction constraint
                Vec3 currentLook = player.getLookAngle();
                // This is a simplified approach - in practice, rotation would be constrained more directly
            }
            
            // Handle Comet impact
            if (player.getPersistentData().getBoolean("lancer_comet_active")) {
                if (player.onGround()) {
                    // Impact!
                    float cometDamage = player.getPersistentData().getFloat("lancer_comet_damage");
                    
                    // Deal shockwave damage in radius
                    dealDamageToNearbyEnemies(player, cometDamage, 6.0);
                    
                    // Yellow shockwave effect
                    for (int ring = 0; ring < 8; ring++) {
                        double radius = 1.0 + ring * 0.8;
                        int points = 32;
                        for (int p = 0; p < points; p++) {
                            double angle = (double) p / points * 2 * Math.PI;
                            double x = player.getX() + Math.cos(angle) * radius;
                            double z = player.getZ() + Math.sin(angle) * radius;
                            
                            level.sendParticles(createDustParticle(1.0f, 1.0f, 0.2f, 1.0f),
                                    x, player.getY() + 0.1, z, 3, 0.05, 0.02, 0.05, 0.02);
                            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                                    x, player.getY() + 0.1, z, 2, 0.05, 0.02, 0.05, 0.01);
                        }
                    }
                    
                    // Central explosion
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                            player.getX(), player.getY() + 0.5, player.getZ(), 5, 0.5, 0.2, 0.5, 0);
                    
                    player.getPersistentData().remove("lancer_comet_active");
                    player.getPersistentData().remove("lancer_comet_damage");
                    
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "§e§l💥 COMET IMPACT!"), true);
                }
            }
        }
    }
    
    /**
     * Update Ravager Heartstopper charging
     */
    public static void updateRavagerHeartstoppers(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            UUID playerUuid = player.getUUID();
            
            if (player.getPersistentData().getBoolean("ravager_heartstopper_charging")) {
                long startTime = player.getPersistentData().getLong("ravager_heartstopper_start");
                long currentTime = level.getGameTime();
                long elapsed = currentTime - startTime;
                
                // Create boss bar if it doesn't exist
                if (!heartstopperBossBars.containsKey(playerUuid)) {
                    ServerBossEvent bossBar = new ServerBossEvent(
                            Component.literal("§c§lHEARTSTOPPER CHARGING"),
                            BossEvent.BossBarColor.RED,
                            BossEvent.BossBarOverlay.PROGRESS
                    );
                    bossBar.addPlayer(player);
                    heartstopperBossBars.put(playerUuid, bossBar);
                }
                
                // Update boss bar progress (0% to 100% over 60 ticks)
                ServerBossEvent bossBar = heartstopperBossBars.get(playerUuid);
                if (bossBar != null) {
                    float progress = Math.min(1.0f, (float) elapsed / 60.0f);
                    bossBar.setProgress(progress);
                }
                
                // Use CURRENT player position plus 1 block in front
                Vec3 currentPos = player.position();
                float currentYaw = player.getYRot();
                double yawRad = Math.toRadians(currentYaw);
                Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
                Vec3 aoeCenterPos = currentPos.add(forward.scale(1.0)); // 1 block in front
                
                // Show red rectangle AOE indicator at position 1 block in front
                if (elapsed % 10 == 0) {
                    spawnHeartstopperAOEIndicator(level, aoeCenterPos, currentYaw);
                }
                
                // After 3 seconds (60 ticks), execute
                if (elapsed >= 60) {
                    float damage = player.getPersistentData().getFloat("ravager_heartstopper_damage");
                    
                    // Deal damage in rectangular AOE 1 block in front of player
                    dealHeartstopperDamage(player, level, aoeCenterPos, currentYaw, damage);
                    
                    // Heal for BLEED and GRIEVOUS WOUNDS stacks
                    float healing = calculateHeartstopperHealing(level, aoeCenterPos, currentYaw);
                    player.heal(healing);
                    
                    // Final slam effect 1 block in front
                    spawnHeartstopperSlamEffect(level, aoeCenterPos, currentYaw);
                    
                    // Clear flags
                    player.getPersistentData().remove("ravager_heartstopper_charging");
                    player.getPersistentData().remove("ravager_heartstopper_start");
                    player.getPersistentData().remove("ravager_heartstopper_damage");
                    
                    // Remove boss bar
                    if (bossBar != null) {
                        bossBar.removeAllPlayers();
                        heartstopperBossBars.remove(playerUuid);
                    }
                    
                    player.displayClientMessage(Component.literal("§c§l☠ HEARTSTOPPER! §a+" + String.format("%.1f", healing) + " HP"), true);
                }
            } else {
                // Player is not charging - cleanup boss bar if it exists
                ServerBossEvent bossBar = heartstopperBossBars.get(playerUuid);
                if (bossBar != null) {
                    bossBar.removeAllPlayers();
                    heartstopperBossBars.remove(playerUuid);
                }
            }
        }
    }
    
    /**
     * Spawn Heartstopper AOE indicator
     */
    private static void spawnHeartstopperAOEIndicator(ServerLevel level, Vec3 center, float yaw) {
        double length = 6.0;
        double width = 4.0;
        double yawRad = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 right = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad));
        
        // Rectangle starts at center (1 block in front of player) and extends forward
        // The back edge of the rectangle is at the center position
        
        // Draw rectangle outline
        int lengthPoints = 12;
        int widthPoints = 8;
        
        for (int i = 0; i <= lengthPoints; i++) {
            double progress = (double) i / lengthPoints; // 0 to 1 instead of -0.5 to 0.5
            Vec3 pos1 = center.add(forward.scale(progress * length)).add(right.scale(-width / 2));
            Vec3 pos2 = center.add(forward.scale(progress * length)).add(right.scale(width / 2));
            
            level.sendParticles(createDustParticle(0.8f, 0.0f, 0.0f, 0.6f),
                    pos1.x, pos1.y + 0.1, pos1.z, 1, 0.02, 0.01, 0.02, 0);
            level.sendParticles(createDustParticle(0.8f, 0.0f, 0.0f, 0.6f),
                    pos2.x, pos2.y + 0.1, pos2.z, 1, 0.02, 0.01, 0.02, 0);
        }
        
        for (int i = 0; i <= widthPoints; i++) {
            double progress = (double) i / widthPoints - 0.5;
            Vec3 pos1 = center.add(right.scale(progress * width)); // Back edge at center
            Vec3 pos2 = center.add(forward.scale(length)).add(right.scale(progress * width)); // Front edge
            
            level.sendParticles(createDustParticle(0.8f, 0.0f, 0.0f, 0.6f),
                    pos1.x, pos1.y + 0.1, pos1.z, 1, 0.02, 0.01, 0.02, 0);
            level.sendParticles(createDustParticle(0.8f, 0.0f, 0.0f, 0.6f),
                    pos2.x, pos2.y + 0.1, pos2.z, 1, 0.02, 0.01, 0.02, 0);
        }
    }
    
    /**
     * Deal Heartstopper damage in rectangular AOE
     */
    private static void dealHeartstopperDamage(ServerPlayer player, ServerLevel level, Vec3 center, float yaw, float damage) {
        double length = 6.0;
        double width = 4.0;
        double yawRad = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 right = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad));
        
        AABB searchBox = new AABB(
                center.x - 10, center.y - 2, center.z - 10,
                center.x + 10, center.y + 3, center.z + 10);
        
        List<Entity> entities = level.getEntities(player, searchBox,
                e -> e instanceof LivingEntity && e != player);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                Vec3 toEntity = entity.position().subtract(center);
                double forwardDist = toEntity.dot(forward);
                double rightDist = toEntity.dot(right);
                
                // Rectangle starts at center and extends forward (0 to length)
                if (forwardDist >= 0 && forwardDist <= length && Math.abs(rightDist) <= width / 2) {
                    living.hurt(player.damageSources().playerAttack(player), damage);
                    
                    // Knockup
                    living.setDeltaMovement(living.getDeltaMovement().add(0, 0.8, 0));
                    living.hurtMarked = true;
                }
            }
        }
    }
    
    /**
     * Calculate Heartstopper healing from BLEED and GRIEVOUS WOUNDS
     */
    private static float calculateHeartstopperHealing(ServerLevel level, Vec3 center, float yaw) {
        double length = 6.0;
        double width = 4.0;
        double yawRad = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 right = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad));
        
        AABB searchBox = new AABB(
                center.x - 10, center.y - 2, center.z - 10,
                center.x + 10, center.y + 3, center.z + 10);
        
        List<Entity> entities = level.getEntities((Entity) null, searchBox,
                e -> e instanceof LivingEntity);
        
        int totalStacks = 0;
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                Vec3 toEntity = entity.position().subtract(center);
                double forwardDist = toEntity.dot(forward);
                double rightDist = toEntity.dot(right);
                
                // Rectangle starts at center and extends forward (0 to length)
                if (forwardDist >= 0 && forwardDist <= length && Math.abs(rightDist) <= width / 2) {
                    UUID entityId = entity.getUUID();
                    
                    // Count BLEED
                    if (activeBleedEffects.containsKey(entityId)) {
                        totalStacks++;
                    }
                    
                    // Count GRIEVOUS WOUNDS stacks
                    if (activeGrievousWounds.containsKey(entityId)) {
                        totalStacks += activeGrievousWounds.get(entityId).stacks;
                    }
                }
            }
        }
        
        return totalStacks * 2.0f; // 2 HP per stack
    }
    
    /**
     * Spawn Heartstopper slam effect
     */
    private static void spawnHeartstopperSlamEffect(ServerLevel level, Vec3 center, float yaw) {
        double length = 6.0;
        double width = 4.0;
        double yawRad = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 right = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad));
        
        // Fill rectangle with particles - starts at center and extends forward
        for (int i = 0; i < 50; i++) {
            double forwardOffset = RANDOM.nextDouble() * length; // 0 to length
            double rightOffset = (RANDOM.nextDouble() - 0.5) * width;
            Vec3 pos = center.add(forward.scale(forwardOffset)).add(right.scale(rightOffset));
            
            level.sendParticles(createDustParticle(0.7f, 0.0f, 0.0f, 1.2f),
                    pos.x, pos.y + 0.1, pos.z, 5, 0.1, 0.3, 0.1, 0.1);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                    pos.x, pos.y + 0.1, pos.z, 3, 0.1, 0.3, 0.1, 0.05);
        }
        
        // Bleed particles - adjust center forward by half length to match new rectangle position
        Vec3 effectCenter = center.add(forward.scale(length / 2));
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                net.minecraft.core.particles.ParticleTypes.BLOCK,
                net.minecraft.world.level.block.Blocks.RED_CONCRETE.defaultBlockState()),
                effectCenter.x, effectCenter.y + 1, effectCenter.z, 40, length / 4, 0.5, width / 4, 0.2);
        
        // Explosion
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                effectCenter.x, effectCenter.y + 0.5, effectCenter.z, 5, length / 4, 0.2, width / 4, 0);
    }
    
    /**
     * Update Tearing Hook pulls
     */
    public static void updateTearingHookPulls(ServerLevel level) {
        long currentTime = level.getGameTime();
        Iterator<TearingHookPull> iterator = scheduledHookPulls.iterator();
        
        while (iterator.hasNext()) {
            TearingHookPull pull = iterator.next();
            
            if (currentTime >= pull.executeAt) {
                if (pull.target.isAlive() && pull.owner.isAlive()) {
                    Vec3 targetPos = pull.target.position();
                    Vec3 ownerPos = pull.owner.position();
                    
                    if (pull.pullPlayerToTarget) {
                        // Pull player to target
                        Vec3 direction = targetPos.subtract(ownerPos).normalize();
                        pull.owner.setDeltaMovement(direction.scale(2.0));
                        pull.owner.hurtMarked = true;
                        
                        // Red chain visual
                        spawnTearingHookChain(level, ownerPos.add(0, pull.owner.getEyeHeight(), 0),
                                targetPos.add(0, pull.target.getBbHeight() * 0.5, 0), true);
                    } else {
                        // Pull target to player
                        Vec3 direction = ownerPos.subtract(targetPos).normalize();
                        pull.target.setDeltaMovement(direction.scale(2.0).add(0, 0.3, 0));
                        pull.target.hurtMarked = true;
                        
                        // Red chain visual
                        spawnTearingHookChain(level, ownerPos.add(0, pull.owner.getEyeHeight(), 0),
                                targetPos.add(0, pull.target.getBbHeight() * 0.5, 0), true);
                    }
                }
                
                iterator.remove();
            }
        }
    }
    
    // ===== BERSERKER ABILITY HELPER METHODS AND DATA STRUCTURES =====
    
    /**
     * Axe Throw projectile data
     */
    public static class AxeThrowProjectile {
        public final ServerPlayer owner;
        public final ServerLevel level;
        public Vec3 position;
        public Vec3 direction;
        public final float damage;
        public int ticksAlive;
        public final int maxOutTicks; // Max ticks before returning
        public final float speed;
        public boolean returning;
        public final List<UUID> hitEntitiesOutbound;
        public final List<UUID> hitEntitiesReturn;
        public final Vec3 startPos;
        
        public AxeThrowProjectile(ServerPlayer owner, ServerLevel level, Vec3 startPos, Vec3 direction, float damage) {
            this.owner = owner;
            this.level = level;
            this.position = startPos;
            this.direction = direction.normalize();
            this.damage = damage;
            this.ticksAlive = 0;
            this.maxOutTicks = AXE_MAX_OUT_TICKS;
            this.speed = 0.7f;
            this.returning = false;
            this.hitEntitiesOutbound = new ArrayList<>();
            this.hitEntitiesReturn = new ArrayList<>();
            this.startPos = startPos;
        }
    }
    
    private static final List<AxeThrowProjectile> activeAxeProjectiles = new ArrayList<>();
    
    /**
     * Spawn Axe Throw projectile
     */
    private static void spawnAxeThrowProjectile(ServerPlayer player, ServerLevel level, Vec3 startPos, Vec3 direction, float damage) {
        AxeThrowProjectile projectile = new AxeThrowProjectile(player, level, startPos, direction, damage);
        activeAxeProjectiles.add(projectile);
    }
    
    /**
     * Update all Axe Throw projectiles
     */
    public static void updateAxeThrowProjectiles() {
        Iterator<AxeThrowProjectile> iterator = activeAxeProjectiles.iterator();
        while (iterator.hasNext()) {
            AxeThrowProjectile proj = iterator.next();
            proj.ticksAlive++;
            
            if (!updateAxeThrowProjectile(proj)) {
                // Projectile returned - restore charge and apply cooldown
                var rpgData = proj.owner.getData(ModAttachments.PLAYER_RPG);
                rpgData.restoreAxeThrowCharge();
                
                // Apply cooldown when charge is restored (7 seconds base)
                String abilityId = "berserker_ability_1";
                int baseCooldownTicks = AbilityUtils.getAbilityCooldownTicks("berserker", 1);
                
                // Apply cooldown reduction stat
                var stats = proj.owner.getData(ModAttachments.PLAYER_STATS);
                int cooldownReduction = stats.getIntStatValue(StatType.COOLDOWN_REDUCTION);
                int adjustedCooldownTicks = (int) (baseCooldownTicks * (1.0 - cooldownReduction / 100.0));
                adjustedCooldownTicks = Math.max(adjustedCooldownTicks, 20); // Minimum 1 second cooldown
                
                rpgData.setAbilityCooldown(abilityId, adjustedCooldownTicks);
                
                // Sync cooldowns to client
                sendToPlayer(new PacketSyncCooldowns(rpgData.getAllCooldowns()), proj.owner);
                
                iterator.remove();
            }
        }
    }
    
    /**
     * Update single Axe Throw projectile
     */
    private static boolean updateAxeThrowProjectile(AxeThrowProjectile proj) {
        if (!proj.owner.isAlive()) {
            return false;
        }
        
        // Calculate current target position (player's chest)
        Vec3 playerChest = proj.owner.position().add(0, proj.owner.getEyeHeight() - 0.5, 0);
        
        if (!proj.returning) {
            // Move outward
            proj.position = proj.position.add(proj.direction.scale(proj.speed));
            
            // Check for block collision
            var blockHit = proj.level.clip(new net.minecraft.world.level.ClipContext(
                    proj.position.subtract(proj.direction.scale(0.3)),
                    proj.position,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    proj.owner));
            
            if (blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                proj.returning = true;
            }
            
            // Check if max distance reached
            if (proj.ticksAlive >= proj.maxOutTicks) {
                proj.returning = true;
            }
        } else {
            // Return to player
            Vec3 toPlayer = playerChest.subtract(proj.position).normalize();
            proj.position = proj.position.add(toPlayer.scale(proj.speed * 1.2)); // Slightly faster return
            
            // Check if returned to player
            if (proj.position.distanceTo(playerChest) < 1.5) {
                return false; // Remove projectile
            }
        }
        
        // Check for entity collision (pierce through)
        AABB hitbox = new AABB(
                proj.position.x - 0.8, proj.position.y - 0.8, proj.position.z - 0.8,
                proj.position.x + 0.8, proj.position.y + 0.8, proj.position.z + 0.8);
        
        List<Entity> entities = proj.level.getEntities(proj.owner, hitbox,
                e -> e instanceof LivingEntity && e != proj.owner);
        
        List<UUID> hitList = proj.returning ? proj.hitEntitiesReturn : proj.hitEntitiesOutbound;
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living && !hitList.contains(entity.getUUID())) {
                living.hurt(proj.owner.damageSources().playerAttack(proj.owner), proj.damage);
                hitList.add(entity.getUUID());
                
                // Impact particles - orange
                proj.level.sendParticles(createDustParticle(1.0f, 0.5f, 0.0f, 1.0f),
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                        10, 0.3, 0.3, 0.3, 0.1);
                proj.level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                        5, 0.2, 0.2, 0.2, 0.1);
            }
        }
        
        // Spawn visual effect - spinning axe particles and line to player
        if (proj.ticksAlive % 2 == 0) {
            // Spinning axe effect
            int spinParticles = 8;
            double spinAngle = (proj.ticksAlive * 0.5) % (2 * Math.PI);
            for (int i = 0; i < spinParticles; i++) {
                double angle = spinAngle + (i / (double) spinParticles) * 2 * Math.PI;
                double radius = 0.4;
                double x = proj.position.x + Math.cos(angle) * radius;
                double z = proj.position.z + Math.sin(angle) * radius;
                
                proj.level.sendParticles(createDustParticle(1.0f, 0.5f, 0.0f, 0.8f),
                        x, proj.position.y, z, 1, 0.02, 0.02, 0.02, 0);
            }
            
            // Draw particle line to player's chest
            Vec3 lineDir = playerChest.subtract(proj.position);
            double lineDist = lineDir.length();
            lineDir = lineDir.normalize();
            int linePoints = (int) (lineDist * 2);
            for (int i = 0; i < linePoints; i += 2) {
                double progress = (double) i / linePoints;
                Vec3 linePos = proj.position.add(lineDir.scale(progress * lineDist));
                proj.level.sendParticles(createDustParticle(1.0f, 0.6f, 0.2f, 0.4f),
                        linePos.x, linePos.y, linePos.z, 1, 0.02, 0.02, 0.02, 0);
            }
        }
        
        return true;
    }
    
    /**
     * Spawn Blood Oath effect
     */
    private static void spawnBloodOathEffect(ServerLevel level, Vec3 center) {
        // Blood particles burst from player
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                net.minecraft.core.particles.ParticleTypes.BLOCK,
                net.minecraft.world.level.block.Blocks.RED_CONCRETE.defaultBlockState()),
                center.x, center.y + 1, center.z, 40, 0.5, 0.5, 0.5, 0.2);
        
        // Dark red aura
        for (int ring = 0; ring < 3; ring++) {
            double radius = 0.8 + ring * 0.3;
            int points = 16;
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                
                level.sendParticles(createDustParticle(0.6f, 0.0f, 0.0f, 1.0f),
                        x, center.y + 0.5 + ring * 0.3, z, 2, 0.05, 0.1, 0.05, 0.02);
            }
        }
        
        // Upward spiral effect
        for (int i = 0; i < 20; i++) {
            double progress = i / 20.0;
            double spiralAngle = progress * 4 * Math.PI;
            double spiralRadius = 0.5 * (1.0 - progress);
            double x = center.x + Math.cos(spiralAngle) * spiralRadius;
            double z = center.z + Math.sin(spiralAngle) * spiralRadius;
            
            level.sendParticles(createDustParticle(1.0f, 0.3f, 0.0f, 0.8f),
                    x, center.y + progress * 2.5, z, 2, 0.03, 0.03, 0.03, 0.01);
        }
    }
    
    /**
     * Spawn Frenzy slashes
     */
    private static void spawnFrenzySlashes(ServerPlayer player, ServerLevel level, Vec3 center, float damage, int totalSlashes, boolean rageSlashes) {
        int durationTicks = 60; // 3 seconds
        int ticksPerSlash = durationTicks / totalSlashes;
        
        // Reset slash counter for this player
        UUID playerUuid = player.getUUID();
        frenzySlashCounts.put(playerUuid, 0);
        
        for (int slash = 0; slash < totalSlashes; slash++) {
            final int slashIndex = slash;
            level.getServer().tell(new net.minecraft.server.TickTask(
                    level.getServer().getTickCount() + slash * ticksPerSlash,
                    () -> {
                        if (!player.isAlive()) {
                            // Cleanup if player dies
                            frenzySlashCounts.remove(playerUuid);
                            return;
                        }
                        
                        // Increment slash counter
                        int currentSlashes = frenzySlashCounts.getOrDefault(playerUuid, 0);
                        frenzySlashCounts.put(playerUuid, currentSlashes + 1);
                        
                        // Calculate random slash angle
                        float baseYaw = player.getYRot();
                        float randomAngle = (RANDOM.nextFloat() - 0.5f) * 120; // -60 to +60 degrees
                        float slashYaw = baseYaw + randomAngle;
                        
                        // Deal damage in small arc
                        dealDamageInArc(player, damage, 3.0, 60.0);
                        
                        // Spawn slash visual
                        spawnFrenzySlashEffect(level, player.position().add(0, 1, 0), slashYaw, rageSlashes && slashIndex >= 8);
                        
                        // Cleanup on final slash
                        if (slashIndex == totalSlashes - 1) {
                            frenzySlashCounts.remove(playerUuid);
                        }
                    }
            ));
        }
    }
    
    /**
     * Spawn single Frenzy slash effect
     */
    private static void spawnFrenzySlashEffect(ServerLevel level, Vec3 center, float yaw, boolean isRageSlash) {
        double yawRad = Math.toRadians(yaw);
        double arcDegrees = 60;
        double halfArc = Math.toRadians(arcDegrees / 2.0);
        double radius = 3.0;
        
        int arcParticles = 15;
        for (int i = 0; i < arcParticles; i++) {
            double angle = -halfArc + (i / (double) arcParticles) * (2 * halfArc);
            double finalAngle = yawRad + angle + Math.PI / 2;
            double dist = radius * (0.7 + RANDOM.nextDouble() * 0.3);
            double x = center.x + Math.cos(finalAngle) * dist;
            double z = center.z + Math.sin(finalAngle) * dist;
            double y = center.y + (RANDOM.nextDouble() - 0.5) * 0.8;
            
            // Gray for normal, orange for RAGE slashes
            if (isRageSlash) {
                level.sendParticles(createDustParticle(1.0f, 0.5f, 0.0f, 0.9f),
                        x, y, z, 2, 0.08, 0.08, 0.08, 0.02);
            } else {
                level.sendParticles(createDustParticle(0.5f, 0.5f, 0.5f, 0.9f),
                        x, y, z, 2, 0.08, 0.08, 0.08, 0.02);
            }
        }
        
        // Sweep attack particle
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                center.x, center.y, center.z, 1, 0.3, 0.2, 0.3, 0);
    }
    
    /**
     * Spawn Unbound Carnage effect
     */
    private static void spawnUnboundCarnageEffect(ServerLevel level, Vec3 center) {
        // Massive explosion of orange particles
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y + 1, center.z, 1, 0, 0, 0, 0);
        
        // Multiple expanding rings of orange
        for (int wave = 0; wave < 5; wave++) {
            final int waveIndex = wave;
            level.getServer().tell(new net.minecraft.server.TickTask(
                    level.getServer().getTickCount() + wave * 3,
                    () -> {
                        double radius = 1.0 + waveIndex * 0.8;
                        int points = 24;
                        for (int p = 0; p < points; p++) {
                            double angle = (double) p / points * 2 * Math.PI;
                            double x = center.x + Math.cos(angle) * radius;
                            double z = center.z + Math.sin(angle) * radius;
                            
                            level.sendParticles(createDustParticle(1.0f, 0.5f, 0.0f, 1.2f),
                                    x, center.y + 0.5, z, 3, 0.05, 0.1, 0.05, 0.02);
                        }
                    }
            ));
        }
        
        // Upward flame burst
        for (int i = 0; i < 30; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = RANDOM.nextDouble() * 1.5;
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    x, center.y + 0.5, z, 1, 0.1, 0.5, 0.1, 0.1);
        }
        
        // Orange dust burst
        spawnDustParticlesBurst(level, center.add(0, 1, 0), 3.0, 1.0f, 0.5f, 0.0f, 50);
        
        // Crit particles
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                center.x, center.y + 1, center.z, 30, 1.0, 0.5, 1.0, 0.3);
    }
    
    /**
     * Update Berserker RAGE system (called every tick)
     */
    public static void updateBerserkerRage(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            var rpgData = player.getData(ModAttachments.PLAYER_RPG);
            if (rpgData.getCurrentClass() == null || !rpgData.getCurrentClass().equalsIgnoreCase("berserker")) {
                continue;
            }
            
            long currentTime = level.getGameTime();
            
            // Handle enhanced enraged state (Unbound Carnage)
            if (rpgData.isEnhancedEnraged()) {
                if (currentTime >= rpgData.getEnhancedEnragedEndTime()) {
                    // End enhanced enraged, start exhaustion
                    rpgData.setEnhancedEnraged(false);
                    rpgData.setRage(0);
                    rpgData.setExhausted(true);
                    rpgData.setExhaustedEndTime(currentTime + 200); // 10 seconds exhaustion
                    
                    // Apply exhaustion debuffs
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0)); // -20% speed
                    
                    // Sync RAGE state to client
                    syncRageToClient(player);
                    
                    player.displayClientMessage(Component.literal("§8§lEXHAUSTED... §7Cannot generate RAGE"), true);
                }
                // Decay rage while enhanced enraged
                if (currentTime % 10 == 0) { // Every 0.5 seconds
                    rpgData.decayRage(5);
                }
                continue;
            }
            
            // Handle exhaustion
            if (rpgData.isExhausted()) {
                if (currentTime >= rpgData.getExhaustedEndTime()) {
                    rpgData.setExhausted(false);
                    syncRageToClient(player);
                    player.displayClientMessage(Component.literal("§aExhaustion ended. §6RAGE generation restored!"), true);
                }
                continue;
            }
            
            // Handle normal enraged state
            if (rpgData.isEnraged()) {
                // Decay rage every 0.5 seconds
                if (currentTime % 10 == 0) {
                    rpgData.decayRage(PlayerRPGData.RAGE_DECAY_RATE);
                    syncRageToClient(player);
                    
                    if (rpgData.getRage() <= 0) {
                        // Exit enraged state
                        rpgData.setEnraged(false);
                        player.displayClientMessage(Component.literal("§7Enraged state ended."), true);
                    }
                }
            } else {
                // Check if rage reached 100 - enter enraged state
                if (rpgData.getRage() >= PlayerRPGData.MAX_RAGE) {
                    rpgData.setEnraged(true);
                    syncRageToClient(player);
                    
                    // Apply enraged buffs: +25% speed, +30% damage
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1)); // Speed II
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1)); // Strength II
                    
                    // Visual effect
                    spawnEnragedActivationEffect(level, player.position());
                    
                    player.displayClientMessage(Component.literal("§c§l💢 ENRAGED! §6+25% Speed, +30% Damage, 5% Lifesteal"), true);
                }
            }
        }
        
        // Update axe throw projectiles
        updateAxeThrowProjectiles();
    }
    
    /**
     * Spawn enraged activation effect
     */
    private static void spawnEnragedActivationEffect(ServerLevel level, Vec3 center) {
        // Orange burst
        spawnDustParticlesBurst(level, center.add(0, 1, 0), 2.5, 1.0f, 0.5f, 0.0f, 30);
        
        // Expanding rings
        for (int ring = 0; ring < 3; ring++) {
            double radius = 1.0 + ring * 0.5;
            int points = 16;
            for (int p = 0; p < points; p++) {
                double angle = (double) p / points * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                
                level.sendParticles(createDustParticle(1.0f, 0.4f, 0.0f, 1.0f),
                        x, center.y + 0.5, z, 3, 0.05, 0.1, 0.05, 0.02);
            }
        }
        
        // Crit particles
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                center.x, center.y + 1, center.z, 20, 0.5, 0.3, 0.5, 0.2);
    }
    
    /**
     * Sync Berserker RAGE data to client
     */
    private static void syncRageToClient(ServerPlayer player) {
        var rpgData = player.getData(ModAttachments.PLAYER_RPG);
        sendToPlayer(new PacketSyncRage(
                rpgData.getRage(),
                rpgData.isEnraged(),
                rpgData.isEnhancedEnraged(),
                rpgData.isExhausted(),
                rpgData.getAxeThrowCharges()
        ), player);
    }
    
    /**
     * Add RAGE from dealing damage (5% of damage dealt)
     */
    public static void addRageFromDamageDealt(ServerPlayer player, float damageDealt) {
        var rpgData = player.getData(ModAttachments.PLAYER_RPG);
        if (rpgData.getCurrentClass() == null || !rpgData.getCurrentClass().equalsIgnoreCase("berserker")) {
            return;
        }
        
        // Don't gain RAGE while enraged or enhanced enraged
        if (rpgData.isEnraged() || rpgData.isEnhancedEnraged() || rpgData.isExhausted()) {
            return;
        }
        
        int rageGain = (int) (damageDealt * RAGE_GAIN_PERCENT);
        if (rageGain > 0) {
            rpgData.addRage(rageGain);
            syncRageToClient(player);
        }
    }
    
    /**
     * Add RAGE from taking damage (10 per hit, 3s cooldown)
     */
    public static void addRageFromDamageTaken(ServerPlayer player, ServerLevel level) {
        var rpgData = player.getData(ModAttachments.PLAYER_RPG);
        if (rpgData.getCurrentClass() == null || !rpgData.getCurrentClass().equalsIgnoreCase("berserker")) {
            return;
        }
        
        // Don't gain RAGE while enraged or enhanced enraged
        if (rpgData.isEnraged() || rpgData.isEnhancedEnraged() || rpgData.isExhausted()) {
            return;
        }
        
        long currentTime = level.getGameTime();
        if (currentTime - rpgData.getLastRageFromDamageTaken() >= PlayerRPGData.RAGE_DAMAGE_TAKEN_COOLDOWN_TICKS) {
            rpgData.addRage(PlayerRPGData.RAGE_DAMAGE_TAKEN_AMOUNT);
            rpgData.setLastRageFromDamageTaken(currentTime);
            syncRageToClient(player);
        }
    }
    
    /**
     * Apply Berserker lifesteal (5% of damage dealt while enraged)
     */
    public static void applyBerserkerLifesteal(ServerPlayer player, float damageDealt) {
        var rpgData = player.getData(ModAttachments.PLAYER_RPG);
        if (rpgData.getCurrentClass() == null || !rpgData.getCurrentClass().equalsIgnoreCase("berserker")) {
            return;
        }
        
        // Only lifesteal while enraged (not enhanced enraged)
        if (rpgData.isEnraged() || rpgData.isEnhancedEnraged()) {
            float healAmount = damageDealt * LIFESTEAL_PERCENT;
            if (healAmount > 0) {
                player.heal(healAmount);
            }
        }
    }
    
    /**
     * Handle Unbound Carnage immortality (prevent death, set to 1 HP)
     */
    public static boolean handleUnboundCarnageImmortality(ServerPlayer player) {
        var rpgData = player.getData(ModAttachments.PLAYER_RPG);
        if (rpgData.getCurrentClass() == null || !rpgData.getCurrentClass().equalsIgnoreCase("berserker")) {
            return false;
        }
        
        if (rpgData.isEnhancedEnraged()) {
            // Prevent death, set to 1 HP
            player.setHealth(1.0f);
            return true;
        }
        return false;
    }
}
