package me.flashyreese.mods.reeses_sodium_options.client.gui.frame;

import me.flashyreese.mods.reeses_sodium_options.client.gui.*;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.LabelComponent;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.OptionGroup;
import net.caffeinemc.mods.sodium.client.config.structure.Page;
import net.caffeinemc.mods.sodium.client.gui.options.control.Control;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PageFrame extends AbstractFrame {
    protected final Dim2i originalDim;
    protected final Page page;
    private long lastTime = 0;
    private ControlElement lastHoveredElement = null;

    public PageFrame(Screen screen, Dim2i dim, boolean renderOutline, Page page, ModOptions modOptions) {
        super(dim, screen, renderOutline, modOptions);
        this.originalDim = new Dim2i(dim.x(), dim.y(), dim.width(), dim.height());
        this.page = page;
        this.setupFrame();
        this.buildFrame();
    }

    public static Builder builder() {
        return new Builder();
    }


    public void setupFrame() {
        this.children.clear();
        this.controlElements.clear();

        int y = 0;
        List<SearchEntry> searchEntries = this.buildSearchEntries();
        if (!searchEntries.isEmpty()) {
            OptionGroup lastGroup = null;
            for (SearchEntry entry : searchEntries) {
                OptionGroup group = entry.group();
                if (group != lastGroup) {
                    if (lastGroup != null) {
                        y += 4;
                    }
                    if (group.name() != null && !group.name().getString().isEmpty()) {
                        y += 18;
                    }
                    lastGroup = group;
                }
                y += 18;
            }
            y += 4;
        } else if (!this.page.groups().isEmpty()) {
            OptionGroup lastGroup = this.page.groups().get(this.page.groups().size() - 1);

            for (OptionGroup group : this.page.groups()) {
                if (group.name() != null && !group.name().getString().isEmpty()) {
                    y += 18;
                }

                y += group.options().size() * 18;
                if (group != lastGroup) {
                    y += 4;
                }
            }
        }

        ((Dim2iAccess) ((Object) ((AbstractWidgetExtended) this).getDim())).setHeight(y);
        this.page.groups().forEach(group -> group.options().forEach(option -> {
            if (option instanceof OptionExtended optionExtended) {
                optionExtended.setParentDimension(((AbstractWidgetExtended) this).getDim());
            }
        }));
    }

    @Override

    public void buildFrame() {

        if (this.page == null) return;



        this.children.clear();

        this.controlElements.clear();

        int y = 0;
        List<SearchEntry> searchEntries = this.buildSearchEntries();
        if (!searchEntries.isEmpty()) {
            OptionGroup lastGroup = null;
            for (SearchEntry entry : searchEntries) {
                OptionGroup group = entry.group();
                if (group != lastGroup) {
                    if (lastGroup != null) {
                        y += 4;
                    }
                    if (group.name() != null && !group.name().getString().isEmpty()) {
                        Dim2i dim = new Dim2i(0, y + 4, this.getWidth(), 18);
                        ((Dim2iAccess) (Object) dim).setPoint2i(((Point2iAccess) (Object) ((AbstractWidgetExtended) this).getDim()));
                        this.children.add(new LabelComponent(dim, group.name(), 0xFFFFFFFF));
                        y += 18;
                    }
                    lastGroup = group;
                }

                Control control = entry.option().getControl();
                Dim2i dim = new Dim2i(0, y, this.getWidth(), 18);
                ((Dim2iAccess) (Object) dim).setPoint2i(((Point2iAccess) (Object) ((AbstractWidgetExtended) this).getDim()));
                ControlElement element = control.createElement(this.screen, this, dim, this.modOptions.theme());
                ((OptionExtended) element.getOption()).setDim2i(dim);
                this.children.add(element);

                y += 18;
            }
            y += 4;
        } else {
            for (OptionGroup group : this.page.groups()) {
                if (group.name() != null && !group.name().getString().isEmpty()) {
                    Dim2i dim = new Dim2i(0, y + 4, this.getWidth(), 18);
                    ((Dim2iAccess) (Object) dim).setPoint2i(((Point2iAccess) (Object) ((AbstractWidgetExtended) this).getDim()));
                    this.children.add(new LabelComponent(dim, group.name(), 0xFFFFFFFF));

                    y += 18;
                }

                // Add each option's control element
                for (Option option : group.options()) {
                    Control control = option.getControl();
                    Dim2i dim = new Dim2i(0, y, this.getWidth(), 18);
                    ((Dim2iAccess) (Object) dim).setPoint2i(((Point2iAccess) (Object) ((AbstractWidgetExtended) this).getDim()));
                    ControlElement element = control.createElement(this.screen, this, dim, this.modOptions.theme());
                    ((OptionExtended) element.getOption()).setDim2i(dim);
                    this.children.add(element);

                    // Move down to the next option
                    y += 18;
                }

                // Add padding beneath each option group
                y += 4;
            }
        }


        super.buildFrame();

    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        ControlElement hoveredElement = this.controlElements.stream()
                .filter(controlElement -> ((Dim2iAccess) (Object) ((AbstractWidgetExtended) controlElement).getDim()).overlapWith(this.originalDim))
                .filter(ControlElement::isHovered)
                .findFirst()
                .orElse(this.controlElements.stream()
                        .filter(controlElement -> ((Dim2iAccess) (Object) ((AbstractWidgetExtended) controlElement).getDim()).overlapWith(this.originalDim))
                        .filter(ControlElement::isFocused)
                        .findFirst()
                        .orElse(null));
        super.render(guiGraphics, mouseX, mouseY, delta);
        if (hoveredElement != null && this.lastHoveredElement == hoveredElement &&
                ((this.originalDim.containsCursor(mouseX, mouseY) && hoveredElement.isHovered() && hoveredElement.isMouseOver(mouseX, mouseY))
                        || hoveredElement.isFocused())) {
            if (this.lastTime == 0) {
                this.lastTime = System.currentTimeMillis();
            }
            this.renderOptionTooltip(guiGraphics, hoveredElement);
        } else {
            this.lastTime = 0;
            this.lastHoveredElement = hoveredElement;
        }
    }

    private void renderOptionTooltip(GuiGraphics guiGraphics, ControlElement element) {
        if (this.lastTime + 500 > System.currentTimeMillis()) return;

        Dim2i dim = ((AbstractWidgetExtended) element).getDim();

        int textPadding = 3;
        int boxPadding = 3;

        int boxWidth = dim.width();

        //Offset based on mouse position, width and height of content and width and height of the window
        int boxY = dim.getLimitY();
        int boxX = dim.x();

        Option option = element.getOption();
        List<FormattedCharSequence> tooltip = new ArrayList<>();

        tooltip.add(Language.getInstance().getVisualOrder(Component.literal(((OptionExtended) option).getId().toString()).withStyle(ChatFormatting.GRAY)));
        tooltip.add(Language.getInstance().getVisualOrder(Component.literal("")));

        tooltip.addAll(Minecraft.getInstance().font.split(option.getTooltip(), boxWidth - (textPadding * 2)));

        OptionImpact impact = option.getImpact();

        if (impact != null) {
            tooltip.add(Language.getInstance().getVisualOrder(Component.translatable("sodium.options.performance_impact_string", impact.getName()).withStyle(ChatFormatting.GRAY)));
        }

        int boxHeight = (tooltip.size() * 12) + boxPadding;
        int boxYLimit = boxY + boxHeight;
        int boxYCutoff = this.originalDim.getLimitY();

        // If the box is going to be cutoff on the Y-axis, move it back up the difference
        if (boxYLimit > boxYCutoff) {
            boxY -= boxHeight + dim.height();
        }

        if (boxY < 0) {
            boxY = dim.getLimitY();
        }

        if (tooltip.isEmpty())
            return;

        this.drawRect(guiGraphics, boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xE0000000);
        this.drawBorder(guiGraphics, boxX, boxY, boxX + boxWidth, boxY + boxHeight, modOptions.theme().theme);

        for (int i = 0; i < tooltip.size(); i++) {
            guiGraphics.drawString(Minecraft.getInstance().font, tooltip.get(i), boxX + textPadding, boxY + textPadding + (i * 12), 0xFFFFFFFF, true);
        }
    }

    private List<SearchEntry> buildSearchEntries() {
        List<ResourceLocation> resultIds = SodiumVideoOptionsScreen.sharedUiState().searchResultIds();
        if (resultIds.isEmpty()) {
            return List.of();
        }
        Map<ResourceLocation, SearchEntry> entries = new HashMap<>();
        for (OptionGroup group : this.page.groups()) {
            for (Option option : group.options()) {
                if (option instanceof OptionExtended optionExtended) {
                    entries.put(optionExtended.getId(), new SearchEntry(group, option));
                }
            }
        }
        List<SearchEntry> ordered = new ArrayList<>(resultIds.size());
        for (ResourceLocation id : resultIds) {
            SearchEntry entry = entries.get(id);
            if (entry != null) {
                ordered.add(entry);
            }
        }
        return ordered;
    }

    private record SearchEntry(OptionGroup group, Option option) {
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NotNull FocusNavigationEvent navigation) {
        return super.nextFocusPath(navigation);
    }

    public static class Builder {
        private Dim2i dim;
        private boolean renderOutline;
        private Page page;
        private Screen screen;
        private ModOptions modOptions;

        public Builder withDimension(Dim2i dim) {
            this.dim = dim;
            return this;
        }

        public Builder withRenderOutline(boolean renderOutline) {
            this.renderOutline = renderOutline;
            return this;
        }

        public Builder withPage(Page page) {
            this.page = page;
            return this;
        }

        public Builder withScreen(Screen screen) {
            this.screen = screen;
            return this;
        }

        public Builder withModOptions(ModOptions modConfig) {
            this.modOptions = modConfig;
            return this;
        }

        public PageFrame build() {
            Validate.notNull(this.dim, "Dimension must be specified");
            Validate.notNull(this.page, "Option Page must be specified");
            Validate.notNull(this.screen, "Screen must be specified");
            Validate.notNull(this.modOptions, "Mod Options must be specified");

            return new PageFrame(this.screen, this.dim, this.renderOutline, this.page, this.modOptions);
        }
    }
}
