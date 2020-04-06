package de.prob2.jupyter;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

public abstract class PositionalParameter<T> extends Parameter<T> {
	public static final class RequiredSingle extends PositionalParameter<@NotNull String> {
		public RequiredSingle(final @NotNull String identifier) {
			super(identifier);
		}
		
		@Override
		public boolean isOptional() {
			return false;
		}
		
		@Override
		public @NotNull String getDefaultValue() {
			throw new UnsupportedOperationException("Not an optional parameter");
		}
		
		@Override
		public @NotNull Parameter.ParseResult<@NotNull String> parse(final @NotNull String argString) {
			final String[] split = CommandUtils.ARG_SPLIT_PATTERN.split(argString, 2);
			return new Parameter.ParseResult<>(split[0], split.length > 1 ? split[1] : "");
		}
	}
	
	public static final class OptionalSingle extends PositionalParameter<@NotNull Optional<String>> {
		public OptionalSingle(final @NotNull String identifier) {
			super(identifier);
		}
		
		@Override
		public boolean isOptional() {
			return true;
		}
		
		@Override
		public @NotNull Optional<String> getDefaultValue() {
			return Optional.empty();
		}
		
		@Override
		public @NotNull Parameter.ParseResult<@NotNull Optional<String>> parse(final @NotNull String argString) {
			final String[] split = CommandUtils.ARG_SPLIT_PATTERN.split(argString, 2);
			return new Parameter.ParseResult<>(Optional.of(split[0]), split.length > 1 ? split[1] : "");
		}
	}
	
	public static final class RequiredRemainder extends PositionalParameter<@NotNull String> {
		public RequiredRemainder(final @NotNull String identifier) {
			super(identifier);
		}
		
		@Override
		public boolean isOptional() {
			return false;
		}
		
		@Override
		public @NotNull String getDefaultValue() {
			throw new UnsupportedOperationException("Not an optional parameter");
		}
		
		@Override
		public Parameter.ParseResult<@NotNull String> parse(final @NotNull String argString) {
			return new Parameter.ParseResult<>(argString, "");
		}
	}
	
	public static final class OptionalRemainder extends PositionalParameter<@NotNull Optional<String>> {
		public OptionalRemainder(final @NotNull String identifier) {
			super(identifier);
		}
		
		@Override
		public boolean isOptional() {
			return true;
		}
		
		@Override
		public @NotNull Optional<String> getDefaultValue() {
			return Optional.empty();
		}
		
		@Override
		public Parameter.ParseResult<@NotNull Optional<String>> parse(final @NotNull String argString) {
			return new Parameter.ParseResult<>(Optional.of(argString), "");
		}
	}
	
	protected PositionalParameter(final @NotNull String identifier) {
		super(identifier);
	}
	
}
