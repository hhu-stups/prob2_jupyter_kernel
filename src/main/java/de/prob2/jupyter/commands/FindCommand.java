package de.prob2.jupyter.commands;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class FindCommand implements Command {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private FindCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":find PREDICATE";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Try to find a state for which the given predicate is true (in addition to the machine's invariant).";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "If such a state is found, it is made the current state, otherwise an error is displayed.\n\nNote that this command does not necessarily find a valid *trace* to the found state. Instead, in some cases a single \"fake\" transition is added to the trace, which goes directly to the found state and does not use the machine's operations to reach it.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		final Trace trace = this.animationSelector.getCurrentTrace();
		final Trace newTrace = CommandUtils.withSourceCode(argString, () -> {
			final IEvalElement pred = trace.getModel().parseFormula(argString, FormulaExpand.EXPAND);
			return trace.getStateSpace().getTraceToState(pred);
		});
		this.animationSelector.changeCurrentAnimation(newTrace);
		return new DisplayData("Found state: " + newTrace.getCurrentState().getId());
	}
	
	@Override
	public @NotNull ReplacementOptions complete(final @NotNull String argString, final int at) {
		return CommandUtils.completeInBExpression(this.animationSelector.getCurrentTrace(), argString, at);
	}
}
