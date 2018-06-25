package de.prob2.jupyter.commands;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;

import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
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
		return "Execute an operation with the specified predicate, or by its ID.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "In the first form, the given operation is executed. If the optional predicate is specified, a transition is found for which the predicate is $\\mathit{TRUE}$. The predicate can be used to restrict what values the operation's parameters or the variables in the next state may have.\n\n"
			+ "In the second form, a known transition with the given numeric ID is executed. A list of the current state's available transitions and their IDs can be viewed using `:browse`. Only transition IDs from the current state can be executed.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
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
			final String translatedOpName = CommandUtils.unprettyOperationName(opNameOrId);
			final List<String> predicates = split.size() < 2 ? Collections.emptyList() : Collections.singletonList(split.get(1));
			op = trace.getCurrentState().findTransition(translatedOpName, predicates);
			if (op == null) {
				throw new UserErrorException("Could not execute operation " + opNameOrId + (split.size() < 2 ? "" : " with the given predicate"));
			}
		}
		
		this.animationSelector.changeCurrentAnimation(trace.add(op));
		trace.getStateSpace().evaluateTransitions(Collections.singleton(op), FormulaExpand.TRUNCATE);
		return new DisplayData(String.format("Executed operation %s: %s", op.getId(), op.getRep()));
	}
	
	@Override
	public @NotNull ReplacementOptions complete(final @NotNull String argString, final int at) {
		final int opNameEnd;
		final Matcher argSplitMatcher = CommandUtils.ARG_SPLIT_PATTERN.matcher(argString);
		if (argSplitMatcher.find()) {
			opNameEnd = argSplitMatcher.start();
		} else {
			opNameEnd = argString.length();
		}
		
		if (opNameEnd < at) {
			// Cursor is in the predicate part of the arguments, provide B completions.
			final ReplacementOptions replacements = CommandUtils.completeInBExpression(this.animationSelector.getCurrentTrace(), argString.substring(opNameEnd), at - opNameEnd);
			return CommandUtils.offsetReplacementOptions(replacements, opNameEnd);
		} else {
			// Cursor is in the first part of the arguments, provide possible operation names and transition IDs.
			final String prefix = argString.substring(0, at);
			final List<String> opNames = this.animationSelector.getCurrentTrace()
				.getNextTransitions()
				.stream()
				.flatMap(t -> Stream.of(CommandUtils.prettyOperationName(t.getName()), t.getId()))
				.distinct()
				.filter(s -> s.startsWith(prefix))
				.sorted()
				.collect(Collectors.toList());
			return new ReplacementOptions(opNames, 0, opNameEnd);
		}
	}
}
