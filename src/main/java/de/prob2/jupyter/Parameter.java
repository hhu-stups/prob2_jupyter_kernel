package de.prob2.jupyter;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

public interface Parameter<T> {
	public static final class SplitResult {
		private final @NotNull PositionedString splitArg;
		private final @NotNull PositionedString remainingArgString;
		
		public SplitResult(final @NotNull PositionedString splitArg, final @NotNull PositionedString remainingArgString) {
			super();
			
			this.splitArg = splitArg;
			this.remainingArgString = remainingArgString;
		}
		
		public @NotNull PositionedString getSplitArg() {
			return this.splitArg;
		}
		
		public @NotNull PositionedString getRemainingArgString() {
			return this.remainingArgString;
		}
	}
	
	public interface Splitter {
		public static final @NotNull Parameter.Splitter REGULAR = argString -> {
			final Matcher argSplitMatcher = CommandUtils.ARG_SPLIT_PATTERN.matcher(argString.getValue());
			final PositionedString splitArg;
			final PositionedString remainingArgString;
			if (argSplitMatcher.find()) {
				splitArg = argString.substring(0, argSplitMatcher.start());
				remainingArgString = argString.substring(argSplitMatcher.end());
			} else {
				splitArg = argString;
				remainingArgString = argString.substring(argString.getValue().length());
			}
			return new SplitResult(splitArg, remainingArgString);
		};
		public static final @NotNull Parameter.Splitter REMAINDER = argString -> new SplitResult(argString, argString.substring(argString.getValue().length()));
		
		public abstract Parameter.SplitResult split(final @NotNull PositionedString argString);
	}
	
	public interface Validator<T> {
		public static final @NotNull Parameter.Validator<@NotNull String> EXACTLY_ONE = (param, argValues) -> {
			if (argValues.isEmpty()) {
				throw new UserErrorException("Missing required parameter " + param.getIdentifier());
			} else if (argValues.size() > 1) {
				throw new UserErrorException("Non-repeating parameter " + param.getIdentifier() + " cannot appear more than once");
			}
			
			return argValues.get(0).getValue();
		};
		public static final @NotNull Parameter.Validator<@NotNull Optional<String>> ZERO_OR_ONE = (param, argValues) -> {
			if (argValues.size() > 1) {
				throw new UserErrorException("Non-repeating parameter " + param.getIdentifier() + " cannot appear more than once");
			}
			
			return argValues.stream().findAny().map(PositionedString::getValue);
		};
		public static final @NotNull Parameter.Validator<@NotNull List<@NotNull String>> ONE_OR_MORE = (param, argValues) -> {
			if (argValues.isEmpty()) {
				throw new UserErrorException("Missing required parameter " + param.getIdentifier());
			}
			
			return argValues.stream()
				.map(PositionedString::getValue)
				.collect(Collectors.toList());
		};
		public static final @NotNull Parameter.Validator<@NotNull List<@NotNull String>> ZERO_OR_MORE = (param, argValues) -> argValues.stream()
			.map(PositionedString::getValue)
			.collect(Collectors.toList());
		
		public abstract T validate(final @NotNull Parameter<T> param, final @NotNull List<@NotNull PositionedString> argValues);
	}
	
	public interface RequiredSingle extends Parameter<@NotNull String> {}
	
	public interface OptionalSingle extends Parameter<@NotNull Optional<String>> {}
	
	public interface Multiple extends Parameter<@NotNull List<@NotNull String>> {}
	
	public abstract @NotNull String getIdentifier();
	
	public abstract boolean isRepeating();
	
	public abstract @NotNull Parameter.Splitter getSplitter();
	
	public abstract @NotNull Parameter.Validator<T> getValidator();
	
	public static Parameter.RequiredSingle required(final String identifier) {
		return new ParameterBase.RequiredSingle(identifier);
	}
	
	public static Parameter.OptionalSingle optional(final String identifier) {
		return new ParameterBase.OptionalSingle(identifier);
	}
	
	public static Parameter.Multiple requiredMultiple(final String identifier) {
		return new ParameterBase.Multiple(identifier) {
			@Override
			public @NotNull Parameter.Validator<@NotNull List<@NotNull String>> getValidator() {
				return Parameter.Validator.ONE_OR_MORE;
			}
		};
	}
	
	public static Parameter.Multiple optionalMultiple(final String identifier) {
		return new ParameterBase.Multiple(identifier) {
			@Override
			public @NotNull Parameter.Validator<@NotNull List<@NotNull String>> getValidator() {
				return Parameter.Validator.ZERO_OR_MORE;
			}
		};
	}
	
	public static Parameter.RequiredSingle requiredRemainder(final String identifier) {
		return new ParameterBase.RequiredSingle(identifier) {
			@Override
			public @NotNull Parameter.Splitter getSplitter() {
				return Parameter.Splitter.REMAINDER;
			}
		};
	}
	
	public static Parameter.OptionalSingle optionalRemainder(final String identifier) {
		return new ParameterBase.OptionalSingle(identifier) {
			@Override
			public @NotNull Parameter.Splitter getSplitter() {
				return Parameter.Splitter.REMAINDER;
			}
		};
	}
	
	public static Parameter.RequiredSingle body(final String identifier) {
		return new ParameterBase.RequiredSingle(identifier) {
			@Override
			public @NotNull Parameter.Splitter getSplitter() {
				return argString -> {
					throw new AssertionError("Splitter of a body parameter should never be used");
				};
			}
			
			@Override
			public @NotNull Parameter.Validator<@NotNull String> getValidator() {
				return (param, argValues) -> {
					if (argValues.isEmpty()) {
						throw new UserErrorException("Missing required body " + param.getIdentifier());
					} else if (argValues.size() > 1) {
						throw new AssertionError("Body " + param.getIdentifier() + " appeared more than once, this should never happen!");
					}
					
					return argValues.get(0).getValue();
				};
			}
		};
	}
}
