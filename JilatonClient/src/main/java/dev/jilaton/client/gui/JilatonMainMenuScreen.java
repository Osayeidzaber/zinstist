package dev.jilaton.client.gui;

import dev.jilaton.client.JilatonClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Jilaton's title screen: a black editorial canvas with slow light arcs, an oversized wordmark and an
 * index-style navigation list whose rows invert under a white sweep. Entrance and hover motion are
 * time-based and scale with the configured animation intensity.
 */
public final class JilatonMainMenuScreen extends Screen {
    private static final String TICKER = "COMBAT  ✦  MOVEMENT  ✦  RENDER  ✦  PLAYER  ✦  CLIENT  ✦  ";
    private final List<MenuAction> actions = new ArrayList<>(5);
    /** Set on the first frame not covered by the loading overlay, so entrances are actually seen. */
    private long openedAt;
    private final AnimatedValue parallaxX = new AnimatedValue();
    private final AnimatedValue parallaxY = new AnimatedValue();
    private long frameNanos;
    private boolean compact;
    private int margin;
    private int heroX;
    private int heroY;
    private int heroWidth;
    private int navTop;
    private MenuAvatar avatar;
    private boolean showAvatar;
    private int avatarCenterX;
    private int avatarFloorY;
    private int avatarHeight;
    private int avatarClipLeft;
    private int avatarClipRight;

    public JilatonMainMenuScreen() {
        super(Component.literal("Jilaton Client"));
    }

