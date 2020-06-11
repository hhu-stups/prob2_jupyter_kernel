package de.prob2.jupyter;

import java.util.Optional;

import com.google.common.base.MoreObjects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The unvalidated result of a command argument splitting operation ({@link CommandUtils#splitArgs(Parameters, String, int)} or {@link CommandUtils#splitArgs(Parameters, String)}).
 */
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
	
	/**
	 * Return the split arguments from the input string,
	 * mapped to their respective parameters.
	 * 
	 * @return the split arguments from the input string,
	 * mapped to their respective parameters
	 */
	public @NotNull SplitArguments getArguments() {
		return this.arguments;
	}
	
	/**
	 * Return the parameter to which the last split argument belongs,
	 * or {@link Optional#empty()} if no arguments have been split.
	 * This information is mainly useful when calling {@link CommandUtils#splitArgs(Parameters, String, int)} with an explicit {@code upToPosition} argument.
	 * 
	 * @return the parameter to which the last split argument belongs,
	 * or {@link Optional#empty()} if no arguments have been split
	 */
	public @NotNull Optional<Parameter<?>> getParameterAtPosition() {
		return Optional.ofNullable(this.parameterAtPosition);
	}
	
	/**
	 * Return the remainder of the argument string that has not been split.
	 * If the argument string was consumed completely by the split operation,
	 * this is an empty string.
	 * 
	 * @return the remainder of the argument string that has not been split
	 */
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
