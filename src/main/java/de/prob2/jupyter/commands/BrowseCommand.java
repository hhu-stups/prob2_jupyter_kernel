package de.prob2.jupyter.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.Constant;
import de.prob.model.representation.Set;
import de.prob.model.representation.Variable;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;

import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import se.sawano.java.text.AlphanumericComparator;

public final class BrowseCommand implements Command {
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
		return "Show information about the current state.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "The output shows the names of all sets, constants, and variables defined by the current machine, as well as a list of transitions that are available in the current state. Each transition has a numeric ID, which can be passed to `:exec` to execute that transition.";
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
	public @NotNull DisplayData run(final @NotNull String argString) {
		if (!argString.isEmpty()) {
			throw new UserErrorException("Unexpected argument: " + argString);
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
		final List<Transition> sortedTransitions = new ArrayList<>(trace.getNextTransitions(true, FormulaExpand.TRUNCATE));
		// Transition IDs are strings, but they almost always contain numbers.
		sortedTransitions.sort(Comparator.comparing(Transition::getId, new AlphanumericComparator()));
		for (final Transition t : sortedTransitions) {
			sb.append('\n');
			sb.append(t.getId());
			sb.append(": ");
			sb.append(t.getRep());
		}
		if (trace.getCurrentState().isMaxTransitionsCalculated()) {
			sb.append("\nMore operations may be available (MAX_OPERATIONS/MAX_INITIALISATIONS reached)");
		}
		return new DisplayData(sb.toString());
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		return null;
	}
}
