package realmayus.youmatter.scanner;


import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import realmayus.youmatter.ObjectHolders;
import realmayus.youmatter.YMConfig;
import realmayus.youmatter.util.MyEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

public class ScannerTile extends TileEntity implements ITickableTileEntity, INamedContainerProvider {



    public ScannerTile() {
        super(ObjectHolders.SCANNER_TILE_ENTITY_TYPE);
    }

    public boolean hasEncoder = false;

    public boolean getHasEncoder() {
        return hasEncoder;
    }

    public void setHasEncoder(boolean hasEncoder) {
        this.hasEncoder = hasEncoder;
    }

    public boolean getHasEncoderClient() {
        return hasEncoderClient;
    }

    public void setHasEncoderClient(boolean hasEncoderClient) {
        this.hasEncoderClient = hasEncoderClient;
    }

    public boolean hasEncoderClient = false;


    @Override
    public ITextComponent getDisplayName() {
        return new StringTextComponent(getType().getRegistryName().getPath());
    }

    @Nullable
    @Override
    public Container createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        return new ScannerContainer(i, world, pos, playerInventory, playerEntity);
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
        return super.getCapability(cap, side);
    }

    /**
     * Handler for the Input Slots
     */
    public ItemStackHandler inventory = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            ScannerTile.this.markDirty();
        }
    };

    private int clientEnergy = -1;
    private int clientProgress = -1;

    private int progress = 0;


    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getClientProgress() {
        return clientProgress;
    }

    public void setClientProgress(int clientProgress) {
        this.clientProgress = clientProgress;
    }

    public int getClientEnergy() {
        return clientEnergy;
    }

    public void setClientEnergy(int clientEnergy) {
        this.clientEnergy = clientEnergy;
    }

    public int getEnergy() {
        return myEnergyStorage.getEnergyStored();
    }

    private MyEnergyStorage myEnergyStorage = new MyEnergyStorage(1000000, Integer.MAX_VALUE);
    
    @Override
    public void read(CompoundNBT compound) {
        super.read(compound);
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        super.write(compound);

        return compound;
    }

    private int currentPartTick = 0;
    @Override
    public void tick() {
        if(currentPartTick >= 2) {
            if (getNeighborEncoder(this.pos) != null) {
                hasEncoder = true;
                BlockPos encoderPos = getNeighborEncoder(this.pos);
                if(!inventory.getStackInSlot(1).isEmpty() && isItemAllowed(inventory.getStackInSlot(1))) {
                    if(getEnergy() > 2048) {
                        if (getProgress() < 100) {
                            setProgress(getProgress() + 1);
                            myEnergyStorage.consumePower(2048);
                        } else if (encoderPos != null) {
                            // Notifying the neighboring encoder of this scanner having finished its operation
//                            ((TileEncoder)world.getTileEntity(encoderPos)).ignite(this.inventory.getStackInSlot(1)); //don't worry, this is already checked by getNeighborEncoder() c:
                            System.out.println("IGNITED"); //TODO remove
                            inventory.setStackInSlot(1, ItemStack.EMPTY);
                            setProgress(0);
                        }
                    }
                } else {
                    setProgress(0); // if item was suddenly removed, reset progress to 0
                }
            } else {
                hasEncoder = false;
            }
            currentPartTick = 0;
        } else {
            currentPartTick++;
        }
    }

    private boolean isItemAllowed(ItemStack itemStack) {
            //If list should act as a blacklist AND it contains the item, disallow scanning
        if (YMConfig.useAsBlacklist && Arrays.stream(YMConfig.itemList).anyMatch(s -> s.equalsIgnoreCase(Objects.requireNonNull(itemStack.getItem().getRegistryName()).toString()))) {
            return false;

            //If list should act as a whitelist AND it DOESN'T contain the item, disallow scanning
        } else if (!(YMConfig.useAsBlacklist) && Arrays.stream(YMConfig.itemList).noneMatch(s -> s.equalsIgnoreCase(Objects.requireNonNull(itemStack.getItem().getRegistryName()).toString()))) {
            return false;

            //Else:
        } else {
            return true;
        }
    }
    private BlockPos getNeighborEncoder(BlockPos scannerPos) {
//        for(Direction direction : Direction.values()) {
//            if(world.getBlockState(scannerPos.offset(direction)).getBlock() instanceof BlockEncoder) {
//                if(world.getTileEntity(scannerPos.offset(direction)) != null) {
//                    if(world.getTileEntity(scannerPos.offset(direction)) instanceof TileEncoder) {
//                        return scannerPos.offset(direction);
//                    }
//                }
//            }
//        }

        return null;
    }

}
