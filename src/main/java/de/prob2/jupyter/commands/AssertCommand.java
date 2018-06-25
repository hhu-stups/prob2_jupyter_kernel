package de.prob2.jupyter.commands;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.AnimationSelector;

import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;
import io.github.spencerpark.jupyter.kernel.display.mime.MIMEType;

import org.jetbrains.annotations.NotNull;

public final class AssertCommand implements Command {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private AssertCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":assert PREDICATE";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Ensure that the predicate is true, and show an error otherwise.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "This command is intended for verifying that a predicate is always true at a certain point in a notebook. Unlike normal evaluation (`:eval`), this command treats a $\\mathit{FALSE}$ result as an error. If the result is $\\mathit{TRUE}$, solutions for free variables (if any) are not displayed.\n\n"
			+ "Only predicates and $\\mathit{BOOL}$ expressions are accepted. Expressions of other types cause an error.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		final AbstractEvalResult result = this.animationSelector.getCurrentTrace().evalCurrent(argString, FormulaExpand.TRUNCATE);
		if (result instanceof EvalResult && "TRUE".equals(((EvalResult)result).getValue())) {
			// Use EvalResult.TRUE instead of the real result so that solution variables are not displayed.
			return CommandUtils.displayDataForEvalResult(EvalResult.TRUE);
		}
		
		final DisplayData displayData;
		try {
			displayData = CommandUtils.displayDataForEvalResult(result);
		} catch (final UserErrorException e) {
			throw new UserErrorException("Error while evaluating assertion: " + e.getMessage());
		}
		throw new UserErrorException("Assertion is not true: " + displayData.getData(MIMEType.TEXT_PLAIN));
	}
	
	@Override
	public @NotNull ReplacementOptions complete(final @NotNull String argString, final int at) {
		return CommandUtils.completeInBExpression(this.animationSelector.getCurrentTrace(), argString, at);
	}
}
