package de.prob2.jupyter.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ComputationNotCompletedResult;
import de.prob.animator.domainobjects.EnumerationWarning;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.unicode.UnicodeTranslator;

import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CommandUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(CommandUtils.class);
	
	public static @NotNull List<@NotNull String> splitArgs(final @NotNull String args, final int limit) {
		final String[] split = args.split("\\h+", limit);
		if (split.length == 1 && split[0].isEmpty()) {
			return Collections.emptyList();
		} else {
			return Arrays.asList(split);
		}
	}
	
	public static @NotNull List<@NotNull String> splitArgs(final @NotNull String args) {
		return splitArgs(args, 0);
	}
	
	public static @NotNull Map<@NotNull String, @NotNull String> parsePreferences(final @NotNull List<@NotNull String> args) {
		final Map<String, String> preferences = new HashMap<>();
		for (final String arg : args) {
			final String[] split = arg.split("=", 2);
			if (split.length == 1) {
				throw new UserErrorException("Missing value for preference " + split[0]);
			}
			preferences.put(split[0], split[1]);
		}
		return preferences;
	}
	
	public static @NotNull DisplayData displayDataForEvalResult(final @NotNull AbstractEvalResult aer) {
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
}
