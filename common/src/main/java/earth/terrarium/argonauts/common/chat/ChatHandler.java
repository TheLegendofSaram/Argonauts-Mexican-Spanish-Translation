package earth.terrarium.argonauts.common.chat;


import com.teamresourceful.resourcefullib.common.utils.CommonUtils;
import earth.terrarium.argonauts.api.teams.Team;
import earth.terrarium.argonauts.common.network.NetworkHandler;
import earth.terrarium.argonauts.common.network.packets.ClientboundSendMessagePacket;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ChatHandler {

    public static final int MAX_MESSAGE_COUNT = 200;

    private static final Map<UUID, Set<ChatMessage>> CHANNELS = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger("Argonauts Chat");

    public static Set<ChatMessage> getChannel(UUID teamId) {
        return CHANNELS.computeIfAbsent(teamId, id -> new LinkedHashSet<>());
    }

    public static void removeChannel(UUID teamId) {
        CHANNELS.remove(teamId);
    }

    public static void clearChannels() {
        CHANNELS.clear();
    }

    public static void sendMessage(Level level, Team team, ChatMessage message) {
        Set<ChatMessage> messages = getChannel(team.id());
        if (messages.size() >= MAX_MESSAGE_COUNT) {
            messages.remove(messages.iterator().next());
        }

        LOGGER.info(String.format("[%s] <%s> %s", team.type().toUpperCase(Locale.ROOT), message.profile().getName(), message.message()));

        if (level.isClientSide()) {
            messages.add(message);
        } else {
            ClientboundSendMessagePacket packet = new ClientboundSendMessagePacket(team.id(), message);
            team.onlineMembers(level).forEach(member -> {
                if (team.isMember(member.getUUID())) {
                    Component messageComponent = CommonUtils.serverTranslatable("chat.argonauts.message",
                        team.displayName().plainCopy().withStyle(team.color()),
                        ChatType.bind(ChatType.CHAT, member).name(),
                        message.message()
                    );
                    member.displayClientMessage(messageComponent, false);
                }

                if (NetworkHandler.CHANNEL.canSendToPlayer(member, packet.type())) {
                    NetworkHandler.CHANNEL.sendToPlayer(packet, member);
                }
            });
        }
    }
}
