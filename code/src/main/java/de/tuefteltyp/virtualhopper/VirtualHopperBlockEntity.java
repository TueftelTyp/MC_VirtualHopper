package de.tuefteltyp.virtualhopper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public final class VirtualHopperBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private String hopperName = "";
    private String targetName = "";
    // Persist the actual destination: a name-only, in-memory index is not restart-safe.
    private String targetDimension = "";
    private long targetPosition = 0L;
    private boolean targetLocationKnown = false;
    private int age = 0;
    private static final int[] ALL = {0,1,2,3,4};

    public VirtualHopperBlockEntity(BlockPos pos, BlockState state) { super(VirtualHopper.BLOCK_ENTITY, pos, state); }
    public String getHopperName() { return hopperName; }
    public String getTargetName() { return targetName; }
    public void setHopperName(String name) { hopperName = name; setChanged(); }
    public void setTargetName(String name) {
        targetName = name;
        if (name.isBlank()) clearTargetLocation();
        setChanged();
    }
    public void setTarget(VirtualHopperBlockEntity destination) {
        if (destination.getLevel() == null || destination == this) return;
        targetName = destination.getHopperName();
        targetDimension = destination.getLevel().dimension().identifier().toString();
        targetPosition = destination.getBlockPos().asLong();
        targetLocationKnown = true;
        setChanged();
    }
    private void clearTargetLocation() {
        targetDimension = "";
        targetPosition = 0L;
        targetLocationKnown = false;
    }
    public VirtualHopperBlockEntity findTarget(ServerLevel current) {
        if (targetName.isBlank()) return null;
        // Old worlds: resolve a name once, then persist the stable address.
        if (!targetLocationKnown) {
            VirtualHopperBlockEntity previous = VirtualHopper.resolve(current.getServer(), targetName);
            if (previous != null) setTarget(previous);
            else return null;
        }
        final Identifier dimensionId;
        try { dimensionId = Identifier.parse(targetDimension); }
        catch (IllegalArgumentException ex) { return null; }
        ServerLevel targetLevel = current.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (targetLevel == null || !targetLevel.hasChunkAt(BlockPos.of(targetPosition))) return null;
        if (targetLevel.getBlockEntity(BlockPos.of(targetPosition)) instanceof VirtualHopperBlockEntity target
                && targetName.equals(target.getHopperName())) return target;
        return null;
    }
    @Override public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        return saveWithoutMetadata(registryLookup);
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public int getContainerSize() { return 5; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> stacks) { items = stacks; }
    @Override protected Component getDefaultName() { return Component.translatable("container.virtualhopper.virtual_hopper"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new HopperMenu(id, inventory, this); }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(5, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        hopperName = input.getStringOr("HopperName", "");
        targetName = input.getStringOr("TargetName", "");
        targetDimension = input.getStringOr("TargetDimension", "");
        targetPosition = input.getLongOr("TargetPosition", 0L);
        targetLocationKnown = input.getBooleanOr("TargetLocationKnown", false);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putString("HopperName", hopperName);
        output.putString("TargetName", targetName);
        output.putString("TargetDimension", targetDimension);
        output.putLong("TargetPosition", targetPosition);
        output.putBoolean("TargetLocationKnown", targetLocationKnown);
    }
    @Override public int[] getSlotsForFace(Direction side) { return ALL; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) { return true; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) { return true; }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, VirtualHopperBlockEntity hopper) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (++hopper.age % VirtualHopperConfig.INSTANCE.transferIntervalTicks != 0) return;
        VirtualHopper.remember(hopper);
        hopper.pull(serverLevel, pos);
        if (!hopper.targetName.isBlank()) {
            VirtualHopperBlockEntity destination = hopper.findTarget(serverLevel);
            if (destination != null && destination != hopper && (VirtualHopperConfig.INSTANCE.allowCrossDimension || destination.getLevel().dimension().equals(level.dimension())) && destination.getLevel() instanceof ServerLevel destLevel
                    && destLevel.hasChunkAt(destination.getBlockPos())) {
                hopper.push(destination, Direction.UP);
            }
        } else {
            Direction facing = state.getValue(VirtualHopperBlock.FACING);
            BlockPos output = pos.relative(facing);
            if (serverLevel.hasChunkAt(output)) {
                BlockEntity be = serverLevel.getBlockEntity(output);
                if (be instanceof Container container && be != hopper) hopper.push(container, facing.getOpposite());
            }
        }
    }
    private void pull(ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        if (level.hasChunkAt(above)) {
            BlockEntity be = level.getBlockEntity(above);
            if (be instanceof Container source && be != this) {
                for (int i = 0; i < source.getContainerSize(); i++) {
                    ItemStack stack = source.getItem(i);
                    if (stack.isEmpty() || source instanceof WorldlyContainer sided && !canExtract(sided,i,stack,Direction.DOWN)) continue;
                    if (!canFit(stack, this, Direction.UP)) continue;
                    ItemStack one = stack.copyWithCount(1);
                    if (insert(one, this, Direction.UP)) {
                        source.removeItem(i,1);
                        source.setChanged();
                    }
                    break;
                }
            }
        }
        if (!VirtualHopperConfig.INSTANCE.collectDroppedItems) return;
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos.getX(), pos.getY()+0.8, pos.getZ(), pos.getX()+1, pos.getY()+2, pos.getZ()+1))) {
            ItemStack stack = drop.getItem();
            if (stack.isEmpty() || !canFit(stack,this,Direction.UP)) continue;
            ItemStack one = stack.copyWithCount(1);
            if (insert(one,this,Direction.UP)) {
                stack.shrink(1);
                if (stack.isEmpty()) drop.discard(); else drop.setItem(stack);
            }
            break;
        }
    }
    private void push(Container target, Direction entrySide) {
        for (int i=0;i<getContainerSize();i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty() || !canFit(stack,target,entrySide)) continue;
            ItemStack one = stack.copyWithCount(1);
            if (insert(one,target,entrySide)) { removeItem(i,1); setChanged(); }
            break;
        }
    }
    private static boolean canExtract(WorldlyContainer container, int slot, ItemStack stack, Direction side) {
        for (int index : container.getSlotsForFace(side)) if (index == slot) return container.canTakeItemThroughFace(slot,stack,side);
        return false;
    }
    private static boolean permitted(Container container,int slot,ItemStack stack,Direction side) {
        if (!container.canPlaceItem(slot,stack)) return false;
        if (container instanceof WorldlyContainer sided) {
            for (int index : sided.getSlotsForFace(side)) if (slot == index) return sided.canPlaceItemThroughFace(slot,stack,side);
            return false;
        }
        return true;
    }
    private static boolean canFit(ItemStack incoming,Container dest,Direction side) {
        for (int i=0;i<dest.getContainerSize();i++) {
            ItemStack existing = dest.getItem(i);
            if (!permitted(dest,i,incoming,side)) continue;
            if (existing.isEmpty() || (ItemStack.isSameItemSameComponents(existing,incoming) && existing.getCount()<Math.min(existing.getMaxStackSize(),dest.getMaxStackSize()))) return true;
        }
        return false;
    }
    private static boolean insert(ItemStack incoming,Container dest,Direction side) {
        for (int i=0;i<dest.getContainerSize();i++) {
            if (!permitted(dest,i,incoming,side)) continue;
            ItemStack existing=dest.getItem(i);
            if (existing.isEmpty()) { dest.setItem(i,incoming); dest.setChanged(); return true; }
            if (ItemStack.isSameItemSameComponents(existing,incoming) && existing.getCount()<Math.min(existing.getMaxStackSize(),dest.getMaxStackSize())) {
                existing.grow(1); dest.setChanged(); return true;
            }
        }
        return false;
    }
}
