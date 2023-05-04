package earth.terrarium.argonauts.common.network.messages;

import com.teamresourceful.resourcefullib.common.networking.base.Packet;
import com.teamresourceful.resourcefullib.common.networking.base.PacketContext;
import com.teamresourceful.resourcefullib.common.networking.base.PacketHandler;
import earth.terrarium.argonauts.Argonauts;
import earth.terrarium.argonauts.common.commands.party.PartyCommandHelper;
import earth.terrarium.argonauts.common.handlers.party.Party;
import earth.terrarium.argonauts.common.handlers.party.PartyException;
import earth.terrarium.argonauts.common.handlers.party.PartyHandler;
import earth.terrarium.argonauts.common.handlers.party.members.MemberPermissions;
import earth.terrarium.argonauts.common.handlers.party.members.PartyMember;
import earth.terrarium.argonauts.common.menus.PartySettingMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ServerboundSetSettingPacket(String setting,
                                          boolean value) implements Packet<ServerboundSetSettingPacket> {

    public static final ResourceLocation ID = new ResourceLocation(Argonauts.MOD_ID, "set_setting");
    public static final PacketHandler<ServerboundSetSettingPacket> HANDLER = new Handler();

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public PacketHandler<ServerboundSetSettingPacket> getHandler() {
        return HANDLER;
    }

    private static class Handler implements PacketHandler<ServerboundSetSettingPacket> {

        @Override
        public void encode(ServerboundSetSettingPacket message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.setting);
            buffer.writeBoolean(message.value);
        }

        @Override
        public ServerboundSetSettingPacket decode(FriendlyByteBuf buffer) {
            return new ServerboundSetSettingPacket(buffer.readUtf(), buffer.readBoolean());
        }

        @Override
        public PacketContext handle(ServerboundSetSettingPacket message) {
            return (player, level) ->
                PartyCommandHelper.runPartyNetworkAction(player, () -> {
                    Party party = PartyHandler.get(player);
                    if (player.containerMenu instanceof PartySettingMenu menu && party != null) {
                        PartyMember member = party.getMember(player);
                        if (menu.isPartyScreen()) {
                            if (!member.hasPermission(MemberPermissions.MANAGE_SETTINGS)) {
                                throw PartyException.NO_PERMISSIONS;
                            }
                            party.settings().set(message.setting, message.value);
                        } else {
                            member.settings().set(message.setting, message.value);
                        }
                    }
                });
        }
    }
}
