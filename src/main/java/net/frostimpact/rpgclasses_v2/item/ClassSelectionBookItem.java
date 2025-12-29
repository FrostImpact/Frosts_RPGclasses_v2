package net.frostimpact.rpgclasses_v2.item;

import net.frostimpact.rpgclasses_v2.rpgclass.ClassSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ClassSelectionBookItem extends Item {
    public ClassSelectionBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (level.isClientSide) {
            Minecraft.getInstance().setScreen(new ClassSelectionScreen());
        }
        
        return InteractionResultHolder.success(itemStack);
    }
}
