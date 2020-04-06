package de.prob2.jupyter.commands;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.animator.command.GetAnimationMatrixForStateCommand;
import de.prob.animator.command.GetImagesForMachineCommand;
import de.prob.animator.command.GetPreferenceCommand;
import de.prob.animator.domainobjects.AnimationMatrixEntry;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ShowCommand implements Command {
	private final @NotNull AnimationSelector animationSelector;
	private final @NotNull Provider<ProBKernel> proBKernelProvider;
	
	@Inject
	private ShowCommand(final @NotNull AnimationSelector animationSelector, final @NotNull Provider<ProBKernel> proBKernelProvider) {
		super();
		
		this.animationSelector = animationSelector;
		this.proBKernelProvider = proBKernelProvider;
	}
	
	@Override
	public @NotNull String getName() {
		return ":show";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return Parameters.NONE;
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
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
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
		
		final Path machineDirectory = this.proBKernelProvider.get().getCurrentMachineDirectory();
		final Map<Integer, String> images = new HashMap<>(cmdImages.getImages());
		// Animation image paths are relative to the machine directory, which may not be the same as the kernel's working directory.
		images.replaceAll((k, v) -> machineDirectory.resolve(v).toString());
		
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
		for (final List<AnimationMatrixEntry> row : cmdMatrix.getMatrix()) {
			tableBuilder.append("\n<tr>");
			for (final AnimationMatrixEntry entry : row) {
				final int padding;
				final String contents;
				if (entry == null) {
					padding = 0;
					contents = "";
				} else if (entry instanceof AnimationMatrixEntry.Image) {
					final AnimationMatrixEntry.Image imageEntry = (AnimationMatrixEntry.Image)entry;
					padding = imagePadding;
					contents = String.format(
						"<img alt=\"%d\" src=\"%s\"/>",
						imageEntry.getImageNumber(),
						images.get(imageEntry.getImageNumber()).replace(File.separator, "/")
					);
				} else if (entry instanceof AnimationMatrixEntry.Text) {
					padding = stringPadding;
					contents = ((AnimationMatrixEntry.Text)entry).getText();
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
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		return null;
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		return null;
	}
}
