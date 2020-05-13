package de.prob2.jupyter;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

public abstract class PositionalParameter<T> extends Parameter<T> {
	public abstract static class ExactlyOne extends PositionalParameter<@NotNull String> {
		protected ExactlyOne(final @NotNull String identifier) {
			super(identifier);
		}
		
		@Override
		public boolean isRepeating() {
			return false;
		}
		
		@Override
		public @NotNull String validate(final @NotNull List<@NotNull String> argValues) {
			if (argValues.isEmpty()) {
				throw new UserErrorException("Missing required parameter " + this.getIdentifier());
			} else if (argValues.size() > 1) {
				throw new AssertionError("Regular (single) required parameter " + this.getIdentifier() + " has more than one value, this should never happen!");
			}
			
			return argValues.get(0);
		}
	}
	
	public abstract static class ZeroOrOne extends PositionalParameter<@NotNull Optional<String>> {
		protected ZeroOrOne(final @NotNull String identifier) {
			super(identifier);
		}
		
		@Override
		public boolean isRepeating() {
			return false;
		}
		
		@Override
		public @NotNull Optional<String> validate(final @NotNull List<@NotNull String> argValues) {
			if (argValues.size() > 1) {
				throw new AssertionError("Regular (single) optional parameter " + this.getIdentifier() + " has more than one value, this should never happen!");
			}
			
			return argValues.stream().findAny();
		}
	}
	
	public static final class RequiredSingle extends PositionalParameter.ExactlyOne {
		public RequiredSingle(final @NotNull String identifier) {
			super(identifier);
		}
		
		@Override
		public @NotNull Parameter.SplitResult split(final @NotNull String argString) {
			return splitOnce(argString);
		}
	}
	
	public static final class OptionalSingle extends PositionalParameter.ZeroOrOne {
		public OptionalSingle(final @NotNull String identifier) {
			super(identifier);
		}
		
		@Override
		public @NotNull Parameter.SplitResult split(final @NotNull String argString) {
			return splitOnce(argString);
		}
	}
	
	public static final class RequiredMultiple extends PositionalParameter<@NotNull List<@NotNull String>> {
		public RequiredMultiple(final @NotNull String identifier) {
			super(identifier);
		}
		
		@Override
		public boolean isRepeating() {
			return true;
		}
		
		@Override
		public @NotNull Parameter.SplitResult split(final @NotNull String argString) {
			return splitOnce(argString);
		}
		
		@Override
		public @NotNull List<@NotNull String> validate(final @NotNull List<@NotNull String> argValues) {
			if (argValues.isEmpty()) {
				throw new UserErrorException("Missing required parameter " + this.getIdentifier());
			}
			
			return argValues;
		}
	}
	
	public static final class OptionalMultiple extends PositionalParameter<@NotNull List<@NotNull String>> {
		public OptionalMultiple(final @NotNull String identifier) {
			super(identifier);
		}
		
		@Override
		public boolean isRepeating() {
			return true;
		}
		
		@Override
		public @NotNull Parameter.SplitResult split(final @NotNull String argString) {
			return splitOnce(argString);
		}
		
		@Override
		public @NotNull List<@NotNull String> validate(final @NotNull List<@NotNull String> argValues) {
			return argValues;
		}
	}
	
	public static final class RequiredRemainder extends PositionalParameter.ExactlyOne {
		public RequiredRemainder(final @NotNull String identifier) {
			super(identifier);
		}
		
		@Override
		public @NotNull Parameter.SplitResult split(final @NotNull String argString) {
			return new Parameter.SplitResult(argString, "");
		}
	}
	
	public static final class OptionalRemainder extends PositionalParameter.ZeroOrOne {
		public OptionalRemainder(final @NotNull String identifier) {
			super(identifier);
		}
		
		@Override
		public @NotNull Parameter.SplitResult split(final @NotNull String argString) {
			return new Parameter.SplitResult(argString, "");
		}
	}
	
	protected PositionalParameter(final @NotNull String identifier) {
		super(identifier);
	}
	
	@NotNull
	static Parameter.SplitResult splitOnce(final @NotNull String argString) {
		final String[] split = CommandUtils.ARG_SPLIT_PATTERN.split(argString, 2);
		return new Parameter.SplitResult(split[0], split.length > 1 ? split[1] : "");
	}
}
