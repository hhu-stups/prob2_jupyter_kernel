package de.prob2.jupyter.commands;

import java.util.Collections;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.AnimationSelector;
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

public final class EvalCommand implements Command {
	private static final @NotNull Parameter.RequiredSingle FORMULA_PARAM = Parameter.requiredRemainder("formula");
	
	private final @NotNull Injector injector;
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private EvalCommand(final @NotNull Injector injector, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.injector = injector;
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":eval";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return new Parameters(Collections.singletonList(FORMULA_PARAM));
	}
	
	@Override
	public @NotNull String getSyntax() {
		return "FORMULA\n// or\n:eval FORMULA";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Evaluate a formula and display the result.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "Normally you do not need to explicitly call `:eval` to evaluate formulas. If you input a formula without any command before it, it is evaluated automatically (e. g. `:eval 1 + 2` is equivalent to just `1 + 2`).\n\n"
			+ "If the formula is a $\\mathit{TRUE}$ predicate with free variables, the variable values found while solving are displayed. For more control about which solver is used to solve the predicate, use the `:solve` command.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final ProBKernel kernel = this.injector.getInstance(ProBKernel.class);
		final IEvalElement formula = kernel.parseFormula(args.get(FORMULA_PARAM), FormulaExpand.EXPAND);
		return CommandUtils.displayDataForEvalResult(CommandUtils.withSourceCode(formula, () -> this.animationSelector.getCurrentTrace().evalCurrent(formula)));
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
