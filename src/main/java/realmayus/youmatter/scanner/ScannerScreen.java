package realmayus.youmatter.scanner;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import realmayus.youmatter.YouMatter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScannerScreen extends ContainerScreen<ScannerContainer> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 168;

    private ScannerTile te;

    private static final ResourceLocation GUI = new ResourceLocation(YouMatter.MODID, "textures/gui/scanner.png");

    public ScannerScreen(ScannerContainer container, PlayerInventory inv, ITextComponent name) {
        super(container, inv, name);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);

        int xAxis = (mouseX - (width - WIDTH) / 2);
        int yAxis = (mouseY - (height - HEIGHT) / 2);

        if (xAxis >= 141 && xAxis <= 156 && yAxis >= 37 && yAxis <= 57) {
            drawTooltip(mouseX, mouseY, Stream.of(new TranslationTextComponent("youmatter.gui.energy.title").getFormattedText(), new TranslationTextComponent("youmatter.gui.energy.description", te.getClientEnergy()).getFormattedText()).collect(Collectors.toList()));
        }

        if (!te.getHasEncoderClient()) {
            if (xAxis >= 16 && xAxis <= 32 && yAxis >= 59 && yAxis <= 75) {
                drawTooltip(mouseX, mouseY, Stream.of(new TranslationTextComponent("youmatter.warning.scanner1").getFormattedText(), new TranslationTextComponent("youmatter.warning.scanner2").getFormattedText(), new TranslationTextComponent("youmatter.warning.scanner3").getFormattedText()).collect(Collectors.toList()));

            }
        }

    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        drawEnergyBolt(te.getClientEnergy());
        drawProgressDisplayChain(te.getClientProgress());

        if(!te.getHasEncoderClient()) {
            this.blit(16, 59, 176, 101, 16, 16);
        }
        drawString(font, new TranslationTextComponent("youmatter.guiname.scanner").getFormattedText(), 8, 6, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(GUI);
        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        this.blit(relX, relY, 0, 0, WIDTH, HEIGHT);
    }

    private void drawProgressDisplayChain(int progress) {
        int circuits;
        int arrow;

        if(progress < 50) {
            circuits = progress * 2;
            arrow = 0;
        } else if(progress < 100) {
            circuits = 100;
            arrow = (progress -50) * 2;
        } else {
            circuits = 100;
            arrow = 100;
        }

        this.minecraft.getTextureManager().bindTexture(GUI);
        this.blit(79, 63, 176, 41, Math.round((arrow / 100.0f) * 18), 12);
        this.blit(104, 35, 176, 53, 17, Math.round((circuits / 100.0f) * 24));
        this.blit(54, 35, 176, 77, 17, Math.round((circuits / 100.0f) * 24));
    }

    private void drawEnergyBolt(int energy) {
        this.minecraft.getTextureManager().bindTexture(GUI);

        if(energy == 0) {
            this.blit(141, 36, 176, 21, 15, 20);
        } else {
            double percentage = energy * 100 / 1000000;  // i know this is dumb
            float percentagef = (float) percentage / 100; // but it works.
            this.blit(141, 36, 176, 0, 15, Math.round(20 * percentagef)); // it's not really intended that the bolt fills from the top but it looks cool tbh.

        }
    }

    private void drawTooltip(int x, int y, List<String> tooltips) {
        renderTooltip(tooltips, x, y);
    }
}
