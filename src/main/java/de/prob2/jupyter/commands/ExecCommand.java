package de.prob2.jupyter.commands;

import java.util.List;

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
		return ":exec OPERATION [PREDICATE]";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Execute an operation with the specified predicate";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String name, final @NotNull String argString) {
		final String[] split = argString.split("\\h", 2);
		assert split.length >= 1;
		final String opName = split[0];
		final String predicate = split.length >= 2 ? split[1] : "1=1";
		final Trace trace = this.animationSelector.getCurrentTrace();
		final List<Transition> transitions = trace.getStateSpace().transitionFromPredicate(trace.getCurrentState(), opName, predicate, 1);
		assert transitions.size() == 1;
		final Transition op = transitions.get(0);
		this.animationSelector.changeCurrentAnimation(trace.add(op));
		return new DisplayData(String.format("Executed operation %s", opName));
	}
}
