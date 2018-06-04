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
		return "Set up the current machine's constants with the specified predicate";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String argString) {
		final Trace trace = this.animationSelector.getCurrentTrace();
		final List<String> predicates = argString.isEmpty() ? Collections.emptyList() : Collections.singletonList(argString);
		final Transition op = trace.getCurrentState().findTransition("$setup_constants", predicates);
		if (op == null) {
			throw new UserErrorException("Could not setup constants" + (argString.isEmpty() ? "" : " with the specified predicate"));
		}
		this.animationSelector.changeCurrentAnimation(trace.add(op));
		trace.getStateSpace().evaluateTransitions(Collections.singleton(op), FormulaExpand.TRUNCATE);
		return new DisplayData(String.format("Machine constants set up using operation %s: %s", op.getId(), op.getRep()));
	}
}
