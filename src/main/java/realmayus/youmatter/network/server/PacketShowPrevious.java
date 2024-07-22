package realmayus.youmatter.network.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PacketShowPrevious() implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation("youmatter", "packet_show_previous");

    public PacketShowPrevious(FriendlyByteBuf buf) {
        this();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}