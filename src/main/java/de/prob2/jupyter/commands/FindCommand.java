package de.prob2.jupyter.commands;

import java.util.Collections;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.CommandUtils;
import de.prob2.jupyter.Parameter;
import de.prob2.jupyter.ParameterCompleters;
import de.prob2.jupyter.ParameterInspectors;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class FindCommand implements Command {
	private static final @NotNull Parameter.RequiredSingle PREDICATE_PARAM = Parameter.requiredRemainder("predicate");
	
	private final @NotNull Provider<@NotNull ProBKernel> kernelProvider;
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private FindCommand(final @NotNull Provider<@NotNull ProBKernel> kernelProvider, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.kernelProvider = kernelProvider;
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":find";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return new Parameters(Collections.singletonList(PREDICATE_PARAM));
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
		return "If such a state is found, it is made the current state, otherwise an error is displayed.\n\n"
			+ "Note that this command does not necessarily find a valid *trace* to the found state. Instead, this command may a single \"fake\" transition to the trace, which goes directly to the found state and does not use the machine's operations to reach it. Such a state may or may not be reachable by a sequence of normal operations.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final ProBKernel kernel = this.kernelProvider.get();
		final Trace trace = this.animationSelector.getCurrentTrace();
		final IEvalElement pred = kernel.parseFormula(args.get(PREDICATE_PARAM), FormulaExpand.EXPAND);
		final Trace newTrace = CommandUtils.withSourceCode(pred, () -> trace.getStateSpace().getTraceToState(pred));
		this.animationSelector.changeCurrentAnimation(newTrace);
		return new DisplayData("Found a matching state and made it current state");
	}
	
	@Override
	public @NotNull ParameterInspectors getParameterInspectors() {
		return new ParameterInspectors(Collections.singletonMap(
			PREDICATE_PARAM, CommandUtils.bExpressionInspector(this.animationSelector.getCurrentTrace())
		));
	}
	
	@Override
	public @NotNull ParameterCompleters getParameterCompleters() {
		return new ParameterCompleters(Collections.singletonMap(
			PREDICATE_PARAM, CommandUtils.bExpressionCompleter(this.animationSelector.getCurrentTrace())
		));
	}
}
