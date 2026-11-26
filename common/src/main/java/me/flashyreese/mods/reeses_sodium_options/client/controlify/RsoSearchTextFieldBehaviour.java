package me.flashyreese.mods.reeses_sodium_options.client.controlify;

import dev.isxander.controlify.api.gui.keyboard.InputTarget;
import dev.isxander.controlify.api.gui.keyboard.KeyboardLayouts;
import dev.isxander.controlify.api.gui.keyboard.KeyboardPositioner;
import dev.isxander.controlify.api.screenop.ElementBehaviour;
import dev.isxander.controlify.api.screenop.ElementKeyboardBehaviour;
import dev.isxander.controlify.api.screenop.ScreenBehaviour;
import dev.isxander.controlify.api.screenop.ScreenContext;
import me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumVideoOptionsScreen;
import me.flashyreese.mods.reeses_sodium_options.client.gui.search.SearchTextFieldWidget;
import net.minecraft.client.gui.ComponentPath;

import java.util.Optional;

record RsoSearchTextFieldBehaviour(SearchTextFieldWidget textField) implements ElementBehaviour {
    static ScreenBehaviour retainKeyboardFocus(ScreenContext<? extends SodiumVideoOptionsScreen> context) {
        return new ScreenBehaviour() {
            @Override
            public void update(float delta, Runnable next) {
                var screen = context.screen();
                ComponentPath searchFocus = null;
                if (!context.inputMode().isController() && screen.rso$getFocusedLeaf() instanceof SearchTextFieldWidget) {
                    searchFocus = screen.getCurrentFocusPath();
                }

                next.run();

                if (searchFocus != null && screen.getFocused() == null) {
                    searchFocus.applyFocus(true);
                }
            }
        };
    }

    @Override
    public Optional<ElementKeyboardBehaviour> keyboardBehaviour(ScreenContext<?> context) {
        int keyboardWidth = (int) (context.screen().width * 0.8F);
        int keyboardHeight = (int) (context.screen().height * 0.4F);

        return Optional.of(new ElementKeyboardBehaviour.Handled(
                KeyboardLayouts.SIMPLE,
                new TextFieldInputTarget(this.textField),
                KeyboardPositioner.aboveOrBelowWidgetPositioner(
                        keyboardWidth,
                        keyboardHeight,
                        1,
                        this.textField::getRectangle
                )
        ));
    }

    private record TextFieldInputTarget(SearchTextFieldWidget textField) implements InputTarget {
        @Override
        public boolean supportsCharInput() {
            return true;
        }

        @Override
        public boolean acceptChar(char ch, int modifiers) {
            return this.textField.rso$acceptChar(ch, modifiers);
        }

        @Override
        public boolean supportsKeyCodeInput() {
            return true;
        }

        @Override
        public boolean acceptKeyCode(int keycode, int scancode, int modifiers) {
            return this.textField.rso$acceptKeyCode(keycode, scancode, modifiers);
        }

        @Override
        public boolean supportsCursorMovement() {
            return true;
        }

        @Override
        public boolean moveCursor(int amount) {
            return this.textField.rso$moveCursor(amount);
        }

        @Override
        public boolean supportsCopying() {
            return true;
        }

        @Override
        public boolean copy() {
            return this.textField.rso$copyText();
        }
    }
}
