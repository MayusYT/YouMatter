package realmayus.youmatter.replicator;

import java.util.Arrays;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import realmayus.youmatter.ObjectHolders;
import realmayus.youmatter.YouMatter;
import realmayus.youmatter.network.PacketChangeSettingsReplicatorServer;
import realmayus.youmatter.network.PacketHandler;
import realmayus.youmatter.network.PacketShowNext;
import realmayus.youmatter.network.PacketShowPrevious;
import realmayus.youmatter.util.DisplaySlot;
import realmayus.youmatter.util.GeneralUtils;


public class ReplicatorScreen extends AbstractContainerScreen<ReplicatorContainer> {

    private static final int WIDTH = 176;
    private static final int HEIGHT = 168;

    private ReplicatorTile te;

    private static final ResourceLocation GUI = new ResourceLocation(YouMatter.MODID, "textures/gui/replicator.png");

    public ReplicatorScreen(ReplicatorContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        this.te = container.te;
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        //Setting color to white because JEI is bae (gui would be yellow)
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem._setShaderTexture(0, GUI);

        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        this.blit(matrixStack, relX, relY, 0, 0, WIDTH, HEIGHT);

        drawFluidTank(matrixStack, 26, 20, te.getTank());

    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        RenderSystem._setShaderTexture(0, GUI);
        drawEnergyBolt(matrixStack, te.getClientEnergy());
        drawActiveIcon(matrixStack, te.isActiveClient());
        drawModeIcon(matrixStack, te.isCurrentClientMode());
        drawProgressArrow(matrixStack, te.getClientProgress());

        font.draw(matrixStack, I18n.get(ObjectHolders.REPLICATOR_BLOCK.getDescriptionId()), 8, 6, 0x404040);
    }

    private void drawEnergyBolt(PoseStack matrixStack, int energy) {
        RenderSystem._setShaderTexture(0, GUI);

        if(te.getClientEnergy() == 0) {
            this.blit(matrixStack, 127, 58, 176, 114, 15, 20);
        } else {
            double percentage = energy * 100.0F / 1000000;  // i know this is dumb
            float percentagef = (float)percentage / 100; // but it works.
            this.blit(matrixStack, 127, 58, 176, 93, 15, Math.round(20 * percentagef)); // it's not really intended that the bolt fills from the top but it looks cool tbh.

        }
    }


    private void drawProgressArrow(PoseStack matrixStack, int progress) {
        RenderSystem._setShaderTexture(0, GUI);
        this.blit(matrixStack, 91, 38, 176, 134, 11, Math.round((progress / 100.0f) * 19));
    }

    private void drawActiveIcon(PoseStack matrixStack, boolean isActive) {
        RenderSystem._setShaderTexture(0, GUI);

        if(isActive) {
            this.blit(matrixStack, 154, 12, 176, 24, 8, 9);
        } else {
            this.blit(matrixStack, 154, 12, 184, 24, 8, 9);
        }
    }

    private void drawModeIcon(PoseStack matrixStack, boolean mode) {
        RenderSystem._setShaderTexture(0, GUI);

        if (mode){
            //loop
            this.blit(matrixStack, 152, 34, 176, 11, 13,13);
        } else {
            this.blit(matrixStack, 151, 35, 176, 0, 13, 11);
        }
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        //Render the dark background

        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);



        //Render any tooltips
        renderTooltip(matrixStack, mouseX, mouseY);

        int xAxis = (mouseX - (width - imageWidth) / 2);
        int yAxis = (mouseY - (height - imageHeight) / 2);

        if(xAxis >= 26 && xAxis <= 39 && yAxis >= 20 && yAxis <= 75) {
            drawTooltip(matrixStack, mouseX, mouseY, Arrays.asList(new TextComponent(I18n.get("youmatter.gui.umatter.title")), new TextComponent(I18n.get("youmatter.gui.umatter.description", te.getTank().getFluid().getAmount()))));
        }

        if(xAxis >= 127 && xAxis <= 142 && yAxis >= 59 && yAxis <= 79) {
            drawTooltip(matrixStack, mouseX, mouseY, Arrays.asList(new TextComponent(I18n.get("youmatter.gui.energy.title")), new TextComponent(I18n.get("youmatter.gui.energy.description", te.getClientEnergy()))));
        }

