package realmayus.youmatter.creator;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import realmayus.youmatter.ModFluids;
import realmayus.youmatter.ObjectHolders;
import realmayus.youmatter.YMConfig;
import realmayus.youmatter.replicator.ReplicatorTile;
import realmayus.youmatter.util.GeneralUtils;
import realmayus.youmatter.util.MyEnergyStorage;

public class CreatorTile extends BlockEntity implements MenuProvider {

    public CreatorTile(BlockPos pos, BlockState state) {
        super(ObjectHolders.CREATOR_TILE, pos, state);
    }

    private static final int MAX_UMATTER = 11000;
    private static final int MAX_STABILIZER = 11000;

    private boolean isActivated = true;

    boolean isActivated() {
        return isActivated;
    }

    public void setActivated(boolean activated) {
        isActivated = activated;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return LazyOptional.of(() -> inventory).cast();
        }

        if(cap == CapabilityEnergy.ENERGY) {
            return LazyOptional.of(() -> myEnergyStorage).cast();

        }
        if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return LazyOptional.of(() -> fluidHandler).cast();

        }
        return super.getCapability(cap, side);
    }

    public ItemStackHandler inventory = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            CreatorTile.this.setChanged();
        }
    };

    private FluidTank uTank = new FluidTank(MAX_UMATTER) {
        @Override
        protected void onContentsChanged() {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
            setChanged();
        }
    };

    private FluidTank sTank = new FluidTank(MAX_STABILIZER) {
        @Override
        protected void onContentsChanged() {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
            setChanged();
        }
    };

    FluidTank getUTank() {
        return uTank;
    }

    FluidTank getSTank() {
        return sTank;
    }

    private IFluidHandler fluidHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 2;
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank == 0) {
                return uTank.getFluid();
            } else if (tank == 1) {
                return sTank.getFluid();
            }
            return null;
        }

        @Override
        public int getTankCapacity(int tank) {
            if (tank == 0) {
                return MAX_UMATTER;
            } else if (tank == 1) {
                return MAX_STABILIZER;
            }
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            if (tank == 0) {
                return false;
            } else if (tank == 1 && stack.getFluid().equals(ModFluids.STABILIZER.get())) {
                return true;
            }
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.getFluid().equals(ModFluids.STABILIZER.get())) {
                if (MAX_STABILIZER - getSTank().getFluidAmount() < resource.getAmount()) {
                    return sTank.fill(new FluidStack(resource.getFluid(), MAX_STABILIZER), action);
                } else {
                    return sTank.fill(resource, action);
                }
            }
            return 0;
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.getFluid().equals(ModFluids.UMATTER.get())) {
                if (uTank.getFluidAmount() < resource.getAmount()) {
                    uTank.drain(uTank.getFluid(), action);
                    return uTank.getFluid();
                } else {
                    uTank.drain(resource, action);
                    return resource;
                }
            }
            return null;
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if(uTank.getFluid().getFluid() != null) {
                return uTank.drain(uTank.getFluid(), action);
            } else {
                return null;
            }
        }
    };

    public int getEnergy() {
        return myEnergyStorage.getEnergyStored();
    }

    public void setEnergy(int energy) {
        myEnergyStorage.setEnergy(energy);
    }

    private MyEnergyStorage myEnergyStorage = new MyEnergyStorage(this, 1000000, Integer.MAX_VALUE);

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);

        if(compound.contains("uTank")) {
            CompoundTag tagUTank = compound.getCompound("uTank");
            uTank.readFromNBT(tagUTank);
        }
        if (compound.contains("sTank")) {
            CompoundTag tagSTank = compound.getCompound("sTank");
            sTank.readFromNBT(tagSTank);
        }
        if (compound.contains("energy")) {
            myEnergyStorage.setEnergy(compound.getInt("energy"));
        }
        if (compound.contains("isActivated")) {
            isActivated = compound.getBoolean("isActivated");
        }
        if(compound.contains("inventory")) {
            inventory.deserializeNBT((CompoundTag) compound.get("inventory"));
        }
    }


    @Override
    public void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        CompoundTag tagSTank = new CompoundTag();
        CompoundTag tagUTank = new CompoundTag();
        sTank.writeToNBT(tagSTank);
        uTank.writeToNBT(tagUTank);
        compound.put("uTank", tagUTank);
        compound.put("sTank", tagSTank);
        compound.putInt("energy", getEnergy());
        compound.putBoolean("isActivated", isActivated);
        if(compound.contains("inventory")) {
            inventory.deserializeNBT((CompoundTag) compound.get("inventory"));
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private int currentPartTick = 0;
    public static void serverTick(Level level, BlockPos pos, BlockState state, CreatorTile be) {
        be.tick(level, pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (currentPartTick == 40) { // 2 sec
            if(isActivated()) {
                if (getEnergy() >= 0.3f * 1000000 && sTank.getFluidAmount() >= 125) { // if energy more than 30 % of max energy
                    if (uTank.getFluidAmount() + YMConfig.CONFIG.productionPerTick.get() <= MAX_UMATTER) {
                        sTank.drain(125, IFluidHandler.FluidAction.EXECUTE);
                        uTank.fill(new FluidStack(ModFluids.UMATTER.get(), YMConfig.CONFIG.productionPerTick.get()), IFluidHandler.FluidAction.EXECUTE);
                        myEnergyStorage.extractEnergy(Math.round(getEnergy()/3f), false);
                    }
                }
            }
            //Auto-outputting U-Matter
            Object[] neighborTE = getNeighborTileEntity(pos);
            if(neighborTE != null){
                if (uTank.getFluidAmount() >= 500) { // set a maximum output of 500 mB (every two seconds)
                    uTank.drain(level.getBlockEntity((BlockPos)neighborTE[0]).getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, (Direction)neighborTE[1]).map(h -> h.fill(new FluidStack(ModFluids.UMATTER.get(), 500), IFluidHandler.FluidAction.EXECUTE)).orElse(0), IFluidHandler.FluidAction.EXECUTE);
                } else {
                    uTank.drain(level.getBlockEntity((BlockPos)neighborTE[0]).getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, (Direction)neighborTE[1]).map(h -> h.fill(new FluidStack(ModFluids.UMATTER.get(), uTank.getFluidAmount()), IFluidHandler.FluidAction.EXECUTE)).orElse(0), IFluidHandler.FluidAction.EXECUTE);
                }
            }
            currentPartTick = 0;
        } else if ((currentPartTick % 5) == 0) { // every five ticks
            if (!(this.inventory.getStackInSlot(3).isEmpty()) && GeneralUtils.canAddItemToSlot(this.inventory.getStackInSlot(4), this.inventory.getStackInSlot(3), false)) {
                ItemStack item = this.inventory.getStackInSlot(3);
                if (item.getItem() instanceof BucketItem) {
                    if (getUTank().getFluidAmount() >= 1000) {
                        getUTank().drain(1000, IFluidHandler.FluidAction.EXECUTE);
                        this.inventory.setStackInSlot(3, ItemStack.EMPTY);
                        this.inventory.insertItem(4, new ItemStack(ObjectHolders.UMATTER_BUCKET, 1), false);
                    }
                } else {
                    item.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).ifPresent(h -> {
                        if (h.getFluidInTank(0).getFluid().isSame(ModFluids.UMATTER.get()) || h.getFluidInTank(0).isEmpty()) {
                            if (h.getTankCapacity(0) - h.getFluidInTank(0).getAmount() < getUTank().getFluidAmount()) { //fluid in S-Tank is more than what fits in the item's tank
                                getUTank().drain(h.fill(new FluidStack(ModFluids.UMATTER.get(), h.getTankCapacity(0) - h.getFluidInTank(0).getAmount()), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                            } else { //S-Tank's fluid fits perfectly in item's tank
                                getUTank().drain(h.fill(getUTank().getFluid(), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                            }
                        }
                    });
                    this.inventory.setStackInSlot(3, ItemStack.EMPTY);
                    this.inventory.insertItem(4, item, false);
                }
            }
            if (!this.inventory.getStackInSlot(1).isEmpty()) {
                ItemStack item = this.inventory.getStackInSlot(1);
                if (item.getItem() instanceof BucketItem && GeneralUtils.canAddItemToSlot(this.inventory.getStackInSlot(2), new ItemStack(Items.BUCKET, 1), false)) {
                    item.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).ifPresent(h -> {
                        if (!h.getFluidInTank(0).isEmpty() && (h.getFluidInTank(0).getFluid().isSame(ModFluids.STABILIZER.get()) || YMConfig.CONFIG.alternativeStabilizer.get().equalsIgnoreCase(h.getFluidInTank(0).getFluid().getRegistryName().getPath()))) {
                            if (MAX_STABILIZER - getSTank().getFluidAmount() >= 1000) {
                                getSTank().fill(new FluidStack(ModFluids.STABILIZER.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
                                this.inventory.setStackInSlot(1, ItemStack.EMPTY);
                                this.inventory.insertItem(2, new ItemStack(Items.BUCKET, 1), false);
                            }
                        }
                    });
                } else if(GeneralUtils.canAddItemToSlot(this.inventory.getStackInSlot(2), this.inventory.getStackInSlot(1), false)) {
                    item.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).ifPresent(h -> {
                        if (h.getFluidInTank(0).getFluid().isSame(ModFluids.STABILIZER.get()) || YMConfig.CONFIG.alternativeStabilizer.get().equalsIgnoreCase(h.getFluidInTank(0).getFluid().getRegistryName().getPath())) {
                            if (h.getFluidInTank(0).getAmount() > MAX_STABILIZER - getSTank().getFluidAmount()) { //given fluid is more than what fits in the S-Tank
                                getSTank().fill(h.drain(MAX_STABILIZER - getSTank().getFluidAmount(), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                            } else { //given fluid fits perfectly in S-Tank
                                getSTank().fill(h.drain(h.getFluidInTank(0).getAmount(), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                            }
                        }
                    });
                    this.inventory.setStackInSlot(1, ItemStack.EMPTY);
                    this.inventory.insertItem(2, item, false);
                }
            }
            currentPartTick++;
        } else {
            currentPartTick++;
        }
    }

    private Object[] getNeighborTileEntity(BlockPos creatorPos) {
        HashMap<BlockPos, Direction> foundPos = new HashMap<>();
        for(Direction facing : Direction.values()) {
            BlockPos offsetPos = creatorPos.relative(facing);
            BlockEntity offsetBe = level.getBlockEntity(offsetPos);

            if(offsetBe != null) {
                offsetBe.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing).ifPresent(x -> foundPos.put(offsetPos, facing));
            }
        }

        // Prioritize Replicator
        for (Map.Entry<BlockPos, Direction> entry : foundPos.entrySet()) {
            if (level.getBlockEntity(entry.getKey()) instanceof ReplicatorTile replicator) {
                if(replicator.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, entry.getValue()).map(h -> h.fill(new FluidStack(ModFluids.UMATTER.get(), 500), IFluidHandler.FluidAction.SIMULATE)).orElse(0) > 0) {
                    //Replicator can take fluid
                    return new Object[] {entry.getKey(), entry.getValue()}; // position, facing
                }
            }
        }

        // Replicator not found / can't take fluid, now trying other blocks
        for (Map.Entry<BlockPos, Direction> entry : foundPos.entrySet()) {
            if(Objects.requireNonNull(level.getBlockEntity(entry.getKey())).getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, entry.getValue()).map(h -> h.fill(new FluidStack(ModFluids.UMATTER.get(), 500), IFluidHandler.FluidAction.SIMULATE)).orElse(0) > 0) {
                //Tile can take fluid
                return new Object[] {entry.getKey(), entry.getValue()}; // position, facing
            }
        }

        // found nothing
        return null;
    }

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent(ObjectHolders.CREATOR_BLOCK.getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowID, Inventory playerInventory, Player playerEntity) {
        return new CreatorContainer(windowID, level, worldPosition, playerInventory, playerEntity);

    }
}
