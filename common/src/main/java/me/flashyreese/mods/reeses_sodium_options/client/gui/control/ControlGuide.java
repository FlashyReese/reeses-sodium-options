package me.flashyreese.mods.reeses_sodium_options.client.gui.control;

import net.minecraft.network.chat.Component;

public record ControlGuide(Input input, Component label) {
    public static ControlGuide press(String label) {
        return new ControlGuide(Input.PRESS, Component.literal(label));
    }

    public static ControlGuide navigationLeftRight(String label) {
        return new ControlGuide(Input.NAVIGATION_LEFT_RIGHT, Component.literal(label));
    }

    public static ControlGuide previousTab(String label) {
        return new ControlGuide(Input.PREVIOUS_TAB, Component.literal(label));
    }

    public static ControlGuide nextTab(String label) {
        return new ControlGuide(Input.NEXT_TAB, Component.literal(label));
    }

    public enum Input {
        PRESS,
        NAVIGATION_LEFT_RIGHT,
        PREVIOUS_TAB,
        NEXT_TAB
    }
}
