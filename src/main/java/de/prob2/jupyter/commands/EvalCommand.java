package de.prob2.jupyter.commands;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.AnimationSelector;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class EvalCommand implements Command {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private EvalCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":eval FORMULA";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Evaluate a formula and display the result.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "This is equivalent to inputting the formula without a command name.\n\n"
			+ "If the formula is a $\\mathit{TRUE}$ predicate with free variables, the variable values found while solving are displayed.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		return CommandUtils.displayDataForEvalResult(this.animationSelector.getCurrentTrace().evalCurrent(argString, FormulaExpand.EXPAND));
	}
	
	@Override
	public @NotNull ReplacementOptions complete(final @NotNull String argString, final int at) {
		return CommandUtils.completeInBExpression(this.animationSelector.getCurrentTrace(), argString, at);
	}
}
