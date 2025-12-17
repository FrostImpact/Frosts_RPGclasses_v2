package net.frostimpact.rpgclasses_v2.combat.slash;

import net.frostimpact.rpgclasses_v2.item.weapon.WeaponType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * Entry point for spawning weapon slash animations
 */
public class SlashRenderer {

    /**
     * Spawn slash particles based on weapon type and combo hit
     */
    public static void spawnSlashParticles(ServerLevel level, Player player, WeaponType weaponType, int comboHit) {
        SlashAnimation.startSlashAnimation(level, player, weaponType, comboHit);
    }
}