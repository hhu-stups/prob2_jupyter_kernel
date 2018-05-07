package de.prob2.jupyter.commands;

import org.jetbrains.annotations.NotNull;

public interface BaseCommand {
	public abstract @NotNull String getSyntax();
	
	public abstract @NotNull String getShortHelp();
	
	public default @NotNull String getLongHelp() {
		return this.getSyntax() + "\n\n" + this.getShortHelp();
	}
}
