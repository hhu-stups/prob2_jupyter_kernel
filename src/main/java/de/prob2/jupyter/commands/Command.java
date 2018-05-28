package de.prob2.jupyter.commands;

import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.messages.DisplayData;

import org.jetbrains.annotations.NotNull;

public interface Command {
	public abstract @NotNull String getSyntax();
	
	public abstract @NotNull String getShortHelp();
	
	public default @NotNull String getLongHelp() {
		return this.getSyntax() + "\n\n" + this.getShortHelp();
	}
	
	public abstract @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String argString);
}
