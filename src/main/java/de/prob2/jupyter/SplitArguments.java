package de.prob2.jupyter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.MoreObjects;

import org.jetbrains.annotations.NotNull;

/**
 * A collection of command arguments that have been split and mapped to their parameters,
 * but not fully validated yet.
 * Internally this is a simple map of parameter objects to lists of strings,
 * but this class provides a more convenient interface for the specialized purpose of this map.
 */
public final class SplitArguments {
	private final @NotNull Map<@NotNull Parameter<?>, @NotNull List<@NotNull PositionedString>> values;
	
	public SplitArguments(final @NotNull Map<@NotNull Parameter<?>, @NotNull List<@NotNull PositionedString>> values) {
		super();
		
		this.values = new HashMap<>(values);
	}
	
	/**
	 * Return whether any values have been associated with the given parameter.
	 * 
	 * @param parameter the parameter for which to check for values
	 * @return whether any values have been associated with the given parameter
	 */
	public boolean containsKey(final @NotNull Parameter<?> parameter) {
		return this.values.containsKey(parameter);
	}
	
	/**
	 * Return the list of values associated with the given parameter.
	 * If there are no associated values
	 * (i. e. {@link #containsKey(Parameter)} returns {@code false} for the parameter),
	 * no exception is thrown and an empty list is returned. 
	 * 
	 * @param parameter the parameter for which to get values
	 * @return the list of values associated with the given parameter,
	 * or an empty list if there are none
	 */
	public @NotNull List<@NotNull PositionedString> get(final @NotNull Parameter<?> parameter) {
		return this.values.getOrDefault(parameter, Collections.emptyList());
	}
	
	/**
	 * Add a new value to the list of values associated with the given parameter.
	 * 
	 * @param parameter the parameter with which to associate the value
	 * @param value the value to add
	 */
	public void add(final @NotNull Parameter<?> parameter, final PositionedString value) {
		this.values.computeIfAbsent(parameter, p -> new ArrayList<>()).add(value);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("values", this.values)
			.toString();
	}
}
