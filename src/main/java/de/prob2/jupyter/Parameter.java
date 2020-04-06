package de.prob2.jupyter;

import com.google.common.base.MoreObjects;

import org.jetbrains.annotations.NotNull;

public abstract class Parameter<T> {
	public static final class ParseResult<T> {
		private final T parsedArg;
		private final @NotNull String remainingArgString;
		
		public ParseResult(final T parsedArg, final @NotNull String remainingArgString) {
			super();
			
			this.parsedArg = parsedArg;
			this.remainingArgString = remainingArgString;
		}
		
		public T getParsedArg() {
			return this.parsedArg;
		}
		
		public @NotNull String getRemainingArgString() {
			return this.remainingArgString;
		}
	}
	
	private final @NotNull String identifier;
	
	protected Parameter(final @NotNull String identifier) {
		super();
		
		this.identifier = identifier;
	}
	
	public @NotNull String getIdentifier() {
		return this.identifier;
	}
	
	public abstract boolean isOptional();
	
	public abstract T getDefaultValue();
	
	public abstract Parameter.ParseResult<T> parse(final @NotNull String argString);
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("identifier", this.getIdentifier())
			.toString();
	}
}
