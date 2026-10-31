package dev.jilaton.client.gui;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.account.SessionSwitcher;
import dev.jilaton.client.gui.design.Canvas;
import dev.jilaton.client.gui.design.Ui;
import net.minecraft.client.User;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Account manager, built to the "Account manager" design board (development/GUI_REDESIGN.md §11): the launch account
 * and saved offline names as rows with Use and a ⋯ menu (Rename, Remove), an Add account dialog
 * ({@link AccountAddScreen}), and a card with the live skin preview of the account in use. Switching happens only
 * with no world loaded. Microsoft sign-in for extra accounts waits for the user's approval of its security design.
 */
public final class AccountsScreen extends Screen {
    /** Inter instead of the vanilla pixel font for everything this screen draws ({@link JilatonType}). */
    private final Font font = JilatonType.font();
    private static final float LEFT = 112, TOP = 64, LIST_W = 680, CARD_X = 848, CARD_W = 320;

    private final Screen parent;
    private final Canvas canvas = new Canvas();
    private MenuAvatar avatar;
    private long openedAt;
    private String skinModel;
    /** Offline name whose ⋯ menu is open, or null. */
    private String menuFor;

    /** A row: the launch account, or a saved offline name. */
    private record Row(String name, boolean launch) { }

    public AccountsScreen(Screen parent) {
        super(Component.literal("Accounts"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        canvas.fit(width, height);
        if (avatar == null) {
            try {
                avatar = new MenuAvatar(minecraft);
            } catch (RuntimeException exception) {
                avatar = null;
            }
        }
    }

    private boolean microsoft() {
        User user = minecraft.getUser();
        return user.getXuid().isPresent() || user.getClientId().isPresent();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        if (openedAt == 0) openedAt = now;
        boolean reduced = JilatonClient.clientConfig().reducedMotion();
        int accent = Ui.accent();
        double mx = canvas.x(mouseX), my = canvas.y(mouseY);
        User user = minecraft.getUser();
        boolean ms = microsoft();

        graphics.fill(0, 0, width, height, Ui.BACKDROP);
        canvas.begin(graphics);
        Ui.glow(graphics, 1140, 160, 380, accent & 0xFFFFFF, 0.05);

        boolean backHover = Ui.inside(mx, my, LEFT, TOP, 36, 36);
        Ui.card(graphics, LEFT, TOP, 36, 36, 10, Ui.alpha(Ui.RAISED, Ui.ease("accounts:back", backHover)), Ui.STROKE);
        Ui.text(graphics, "‹", LEFT + 13, TOP + 8, 17, Ui.SOFT);
        Ui.text(graphics, "Accounts", LEFT + 52, TOP + 4, 26, JilatonType.Face.SEMIBOLD, Ui.TEXT);

        boolean canSwitch = SessionSwitcher.canSwitch(minecraft);
        boolean addHover = Ui.inside(mx, my, LEFT + LIST_W - 140, TOP, 140, 36);
        Ui.button(graphics, "Add account", LEFT + LIST_W - 140, TOP, 140, 36, addHover, true, accent);
        if (!canSwitch)
            Ui.text(graphics, "Leave the world to switch accounts", LEFT + 52, TOP + 40, 12, Ui.TEXT_3);

        List<Row> rows = rows();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            float rowY = rowY(i);
            boolean active = inUse(row);
            boolean rowHover = Ui.inside(mx, my, LEFT, rowY, LIST_W, 76);
            double t = Ui.ease("accounts:row:" + row.name(), rowHover);
            Ui.card(graphics, LEFT, rowY, LIST_W, 76, 14, Ui.mix(Ui.CARD, Ui.RAISED, active ? 0.4 : t * 0.6),
                    active ? Ui.alpha(accent, 0.5) : Ui.STROKE);
            if (active) Ui.face(graphics, LEFT + 18, rowY + 16, 44);
            else Ui.face(graphics, LEFT + 18, rowY + 16, 44, DefaultPlayerSkin.get(row.launch()
                    ? SessionSwitcher.launchUser(minecraft).getProfileId() : UUIDUtil.createOfflinePlayerUUID(row.name())));
            boolean rowMs = row.launch() && (SessionSwitcher.launchUser(minecraft).getXuid().isPresent()
                    || SessionSwitcher.launchUser(minecraft).getClientId().isPresent());
            Ui.text(graphics, row.name(), LEFT + 78, rowY + 18, 15, JilatonType.Face.SEMIBOLD, Ui.TEXT);
            String type = rowMs ? "MICROSOFT" : "OFFLINE";
            float tx = LEFT + 86 + Ui.width(row.name(), 15, JilatonType.Face.SEMIBOLD);
            float tw = Ui.spacedWidth(type, 10, 0.6F, JilatonType.Face.MEDIUM) + 16;
            Ui.outline(graphics, tx, rowY + 17, tw, 20, 6, Ui.STROKE);
            Ui.spaced(graphics, type, tx + 8, rowY + 22, 10, 0.6F, JilatonType.Face.MEDIUM, rowMs ? Ui.TEXT_2 : Ui.WARN);
            String sub = row.launch() ? (rowMs ? "Signed in by your launcher" : "The name the game started with")
                    : "Singleplayer and offline servers only";
            Ui.text(graphics, sub, LEFT + 78, rowY + 44, 12, Ui.TEXT_2);
            float right = LEFT + LIST_W - 18;
            if (!row.launch()) {
                boolean dots = Ui.inside(mx, my, right - 32, rowY + 22, 32, 32);
                double dt = Ui.ease("accounts:dots:" + row.name(), dots || row.name().equals(menuFor));
                if (dt > 0.01) Ui.round(graphics, right - 32, rowY + 22, 32, 32, 8, Ui.alpha(Ui.RAISED, dt));
                for (int d = 0; d < 3; d++) Ui.circle(graphics, right - 22 + d * 6, rowY + 38, 1.6F, Ui.TEXT_2);
                right -= 42;
            }
            if (active) {
                Ui.circle(graphics, right - 52, rowY + 38, 3.5F, Ui.ONLINE);
                Ui.text(graphics, "In use", right - 42, rowY + 32, 12, Ui.ONLINE);
            } else if (canSwitch) {
                Ui.button(graphics, "Use", right - 64, rowY + 22, 64, 32, Ui.inside(mx, my, right - 64, rowY + 22, 64, 32), false, accent);
            } else {
                Ui.outline(graphics, right - 64, rowY + 22, 64, 32, 10, Ui.alpha(Ui.STROKE, 0.6));
                Ui.textCentre(graphics, "Use", right - 32, rowY + 32, 13, JilatonType.Face.MEDIUM, Ui.FAINT);
            }
        }
        if (menuFor != null) {
            int index = indexOf(rows, menuFor);
            if (index < 0) menuFor = null;
            else {
                float menuX = LEFT + LIST_W - 18 - 140, menuY = rowY(index) + 58;
                Ui.round(graphics, menuX - 2, menuY + 4, 144, 84, 12, Ui.alpha(0xFF000000, 0.35));
                Ui.card(graphics, menuX, menuY, 140, 80, 12, Ui.RAISED, Ui.STROKE);
                menuItem(graphics, "Rename", menuX, menuY + 4, Ui.TEXT, mx, my);
                menuItem(graphics, "Remove", menuX, menuY + 40, Ui.DANGER, mx, my);
            }
        }

        // Trust note.
        float noteY = 600;
        Ui.card(graphics, LEFT, noteY, LIST_W, 56, 12, Ui.DRAWER, Ui.STROKE);
        Ui.text(graphics, "Microsoft sign-in happens on Microsoft's own page. Jilaton never sees your password;",
                LEFT + 44, noteY + 13, 12, Ui.TEXT_2);
        Ui.text(graphics, "it keeps only the session token, encrypted on this PC.", LEFT + 44, noteY + 30, 12, Ui.TEXT_2);
        lockIcon(graphics, LEFT + 18, noteY + 20);

        // Selected-account card.
        Ui.card(graphics, CARD_X, TOP, CARD_W, 592, 18, Ui.DRAWER, Ui.STROKE);
        Ui.textCentre(graphics, user.getName(), CARD_X + CARD_W / 2, TOP + 360, 18, JilatonType.Face.SEMIBOLD, Ui.TEXT);
        float skinW = Ui.width("Change skin", 12, JilatonType.Face.MEDIUM) + 24;
        boolean skinHover = Ui.inside(mx, my, CARD_X + (CARD_W - skinW) / 2, TOP + 392, skinW, 30);
        Ui.button(graphics, "Change skin", CARD_X + (CARD_W - skinW) / 2, TOP + 392, skinW, 32, skinHover, false, accent);

        float factY = TOP + 460;
        fact(graphics, "Type", ms ? "Microsoft" : "Offline", Ui.TEXT, factY);
        fact(graphics, "Session", ms ? "Valid \u00b7 refreshes on its own" : "Offline session", ms ? Ui.ONLINE : Ui.TEXT_2, factY + 26);
        if (skinModel == null) skinModel = skinModel();
        fact(graphics, "Skin model", skinModel, Ui.TEXT, factY + 52);
        if (!SessionSwitcher.usingLaunch(minecraft)) {
            float sw = CARD_W - 48;
            boolean signOut = canSwitch && Ui.inside(mx, my, CARD_X + 24, TOP + 538, sw, 36);
            Ui.round(graphics, CARD_X + 24, TOP + 538, sw, 36, 10, signOut ? Ui.alpha(Ui.DANGER, 0.12) : 0);
            Ui.outline(graphics, CARD_X + 24, TOP + 538, sw, 36, 10, Ui.alpha(Ui.DANGER, canSwitch ? 0.6 : 0.25));
            Ui.textCentre(graphics, "Sign out of this account", CARD_X + CARD_W / 2, TOP + 550, 13, JilatonType.Face.MEDIUM,
                    canSwitch ? Ui.DANGER : Ui.alpha(Ui.DANGER, 0.4));
        }
        canvas.end(graphics);

        if (avatar != null) {
            avatar.render(graphics, canvas.screenX(CARD_X + CARD_W / 2), canvas.screenY(TOP + 320),
                    Math.round(240 * canvas.scale()), canvas.screenX(CARD_X + 8), canvas.screenX(CARD_X + CARD_W - 8),
                    openedAt, now, mouseX, mouseY, reduced, JilatonClient.clientConfig().animationIntensity());
        }
    }

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(SessionSwitcher.launchUser(minecraft).getName(), true));
        for (String name : SessionSwitcher.offline().names()) {
            if (rows.size() >= 5) break;
            rows.add(new Row(name, false));
        }
        return rows;
    }

    private static int indexOf(List<Row> rows, String name) {
        for (int i = 0; i < rows.size(); i++) if (!rows.get(i).launch() && rows.get(i).name().equals(name)) return i;
        return -1;
    }

    private static float rowY(int index) {
        return TOP + 64 + index * 86;
    }

    private boolean inUse(Row row) {
        if (row.launch()) return SessionSwitcher.usingLaunch(minecraft);
        return !SessionSwitcher.usingLaunch(minecraft) && minecraft.getUser().getName().equals(row.name());
    }

    private void menuItem(GuiGraphics graphics, String label, float x, float y, int color, double mx, double my) {
        double t = Ui.ease("accounts:menu:" + label, Ui.inside(mx, my, x + 4, y, 132, 32));
        if (t > 0.01) Ui.round(graphics, x + 4, y, 132, 32, 8, Ui.alpha(Ui.STROKE, t));
        Ui.text(graphics, label, x + 16, y + 10, 13, JilatonType.Face.MEDIUM, color);
    }

    /** The account changed: rebuild the preview for the new profile. */
    private void sessionChanged() {
        skinModel = null;
        try {
            avatar = new MenuAvatar(minecraft);
        } catch (RuntimeException exception) {
            avatar = null;
        }
        openedAt = 0;
    }

    private String skinModel() {
        var skin = minecraft.getSkinManager().createLookup(minecraft.getGameProfile(), false).get();
        if (skin == null) return "Wide";
        return skin.model() == net.minecraft.world.entity.player.PlayerModelType.SLIM ? "Slim" : "Wide";
    }

    private void fact(GuiGraphics graphics, String label, String value, int color, float y) {
        Ui.text(graphics, label, CARD_X + 24, y, 12, Ui.TEXT_2);
        Ui.textRight(graphics, value, CARD_X + CARD_W - 24, y, 12, JilatonType.Face.REGULAR, color);
    }

    private static void lockIcon(GuiGraphics graphics, float x, float y) {
        Ui.outline(graphics, x, y + 6, 12, 9, 2, Ui.TEXT_2);
        Ui.outline(graphics, x + 3, y, 6, 8, 3, Ui.TEXT_2);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = canvas.x(event.x()), my = canvas.y(event.y());
            List<Row> rows = rows();
            if (menuFor != null) {
                int index = indexOf(rows, menuFor);
                String name = menuFor;
                menuFor = null;
                if (index >= 0) {
                    float menuX = LEFT + LIST_W - 18 - 140, menuY = rowY(index) + 58;
                    if (Ui.inside(mx, my, menuX + 4, menuY + 4, 132, 32)) {
                        minecraft.setScreen(AccountAddScreen.rename(this, name));
                        return true;
                    }
                    if (Ui.inside(mx, my, menuX + 4, menuY + 40, 132, 32)) {
                        if (!SessionSwitcher.usingLaunch(minecraft) && minecraft.getUser().getName().equals(name)
                                && SessionSwitcher.useLaunch(minecraft)) sessionChanged();
                        SessionSwitcher.offline().remove(name);
                        return true;
                    }
                }
                return true;
            }
            if (Ui.inside(mx, my, LEFT, TOP, 36, 36)) {
                onClose();
                return true;
            }
            if (Ui.inside(mx, my, LEFT + LIST_W - 140, TOP, 140, 36)) {
                minecraft.setScreen(new AccountAddScreen(this, true));
                return true;
            }
            boolean canSwitch = SessionSwitcher.canSwitch(minecraft);
            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);
                float rowY = rowY(i), right = LEFT + LIST_W - 18;
                if (!row.launch()) {
                    if (Ui.inside(mx, my, right - 32, rowY + 22, 32, 32)) {
                        menuFor = row.name();
                        return true;
                    }
                    right -= 42;
                }
                if (!inUse(row) && canSwitch && Ui.inside(mx, my, right - 64, rowY + 22, 64, 32)) {
                    boolean switched = row.launch() ? SessionSwitcher.useLaunch(minecraft)
                            : SessionSwitcher.useOffline(minecraft, row.name());
                    if (switched) sessionChanged();
                    return true;
                }
            }
            if (!SessionSwitcher.usingLaunch(minecraft) && canSwitch
                    && Ui.inside(mx, my, CARD_X + 24, TOP + 538, CARD_W - 48, 36)) {
                if (SessionSwitcher.useLaunch(minecraft)) sessionChanged();
                return true;
            }
            float skinW = Ui.width("Change skin", 12, JilatonType.Face.MEDIUM) + 24;
            if (Ui.inside(mx, my, CARD_X + (CARD_W - skinW) / 2, TOP + 392, skinW, 30)) {
                minecraft.setScreen(new SkinCustomizationScreen(this, minecraft.options));
                return true;
            }
            if (avatar != null && avatar.mouseClicked(event.x(), event.y())) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (avatar != null && avatar.mouseDragged(dragX)) return true;
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (avatar != null) avatar.mouseReleased();
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
