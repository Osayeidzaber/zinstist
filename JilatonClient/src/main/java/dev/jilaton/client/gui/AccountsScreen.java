package dev.jilaton.client.gui;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.design.Canvas;
import dev.jilaton.client.gui.design.Ui;
import net.minecraft.client.User;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Account manager, built to the "Account manager" design board (development/GUI_REDESIGN.md §11). It shows the
 * signed-in session with a live skin preview and opens Minecraft's skin settings. Adding and switching accounts
 * needs the Microsoft sign-in backend, which waits for the user's approval of its security design, so no add or
 * switch control is shown until it exists.
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

        // The signed-in account row.
        float rowY = TOP + 64;
        Ui.card(graphics, LEFT, rowY, LIST_W, 76, 14, Ui.CARD, Ui.alpha(accent, 0.5));
        Ui.face(graphics, LEFT + 18, rowY + 16, 44);
        Ui.text(graphics, user.getName(), LEFT + 78, rowY + 18, 15, JilatonType.Face.SEMIBOLD, Ui.TEXT);
        String type = ms ? "MICROSOFT" : "OFFLINE";
        float tx = LEFT + 86 + Ui.width(user.getName(), 15, JilatonType.Face.SEMIBOLD);
        float tw = Ui.spacedWidth(type, 10, 0.6F, JilatonType.Face.MEDIUM) + 16;
        Ui.outline(graphics, tx, rowY + 17, tw, 20, 6, Ui.STROKE);
        Ui.spaced(graphics, type, tx + 8, rowY + 22, 10, 0.6F, JilatonType.Face.MEDIUM, ms ? Ui.TEXT_2 : Ui.WARN);
        Ui.text(graphics, ms ? "Signed in · this session" : "Singleplayer and offline servers only",
                LEFT + 78, rowY + 44, 12, Ui.TEXT_2);
        Ui.circle(graphics, LEFT + LIST_W - 74, rowY + 38, 3.5F, Ui.ONLINE);
        Ui.text(graphics, "In use", LEFT + LIST_W - 64, rowY + 32, 12, Ui.ONLINE);

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

        float factY = TOP + 470;
        fact(graphics, "Type", ms ? "Microsoft" : "Offline", Ui.TEXT, factY);
        fact(graphics, "Session", ms ? "Valid · refreshes on its own" : "Offline session", ms ? Ui.ONLINE : Ui.TEXT_2, factY + 26);
        if (skinModel == null) skinModel = skinModel();
        fact(graphics, "Skin model", skinModel, Ui.TEXT, factY + 52);
        canvas.end(graphics);

        if (avatar != null) {
            avatar.render(graphics, canvas.screenX(CARD_X + CARD_W / 2), canvas.screenY(TOP + 320),
                    Math.round(240 * canvas.scale()), canvas.screenX(CARD_X + 8), canvas.screenX(CARD_X + CARD_W - 8),
                    openedAt, now, mouseX, mouseY, reduced, JilatonClient.clientConfig().animationIntensity());
        }
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
            if (Ui.inside(mx, my, LEFT, TOP, 36, 36)) {
                onClose();
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
