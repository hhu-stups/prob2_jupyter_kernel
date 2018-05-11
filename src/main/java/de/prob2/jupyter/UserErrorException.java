package de.prob2.jupyter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserErrorException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public UserErrorException(final @NotNull String message) {
		this(message, null);
	}
	
	public UserErrorException(final @NotNull String message, final @Nullable Throwable cause) {
		super(message, cause);
	}
}
