package realmayus.youmatter;

import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import realmayus.youmatter.creator.CreatorScreen;
import realmayus.youmatter.encoder.EncoderScreen;
import realmayus.youmatter.replicator.ReplicatorScreen;
import realmayus.youmatter.scanner.ScannerScreen;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = YouMatter.MODID, value = Dist.CLIENT)
public class ClientRegistry {
    @SubscribeEvent
    public static void onFMLClientSetup(FMLClientSetupEvent event)
    {
        ScreenManager.register(ObjectHolders.SCANNER_CONTAINER, ScannerScreen::new);
        ScreenManager.register(ObjectHolders.ENCODER_CONTAINER, EncoderScreen::new);
        ScreenManager.register(ObjectHolders.CREATOR_CONTAINER, CreatorScreen::new);
        ScreenManager.register(ObjectHolders.REPLICATOR_CONTAINER, ReplicatorScreen::new);
    }
}
