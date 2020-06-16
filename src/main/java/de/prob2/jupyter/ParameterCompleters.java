package de.prob2.jupyter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.MoreObjects;

import org.jetbrains.annotations.NotNull;

/**
 * An unmodifiable collection of per-parameter code completion handlers for a command.
 */
public final class ParameterCompleters {
	/**
	 * An empty set of code completion handlers,
	 * for commands that do not accept any arguments or do not implement code completion.
	 */
	public static final @NotNull ParameterCompleters NONE = new ParameterCompleters(Collections.emptyMap());
	
	private final @NotNull Map<@NotNull Parameter<?>, @NotNull Completer> completers;
	
	public ParameterCompleters(final @NotNull Map<@NotNull Parameter<?>, @NotNull Completer> completers) {
		super();
		
		this.completers = Collections.unmodifiableMap(new HashMap<>(completers));
	}
	
	/**
	 * Return the stored mapping of parameters to code completion handlers.
	 * 
	 * @return the stored mapping of parameters to code completion handlers
	 */
	public @NotNull Map<@NotNull Parameter<?>, @NotNull Completer> getCompleters() {
		return this.completers;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("completers", this.getCompleters())
			.toString();
	}
	
	/**
	 * Return the code completion handler for a parameter, if any.
	 * 
	 * @param parameter the parameter for which to look up the code completion handler
	 * @return the code completion handler for a parameter,
	 * or {@link Optional#empty()} if there is none
	 */
	public @NotNull Optional<Completer> getCompleterForParameter(final @NotNull Parameter<?> parameter) {
		if (this.getCompleters().containsKey(parameter)) {
			return Optional.of(this.getCompleters().get(parameter));
		} else {
			return Optional.empty();
		}
	}
}
