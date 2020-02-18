package de.prob2.jupyter.commands;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.AnimationSelector;
import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;
import io.github.spencerpark.jupyter.kernel.display.mime.MIMEType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AssertCommand implements Command {
	private final @NotNull Provider<@NotNull ProBKernel> kernelProvider;
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private AssertCommand(final @NotNull Provider<@NotNull ProBKernel> kernelProvider, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.kernelProvider = kernelProvider;
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":assert";
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
		return "Unlike normal evaluation (`:eval`), this command treats a $\\mathit{FALSE}$ result as an error. If the result is $\\mathit{TRUE}$, solutions for free variables (if any) are not displayed.\n\n"
			+ "Only predicates and $\\mathit{BOOL}$ expressions are accepted. Expressions of other types cause an error.\n\n"
			+ "This command is intended for verifying that a condition holds at a certain point in the notebook. It may also be used in combination with the Jupyter Notebook [nbgrader](https://nbgrader.readthedocs.io/) extension for automatic checking/grading of exercises.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		final String code = this.kernelProvider.get().insertLetVariables(argString);
		final AbstractEvalResult result = CommandUtils.withSourceCode(code, () -> this.animationSelector.getCurrentTrace().evalCurrent(code, FormulaExpand.TRUNCATE));
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
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		return CommandUtils.inspectInBExpression(this.animationSelector.getCurrentTrace(), argString, at);
	}
	
	@Override
	public @NotNull ReplacementOptions complete(final @NotNull String argString, final int at) {
		return CommandUtils.completeInBExpression(this.animationSelector.getCurrentTrace(), argString, at);
	}
}
