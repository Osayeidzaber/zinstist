package dev.jilaton.client.gui.shell;

import dev.jilaton.client.module.ClientModule;

import java.util.ArrayList;
import java.util.List;

/**
 * Flows the module cards into columns and breaks the grid whenever a module is expanded, so its
 * settings can use the full content width. The same instance drives drawing and hit-testing, which
 * keeps the two from drifting apart.
 */
public final class ModuleFlow {
    public record Card(ClientModule module, int x, int y, int width, int height) { }

    public record Panel(ClientModule module, int x, int y, int width, int height, int settingsTop) { }

    private final List<Card> cards;
    private final List<Panel> panels;
    private final int height;

    private ModuleFlow(List<Card> cards, List<Panel> panels, int height) {
        this.cards = List.copyOf(cards);
        this.panels = List.copyOf(panels);
        this.height = height;
    }

    public static ModuleFlow layout(List<ClientModule> modules, String expandedId, int columns,
                                    int cardWidth, int cardHeight, int contentWidth,
                                    int columnGap, int rowGap, int expansionHeader, int settingRowHeight) {
        List<Card> cards = new ArrayList<>();
        List<Panel> panels = new ArrayList<>();
        int column = 0;
        int y = 0;
        int maxY = 0;
        for (ClientModule module : modules) {
            cards.add(new Card(module, column * (cardWidth + columnGap), y, cardWidth, cardHeight));
            maxY = Math.max(maxY, y + cardHeight);
            column++;
            boolean expanded = module.id().equals(expandedId) && !module.settings().isEmpty();
            if (expanded) {
                y += cardHeight + rowGap;
                column = 0;
                int panelHeight = expansionHeader + module.settings().size() * settingRowHeight + 10;
                panels.add(new Panel(module, 0, y, contentWidth, panelHeight, y + expansionHeader));
                maxY = Math.max(maxY, y + panelHeight);
                y += panelHeight + rowGap;
            } else if (column >= columns) {
                column = 0;
                y += cardHeight + rowGap;
            }
        }
        return new ModuleFlow(cards, panels, maxY);
    }

    public List<Card> cards() {
        return cards;
    }

    public List<Panel> panels() {
        return panels;
    }

    public int height() {
        return height;
    }

    /** Card under the mouse, or null. Coordinates are content-relative (scroll already applied). */
    public Card cardAt(double x, double y) {
        for (Card card : cards) {
            if (x >= card.x() && x < card.x() + card.width() && y >= card.y() && y < card.y() + card.height()) {
                return card;
            }
        }
        return null;
    }

    public Panel panelAt(double x, double y) {
        for (Panel panel : panels) {
            if (x >= panel.x() && x < panel.x() + panel.width() && y >= panel.y() && y < panel.y() + panel.height()) {
                return panel;
            }
        }
        return null;
    }

    public Card cardOf(ClientModule module) {
        for (Card card : cards) {
            if (card.module() == module) return card;
        }
        return null;
    }
}
