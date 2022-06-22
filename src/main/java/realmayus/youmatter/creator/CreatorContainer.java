package realmayus.youmatter.creator;


import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import realmayus.youmatter.ModFluids;
import realmayus.youmatter.ObjectHolders;
import realmayus.youmatter.YMConfig;
import realmayus.youmatter.network.PacketHandler;
import realmayus.youmatter.network.PacketUpdateCreatorClient;



public class CreatorContainer extends Container implements ICreatorStateContainer {

    public CreatorTile te;
    private PlayerEntity playerEntity;
    private IItemHandler playerInventory;


    public CreatorContainer(int windowId, World world, BlockPos pos, PlayerInventory playerInventory, PlayerEntity player) {
        super(ObjectHolders.CREATOR_CONTAINER, windowId);
        te = world.getBlockEntity(pos) instanceof CreatorTile ? (CreatorTile) world.getBlockEntity(pos) : null;
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        addPlayerSlots(this.playerInventory);
        addCustomSlots();
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        for(IContainerListener p : this.containerListeners) {
            if(p != null) {
                if (p instanceof ServerPlayerEntity) {
                    PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) p), new PacketUpdateCreatorClient(te.getEnergy(), 0, te.getUTank().getFluid(), te.getSTank().getFluid(), te.isActivated()));
                }
            }
        }
    }

    @Override
    public boolean stillValid(PlayerEntity playerIn) {
        return true;
    }

    private void addPlayerSlots(IItemHandler iItemHandler) {
        // Slots for the main inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int x = col * 18 + 8;
                int y = row * 18 + 85;
                addSlot(new SlotItemHandler(iItemHandler, col + row * 9 + 9, x, y));
            }
        }
        // Slots for the hotbar
        for (int row = 0; row < 9; ++row) {
            int x = 8 + row * 18;
            int y = 143;
            addSlot(new SlotItemHandler(iItemHandler, row, x, y));
        }
    }

    private void addCustomSlots() {
        te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(h -> {
                    addSlot(new SlotItemHandler(h, 1, 52, 20));
                    addSlot(new SlotItemHandler(h, 2, 52, 62));
                    addSlot(new SlotItemHandler(h, 3, 110, 20));
                    addSlot(new SlotItemHandler(h, 4, 110, 62));
                });
    }

    /**
     * This is actually needed in order to achieve shift click functionality in the Controller GUI. If this method isn't overridden, the game crashes.
     */
    @Override
    public ItemStack quickMoveStack(PlayerEntity playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index >= 36 && index <= 39) { //originating slot is custom slot
                if (!this.moveItemStackTo(itemstack1, 0, 36, true)) {
                    return ItemStack.EMPTY; // Inventory is full, can't transfer item!
                }
            } else {
                if(itemstack1.getItem() instanceof BucketItem) {
                    BucketItem bucket = (BucketItem) itemstack1.getItem();
                    if (bucket.getFluid().getFluid().equals(ModFluids.STABILIZER.get()) || YMConfig.CONFIG.alternativeStabilizer.get().equalsIgnoreCase(bucket.getFluid().getFluid().getRegistryName().getPath())) {
                        if(!this.moveItemStackTo(itemstack1, 36, 37, false)) {
                            return ItemStack.EMPTY; // custom slot is full, can't transfer item!
                        }
                    } else if(bucket == Items.BUCKET) {
                        if(!this.moveItemStackTo(itemstack1, 38, 39, false)) {
                            return ItemStack.EMPTY; // custom slot is full, can't transfer item!
                        }
                    }
                } else if(itemstack1.getItem().equals(Items.BUCKET)) {
                    if(!this.moveItemStackTo(itemstack1, 38, 39, false)) {
                        return ItemStack.EMPTY; // custom slot is full, can't transfer item!
                    }

                } else if(itemstack1.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).isPresent()) {
                    return itemstack1.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).map(h -> {
                       if (h.getFluidInTank(0).isEmpty() || h.getFluidInTank(0).getFluid().isSame(ModFluids.UMATTER.get())) {
                           if(!this.moveItemStackTo(itemstack1, 38, 39, false)) {
                               return ItemStack.EMPTY; // custom slot is full, can't transfer item!
                           }
                       } else if (h.getFluidInTank(0).getFluid().isSame(ModFluids.STABILIZER.get()) || YMConfig.CONFIG.alternativeStabilizer.get().equalsIgnoreCase(h.getFluidInTank(0).getFluid().getFluid().getRegistryName().getPath())) {
                           if(!this.moveItemStackTo(itemstack1, 36, 37, false)) {
                               return ItemStack.EMPTY; // custom slot is full, can't transfer item!
                           }
                       } else {
                           return ItemStack.EMPTY;
                       }
                        return ItemStack.EMPTY;
                    }).orElse(ItemStack.EMPTY);
                }
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public void sync(int energy, int progress, FluidStack uTank, FluidStack sTank, boolean isActivated) {
        te.setClientEnergy(energy);
        te.getUTank().setFluid(uTank);
        te.getSTank().setFluid(sTank);
        te.setActivatedClient(isActivated);
    }
}
