package de.prob2.jupyter.commands;

import java.util.Map;

import com.google.inject.Inject;

import de.prob.animator.command.GetImagesForMachineCommand;
import de.prob.animator.command.GetImagesForStateCommand;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

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
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String argString) {
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
		
		final GetImagesForStateCommand cmd2 = new GetImagesForStateCommand(trace.getCurrentState().getId());
		trace.getStateSpace().execute(cmd2);
		
		final StringBuilder tableBuilder = new StringBuilder("<table><tbody>");
		for (final Integer[] row : cmd2.getMatrix()) {
			tableBuilder.append("\n<tr>");
			for (final Integer id : row) {
				tableBuilder.append(String.format("\n<td style=\"padding:0\">![%d](%s)</td>", id, images.get(id)));
			}
			tableBuilder.append("\n</tr>");
		}
		tableBuilder.append("\n</tbody></table>");
		
		final DisplayData result = new DisplayData("<Animation function visualisation>");
		result.putMarkdown(tableBuilder.toString());
		return result;
	}
}
