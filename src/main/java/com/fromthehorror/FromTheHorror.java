package com.fromthehorror;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FromTheHorror implements ModInitializer {
    public static final String ID = "fromthehorror";
    public static EntityType<DwellerEntity> DWELLER;
    public static EntityType<WatcherEntity> WATCHER;
    public static Item FLASHLIGHT;

    @Override
    public void onInitialize() {
        DWELLER = Registry.register(Registries.ENTITY_TYPE, new Identifier(ID, "dweller"),
                FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DwellerEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 2.2f)).trackRangeBlocks(64).build());
        WATCHER = Registry.register(Registries.ENTITY_TYPE, new Identifier(ID, "watcher"),
                FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, WatcherEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 1.95f)).trackRangeBlocks(96).build());
        FabricDefaultAttributeRegistry.register(DWELLER, DwellerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WATCHER, WatcherEntity.createAttributes());

        FLASHLIGHT = Registry.register(Registries.ITEM, new Identifier(ID, "flashlight"),
                new FlashlightItem(new FabricItemSettings().maxCount(1).maxDamage(1200)));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(e -> e.add(FLASHLIGHT));

        ServerTickEvents.END_WORLD_TICK.register(DreadDirector::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> DreadDirector.clear());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> DreadDirector.forget(handler.player.getUuid()));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
            dispatcher.register(CommandManager.literal("horror")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("spawn")
                    .then(CommandManager.literal("dweller").executes(c ->
                        spawnCmd(c.getSource(), DWELLER, 10, 14)))
                    .then(CommandManager.literal("watcher").executes(c ->
                        spawnCmd(c.getSource(), WATCHER, 20, 26))))
                .then(CommandManager.literal("event").executes(c -> {
                    DreadDirector.trigger(c.getSource().getWorld(), c.getSource().getPlayerOrThrow());
                    return 1;
                }))));
    }

    private static int spawnCmd(ServerCommandSource src, EntityType<? extends HostileEntity> type, int min, int max)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        boolean ok = DreadDirector.spawn(src.getWorld(), src.getPlayerOrThrow(), type, min, max);
        src.sendFeedback(() -> Text.literal(ok ? "Something stirs..."
                : "Could not spawn (one already exists nearby, or no space found)."), false);
        return ok ? 1 : 0;
    }
}
