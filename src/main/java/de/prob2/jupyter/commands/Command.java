package de.prob2.jupyter.commands;

import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public interface Command {
	public abstract @NotNull String getSyntax();
	
	public abstract @NotNull String getShortHelp();
	
	public abstract @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String argString);
}
