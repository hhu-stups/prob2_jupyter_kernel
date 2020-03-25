package de.prob2.jupyter.commands;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.animator.command.GetVersionCommand;
import de.prob.statespace.AnimationSelector;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VersionCommand implements Command {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private VersionCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":version";
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":version";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Display version info about the ProB 2 Jupyter kernel, ProB 2, and the underlying ProB CLI.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		final StringBuilder sb = new StringBuilder("ProB 2 Jupyter kernel: ");
		sb.append(ProBKernel.getVersion());
		sb.append(" (");
		sb.append(ProBKernel.getCommit());
		sb.append(")\nProB 2: ");
		sb.append(Main.getVersion());
		sb.append(" (");
		sb.append(Main.getGitSha());
		sb.append(")\nProB CLI:");
		final GetVersionCommand cmd = new GetVersionCommand();
		this.animationSelector.getCurrentTrace().getStateSpace().execute(cmd);
		for (final String line : cmd.getVersionString().split("\\n")) {
			sb.append("\n\t");
			sb.append(line);
		}
		return new DisplayData(sb.toString());
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
