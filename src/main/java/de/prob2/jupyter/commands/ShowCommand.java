package de.prob2.jupyter.commands;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import de.prob.animator.command.GetAnimationMatrixForStateCommand;
import de.prob.animator.command.GetImagesForMachineCommand;
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
		
		final GetImagesForMachineCommand cmd1 = new GetImagesForMachineCommand();
		trace.getStateSpace().execute(cmd1);
		final Map<Integer, String> images = cmd1.getImages();
		
		final GetAnimationMatrixForStateCommand cmd2 = new GetAnimationMatrixForStateCommand(trace.getCurrentState());
		trace.getStateSpace().execute(cmd2);
		
		if (cmd2.getMatrix() == null) {
			throw new UserErrorException("No animation function visualisation available");
		}
		
		final StringBuilder tableBuilder = new StringBuilder("<table><tbody>");
		for (final List<Object> row : cmd2.getMatrix()) {
			tableBuilder.append("\n<tr>");
			for (final Object entry : row) {
				tableBuilder.append("\n<td style=\"padding:0\">");
				if (entry instanceof Integer) {
					tableBuilder.append(String.format("![%d](%s)", entry, images.get(entry)));
				} else if (entry instanceof String) {
					tableBuilder.append(entry);
				} else {
					throw new AssertionError("Unhandled animation matrix entry type: " + entry.getClass());
				}
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
