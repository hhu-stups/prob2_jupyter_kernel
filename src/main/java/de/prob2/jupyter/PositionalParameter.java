package de.prob2.jupyter;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

public final class PositionalParameter {
	public static final class RequiredSingle extends Parameter<@NotNull String> {
		public RequiredSingle(final @NotNull String identifier) {
			super(identifier, false, Parameter.Splitter.REGULAR, Parameter.Validator.EXACTLY_ONE);
		}
	}
	
	public static final class OptionalSingle extends Parameter<@NotNull Optional<String>> {
		public OptionalSingle(final @NotNull String identifier) {
			super(identifier, false, Parameter.Splitter.REGULAR, Parameter.Validator.ZERO_OR_ONE);
		}
	}
	
	public static final class RequiredMultiple extends Parameter<@NotNull List<@NotNull String>> {
		public RequiredMultiple(final @NotNull String identifier) {
			super(identifier, true, Parameter.Splitter.REGULAR, Parameter.Validator.ONE_OR_MORE);
		}
	}
	
	public static final class OptionalMultiple extends Parameter<@NotNull List<@NotNull String>> {
		public OptionalMultiple(final @NotNull String identifier) {
			super(identifier, true, Parameter.Splitter.REGULAR, Parameter.Validator.ZERO_OR_MORE);
		}
	}
	
	public static final class RequiredRemainder extends Parameter<@NotNull String> {
		public RequiredRemainder(final @NotNull String identifier) {
			super(identifier, false, Parameter.Splitter.REMAINDER, Parameter.Validator.EXACTLY_ONE);
		}
	}
	
	public static final class OptionalRemainder extends Parameter<@NotNull Optional<String>> {
		public OptionalRemainder(final @NotNull String identifier) {
			super(identifier, false, Parameter.Splitter.REMAINDER, Parameter.Validator.ZERO_OR_ONE);
		}
	}
	
	private PositionalParameter() {
		super();
		
		throw new AssertionError("Utility class");
	}
}
