package de.prob2.jupyter.commands;

import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
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

public final class ConstantsCommand implements Command {
	private static final @NotNull PositionalParameter.OptionalRemainder PREDICATE_PARAM = new PositionalParameter.OptionalRemainder("predicate");
	
	private final @NotNull Provider<@NotNull ProBKernel> kernelProvider;
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private ConstantsCommand(final @NotNull Provider<@NotNull ProBKernel> kernelProvider, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.kernelProvider = kernelProvider;
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":constants";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return new Parameters(Collections.singletonList(PREDICATE_PARAM));
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":constants [PREDICATE]";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Set up the current machine's constants.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "This is a shorthand for `:exec SETUP_CONSTANTS [PREDICATE]`.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final Trace trace = this.animationSelector.getCurrentTrace();
		final String predicate;
		if (!args.get(PREDICATE_PARAM).isPresent()) {
			predicate = "1=1";
		} else {
			predicate = this.kernelProvider.get().insertLetVariables(args.get(PREDICATE_PARAM).get());
		}
		final List<Transition> ops = trace.getStateSpace().transitionFromPredicate(trace.getCurrentState(), "$setup_constants", predicate, 1);
		assert !ops.isEmpty();
		final Transition op = ops.get(0);
		this.animationSelector.changeCurrentAnimation(trace.add(op));
		trace.getStateSpace().evaluateTransitions(Collections.singleton(op), FormulaExpand.TRUNCATE);
		return new DisplayData(String.format("Machine constants set up using operation %s: %s", op.getId(), op.getRep()));
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