    @Override
    protected void init() {
        actions.clear();
        compact = width < 640 || height < 380;
        margin = Math.max(16, Math.min(48, width / 20));
        int navWidth;
        int navX;
        int navY;
        int rowHeight;
        if (compact) {
            rowHeight = height < 330 ? 20 : 24;
            navY = height - margin - 18 - rowHeight * 5;
            heroX = margin;
            heroWidth = width - margin * 2;
            heroY = margin / 2 + 6 + 28 + 22;
            avatarHeight = Math.max(70, Math.min(150, rowHeight * 5 + 24));
            int slot = Math.round(avatarHeight * 0.55F) + 40;
            showAvatar = width - margin * 2 - slot >= 230;
            if (showAvatar) {
                navWidth = Math.min(320, width - margin * 2 - slot);
                navX = margin;
                avatarCenterX = navX + navWidth + slot / 2 + 8;
                avatarFloorY = navY + rowHeight * 5;
                avatarClipLeft = navX + navWidth + 6;
                avatarClipRight = width;
            } else {
                navWidth = Math.min(340, width - margin * 2);
                navX = (width - navWidth) / 2;
            }
        } else {
            navWidth = Math.min(330, Math.max(220, width * 32 / 100));
            navX = width - margin - navWidth;
            rowHeight = 30;
            navY = Math.max(margin + 70, height / 2 - rowHeight * 5 / 2 + 10);
            heroX = margin;
            heroWidth = navX - margin - 40;
            heroY = margin + 62;
            avatarHeight = Math.max(110, Math.min(200, height * 42 / 100));
            int slot = Math.round(avatarHeight * 0.55F) + 60;
            showAvatar = heroWidth - slot >= 320;
            if (showAvatar) {
                heroWidth -= slot;
                avatarCenterX = navX - 30 - slot / 2;
                avatarFloorY = navY + rowHeight * 5;
                avatarClipLeft = heroX + heroWidth + 10;
                avatarClipRight = navX - 12;
            }
        }
        if (showAvatar && avatar == null) {
            try {
                avatar = new MenuAvatar(minecraft);
            } catch (RuntimeException exception) {
                showAvatar = false;
            }
        }
        navTop = navY;
        addAction("Singleplayer", "LOCAL WORLDS", navX, navY, navWidth, rowHeight, true,
                () -> minecraft.setScreen(new JilatonWorldsScreen(this)));
        addAction("Multiplayer", "SERVERS", navX, navY + rowHeight, navWidth, rowHeight, false,
                () -> minecraft.setScreen(minecraft.options.skipMultiplayerWarning
                        ? new JilatonServersScreen(this) : new net.minecraft.client.gui.screens.multiplayer.SafetyScreen(this)));
        addAction("Jilaton", "MODULES  ·  THEME", navX, navY + rowHeight * 2, navWidth, rowHeight, false,
                () -> JilatonClient.openClickGui(this));
        addAction("Options", "VIDEO  ·  CONTROLS", navX, navY + rowHeight * 3, navWidth, rowHeight, false,
                () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)));
        addAction("Quit", "EXIT GAME", navX, navY + rowHeight * 4, navWidth, rowHeight, false, minecraft::stop);
    }

    private void addAction(String label, String detail, int x, int y, int rowWidth, int rowHeight,
                           boolean primary, Runnable action) {
        Button button = Button.builder(Component.literal(label), ignored -> action.run())
                .bounds(x, y, rowWidth, rowHeight)
                .build();
        button.setAlpha(0.0F);
        addRenderableWidget(button);
        actions.add(new MenuAction(label, detail, x, y, rowWidth, rowHeight, primary, button,
                new Motion.Transition(0.34, Motion.SETTLE)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        frameNanos = System.nanoTime();
        if (openedAt == 0L || minecraft.getOverlay() != null) openedAt = frameNanos;
        boolean reduced = JilatonClient.clientConfig().reducedMotion();
        double intensity = JilatonClient.clientConfig().animationIntensity();
        double time = reduced ? 0.0 : (frameNanos - openedAt) / 1_000_000_000.0;
        double px = parallaxX.update(Motion.amount((mouseX - width / 2.0) / Math.max(1, width), intensity, reduced), frameNanos, reduced);
        double py = parallaxY.update(Motion.amount((mouseY - height / 2.0) / Math.max(1, height), intensity, reduced), frameNanos, reduced);
        renderBackdrop(graphics, time, px, py, reduced);
        renderChrome(graphics, reduced, intensity, time);
        renderHero(graphics, reduced, intensity, px, py);
        if (showAvatar && avatar != null) {
            avatar.render(graphics, avatarCenterX, avatarFloorY, avatarHeight, avatarClipLeft, avatarClipRight,
                    openedAt, frameNanos, mouseX, mouseY, reduced, intensity);
            double label = entrance(1.9, reduced);
            String name = avatar.name().toUpperCase(java.util.Locale.ROOT);
            graphics.drawString(font, name, avatarCenterX - font.width(name) / 2, avatarFloorY + 10,
                    JilatonUi.alpha(JilatonUi.TEXT_2, label), false);
            if (!compact) {
                String hint = "DRAG TO TURN";
                graphics.drawString(font, hint, avatarCenterX - font.width(hint) / 2, avatarFloorY + 22,
                        JilatonUi.alpha(JilatonUi.TEXT_4, label), false);
            }
        }
        renderNavigation(graphics, mouseX, mouseY, reduced, intensity);
        renderTicker(graphics, reduced, intensity, time);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private double entrance(double delay, boolean reduced) {
        return reduced ? 1.0 : Motion.staggered(openedAt, frameNanos, delay, 0.85, Motion.SETTLE);
    }

    private int rise(double progress, double intensity, boolean reduced) {
        return (int) Math.round(Motion.amount(14, intensity, reduced) * (1 - progress));
    }

    private void renderBackdrop(GuiGraphics graphics, double time, double px, double py, boolean reduced) {
        JilatonBackdrop.render(graphics, width, height, time, px, py, entrance(0.0, reduced));
    }

    private void renderChrome(GuiGraphics graphics, boolean reduced, double intensity, double time) {
        double p = entrance(0.0, reduced);
        int top = margin / 2 + 6 - rise(p, intensity, reduced) / 2;
        int markSize = 12;
        JilatonUi.strokeRounded(graphics, margin, top - 3, markSize + 6, markSize + 6, 3, JilatonUi.alpha(JilatonUi.HAIRLINE_STRONG, p));
        JilatonLogo.draw(graphics, margin + 3, top, markSize, JilatonUi.alpha(JilatonUi.TEXT, p));
        graphics.drawString(font, "JILATON", margin + markSize + 14, top + 2, JilatonUi.alpha(JilatonUi.TEXT, p), false);
        graphics.drawString(font, "— 0.1", margin + markSize + 18 + font.width("JILATON"), top + 2,
                JilatonUi.alpha(JilatonUi.TEXT_3, p), false);

        String status = JilatonClient.moduleManager().all().size() + " MODULES  ·  READY";
        int pillWidth = font.width(status) + 26;
        int pillX = width - margin - pillWidth;
        JilatonUi.strokeRounded(graphics, pillX, top - 4, pillWidth, 18, 3, JilatonUi.alpha(JilatonUi.HAIRLINE_STRONG, p));
        double pulse = reduced ? 1 : 0.55 + 0.45 * Math.sin(time * 2.4);
        graphics.fill(pillX + 9, top + 3, pillX + 12, top + 6, JilatonUi.alpha(JilatonUi.TEXT, p * pulse));
        graphics.drawString(font, status, pillX + 17, top + 1, JilatonUi.alpha(JilatonUi.TEXT_2, p), false);

        double q = entrance(0.08, reduced);
        int labelY = top + 28 - rise(q, intensity, reduced) / 2;
        graphics.drawString(font, "00 — MAIN MENU", margin, labelY, JilatonUi.alpha(JilatonUi.TEXT_3, q), false);
        if (!compact) {
            String right = "FABRIC  ·  MINECRAFT 1.21.11";
            graphics.drawString(font, right, width - margin - font.width(right), labelY, JilatonUi.alpha(JilatonUi.TEXT_3, q), false);
        }
        int lineWidth = (int) Math.round((width - margin * 2) * q);
        graphics.fill(margin, labelY + 13, margin + lineWidth, labelY + 14, JilatonUi.HAIRLINE);
    }

    private void renderHero(GuiGraphics graphics, boolean reduced, double intensity, double px, double py) {
        Component word = Component.literal("JILATON").withStyle(ChatFormatting.BOLD);
        int wordWidth = font.width(word);
        float scale = (float) Math.max(2.0, Math.min(compact ? 4.0 : 7.0, heroWidth * 0.92 / wordWidth));
        boolean subtitle = true;
        if (compact) {
            // Fit wordmark + subtitle into the space above the navigation; drop the subtitle if it cannot fit.
            int available = navTop - 8 - heroY;
            scale = (float) Math.min(scale, (available - 8) / 12.06);
            if (scale < 2.2F) {
                subtitle = false;
                scale = (float) Math.min(heroWidth * 0.92 / wordWidth, available / 9.0);
            }
            scale = Math.max(1.0F, scale);
        }
        double p = entrance(0.14, reduced);
        int y = heroY + rise(p, intensity, reduced);
        float shiftX = (float) (px * 10);
        float shiftY = (float) (py * 6);
        graphics.pose().pushMatrix();
        graphics.pose().translate(heroX + shiftX, y + shiftY);
        graphics.pose().scale(scale, scale);
        graphics.drawString(font, word, 0, 0, JilatonUi.alpha(JilatonUi.TEXT, p), false);
        graphics.pose().popMatrix();

        if (!subtitle) return;
        int below = y + Math.round(9 * scale) + 8;
        double q = entrance(0.24, reduced);
        float subScale = Math.max(1.0F, scale * 0.34F);
        int subY = below + rise(q, intensity, reduced);
        graphics.pose().pushMatrix();
        graphics.pose().translate(heroX + shiftX * 0.6F, subY + shiftY * 0.6F);
        graphics.pose().scale(subScale, subScale);
        Component client = Component.literal("client").withStyle(ChatFormatting.ITALIC);
        graphics.drawString(font, client, 1, 0, JilatonUi.alpha(JilatonUi.TEXT_3, q), false);
        Component edition = Component.literal("  for java edition");
        graphics.drawString(font, edition, 1 + font.width(client), 0, JilatonUi.alpha(JilatonUi.TEXT_4, q), false);
        graphics.pose().popMatrix();
        if (compact) return;

        double r = entrance(0.32, reduced);
        int textY = subY + Math.round(9 * subScale) + 26 + rise(r, intensity, reduced);
        Component tagline = Component.literal("Built for the fight, ")
                .append(Component.literal("drawn in the dark.").withStyle(ChatFormatting.ITALIC));
        graphics.pose().pushMatrix();
        graphics.pose().translate(heroX, textY);
        graphics.pose().scale(1.5F, 1.5F);
        graphics.drawString(font, tagline, 0, 0, JilatonUi.alpha(JilatonUi.TEXT, r), false);
        graphics.pose().popMatrix();
        double s = entrance(0.4, reduced);
        int bodyY = textY + 24 + rise(s, intensity, reduced);
        graphics.drawString(font, "Combat, movement and render modules behind one", heroX, bodyY,
                JilatonUi.alpha(JilatonUi.TEXT_2, s), false);
        graphics.drawString(font, "quiet, monochrome interface. Tune everything; keep it yours.", heroX, bodyY + 12,
                JilatonUi.alpha(JilatonUi.TEXT_2, s), false);
    }

    private void renderNavigation(GuiGraphics graphics, int mouseX, int mouseY, boolean reduced, double intensity) {
        for (int index = 0; index < actions.size(); index++) {
            MenuAction action = actions.get(index);
            double p = entrance(0.22 + index * 0.055, reduced);
            int offset = (int) Math.round(Motion.amount(18, intensity, reduced) * (1 - p));
            int x = action.x() + offset;
            int y = action.y();
            int w = action.width();
            int h = action.height();
            boolean hovered = mouseX >= action.x() && mouseX < action.x() + w && mouseY >= y && mouseY < y + h;
            boolean active = hovered || action.button().isFocused();
            double sweep = action.hover().update(active, frameNanos, reduced);
            if (index == 0) graphics.fill(x, y, x + (int) Math.round(w * p), y + 1, JilatonUi.HAIRLINE_STRONG);
            graphics.fill(x, y + h - 1, x + (int) Math.round(w * p), y + h, JilatonUi.HAIRLINE_STRONG);
            int fillWidth = (int) Math.round(w * sweep);
            if (fillWidth > 0) graphics.fill(x, y + 1, x + fillWidth, y + h - 1, JilatonUi.alpha(JilatonUi.TEXT, p));

            int light = action.label().equals("Quit") ? JilatonUi.TEXT_2 : JilatonUi.TEXT;
            drawRow(graphics, action, index, x, y, w, h, JilatonUi.alpha(light, p), JilatonUi.alpha(JilatonUi.TEXT_3, p),
                    JilatonUi.alpha(JilatonUi.TEXT_4, p), sweep);
            if (fillWidth > 0) {
                graphics.enableScissor(x, y + 1, x + fillWidth, y + h - 1);
                int ink = JilatonUi.alpha(0xFF050505, p);
                drawRow(graphics, action, index, x, y, w, h, ink, JilatonUi.alpha(0xFF3A3A3A, p), JilatonUi.alpha(0xFF5A5A5A, p), sweep);
                graphics.disableScissor();
            }
        }
    }

    private void drawRow(GuiGraphics graphics, MenuAction action, int index, int x, int y, int w, int h,
                         int primary, int secondary, int tertiary, double sweep) {
        int textY = y + (h - 8) / 2;
        graphics.drawString(font, String.format("%02d", index + 1), x + 2, textY, tertiary, false);
        int labelX = x + 28 + (int) Math.round(6 * sweep);
        graphics.drawString(font, action.label(), labelX, textY, primary, false);
        if (action.primary()) {
            int dotX = labelX + font.width(action.label()) + 6;
            graphics.fill(dotX, textY + 3, dotX + 3, textY + 6, primary);
        }
        String arrow = "↗";
        int arrowX = x + w - font.width(arrow) - 4 - (int) Math.round(2 * sweep);
        graphics.drawString(font, arrow, arrowX, textY - (int) Math.round(2 * sweep), primary, false);
        if (!compact) {
            graphics.drawString(font, action.detail(), arrowX - 10 - font.width(action.detail()), textY, secondary, false);
        }
    }

    private void renderTicker(GuiGraphics graphics, boolean reduced, double intensity, double time) {
        double p = entrance(0.5, reduced);
        int baseline = height - margin / 2 - 10;
        if (!compact) {
            int stripY = baseline - 22;
            graphics.fill(0, stripY, width, stripY + 1, JilatonUi.alpha(JilatonUi.HAIRLINE, p));
            int tickerWidth = font.width(TICKER);
            int offset = (int) Math.round((time * 18.0) % tickerWidth);
            for (int x = -offset; x < width; x += tickerWidth) {
                graphics.drawString(font, TICKER, x, stripY + 7, JilatonUi.alpha(JilatonUi.TEXT_4, p), false);
            }
            graphics.fill(0, stripY + 21, width, stripY + 22, JilatonUi.alpha(JilatonUi.HAIRLINE, p));
        }
        graphics.drawString(font, "RIGHT SHIFT — OPEN MENU", margin, baseline + 6, JilatonUi.alpha(JilatonUi.TEXT_4, p), false);
        if (compact && showAvatar) return;
        String right = "MONOCHROME BY DEFAULT";
        graphics.drawString(font, right, width - margin - font.width(right), baseline + 6, JilatonUi.alpha(JilatonUi.TEXT_4, p), false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (showAvatar && avatar != null && event.button() == 0 && avatar.mouseClicked(event.x(), event.y())) return true;
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
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private record MenuAction(String label, String detail, int x, int y, int width, int height,
                              boolean primary, Button button, Motion.Transition hover) {
    }
}
