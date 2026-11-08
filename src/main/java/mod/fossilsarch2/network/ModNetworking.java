package mod.fossilsarch2.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Common (both-sides) payload registration. Client-bound handlers that touch client-only classes
 * are registered separately from {@link mod.fossilsarch2.FossilsArch2Client}, so this class never
 * references anything that is absent on a dedicated server.
 */
public final class ModNetworking {

	private static final String VERSION = "1";

	public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(VERSION);
		registrar.playToClient(DinopediaPayload.TYPE, DinopediaPayload.CODEC);
	}

	public static void sendDinopedia(ServerPlayer player, int entityId) {
		PacketDistributor.sendToPlayer(player, new DinopediaPayload(entityId));
	}

	private ModNetworking() {}
}
