package net.frostimpact.rpgclasses_v2.entity.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for managing custom enemy definitions.
 * Custom enemies are registered here and can be looked up by ID.
 * 
 * Each custom enemy has a unique summon ID that can be used with commands:
 * /summon rpgclasses_v2:<summon_id>
 * 
 * For testing purposes, custom enemies will also target armor stands
 * that have the "player" tag.
 */
public class CustomEntityRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomEntityRegistry.class);
    private static final Map<String, CustomEnemy> customEnemies = new HashMap<>();
    
    /**
     * Register a custom enemy
     */
    public static void register(CustomEnemy enemy) {
        String enemyId = enemy.getId();
        if (customEnemies.containsKey(enemyId)) {
            LOGGER.warn("Custom enemy {} is already registered. Overwriting.", enemyId);
        }
        customEnemies.put(enemyId, enemy);
        LOGGER.debug("Registered custom enemy: {} (summon: {})", enemyId, enemy.getSummonId());
    }
    
    /**
     * Get a custom enemy by ID
     */
    public static Optional<CustomEnemy> getEnemy(String enemyId) {
        return Optional.ofNullable(customEnemies.get(enemyId));
    }
    
    /**
     * Get a custom enemy by summon ID
     */
    public static Optional<CustomEnemy> getEnemyBySummonId(String summonId) {
        return customEnemies.values().stream()
            .filter(e -> e.getSummonId().equals(summonId))
            .findFirst();
    }
    
    /**
     * Check if a custom enemy is registered
     */
    public static boolean isRegistered(String enemyId) {
        return customEnemies.containsKey(enemyId);
    }
    
    /**
     * Get all registered custom enemy IDs
     */
    public static Iterable<String> getAllEnemyIds() {
        return customEnemies.keySet();
    }
    
    /**
     * Get all registered custom enemies
     */
    public static Iterable<CustomEnemy> getAllEnemies() {
        return customEnemies.values();
    }
    
    /**
     * Get the count of registered enemies
     */
    public static int getEnemyCount() {
        return customEnemies.size();
    }
    
    /**
     * Initialize all example entities
     * 
     * This registers the following custom enemies demonstrating each AI behavior:
     * 
     * - GRUNT (NORMAL): Targets closest player
     * - SKELETON_ARCHER (RANGED): Keeps distance from players
     * - ELITE_MARKSMAN (RANGED_ADVANCED): Keeps distance, flees when too close
     * - SHADOW_STALKER (FLANKER): Stays outside line of sight until in range
     * - SILENT_BLADE (ASSASSIN): Targets lowest HP player
     * - IRON_GOLIATH (BRUTE): Targets highest HP player
     * - GOBLIN_SCOUT (COWARD): Runs away when below 20% HP
     * - EAGLE_EYE (FAR_SIGHTED): Targets farthest player
     * - CHAOS_IMP (CHAOTIC): Targets random player
     */
    public static void initializeExampleEntities() {
        // Register all example enemies
        register(ExampleEnemies.GRUNT);
        register(ExampleEnemies.SKELETON_ARCHER);
        register(ExampleEnemies.ELITE_MARKSMAN);
        register(ExampleEnemies.SHADOW_STALKER);
        register(ExampleEnemies.SILENT_BLADE);
        register(ExampleEnemies.IRON_GOLIATH);
        register(ExampleEnemies.GOBLIN_SCOUT);
        register(ExampleEnemies.EAGLE_EYE);
        register(ExampleEnemies.CHAOS_IMP);
        
        LOGGER.info("Initialized {} custom enemies", customEnemies.size());
        LOGGER.info("Custom enemies can target players and armor stands with the 'player' tag");
        
        // Log summon commands for reference
        LOGGER.info("Summon commands:");
        for (CustomEnemy enemy : customEnemies.values()) {
            LOGGER.info("  /{} - {} (AI: {})", 
                enemy.getSummonCommand(), 
                enemy.getDisplayName(),
                enemy.getAiPreset().getDisplayName());
        }
    }
    
    /**
     * Get a formatted list of all enemy summon commands
     * @return Array of summon command strings
     */
    public static String[] getSummonCommands() {
        return customEnemies.values().stream()
            .map(CustomEnemy::getSummonCommand)
            .toArray(String[]::new);
    }
    
    /**
     * Get information about all registered enemies
     * @return Formatted string with enemy information
     */
    public static String getEnemyInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Custom Enemy Registry ===\n");
        sb.append("Total enemies: ").append(customEnemies.size()).append("\n\n");
        
        for (CustomEnemy enemy : customEnemies.values()) {
            sb.append("--- ").append(enemy.getDisplayName()).append(" ---\n");
            sb.append("ID: ").append(enemy.getId()).append("\n");
            sb.append("AI Behavior: ").append(enemy.getAiPreset().getDisplayName()).append("\n");
            sb.append("  ").append(enemy.getAiPreset().getDescription()).append("\n");
            sb.append("Stats: HP=").append(enemy.getMaxHealth())
              .append(", DMG=").append(enemy.getDamage())
              .append(", DEF=").append(enemy.getDefense()).append("\n");
            sb.append("Summon: /").append(enemy.getSummonCommand()).append("\n\n");
        }
        
        return sb.toString();
    }
}
