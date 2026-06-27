package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.Holder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class TabGroupModel {
    private final List<TabGroup> groups;
    private final Holder<String> selectedGroup;
    private final Set<String> manuallyCollapsedGroups;

    TabGroupModel(List<Tab<?>> tabs, Holder<String> selectedGroup, Set<String> manuallyCollapsedGroups) {
        this.groups = buildGroups(tabs);
        this.selectedGroup = selectedGroup;
        this.manuallyCollapsedGroups = manuallyCollapsedGroups;
    }

    List<TabGroup> groups() {
        return this.groups;
    }

    private static List<TabGroup> buildGroups(List<Tab<?>> tabs) {
        Map<String, TabGroup> tabGroups = new LinkedHashMap<>();
        for (Tab<?> tab : tabs) {
            String id = tab.getModOptions().configId();
            tabGroups.computeIfAbsent(id, ignored -> new TabGroup(id, tab.getModOptions(), new ArrayList<>())).tabs().add(tab);
        }

        return List.copyOf(tabGroups.values());
    }

    int visibleHeight(List<TabGroup> groups) {
        if (groups.isEmpty()) {
            return 0;
        }

        return groups.stream()
                .mapToInt(this::visibleGroupHeight)
                .sum() + this.headerPadding(groups);
    }

    ReeseSodiumOptionsConfig.TabHeaderCollapseMode collapseMode() {
        return ReeseSodiumOptionsConfig.config().getTabHeaderCollapseMode();
    }

    boolean shouldShowHeaders() {
        return this.collapseMode() != ReeseSodiumOptionsConfig.TabHeaderCollapseMode.ALL_EXPANDED || ReeseSodiumOptionsConfig.config().isTabHeaders();
    }

    boolean shouldCollapseSinglePageGroups() {
        return ReeseSodiumOptionsConfig.config().isCollapseSinglePageGroups();
    }

    int headerHeight() {
        return ReeseSodiumOptionsConfig.config().isTabHeaderVersionLabels() ? TabFrame.TAB_HEADER_VERSION_HEIGHT : TabFrame.TAB_HEADER_HEIGHT;
    }

    boolean isSinglePageGroupCollapsed(TabGroup group) {
        return group.tabs().size() == 1 && this.shouldCollapseSinglePageGroups();
    }

    boolean isGroupExpanded(TabGroup group) {
        return switch (this.collapseMode()) {
            case SELECTED_GROUP -> group.id().equals(this.selectedGroup.get());
            case ALL_EXPANDED -> true;
            case MANUAL -> !this.manuallyCollapsedGroups.contains(group.id());
        };
    }

    boolean shouldShowTabs(TabGroup group) {
        return this.isGroupExpanded(group) && !this.isSinglePageGroupCollapsed(group);
    }

    boolean isHeaderSelected(TabGroup group, Optional<Tab<?>> selectedTab) {
        return !this.shouldShowTabs(group) && selectedTab.filter(group.tabs()::contains).isPresent();
    }

    void selectGroup(TabGroup group) {
        this.selectedGroup.set(group.id());
    }

    void selectGroupFor(Optional<Tab<?>> selectedTab) {
        if (this.collapseMode() == ReeseSodiumOptionsConfig.TabHeaderCollapseMode.SELECTED_GROUP) {
            selectedTab.ifPresent(tab -> this.selectedGroup.set(tab.getModOptions().configId()));
        }
    }

    void toggleManualGroup(TabGroup group) {
        if (!this.manuallyCollapsedGroups.remove(group.id())) {
            this.manuallyCollapsedGroups.add(group.id());
        }
    }

    void showRestoredSelectedTab(Tab<?> tab) {
        for (TabGroup group : this.groups()) {
            if (group.id().equals(tab.getModOptions().configId())) {
                switch (this.collapseMode()) {
                    case SELECTED_GROUP -> this.selectedGroup.set(group.id());
                    case MANUAL -> this.manuallyCollapsedGroups.remove(group.id());
                    case ALL_EXPANDED -> {
                    }
                }
            }
        }
    }

    private int visibleGroupHeight(TabGroup group) {
        if (!this.shouldShowHeaders()) {
            return group.tabs().size() * TabFrame.TAB_HEIGHT;
        }

        return this.headerHeight() + (this.shouldShowTabs(group) ? group.tabs().size() * TabFrame.TAB_HEIGHT : 0);
    }

    private int headerPadding(List<TabGroup> groups) {
        return this.shouldShowHeaders() ? (groups.size() - 1) * TabFrame.TAB_HEADER_PADDING : 0;
    }
}