        if(xAxis >= 148 && xAxis <= 167 && yAxis >= 7 && yAxis <= 27) {
            drawTooltip(matrixStack, mouseX, mouseY, Arrays.asList(new TextComponent(te.isActiveClient() ? I18n.get("youmatter.gui.active") : I18n.get("youmatter.gui.paused")), new TextComponent(I18n.get("youmatter.gui.clicktochange"))));
        }

        if(xAxis >= 148 && xAxis <= 167 && yAxis >= 31 && yAxis <= 51) {
            drawTooltip(matrixStack, mouseX, mouseY, Arrays.asList(new TextComponent(te.isCurrentClientMode() ? I18n.get("youmatter.gui.performInfiniteRuns") : I18n.get("youmatter.gui.performSingleRun")), new TextComponent(I18n.get("youmatter.gui.clicktochange"))));
        }
    }

    private void drawTooltip(PoseStack matrixStack, int x, int y, List<Component> tooltips) {
        renderComponentTooltip(matrixStack, tooltips, x, y);
    }

    @Override
    public List<Component> getTooltipFromItem(ItemStack givenItem) {
        if (hoveredSlot instanceof DisplaySlot) {
            if(givenItem.sameItem(hoveredSlot.getItem())) {
                List<Component> existingTooltips =  super.getTooltipFromItem(givenItem);
                existingTooltips.add(new TextComponent(""));
                existingTooltips.add(new TextComponent(I18n.get("gui.youmatter.requiredAmount", GeneralUtils.getUMatterAmountForItem(givenItem.getItem()))));
                return existingTooltips;
            }
        }
        return super.getTooltipFromItem(givenItem);
    }

    //both drawFluid and drawFluidTank is courtesy of DarkGuardsMan and was modified to suit my needs. Go check him out: https://github.com/BuiltBrokenModding/Atomic-Science | MIT License |  Copyright (c) 2018 Built Broken Modding License: https://opensource.org/licenses/MIT
    private void drawFluid(PoseStack matrixStack, int x, int y, int line, int col, int width, int drawSize, FluidStack fluidStack)
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
            RenderSystem._setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);

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

                blit(matrixStack, x + col, y + line + 58 - renderY - start, 1000, width, textureSize - (textureSize - renderY), this.minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(fluidIcon));
                start = start + textureSize;
            }
        }
    }

    private void drawFluidTank(PoseStack matrixStack, int x, int y, IFluidTank tank) {

        //Get data
        final float scale = tank.getFluidAmount() / (float) tank.getCapacity();
        final FluidStack fluidStack = tank.getFluid();

        //Reset color
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);


        //Draw fluid
        int meterHeight = 55;
        if (fluidStack != null)
        {
            this.drawFluid(matrixStack, this.leftPos + x -1, this.topPos + y, -3, 1, 14, (int) ((meterHeight - 1) * scale), fluidStack);
        }

        //Draw lines
        RenderSystem._setShaderTexture(0, GUI);
        int meterWidth = 14;
        this.blit(matrixStack, this.leftPos + x, this.topPos + y, 176, 35, meterWidth, meterHeight);

        //Reset color
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if(mouseButton == 0) {
            double xAxis = (mouseX - (width - imageWidth) / 2);
            double yAxis = (mouseY - (height - imageHeight) / 2);
            if(xAxis >= 80 && xAxis <= 85 && yAxis >= 21 && yAxis <= 31) {
                //Playing Click sound
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                //Sending packet to server
                PacketHandler.INSTANCE.sendToServer(new PacketShowPrevious());
            } else if(xAxis >= 108 && xAxis <= 113 && yAxis >= 21 && yAxis <= 31) {
                //Playing Click sound
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                //Sending packet to server
                PacketHandler.INSTANCE.sendToServer(new PacketShowNext() );
            } else if(xAxis >= 148 && xAxis <= 167 && yAxis >= 7 && yAxis <= 27) {
                //Playing Click sound
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                //Sending packet to server
                PacketHandler.INSTANCE.sendToServer(new PacketChangeSettingsReplicatorServer(!te.isActiveClient(), te.isCurrentClientMode()) );
            } else if(xAxis >= 148 && xAxis <= 167 && yAxis >= 31 && yAxis <= 51) {
                //Playing Click sound
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                //Sending packet to server
                PacketHandler.INSTANCE.sendToServer(new PacketChangeSettingsReplicatorServer(te.isActiveClient(), !te.isCurrentClientMode()) );
            }
        }
        return true;
    }
}