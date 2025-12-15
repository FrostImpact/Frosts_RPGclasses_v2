package net.frostimpact.rpgclasses_v2.rpg;

import net.frostimpact.rpgclasses_v2.rpg.stats.PlayerStats;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = 
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "rpgclasses_v2");

    public static final Supplier<AttachmentType<PlayerRPGData>> PLAYER_RPG = ATTACHMENT_TYPES.register(
        "player_rpg",
        () -> AttachmentType.serializable(PlayerRPGData::new).build()
    );

    public static final Supplier<AttachmentType<PlayerStats>> PLAYER_STATS = ATTACHMENT_TYPES.register(
        "player_stats",
        () -> AttachmentType.serializable(PlayerStats::new).build()
    );

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
