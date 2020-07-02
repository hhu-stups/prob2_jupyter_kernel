package de.prob2.jupyter.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.CommandUtils;
import de.prob2.jupyter.Parameter;
import de.prob2.jupyter.ParameterCompleters;
import de.prob2.jupyter.ParameterInspectors;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

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
		return this.kernelProvider.get().executeOperation(args.get(OPERATION_PARAM), args.get(PREDICATE_PARAM).orElse(null));
	}
	
	@Override
	public @NotNull ParameterInspectors getParameterInspectors() {
		return new ParameterInspectors(ImmutableMap.of(
			OPERATION_PARAM, (operation, at) -> null, // TODO
			PREDICATE_PARAM, CommandUtils.bExpressionInspector(this.animationSelector.getCurrentTrace())
		));
	}
	
	@Override
	public @NotNull ParameterCompleters getParameterCompleters() {
		return new ParameterCompleters(ImmutableMap.of(
			OPERATION_PARAM, (operation, at) -> {
				final String prefix = operation.substring(0, at);
				final List<String> opNames = this.animationSelector.getCurrentTrace()
					.getNextTransitions()
					.stream()
					.map(Transition::getPrettyName)
					.distinct()
					.filter(s -> s.startsWith(prefix))
					.sorted()
					.collect(Collectors.toList());
				return new ReplacementOptions(opNames, 0, operation.length());
			},
			PREDICATE_PARAM, CommandUtils.bExpressionCompleter(this.animationSelector.getCurrentTrace())
		));
	}
}
