package de.prob2.jupyter.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.CommandUtils;
import de.prob2.jupyter.Parameter;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ExecCommand implements Command {
	private static final @NotNull Parameter.RequiredSingle OPERATION_PARAM = Parameter.required("operation");
	private static final @NotNull Parameter.OptionalSingle PREDICATE_PARAM = Parameter.optionalRemainder("predicate");
	
	private final @NotNull Provider<@NotNull ProBKernel> kernelProvider;
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private ExecCommand(final @NotNull Provider<@NotNull ProBKernel> kernelProvider, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.kernelProvider = kernelProvider;
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":exec";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return new Parameters(Arrays.asList(OPERATION_PARAM, PREDICATE_PARAM));
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
		return "The predicate is used to select the operation's parameter values. The parameters can be fully specified explicitly (e. g. `:exec op param1 = 123 & param2 = {1, 2}`), or they can be partially constrained (e. g. `:exec op param1 > 100 & card(param2) >= 2`) to let ProB find a valid combination of parameters. If there are multiple valid combinations of parameters that satisfy the predicate, it is undefined which one is selected by ProB.\n\n"
			+ "If no predicate is specified, the parameters are not constrained, and ProB will select an arbitrary valid combination of parameters.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final Trace trace = this.animationSelector.getCurrentTrace();
		final String translatedOpName = CommandUtils.unprettyOperationName(args.get(OPERATION_PARAM));
		final String predicate;
		if (!args.get(PREDICATE_PARAM).isPresent()) {
			predicate = "1=1";
		} else {
			predicate = this.kernelProvider.get().insertLetVariables(args.get(PREDICATE_PARAM).get());
		}
		final List<Transition> ops = trace.getStateSpace().transitionFromPredicate(trace.getCurrentState(), translatedOpName, predicate, 1);
		assert !ops.isEmpty();
		final Transition op = ops.get(0);
		
		this.animationSelector.changeCurrentAnimation(trace.add(op));
		trace.getStateSpace().evaluateTransitions(Collections.singleton(op), FormulaExpand.TRUNCATE);
		return new DisplayData(String.format("Executed operation: %s", op.getRep()));
	}
	
	@Override
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		return CommandUtils.inspectArgs(
			argString, at,
			(operation, at0) -> null, // TODO
			CommandUtils.bExpressionInspector(this.animationSelector.getCurrentTrace())
		);
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
