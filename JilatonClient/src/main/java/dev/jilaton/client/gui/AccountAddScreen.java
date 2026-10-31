package dev.jilaton.client.gui;

import dev.jilaton.client.account.OfflineAccounts;
import dev.jilaton.client.account.SessionSwitcher;
import dev.jilaton.client.gui.design.Canvas;
import dev.jilaton.client.gui.design.Ui;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Add-account dialog, built to the "Account manager · add account" board (development/GUI_REDESIGN.md §11): a
 * 600x500 card over a 60% scrim with Microsoft and Offline name tabs.
 * <p>
 * The Offline tab adds a name (3 to 16 letters, digits or _), stored as a name only. The Microsoft tab shows the
 * device-code layout from the board, but it stays switched off: Microsoft sign-in needs an Azure app client ID and the
 * user's approval of how the session token is stored, so its controls are inactive and the tab says so plainly. It
 * never shows a password field. In rename mode the dialog edits an existing offline name instead.
 */
public final class AccountAddScreen extends Screen {
    /** Inter instead of the vanilla pixel font for everything this screen draws ({@link JilatonType}). */
    private final Font font = JilatonType.font();
    private static final float X = 340, Y = 110, W = 600, H = 500, PAD = 28;
    private static final String[] TABS = {"Microsoft", "Offline name"};

    private final Screen parent;
    private final String renaming;
    private final Canvas canvas = new Canvas();
    private int tab;
    private EditBox name;
    private String problem = "";

    /** Add dialog, opening on the Offline tab when {@code offline}. */
    public AccountAddScreen(Screen parent, boolean offline) {
        this(parent, null);
        tab = offline ? 1 : 0;
    }

    private AccountAddScreen(Screen parent, String renaming) {
        super(Component.literal(renaming == null ? "Add account" : "Rename account"));
        this.parent = parent;
        this.renaming = renaming;
        this.tab = 1;
    }

    /** Rename dialog for an existing offline name. */
    public static AccountAddScreen rename(Screen parent, String name) {
        return new AccountAddScreen(parent, name);
    }

    @Override
    protected void init() {
        canvas.fit(width, height);
        if (parent != null) parent.init(width, height);
        String previous = name == null ? (renaming == null ? "" : renaming) : name.getValue();
        float fx = X + PAD + 14, fy = Y + 178 + 20;
        name = new EditBox(font, canvas.screenX(fx), canvas.screenY(fy) - 4,
                Math.max(40, canvas.screenX(X + W - PAD - 14) - canvas.screenX(fx)), 10, Component.literal("Name"));
        name.setBordered(false);
        name.setMaxLength(16);
        name.setTextColor(Ui.TEXT);
        name.setHint(Component.literal("Player name").withColor(Ui.TEXT_3));
        name.setFilter(value -> value.chars().allMatch(c -> c < 128 && (Character.isLetterOrDigit(c) || c == '_')));
        name.setValue(previous);
        name.setResponder(value -> problem = "");
        addRenderableWidget(name);
        updateVisibility();
    }

