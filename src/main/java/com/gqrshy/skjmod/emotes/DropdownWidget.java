package com.gqrshy.skjmod.emotes;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class DropdownWidget extends ClickableWidget {
    private final TextRenderer textRenderer;
    private final String label;
    private final List<String> options;
    private final Consumer<String> onSelectionChanged;
    private final boolean filterable;
    private String selectedValue;
    private boolean isOpen = false;
    private String filterText = "";
    private int scrollOffset = 0;
    private static final int MAX_VISIBLE_OPTIONS = 5;

    public DropdownWidget(TextRenderer textRenderer, int x, int y, int width, Text message, String label, List<String> options, Consumer<String> onSelectionChanged, boolean filterable) {
        super(x, y, width, 20, message);
        this.textRenderer = textRenderer;
        this.label = label;
        this.options = options;
        this.onSelectionChanged = onSelectionChanged;
        this.filterable = filterable;
        this.selectedValue = options.isEmpty() ? "" : options.get(0);
    }

    public void renderMain(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw main dropdown button
        context.fill(getX(), getY(), getX() + width, getY() + height, Color.DARK_GRAY.getRGB());
        context.drawBorder(getX(), getY(), width, height, Color.WHITE.getRGB());

        // Draw selected value or filter text
        String displayText = filterable && isOpen ? filterText : selectedValue;
        if (displayText.isEmpty()) displayText = "Select...";
        
        context.drawTextWithShadow(textRenderer, displayText, getX() + 2, getY() + 6, Color.WHITE.getRGB());
        
        // Draw dropdown arrow
        context.drawTextWithShadow(textRenderer, isOpen ? "▲" : "▼", getX() + width - 12, getY() + 6, Color.WHITE.getRGB());
    }

    public void renderDropdown(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!isOpen) return;

        List<String> filteredOptions = options.stream()
                .filter(option -> option.toLowerCase().contains(filterText.toLowerCase()))
                .toList();

        int dropdownHeight = Math.min(filteredOptions.size(), MAX_VISIBLE_OPTIONS) * 20;
        int dropdownY = getY() + height;

        // Draw dropdown background
        context.fill(getX(), dropdownY, getX() + width, dropdownY + dropdownHeight, Color.DARK_GRAY.getRGB());
        context.drawBorder(getX(), dropdownY, width, dropdownHeight, Color.WHITE.getRGB());

        // Draw options
        for (int i = 0; i < Math.min(filteredOptions.size(), MAX_VISIBLE_OPTIONS); i++) {
            int optionIndex = i + scrollOffset;
            if (optionIndex >= filteredOptions.size()) break;

            String option = filteredOptions.get(optionIndex);
            int optionY = dropdownY + i * 20;

            // Highlight hovered option
            if (mouseX >= getX() && mouseX <= getX() + width && mouseY >= optionY && mouseY <= optionY + 20) {
                context.fill(getX(), optionY, getX() + width, optionY + 20, Color.GRAY.getRGB());
            }

            context.drawTextWithShadow(textRenderer, option, getX() + 2, optionY + 6, Color.WHITE.getRGB());
        }
    }

    public boolean willClick(double mouseX, double mouseY) {
        if (mouseX >= getX() && mouseX <= getX() + width) {
            if (mouseY >= getY() && mouseY <= getY() + height) {
                return true;
            }
            if (isOpen) {
                List<String> filteredOptions = options.stream()
                        .filter(option -> option.toLowerCase().contains(filterText.toLowerCase()))
                        .toList();
                int dropdownHeight = Math.min(filteredOptions.size(), MAX_VISIBLE_OPTIONS) * 20;
                int dropdownY = getY() + height;
                return mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= getX() && mouseX <= getX() + width && mouseY >= getY() && mouseY <= getY() + height) {
            isOpen = !isOpen;
            if (isOpen && filterable) {
                filterText = "";
            }
            return true;
        }

        if (isOpen) {
            List<String> filteredOptions = options.stream()
                    .filter(option -> option.toLowerCase().contains(filterText.toLowerCase()))
                    .toList();
            int dropdownY = getY() + height;
            int optionIndex = (int) ((mouseY - dropdownY) / 20) + scrollOffset;

            if (mouseX >= getX() && mouseX <= getX() + width && optionIndex >= 0 && optionIndex < filteredOptions.size()) {
                selectedValue = filteredOptions.get(optionIndex);
                onSelectionChanged.accept(selectedValue);
                isOpen = false;
                return true;
            }
        }

        if (isOpen) {
            isOpen = false;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isOpen || !filterable) return false;

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!filterText.isEmpty()) {
                filterText = filterText.substring(0, filterText.length() - 1);
            }
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!isOpen || !filterable) return false;

        if (Character.isLetterOrDigit(chr) || chr == '_' || chr == ' ') {
            filterText += chr;
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isOpen) return false;

        List<String> filteredOptions = options.stream()
                .filter(option -> option.toLowerCase().contains(filterText.toLowerCase()))
                .toList();

        if (verticalAmount > 0 && scrollOffset > 0) {
            scrollOffset--;
            return true;
        } else if (verticalAmount < 0 && scrollOffset < filteredOptions.size() - MAX_VISIBLE_OPTIONS) {
            scrollOffset++;
            return true;
        }
        return false;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // This method is required by ClickableWidget
        renderMain(context, mouseX, mouseY, delta);
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        // Empty implementation
    }
}