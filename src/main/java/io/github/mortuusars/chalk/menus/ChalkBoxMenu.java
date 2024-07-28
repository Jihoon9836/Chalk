package io.github.mortuusars.chalk.menus;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Pair;
import io.github.mortuusars.chalk.Chalk;
import io.github.mortuusars.chalk.config.Config;
import io.github.mortuusars.chalk.item.ChalkBoxItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class ChalkBoxMenu extends AbstractContainerMenu {
    public Pair<Integer, Integer> chalkBoxCoords = Pair.of(Integer.MIN_VALUE, Integer.MIN_VALUE);

    private final int chalkBoxSlotIndex;
    private final ChalkBoxItem chalkBoxItem;
    private final Player player;
    private final boolean glowingEnabled;

    public ChalkBoxMenu(int containerId, Inventory playerInventory, int chalkBoxSlotIndex) {
        super(Chalk.Menus.CHALK_BOX.get(), containerId);

        ItemStack chalkBoxStack = playerInventory.getItem(chalkBoxSlotIndex);
        Preconditions.checkArgument(chalkBoxStack.getItem() instanceof ChalkBoxItem, "{} is not a ChalkBoxItem.", chalkBoxStack);

        this.chalkBoxSlotIndex = chalkBoxSlotIndex;
        this.chalkBoxItem = ((ChalkBoxItem) chalkBoxStack.getItem());
        this.player = playerInventory.player;

        glowingEnabled = Config.Common.CHALK_BOX_GLOWING_ENABLED.get();

        int slotsYPos = glowingEnabled ? 18 : 33;

        IItemHandler itemHandler = new ChalkBoxItemStackHandler(chalkBoxStack) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                if (player.isCreative()) {
                    playerInventory.setItem(chalkBoxSlotIndex, this.getChalkBoxStack());
                }
            }
        };

        // Add chalk slots
        int index = 0;
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 4; column++) {

                if (index >= ChalkBoxItem.GLOWINGS_SLOT_INDEX)
                    throw new IllegalStateException("Chalk slot ids should go before Glowing Item slot id and not exceed it.");

                addSlot(new SlotItemHandler(itemHandler, index++, column * 18 + 53, row * 18 + slotsYPos));
            }
        }

        if (glowingEnabled) {
            addSlot(new SlotItemHandler(itemHandler, ChalkBoxItem.GLOWINGS_SLOT_INDEX, 80, 68) {
                @Override
                public void set(@NotNull ItemStack stack) {
                    if (player.level().isClientSide && this.getItem().isEmpty()
                            && getGlowAmount() <= 0 && stack.is(Chalk.Tags.Items.GLOWINGS)) {
                        Vec3 pos = player.position();
                        player.level().playSound(player, pos.x, pos.y, pos.z, Chalk.SoundEvents.GLOW_APPLIED.get(), SoundSource.PLAYERS, 1f, 1f);
                        player.level().playSound(player, pos.x, pos.y, pos.z, Chalk.SoundEvents.GLOWING.get(), SoundSource.PLAYERS, 1f, 1f);
                    }

                    super.set(stack);
                }
            });
        }

        addPlayerSlots(playerInventory);
    }

    protected void addPlayerSlots(Inventory playerInventory) {
        //Player Inventory
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int index = (column + row * 9) + 9;

                if (index == chalkBoxSlotIndex) {
                    chalkBoxCoords = Pair.of(column * 18 + 8, 98 + row * 18);
                    addSlot(new Slot(playerInventory, index, column * 18 + 8, 98 + row * 18) {
                        @Override
                        public boolean mayPlace(@NotNull ItemStack pStack) {
                            return false;
                        }

                        @Override
                        public boolean mayPickup(@NotNull Player pPlayer) {
                            return false;
                        }

                        @Override
                        public boolean isActive() {
                            return false;
                        }

                        @Override
                        public boolean isHighlightable() {
                            return false;
                        }
                    });
                    continue;
                }

                addSlot(new Slot(playerInventory, index, column * 18 + 8, 98 + row * 18));
            }
        }

        //Hotbar
        for (int index = 0; index < 9; index++) {
            if (index == chalkBoxSlotIndex) {
                chalkBoxCoords = Pair.of(index * 18 + 8, 156);
                addSlot(new Slot(playerInventory, index, index * 18 + 8, 156) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack pStack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(@NotNull Player pPlayer) {
                        return false;
                    }

                    @Override
                    public boolean isActive() {
                        return false;
                    }

                    @Override
                    public boolean isHighlightable() {
                        return false;
                    }
                });
                continue;
            }

            addSlot(new Slot(playerInventory, index, index * 18 + 8, 156));
        }
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        player.playSound(Chalk.SoundEvents.CHALK_BOX_CLOSE.get(), 0.85f, 0.9f + player.level().random.nextFloat() * 0.2f);

        if (player.isCreative() && !player.level().isClientSide && chalkBoxSlotIndex >= 0) {
            player.getInventory().setItem(chalkBoxSlotIndex, getChalkBoxStack());
        }

        // Fixes inventory not syncing after closing:
        player.inventoryMenu.resumeRemoteUpdates();
    }

    public ItemStack getChalkBoxStack() {
        return player.getInventory().getItem(chalkBoxSlotIndex);
    }

    public boolean isGlowingEnabled() {
        return glowingEnabled;
    }

    public int getGlowAmount() {
        return chalkBoxItem.getGlowAmount(getChalkBoxStack());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack slotItemStack = slot.getItem();
            itemstack = slotItemStack.copy();
            if (index < ChalkBoxItem.SLOTS) { // From Chalk Box to player inventory.
                if (!this.moveItemStackTo(slotItemStack, ChalkBoxItem.SLOTS, this.slots.size(), true))
                    return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(slotItemStack, 0, ChalkBoxItem.SLOTS, false)) // From player inventory to box.
                return ItemStack.EMPTY;


            if (slotItemStack.isEmpty())
                slot.set(ItemStack.EMPTY);
            else
                slot.setChanged();
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return chalkBoxSlotIndex >= 0 && getChalkBoxStack().getItem().equals(chalkBoxItem);
    }

    public static ChalkBoxMenu fromBuffer(int containerID, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        return new ChalkBoxMenu(containerID, playerInventory, buffer.readVarInt());
    }
}
