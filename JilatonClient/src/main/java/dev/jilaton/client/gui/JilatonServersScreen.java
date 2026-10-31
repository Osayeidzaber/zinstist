package dev.jilaton.client.gui;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
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
import org.slf4j.Logger;
import dev.jilaton.client.gui.design.Ui;

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
        return segment() == 0 ? "Shift + ↑ ↓ to reorder  ·  Double-click to join" : "Games open to LAN on this network";
    }

    @Override
    protected Component emptyMessage() {
        return Component.literal(segment() == 0 ? "No saved servers. Add one or connect directly."
                : "No LAN games found yet. Open a world to LAN on this network.");
    }

    @Override
    protected List<String> segments() { return List.of("Saved", "LAN"); }

    @Override
    protected boolean inSegment(ServerData item, int segment) { return item.isLan() == (segment == 1); }

    @Override
    protected String rowAction() { return "Join"; }

    @Override
    protected List<Action> actions() {
        return List.of(
                new Action("Add server", false, always(), () -> {
                    editing = new ServerData("", "", ServerData.Type.OTHER);
                    minecraft.setScreen(new ManageServerScreen(this, Component.translatable("manageServer.add.title"), this::added, editing));
                }),
                new Action("Direct connect", false, always(), () -> {
                    editing = new ServerData(I18n.get("selectServer.defaultName"), "", ServerData.Type.OTHER);
                    minecraft.setScreen(new DirectJoinServerScreen(this, this::directJoined, editing));
                }),
                new Action("Edit", false, () -> selected != null && !selected.isLan(), this::editSelected),
                new Action("Delete", false, () -> selected != null && !selected.isLan(), this::confirmDelete),
                new Action("Refresh", false, always(), () -> minecraft.setScreen(new JilatonServersScreen(parent))));
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
    protected void renderCard(GuiGraphics graphics, ServerData item, float x, float y, float w, float h, double hover,
                              boolean selected, double alpha, long now) {
        if (item.state() == ServerData.State.INITIAL && !item.isLan()) ping(item);
        cardIcon(graphics, icon(item), item.name, x + 14, y + 14, 48, alpha);
        float textX = x + 76;
        float statusW = renderStatus(graphics, item, x + w - 20, y + 30, alpha, now);
        float nameMax = Math.max(60, w - 96 - statusW - 24);
        String name = Ui.fit(item.name, nameMax * 0.6F, 15, JilatonType.Face.SEMIBOLD);
        Ui.text(graphics, name, textX, y + 17, 15, JilatonType.Face.SEMIBOLD, Ui.alpha(Ui.TEXT, alpha));
        float nameW = Ui.width(name, 15, JilatonType.Face.SEMIBOLD);
        if (!item.isLan())
            Ui.text(graphics, Ui.fit(item.ip, nameMax - nameW - 12, 11, JilatonType.Face.MONO), textX + nameW + 10, y + 20, 11,
                    JilatonType.Face.MONO, Ui.alpha(Ui.TEXT_3, alpha));
        String motd = item.motd == null ? "" : item.motd.getString().replace('\n', ' ').strip();
        if (motd.isEmpty()) motd = item.isLan() ? item.ip : "";
        Ui.text(graphics, Ui.fit(motd, nameMax, 12, JilatonType.Face.REGULAR), textX, y + 43, 12, JilatonType.Face.REGULAR,
                Ui.alpha(Ui.TEXT_2, alpha));
    }

    /** Players, ping meter and ms right-aligned at {@code right}, centred on {@code cy}; returns the width used. */
    private float renderStatus(GuiGraphics graphics, ServerData item, float right, float cy, double alpha, long now) {
        if (item.isLan()) {
            float w = Ui.spacedWidth("LAN", 10, 0.8F, JilatonType.Face.MEDIUM) + 16;
            Ui.outline(graphics, right - w, cy - 4, w, 20, 6, Ui.alpha(Ui.STROKE, alpha));
            Ui.spaced(graphics, "LAN", right - w + 8, cy + 1, 10, 0.8F, JilatonType.Face.MEDIUM, Ui.alpha(Ui.TEXT_2, alpha));
            return w;
        }
        switch (item.state()) {
            case SUCCESSFUL -> {
                int bars = item.ping < 150 ? 4 : item.ping < 300 ? 3 : item.ping < 600 ? 2 : 1;
                int on = bars >= 3 ? Ui.ONLINE : Ui.WARN;
                float bx = right - 22;
                for (int i = 0; i < 4; i++) {
                    float bh = 5 + i * 3;
                    Ui.round(graphics, bx + i * 6, cy + 12 - bh, 4, bh, 1, Ui.alpha(i < bars ? on : Ui.FAINT, alpha));
                }
                String ms = item.ping + " ms";
                float msW = Ui.width(ms, 11, JilatonType.Face.MONO);
                Ui.text(graphics, ms, bx - 10 - msW, cy + 2, 11, JilatonType.Face.MONO, Ui.alpha(Ui.TEXT_3, alpha));
                float used = 22 + 10 + msW;
                if (item.players != null) {
                    String players = item.players.online() + " / " + item.players.max();
                    float pw = Ui.width(players, 12, JilatonType.Face.MONO);
                    Ui.text(graphics, players, right - used - 22 - pw, cy + 1, 12, JilatonType.Face.MONO, Ui.alpha(Ui.SOFT, alpha));
                    used += 22 + pw;
                }
                return used;
            }
            case INCOMPATIBLE -> {
                String version = Ui.fit(item.version.getString(), 140, 12, JilatonType.Face.MEDIUM);
                float vw = Ui.width(version, 12, JilatonType.Face.MEDIUM);
                Ui.text(graphics, version, right - vw, cy + 1, 12, JilatonType.Face.MEDIUM, Ui.alpha(Ui.DANGER, alpha));
                return vw;
            }
            case UNREACHABLE -> {
                float vw = Ui.width("Offline", 12, JilatonType.Face.MEDIUM);
                Ui.text(graphics, "Offline", right - vw, cy + 1, 12, JilatonType.Face.MEDIUM, Ui.alpha(Ui.DANGER, alpha));
                return vw;
            }
            default -> {
                // Pinging: three dots breathing in turn.
                for (int i = 0; i < 3; i++) {
                    double phase = ((now / 1e9) * 1.6 - i * 0.18) % 1.0;
                    double a = 0.25 + 0.75 * Math.max(0, Math.sin(Math.PI * phase));
                    Ui.circle(graphics, right - 26 + i * 10, cy + 6, 2.5F, Ui.alpha(Ui.TEXT_2, alpha * a));
                }
                return 30;
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
