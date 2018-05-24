package de.prob2.jupyter.commands;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.animator.command.GetVersionCommand;
import de.prob.statespace.AnimationSelector;

import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.messages.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class VersionCommand implements LineCommand {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private VersionCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":version";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Display version info about the ProB CLI and ProB 2";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String argString) {
		final GetVersionCommand cmd = new GetVersionCommand();
		this.animationSelector.getCurrentTrace().getStateSpace().execute(cmd);
		return new DisplayData(String.format("ProB CLI: %s\nProB 2: %s (%s)", cmd.getVersion(), Main.getVersion(), Main.getGitSha()));
	}
}