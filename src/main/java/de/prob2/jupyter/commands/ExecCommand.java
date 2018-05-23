package de.prob2.jupyter.commands;

import java.util.List;
import java.util.Optional;

import com.google.inject.Inject;

import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;

import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.messages.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class ExecCommand implements LineCommand {
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
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String name, final @NotNull String argString) {
		final String[] split = argString.split("\\h", 2);
		assert split.length >= 1;
		final String opNameOrId = split[0];
		
		final Trace trace = this.animationSelector.getCurrentTrace();
		// Check if the argument is an ID, by searching for a transition with that ID.
		final Optional<Transition> opt = trace.getNextTransitions().stream().filter(t -> t.getId().equals(opNameOrId)).findAny();
		final Transition op;
		if (opt.isPresent()) {
			// Transition found, nothing else needs to be done.
			if (split.length != 1) {
				throw new CommandExecutionException(name, "Cannot specify a predicate when executing an operation by ID");
			}
			op = opt.get();
		} else {
			// Transition not found, assume that the argument is an operation name instead.
			final String predicate = split.length >= 2 ? split[1] : "1=1";
			final List<Transition> transitions = trace.getStateSpace().transitionFromPredicate(trace.getCurrentState(), opNameOrId, predicate, 1);
			assert transitions.size() == 1;
			op = transitions.get(0);
		}
		
		this.animationSelector.changeCurrentAnimation(trace.add(op));
		return new DisplayData(String.format("Executed operation %s", opNameOrId));
	}
}
