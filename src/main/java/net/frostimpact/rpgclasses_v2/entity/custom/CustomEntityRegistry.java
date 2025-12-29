package net.frostimpact.rpgclasses_v2.entity.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for managing custom enemy definitions.
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
        LOGGER.debug("Registered custom enemy: {}", enemyId);
    }
    
    /**
     * Get a custom enemy by ID
     */
    public static Optional<CustomEnemy> getEnemy(String enemyId) {
        return Optional.ofNullable(customEnemies.get(enemyId));
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
     * Initialize example custom entities
     */
    public static void initializeExampleEntities() {
        // Register the example Shadow Wraith enemy
        register(ExampleEnemies.SHADOW_WRAITH);
        
        LOGGER.info("Initialized {} custom enemies", customEnemies.size());
    }
}
