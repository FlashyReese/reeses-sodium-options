package me.flashyreese.mods.reeses_sodium_options.compat;

import net.minecraft.client.gui.screen.Screen;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;

public class IrisCompat {
    private static boolean irisPresent;
    private static MethodHandle handleScreen;
    private static MethodHandle handleTranslationKey;
    private static Object apiInstance;

    static {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            apiInstance = api.cast(api.getDeclaredMethod("getInstance").invoke(null));
            handleScreen = MethodHandles.lookup().findVirtual(api, "openMainIrisScreenObj", MethodType.methodType(Object.class, Object.class));
            handleTranslationKey = MethodHandles.lookup().findVirtual(api, "getMainScreenLanguageKey", MethodType.methodType(String.class));
            irisPresent = true;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            irisPresent = false;
        }
    }

    public static Screen getIrisShaderPacksScreen(Screen parent) {
        if (irisPresent) {
            try {
                return (Screen) handleScreen.invokeWithArguments(apiInstance, parent);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        return null;
    }

    public static String getIrisShaderPacksScreenLanguageKey() {
        if (irisPresent) {
            try {
                return (String) handleTranslationKey.invoke(apiInstance);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        return null;
    }

    public static boolean isIrisPresent() {
        return irisPresent;
    }
}