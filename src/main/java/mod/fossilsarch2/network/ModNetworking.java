package mod.fossilsarch2.network;

import mod.fossilsarch2.entity.DinoEggEntity;
import mod.fossilsarch2.entity.DinosaurEntity;
import mod.fossilsarch2.screen.DinopediaScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {

	private static final String VERSION = "1";

	public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(VERSION);
		registrar.playToClient(
				DinopediaPayload.TYPE,
				DinopediaPayload.CODEC,
				ModNetworking::handleDinopedia);
	}

	public static void sendDinopedia(ServerPlayer player, int entityId) {
		PacketDistributor.sendToPlayer(player, new DinopediaPayload(entityId));
	}

	private static void handleDinopedia(DinopediaPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> openDinopediaScreen(payload.entityId()));
	}

	private static void openDinopediaScreen(int entityId) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) return;
		Entity entity = client.level.getEntity(entityId);
		if (entity instanceof DinosaurEntity dino) {
			client.setScreen(DinopediaScreen.forDinosaur(dino));
		} else if (entity instanceof DinoEggEntity egg) {
			client.setScreen(DinopediaScreen.forEgg(egg));
		}
	}

	private ModNetworking() {}
}
