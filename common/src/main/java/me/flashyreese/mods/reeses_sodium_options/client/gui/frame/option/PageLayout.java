package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option;

import me.flashyreese.mods.reeses_sodium_options.client.gui.option.OptionExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.SearchResultOrder;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.SearchResultOrdering;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.OptionGroup;
import net.caffeinemc.mods.sodium.client.config.structure.Page;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class PageLayout {
    static final int ROW_HEIGHT = 18;

    private static final int GROUP_PADDING = 4;

    private final List<Row> rows;
    private final int contentHeight;

    private PageLayout(List<Row> rows, int contentHeight) {
        this.rows = List.copyOf(rows);
        this.contentHeight = contentHeight;
    }

    static PageLayout create(Page page, List<ResourceLocation> resultIds, SearchResultOrder resultOrder, boolean collapsible, Set<ResourceLocation> collapsedGroups) {
        List<SearchEntry> searchEntries = buildSearchEntries(page, resultIds, resultOrder);

        if (!searchEntries.isEmpty()) {
            return createSearchLayout(searchEntries);
        }

        return createPageLayout(page, collapsible, collapsedGroups);
    }

    List<Row> rows() {
        return this.rows;
    }

    int contentHeight() {
        return this.contentHeight;
    }

    private static PageLayout createSearchLayout(List<SearchEntry> searchEntries) {
        List<Row> rows = new ArrayList<>();
        int y = 0;
        OptionGroup lastGroup = null;

        for (SearchEntry entry : searchEntries) {
            OptionGroup group = entry.group();

            if (group != lastGroup) {
                if (lastGroup != null) {
                    y += GROUP_PADDING;
                }

                if (hasLabel(group)) {
                    int labelY = y + GROUP_PADDING;
                    rows.add(new LabelRow(group, group.name(), labelY, null, false));
                    y = labelY + ROW_HEIGHT;
                }

                lastGroup = group;
            }

            rows.add(new OptionRow(group, entry.option(), y));
            y += ROW_HEIGHT;
        }

        y += GROUP_PADDING;

        return new PageLayout(rows, y);
    }

    private static PageLayout createPageLayout(Page page, boolean collapsible, Set<ResourceLocation> collapsedGroups) {
        List<Row> rows = new ArrayList<>();
        List<OptionGroup> groups = page.groups();
        int y = 0;

        for (int i = 0; i < groups.size(); i++) {
            OptionGroup group = groups.get(i);

            ResourceLocation collapseKey = collapsible ? groupCollapseKey(group) : null;
            boolean collapsed = collapseKey != null && collapsedGroups.contains(collapseKey);

            if (hasLabel(group)) {
                int labelY = y + (i == 0 ? 0 : GROUP_PADDING);
                rows.add(new LabelRow(group, group.name(), labelY, collapseKey, collapsed));
                y = labelY + ROW_HEIGHT;
            }

            if (!collapsed) {
                for (Option option : group.options()) {
                    rows.add(new OptionRow(group, option, y));
                    y += ROW_HEIGHT;
                }
            }

            if (i < groups.size() - 1) {
                y += GROUP_PADDING;
            }
        }

        return new PageLayout(rows, y);
    }

    private static @Nullable ResourceLocation groupCollapseKey(OptionGroup group) {
        for (Option option : group.options()) {
            if (option instanceof OptionExtended optionExtended) {
                return optionExtended.rso$getId();
            }
        }

        return null;
    }

    private static boolean hasLabel(OptionGroup group) {
        return group.name() != null && !group.name().getString().isEmpty();
    }

    private static List<SearchEntry> buildSearchEntries(Page page, List<ResourceLocation> resultIds, SearchResultOrder resultOrder) {
        if (resultIds.isEmpty()) {
            return List.of();
        }

        List<SearchEntry> entries = new ArrayList<>();
        for (OptionGroup group : page.groups()) {
            for (Option option : group.options()) {
                if (option instanceof OptionExtended optionExtended) {
                    entries.add(new SearchEntry(group, option, optionExtended.rso$getId()));
                }
            }
        }

        return SearchResultOrdering.order(resultOrder, resultIds, entries, SearchEntry::id);
    }

    interface Row {
        int y();
    }

    record LabelRow(OptionGroup group, Component text, int y, @Nullable ResourceLocation collapseKey, boolean collapsed) implements Row {
        boolean collapsible() {
            return this.collapseKey != null;
        }
    }

    record OptionRow(OptionGroup group, Option option, int y) implements Row {
    }

    private record SearchEntry(OptionGroup group, Option option, ResourceLocation id) {
    }
}
