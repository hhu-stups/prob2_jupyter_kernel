package de.prob2.jupyter.commands;

import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Command {
	public abstract @NotNull String getSyntax();
	
	public abstract @NotNull String getShortHelp();
	
	public abstract @Nullable DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String argString);
	
	public abstract @Nullable ReplacementOptions complete(final @NotNull ProBKernel kernel, final @NotNull String argString, final int at);
}
