package de.prob2.jupyter;

import java.util.List;
import java.util.Optional;

import com.google.common.base.MoreObjects;

import org.jetbrains.annotations.NotNull;

abstract class ParameterBase<T> implements Parameter<T> {
	static class RequiredSingle extends ParameterBase<@NotNull String> implements Parameter.RequiredSingle {
		protected RequiredSingle(final @NotNull String identifier) {
			super(identifier);
		}
		
		@Override
		public @NotNull Parameter.Validator<@NotNull String> getValidator() {
			return Parameter.Validator.EXACTLY_ONE;
		}
	}
	
	static class OptionalSingle extends ParameterBase<@NotNull Optional<String>> implements Parameter.OptionalSingle {
		protected OptionalSingle(final @NotNull String identifier) {
			super(identifier);
		}
		
		@Override
		public @NotNull Parameter.Validator<@NotNull Optional<String>> getValidator() {
			return Parameter.Validator.ZERO_OR_ONE;
		}
	}
	
	abstract static class Multiple extends ParameterBase<@NotNull List<@NotNull String>> implements Parameter.Multiple {
		protected Multiple(final @NotNull String identifier) {
			super(identifier);
		}
		
		@Override
		public boolean isRepeating() {
			return true;
		}
	}
	
	private final @NotNull String identifier;
	
	protected ParameterBase(final @NotNull String identifier) {
		super();
		
		this.identifier = identifier;
	}
	
	@Override
	public @NotNull String getIdentifier() {
		return this.identifier;
	}
	
	@Override
	public boolean isRepeating() {
		return false;
	}
	
	@Override
	public @NotNull Parameter.Splitter getSplitter() {
		return Parameter.Splitter.REGULAR;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("identifier", this.getIdentifier())
			.toString();
	}
}
