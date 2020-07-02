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
import de.prob2.jupyter.Parameter;
import de.prob2.jupyter.ParameterCompleters;
import de.prob2.jupyter.ParameterInspectors;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class ConstantsCommand implements Command {
	private static final @NotNull Parameter.OptionalSingle PREDICATE_PARAM = Parameter.optionalRemainder("predicate");
	
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
		return this.kernelProvider.get().executeOperation(Transition.SETUP_CONSTANTS_NAME, args.get(PREDICATE_PARAM).orElse(null));
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
