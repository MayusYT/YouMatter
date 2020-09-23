package realmayus.youmatter.replicator;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import realmayus.youmatter.ObjectHolders;
import realmayus.youmatter.YouMatter;
import realmayus.youmatter.creator.CreatorContainer;
import realmayus.youmatter.network.PacketChangeSettingsReplicatorServer;
import realmayus.youmatter.network.PacketHandler;
import realmayus.youmatter.network.PacketShowNext;
import realmayus.youmatter.network.PacketShowPrevious;
import realmayus.youmatter.util.DisplaySlot;
import realmayus.youmatter.util.GeneralUtils;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReplicatorScreen extends ContainerScreen<ReplicatorContainer> {

    private static final int WIDTH = 176;
    private static final int HEIGHT = 168;

    private ReplicatorTile te;

    private static final ResourceLocation GUI = new ResourceLocation(YouMatter.MODID, "textures/gui/replicator.png");

    public ReplicatorScreen(ReplicatorContainer container, PlayerInventory inv, ITextComponent name) {
        super(container, inv, name);
        this.te = container.te;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        //Setting color to white because JEI is bae (gui would be yellow)
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(GUI);

        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        this.blit(matrixStack, relX, relY, 0, 0, WIDTH, HEIGHT);

        drawFluidTank(matrixStack, 26, 20, te.getTank());

    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(matrixStack, mouseX, mouseY);

        this.minecraft.getTextureManager().bindTexture(GUI);
        drawEnergyBolt(matrixStack, te.getClientEnergy());
        drawActiveIcon(matrixStack, te.isActiveClient());
        drawModeIcon(matrixStack, te.isCurrentClientMode());
        drawProgressArrow(matrixStack, te.getClientProgress());

        font.drawString(matrixStack, I18n.format(ObjectHolders.REPLICATOR_BLOCK.getTranslationKey()), 8, 6, 0x404040);
    }

    private void drawEnergyBolt(MatrixStack matrixStack, int energy) {
        this.minecraft.getTextureManager().bindTexture(GUI);

        if(te.getClientEnergy() == 0) {
            this.blit(matrixStack, 127, 58, 176, 114, 15, 20);
        } else {
            double percentage = energy * 100.0F / 1000000;  // i know this is dumb
            float percentagef = (float)percentage / 100; // but it works.
            this.blit(matrixStack, 127, 58, 176, 93, 15, Math.round(20 * percentagef)); // it's not really intended that the bolt fills from the top but it looks cool tbh.

        }
    }


    private void drawProgressArrow(MatrixStack matrixStack, int progress) {
        this.minecraft.getTextureManager().bindTexture(GUI);
        this.blit(matrixStack, 91, 38, 176, 134, 11, Math.round((progress / 100.0f) * 19));
    }

    private void drawActiveIcon(MatrixStack matrixStack, boolean isActive) {
        this.minecraft.getTextureManager().bindTexture(GUI);

        if(isActive) {
            this.blit(matrixStack, 154, 12, 176, 24, 8, 9);
        } else {
            this.blit(matrixStack, 154, 12, 184, 24, 8, 9);
        }
    }

    private void drawModeIcon(MatrixStack matrixStack, boolean mode) {
        this.minecraft.getTextureManager().bindTexture(GUI);

        if (mode){
            //loop
            this.blit(matrixStack, 152, 34, 176, 11, 13,13);
        } else {
            this.blit(matrixStack, 151, 35, 176, 0, 13, 11);
        }
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        //Render the dark background

        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);



        //Render any tooltips
        //Mappings are not complete, I took a guess that this is actually renderHoveredTooltip
        func_230459_a_(matrixStack, mouseX, mouseY);

        int xAxis = (mouseX - (width - xSize) / 2);
        int yAxis = (mouseY - (height - ySize) / 2);

        if(xAxis >= 26 && xAxis <= 39 && yAxis >= 20 && yAxis <= 75) {
            drawTooltip(matrixStack, mouseX, mouseY, Stream.of(new StringTextComponent(I18n.format("youmatter.gui.umatter.title")), new StringTextComponent(I18n.format("youmatter.gui.umatter.description", te.getTank().getFluid().getAmount()))).collect(Collectors.toList()));
        }

        if(xAxis >= 127 && xAxis <= 142 && yAxis >= 59 && yAxis <= 79) {
            drawTooltip(matrixStack, mouseX, mouseY, Stream.of(new StringTextComponent(I18n.format("youmatter.gui.energy.title")), new StringTextComponent(I18n.format("youmatter.gui.energy.description", te.getClientEnergy()))).collect(Collectors.toList()));
        }

        if(xAxis >= 148 && xAxis <= 167 && yAxis >= 7 && yAxis <= 27) {
            drawTooltip(matrixStack, mouseX, mouseY, Stream.of(new StringTextComponent(te.isActiveClient() ? I18n.format("youmatter.gui.active") : I18n.format("youmatter.gui.paused")), new StringTextComponent(I18n.format("youmatter.gui.clicktochange"))).collect(Collectors.toList()));
        }

        if(xAxis >= 148 && xAxis <= 167 && yAxis >= 31 && yAxis <= 51) {
            drawTooltip(matrixStack, mouseX, mouseY, Stream.of(new StringTextComponent(te.isCurrentClientMode() ? I18n.format("youmatter.gui.performInfiniteRuns") : I18n.format("youmatter.gui.performSingleRun")), new StringTextComponent(I18n.format("youmatter.gui.clicktochange"))).collect(Collectors.toList()));
        }
    }

    private void drawTooltip(MatrixStack matrixStack, int x, int y, List<ITextComponent> tooltips) {
        renderTooltip(matrixStack, tooltips, x, y);
    }

    @Override
    public List<ITextComponent> getTooltipFromItem(ItemStack givenItem) {
        if (hoveredSlot instanceof DisplaySlot) {
            if(givenItem.isItemEqual(hoveredSlot.getStack())) {
                List<ITextComponent> existingTooltips =  super.getTooltipFromItem(givenItem);
                existingTooltips.add(new StringTextComponent(""));
                existingTooltips.add(new StringTextComponent(I18n.format("gui.youmatter.requiredAmount", GeneralUtils.getUMatterAmountForItem(givenItem.getItem()))));
                return existingTooltips;
            }
        }
        return super.getTooltipFromItem(givenItem);
    }

    //both drawFluid and drawFluidTank is courtesy of DarkGuardsMan and was modified to suit my needs. Go check him out: https://github.com/BuiltBrokenModding/Atomic-Science | MIT License |  Copyright (c) 2018 Built Broken Modding License: https://opensource.org/licenses/MIT
    private void drawFluid(MatrixStack matrixStack, int x, int y, int line, int col, int width, int drawSize, FluidStack fluidStack)
    {
        if (fluidStack != null && fluidStack.getFluid() != null && !fluidStack.isEmpty())
        {
            drawSize -= 1;
            ResourceLocation fluidIcon;
            Fluid fluid = fluidStack.getFluid();

            ResourceLocation waterSprite = Fluids.WATER.getAttributes().getStillTexture(new FluidStack(Fluids.WATER, 1000));

            if (fluid instanceof FlowingFluid) {
                if (fluid.getAttributes().getStillTexture(fluidStack) != null) {
                    fluidIcon = fluid.getAttributes().getStillTexture(fluidStack);
                } else if (fluid.getAttributes().getFlowingTexture(fluidStack) != null) {
                    fluidIcon = fluid.getAttributes().getFlowingTexture(fluidStack);
                } else {
                    fluidIcon = waterSprite;
                }
            } else {
                fluidIcon = waterSprite;
            }

            //Bind fluid texture
            this.getMinecraft().getTextureManager().bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);

            final int textureSize = 16;
            int start = 0;
            int renderY;
            while (drawSize != 0) {
                if (drawSize > textureSize) {
                    renderY = textureSize;
                    drawSize -= textureSize;
                } else {
                    renderY = drawSize;
                    drawSize = 0;
                }

                blit(matrixStack, x + col, y + line + 58 - renderY - start, 1000, width, textureSize - (textureSize - renderY), this.minecraft.getAtlasSpriteGetter(AtlasTexture.LOCATION_BLOCKS_TEXTURE).apply(fluidIcon));
                start = start + textureSize;
            }
        }
    }

    private void drawFluidTank(MatrixStack matrixStack, int x, int y, IFluidTank tank) {

        //Get data
        final float scale = tank.getFluidAmount() / (float) tank.getCapacity();
        final FluidStack fluidStack = tank.getFluid();

        //Reset color
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);


        //Draw fluid
        int meterHeight = 55;
        if (fluidStack != null)
        {
            this.drawFluid(matrixStack, this.guiLeft + x -1, this.guiTop + y, -3, 1, 14, (int) ((meterHeight - 1) * scale), fluidStack);
        }

        //Draw lines
        this.minecraft.getTextureManager().bindTexture(GUI);
        int meterWidth = 14;
        this.blit(matrixStack, this.guiLeft + x, this.guiTop + y, 176, 35, meterWidth, meterHeight);

        //Reset color
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if(mouseButton == 0) {
            double xAxis = (mouseX - (width - xSize) / 2);
            double yAxis = (mouseY - (height - ySize) / 2);
            if(xAxis >= 80 && xAxis <= 85 && yAxis >= 21 && yAxis <= 31) {
                //Playing Click sound
                Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                //Sending packet to server
                PacketHandler.INSTANCE.sendToServer(new PacketShowPrevious());
            } else if(xAxis >= 108 && xAxis <= 113 && yAxis >= 21 && yAxis <= 31) {
                //Playing Click sound
                Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                //Sending packet to server
                PacketHandler.INSTANCE.sendToServer(new PacketShowNext() );
            } else if(xAxis >= 148 && xAxis <= 167 && yAxis >= 7 && yAxis <= 27) {
                //Playing Click sound
                Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                //Sending packet to server
                PacketHandler.INSTANCE.sendToServer(new PacketChangeSettingsReplicatorServer(!te.isActiveClient(), te.isCurrentClientMode()) );
            } else if(xAxis >= 148 && xAxis <= 167 && yAxis >= 31 && yAxis <= 51) {
                //Playing Click sound
                Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                //Sending packet to server
                PacketHandler.INSTANCE.sendToServer(new PacketChangeSettingsReplicatorServer(te.isActiveClient(), !te.isCurrentClientMode()) );
            }
        }
        return true;
    }
}