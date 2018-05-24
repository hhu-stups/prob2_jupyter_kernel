package de.prob2.jupyter.commands;

import de.prob2.jupyter.UserErrorException;

import org.jetbrains.annotations.NotNull;

public final class CommandExecutionException extends UserErrorException {
	private static final long serialVersionUID = 1L;
	
	private final @NotNull String commandName;
	
	public CommandExecutionException(final @NotNull String commandName, final @NotNull Throwable cause) {
		super(formatMessage(commandName, cause.getMessage()), cause);
		
		this.commandName = commandName;
	}
	
	private static @NotNull String formatMessage(final @NotNull String commandName, final @NotNull String message) {
		return String.format("%s: %s", commandName, message);
	}
	
	public @NotNull String getCommandName() {
		return this.commandName;
	}
}
