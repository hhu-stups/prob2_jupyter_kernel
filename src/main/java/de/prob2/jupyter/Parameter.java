package de.prob2.jupyter;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

/**
 * A parameter of a {@link Command}.
 * How a command's arguments are parsed depends on the {@link Parameter} objects returned by its {@link Command#getParameters()} method,
 * as well as the {@link Parameter.Splitter} and {@link Parameter.Validator} objects of those parameters.
 * 
 * @param <T> the type of parsed values for this parameter, as returned by its validator
 */
public interface Parameter<T> {
	/**
	 * The result of a {@link Parameter.Splitter#split(PositionedString)} call.
	 */
	public static final class SplitResult {
		private final @NotNull PositionedString splitArg;
		private final @NotNull PositionedString remainingArgString;
		
		public SplitResult(final @NotNull PositionedString splitArg, final @NotNull PositionedString remainingArgString) {
			super();
			
			this.splitArg = splitArg;
			this.remainingArgString = remainingArgString;
		}
		
		/**
		 * Get the argument value that has been split off by the splitter.
		 * 
		 * @return the argument value that has been split off by the splitter
		 */
		public @NotNull PositionedString getSplitArg() {
			return this.splitArg;
		}
		
		/**
		 * Get the remainder of the argument string that has not been split yet.
		 * If the argument string has been fully split,
		 * this is an empty string positioned at the end of the argument string.
		 * 
		 * @return the remainder of the argument string that has not been split yet
		 */
		public @NotNull PositionedString getRemainingArgString() {
			return this.remainingArgString;
		}
	}
	
	/**
	 * <p>Controls how a {@link Parameter} splits its part of the argument string into separate arguments.</p>
	 * <p>
	 * Splitters do not perform any kind of validation on the argument strings they receive.
	 * They must always return some kind of result without failing,
	 * even when the input string is invalid and would not be accepted by the parameter/command.
	 * This is necessary because splitters are used to partially parse argument strings that may be incomplete or otherwise invalid,
	 * for example to implement code completion and inspection.
	 * Validation of the split arguments is performed separately later,
	 * by the {@link Parameter.Validator} or the {@link Command#run(ParsedArguments)} method.
	 * </p>
	 */
	@FunctionalInterface
	public interface Splitter {
		/**
		 * A {@link Parameter.Splitter} that splits on whitespace.
		 */
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
		
		/**
		 * A {@link Parameter.Splitter} that consumes the entire remaining argument string without splitting.
		 */
		public static final @NotNull Parameter.Splitter REMAINDER = argString -> new SplitResult(argString, argString.substring(argString.getValue().length()));
		
		/**
		 * <p>Split an argument off the given argument string.</p>
		 * <p>
		 * A call to this method must always return some kind of usable result and never fail -
		 * see the documentation of {@link Parameter.Splitter} for details.
		 * </p>
		 * 
		 * @param argString the argument string from which to split a single argument
		 * @return the split argument and the not yet split remainder of the argument string
		 */
		public abstract Parameter.SplitResult split(final @NotNull PositionedString argString);
	}
	
	/**
	 * <p>
	 * Controls how a {@link Parameter}'s argument values are validated after being split by a {@link Splitter}.
	 * After the argument values have been validated,
	 * they may be translated to a different type than the original {@link List}{@code <}{@link String}{@code >}.
	 * For example,
	 * single-value parameters are represented as a {@link String} or {@link Optional}{@code <}{@link String}{@code >},
	 * depending on whether they are required or optional.
	 * </p>
	 * 
	 * @param <T> the type to which the argument values are translated after validation
	 */
	@FunctionalInterface
	public interface Validator<T> {
		/**
		 * A {@link Parameter.Validator} that requires exactly one argument value to be present for the parameter.
		 * The single argument value is returned as a {@link String}.
		 */
		public static final @NotNull Parameter.Validator<@NotNull String> EXACTLY_ONE = (param, argValues) -> {
			if (argValues.isEmpty()) {
				throw new UserErrorException("Missing required parameter " + param.getIdentifier());
			} else if (argValues.size() > 1) {
				throw new UserErrorException("Non-repeating parameter " + param.getIdentifier() + " cannot appear more than once");
			}
			
			return argValues.get(0).getValue();
		};
		
		/**
		 * A {@link Parameter.Validator} that requires at most one argument value to be present for the parameter.
		 * The argument value is represented as an {@link Optional}{@code <}{@link String}{@code >},
		 * which contains the argument value if it was present,
		 * or is empty otherwise.
		 */
		public static final @NotNull Parameter.Validator<@NotNull Optional<String>> ZERO_OR_ONE = (param, argValues) -> {
			if (argValues.size() > 1) {
				throw new UserErrorException("Non-repeating parameter " + param.getIdentifier() + " cannot appear more than once");
			}
			
			return argValues.stream().findAny().map(PositionedString::getValue);
		};
		
		/**
		 * A {@link Parameter.Validator} that requires one or more argument value to be present for the parameter.
		 * The argument values are returned as a {@link List}{@code <}{@link String}{@code >},
		 * which is never empty.
		 */
		public static final @NotNull Parameter.Validator<@NotNull List<@NotNull String>> ONE_OR_MORE = (param, argValues) -> {
			if (argValues.isEmpty()) {
				throw new UserErrorException("Missing required parameter " + param.getIdentifier());
			}
			
			return argValues.stream()
				.map(PositionedString::getValue)
				.collect(Collectors.toList());
		};
		
		/**
		 * A {@link Parameter.Validator} that requires zero or more argument value to be present for the parameter.
		 * This validator effectively performs no validation.
		 * The argument values are returned as a {@link List}{@code <}{@link String}{@code >},
		 * which may be empty.
		 */
		public static final @NotNull Parameter.Validator<@NotNull List<@NotNull String>> ZERO_OR_MORE = (param, argValues) -> argValues.stream()
			.map(PositionedString::getValue)
			.collect(Collectors.toList());
		
