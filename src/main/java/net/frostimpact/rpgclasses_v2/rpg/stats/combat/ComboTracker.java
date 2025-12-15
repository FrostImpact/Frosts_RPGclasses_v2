package net.frostimpact.rpgclasses_v2.rpg.stats.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks combo state for each player
 */
public class ComboTracker {
    private static final Map<UUID, ComboState> playerCombos = new HashMap<>();
    
    public static class ComboState {
        private int comboCount = 0;
        private int ticksSinceLastAttack = 0;
        
        public int getComboCount() {
            return comboCount;
        }
        
        public void incrementCombo() {
            comboCount++;
            if (comboCount > CombatConfig.MAX_COMBO_COUNT) {
                comboCount = 1; // Reset to 1 after 4-hit combo
            }
            ticksSinceLastAttack = 0;
        }
        
        public void resetCombo() {
            comboCount = 0;
            ticksSinceLastAttack = 0;
        }
        
        public void tick() {
            if (comboCount > 0) {
                ticksSinceLastAttack++;
                if (ticksSinceLastAttack >= CombatConfig.COMBO_RESET_TIME) {
                    resetCombo();
                }
            }
        }
    }
    
    public static ComboState getComboState(UUID playerUUID) {
        return playerCombos.computeIfAbsent(playerUUID, k -> new ComboState());
    }
    
    public static void tick(UUID playerUUID) {
        ComboState state = playerCombos.get(playerUUID);
        if (state != null) {
            state.tick();
        }
    }
    
    public static void removePlayer(UUID playerUUID) {
        playerCombos.remove(playerUUID);
    }
}
