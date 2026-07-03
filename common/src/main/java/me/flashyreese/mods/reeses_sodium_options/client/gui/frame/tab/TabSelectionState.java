package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.OptionExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option.OptionRow;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.ScrollableFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.Holder;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalPage;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

final class TabSelectionState {
    private final List<Tab<?>> tabs;
    private final Holder<String> persistedTabKey;
    private final Screen screen;
    private final Map<Tab<?>, AbstractFrame> framesByTab = new HashMap<>();
    private Optional<Tab<?>> selectedTab = Optional.empty();
    private @Nullable AbstractFrame selectedFrame;

    TabSelectionState(List<Tab<?>> tabs, Holder<String> persistedTabKey, Screen screen) {
        this.tabs = tabs;
        this.persistedTabKey = persistedTabKey;
        this.screen = screen;
    }

    void restorePersistedTab(Consumer<Tab<?>> restoredTabConsumer) {
        if (this.persistedTabKey.get() == null) {
            return;
        }

        this.selectedTab = this.tabs.stream()
                .filter(tab -> getTabKey(tab).equals(this.persistedTabKey.get()))
                .findAny();
        this.selectedTab.ifPresent(restoredTabConsumer);
    }

    void ensureSelectedTab(Consumer<Tab<?>> selectedTabConsumer) {
        if (this.selectedTab.isPresent() || this.tabs.isEmpty()) {
            return;
        }

        this.selectedTab = Optional.ofNullable(this.tabs.getFirst());
        this.selectedTab.ifPresent(selectedTabConsumer);
    }

    void setSelectedTab(Optional<Tab<?>> tab) {
        this.selectedTab = tab;
    }

    void activateSelectedTab() {
        this.selectedTab.ifPresent(value -> {
            if (value.getPage() instanceof ExternalPage externalPage) {
                externalPage.currentScreenConsumer().accept(this.screen);
            } else {
                this.persistedTabKey.set(getTabKey(value));
            }
        });
    }

    void warmInactiveFrames(LayoutBounds frameSection) {
        this.tabs.stream()
                .filter(tab -> this.selectedTab.filter(value -> value != tab).isPresent())
                .forEach(tab -> this.frameFor(tab, frameSection));
    }

    void refreshFrames(LayoutBounds frameSection) {
        for (Tab<?> tab : this.tabs) {
            AbstractFrame frame = this.frameFor(tab, frameSection);
            if (frame != null) {
                frame.rebuildFrameContent();
            }
        }
    }

    void rebuildSelectedFrame(LayoutBounds frameSection, List<GuiEventListener> children) {
        if (this.selectedTab.isEmpty()) {
            this.selectedFrame = null;
            return;
        }

        AbstractFrame frame = this.frameFor(this.selectedTab.get(), frameSection);
        if (frame != null) {
            this.selectedFrame = frame;
            frame.buildFrame();
            children.add(frame);
        }
    }

    Optional<Tab<?>> selectedTab() {
        return this.selectedTab;
    }

    @Nullable
    AbstractFrame selectedFrame() {
        return this.selectedFrame;
    }

    Optional<String> selectedTabKey() {
        return this.selectedTab.map(TabSelectionState::getTabKey);
    }

    @Nullable
    OptionRow findSelectedOptionRow(Identifier optionId) {
        if (this.selectedFrame == null) {
            return null;
        }

        return this.selectedFrame.findFirstOptionRow(control -> control.getOption() instanceof OptionExtended optionExtended
                && optionExtended.rso$getId().equals(optionId));
    }

    @Nullable
    OptionRow findFirstSelectedOptionRow() {
        if (this.selectedFrame instanceof ScrollableFrame scrollableFrame) {
            return scrollableFrame.findFirstOptionRow();
        }

        return this.selectedFrame == null ? null : this.selectedFrame.findFirstOptionRow(control -> true);
    }

    @Nullable
    OptionRow findLastSelectedOptionRow() {
        if (this.selectedFrame instanceof ScrollableFrame scrollableFrame) {
            return scrollableFrame.findLastOptionRow();
        }

        return this.selectedFrame == null ? null : this.selectedFrame.findLastOptionRow(control -> true);
    }

    @Nullable
    OptionRow findFirstVisibleSelectedOptionRow() {
        if (this.selectedFrame instanceof ScrollableFrame scrollableFrame) {
            return scrollableFrame.findFirstVisibleOptionRow();
        }

        return this.findFirstSelectedOptionRow();
    }

    @Nullable
    OptionRow findLastVisibleSelectedOptionRow() {
        if (this.selectedFrame instanceof ScrollableFrame scrollableFrame) {
            return scrollableFrame.findLastVisibleOptionRow();
        }

        return this.findLastSelectedOptionRow();
    }

    boolean scrollSelectedPageToStart() {
        return this.selectedFrame instanceof ScrollableFrame scrollableFrame && scrollableFrame.scrollToStart();
    }

    boolean scrollSelectedPageToEnd() {
        return this.selectedFrame instanceof ScrollableFrame scrollableFrame && scrollableFrame.scrollToEnd();
    }

    boolean scrollSelectedPage(int direction) {
        return this.selectedFrame instanceof ScrollableFrame scrollableFrame && scrollableFrame.scrollPage(direction);
    }

    private static String getTabKey(Tab<?> tab) {
        return tab.key();
    }

    private @Nullable AbstractFrame frameFor(Tab<?> tab, LayoutBounds frameSection) {
        AbstractFrame frame = this.framesByTab.get(tab);
        if (frame == null) {
            frame = tab.getFrameFunction().apply(frameSection);
            if (frame != null) {
                this.framesByTab.put(tab, frame);
            }
        }

        return frame;
    }
}
