package de.prob2.jupyter.commands;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.TypeCheckResult;
import de.prob.exception.ProBError;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class TypeCommand implements Command {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private TypeCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":type FORMULA";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Display the type of a formula.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String argString) {
		final Trace trace = this.animationSelector.getCurrentTrace();
		final IEvalElement formula = trace.getModel().parseFormula(argString, FormulaExpand.EXPAND);
		final TypeCheckResult result = trace.getStateSpace().typeCheck(formula);
		if (result.isOk()) {
			return new DisplayData(result.getType());
		} else {
			throw new ProBError("Type errors in formula", result.getErrors());
		}
	}
	
	@Override
	public @NotNull ReplacementOptions complete(final @NotNull ProBKernel kernel, final @NotNull String argString, final int at) {
		return CommandUtils.completeInBExpression(this.animationSelector.getCurrentTrace(), argString, at);
	}
}
