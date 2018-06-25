package de.prob2.jupyter.commands;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Command {
	public abstract @NotNull String getSyntax();
	
	public abstract @NotNull String getShortHelp();
	
	public abstract  @NotNull String getHelpBody();
	
	public abstract @Nullable DisplayData run(final @NotNull String argString);
	
	public abstract @Nullable ReplacementOptions complete(final @NotNull String argString, final int at);
}
