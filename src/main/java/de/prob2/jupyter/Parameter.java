package de.prob2.jupyter;

import java.util.List;

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
	
	private final @NotNull String identifier;
	
	protected Parameter(final @NotNull String identifier) {
		super();
		
		this.identifier = identifier;
	}
	
	public @NotNull String getIdentifier() {
		return this.identifier;
	}
	
	public abstract boolean isRepeating();
	
	public abstract Parameter.SplitResult split(final @NotNull String argString);
	
	public abstract T validate(final @NotNull List<@NotNull String> argValues);
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("identifier", this.getIdentifier())
			.toString();
	}
}
