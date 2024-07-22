package realmayus.youmatter.scanner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import realmayus.youmatter.ModContent;
import realmayus.youmatter.YMConfig;
import realmayus.youmatter.encoder.EncoderBlock;
import realmayus.youmatter.encoder.EncoderBlockEntity;
import realmayus.youmatter.util.MyEnergyStorage;
import realmayus.youmatter.util.RegistryUtil;

import javax.annotation.Nullable;
import java.util.Objects;

public class ScannerBlockEntity extends BlockEntity implements MenuProvider {

    public boolean hasEncoder = false;

    public ScannerBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.SCANNER_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean getHasEncoder() {
        return hasEncoder;
    }

    public void setHasEncoder(boolean hasEncoder) {
        this.hasEncoder = hasEncoder;
        setChanged();
    }

    public ItemStackHandler inventory = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            ScannerBlockEntity.this.setChanged();
        }
    };

    private int progress = 0;

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        setChanged();
    }

    public int getEnergy() {
        return myEnergyStorage.getEnergyStored();
    }

    public void setEnergy(int energy) {
        myEnergyStorage.setEnergy(energy);
    }

    private final MyEnergyStorage myEnergyStorage = new MyEnergyStorage(this, 1000000, Integer.MAX_VALUE);

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        if (compound.contains("progress")) {
            setProgress(compound.getInt("progress"));
        }
        if (compound.contains("energy")) {
            setEnergy(compound.getInt("energy"));
        }
        if(compound.contains("inventory")) {
            inventory.deserializeNBT((CompoundTag) compound.get("inventory"));
        }

        setHasEncoder(compound.getBoolean("encoder"));
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        compound.putInt("progress", getProgress());
        compound.putInt("energy", getEnergy());
        compound.putBoolean("encoder", getHasEncoder());
        if (inventory != null) {
            compound.put("inventory", inventory.serializeNBT());
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

    public static void tick(Level level, BlockPos pos, BlockState state, ScannerBlockEntity be) {
        be.tick(level, pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (currentPartTick < 2) {
            currentPartTick++;
            return;
        }

        BlockPos encoderPos = getNeighborEncoder(this.worldPosition);
        if (encoderPos == null) {
            return;
        }

        if (!hasEncoder) {
            setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }

        hasEncoder = true;
        if (inventory == null) {
            if (hasEncoder) {
                setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
            }
            hasEncoder = false;
            return;
        }

        ItemStack stackInSlot = inventory.getStackInSlot(1);
        if (stackInSlot.isEmpty() || !isItemAllowed(stackInSlot)) {
            if (getProgress() != 0) {
                setProgress(0); // if item was suddenly removed, reset progress to 0
            }
            return;
        }

        for (Direction direction : Direction.values()) {
            if (myEnergyStorage.getEnergyStored() <= 0) {
                return;
            }
            IEnergyStorage energyStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, getBlockPos().relative(direction), null);
            if (getEnergy() > YMConfig.CONFIG.energyScanner.get()) {
                if (getProgress() < 100) {
                    setProgress(getProgress() + 1);
                    if (energyStorage != null) {
                        myEnergyStorage.extractEnergy(YMConfig.CONFIG.energyScanner.get(), false);
                    }
                } else {
                    // Notifying the neighboring encoder of this scanner having finished its operation
                    ((EncoderBlockEntity) level.getBlockEntity(encoderPos)).ignite(stackInSlot); //don't worry, this is already checked by getNeighborEncoder() c:
                    inventory.setStackInSlot(1, ItemStack.EMPTY);
                    setProgress(0);
                }
            }
        }
        currentPartTick = 0;
    }


    private boolean isItemAllowed(ItemStack itemStack) {

        boolean matches = YMConfig.CONFIG.filterItems.get().stream().anyMatch(s -> s.equalsIgnoreCase(Objects.requireNonNull(RegistryUtil.getRegistryName(itemStack.getItem())).toString()));
        //If list should act as a blacklist AND it contains the item, disallow scanning
        if (YMConfig.CONFIG.filterMode.get() && matches) {
            return false;
            //If list should act as a whitelist AND it DOESN'T contain the item, disallow scanning
        } else if (YMConfig.CONFIG.filterMode.get() || matches) return true;
        else return false;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        level.invalidateCapabilities(worldPosition);
    }

    @Nullable
    private BlockPos getNeighborEncoder(BlockPos scannerPos) {
        for(Direction facing : Direction.values()) {
            BlockPos offsetPos = scannerPos.relative(facing);

            if(level.getBlockState(offsetPos).getBlock() instanceof EncoderBlock) {
                if(level.getBlockEntity(offsetPos) instanceof EncoderBlockEntity) {
                    return offsetPos;
                }
            }
        }
        return null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(ModContent.SCANNER_BLOCK.get().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowID, Inventory playerInventory, Player player) {
        return new ScannerMenu(windowID, level, worldPosition, playerInventory, player);
    }

    public ItemStackHandler getItemHandler() {
        return inventory;
    }

    public IEnergyStorage getEnergyHandler() {
        return myEnergyStorage;
    }
}
