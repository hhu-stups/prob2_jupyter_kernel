package de.prob2.jupyter.commands;

import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class InitialiseCommand implements Command {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private InitialiseCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":init [PREDICATE]";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Initialise the current machine with the specified predicate";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "This is a shorthand for `:exec INITIALISATION [PREDICATE]`.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		final Trace trace = this.animationSelector.getCurrentTrace();
		final String predicate = argString.isEmpty() ? "1=1" : argString;
		final List<Transition> ops = trace.getStateSpace().transitionFromPredicate(trace.getCurrentState(), "$initialise_machine", predicate, 1);
		assert !ops.isEmpty();
		final Transition op = ops.get(0);
		this.animationSelector.changeCurrentAnimation(trace.add(op));
		trace.getStateSpace().evaluateTransitions(Collections.singleton(op), FormulaExpand.TRUNCATE);
		return new DisplayData(String.format("Machine initialised using operation %s: %s", op.getId(), op.getRep()));
	}
	
	@Override
	public @NotNull ReplacementOptions complete(final @NotNull String argString, final int at) {
		return CommandUtils.completeInBExpression(this.animationSelector.getCurrentTrace(), argString, at);
	}
}