    private void updateVisibility() {
        name.visible = tab == 1;
        if (tab == 1) setInitialFocus(name);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (parent != null) parent.render(graphics, -1, -1, partialTick);
        graphics.fill(0, 0, width, height, 0x99060606);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int accent = Ui.accent();
        double mx = canvas.x(mouseX), my = canvas.y(mouseY);
        canvas.begin(graphics);
        for (int i = 3; i >= 1; i--)
            Ui.round(graphics, X - i * 4, Y - i * 4 + 8, W + i * 8, H + i * 8, 18 + i * 4, Ui.alpha(0xFF000000, 0.12));
        Ui.card(graphics, X, Y, W, H, 18, Ui.DRAWER, Ui.STROKE);
        Ui.text(graphics, getTitle().getString(), X + PAD, Y + 26, 20, JilatonType.Face.SEMIBOLD, Ui.TEXT);
        boolean closeHover = Ui.inside(mx, my, X + W - PAD - 30, Y + 22, 30, 30);
        if (closeHover) Ui.round(graphics, X + W - PAD - 30, Y + 22, 30, 30, 8, Ui.RAISED);
        Ui.textCentre(graphics, "×", X + W - PAD - 15, Y + 27, 18, JilatonType.Face.REGULAR, Ui.TEXT_2);

        if (renaming == null) {
            float sx = X + PAD, sy = Y + 72;
            float total = 6;
            for (String label : TABS) total += Ui.width(label, 13, JilatonType.Face.MEDIUM) + 32;
            Ui.card(graphics, sx, sy, total, 36, 10, Ui.CARD, Ui.STROKE);
            float cx = sx + 3;
            for (int i = 0; i < TABS.length; i++) {
                float w = Ui.width(TABS[i], 13, JilatonType.Face.MEDIUM) + 32;
                double t = Ui.ease("add:tab:" + i, Ui.inside(mx, my, cx, sy + 3, w, 30));
                if (i == tab) Ui.round(graphics, cx, sy + 3, w, 30, 8, Ui.TEXT);
                else if (t > 0.01) Ui.round(graphics, cx, sy + 3, w, 30, 8, Ui.alpha(Ui.RAISED, t));
                Ui.text(graphics, TABS[i], cx + 16, sy + 12, 13, JilatonType.Face.MEDIUM, i == tab ? Ui.DEEP : Ui.mix(Ui.TEXT_2, Ui.TEXT, t));
                cx += w;
            }
        }

        if (tab == 0 && renaming == null) renderMicrosoft(graphics, mx, my, accent);
        else renderOffline(graphics, mx, my, accent);
        canvas.end(graphics);
        name.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderMicrosoft(GuiGraphics graphics, double mx, double my, int accent) {
        float x = X + PAD, w = W - PAD * 2;
        Ui.text(graphics, "Sign in on Microsoft's own page with a one-time code.", x, Y + 134, 13, Ui.TEXT_2);
        float box = 52, gap = 8, rowW = box * 8 + gap * 7, bx = X + (W - rowW) / 2, by = Y + 170;
        for (int i = 0; i < 8; i++) {
            float cx = bx + i * (box + gap);
            Ui.card(graphics, cx, by, box, 64, 10, Ui.CARD, Ui.STROKE);
            Ui.textCentre(graphics, "–", cx + box / 2, by + 20, 24, JilatonType.Face.MONO, Ui.FAINT);
        }
        float buttonsY = Y + 262;
        String open = "Open microsoft.com/link", copy = "Copy code";
        float ow = Ui.width(open, 13, JilatonType.Face.SEMIBOLD) + 36, cw = Ui.width(copy, 13, JilatonType.Face.MEDIUM) + 36;
        float start = X + (W - ow - 10 - cw) / 2;
        Ui.round(graphics, start, buttonsY, ow, 40, 10, Ui.alpha(accent, 0.18));
        Ui.textCentre(graphics, open, start + ow / 2, buttonsY + 14, 13, JilatonType.Face.SEMIBOLD, Ui.TEXT_3);
        Ui.outline(graphics, start + ow + 10, buttonsY, cw, 40, 10, Ui.alpha(Ui.STROKE, 0.7));
        Ui.textCentre(graphics, copy, start + ow + 10 + cw / 2, buttonsY + 14, 13, JilatonType.Face.MEDIUM, Ui.TEXT_3);

        float noteY = Y + 330;
        Ui.card(graphics, x, noteY, w, 78, 12, Ui.CARD, Ui.alpha(Ui.WARN, 0.35));
        Ui.circle(graphics, x + 20, noteY + 22, 4, Ui.WARN);
        Ui.text(graphics, "Microsoft sign-in is not switched on yet", x + 34, noteY + 15, 13, JilatonType.Face.SEMIBOLD, Ui.TEXT);
        Ui.text(graphics, "It needs your Azure app client ID and your OK on how the session", x + 34, noteY + 38, 12, Ui.TEXT_2);
        Ui.text(graphics, "token is stored. Until then, use an offline name.", x + 34, noteY + 55, 12, Ui.TEXT_2);

        Ui.hline(graphics, X + 1, Y + H - 58, W - 2, Ui.STROKE);
        Ui.text(graphics, "No password is ever typed into Jilaton. The code expires after 15 minutes.", x, Y + H - 36, 12, Ui.TEXT_3);
    }

    private void renderOffline(GuiGraphics graphics, double mx, double my, int accent) {
        float x = X + PAD, w = W - PAD * 2;
        float top = renaming == null ? Y + 134 : Y + 84;
        Ui.text(graphics, renaming == null ? "Play under any name in singleplayer and on offline-mode servers."
                : "Pick a new name for this offline account.", x, top, 13, Ui.TEXT_2);
        Ui.text(graphics, "Name", x, Y + 158, 12, JilatonType.Face.MEDIUM, Ui.TEXT_2);
        boolean focused = name.isFocused();
        Ui.card(graphics, x, Y + 178, w, 40, 10, Ui.CARD, focused ? Ui.alpha(accent, 0.55) : Ui.STROKE);
        String hint = problem.isEmpty() ? "3 to 16 letters, digits or _" : problem;
        Ui.text(graphics, hint, x, Y + 228, 12, problem.isEmpty() ? Ui.TEXT_3 : Ui.DANGER);

        float noteY = Y + 262;
        Ui.card(graphics, x, noteY, w, 60, 12, Ui.CARD, Ui.STROKE);
        Ui.text(graphics, "Online servers check your account with Microsoft, so an offline", x + 16, noteY + 14, 12, Ui.TEXT_2);
        Ui.text(graphics, "name only works where the server has online mode turned off.", x + 16, noteY + 32, 12, Ui.TEXT_2);

        Ui.hline(graphics, X + 1, Y + H - 72, W - 2, Ui.STROKE);
        String action = renaming == null ? "Add" : "Save";
        float aw = 104, cw = 96, by = Y + H - 56;
        boolean addHover = Ui.inside(mx, my, X + W - PAD - aw, by, aw, 40);
        boolean cancelHover = Ui.inside(mx, my, X + W - PAD - aw - 10 - cw, by, cw, 40);
        Ui.button(graphics, "Cancel", X + W - PAD - aw - 10 - cw, by, cw, 40, cancelHover, false, accent);
        Ui.button(graphics, action, X + W - PAD - aw, by, aw, 40, addHover, true, accent);
    }

    private void submit() {
        OfflineAccounts accounts = SessionSwitcher.offline();
        String value = name.getValue().strip();
        if (renaming != null) {
            if (value.equalsIgnoreCase(renaming) && OfflineAccounts.valid(value)) {
                accounts.rename(renaming, value);
                onClose();
                return;
            }
            problem = accounts.problem(value);
            if (problem.isEmpty() && accounts.rename(renaming, value)) onClose();
            return;
        }
        problem = accounts.problem(value);
        if (problem.isEmpty() && accounts.add(value)) onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = canvas.x(event.x()), my = canvas.y(event.y());
            if (Ui.inside(mx, my, X + W - PAD - 30, Y + 22, 30, 30) || !Ui.inside(mx, my, X, Y, W, H)) {
                onClose();
                return true;
            }
            if (renaming == null) {
                float cx = X + PAD + 3;
                for (int i = 0; i < TABS.length; i++) {
                    float w = Ui.width(TABS[i], 13, JilatonType.Face.MEDIUM) + 32;
                    if (Ui.inside(mx, my, cx, Y + 75, w, 30)) {
                        tab = i;
                        updateVisibility();
                        return true;
                    }
                    cx += w;
                }
            }
            if (tab == 1 || renaming != null) {
                float aw = 104, cw = 96, by = Y + H - 56;
                if (Ui.inside(mx, my, X + W - PAD - aw, by, aw, 40)) {
                    submit();
                    return true;
                }
                if (Ui.inside(mx, my, X + W - PAD - aw - 10 - cw, by, cw, 40)) {
                    onClose();
                    return true;
                }
                if (Ui.inside(mx, my, X + PAD, Y + 178, W - PAD * 2, 40)) {
                    setFocused(name);
                    name.setFocused(true);
                    return super.mouseClicked(event, doubleClick) || true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) && (tab == 1 || renaming != null)) {
            submit();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
