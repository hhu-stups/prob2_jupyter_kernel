package de.prob2.jupyter.commands;

import java.util.Collections;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob.statespace.AnimationSelector;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.CommandUtils;
import de.prob2.jupyter.Parameter;
import de.prob2.jupyter.ParameterCompleters;
import de.prob2.jupyter.ParameterInspectors;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;
import io.github.spencerpark.jupyter.kernel.display.mime.MIMEType;

import org.jetbrains.annotations.NotNull;

public final class AssertCommand implements Command {
	private static final @NotNull Parameter.RequiredSingle FORMULA_PARAM = Parameter.requiredRemainder("formula");
	
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
	public @NotNull Parameters getParameters() {
		return new Parameters(Collections.singletonList(FORMULA_PARAM));
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
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final ProBKernel kernel = this.kernelProvider.get();
		final IEvalElement formula = kernel.parseFormula(args.get(FORMULA_PARAM), FormulaExpand.TRUNCATE);
		final AbstractEvalResult result = CommandUtils.withSourceCode(formula, () -> 
			kernel.postprocessEvalResult(this.animationSelector.getCurrentTrace().evalCurrent(formula, ProBKernel.TRUNCATE_EVAL_OPTIONS))
		);
		if (result instanceof EvalResult && "TRUE".equals(((EvalResult)result).getValue())) {
			// Use EvalResult.TRUE instead of the real result so that solution variables are not displayed.
			return CommandUtils.displayDataForEvalResult(EvalResult.TRUE);
		}
		
		final DisplayData displayData;
		try {
			displayData = CommandUtils.displayDataForEvalResult(result);
		} catch (final ProBError e) {
			throw new UserErrorException("Error while evaluating assertion: " + CommandUtils.inlinePlainTextForEvalResult(result), e);
		}
		throw new UserErrorException("Assertion is not true: " + displayData.getData(MIMEType.TEXT_PLAIN));
	}
	
	@Override
	public @NotNull ParameterInspectors getParameterInspectors() {
		return new ParameterInspectors(Collections.singletonMap(
			FORMULA_PARAM, CommandUtils.bExpressionInspector(this.animationSelector.getCurrentTrace())
		));
	}
	
	@Override
	public @NotNull ParameterCompleters getParameterCompleters() {
		return new ParameterCompleters(Collections.singletonMap(
			FORMULA_PARAM, CommandUtils.bExpressionCompleter(this.animationSelector.getCurrentTrace())
		));
	}
}
