package de.prob2.jupyter.commands;

import java.util.Collections;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.animator.command.PrettyPrintFormulaCommand;
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

public final class PrettyPrintCommand implements Command {
	private static final @NotNull Parameter.RequiredSingle PREDICATE_PARAM = Parameter.requiredRemainder("predicate");
	
	private final @NotNull Provider<@NotNull ProBKernel> kernelProvider;
	private final AnimationSelector animationSelector;
	
	@Inject
	private PrettyPrintCommand(final @NotNull Provider<@NotNull ProBKernel> kernelProvider, final AnimationSelector animationSelector) {
		super();
		
		this.kernelProvider = kernelProvider;
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":prettyprint";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return new Parameters(Collections.singletonList(PREDICATE_PARAM));
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
	public @NotNull String getHelpBody() {
		return "The predicate is not evaluated or simplified, it is only reformatted and converted to Unicode/$\\LaTeX$ form.\n\n"
			+ "Expressions cannot be pretty-printed, only predicates.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final IEvalElement formula = this.kernelProvider.get().parseFormula(args.get(PREDICATE_PARAM), FormulaExpand.EXPAND);
		
		final PrettyPrintFormulaCommand cmdUnicode = new PrettyPrintFormulaCommand(formula, PrettyPrintFormulaCommand.Mode.UNICODE);
		cmdUnicode.setOptimize(false);
		final PrettyPrintFormulaCommand cmdLatex = new PrettyPrintFormulaCommand(formula, PrettyPrintFormulaCommand.Mode.LATEX);
		cmdLatex.setOptimize(false);
		CommandUtils.withSourceCode(formula, () -> this.animationSelector.getCurrentTrace().getStateSpace().execute(cmdUnicode, cmdLatex));
		
		final DisplayData ret = new DisplayData(cmdUnicode.getPrettyPrint());
		ret.putLatex('$' + cmdLatex.getPrettyPrint() + '$');
		return ret;
	}
	
	@Override
	public @NotNull ParameterInspectors getParameterInspectors() {
		return new ParameterInspectors(Collections.singletonMap(
			PREDICATE_PARAM, CommandUtils.bExpressionInspector(this.animationSelector.getCurrentTrace())
		));
	}
	
	@Override
	public @NotNull ParameterCompleters getParameterCompleters() {
		return new ParameterCompleters(Collections.singletonMap(
			PREDICATE_PARAM, CommandUtils.bExpressionCompleter(this.animationSelector.getCurrentTrace())
		));
	}
}
