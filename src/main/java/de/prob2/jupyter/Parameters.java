package de.prob2.jupyter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Parameters {
	public static final @NotNull Parameters NONE = new Parameters(Collections.emptyList());
	
	private final @NotNull List<Parameter<?>> positionalParameters;
	private final @Nullable Parameter.RequiredSingle bodyParam;
	
	public Parameters(final @NotNull List<Parameter<?>> positionalParameters, final @Nullable Parameter.RequiredSingle bodyParam) {
		super();
		
		this.positionalParameters = positionalParameters;
		
		boolean seenOptional = false;
		boolean seenOnlyLast = false;
		for (final Parameter<?> param : positionalParameters) {
			final boolean isOptional = param.getValidator() == Parameter.Validator.ZERO_OR_ONE || param.getValidator() == Parameter.Validator.ZERO_OR_MORE;
			final boolean isOnlyLast = param.isRepeating() || param.getSplitter() == Parameter.Splitter.REMAINDER;
			if (seenOnlyLast) {
				throw new IllegalArgumentException("A repeating or remainder positional parameter cannot be followed by any more positional parameters");
			}
			if (seenOptional && isOptional) {
				throw new IllegalArgumentException("Required positional parameter " + param + " cannot follow an optional positional parameter");
			}
			seenOptional |= isOptional;
			seenOnlyLast |= isOnlyLast;
		}
		
		this.bodyParam = bodyParam;
	}
	
	public Parameters(final @NotNull List<Parameter<?>> positionalParameters) {
		this(positionalParameters, null);
	}
	
	public @NotNull List<Parameter<?>> getPositionalParameters() {
		return this.positionalParameters;
	}
	
	public @NotNull Optional<Parameter.RequiredSingle> getBodyParam() {
		return Optional.ofNullable(this.bodyParam);
	}
}
