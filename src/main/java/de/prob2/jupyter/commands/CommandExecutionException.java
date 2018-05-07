package de.prob2.jupyter.commands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CommandExecutionException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private final @NotNull String commandName;
	
	public CommandExecutionException(final @NotNull String commandName, final @NotNull String message, final @Nullable Throwable cause) {
		super(formatMessage(commandName, message), cause);
		
		this.commandName = commandName;
	}
	
	public CommandExecutionException(final @NotNull String commandName, final @NotNull String message) {
		this(commandName, message, null);
	}
	
	private static @NotNull String formatMessage(final @NotNull String commandName, final @NotNull String message) {
		return String.format("%s: %s", commandName, message);
	}
	
	public @NotNull String getCommandName() {
		return this.commandName;
	}
}
