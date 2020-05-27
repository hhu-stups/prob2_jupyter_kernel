package de.prob2.jupyter;

import org.jetbrains.annotations.NotNull;

public final class SplitResult {
	private final @NotNull SplitArguments arguments;
	private final @NotNull PositionedString remaining;
	
	public SplitResult(final @NotNull SplitArguments arguments, final @NotNull PositionedString remaining) {
		super();
		
		this.arguments = arguments;
		this.remaining = remaining;
	}
	
	public @NotNull SplitArguments getArguments() {
		return this.arguments;
	}
	
	public @NotNull PositionedString getRemaining() {
		return this.remaining;
	}
}
