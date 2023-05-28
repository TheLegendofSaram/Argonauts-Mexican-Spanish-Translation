package earth.terrarium.argonauts.client.screens.base.members;

import com.teamresourceful.resourcefullib.client.screens.AbstractContainerCursorScreen;
import com.teamresourceful.resourcefullib.client.utils.ScreenUtils;
import earth.terrarium.argonauts.Argonauts;
import earth.terrarium.argonauts.client.screens.base.members.entries.*;
import earth.terrarium.argonauts.client.utils.MouseLocationFix;
import earth.terrarium.argonauts.common.handlers.base.MemberPermissions;
import earth.terrarium.argonauts.common.handlers.base.members.Member;
import earth.terrarium.argonauts.common.handlers.base.members.MemberState;
import earth.terrarium.argonauts.common.menus.base.MembersMenu;
import net.minecraft.Optionull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class MembersScreen extends AbstractContainerCursorScreen<MembersMenu> {

    private static final ResourceLocation CONTAINER_BACKGROUND = new ResourceLocation(Argonauts.MOD_ID, "textures/gui/members.png");

    public MembersScreen(MembersMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageHeight = 223;
        this.imageWidth = 276;
    }

    @Override
    protected void init() {
        MouseLocationFix.fix(this.getClass());
        super.init();

        addRenderableWidget(new MembersList(this.leftPos + 8, this.topPos + 29, 70, 180, 20, item ->
            this.menu.getId(Optionull.map(item, MembersList.Entry::profile))
                .ifPresent(id -> ScreenUtils.sendClick(this.menu.containerId, id)))).update(this.menu.members());

        var list = addRenderableWidget(new MemberSettingList(this.leftPos + 84, this.topPos + 29, 184, 180));

        Member member = this.menu.getSelected();
        Member self = this.menu.getSelf();
        if (member != null && self != null) {
            boolean cantModify = member.equals(self) || member.getState().isLeader();

            Component status = member.getState() == MemberState.INVITED ? Component.translatable("argonauts.member.status.invited") : Component.translatable("argonauts.member.status.accepted");
            list.addEntry(new TextEntry(Component.translatable("argonauts.member.status.text"), status));

            list.addEntry(new DividerEntry(Component.translatable("argonauts.member.settings")));

            var entry = new RoleNameEntry(!cantModify);
            list.addEntry(entry);
            entry.setText(member.getRole());

            list.addEntry(new DividerEntry(Component.translatable("argonauts.member.permissions")));
            List<String> leftOver = new ArrayList<>(member.permissions());
            leftOver.remove(MemberPermissions.TEMPORARY_GUILD_PERMISSIONS);
            for (String permission : MemberPermissions.ALL_PERMISSIONS) {
                list.addEntry(new BooleanEntry(permission, member.hasPermission(permission), !cantModify && this.menu.canManagePermissions() && self.hasPermission(permission)));
                leftOver.remove(permission);
            }
            leftOver.removeAll(getAdditionalPermissions());

            for (String permission : leftOver) {
                list.addEntry(new BooleanEntry(permission, true, !cantModify && this.menu.canManagePermissions() && self.hasPermission(permission)));
            }

            additionalEntries(list, member, cantModify, self);

            list.addEntry(new DividerEntry(Component.translatable("argonauts.member.actions")));
            list.addEntry(new CommandEntry(
                Component.translatable("argonauts.member.remove"),
                Component.translatable("argonauts.member.remove.button"),
                runRemoveCommand(member),
                !cantModify && this.menu.canManageMembers()
            ));
        }
    }

    public void additionalEntries(MemberSettingList list, Member member, boolean cantModify, Member self) {}

    public Collection<String> getAdditionalPermissions() {
        return new ArrayList<>();
    }

    public abstract String runRemoveCommand(Member member);

    @Override
    public void render(@NotNull GuiGraphics graphics, int i, int j, float f) {
        this.renderBackground(graphics);
        super.render(graphics, i, j, f);
        this.renderTooltip(graphics, i, j);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int i, int j) {
        graphics.drawString(font, title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float f, int i, int j) {
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;
        graphics.blit(CONTAINER_BACKGROUND, k, l, 0, 0, this.imageWidth, this.imageHeight, 512, 512);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (Minecraft.getInstance().options.keyInventory.matches(i, j)) {
            return true;
        }
        return super.keyPressed(i, j, k);
    }

    @Override
    public void resize(Minecraft minecraft, int i, int j) {
        super.resize(minecraft, i, j);
    }
}
