package realmayus.youmatter.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.Level;
import realmayus.youmatter.YouMatter;

public class PacketHandler {
    public static SimpleChannel INSTANCE;

    private static int ID = 0;
    private static int nextID() {return ID++;}

    public static void registerMessages() {
        YouMatter.logger.log(Level.INFO, "Registered Packets");
        INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(YouMatter.MODID, "youmatter"), () -> "1.0", s -> true, s -> true);

        //To: Client
        INSTANCE.registerMessage(nextID(), PacketUpdateScannerClient.class, PacketUpdateScannerClient::encode, PacketUpdateScannerClient::new, PacketUpdateScannerClient::handle);
        INSTANCE.registerMessage(nextID(), PacketUpdateReplicatorClient.class, PacketUpdateReplicatorClient::encode, PacketUpdateReplicatorClient::new, PacketUpdateReplicatorClient::handle);
        INSTANCE.registerMessage(nextID(), PacketUpdateCreatorClient.class, PacketUpdateCreatorClient::encode, PacketUpdateCreatorClient::new, PacketUpdateCreatorClient::handle);
        INSTANCE.registerMessage(nextID(), PacketUpdateEncoderClient.class, PacketUpdateEncoderClient::encode, PacketUpdateEncoderClient::new, PacketUpdateEncoderClient::handle);

        //To: Server
        INSTANCE.registerMessage(nextID(), PacketShowNext.class, PacketShowNext::encode, PacketShowNext::new, PacketShowNext::handle);
        INSTANCE.registerMessage(nextID(), PacketShowPrevious.class, PacketShowPrevious::encode, PacketShowPrevious::new, PacketShowPrevious::handle);
        INSTANCE.registerMessage(nextID(), PacketChangeSettingsReplicatorServer.class, PacketChangeSettingsReplicatorServer::encode, PacketChangeSettingsReplicatorServer::new, PacketChangeSettingsReplicatorServer::handle);
        INSTANCE.registerMessage(nextID(), PacketChangeSettingsCreatorServer.class, PacketChangeSettingsCreatorServer::encode, PacketChangeSettingsCreatorServer::new, PacketChangeSettingsCreatorServer::handle);

    }
}
