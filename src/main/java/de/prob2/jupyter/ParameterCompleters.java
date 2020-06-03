package de.prob2.jupyter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.MoreObjects;

import org.jetbrains.annotations.NotNull;

public final class ParameterCompleters {
	public static final @NotNull ParameterCompleters NONE = new ParameterCompleters(Collections.emptyMap());
	
	private final @NotNull Map<@NotNull Parameter<?>, CommandUtils.@NotNull Completer> completers;
	
	public ParameterCompleters(final @NotNull Map<@NotNull Parameter<?>, CommandUtils.@NotNull Completer> completers) {
		super();
		
		this.completers = Collections.unmodifiableMap(new HashMap<>(completers));
	}
	
	public @NotNull Map<@NotNull Parameter<?>, CommandUtils.@NotNull Completer> getCompleters() {
		return this.completers;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("completers", this.getCompleters())
			.toString();
	}
	
	public @NotNull Optional<CommandUtils.Completer> getCompleterForParameter(final @NotNull Parameter<?> parameter) {
		if (this.getCompleters().containsKey(parameter)) {
			return Optional.of(this.getCompleters().get(parameter));
		} else {
			return Optional.empty();
		}
	}
}
