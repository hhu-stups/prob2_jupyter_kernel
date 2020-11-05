package de.prob2.jupyter.commands;

import java.util.Collections;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.TypeCheckResult;
import de.prob.exception.ProBError;
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

public final class TypeCommand implements Command {
	private static final @NotNull Parameter.RequiredSingle FORMULA_PARAM = Parameter.requiredRemainder("formula");
	
	private final @NotNull Provider<@NotNull ProBKernel> kernelProvider;
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private TypeCommand(final @NotNull Provider<@NotNull ProBKernel> kernelProvider, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.kernelProvider = kernelProvider;
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":type";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return new Parameters(Collections.singletonList(FORMULA_PARAM));
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":type FORMULA";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Display the static type of a formula.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "The type is displayed in B syntax. If the formula is a predicate, the type is `predicate`.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final ProBKernel kernel = this.kernelProvider.get();
		final Trace trace = this.animationSelector.getCurrentTrace();
		final String code = kernel.insertLetVariables(args.get(FORMULA_PARAM));
		final IEvalElement formula = CommandUtils.withSourceCode(code, () -> kernel.parseFormula(code, FormulaExpand.EXPAND));
		final TypeCheckResult result = trace.getStateSpace().typeCheck(formula);
		if (result.isOk()) {
			return new DisplayData(result.getType());
		} else {
			throw new ProBError("Type errors in formula", result.getErrors());
		}
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
