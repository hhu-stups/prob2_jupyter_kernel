package de.prob2.jupyter;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.MoreObjects;

import org.jetbrains.annotations.NotNull;

/**
 * A collection of parsed and validated command arguments,
 * mapped to their parameters.
 * Internally this is a map of parameters to arbitrary objects,
 * but the methods of this class ensure that parameters can only be mapped to their declared parsed value type.
 */
public final class ParsedArguments {
	private final @NotNull Map<@NotNull Parameter<?>, Object> values;
	
	public ParsedArguments(final @NotNull Map<@NotNull Parameter<?>, Object> values) {
		super();
		
		this.values = new HashMap<>(values);
	}
	
	/**
	 * Return whether a value has been associated with the given parameter.
	 *
	 * @param parameter the parameter for which to check for a value
	 * @return whether a value has been associated with the given parameter
	 */
	public boolean containsKey(final @NotNull Parameter<?> parameter) {
		return this.values.containsKey(parameter);
	}
	
	/**
	 * Return the parsed value associated with the given parameter.
	 * 
	 * @param parameter the parameter for which to return the value
	 * @param <T> the type of parsed value for the parameter
	 * @return the parsed value associated with the given parameter
	 */
	public <T> T get(final @NotNull Parameter<T> parameter) {
		if (!this.containsKey(parameter)) {
			throw new IllegalArgumentException("No value present for parameter " + parameter);
		}
		@SuppressWarnings("unchecked")
		final T value = (T)this.values.get(parameter);
		return value;
	}
	
	/**
	 * Associate a parsed value with the given parameter.
	 * 
	 * @param parameter the parameter with which to associate the value
	 * @param value the parsed value to associate with the parameter
	 * @param <T> the type of parsed value for the parameter
	 */
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
