package com.elfmcys.ysm.network;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.network.forge.ClientSessionRuntime;
import com.elfmcys.ysm.network.forge.ForgeUniBufferIO;
import com.elfmcys.ysm.network.frame.FrameCodec;
import com.elfmcys.ysm.network.frame.OutboundFrame;
import com.elfmcys.ysm.network.protocol.MessageDirection;
import com.elfmcys.ysm.network.protocol.ProtocolMessageSpec;
import com.elfmcys.ysm.network.protocol.ProtocolMessages;
import com.elfmcys.ysm.network.protocol.ProtocolVersion;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.event.EventNetworkChannel;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import us.hebi.quickbuf.ProtoMessage;

/** The sole Forge adapter for the current YSM frame protocol. */
public final class NetworkHandler {
    public static final String VERSION = ProtocolVersion.TRANSPORT_VERSION;
    public static final ResourceLocation CHANNEL_NAME =
            new ResourceLocation(YesSteveModel.MOD_ID, ProtocolVersion.CHANNEL_PATH);
    public static final EventNetworkChannel CHANNEL = NetworkRegistry.newEventChannel(
            CHANNEL_NAME, () -> VERSION,
            ignored -> true,
            ignored -> true);

    private NetworkHandler() {
    }

    public static void init() {
        CHANNEL.addListener((NetworkEvent.ServerCustomPayloadEvent event) ->
                receive(event, MessageDirection.SERVER_TO_CLIENT));
        CHANNEL.addListener((NetworkEvent.ClientCustomPayloadEvent event) ->
                receive(event, MessageDirection.CLIENT_TO_SERVER));
    }

    @SuppressWarnings("ConstantValue")
    public static boolean isPlayerChannelPresent(ServerPlayer player) {
        return player.connection != null && isChannelPresent(player.connection.connection);
    }

    public static boolean isRemoteChannelPresent() {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        return listener != null && isChannelPresent(listener.getConnection());
    }

    public static boolean isChannelPresent(@Nullable Connection connection) {
        return connection != null && connection.channel() != null
                && CHANNEL.isRemotePresent(connection);
    }

    public static void sendToServer(ProtoMessage<?> message) {
        sendToServer(payload(message));
    }

    public static void sendToServer(NetworkPayload<?> payload) {
        if (!isRemoteChannelPresent()) {
            payload.close();
            return;
        }
        send(PacketDistributor.SERVER.noArg(), payload);
    }

    public static void sendToClientPlayer(ProtoMessage<?> message, Player player) {
        sendToClientPlayer(payload(message), player);
    }

    public static void sendToClientPlayer(NetworkPayload<?> payload, Player player) {
        send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), payload);
    }

    public static void broadcastToAllPlayers(ProtoMessage<?> message) {
        send(PacketDistributor.ALL.noArg(), payload(message));
    }

    public static void broadcastToVisiblePlayers(ProtoMessage<?> message, Entity centerEntity) {
        send(PacketDistributor.TRACKING_ENTITY.with(() -> centerEntity), payload(message));
    }

    public static void broadcastToVisiblePlayersAndSelf(ProtoMessage<?> message, Player self) {
        send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> self), payload(message));
    }

    public static void sendFrame(PacketDistributor.PacketTarget target,
                                 OutboundFrame frame) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer(frame.size(), frame.size()));
        var submitted = false;
        try {
            ForgeUniBufferIO.write(buffer, frame.borrow());
            var packet = target.getDirection().buildPacket(Pair.of(buffer, 0), CHANNEL_NAME).getThis();
            target.send(packet);
            submitted = true;
        } finally {
            if (!submitted) {
                buffer.release();
            }
        }
    }

    private static void send(PacketDistributor.PacketTarget target, NetworkPayload<?> payload) {
        try (payload; var protobuf = ProtocolBuffer.serialize(payload.protobuf());
             var attachment = payload.raw().map(UniBuffer::borrow)
                     .orElseGet(() -> ArrayBuffer.allocate(0))) {
            var spec = messageSpec(payload.protobuf(), target.getDirection());
            spec.attachmentPolicy().validate(attachment.size());
            try (var frame = FrameCodec.encode(spec.id(), protobuf, attachment)) {
                sendFrame(target, frame);
            }
        }
    }

    private static ProtocolMessageSpec<?> messageSpec(
            ProtoMessage<?> message, NetworkDirection direction) {
        var spec = ProtocolMessages.REGISTRY.find(message.getClass())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unregistered protocol message type: " + message.getClass().getName()));
        var expected = direction == NetworkDirection.PLAY_TO_SERVER
                ? MessageDirection.CLIENT_TO_SERVER : MessageDirection.SERVER_TO_CLIENT;
        if (spec.direction() != expected) {
            throw new IllegalArgumentException("Protocol message has the wrong network direction");
        }
        return spec;
    }

    private static void receive(NetworkEvent event, MessageDirection direction) {
        var context = event.getSource();
        try (var wire = ForgeUniBufferIO.readNative(
                event.getPayload(), event.getPayload().readableBytes());
             var frame = FrameCodec.decode(wire, id -> ProtocolInbound.accepts(id, direction))) {
            var spec = ProtocolMessages.REGISTRY.find(frame.messageId()).orElseThrow();
            ProtocolInbound.dispatch(frame, spec, context);
        } catch (RuntimeException error) {
            YesSteveModel.LOGGER.warn("Rejected invalid YSM frame", error);
            if (direction == MessageDirection.SERVER_TO_CLIENT) {
                var source = context.get();
                var connection = source.getNetworkManager();
                source.enqueueWork(() -> {
                    try {
                        ClientSessionRuntime.failProtocolSession(connection);
                    } catch (IllegalStateException ignored) {
                    }
                });
            }
        } finally {
            context.get().setPacketHandled(true);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static NetworkPayload<?> payload(ProtoMessage<?> message) {
        return NetworkPayload.protobuf((ProtoMessage) message);
    }
}
