package de.tuefteltyp.virtualhopper;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.Registry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public final class VirtualHopper implements ModInitializer {
    public static final String ID = "virtualhopper";
    public static final Map<String, Endpoint> ENDPOINTS = new HashMap<>();
    public record Endpoint(ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos pos) {}
    public static final Identifier BLOCK_ID = Identifier.fromNamespaceAndPath(ID, "virtual_hopper");
    public static final Block BLOCK = Registry.register(BuiltInRegistries.BLOCK, BLOCK_ID,
            new VirtualHopperBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER).setId(ResourceKey.create(Registries.BLOCK, BLOCK_ID))));
    public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, BLOCK_ID,
            new BlockItem(BLOCK, new Item.Properties().useBlockDescriptionPrefix().setId(ResourceKey.create(Registries.ITEM, BLOCK_ID))));
    public static final BlockEntityType<VirtualHopperBlockEntity> BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, BLOCK_ID,
            FabricBlockEntityTypeBuilder.create(VirtualHopperBlockEntity::new, BLOCK).build());

    public static void remember(VirtualHopperBlockEntity entity) {
        if (entity.getLevel() instanceof ServerLevel level) HopperNameRegistry.registerIfNeeded(entity, level);
    }
    public static void forget(VirtualHopperBlockEntity entity) {
        HopperNameRegistry.remove(entity);
    }
    public static VirtualHopperBlockEntity resolve(net.minecraft.server.MinecraftServer server, String name) {
        Endpoint loc = ENDPOINTS.get(name);
        if (loc == null) return null;
        ServerLevel level = server.getLevel(loc.dimension());
        if (level == null || !level.hasChunkAt(loc.pos())) return null;
        BlockEntity blockEntity = level.getBlockEntity(loc.pos());
        if (blockEntity instanceof VirtualHopperBlockEntity hopper && name.equals(hopper.getHopperName())) return hopper;
        return null; // Never discard a reservation merely because a chunk is unloaded.
    }
    /** Returns a reserved endpoint even if the destination chunk is not currently loaded. */
    public static Endpoint findReservedEndpoint(String name) {
        return ENDPOINTS.get(name);
    }
    public static boolean claim(VirtualHopperBlockEntity hopper, String name) {
        return HopperNameRegistry.rename(hopper, name);
    }

    @Override public void onInitialize() {
        VirtualHopperConfig.load();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(HopperNameRegistry::load);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> HopperNameRegistry.save());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> entries.accept(ITEM));
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(
                Commands.literal("virtualhopper")
                        .then(Commands.literal("open")
                            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    VirtualHopperBlockEntity hopper = editable(player, BlockPosArgument.getBlockPos(ctx, "pos"));
                                    if (hopper == null) return 0;
                                    player.openMenu(hopper);
                                    return 1;
                                })))
                        .then(Commands.literal("list").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            player.sendSystemMessage(Component.translatable("message.virtualhopper.available"));
                            for (var entry : ENDPOINTS.entrySet()) {
                                var point = entry.getValue();
                                MutableComponent name = Component.literal(entry.getKey() + " [" + point.dimension().identifier() + " " + point.pos().toShortString() + "]");
                                player.sendSystemMessage(name);
                            }
                            return 1;
                        }))
                        .then(Commands.literal("name")
                            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("name", StringArgumentType.word())
                                    .executes(ctx -> {
                                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                                        VirtualHopperBlockEntity hopper = editable(p, BlockPosArgument.getBlockPos(ctx, "pos"));
                                        if (hopper == null) return 0;
                                        String name = StringArgumentType.getString(ctx, "name");
                                        boolean ok = claim(hopper, name);
                                        p.sendSystemMessage(Component.translatable(ok ? "message.virtualhopper.saved" : "message.virtualhopper.name_taken"));
                                        return ok ? 1 : 0;
                                    }))))
                        .then(Commands.literal("filter")
                            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("direction", StringArgumentType.word())
                                    .then(Commands.argument("setting", StringArgumentType.word())
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                            .executes(ctx -> {
                                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                VirtualHopperBlockEntity hopper = editable(player, BlockPosArgument.getBlockPos(ctx, "pos"));
                                                if (hopper == null) return 0;
                                                String direction = StringArgumentType.getString(ctx, "direction");
                                                if (!direction.equals("pickup") && !direction.equals("send")) return 0;
                                                boolean pickup = direction.equals("pickup");
                                                String setting = StringArgumentType.getString(ctx, "setting");
                                                String value = StringArgumentType.getString(ctx, "value");
                                                boolean ok;
                                                if (setting.equals("enabled")) {
                                                    ok = value.equals("on") || value.equals("off");
                                                    if (ok) hopper.setFilterEnabled(pickup, value.equals("on"));
                                                } else if (setting.equals("mode")) {
                                                    ok = value.equals("whitelist") || value.equals("blacklist");
                                                    if (ok) hopper.setFilterBlacklist(pickup, value.equals("blacklist"));
                                                } else if (setting.matches("slot[1-9]")) {
                                                    int slot = setting.charAt(4) - '1';
                                                    ok = hopper.setFilterItem(pickup, slot, value.equals("none") ? "" : value);
                                                } else ok = false;
                                                player.sendSystemMessage(Component.translatable(
                                                        ok ? "message.virtualhopper.saved" : "message.virtualhopper.filter.invalid"));
                                                return ok ? 1 : 0;
                                            }))))))
                        .then(Commands.literal("distribution")
                            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("mode", StringArgumentType.word())
                                    .executes(ctx -> {
                                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                                        VirtualHopperBlockEntity hopper = editable(player, BlockPosArgument.getBlockPos(ctx, "pos"));
                                        if (hopper == null) return 0;
                                        String mode = StringArgumentType.getString(ctx, "mode");
                                        int index = switch (mode) { case "round_robin" -> 0; case "priority" -> 1; case "balanced" -> 2; default -> -1; };
                                        if (index < 0) return 0;
                                        hopper.setDistributionMode(index);
                                        player.sendSystemMessage(Component.translatable("message.virtualhopper.saved"));
                                        return 1;
                                    }))))
                        .then(Commands.literal("targets")
                            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    VirtualHopperBlockEntity hopper = editable(player, BlockPosArgument.getBlockPos(ctx, "pos"));
                                    if (hopper == null) return 0;
                                    int mode = hopper.getDistributionMode();
                                    player.sendSystemMessage(Component.translatable("message.virtualhopper.targets.header",
                                            hopper.getHopperName(), Component.translatable("screen.virtualhopper.targets.mode." + mode)));
                                    for (int i = 0; i < 9; i++) {
                                        String target = hopper.getTargetName(i);
                                        if (target.isBlank()) continue;
                                        Endpoint point = findReservedEndpoint(target);
                                        Component state;
                                        if (point == null) {
                                            state = Component.translatable("message.virtualhopper.targets.unregistered");
                                        } else if (!VirtualHopperConfig.INSTANCE.allowCrossDimension
                                                && !point.dimension().equals(player.level().dimension())) {
                                            state = Component.translatable("message.virtualhopper.targets.dimension_disabled");
                                        } else {
                                            ServerLevel destLevel = ((ServerLevel) player.level()).getServer().getLevel(point.dimension());
                                            boolean loaded = destLevel != null && destLevel.hasChunkAt(point.pos());
                                            state = Component.translatable(loaded
                                                    ? "message.virtualhopper.targets.loaded"
                                                    : "message.virtualhopper.targets.unloaded");
                                        }
                                        player.sendSystemMessage(Component.translatable("message.virtualhopper.targets.row",
                                                i+1, target, state, hopper.getSuccessfulDeliveries(i)));
                                    }
                                    return 1;
                                })))
                        .then(Commands.literal("targetslot")
                            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 9))
                                    .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            VirtualHopperBlockEntity hopper = editable(player, BlockPosArgument.getBlockPos(ctx, "pos"));
                                            if (hopper == null) return 0;
                                            int index = IntegerArgumentType.getInteger(ctx, "slot") - 1;
                                            String name = StringArgumentType.getString(ctx, "name");
                                            if (name.equals("none")) name = "";
                                            Endpoint endpoint = name.isEmpty() ? null : findReservedEndpoint(name);
                                            boolean ok = hopper.setTargetSlot(index, name, endpoint);
                                            player.sendSystemMessage(Component.translatable(ok ? "message.virtualhopper.saved" : "message.virtualhopper.unknown"));
                                            return ok ? 1 : 0;
                                        })))))
                        .then(Commands.literal("targetfilter")
                            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 9))
                                    .then(Commands.argument("setting", StringArgumentType.word())
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                            .executes(ctx -> {
                                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                VirtualHopperBlockEntity hopper = editable(player, BlockPosArgument.getBlockPos(ctx, "pos"));
                                                if (hopper == null) return 0;
                                                int index = IntegerArgumentType.getInteger(ctx, "slot") - 1;
                                                String setting = StringArgumentType.getString(ctx, "setting");
                                                String value = StringArgumentType.getString(ctx, "value");
                                                boolean ok = false;
                                                if (setting.equals("enabled") && (value.equals("on") || value.equals("off")))
                                                    ok = hopper.setDestinationFilterEnabled(index, value.equals("on"));
                                                else if (setting.equals("mode") && (value.equals("whitelist") || value.equals("blacklist")))
                                                    ok = hopper.setDestinationFilterBlacklist(index, value.equals("blacklist"));
                                                else if (setting.matches("slot[1-9]"))
                                                    ok = hopper.setDestinationFilterItem(index, setting.charAt(4) - '1', value.equals("none") ? "" : value);
                                                player.sendSystemMessage(Component.translatable(ok ? "message.virtualhopper.saved" : "message.virtualhopper.filter.invalid"));
                                                return ok ? 1 : 0;
                                            }))))))
                        .then(Commands.literal("target")
                            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("name", StringArgumentType.word())
                                    .executes(ctx -> {
                                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                                        VirtualHopperBlockEntity hopper = editable(p, BlockPosArgument.getBlockPos(ctx, "pos"));
                                        if (hopper == null) return 0;
                                        String target = StringArgumentType.getString(ctx, "name");
                                        String requested = target.equals("none") ? "" : target;
                                        Endpoint endpoint = requested.isEmpty() ? null : findReservedEndpoint(requested);
                                        boolean ok = hopper.setTargetSlot(0, requested, endpoint);
                                        p.sendSystemMessage(Component.translatable(ok ? "message.virtualhopper.saved" : "message.virtualhopper.unknown"));
                                        return ok ? 1 : 0;
                                    }))))
        ));
    }
    private static VirtualHopperBlockEntity editable(ServerPlayer player, BlockPos pos) {
        if (player.distanceToSqr(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5) > 64) {
            player.sendSystemMessage(Component.translatable("message.virtualhopper.too_far")); return null;
        }
        if (player.level().getBlockEntity(pos) instanceof VirtualHopperBlockEntity hopper) return hopper;
        player.sendSystemMessage(Component.translatable("message.virtualhopper.not_found")); return null;
    }
}
