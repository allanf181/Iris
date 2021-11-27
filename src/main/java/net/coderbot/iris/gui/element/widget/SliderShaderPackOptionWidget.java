package net.coderbot.iris.gui.element.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gui.GuiUtil;
import net.coderbot.iris.gui.screen.ShaderPackScreen;
import net.coderbot.iris.shaderpack.option.StringOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.*;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

// TODO: Deduplicate a lot of code
public class SliderShaderPackOptionWidget extends AbstractShaderPackOptionWidget {
	private static final Component DIVIDER = new TextComponent(": ");
	private static final int VALUE_SECTION_WIDTH = 45;
	private static final int PREVIEW_SLIDER_WIDTH = 4;
	private static final int ACTIVE_SLIDER_WIDTH = 6;

	private final StringOption option;
	private final int originalValueIndex;
	private final String originalValue;
	private final int valueCount;
	private final MutableComponent label;

	private @Nullable Component trimmedLabel = null;
	private Component valueLabel;
	private int valueIndex;
	private boolean mouseDown = false;

	private int maxLabelWidth = -1;

	public SliderShaderPackOptionWidget(StringOption option, String value) {
		this.option = option;
		this.label = new TranslatableComponent("option." + option.getName());

		List<String> values = option.getAllowedValues();
		this.valueCount = values.size();
		this.originalValueIndex = values.indexOf(value);
		this.valueIndex = originalValueIndex;
		this.originalValue = value;

		updateValueLabel();
	}

	@Override
	public void render(PoseStack poseStack, int x, int y, int width, int height, int mouseX, int mouseY, float tickDelta, boolean hovered) {
		GuiUtil.bindIrisWidgetsTexture();

		// Does not change based on whether widget is hovered, due to the special rendering when hovered
		GuiUtil.drawButton(poseStack, x, y, width, height, false, false);

		Font font = Minecraft.getInstance().font;

		int valueSectionWidth = Math.max(VALUE_SECTION_WIDTH, font.width(this.valueLabel) + 8);

		this.maxLabelWidth = (width - 8) - valueSectionWidth;

		if (this.trimmedLabel == null) {
			updateLabel();
		}

		if (!hovered) {
			GuiUtil.drawButton(poseStack, (x + width) - (valueSectionWidth + 2), y + 2, valueSectionWidth, height - 4, false, true);

			// Draw the preview slider
			if (this.valueIndex >= 0) {
				// Range of x values the slider can occupy
				int sliderSpace = (valueSectionWidth - 4) - PREVIEW_SLIDER_WIDTH;

				// Position of slider
				int sliderPos = ((x + width) - valueSectionWidth) + (int)(((float)valueIndex / (valueCount - 1)) * sliderSpace);

				GuiUtil.drawButton(poseStack, sliderPos, y + 4, PREVIEW_SLIDER_WIDTH, height - 8, false, false);
			}

			font.drawShadow(poseStack, trimmedLabel, x + 6, y + 7, 0xFFFFFF);

			font.drawShadow(poseStack, this.valueLabel, (x + (width - 2)) - (int)(valueSectionWidth * 0.5) - (int)(font.width(this.valueLabel) * 0.5), y + 7, 0xFFFFFF);

			this.maxLabelWidth = (width - 8) - valueSectionWidth;
		} else {
			renderHovered(poseStack, x, y, width, height, mouseX, mouseY, tickDelta);
		}

		if (this.mouseDown) {
			// Release if the mouse went off the slider
			if (!hovered) {
				this.onReleased();
			}

			whileDragging(x, width, mouseX);
		}
	}

	private void renderHovered(PoseStack poseStack, int x, int y, int width, int height, int mouseX, int mouseY, float tickDelta) {
		// Draw slider area
		GuiUtil.drawButton(poseStack, x + 2, y + 2, width - 4, height - 4, false, true);

		// Range of x values the slider can occupy
		int sliderSpace = (width - 8) - ACTIVE_SLIDER_WIDTH;
		// Position of slider
		int sliderPos = (x + 4) + (int)(((float)valueIndex / (valueCount - 1)) * sliderSpace);
		// Draw slider
		GuiUtil.drawButton(poseStack, sliderPos, y + 4, ACTIVE_SLIDER_WIDTH, height - 8, this.mouseDown, false);

		// Draw value label
		Font font = Minecraft.getInstance().font;
		font.drawShadow(poseStack, this.valueLabel, (int)(x + (width * 0.5)) - (int)(font.width(this.valueLabel) * 0.5), y + 7, 0xFFFFFF);

		// To prevent other elements from being drawn on top of the tooltip
		ShaderPackScreen.TOP_LAYER_RENDER_QUEUE.add(() -> GuiUtil.drawTextPanel(font, poseStack, this.label, mouseX + 2, mouseY - 16));
	}

	private void whileDragging(int x, int width, int mouseX) {
		float mousePositionAcrossWidget = Mth.clamp((float)(mouseX - (x + 4)) / (width - 8), 0, 1);

		int newValueIndex = Math.min(valueCount - 1, (int)(mousePositionAcrossWidget * valueCount));

		if (valueIndex != newValueIndex) {
			valueIndex = newValueIndex;
			updateValueLabel();
		}
	}

	private void updateLabel() {
		MutableComponent label = GuiUtil.shortenText(
				Minecraft.getInstance().font,
				this.label.copy().append(DIVIDER),
				maxLabelWidth);

		if (this.valueIndex != originalValueIndex) {
			label = label.withStyle(style -> style.withColor(TextColor.fromRgb(0xffc94a)));
		}
		this.trimmedLabel = label;

		updateValueLabel();
	}

	private void updateValueLabel() {
		String valueStr;
		if (this.valueIndex < 0) {
			valueStr = this.originalValue;
		} else {
			valueStr = this.option.getAllowedValues().get(this.valueIndex);
		}
		this.valueLabel = GuiUtil.translateOrDefault(
				new TextComponent(valueStr).withStyle(style -> style.withColor(TextColor.fromRgb(0x6688ff))),
				"value." + option.getName() + "." + valueStr);
	}

	private void queueOptionToPending() {
		Iris.addPendingShaderPackOption(this.option.getName(), this.option.getAllowedValues().get(valueIndex));
	}

	private void onReleased() {
		mouseDown = false;

		this.queueOptionToPending();
		this.updateLabel();
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
			mouseDown = true;
			GuiUtil.playButtonClickSound();

			return true;
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
			this.onReleased();

			return true;
		}
		return super.mouseReleased(mx, my, button);
	}
}