package de.prob2.jupyter.commands;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;

import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class ExecCommand implements Command {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private ExecCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":exec OPERATION [PREDICATE]\n:exec OPERATION_ID";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Execute an operation with the specified predicate, or by its ID";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String argString) {
		final List<String> split = CommandUtils.splitArgs(argString, 2);
		assert !split.isEmpty();
		final String opNameOrId = split.get(0);
		
		final Trace trace = this.animationSelector.getCurrentTrace();
		// Check if the argument is an ID, by searching for a transition with that ID.
		final Optional<Transition> opt = trace.getNextTransitions().stream().filter(t -> t.getId().equals(opNameOrId)).findAny();
		final Transition op;
		if (opt.isPresent()) {
			// Transition found, nothing else needs to be done.
			if (split.size() != 1) {
				throw new UserErrorException("Cannot specify a predicate when executing an operation by ID");
			}
			op = opt.get();
		} else {
			// Transition not found, assume that the argument is an operation name instead.
			final List<String> predicates = split.size() < 2 ? Collections.emptyList() : Collections.singletonList(split.get(1));
			op = trace.getCurrentState().findTransition(opNameOrId, predicates);
		}
		
		this.animationSelector.changeCurrentAnimation(trace.add(op));
		trace.getStateSpace().evaluateTransitions(Collections.singleton(op), FormulaExpand.TRUNCATE);
		return new DisplayData(String.format("Executed operation %s: %s", op.getId(), op.getRep()));
	}
}
