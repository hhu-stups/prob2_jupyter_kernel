package de.prob2.jupyter.commands;

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
import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TypeCommand implements Command {
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
	public @NotNull DisplayData run(final @NotNull String argString) {
		final Trace trace = this.animationSelector.getCurrentTrace();
		final String code = this.kernelProvider.get().insertLetVariables(argString);
		final IEvalElement formula = CommandUtils.withSourceCode(code, () -> trace.getModel().parseFormula(code, FormulaExpand.EXPAND));
		final TypeCheckResult result = trace.getStateSpace().typeCheck(formula);
		if (result.isOk()) {
			return new DisplayData(result.getType());
		} else {
			throw new ProBError("Type errors in formula", result.getErrors());
		}
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
