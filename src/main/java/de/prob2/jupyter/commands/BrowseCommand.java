package de.prob2.jupyter.commands;

import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.BEvent;
import de.prob.model.representation.Constant;
import de.prob.model.representation.Set;
import de.prob.model.representation.Variable;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.messages.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class BrowseCommand implements LineCommand {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private BrowseCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":browse";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Show information about the current state";
	}
	
	private static <T extends AbstractElement> @NotNull String elementsToString(
		final @NotNull AbstractElement element,
		final @NotNull Class<T> clazz,
		final @NotNull Function<@NotNull T, @NotNull String> func
	) {
		final String elements = element.getChildrenOfType(clazz).stream().map(func).collect(Collectors.joining(", "));
		return elements.isEmpty() ? "(none)" : elements;
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String name, final @NotNull String argString) {
		if (!argString.isEmpty()) {
			throw new CommandExecutionException(name, "Unexpected argument: " + argString);
		}
		
		final Trace trace = this.animationSelector.getCurrentTrace();
		final StringBuilder sb = new StringBuilder("Machine: ");
		final AbstractElement mainComponent = trace.getStateSpace().getMainComponent();
		sb.append(mainComponent);
		sb.append("\nSets: ");
		sb.append(elementsToString(mainComponent, Set.class, Set::getName));
		sb.append("\nConstants: ");
		sb.append(elementsToString(mainComponent, Constant.class, Object::toString));
		sb.append("\nVariables: ");
		sb.append(elementsToString(mainComponent, Variable.class, Variable::getName));
		sb.append("\nOperations: ");
		sb.append(elementsToString(mainComponent, BEvent.class, Object::toString));
		sb.append('\n');
		return new DisplayData(sb.toString());
	}
}
