package dev.jilaton.client.gui;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.server.LanServer;
import net.minecraft.client.server.LanServerDetection;
import net.minecraft.network.chat.CommonComponents;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.FormattedCharSequence;
import org.slf4j.Logger;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/** Saved servers and LAN games as cards with live status; add, edit, delete, reorder and connect directly. */
public final class JilatonServersScreen extends JilatonCardScreen<ServerData> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadPoolExecutor PINGERS = new ScheduledThreadPoolExecutor(3,
            new ThreadFactoryBuilder().setNameFormat("Jilaton Server Pinger #%d").setDaemon(true).build());
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    private final List<ServerData> saved = new ArrayList<>();
    private final List<ServerData> lan = new ArrayList<>();
    private final List<ServerData> entries = new ArrayList<>();
    private final LanServerDetection.LanServerList lanServers = new LanServerDetection.LanServerList();
    private LanServerDetection.LanServerDetector lanDetector;
    private final Map<ServerData, FaviconTexture> icons = new IdentityHashMap<>();
    private final Map<ServerData, byte[]> iconBytes = new IdentityHashMap<>();
    private ServerList servers;
    private ServerData editing;
    private Button join;
    private Button edit;
    private Button delete;

    public JilatonServersScreen(Screen parent) {
        super(Component.literal("Servers"), "02  SERVERS", parent);
    }

    @Override
    protected void init() {
        if (servers == null) {
            servers = new ServerList(minecraft);
            servers.load();
            reloadEntries();
        }
        if (lanDetector == null) {
            try {
                lanDetector = new LanServerDetection.LanServerDetector(lanServers);
                lanDetector.start();
            } catch (Exception exception) {
                LOGGER.warn("Unable to start LAN server detection: {}", exception.getMessage());
            }
        }
        super.init();
    }

    private void reloadEntries() {
        saved.clear();
        for (int i = 0; i < servers.size(); i++) saved.add(servers.get(i));
        entries.clear();
        entries.addAll(saved);
        entries.addAll(lan);
        refreshFilter();
    }

    @Override
    protected List<ServerData> source() { return entries; }

    @Override
    protected String searchText(ServerData item) { return item.name + " " + item.ip; }

    @Override
    protected String subtitle(int shown) {
        return shown + (shown == 1 ? " server" : " servers") + "  ·  double-click to join  ·  shift + arrows to reorder";
    }

    @Override
    protected Component emptyMessage() {
        return Component.literal("No saved servers — add one or connect directly.");
    }

    @Override
    protected void addActions(int y) {
        int w = Math.min(92, (gridW - 36) / 7);
        int x = width - margin - (w * 7 + 36);
        join = addRenderableWidget(Button.builder(Component.literal("Join"), b -> { if (selected != null) activate(selected); })
                .bounds(x, y, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Direct"), b -> {
            editing = new ServerData(I18n.get("selectServer.defaultName"), "", ServerData.Type.OTHER);
            minecraft.setScreen(new DirectJoinServerScreen(this, this::directJoined, editing));
        }).bounds(x + (w + 6), y, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Add"), b -> {
            editing = new ServerData("", "", ServerData.Type.OTHER);
            minecraft.setScreen(new ManageServerScreen(this, Component.translatable("manageServer.add.title"), this::added, editing));
        }).bounds(x + (w + 6) * 2, y, w, 20).build());
        edit = addRenderableWidget(Button.builder(Component.literal("Edit"), b -> editSelected()).bounds(x + (w + 6) * 3, y, w, 20).build());
        delete = addRenderableWidget(Button.builder(Component.literal("Delete"), b -> confirmDelete()).bounds(x + (w + 6) * 4, y, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> minecraft.setScreen(new JilatonServersScreen(parent)))
                .bounds(x + (w + 6) * 5, y, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(x + (w + 6) * 6, y, w, 20).build());
        selectionChanged();
    }

    @Override
    protected void selectionChanged() {
        if (join == null) return;
        boolean saved = selected != null && !selected.isLan();
        join.active = selected != null;
        edit.active = saved;
        delete.active = saved;
    }

    @Override
    protected void activate(ServerData item) {
        ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(item.ip), item, false, null);
    }

    private void editSelected() {
        if (selected == null || selected.isLan()) return;
        ServerData target = selected;
        editing = new ServerData(target.name, target.ip, ServerData.Type.OTHER);
        editing.copyFrom(target);
        minecraft.setScreen(new ManageServerScreen(this, Component.translatable("manageServer.edit.title"), confirmed -> {
            if (confirmed) {
                target.name = editing.name;
                target.ip = editing.ip;
                target.copyFrom(editing);
                target.setState(ServerData.State.INITIAL);
                servers.save();
            }
            minecraft.setScreen(this);
        }, editing));
    }

    private void confirmDelete() {
        if (selected == null || selected.isLan() || selected.name == null) return;
        ServerData target = selected;
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                servers.remove(target);
                servers.save();
                selected = null;
                reloadEntries();
            }
            minecraft.setScreen(this);
        }, Component.translatable("selectServer.deleteQuestion"),
                Component.translatable("selectServer.deleteWarning", target.name),
                Component.translatable("selectServer.deleteButton"), CommonComponents.GUI_CANCEL));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.hasShiftDown() && selected != null && !selected.isLan() && !search.isFocused() && search.getValue().isBlank()) {
            int index = saved.indexOf(selected);
            int target = switch (event.key()) {
                case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_LEFT -> index - 1;
                case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_RIGHT -> index + 1;
                default -> -2;
            };
            if (target != -2) {
                if (index >= 0 && target >= 0 && target < saved.size()) {
                    servers.swap(index, target);
                    servers.save();
                    ServerData keep = selected;
                    reloadEntries();
                    select(keep);
                }
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private void added(boolean confirmed) {
        if (confirmed) {
            ServerData hidden = servers.unhide(editing.ip);
            if (hidden != null) hidden.copyNameIconFrom(editing);
            else servers.add(editing, false);
            servers.save();
            reloadEntries();
        }
        minecraft.setScreen(this);
    }

    private void directJoined(boolean confirmed) {
        if (!confirmed) {
            minecraft.setScreen(this);
            return;
        }
        ServerData known = servers.get(editing.ip);
        if (known == null) {
            servers.add(editing, true);
            servers.save();
            activate(editing);
        } else {
            activate(known);
        }
    }

    private void ping(ServerData data) {
        data.setState(ServerData.State.PINGING);
        data.motd = Component.empty();
        data.status = Component.empty();
        PINGERS.submit(() -> {
            try {
                pinger.pingServer(data, () -> minecraft.execute(() -> servers.save()), () -> data.setState(
                                data.protocol == SharedConstants.getCurrentVersion().protocolVersion()
                                        ? ServerData.State.SUCCESSFUL : ServerData.State.INCOMPATIBLE),
                        EventLoopGroupHolder.remote(minecraft.options.useNativeTransport()));
            } catch (UnknownHostException exception) {
                data.setState(ServerData.State.UNREACHABLE);
                data.motd = Component.literal("Can't resolve hostname");
            } catch (Exception exception) {
                data.setState(ServerData.State.UNREACHABLE);
                data.motd = Component.literal("Can't connect to server");
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        pinger.tick();
        List<LanServer> found = lanServers.takeDirtyServers();
        if (found != null) {
            lan.clear();
            for (LanServer server : found) {
                ServerData data = new ServerData(server.getMotd(), server.getAddress(), ServerData.Type.LAN);
                data.motd = Component.literal(server.getAddress());
                lan.add(data);
            }
            ServerData keep = selected;
            reloadEntries();
            if (keep != null && entries.contains(keep)) select(keep);
        }
    }

    @Override
    protected void renderCard(GuiGraphics graphics, ServerData item, int x, int y, int w, int h, double hover,
                              boolean selected, double alpha, long now) {
        if (item.state() == ServerData.State.INITIAL && !item.isLan()) ping(item);
        cardSurface(graphics, x, y, w, h, hover, selected, alpha);
        cardIcon(graphics, icon(item), item.name, x + 8, y + 8, 48, alpha);
        int textX = x + 66;
        int statusW = renderStatus(graphics, item, x + w - 9, y + 10, alpha, now);
        graphics.drawString(font, fit(item.name, w - 74 - statusW - 8), textX, y + 11, JilatonUi.alpha(JilatonUi.TEXT, alpha), false);
        List<FormattedCharSequence> motd = font.split(item.motd, w - 76);
        for (int line = 0; line < Math.min(2, motd.size()); line++) {
            graphics.drawString(font, motd.get(line), textX, y + 25 + line * 11, JilatonUi.alpha(line == 0 ? JilatonUi.TEXT_2 : JilatonUi.TEXT_3, alpha), false);
        }
        if (motd.isEmpty()) graphics.drawString(font, fit(item.ip, w - 76), textX, y + 25, JilatonUi.alpha(JilatonUi.TEXT_4, alpha), false);
        if (item.state() == ServerData.State.SUCCESSFUL && item.players != null) {
            String players = item.players.online() + " / " + item.players.max();
            graphics.drawString(font, players, x + w - font.width(players) - 9, y + h - 15, JilatonUi.alpha(JilatonUi.TEXT_3, alpha), false);
        }
        if (hover > 0.01) {
            String cue = "JOIN ↗";
            int cueX = item.state() == ServerData.State.SUCCESSFUL ? textX : x + w - font.width(cue) - 9;
            graphics.drawString(font, cue, cueX, y + h - 15, JilatonUi.alpha(JilatonUi.TEXT, alpha * hover), false);
        }
    }

    /** Draws latency bars or a status word right-aligned at {@code right}; returns the width used. */
    private int renderStatus(GuiGraphics graphics, ServerData item, int right, int y, double alpha, long now) {
        if (item.isLan()) {
            int w = font.width("LAN") + 8;
            JilatonUi.strokeRounded(graphics, right - w, y - 1, w, 12, 2, JilatonUi.alpha(JilatonUi.TEXT_3, alpha * 0.6));
            graphics.drawString(font, "LAN", right - w + 4, y + 1, JilatonUi.alpha(JilatonUi.TEXT_2, alpha), false);
            return w;
        }
        switch (item.state()) {
            case SUCCESSFUL -> {
                int bars = item.ping < 150 ? 5 : item.ping < 300 ? 4 : item.ping < 600 ? 3 : item.ping < 1000 ? 2 : 1;
                int bx = right - 14;
                for (int i = 0; i < 5; i++) {
                    int bh = 3 + i * 2;
                    graphics.fill(bx + i * 3, y + 10 - bh, bx + i * 3 + 2, y + 10, JilatonUi.alpha(i < bars ? JilatonUi.TEXT : 0xFF2A2A2A, alpha));
                }
                String ms = item.ping + "ms";
                graphics.drawString(font, ms, bx - font.width(ms) - 5, y + 1, JilatonUi.alpha(JilatonUi.TEXT_3, alpha), false);
                return font.width(ms) + 19;
            }
            case INCOMPATIBLE -> {
                String version = fit(item.version.getString(), 80);
                graphics.drawString(font, version, right - font.width(version), y + 1, JilatonUi.alpha(JilatonUi.DANGER, alpha), false);
                return font.width(version);
            }
            case UNREACHABLE -> {
                graphics.drawString(font, "OFFLINE", right - font.width("OFFLINE"), y + 1, JilatonUi.alpha(JilatonUi.DANGER, alpha), false);
                return font.width("OFFLINE");
            }
            default -> {
                int dots = (int) ((now / 250_000_000L) % 4);
                String text = "PINGING" + ".".repeat(dots);
                graphics.drawString(font, text, right - font.width("PINGING..."), y + 1, JilatonUi.alpha(JilatonUi.TEXT_4, alpha), false);
                return font.width("PINGING...");
            }
        }
    }

    private net.minecraft.resources.Identifier icon(ServerData item) {
        byte[] bytes = item.getIconBytes();
        if (bytes == null) return null;
        FaviconTexture texture = icons.get(item);
        if (texture == null || !Arrays.equals(bytes, iconBytes.get(item))) {
            if (texture == null) {
                texture = FaviconTexture.forServer(minecraft.getTextureManager(), item.ip);
                icons.put(item, texture);
            }
            try {
                texture.upload(NativeImage.read(bytes));
                iconBytes.put(item, bytes);
            } catch (Throwable failure) {
                LOGGER.warn("Invalid icon for server {}", item.name, failure);
                item.setIconBytes(null);
                return null;
            }
        }
        return texture.textureLocation();
    }

    @Override
    public void removed() {
        if (lanDetector != null) {
            lanDetector.interrupt();
            lanDetector = null;
        }
        pinger.removeAll();
        icons.values().forEach(FaviconTexture::close);
        icons.clear();
        iconBytes.clear();
    }
}
