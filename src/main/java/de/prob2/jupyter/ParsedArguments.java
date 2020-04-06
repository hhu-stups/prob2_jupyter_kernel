package de.prob2.jupyter;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.MoreObjects;

import org.jetbrains.annotations.NotNull;

public final class ParsedArguments {
	private final @NotNull Map<@NotNull Parameter<?>, Object> values;
	
	public ParsedArguments(final @NotNull Map<@NotNull Parameter<?>, Object> values) {
		super();
		
		this.values = new HashMap<>(values);
	}
	
	public boolean containsKey(final @NotNull Parameter<?> parameter) {
		return this.values.containsKey(parameter);
	}
	
	public <T> T get(final @NotNull Parameter<T> parameter) {
		if (!this.containsKey(parameter)) {
			throw new IllegalArgumentException("No value present for parameter " + parameter);
		}
		@SuppressWarnings("unchecked")
		final T value = (T)this.values.get(parameter);
		return value;
	}
	
	public <T> void put(final @NotNull Parameter<T> parameter, final T value) {
		this.values.put(parameter, value);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("values", this.values)
			.toString();
	}
}
