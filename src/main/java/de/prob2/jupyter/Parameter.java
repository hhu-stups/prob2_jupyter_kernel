package de.prob2.jupyter;

import java.util.List;
import java.util.Optional;

import com.google.common.base.MoreObjects;

import org.jetbrains.annotations.NotNull;

public abstract class Parameter<T> {
	public static final class SplitResult {
		private final @NotNull String splitArg;
		private final @NotNull String remainingArgString;
		
		public SplitResult(final @NotNull String splitArg, final @NotNull String remainingArgString) {
			super();
			
			this.splitArg = splitArg;
			this.remainingArgString = remainingArgString;
		}
		
		public @NotNull String getSplitArg() {
			return this.splitArg;
		}
		
		public @NotNull String getRemainingArgString() {
			return this.remainingArgString;
		}
	}
	
	public interface Splitter {
		public static final @NotNull Parameter.Splitter REGULAR = argString -> {
			final String[] split = CommandUtils.ARG_SPLIT_PATTERN.split(argString, 2);
			return new SplitResult(split[0], split.length > 1 ? split[1] : "");
		};
		public static final @NotNull Parameter.Splitter REMAINDER = argString -> new SplitResult(argString, "");
		
		public abstract Parameter.SplitResult split(final @NotNull String argString);
	}
	
	public interface Validator<T> {
		public static final @NotNull Parameter.Validator<@NotNull String> EXACTLY_ONE = (param, argValues) -> {
			if (argValues.isEmpty()) {
				throw new UserErrorException("Missing required parameter " + param.getIdentifier());
			} else if (argValues.size() > 1) {
				throw new UserErrorException("Non-repeating parameter " + param.getIdentifier() + " cannot appear more than once");
			}
			
			return argValues.get(0);
		};
		public static final @NotNull Parameter.Validator<@NotNull Optional<String>> ZERO_OR_ONE = (param, argValues) -> {
			if (argValues.size() > 1) {
				throw new UserErrorException("Non-repeating parameter " + param.getIdentifier() + " cannot appear more than once");
			}
			
			return argValues.stream().findAny();
		};
		public static final @NotNull Parameter.Validator<@NotNull List<@NotNull String>> ONE_OR_MORE = (param, argValues) -> {
			if (argValues.isEmpty()) {
				throw new UserErrorException("Missing required parameter " + param.getIdentifier());
			}
			
			return argValues;
		};
		public static final @NotNull Parameter.Validator<@NotNull List<@NotNull String>> ZERO_OR_MORE = (param, argValues) -> argValues;
		
		public abstract T validate(final @NotNull Parameter<T> param, final @NotNull List<@NotNull String> argValues);
	}
	
	private final @NotNull String identifier;
	private final boolean repeating;
	private final @NotNull Parameter.Splitter splitter;
	private final @NotNull Parameter.Validator<T> validator;
	
	protected Parameter(final @NotNull String identifier, final boolean repeating, final @NotNull Parameter.Splitter splitter, final @NotNull Parameter.Validator<T> validator) {
		super();
		
		this.identifier = identifier;
		this.repeating = repeating;
		this.splitter = splitter;
		this.validator = validator;
	}
	
	public @NotNull String getIdentifier() {
		return this.identifier;
	}
	
	public boolean isRepeating() {
		return this.repeating;
	}
	
	public @NotNull Parameter.Splitter getSplitter() {
		return this.splitter;
	}
	
	public @NotNull Parameter.Validator<T> getValidator() {
		return this.validator;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("identifier", this.getIdentifier())
			.add("repeating", this.isRepeating())
			.add("splitter", this.getSplitter())
			.add("validator", this.getValidator())
			.toString();
	}
}
