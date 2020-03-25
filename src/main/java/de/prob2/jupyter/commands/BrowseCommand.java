package de.prob2.jupyter.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.LoadedMachine;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;

import de.prob2.jupyter.Command;
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
	public @NotNull String getName() {
		return ":browse";
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
		return "The output shows the names of all sets, constants, and variables defined by the current machine, as well as a list of enabled operations (and possible parameter values) in the current state.";
	}
	
	private static @NotNull String listToString(final List<String> list) {
		return list.isEmpty() ? "(none)" : String.join(", ", list);
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		if (!argString.isEmpty()) {
			throw new UserErrorException("Unexpected argument: " + argString);
		}
		
		final Trace trace = this.animationSelector.getCurrentTrace();
		final StringBuilder sb = new StringBuilder("Machine: ");
		sb.append(trace.getStateSpace().getMainComponent());
		final LoadedMachine lm = trace.getStateSpace().getLoadedMachine();
		sb.append("\nSets: ");
		sb.append(listToString(lm.getSetNames()));
		sb.append("\nConstants: ");
		sb.append(listToString(lm.getConstantNames()));
		sb.append("\nVariables: ");
		sb.append(listToString(lm.getVariableNames()));
		sb.append("\nOperations: ");
		final List<Transition> sortedTransitions = new ArrayList<>(trace.getNextTransitions(true, FormulaExpand.TRUNCATE));
		// Sort transitions by ID to get a consistent ordering.
		// Transition IDs are strings, but they almost always contain numbers.
		sortedTransitions.sort(Comparator.comparing(Transition::getId, new AlphanumericComparator()));
		for (final Transition t : sortedTransitions) {
			sb.append('\n');
			sb.append(t.getPrettyRep());
		}
		if (trace.getCurrentState().isMaxTransitionsCalculated()) {
			sb.append("\nMore operations may be available (MAX_OPERATIONS/MAX_INITIALISATIONS reached)");
		}
		return new DisplayData(sb.toString());
	}
	
	@Override
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		return null;
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		return null;
	}
}
