package de.prob2.jupyter.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.prob.animator.command.CompleteIdentifierCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ComputationNotCompletedResult;
import de.prob.animator.domainobjects.EnumerationWarning;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.statespace.Trace;
import de.prob.unicode.UnicodeTranslator;

import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CommandUtils {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(CommandUtils.class);
	
	private static final @NotNull Pattern B_IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");
	
	private CommandUtils() {
		super();
		
		throw new AssertionError("Utility class");
	}
	
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
		final StringBuilder sbMarkdown = new StringBuilder();
		final boolean error;
		if (aer instanceof EvalResult) {
			final EvalResult result = (EvalResult)aer;
			sb.append(UnicodeTranslator.toUnicode(result.getValue()));
			sbMarkdown.append('$');
			sbMarkdown.append(UnicodeTranslator.toLatex(result.getValue()));
			sbMarkdown.append('$');
			if (!result.getSolutions().isEmpty()) {
				sb.append("\n\nSolution:");
				sbMarkdown.append("\n\n**Solution:**");
				result.getSolutions().forEach((k, v) -> {
					sb.append("\n\t");
					sb.append(UnicodeTranslator.toUnicode(k));
					sb.append(" = ");
					sb.append(UnicodeTranslator.toUnicode(v));
					sbMarkdown.append("\n* $");
					sbMarkdown.append(UnicodeTranslator.toLatex(k));
					sbMarkdown.append(" = ");
					sbMarkdown.append(UnicodeTranslator.toLatex(v));
					sbMarkdown.append('$');
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
			sbMarkdown.append(aer);
			error = false;
		}
		
		if (error) {
			throw new UserErrorException(sb.toString());
		} else {
			final DisplayData result = new DisplayData(sb.toString());
			result.putMarkdown(sbMarkdown.toString());
			return result;
		}
	}
	
	public static @NotNull ReplacementOptions completeInBExpression(final @NotNull Trace trace, final @NotNull String code, final int at) {
		final Matcher identifierMatcher = B_IDENTIFIER_PATTERN.matcher(code);
		String identifier = "";
		int start = at;
		int end = at;
		// Try to find the identifier that the cursor is in.
		// If the cursor is not on an identifier, default to empty string, i. e. show all possible completions.
		while (identifierMatcher.find() && identifierMatcher.start() < at) {
			if (identifierMatcher.end() >= at) {
				identifier = code.substring(identifierMatcher.start(), at);
				start = identifierMatcher.start();
				end = identifierMatcher.end();
				break;
			}
		}
		
		final CompleteIdentifierCommand cmdExact = new CompleteIdentifierCommand(identifier);
		cmdExact.setIncludeKeywords(true);
		trace.getStateSpace().execute(cmdExact);
		// Use LinkedHashSet to remove duplicates while maintaining order.
		final Set<String> completions = new LinkedHashSet<>(cmdExact.getCompletions());
		
		final CompleteIdentifierCommand cmdIgnoreCase = new CompleteIdentifierCommand(identifier);
		cmdIgnoreCase.setIgnoreCase(true);
		cmdIgnoreCase.setIncludeKeywords(true);
		trace.getStateSpace().execute(cmdIgnoreCase);
		completions.addAll(cmdIgnoreCase.getCompletions());
		
		return new ReplacementOptions(new ArrayList<>(completions), start, end);
	}
}
