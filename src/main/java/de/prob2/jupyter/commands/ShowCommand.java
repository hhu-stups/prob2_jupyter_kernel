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
import de.prob.animator.command.GetVisBHtmlForStates;
import de.prob.animator.command.GetVisBLoadedJsonFileCommand;
import de.prob.animator.command.LoadVisBCommand;
import de.prob.animator.domainobjects.AnimationMatrixEntry;
import de.prob.animator.domainobjects.VisBExportOptions;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.State;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.ParameterCompleters;
import de.prob2.jupyter.ParameterInspectors;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

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
		return "Show the machine's visualization for the current state.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "Only the current state is visualized. Interactively executing operations is not supported.\n\n"
			+ "Both VisB and the classical `ANIMATION_FUNCTION` are supported. The visualization type is detected automatically (if both are defined, VisB is preferred).";
	}
	
	private @Nullable DisplayData getAnimationFunctionVisualization(final @NotNull State state) {
		final GetImagesForMachineCommand cmdImages = new GetImagesForMachineCommand();
		final GetAnimationMatrixForStateCommand cmdMatrix = new GetAnimationMatrixForStateCommand(state);
		final GetPreferenceCommand cmdImagePadding = new GetPreferenceCommand("TK_CUSTOM_STATE_VIEW_PADDING");
		final GetPreferenceCommand cmdStringPadding = new GetPreferenceCommand("TK_CUSTOM_STATE_VIEW_STRING_PADDING");
		final GetPreferenceCommand cmdFontName = new GetPreferenceCommand("TK_CUSTOM_STATE_VIEW_FONT_NAME");
		final GetPreferenceCommand cmdFontSize = new GetPreferenceCommand("TK_CUSTOM_STATE_VIEW_FONT_SIZE");
		state.getStateSpace().execute(
			cmdImages,
			cmdMatrix,
			cmdImagePadding,
			cmdStringPadding,
			cmdFontName,
			cmdFontSize
		);
		
		if (cmdMatrix.getMatrix().isEmpty()) {
			return null;
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
		
		final DisplayData result = new DisplayData("<Animation function visualization>");
		result.putMarkdown(tableBuilder.toString());
		return result;
	}
	
	private static @Nullable DisplayData getVisBVisualization(final @NotNull State state) {
		GetVisBLoadedJsonFileCommand loadedCmd = new GetVisBLoadedJsonFileCommand();
		state.getStateSpace().execute(loadedCmd);
		
		if (loadedCmd.getPath() == null) {
			// Load VisB visualization defined inside the model itself
			state.getStateSpace().execute(new LoadVisBCommand(""));
		}
		
		state.getStateSpace().execute(loadedCmd);
		if (loadedCmd.getPath() == null) {
			// The model doesn't contain any VisB visualization
			return null;
		}
		
		GetVisBHtmlForStates htmlCmd = new GetVisBHtmlForStates(state, VisBExportOptions.DEFAULT.withShowHeader(false).withShowVersionInfo(false));
		state.getStateSpace().execute(htmlCmd);
		
		final DisplayData result = new DisplayData("<VisB visualization>");
		result.putHTML(htmlCmd.getHtml());
		return result;
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final State state = this.animationSelector.getCurrentTrace().getCurrentState();
		
		final DisplayData visBResult = getVisBVisualization(state);
		if (visBResult != null) {
			return visBResult;
		}
		
		DisplayData animationFunctionResult = this.getAnimationFunctionVisualization(state);
		if (animationFunctionResult != null) {
			return animationFunctionResult;
		}
		
		if (state.isInitialised()) {
			throw new UserErrorException("No VisB visualization or ANIMATION_FUNCTION defined in the model");
		} else {
			throw new UserErrorException("Model is not initialized, cannot show visualization (or no visualization is defined)");
		}
	}
	
	@Override
	public @NotNull ParameterInspectors getParameterInspectors() {
		return ParameterInspectors.NONE;
	}
	
	@Override
	public @NotNull ParameterCompleters getParameterCompleters() {
		return ParameterCompleters.NONE;
	}
}
