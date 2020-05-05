package de.prob2.jupyter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Parameters {
	public static final @NotNull Parameters NONE = new Parameters(Collections.emptyList());
	
	private final @NotNull List<PositionalParameter<?>> positionalParameters;
	private final @Nullable PositionalParameter.RequiredRemainder bodyParam;
	
	public Parameters(final @NotNull List<PositionalParameter<?>> positionalParameters, final @Nullable PositionalParameter.RequiredRemainder bodyParam) {
		super();
		
		this.positionalParameters = positionalParameters;
		
		boolean seenOptional = false;
		boolean seenOnlyLast = false;
		for (final PositionalParameter<?> param : positionalParameters) {
			final boolean isOptional = param instanceof PositionalParameter.OptionalSingle || param instanceof PositionalParameter.OptionalRemainder;
			final boolean isOnlyLast = param instanceof PositionalParameter.RequiredRemainder || param instanceof PositionalParameter.OptionalRemainder;
			if (seenOnlyLast) {
				throw new IllegalArgumentException("A remainder positional parameter cannot be followed by any more positional parameters");
			}
			if (seenOptional && isOptional) {
				throw new IllegalArgumentException("Required positional parameter " + param + " cannot follow an optional positional parameter");
			}
			seenOptional |= isOptional;
			seenOnlyLast |= isOnlyLast;
		}
		
		this.bodyParam = bodyParam;
	}
	
	public Parameters(final @NotNull List<PositionalParameter<?>> positionalParameters) {
		this(positionalParameters, null);
	}
	
	public @NotNull List<PositionalParameter<?>> getPositionalParameters() {
		return this.positionalParameters;
	}
	
	public @NotNull Optional<PositionalParameter.RequiredRemainder> getBodyParam() {
		return Optional.ofNullable(this.bodyParam);
	}
}
