package me.flashyreese.mods.reeses_sodium_options.client.gui.state;

import org.jetbrains.annotations.Nullable;

public final class Holder<T> {
    private @Nullable T value;

    public Holder(@Nullable T value) {
        this.value = value;
    }

    public @Nullable T get() {
        return this.value;
    }

    public T getOrDefault(T fallback) {
        return this.value != null ? this.value : fallback;
    }

    public void set(@Nullable T value) {
        this.value = value;
    }
}
