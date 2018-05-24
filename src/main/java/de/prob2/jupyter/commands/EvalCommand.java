package de.prob2.jupyter.commands;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ComputationNotCompletedResult;
import de.prob.animator.domainobjects.EnumerationWarning;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.AnimationSelector;
import de.prob.unicode.UnicodeTranslator;

import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.messages.DisplayData;

import org.jetbrains.annotations.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EvalCommand implements LineCommand {
	private static final Logger LOGGER = LoggerFactory.getLogger(EvalCommand.class);
	
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private EvalCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":eval EXPRESSION";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Evaluate an expression.";
	}
	
	private static @NotNull DisplayData displayDataForEvalResult(final @NotNull AbstractEvalResult aer) {
		final StringBuilder sb = new StringBuilder();
		final boolean error;
		if (aer instanceof EvalResult) {
			final EvalResult result = (EvalResult)aer;
			sb.append(UnicodeTranslator.toUnicode(result.getValue()));
			if (!result.getSolutions().isEmpty()) {
				sb.append("\n\nSolution:");
				result.getSolutions().forEach((k, v) -> {
					sb.append("\n\t");
					sb.append(UnicodeTranslator.toUnicode(k));
					sb.append(" = ");
					sb.append(UnicodeTranslator.toUnicode(v));
				});
			}
			error = false;
		} else if (aer instanceof ComputationNotCompletedResult) {
			final ComputationNotCompletedResult result = (ComputationNotCompletedResult)aer;
			sb.append("Computation not completed: ");
			sb.append(result.getReason());
			error = true;
		} else if (aer instanceof EnumerationWarning) {
			sb.append("UNKNOWN (FALSE with enumeration warning)");
			error = true;
		} else if (aer instanceof EvaluationErrorResult) {
			final EvaluationErrorResult result = (EvaluationErrorResult)aer;
			sb.append(result.getResult());
			if (!result.getErrors().isEmpty()) {
				sb.append(": ");
				result.getErrors().forEach(s -> {
					sb.append('\n');
					sb.append(s);
				});
			}
			error = true;
		} else {
			LOGGER.info("Unhandled eval result type, falling back to toString(): {}", aer.getClass());
			sb.append(aer);
			error = false;
		}
		
		if (error) {
			throw new UserErrorException(sb.toString());
		} else {
			return new DisplayData(sb.toString());
		}
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String argString) {
		return EvalCommand.displayDataForEvalResult(this.animationSelector.getCurrentTrace().evalCurrent(argString, FormulaExpand.EXPAND));
	}
}
