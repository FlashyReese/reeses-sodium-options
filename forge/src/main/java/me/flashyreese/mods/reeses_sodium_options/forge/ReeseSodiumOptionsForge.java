package me.flashyreese.mods.reeses_sodium_options.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Empty mod entry point.
 *
 * <p>NeoForge accepts a {@code javafml} mod without an {@code @Mod} class, so the neoforge module has
 * none. MinecraftForge refuses such a jar ("constructed 0 mods"), hence this class. Everything else
 * goes through the mixins and the {@code sodium:config_api_user} entry point declared in mods.toml.
 */
@Mod("reeses_sodium_options")
public class ReeseSodiumOptionsForge {
    public ReeseSodiumOptionsForge(FMLJavaModLoadingContext context) {
    }
}
