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
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.PositionalParameter;
import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FindCommand implements Command {
	private static final @NotNull PositionalParameter.RequiredRemainder PREDICATE_PARAM = new PositionalParameter.RequiredRemainder("predicate");
	
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
		final Trace trace = this.animationSelector.getCurrentTrace();
		final String code = this.kernelProvider.get().insertLetVariables(args.get(PREDICATE_PARAM));
		final Trace newTrace = CommandUtils.withSourceCode(code, () -> {
			final IEvalElement pred = trace.getModel().parseFormula(code, FormulaExpand.EXPAND);
			return trace.getStateSpace().getTraceToState(pred);
		});
		this.animationSelector.changeCurrentAnimation(newTrace);
		return new DisplayData("Found a matching state and made it current state");
	}
	
	@Override
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		return CommandUtils.inspectInBExpression(this.animationSelector.getCurrentTrace(), argString, at);
	}
	
	@Override
	public @NotNull ReplacementOptions complete(final @NotNull String argString, final int at) {
		return CommandUtils.completeInBExpression(this.animationSelector.getCurrentTrace(), argString, at);
	}
}
