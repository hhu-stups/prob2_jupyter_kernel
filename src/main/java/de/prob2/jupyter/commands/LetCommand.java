package de.prob2.jupyter.commands;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.AnimationSelector;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.CommandUtils;
import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LetCommand implements Command {
	private final @NotNull Provider<@NotNull ProBKernel> kernelProvider;
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	public LetCommand(final @NotNull Provider<@NotNull ProBKernel> kernelProvider, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.kernelProvider = kernelProvider;
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":let";
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":let NAME EXPR";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Evaluate an expression and store it in a local variable.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "The expression is evaluated once in the current state, and its value is stored. Once set, variables are available in all states. A variable created by `:let` shadows any identifier with the same name from the machine.\n\n"
			+ "Variables created by `:let` are retained even when a different machine is loaded. To remove a variable, use `:unlet`.\n\n"
			+ "**Note:** The values of local variables are currently stored in text form. Values must have a syntactically valid text representation, and large values may cause performance issues.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		final List<String> split = CommandUtils.splitArgs(argString, 2);
		if (split.size() != 2) {
			throw new UserErrorException("Expected 2 arguments, not " + split.size());
		}
		final String name = split.get(0);
		final String expr = this.kernelProvider.get().insertLetVariables(split.get(1));
		final AbstractEvalResult evaluated = CommandUtils.withSourceCode(expr, () -> this.animationSelector.getCurrentTrace().evalCurrent(expr, FormulaExpand.EXPAND));
		if (evaluated instanceof EvalResult) {
			this.kernelProvider.get().getVariables().put(name, evaluated.toString());
		}
		return CommandUtils.displayDataForEvalResult(evaluated);
	}
	
	@Override
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		// TODO
		return null;
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		// TODO
		return null;
	}
}
