package de.prob2.jupyter.commands;

import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;

import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

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
		return ":initialise [PREDICATE]\n:init [PREDICATE]";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Initialise the current machine with the specified predicate";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String argString) {
		final Trace trace = this.animationSelector.getCurrentTrace();
		final List<String> predicates = argString.isEmpty() ? Collections.emptyList() : Collections.singletonList(argString);
		final Transition op = trace.getCurrentState().findTransition("$initialise_machine", predicates);
		if (op == null) {
			if (trace.getCurrentState().isInitialised()) {
				throw new UserErrorException("Machine is already initialised");
			} else if (trace.getHead().getPrevious() == null && trace.canExecuteEvent("$setup_constants")) {
				throw new UserErrorException("Machine constants are not yet set up, use :constants first");
			} else {
				throw new UserErrorException("Could not initialise machine" + (argString.isEmpty() ? "" : " with the specified predicate"));
			}
		}
		this.animationSelector.changeCurrentAnimation(trace.add(op));
		trace.getStateSpace().evaluateTransitions(Collections.singleton(op), FormulaExpand.TRUNCATE);
		return new DisplayData(String.format("Machine initialised using operation %s: %s", op.getId(), op.getRep()));
	}
	
	@Override
	public @NotNull ReplacementOptions complete(final @NotNull ProBKernel kernel, final @NotNull String argString, final int at) {
		return CommandUtils.completeInBExpression(this.animationSelector.getCurrentTrace(), argString, at);
	}
}
