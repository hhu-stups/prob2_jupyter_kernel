package de.prob2.jupyter.commands;

import com.google.inject.Inject;

import de.prob.animator.command.PrettyPrintFormulaCommand;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.AnimationSelector;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class PrettyPrintCommand implements Command {
	private final AnimationSelector animationSelector;
	
	@Inject
	private PrettyPrintCommand(final AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":prettyprint PREDICATE";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Pretty-print a predicate.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		final IEvalElement formula = this.animationSelector.getCurrentTrace().getModel().parseFormula(argString, FormulaExpand.EXPAND);
		
		final PrettyPrintFormulaCommand cmdUnicode = new PrettyPrintFormulaCommand(formula, PrettyPrintFormulaCommand.Mode.UNICODE);
		cmdUnicode.setOptimize(false);
		final PrettyPrintFormulaCommand cmdLatex = new PrettyPrintFormulaCommand(formula, PrettyPrintFormulaCommand.Mode.LATEX);
		cmdLatex.setOptimize(false);
		this.animationSelector.getCurrentTrace().getStateSpace().execute(cmdUnicode, cmdLatex);
		
		final DisplayData ret = new DisplayData(cmdUnicode.getPrettyPrint());
		ret.putLatex('$' + cmdLatex.getPrettyPrint() + '$');
		return ret;
	}
	
	@Override
	public @NotNull ReplacementOptions complete(final @NotNull String argString, final int at) {
		return CommandUtils.completeInBExpression(this.animationSelector.getCurrentTrace(), argString, at);
	}
}