		/**
		 * Validate the given split argument values and translate them to this validator's parsed value type.
		 * 
		 * @param param the identifier of the parameter to which this validator belongs (only for use in error and debugging messages)
		 * @param argValues the split argument values to validate
		 * @return the argument values in parsed form after validation
		 * @throws UserErrorException if the split argument values are not valid
		 */
		public abstract T validate(final @NotNull Parameter<T> param, final @NotNull List<@NotNull PositionedString> argValues);
	}
	
	/**
	 * A parameter which results in a single value after validation.
	 */
	public interface RequiredSingle extends Parameter<@NotNull String> {}
	
	/**
	 * A parameter which results in an optional value after validation.
	 */
	public interface OptionalSingle extends Parameter<@NotNull Optional<String>> {}
	
	/**
	 * A parameter which results in a variable number of values after validation.
	 */
	public interface Multiple extends Parameter<@NotNull List<@NotNull String>> {}
	
	/**
	 * <p>Return an internal identifier for this parameter.</p>
	 * <p>
	 * This identifier should only be used in error messages and for debugging purposes.
	 * It is <em>not</em> used to programmatically identify the parameter during argument splitting/validation/parsing -
	 * instead,
	 * the parameter object itself is used as the identifier,
	 * for example in the maps in {@link SplitArguments} or {@link ParsedArguments}.
	 * Because of this,
	 * parameter identifiers are technically not required to be unique,
	 * even within a single command,
	 * although this is strongly recommended.
	 * </p>
	 * 
	 * @return an internal identifier for this parameter
	 */
	public abstract @NotNull String getIdentifier();
	
	/**
	 * <p>
	 * Return whether this parameter is repeating.
	 * If false,
	 * the argument parser will use this parameter's splitter to split an argument once,
	 * and then continue with the next parameters to parse the following arguments.
	 * If true,
	 * the argument parser will use this parameter's splitter repeatedly to split the remainder of the argument string.
	 * </p>
	 * <p>
	 * This setting has no meaning in some cases,
	 * for example for body parameters,
	 * or parameters whose splitter always consumes the entire remaining argument string anyway.
	 * </p>
	 * 
	 * @return whether this parameter is repeating
	 */
	public abstract boolean isRepeating();
	
	/**
	 * Return this parameter's splitter.
	 * See the documentation of {@link Parameter.Splitter} for details about the role of splitters in argument parsing.
	 * 
	 * @return this parameter's splitter
	 */
	public abstract @NotNull Parameter.Splitter getSplitter();
	
	/**
	 * Return this parameter's validator.
	 * See the documentation of {@link Parameter.Validator} for details about the role of validators in argument parsing.
	 *
	 * @return this parameter's validator
	 */
	public abstract @NotNull Parameter.Validator<T> getValidator();
	
	/**
	 * Create a new parameter that accepts a single value and must always be present.
	 * 
	 * @param identifier an identifier for the parameter
	 * @return a required single-value parameter
	 */
	public static Parameter.RequiredSingle required(final String identifier) {
		return new ParameterBase.RequiredSingle(identifier);
	}
	
	/**
	 * Create a new parameter that accepts a single value and might not be present.
	 *
	 * @param identifier an identifier for the parameter
	 * @return an optional single-value parameter
	 */
	public static Parameter.OptionalSingle optional(final String identifier) {
		return new ParameterBase.OptionalSingle(identifier);
	}
	
	/**
	 * Create a new parameter that accepts multiple values and must always have at least one value present.
	 *
	 * @param identifier an identifier for the parameter
	 * @return a required multi-value parameter
	 */
	public static Parameter.Multiple requiredMultiple(final String identifier) {
		return new ParameterBase.Multiple(identifier) {
			@Override
			public @NotNull Parameter.Validator<@NotNull List<@NotNull String>> getValidator() {
				return Parameter.Validator.ONE_OR_MORE;
			}
		};
	}
	
	/**
	 * Create a new parameter that accepts multiple values and might not have any value present.
	 *
	 * @param identifier an identifier for the parameter
	 * @return an optional multi-value parameter
	 */
	public static Parameter.Multiple optionalMultiple(final String identifier) {
		return new ParameterBase.Multiple(identifier) {
			@Override
			public @NotNull Parameter.Validator<@NotNull List<@NotNull String>> getValidator() {
				return Parameter.Validator.ZERO_OR_MORE;
			}
		};
	}
	
	/**
	 * Create a new parameter that consumes the entire remaining argument string as a single value and must always be present.
	 *
	 * @param identifier an identifier for the parameter
	 * @return a required remainder parameter
	 */
	public static Parameter.RequiredSingle requiredRemainder(final String identifier) {
		return new ParameterBase.RequiredSingle(identifier) {
			@Override
			public @NotNull Parameter.Splitter getSplitter() {
				return Parameter.Splitter.REMAINDER;
			}
		};
	}
	
	/**
	 * Create a new parameter that consumes the entire remaining argument string as a single value and might not be present.
	 *
	 * @param identifier an identifier for the parameter
	 * @return an optional remainder parameter
	 */
	public static Parameter.OptionalSingle optionalRemainder(final String identifier) {
		return new ParameterBase.OptionalSingle(identifier) {
			@Override
			public @NotNull Parameter.Splitter getSplitter() {
				return Parameter.Splitter.REMAINDER;
			}
		};
	}
	
	/**
	 * Create a body parameter that must always be present.
	 * 
	 * @param identifier an identifier for the parameter
	 * @return a required body parameter
	 */
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
