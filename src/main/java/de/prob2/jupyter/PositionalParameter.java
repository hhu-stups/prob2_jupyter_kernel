package de.prob2.jupyter;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

public class PositionalParameter<T> extends Parameter<T> {
	public static final class RequiredSingle extends PositionalParameter<@NotNull String> {
		public RequiredSingle(final @NotNull String identifier) {
			super(identifier, false, Parameter.Splitter.REGULAR, Parameter.Validator.EXACTLY_ONE);
		}
	}
	
	public static final class OptionalSingle extends PositionalParameter<@NotNull Optional<String>> {
		public OptionalSingle(final @NotNull String identifier) {
			super(identifier, false, Parameter.Splitter.REGULAR, Parameter.Validator.ZERO_OR_ONE);
		}
	}
	
	public static final class RequiredMultiple extends PositionalParameter<@NotNull List<@NotNull String>> {
		public RequiredMultiple(final @NotNull String identifier) {
			super(identifier, true, Parameter.Splitter.REGULAR, Parameter.Validator.ONE_OR_MORE);
		}
	}
	
	public static final class OptionalMultiple extends PositionalParameter<@NotNull List<@NotNull String>> {
		public OptionalMultiple(final @NotNull String identifier) {
			super(identifier, true, Parameter.Splitter.REGULAR, Parameter.Validator.ZERO_OR_MORE);
		}
	}
	
	public static final class RequiredRemainder extends PositionalParameter<@NotNull String> {
		public RequiredRemainder(final @NotNull String identifier) {
			super(identifier, false, Parameter.Splitter.REMAINDER, Parameter.Validator.EXACTLY_ONE);
		}
	}
	
	public static final class OptionalRemainder extends PositionalParameter<@NotNull Optional<String>> {
		public OptionalRemainder(final @NotNull String identifier) {
			super(identifier, false, Parameter.Splitter.REMAINDER, Parameter.Validator.ZERO_OR_ONE);
		}
	}
	
	public PositionalParameter(final @NotNull String identifier, final boolean repeating, final @NotNull Parameter.Splitter splitter, final @NotNull Parameter.Validator<T> validator) {
		super(identifier, repeating, splitter, validator);
	}
}
