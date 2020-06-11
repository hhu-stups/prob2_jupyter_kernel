package de.prob2.jupyter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.MoreObjects;

import org.jetbrains.annotations.NotNull;

/**
 * An unmodifiable collection of per-parameter inspection handlers for a command.
 */
public final class ParameterInspectors {
	/**
	 * An empty set of inspection handlers,
	 * for commands that do not accept any arguments or do not implement inspection.
	 */
	public static final @NotNull ParameterInspectors NONE = new ParameterInspectors(Collections.emptyMap());
	
	private final @NotNull Map<@NotNull Parameter<?>, CommandUtils.@NotNull Inspector> inspectors;
	
	public ParameterInspectors(final @NotNull Map<@NotNull Parameter<?>, CommandUtils.@NotNull Inspector> inspectors) {
		super();
		
		this.inspectors = Collections.unmodifiableMap(new HashMap<>(inspectors));
	}
	
	/**
	 * Return the stored mapping of parameters to inspection handlers.
	 *
	 * @return the stored mapping of parameters to inspection handlers
	 */
	public @NotNull Map<@NotNull Parameter<?>, CommandUtils.@NotNull Inspector> getInspectors() {
		return this.inspectors;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("inspectors", this.getInspectors())
			.toString();
	}
	
	/**
	 * Return the inspection handler for a parameter, if any.
	 *
	 * @param parameter the parameter for which to look up the inspection handler
	 * @return the inspection handler for a parameter,
	 * or {@link Optional#empty()} if there is none
	 */
	public @NotNull Optional<CommandUtils.Inspector> getInspectorForParameter(final @NotNull Parameter<?> parameter) {
		if (this.getInspectors().containsKey(parameter)) {
			return Optional.of(this.getInspectors().get(parameter));
		} else {
			return Optional.empty();
		}
	}
}
