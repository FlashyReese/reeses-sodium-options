package me.flashyreese.mods.reeses_sodium_options.client.controlify;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.api.bind.InputBinding;
import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import me.flashyreese.mods.reeses_sodium_options.client.gui.control.ControlGuide;
import me.flashyreese.mods.reeses_sodium_options.client.gui.control.ControlGuideProvider;
import me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumVideoOptionsScreen;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.TabFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.FlatButtonWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Supplier;

final class RsoOptionsScreenProcessor extends ScreenProcessor<SodiumVideoOptionsScreen> {
    private static final int FOOTER_GUIDE_RIGHT_GAP = 8;

    RsoOptionsScreenProcessor(SodiumVideoOptionsScreen screen) {
        super(screen);
    }

    @Override
    protected void handleButtons(ControllerEntity controller) {
        String previousTabKey = this.screen.rso$getSelectedTabKey();
        boolean handled = false;
        boolean promptOpen = this.screen.getPrompt() != null;

        if (!promptOpen) {
            if (ControlifyBindings.GUI_ABSTRACT_ACTION_1.on(controller).justPressed()) {
                handled = press(this.screen.rso$getApplyButton()) || handled;
            }

            if (ControlifyBindings.GUI_ABSTRACT_ACTION_2.on(controller).justPressed()) {
                handled = press(this.screen.rso$getUndoButton()) || handled;
            }

            if (ControlifyBindings.GUI_NEXT_TAB.on(controller).justPressed()) {
                handled = this.screen.rso$cycleTab(1) || handled;
            }

            if (ControlifyBindings.GUI_PREV_TAB.on(controller).justPressed()) {
                handled = this.screen.rso$cycleTab(-1) || handled;
            }
        }

        if (ControlifyBindings.GUI_PRESS.on(controller).guiPressed().get()) {
            GuiEventListener focusedLeaf = this.screen.rso$getFocusedLeaf();
            handled = (this.tryOpenKeyboard(controller, focusedLeaf) || this.screen.rso$handleControllerPress()) || handled;
        }

        if (ControlifyBindings.GUI_BACK.on(controller).guiPressed().get()) {
            handled = this.screen.rso$handleControllerBack() || handled;
        }

        if (handled) {
            this.screen.rso$afterControllerInput(previousTabKey);
        }
    }

    @Override
    protected @Nullable Supplier<Boolean> createScreenNavigationFunc(ScreenDirection direction) {
        return () -> this.screen.rso$navigateController(direction);
    }

    @Override
    protected Queue<GuiEventListener> getFocusTree() {
        ArrayDeque<GuiEventListener> tree = new ArrayDeque<>();
        GuiEventListener focused = this.screen.getFocused();

        while (focused != null) {
            tree.addFirst(focused);

            if (focused instanceof ContainerEventHandler container) {
                focused = container.getFocused();
            } else {
                focused = null;
            }
        }

        return tree;
    }

    @Override
    public void onWidgetRebuild() {
        super.onWidgetRebuild();
        this.decorateButton(this.screen.rso$getApplyButton(), ControlifyBindings.GUI_ABSTRACT_ACTION_1);
        this.decorateButton(this.screen.rso$getUndoButton(), ControlifyBindings.GUI_ABSTRACT_ACTION_2);
        this.decorateButton(this.screen.rso$getCloseButton(), ControlifyBindings.GUI_BACK);

        if (Controlify.instance().currentInputMode().isController()) {
            this.screen.rso$focusFirstOptionInSelectedTab();
        }
    }

    @Override
    protected void render(ControllerEntity controller, GuiGraphics graphics, float tickDelta, Optional<VirtualMouseHandler> vmouse) {
        if (this.screen.getPrompt() != null || !shouldShowGuides(controller)) {
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

        int y = closeButton.getCenterY() - minecraft.font.lineHeight / 2;

        graphics.drawString(minecraft.font, hint.get(), x, y, 0xFFFFFFFF);
    }

    private Optional<Component> footerHint(ControllerEntity controller, int availableWidth) {
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

    private Optional<Component> guideComponent(ControllerEntity controller, ControlGuide guide) {
        return switch (guide.input()) {
            case PRESS -> this.bindingGuide(controller, ControlifyBindings.GUI_PRESS, guide.label());
            case PREVIOUS_TAB -> this.bindingGuide(controller, ControlifyBindings.GUI_PREV_TAB, guide.label());
            case NEXT_TAB -> this.bindingGuide(controller, ControlifyBindings.GUI_NEXT_TAB, guide.label());
            case NAVIGATION_LEFT_RIGHT -> this.navigationLeftRightGuide(controller, guide.label());
        };
    }

    private Optional<Component> bindingGuide(ControllerEntity controller, InputBindingSupplier supplier, Component label) {
        return activeBinding(controller, supplier).map(binding -> Component.empty()
                .append(binding.inputGlyph())
                .append(CommonComponents.SPACE)
                .append(label));
    }

    private Optional<Component> navigationLeftRightGuide(ControllerEntity controller, Component label) {
        Optional<InputBinding> left = activeBinding(controller, ControlifyBindings.GUI_NAVI_LEFT);
        Optional<InputBinding> right = activeBinding(controller, ControlifyBindings.GUI_NAVI_RIGHT);
        if (left.isEmpty() || right.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(Component.empty()
                .append(left.get().inputGlyph())
                .append(Component.literal("/"))
                .append(right.get().inputGlyph())
                .append(CommonComponents.SPACE)
                .append(label));
    }

    private Optional<Component> fitGuides(List<Component> guides, int availableWidth) {
        for (int count = guides.size(); count > 0; count--) {
            Component hint = joinGuides(guides.subList(0, count));
            if (minecraft.font.width(hint) <= availableWidth) {
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

    private void decorateButton(@Nullable FlatButtonWidget button, InputBindingSupplier binding) {
        if (button == null) {
            return;
        }

        button.setLabelDecorator(label -> Controlify.instance().getCurrentController()
                .filter(RsoOptionsScreenProcessor::shouldShowGuides)
                .map(controller -> binding.on(controller))
                .filter(bind -> !bind.isUnbound())
                .<Component>map(bind -> Component.empty()
                        .append(bind.inputGlyph())
                        .append(CommonComponents.SPACE)
                        .append(label))
                .orElse(label));
    }

    private static boolean press(@Nullable FlatButtonWidget button) {
        return button != null && button.tryPress();
    }

    private static Optional<InputBinding> activeBinding(ControllerEntity controller, InputBindingSupplier supplier) {
        InputBinding binding = supplier.on(controller);
        return binding.isUnbound() ? Optional.empty() : Optional.of(binding);
    }

    private static boolean shouldShowGuides(ControllerEntity controller) {
        return Controlify.instance().currentInputMode().isController()
                && controller.settings().generic.guide.showScreenGuides;
    }
}
