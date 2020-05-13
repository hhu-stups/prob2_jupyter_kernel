package de.prob2.jupyter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.MoreObjects;

import org.jetbrains.annotations.NotNull;

public final class SplitArguments {
	private final @NotNull Map<@NotNull Parameter<?>, @NotNull List<@NotNull String>> values;
	
	public SplitArguments(final @NotNull Map<@NotNull Parameter<?>, @NotNull List<@NotNull String>> values) {
		super();
		
		this.values = new HashMap<>(values);
	}
	
	public boolean containsKey(final @NotNull Parameter<?> parameter) {
		return this.values.containsKey(parameter);
	}
	
	public @NotNull List<@NotNull String> get(final @NotNull Parameter<?> parameter) {
		return this.values.getOrDefault(parameter, Collections.emptyList());
	}
	
	public void add(final @NotNull Parameter<?> parameter, final String value) {
		this.values.computeIfAbsent(parameter, p -> new ArrayList<>()).add(value);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("values", this.values)
			.toString();
	}
}
