package net.frostimpact.rpgclasses_v2.item;

import net.frostimpact.rpgclasses_v2.networking.ModMessages;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncMana;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Item that instantly restores the player's mana to maximum
 */
public class ManaRestorationItem extends Item {
    public ManaRestorationItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            var rpgData = serverPlayer.getData(ModAttachments.PLAYER_RPG);
            int currentMana = rpgData.getMana();
            int maxMana = rpgData.getMaxMana();
            
            // Only use if not already at max mana
            if (currentMana < maxMana) {
                // Restore mana to max
                rpgData.setMana(maxMana);
                
                // Sync mana to client
                ModMessages.sendToPlayer(new PacketSyncMana(rpgData.getMana(), rpgData.getMaxMana()), serverPlayer);
                
                // Consume the item
                if (!player.getAbilities().instabuild) {
                    itemStack.shrink(1);
                }
                
                // Play sound effect
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.5F);
                
                // Spawn particle effects
                if (level instanceof ServerLevel serverLevel) {
                    spawnManaRestoreParticles(serverLevel, player);
                }
                
                // Display message
                int manaRestored = maxMana - currentMana;
                player.displayClientMessage(
                        Component.literal("§9Mana restored to max! §7(+" + manaRestored + " mana)"), true);
                
                return InteractionResultHolder.consume(itemStack);
            } else {
                // Already at max mana
                player.displayClientMessage(
                        Component.literal("§7Your mana is already full!"), true);
                return InteractionResultHolder.fail(itemStack);
            }
        }
        
        return InteractionResultHolder.success(itemStack);
    }
    
    /**
     * Spawn dramatic particle effects when mana is restored
     */
    private void spawnManaRestoreParticles(ServerLevel level, Player player) {
        double x = player.getX();
        double y = player.getY() + 1;
        double z = player.getZ();
        
        // Spiral of enchantment particles rising up
        for (int i = 0; i < 40; i++) {
            double angle = (double) i / 40 * 4 * Math.PI;
            double radius = 0.5 + (double) i / 40;
            double yOffset = (double) i / 40 * 2;
            double particleX = x + Math.cos(angle) * radius;
            double particleZ = z + Math.sin(angle) * radius;
            
            level.sendParticles(ParticleTypes.ENCHANT, particleX, y + yOffset, particleZ, 
                    1, 0, 0.1, 0, 0.02);
        }
        
        // Ring burst at player position
        for (int i = 0; i < 24; i++) {
            double angle = (double) i / 24 * 2 * Math.PI;
            double ringX = x + Math.cos(angle) * 1.5;
            double ringZ = z + Math.sin(angle) * 1.5;
            
            level.sendParticles(ParticleTypes.WITCH, ringX, y + 0.5, ringZ, 
                    2, 0.1, 0.1, 0.1, 0.01);
        }
        
        // Central glow
        level.sendParticles(ParticleTypes.GLOW, x, y + 1, z, 15, 0.3, 0.5, 0.3, 0.01);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 1, z, 8, 0.2, 0.3, 0.2, 0.02);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("§9Instantly restores mana to maximum"));
        tooltipComponents.add(Component.literal("§7Right-click to use"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
