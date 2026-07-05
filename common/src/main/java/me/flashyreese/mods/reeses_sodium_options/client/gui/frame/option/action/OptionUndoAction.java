package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option.action;

import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public final class OptionUndoAction {
    static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("reeses-sodium-options", "textures/gui/undo_to_unmodified.png");

    public static boolean isVisible(StatefulOption<?> option) {
        return ReeseSodiumOptionsConfig.config().isUndoButtonOverlay()
                && (ReeseSodiumOptionsConfig.config().isAlwaysShowActionButtons() || canUndo(option));
    }

    public static boolean isActive(StatefulOption<?> option) {
        return ReeseSodiumOptionsConfig.config().isUndoButtonOverlay()
                && canUndo(option);
    }

    public static boolean canUndo(StatefulOption<?> option) {
        return option.isEnabled()
                && option.hasChanged()
                && !Objects.equals(option.getValidatedValue(), option.getAppliedValue());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void undoChanges(StatefulOption<?> option) {
        ((StatefulOption) option).modifyValue(option.getAppliedValue());
    }

    public static void normalizeEquivalentChange(StatefulOption<?> option) {
        if (option.hasChanged() && Objects.equals(option.getValidatedValue(), option.getAppliedValue())) {
            undoChanges(option);
        }
    }

}
