package dev.skydock.block;

import dev.architectury.registry.fuel.FuelRegistry;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import dev.skydock.menu.EngineMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class EngineBlockEntity extends BlockEntity implements Container, MenuProvider {
    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    public int burnTicks;
    public EngineBlockEntity(BlockPos pos, BlockState state) { super(SkydockBlocks.ENGINE_ENTITY.get(), pos, state); }
    public boolean burn() {
        if (burnTicks > 0) { burnTicks--; setChanged(); return true; }
        for (int i = 0; i < items.size(); i++) {
            ItemStack fuel = items.get(i); int time = FuelRegistry.get(fuel);
            if (time > 0) {
                ItemStack remainder = fuel.getItem().hasCraftingRemainingItem() ? new ItemStack(fuel.getItem().getCraftingRemainingItem()) : ItemStack.EMPTY;
                fuel.shrink(1); if (fuel.isEmpty()) items.set(i, remainder);
                burnTicks = Math.max(0, (int) Math.round(time * tier().fuelEfficiency) - 1); setChanged(); return true;
            }
        }
        return false;
    }
    public EngineTier tier() { return EngineTier.from(getBlockState()); }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int i) { return items.get(i); }
    @Override public ItemStack removeItem(int i, int n) { ItemStack stack = ContainerHelper.removeItem(items, i, n); setChanged(); return stack; }
    @Override public ItemStack removeItemNoUpdate(int i) { return ContainerHelper.takeItem(items, i); }
    @Override public void setItem(int i, ItemStack stack) { items.set(i, stack); stack.limitSize(getMaxStackSize(stack)); setChanged(); }
    @Override public boolean stillValid(Player p) { return Container.stillValidBlockEntity(this, p); }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public Component getDisplayName() { return Component.translatable(switch (tier()) {
        case BRASS -> "block.skydock.engine";
        case COMPOUND -> "block.skydock.engine_reinforced";
        case TURBINE -> "block.skydock.engine_turbine";
        case AETHER -> "block.skydock.engine_aether";
    }); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new EngineMenu(id, inventory, this); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); ContainerHelper.saveAllItems(tag, items, registries); tag.putInt("BurnTicks", burnTicks);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); items = NonNullList.withSize(5, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries); burnTicks = Math.max(0, tag.getInt("BurnTicks"));
    }
}
