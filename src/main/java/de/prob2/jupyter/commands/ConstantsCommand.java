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

public final class ConstantsCommand implements Command {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private ConstantsCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":constants [PREDICATE]";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Set up the current machine's constants.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "This is a shorthand for `:exec SETUP_CONSTANTS [PREDICATE]`.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		final Trace trace = this.animationSelector.getCurrentTrace();
		final String predicate = argString.isEmpty() ? "1=1" : argString;
		final List<Transition> ops = trace.getStateSpace().transitionFromPredicate(trace.getCurrentState(), "$setup_constants", predicate, 1);
		assert !ops.isEmpty();
		final Transition op = ops.get(0);
		this.animationSelector.changeCurrentAnimation(trace.add(op));
		trace.getStateSpace().evaluateTransitions(Collections.singleton(op), FormulaExpand.TRUNCATE);
		return new DisplayData(String.format("Machine constants set up using operation %s: %s", op.getId(), op.getRep()));
	}
	
	@Override
	public @NotNull ReplacementOptions complete(final @NotNull String argString, final int at) {
		return CommandUtils.completeInBExpression(this.animationSelector.getCurrentTrace(), argString, at);
	}
}
