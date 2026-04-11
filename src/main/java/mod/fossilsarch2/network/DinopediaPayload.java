package mod.fossilsarch2.network;

import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DinopediaPayload(int entityId) implements CustomPayload {

    public static final CustomPayload.Id<DinopediaPayload> ID =
            new CustomPayload.Id<>(Identifier.of(FossilsArch2Mod.MOD_ID, "dinopedia"));

    public static final PacketCodec<RegistryByteBuf, DinopediaPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.INTEGER, DinopediaPayload::entityId, DinopediaPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
