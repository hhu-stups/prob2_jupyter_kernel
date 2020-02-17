package de.prob2.jupyter.commands;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.AnimationSelector;
import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EvalCommand implements Command {
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
		final String code = this.injector.getInstance(ProBKernel.class).insertLetVariables(argString);
		return CommandUtils.displayDataForEvalResult(CommandUtils.withSourceCode(code, () -> this.animationSelector.getCurrentTrace().evalCurrent(code, FormulaExpand.EXPAND)));
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
