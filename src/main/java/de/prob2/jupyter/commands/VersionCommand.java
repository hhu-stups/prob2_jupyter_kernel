package de.prob2.jupyter.commands;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.BParser;
import de.prob.Main;
import de.prob.animator.command.GetVersionCommand;
import de.prob.statespace.AnimationSelector;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.ParameterCompleters;
import de.prob2.jupyter.ParameterInspectors;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

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
	public @NotNull Parameters getParameters() {
		return Parameters.NONE;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":version";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Display version info about the ProB 2 Jupyter kernel and its underlying components.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final StringBuilder sb = new StringBuilder("ProB 2 Jupyter kernel: ");
		sb.append(ProBKernel.getVersion());
		sb.append(" (");
		sb.append(ProBKernel.getCommit());
		sb.append(")\nProB 2: ");
		sb.append(Main.getVersion());
		sb.append(" (");
		sb.append(Main.getGitSha());
		sb.append(")\nProB B parser: ");
		sb.append(BParser.getVersion());
		sb.append(" (");
		sb.append(BParser.getGitSha());
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
	public @NotNull ParameterInspectors getParameterInspectors() {
		return ParameterInspectors.NONE;
	}
	
	@Override
	public @NotNull ParameterCompleters getParameterCompleters() {
		return ParameterCompleters.NONE;
	}
}
