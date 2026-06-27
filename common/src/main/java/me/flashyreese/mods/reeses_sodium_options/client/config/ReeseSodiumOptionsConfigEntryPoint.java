package me.flashyreese.mods.reeses_sodium_options.client.config;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ReeseSodiumOptionsConfigEntryPoint implements ConfigEntryPoint {
    private static final String MOD_ID = "reeses-sodium-options";
    private static final String KO_FI_URL = "https://ko-fi.com/flashyreese";

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        builder.registerOwnModOptions()
                .setNonTintedIcon(this.modIcon())
                .addPage(this.createOptionsPage(builder));
    }

    private OptionPageBuilder createOptionsPage(ConfigBuilder builder) {
        return builder.createOptionPage()
                .setName(Component.translatable("rso.options.page"))
                .addOptionGroup(this.createGeneralOptions(builder))
                .addOptionGroup(this.createAppearanceOptions(builder))
                .addOptionGroup(this.createBehaviorOptions(builder))
                .addOptionGroup(this.createSupportOptions(builder));
    }

    private OptionGroupBuilder createGeneralOptions(ConfigBuilder builder) {
        return builder.createOptionGroup()
                .setName(Component.translatable("rso.options.group.general"))
                .addOption(builder.createBooleanOption(this.optionId("enabled"))
                        .setName(Component.translatable("rso.options.enabled.name"))
                        .setTooltip(Component.translatable("rso.options.enabled.tooltip"))
                        .setDefaultValue(true)
                        .setBinding(value -> ReeseSodiumOptionsConfig.config().setEnabled(value), () -> ReeseSodiumOptionsConfig.config().isEnabled())
                        .setStorageHandler(ReeseSodiumOptionsConfig.STORAGE_HANDLER)
                        .setApplyHook(ReeseSodiumOptionsConfig::reopenScreen));
    }

    private OptionGroupBuilder createAppearanceOptions(ConfigBuilder builder) {
        return builder.createOptionGroup()
                .setName(Component.translatable("rso.options.group.appearance"))
                .addOption(this.createBooleanOption(
                        builder,
                        "tab_header_icons",
                        value -> ReeseSodiumOptionsConfig.config().setTabHeaderIcons(value),
                        () -> ReeseSodiumOptionsConfig.config().isTabHeaderIcons(),
                        true,
                        true
                ))
                .addOption(this.createBooleanOption(
                        builder,
                        "tab_header_version_labels",
                        value -> ReeseSodiumOptionsConfig.config().setTabHeaderVersionLabels(value),
                        () -> ReeseSodiumOptionsConfig.config().isTabHeaderVersionLabels(),
                        true,
                        true
                ))
                .addOption(builder.createEnumOption(this.optionId("tab_header_collapse_mode"), ReeseSodiumOptionsConfig.TabHeaderCollapseMode.class)
                        .setName(Component.translatable("rso.options.tab_header_collapse_mode.name"))
                        .setTooltip(Component.translatable("rso.options.tab_header_collapse_mode.tooltip"))
                        .setDefaultValue(ReeseSodiumOptionsConfig.DEFAULT_TAB_HEADER_COLLAPSE_MODE)
                        .setElementNameProvider(value -> Component.translatable("rso.options.tab_header_collapse_mode.value." + value.id()))
                        .setBinding(value -> ReeseSodiumOptionsConfig.config().setTabHeaderCollapseMode(value), () -> ReeseSodiumOptionsConfig.config().getTabHeaderCollapseMode())
                        .setStorageHandler(ReeseSodiumOptionsConfig.STORAGE_HANDLER)
                        .setApplyHook(ReeseSodiumOptionsConfig::rebuildCurrentScreen))
                .addOption(this.createBooleanOption(
                        builder,
                        "tab_headers",
                        value -> ReeseSodiumOptionsConfig.config().setTabHeaders(value),
                        () -> ReeseSodiumOptionsConfig.config().isTabHeaders(),
                        true,
                        true
                ))
                .addOption(this.createBooleanOption(
                        builder,
                        "collapse_single_page_groups",
                        value -> ReeseSodiumOptionsConfig.config().setCollapseSinglePageGroups(value),
                        () -> ReeseSodiumOptionsConfig.config().isCollapseSinglePageGroups(),
                        true,
                        true
                ))
                .addOption(this.createBooleanOption(
                        builder,
                        "collapsible_groups",
                        value -> ReeseSodiumOptionsConfig.config().setCollapsibleGroups(value),
                        () -> ReeseSodiumOptionsConfig.config().isCollapsibleGroups(),
                        true,
                        true
                ))
                .addOption(builder.createIntegerOption(this.optionId("tooltip_delay"))
                        .setName(Component.translatable("rso.options.tooltip_delay.name"))
                        .setTooltip(Component.translatable("rso.options.tooltip_delay.tooltip"))
                        .setDefaultValue(ReeseSodiumOptionsConfig.DEFAULT_TOOLTIP_DELAY_MS)
                        .setRange(ReeseSodiumOptionsConfig.MIN_TOOLTIP_DELAY_MS, ReeseSodiumOptionsConfig.MAX_TOOLTIP_DELAY_MS, 100)
                        .setValueFormatter(value -> Component.translatable("rso.options.value.milliseconds", value))
                        .setBinding(value -> ReeseSodiumOptionsConfig.config().setTooltipDelayMs(value), () -> ReeseSodiumOptionsConfig.config().getTooltipDelayMs())
                        .setStorageHandler(ReeseSodiumOptionsConfig.STORAGE_HANDLER))
                .addOption(this.createBooleanOption(
                        builder,
                        "tooltip_option_ids",
                        value -> ReeseSodiumOptionsConfig.config().setTooltipOptionIds(value),
                        () -> ReeseSodiumOptionsConfig.config().isTooltipOptionIds(),
                        false,
                        false
                ))
                .addOption(this.createBooleanOption(
                        builder,
                        "color_themes",
                        value -> ReeseSodiumOptionsConfig.config().setColorThemes(value),
                        () -> ReeseSodiumOptionsConfig.config().isColorThemes(),
                        true,
                        true
                ))
                .addOption(this.createBooleanOption(
                        builder,
                        "themed_headers_and_labels",
                        value -> ReeseSodiumOptionsConfig.config().setThemedHeadersAndLabels(value),
                        () -> ReeseSodiumOptionsConfig.config().isThemedHeadersAndLabels(),
                        true,
                        true
                ))
                .addOption(this.createBooleanOption(
                        builder,
                        "themed_tooltip_borders",
                        value -> ReeseSodiumOptionsConfig.config().setThemedTooltipBorders(value),
                        () -> ReeseSodiumOptionsConfig.config().isThemedTooltipBorders(),
                        true,
                        false
                ))
                .addOption(this.createBooleanOption(
                        builder,
                        "reduced_motion",
                        value -> ReeseSodiumOptionsConfig.config().setReducedMotion(value),
                        () -> ReeseSodiumOptionsConfig.config().isReducedMotion(),
                        false,
                        false
                ));
    }

    private OptionGroupBuilder createBehaviorOptions(ConfigBuilder builder) {
        return builder.createOptionGroup()
                .setName(Component.translatable("rso.options.group.behavior"))
                .addOption(this.createBooleanOption(
                        builder,
                        "reverse_cycling_controls",
                        value -> ReeseSodiumOptionsConfig.config().setReverseCyclingControls(value),
                        () -> ReeseSodiumOptionsConfig.config().isReverseCyclingControls(),
                        true,
                        false
                ))
                .addOption(this.createBooleanOption(
                        builder,
                        "shift_scroll_slider_adjustments",
                        value -> ReeseSodiumOptionsConfig.config().setShiftScrollSliderAdjustments(value),
                        () -> ReeseSodiumOptionsConfig.config().isShiftScrollSliderAdjustments(),
                        true,
                        false
                ))
                .addOption(this.createBooleanOption(
                        builder,
                        "hide_non_matching_options",
                        value -> ReeseSodiumOptionsConfig.config().setHideNonMatchingOptions(value),
                        () -> ReeseSodiumOptionsConfig.config().isHideNonMatchingOptions(),
                        true,
                        true
                ))
                .addOption(this.createBooleanOption(
                        builder,
                        "hide_non_matching_tabs",
                        value -> ReeseSodiumOptionsConfig.config().setHideNonMatchingTabs(value),
                        () -> ReeseSodiumOptionsConfig.config().isHideNonMatchingTabs(),
                        true,
                        true
                ))
                .addOption(builder.createEnumOption(this.optionId("disabled_option_visibility"), ReeseSodiumOptionsConfig.DisabledOptionVisibility.class)
                        .setName(Component.translatable("rso.options.disabled_option_visibility.name"))
                        .setTooltip(Component.translatable("rso.options.disabled_option_visibility.tooltip"))
                        .setDefaultValue(ReeseSodiumOptionsConfig.DEFAULT_DISABLED_OPTION_VISIBILITY)
                        .setElementNameProvider(value -> Component.translatable("rso.options.disabled_option_visibility.value." + value.id()))
                        .setBinding(value -> ReeseSodiumOptionsConfig.config().setDisabledOptionVisibility(value), () -> ReeseSodiumOptionsConfig.config().getDisabledOptionVisibility())
                        .setStorageHandler(ReeseSodiumOptionsConfig.STORAGE_HANDLER)
                        .setApplyHook(ReeseSodiumOptionsConfig::rebuildCurrentScreen))
                .addOption(builder.createEnumOption(this.optionId("focus_border_mode"), ReeseSodiumOptionsConfig.FocusBorderMode.class)
                        .setName(Component.translatable("rso.options.focus_border_mode.name"))
                        .setTooltip(Component.translatable("rso.options.focus_border_mode.tooltip"))
                        .setDefaultValue(ReeseSodiumOptionsConfig.DEFAULT_FOCUS_BORDER_MODE)
                        .setElementNameProvider(value -> Component.translatable("rso.options.focus_border_mode.value." + value.id()))
                        .setBinding(value -> ReeseSodiumOptionsConfig.config().setFocusBorderMode(value), () -> ReeseSodiumOptionsConfig.config().getFocusBorderMode())
                        .setStorageHandler(ReeseSodiumOptionsConfig.STORAGE_HANDLER))
                .addOption(this.createBooleanOption(
                        builder,
                        "controller_guides",
                        value -> ReeseSodiumOptionsConfig.config().setControllerGuides(value),
                        () -> ReeseSodiumOptionsConfig.config().isControllerGuides(),
                        true,
                        false
                ))
                .addOption(this.createBooleanOption(
                        builder,
                        "reset_button_overlay",
                        value -> ReeseSodiumOptionsConfig.config().setResetButtonOverlay(value),
                        () -> ReeseSodiumOptionsConfig.config().isResetButtonOverlay(),
                        true,
                        false
                ))
                .addOption(this.createBooleanOption(
                        builder,
                        "undo_button_overlay",
                        value -> ReeseSodiumOptionsConfig.config().setUndoButtonOverlay(value),
                        () -> ReeseSodiumOptionsConfig.config().isUndoButtonOverlay(),
                        true,
                        false
                ));
    }

    private OptionGroupBuilder createSupportOptions(ConfigBuilder builder) {
        return builder.createOptionGroup()
                .setName(Component.translatable("rso.options.group.support"))
                .addOption(builder.createExternalButtonOption(this.optionId("support_project"))
                        .setName(Component.translatable("rso.options.support_project.name"))
                        .setTooltip(Component.translatable("rso.options.support_project.tooltip"))
                        .setScreenConsumer(screen -> Util.getPlatform().openUri(KO_FI_URL)));
    }

    private OptionBuilder createBooleanOption(ConfigBuilder builder, String name, Consumer<Boolean> setter, Supplier<Boolean> getter, boolean defaultValue, boolean rebuildScreen) {
        var option = builder.createBooleanOption(this.optionId(name))
                .setName(Component.translatable("rso.options." + name + ".name"))
                .setTooltip(Component.translatable("rso.options." + name + ".tooltip"))
                .setDefaultValue(defaultValue)
                .setBinding(setter, getter)
                .setStorageHandler(ReeseSodiumOptionsConfig.STORAGE_HANDLER);

        if (rebuildScreen) {
            option.setApplyHook(ReeseSodiumOptionsConfig::rebuildCurrentScreen);
        }

        return option;
    }

    private ResourceLocation optionId(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private ResourceLocation modIcon() {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, "icon.png");
    }
}
