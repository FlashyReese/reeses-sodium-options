package me.flashyreese.mods.reeses_sodium_options.client.controlify;

import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.action.ActionAccessor;
import dev.isxander.controlify.api.action.BuiltinActions;
import dev.isxander.controlify.api.controller.Controller;
import dev.isxander.controlify.api.gui.font.ActionGlyphs;
import dev.isxander.controlify.api.screenop.ScreenBehaviour;
import dev.isxander.controlify.api.screenop.ScreenContext;
import dev.isxander.controlify.impl.controller.ControllerImpl;
import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumVideoOptionsScreen;
import me.flashyreese.mods.reeses_sodium_options.client.gui.control.ControlGuide;
import me.flashyreese.mods.reeses_sodium_options.client.gui.control.ControlGuideProvider;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.TabFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.FlatButtonWidget;
import net.minecraft.client.InputType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class RsoOptionsScreenBehaviour implements ScreenBehaviour {
    private static final int FOOTER_GUIDE_RIGHT_GAP = 8;

    private final ScreenContext<? extends SodiumVideoOptionsScreen> context;
    private final SodiumVideoOptionsScreen screen;

    RsoOptionsScreenBehaviour(ScreenContext<? extends SodiumVideoOptionsScreen> context) {
        this.context = context;
        this.screen = context.screen();
    }

    @Override
    public void handleNavigation(Controller controller, Runnable next) {
        if (this.context.isVirtualMouseEnabled()) {
            next.run();
            return;
        }

        this.navigate(controller, BuiltinActions.GUI_NAVI_RIGHT, ScreenDirection.RIGHT);
        this.navigate(controller, BuiltinActions.GUI_NAVI_LEFT, ScreenDirection.LEFT);
        this.navigate(controller, BuiltinActions.GUI_NAVI_UP, ScreenDirection.UP);
        this.navigate(controller, BuiltinActions.GUI_NAVI_DOWN, ScreenDirection.DOWN);

        boolean nextTab = consumePulses(controller, BuiltinActions.GUI_NEXT_TAB);
        boolean previousTab = consumePulses(controller, BuiltinActions.GUI_PREV_TAB);
        String previousTabKey = this.screen.rso$getSelectedTabKey();
        boolean handled = false;
        if (this.screen.getPrompt() == null) {
            if (nextTab) {
                handled = this.screen.rso$cycleTab(1);
            }
            if (previousTab) {
                handled = this.screen.rso$cycleTab(-1) || handled;
            }
        }

        if (handled) {
            this.screen.rso$afterControllerInput(previousTabKey);
        }
        next.run();
    }

    private void navigate(Controller controller, ActionAccessor action, ScreenDirection direction) {
        while (action.on(controller).pulse().consumePulse()) {
            Minecraft.getInstance().setLastInputType(InputType.KEYBOARD_ARROW);
            if (this.screen.rso$navigateController(direction)) {
                this.context.notifyNavigation(controller);
            }
        }
    }

    @Override
    public void handleInteraction(Controller controller, Runnable next) {
        if (this.context.isVirtualMouseEnabled()) {
            next.run();
            return;
        }

        String previousTabKey = this.screen.rso$getSelectedTabKey();
        boolean promptOpen = this.screen.getPrompt() != null;
        boolean handled = false;
        if (consumePulses(controller, BuiltinActions.GUI_ABSTRACT_ACTION_1) && !promptOpen) {
            handled = press(this.screen.rso$getApplyButton());
        }
        if (consumePulses(controller, BuiltinActions.GUI_ABSTRACT_ACTION_2) && !promptOpen) {
            handled = press(this.screen.rso$getUndoButton()) || handled;
        }
        if (consumePulses(controller, BuiltinActions.GUI_PRESS)) {
            if (this.screen.getPrompt() == null && this.context.tryOpenKeyboard(controller, this.screen.rso$getFocusedLeaf())) {
                return;
            }
            handled = this.screen.rso$handleControllerPress() || handled;
        }
        if (consumePulses(controller, BuiltinActions.GUI_BACK)) {
            handled = this.screen.rso$handleControllerBack() || handled;
        }

        if (handled) {
            this.screen.rso$afterControllerInput(previousTabKey);
        }
        next.run();
    }

    private static boolean consumePulses(Controller controller, ActionAccessor action) {
        return action.on(controller).pulse().consumeAllPulses();
    }

    @Override
    public void onWidgetRebuild(Runnable next) {
        next.run();
        this.decorateButton(this.screen.rso$getApplyButton(), BuiltinActions.GUI_ABSTRACT_ACTION_1);
        this.decorateButton(this.screen.rso$getUndoButton(), BuiltinActions.GUI_ABSTRACT_ACTION_2);
        this.decorateButton(this.screen.rso$getCloseButton(), BuiltinActions.GUI_BACK);

        if (this.context.inputMode().isController() && !this.context.isVirtualMouseEnabled()) {
            this.screen.rso$focusFirstOptionInSelectedTab();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, float tickDelta, Controller controller, Runnable next) {
        next.run();

        if (this.screen.getPrompt() != null || !ReeseSodiumOptionsConfig.config().isControllerGuides() || !shouldShowGuides(controller)) {
            return;
        }

        TabFrame tabFrame = this.screen.rso$getTabFrame();
        if (tabFrame == null) {
            return;
        }

        FlatButtonWidget closeButton = this.screen.rso$getCloseButton();
        if (closeButton == null) {
            return;
        }

        int x = tabFrame.getX();
        int availableWidth = this.footerButtonLimitX() - x;
        if (availableWidth <= 0) {
            return;
        }

        Optional<Component> hint = this.footerHint(controller, availableWidth);
        if (hint.isEmpty()) {
            return;
        }

        var font = Minecraft.getInstance().font;
        int y = closeButton.getCenterY() - font.lineHeight / 2;

        graphics.text(font, hint.get(), x, y, 0xFFFFFFFF);
    }

    private Optional<Component> footerHint(Controller controller, int availableWidth) {
        List<Component> guides = new ArrayList<>();

        GuiEventListener focusedLeaf = this.screen.rso$getFocusedLeaf();
        if (focusedLeaf instanceof ControlGuideProvider guideProvider) {
            guideProvider.controlGuides().stream()
                    .map(guide -> this.guideComponent(controller, guide))
                    .flatMap(Optional::stream)
                    .forEach(guides::add);
        }

        if (this.screen.rso$getTabFrame() != null && this.screen.rso$getTabFrame().getTabs().size() > 1) {
            this.guideComponent(controller, ControlGuide.previousTab(Component.translatable("rso.controller.guide.previous_tab"))).ifPresent(guides::add);
            this.guideComponent(controller, ControlGuide.nextTab(Component.translatable("rso.controller.guide.next_tab"))).ifPresent(guides::add);
        }

        return this.fitGuides(guides, availableWidth);
    }

    private Optional<Component> guideComponent(Controller controller, ControlGuide guide) {
        return switch (guide.input()) {
            case PRESS -> actionGuide(controller, BuiltinActions.GUI_PRESS, guide.label());
            case PREVIOUS_TAB -> actionGuide(controller, BuiltinActions.GUI_PREV_TAB, guide.label());
            case NEXT_TAB -> actionGuide(controller, BuiltinActions.GUI_NEXT_TAB, guide.label());
            case NAVIGATION_LEFT_RIGHT -> this.navigationLeftRightGuide(controller, guide.label());
        };
    }

    private static Optional<Component> actionGuide(Controller controller, ActionAccessor action, Component label) {
        return actionGlyph(controller, action).map(glyph -> Component.empty()
                .append(glyph)
                .append(CommonComponents.SPACE)
                .append(label));
    }

    private Optional<Component> navigationLeftRightGuide(Controller controller, Component label) {
        Optional<Component> left = actionGlyph(controller, BuiltinActions.GUI_NAVI_LEFT);
        Optional<Component> right = actionGlyph(controller, BuiltinActions.GUI_NAVI_RIGHT);
        if (left.isEmpty() || right.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(Component.empty()
                .append(left.get())
                .append(Component.literal("/"))
                .append(right.get())
                .append(CommonComponents.SPACE)
                .append(label));
    }

    private Optional<Component> fitGuides(List<Component> guides, int availableWidth) {
        var font = Minecraft.getInstance().font;
        for (int count = guides.size(); count > 0; count--) {
            Component hint = joinGuides(guides.subList(0, count));
            if (font.width(hint) <= availableWidth) {
                return Optional.of(hint);
            }
        }

        return Optional.empty();
    }

    private static Component joinGuides(List<Component> guides) {
        MutableComponent hint = Component.empty();
        for (int i = 0; i < guides.size(); i++) {
            if (i > 0) {
                hint.append(CommonComponents.SPACE)
                        .append(CommonComponents.SPACE)
                        .append(CommonComponents.SPACE);
            }

            hint.append(guides.get(i));
        }

        return hint;
    }

    private int footerButtonLimitX() {
        int limit = Integer.MAX_VALUE;
        limit = Math.min(limit, buttonX(this.screen.rso$getUndoButton()));
        limit = Math.min(limit, buttonX(this.screen.rso$getApplyButton()));
        limit = Math.min(limit, buttonX(this.screen.rso$getCloseButton()));
        return limit == Integer.MAX_VALUE ? this.screen.width : limit - FOOTER_GUIDE_RIGHT_GAP;
    }

    private static int buttonX(@Nullable FlatButtonWidget button) {
        return button == null ? Integer.MAX_VALUE : button.getX();
    }

    private void decorateButton(@Nullable FlatButtonWidget button, ActionAccessor action) {
        if (button == null) {
            return;
        }

        button.setLabelDecorator(label -> ControlifyApi.get().getCurrentController()
                .filter(this::shouldShowGuides)
                .flatMap(controller -> actionGuide(controller, action, label))
                .orElse(label));
    }

    private static boolean press(@Nullable FlatButtonWidget button) {
        return button != null && button.tryPress();
    }

    private static Optional<Component> actionGlyph(Controller controller, ActionAccessor action) {
        return action.onOptional(controller)
                .filter(boundAction -> boundAction.primaryInput() != null)
                .map(boundAction -> ActionGlyphs.action(action));
    }

    private boolean shouldShowGuides(Controller controller) {
        return this.context.inputMode().isController()
                && controller instanceof ControllerImpl impl
                && impl.settings().generic.guide.showScreenGuides;
    }
}
