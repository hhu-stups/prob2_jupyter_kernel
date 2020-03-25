package de.prob2.jupyter.commands;

import com.google.inject.Inject;

import de.prob.animator.command.ComputeStateSpaceStatsCommand;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.AnimationSelector;

import de.prob2.jupyter.Command;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StatsCommand implements Command {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private StatsCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":stats";
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":stats";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Show statistics about the state space.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		if (!argString.isEmpty()) {
			throw new UserErrorException("Expected no arguments");
		}
		final ComputeStateSpaceStatsCommand cmd = new ComputeStateSpaceStatsCommand();
		this.animationSelector.getCurrentTrace().getStateSpace().execute(cmd);
		final StateSpaceStats stats = cmd.getResult();
		
		final StringBuilder sbPlain = new StringBuilder();
		final StringBuilder sbMarkdown = new StringBuilder();
		
		sbPlain.append("Explored States: ");
		sbPlain.append(stats.getNrProcessedNodes());
		sbPlain.append('/');
		sbPlain.append(stats.getNrTotalNodes());
		sbPlain.append('\n');
		sbMarkdown.append("**Explored States:** ");
		sbMarkdown.append(stats.getNrProcessedNodes());
		sbMarkdown.append('/');
		sbMarkdown.append(stats.getNrTotalNodes());
		sbMarkdown.append("  \n");
		
		sbPlain.append("Transitions: ");
		sbPlain.append(stats.getNrTotalTransitions());
		sbMarkdown.append("**Transitions:** ");
		sbMarkdown.append(stats.getNrTotalTransitions());
		
		final DisplayData result = new DisplayData(sbPlain.toString());
		result.putMarkdown(sbMarkdown.toString());
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
