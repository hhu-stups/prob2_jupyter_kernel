package de.prob2.jupyter.commands;

import com.google.inject.Inject;

import de.prob2.jupyter.Command;
import de.prob2.jupyter.ParameterInspectors;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BsymbCommand implements Command {
	@Inject
	private BsymbCommand() {
		super();
	}
	
	@Override
	public @NotNull String getName() {
		return ":bsymb";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return Parameters.NONE;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":bsymb";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Load all bsymb.sty command definitions, so that they can be used in $\\LaTeX$ formulas in Markdown cells.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "If you want to use bsymb.sty commands in your Markdown cells, you *must* run `:bsymb` first. Otherwise the commands may not be reliably available after reopening the notebook.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final DisplayData data = new DisplayData("Your current environment uses plain text output; the bsymb.sty LaTeX commands will not be loaded.");
		// The actual bsymb definitions are added by the ProBKernel class when it processes the command result, and the error message below will be replaced.
		// If this error message is visible to the user, it means that the definitions were not added correctly.
		data.putMarkdown("The bsymb.sty $\\LaTeX$ commands were not loaded due to an internal error. If you see this message, please file a bug report.");
		return data;
	}
	
	@Override
	public @NotNull ParameterInspectors getParameterInspectors() {
		return ParameterInspectors.NONE;
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		return null;
	}
}
