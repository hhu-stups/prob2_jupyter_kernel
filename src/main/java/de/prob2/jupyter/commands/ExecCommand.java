package de.prob2.jupyter.commands;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ExecCommand implements Command {
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
		return "Execute an operation.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "A transition for the given operation is found and executed. If the optional predicate is specified, a transition is found for which the predicate is $\\mathit{TRUE}$. The predicate can be used to restrict what values the operation's parameters or the variables in the next state may have.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		final List<String> split = CommandUtils.splitArgs(argString, 2);
		assert !split.isEmpty();
		
		final Trace trace = this.animationSelector.getCurrentTrace();
		final String translatedOpName = CommandUtils.unprettyOperationName(split.get(0));
		final String predicate = split.size() < 2 ? "1=1" : split.get(1);
		final List<Transition> ops = trace.getStateSpace().transitionFromPredicate(trace.getCurrentState(), translatedOpName, predicate, 1);
		assert !ops.isEmpty();
		final Transition op = ops.get(0);
		
		this.animationSelector.changeCurrentAnimation(trace.add(op));
		trace.getStateSpace().evaluateTransitions(Collections.singleton(op), FormulaExpand.TRUNCATE);
		return new DisplayData(String.format("Executed operation: %s", op.getRep()));
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		return CommandUtils.completeArgs(
			argString, at,
			(operation, at0) -> {
				final String prefix = operation.substring(0, at0);
				final List<String> opNames = this.animationSelector.getCurrentTrace()
					.getNextTransitions()
					.stream()
					.map(Transition::getName)
					.map(CommandUtils::prettyOperationName)
					.distinct()
					.filter(s -> s.startsWith(prefix))
					.sorted()
					.collect(Collectors.toList());
				return new ReplacementOptions(opNames, 0, operation.length());
			},
			CommandUtils.bExpressionCompleter(this.animationSelector.getCurrentTrace())
		);
	}
}
