package mod.fossilsarch2.network;

import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DinopediaPayload(int entityId) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<DinopediaPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "dinopedia"));

	public static final StreamCodec<RegistryFriendlyByteBuf, DinopediaPayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.INT, DinopediaPayload::entityId, DinopediaPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
