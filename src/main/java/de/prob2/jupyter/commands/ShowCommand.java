package de.prob2.jupyter.commands;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import de.prob.animator.command.GetAnimationMatrixForStateCommand;
import de.prob.animator.command.GetImagesForMachineCommand;
import de.prob.animator.command.GetPreferenceCommand;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ShowCommand implements Command {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private ShowCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":show";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Show the machine's animation function visualisation for the current state.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "The visualisation is static, any defined right-click options cannot be viewed or used.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		if (!argString.isEmpty()) {
			throw new UserErrorException("Expected no arguments");
		}
		
		final Trace trace = this.animationSelector.getCurrentTrace();
		
		if (!trace.getCurrentState().isInitialised()) {
			throw new UserErrorException("Machine is not initialised, cannot show animation function visualisation");
		}
		
		final GetImagesForMachineCommand cmdImages = new GetImagesForMachineCommand();
		final GetAnimationMatrixForStateCommand cmdMatrix = new GetAnimationMatrixForStateCommand(trace.getCurrentState());
		final GetPreferenceCommand cmdImagePadding = new GetPreferenceCommand("TK_CUSTOM_STATE_VIEW_PADDING");
		final GetPreferenceCommand cmdStringPadding = new GetPreferenceCommand("TK_CUSTOM_STATE_VIEW_STRING_PADDING");
		final GetPreferenceCommand cmdFontName = new GetPreferenceCommand("TK_CUSTOM_STATE_VIEW_FONT_NAME");
		final GetPreferenceCommand cmdFontSize = new GetPreferenceCommand("TK_CUSTOM_STATE_VIEW_FONT_SIZE");
		trace.getStateSpace().execute(
			cmdImages,
			cmdMatrix,
			cmdImagePadding,
			cmdStringPadding,
			cmdFontName,
			cmdFontSize
		);
		
		if (cmdMatrix.getMatrix() == null) {
			throw new UserErrorException("No animation function visualisation available");
		}
		
		final Map<Integer, String> images = cmdImages.getImages();
		final int imagePadding = Integer.parseInt(cmdImagePadding.getValue());
		final int stringPadding = Integer.parseInt(cmdStringPadding.getValue());
		final String fontName = cmdFontName.getValue();
		final int fontSize = Integer.parseInt(cmdFontSize.getValue());
		
		final StringBuilder tableBuilder = new StringBuilder("<table style=\"font-family:");
		if (!fontName.isEmpty()) {
			tableBuilder.append('"');
			tableBuilder.append(fontName);
			tableBuilder.append("\" ");
		}
		tableBuilder.append("monospace");
		if (fontSize != 0) {
			tableBuilder.append(";font-size:");
			tableBuilder.append(fontSize);
			tableBuilder.append("px");
		}
		tableBuilder.append("\"><tbody>");
		for (final List<Object> row : cmdMatrix.getMatrix()) {
			tableBuilder.append("\n<tr>");
			for (final Object entry : row) {
				final int padding;
				final String contents;
				if (entry == null) {
					padding = 0;
					contents = "";
				} else if (entry instanceof Integer) {
					padding = imagePadding;
					contents = String.format("![%d](%s)", entry, images.get(entry));
				} else if (entry instanceof String) {
					padding = stringPadding;
					contents = (String)entry;
				} else {
					throw new AssertionError("Unhandled animation matrix entry type: " + entry.getClass());
				}
				tableBuilder.append("\n<td style=\"padding:");
				tableBuilder.append(padding);
				tableBuilder.append("px\">");
				tableBuilder.append(contents);
				tableBuilder.append("</td>");
			}
			tableBuilder.append("\n</tr>");
		}
		tableBuilder.append("\n</tbody></table>");
		
		final DisplayData result = new DisplayData("<Animation function visualisation>");
		result.putMarkdown(tableBuilder.toString());
		return result;
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		return null;
	}
}
