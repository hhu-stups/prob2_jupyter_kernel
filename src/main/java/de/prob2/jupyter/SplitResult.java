package de.prob2.jupyter;

import java.util.Optional;

import com.google.common.base.MoreObjects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SplitResult {
	private final @NotNull SplitArguments arguments;
	private final @Nullable Parameter<?> parameterAtPosition;
	private final @NotNull PositionedString remaining;
	
	public SplitResult(final @NotNull SplitArguments arguments, final @Nullable Parameter<?> parameterAtPosition, final @NotNull PositionedString remaining) {
		super();
		
		this.arguments = arguments;
		this.parameterAtPosition = parameterAtPosition;
		this.remaining = remaining;
	}
	
	public @NotNull SplitArguments getArguments() {
		return this.arguments;
	}
	
	public @NotNull Optional<Parameter<?>> getParameterAtPosition() {
		return Optional.ofNullable(this.parameterAtPosition);
	}
	
	public @NotNull PositionedString getRemaining() {
		return this.remaining;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("arguments", this.getArguments())
			.add("parameterAtPosition", this.getParameterAtPosition())
			.add("remaining", this.getRemaining())
			.toString();
	}
}
