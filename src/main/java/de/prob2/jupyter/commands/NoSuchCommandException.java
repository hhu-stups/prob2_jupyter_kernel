package de.prob2.jupyter.commands;

import de.prob2.jupyter.UserErrorException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NoSuchCommandException extends UserErrorException {
	private static final long serialVersionUID = 1L;
	
	private final @NotNull String name;
	
	public NoSuchCommandException(final @NotNull String name, final @Nullable String message, final @Nullable Throwable cause) {
		super(formatMessage(name, message), cause);
		
		this.name = name;
	}
	
	public NoSuchCommandException(final @NotNull String name, final @Nullable String message) {
		this(name, message, null);
	}
	
	public NoSuchCommandException(final @NotNull String name, final @Nullable Throwable cause) {
		this(name, null, cause);
	}
	
	public NoSuchCommandException(final @NotNull String name) {
		this(name, null, null);
	}
	
	private static final @NotNull String formatMessage(final @NotNull String name, final @Nullable String message) {
		final StringBuilder sb = new StringBuilder("Unknown command: \"");
		sb.append(name);
		sb.append('"');
		if (message != null) {
			sb.append(": ");
			sb.append(message);
		}
		return sb.toString();
	}
	
	public @NotNull String getName() {
		return this.name;
	}
}
