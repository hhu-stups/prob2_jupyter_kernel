package de.prob2.jupyter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.MoreObjects;

import org.jetbrains.annotations.NotNull;

public final class ParameterInspectors {
	public static final @NotNull ParameterInspectors NONE = new ParameterInspectors(Collections.emptyMap());
	
	private final @NotNull Map<@NotNull Parameter<?>, CommandUtils.@NotNull Inspector> inspectors;
	
	public ParameterInspectors(final @NotNull Map<@NotNull Parameter<?>, CommandUtils.@NotNull Inspector> inspectors) {
		super();
		
		this.inspectors = Collections.unmodifiableMap(new HashMap<>(inspectors));
	}
	
	public @NotNull Map<@NotNull Parameter<?>, CommandUtils.@NotNull Inspector> getInspectors() {
		return this.inspectors;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("inspectors", this.getInspectors())
			.toString();
	}
	
	public @NotNull Optional<CommandUtils.Inspector> getInspectorForParameter(final @NotNull Parameter<?> parameter) {
		if (this.getInspectors().containsKey(parameter)) {
			return Optional.of(this.getInspectors().get(parameter));
		} else {
			return Optional.empty();
		}
	}
}
