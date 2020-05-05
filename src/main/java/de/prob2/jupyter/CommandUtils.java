package de.prob2.jupyter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.prob.animator.command.CompleteIdentifierCommand;
import de.prob.animator.command.GetCurrentPreferencesCommand;
import de.prob.animator.command.GetDefaultPreferencesCommand;
import de.prob.animator.command.GetPreferenceCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ComputationNotCompletedResult;
import de.prob.animator.domainobjects.EnumerationWarning;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.IdentifierNotInitialised;
import de.prob.animator.domainobjects.ProBPreference;
import de.prob.animator.domainobjects.TypeCheckResult;
import de.prob.animator.domainobjects.WDError;
import de.prob.exception.ProBError;
import de.prob.statespace.Trace;
import de.prob.unicode.UnicodeTranslator;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CommandUtils {
	@FunctionalInterface
	public interface Inspector {
		public abstract @Nullable DisplayData inspect(final @NotNull String argString, final int at);
	}
	
	@FunctionalInterface
	public interface Completer {
		public abstract @Nullable ReplacementOptions complete(final @NotNull String argString, final int at);
	}
	
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(CommandUtils.class);
	
	public static final @NotNull Pattern ARG_SPLIT_PATTERN = Pattern.compile("\\s+");
	private static final @NotNull Pattern B_IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");
	
	private CommandUtils() {
		super();
		
		throw new AssertionError("Utility class");
	}
	
	public static @NotNull String prettyOperationName(final @NotNull String name) {
		switch (name) {
			case "$setup_constants":
				return "SETUP_CONSTANTS";
			
			case "$initialise_machine":
				return "INITIALISATION";
			
			default:
				return name;
		}
	}
	
	public static @NotNull String unprettyOperationName(final @NotNull String name) {
		switch (name) {
			case "SETUP_CONSTANTS":
				return "$setup_constants";
			
			case "INITIALISATION":
				return "$initialise_machine";
			
			default:
				return name;
		}
	}
	
	public static @NotNull List<@NotNull String> splitArgs(final @NotNull String args, final int limit) {
		final List<String> split = new ArrayList<>(Arrays.asList(ARG_SPLIT_PATTERN.split(args, limit)));
		if (!split.isEmpty() && split.get(split.size()-1).isEmpty()) {
			split.remove(split.size()-1);
		}
		return split;
	}
	
	public static @NotNull List<@NotNull String> splitArgs(final @NotNull String args) {
		return splitArgs(args, 0);
	}
	
	private static <T> @NotNull String parseSingleArg(final @NotNull ParsedArguments parsed, final @NotNull String remainingArgs, final @NotNull PositionalParameter<T> param) {
		final Parameter.ParseResult<T> parsedSingleArg = param.parse(remainingArgs);
		parsed.put(param, parsedSingleArg.getParsedArg());
		return parsedSingleArg.getRemainingArgString();
	}
	
	private static <T> void checkParsedParameter(final @NotNull ParsedArguments parsed, final @NotNull PositionalParameter<T> param) {
		if (!parsed.containsKey(param)) {
			if (param.isOptional()) {
				parsed.put(param, param.getDefaultValue());
			} else {
				throw new UserErrorException("Missing required parameter " + param.getIdentifier());
			}
		}
	}
	
	public static @NotNull ParsedArguments parseArgs(final @NotNull Parameters parameters, final @NotNull String argString) {
		final ParsedArguments parsed = new ParsedArguments(Collections.emptyMap());
		String remainingArgs;
		if (parameters.getBodyParam().isPresent()) {
			final String[] split = argString.split("\n", 2);
			final PositionalParameter.RequiredRemainder bodyParam = parameters.getBodyParam().get();
			if (split.length < 2) {
				throw new UserErrorException("Missing required body " + bodyParam.getIdentifier());
			}
			remainingArgs = split[0];
			parsed.put(bodyParam, split[1]);
		} else {
			remainingArgs = argString;
		}
		
		for (final PositionalParameter<?> param : parameters.getPositionalParameters()) {
			if (remainingArgs.isEmpty()) {
				break;
			}
			
			remainingArgs = parseSingleArg(parsed, remainingArgs, param);
		}
		if (!remainingArgs.isEmpty()) {
			throw new UserErrorException("Expected at most " + parameters.getPositionalParameters().size() + " arguments, got extra argument: " + remainingArgs);
		}
		for (final PositionalParameter<?> param : parameters.getPositionalParameters()) {
			checkParsedParameter(parsed, param);
		}
		return parsed;
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
	
	public static <T> T withSourceCode(final @NotNull String code, final Supplier<T> action) {
		try {
			return action.get();
		} catch (final ProBError e) {
			throw new WithSourceCodeException(code, e);
		}
	}
	
	public static void withSourceCode(final @NotNull String code, final Runnable action) {
		withSourceCode(code, () -> {
			action.run();
			return null;
		});
	}
	
	public static @NotNull String insertLetVariables(final @NotNull String code, final @NotNull Map<@NotNull String, @NotNull String> variables) {
		if (variables.isEmpty()) {
			return code;
		} else {
			final StringJoiner varNames = new StringJoiner(",");
			final StringJoiner varAssignments = new StringJoiner("&");
			variables.forEach((name, value) -> {
				varNames.add(name);
				varAssignments.add(name + "=(" + value + ')');
			});
			return String.format("LET %s BE %s IN(\n%s\n)END", varNames, varAssignments, code);
		}
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
			LOGGER.warn("Unknown eval result of type {}, falling back to toString(): {}", aer.getClass(), aer);
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
	
	public static @NotNull String inlinePlainTextForEvalResult(final @NotNull AbstractEvalResult aer) {
		if (aer instanceof EvalResult) {
			return UnicodeTranslator.toUnicode(((EvalResult)aer).getValue());
		} else if (aer instanceof ComputationNotCompletedResult) {
			return "(computation not completed: " + ((ComputationNotCompletedResult)aer).getReason() + ')';
		} else if (aer instanceof IdentifierNotInitialised) {
			return "(not initialised)";
		} else if (aer instanceof WDError) {
			return "(not well-defined)";
		} else if (aer instanceof EvaluationErrorResult) {
			LOGGER.warn("Unknown evaluation error of type {}: {}", aer.getClass(), aer);
			return "(evaluation error: " + ((EvaluationErrorResult)aer).getErrors() + ')';
		} else {
			LOGGER.warn("Unknown eval result of type {}, falling back to toString(): {}", aer.getClass(), aer);
			return aer.toString();
		}
	}
	
	public static @NotNull String inlineMarkdownForEvalResult(final @NotNull AbstractEvalResult aer) {
		if (aer instanceof EvalResult) {
			return '$' + UnicodeTranslator.toLatex(((EvalResult)aer).getValue()) + '$';
		} else if (aer instanceof ComputationNotCompletedResult) {
			return "*(computation not completed: " + ((ComputationNotCompletedResult)aer).getReason() + ")*";
		} else if (aer instanceof IdentifierNotInitialised) {
			return "*(not initialised)*";
		} else if (aer instanceof WDError) {
			return "*(not well-defined)*";
		} else if (aer instanceof EvaluationErrorResult) {
			LOGGER.warn("Unknown evaluation error of type {}: {}", aer.getClass(), aer);
			return "*(evaluation error: " + ((EvaluationErrorResult)aer).getErrors() + ")*";
		} else {
			LOGGER.warn("Unknown eval result of type {}, falling back to toString(): {}", aer.getClass(), aer);
			return aer.toString();
		}
	}
	
	public static @NotNull ReplacementOptions offsetReplacementOptions(final @NotNull ReplacementOptions replacements, final int offset) {
		return new ReplacementOptions(
			replacements.getReplacements(),
			replacements.getSourceStart() + offset,
			replacements.getSourceEnd() + offset
		);
	}
	
	public static @Nullable DisplayData inspectArgs(final @NotNull String argString, final int at, final @NotNull Inspector @NotNull... inspectors) {
		final Matcher argSplitMatcher = ARG_SPLIT_PATTERN.matcher(argString);
		int argStart = 0;
		int argEnd = argString.length();
		int i = 0;
		while (argSplitMatcher.find()) {
			if (argSplitMatcher.end() > at) {
				argEnd = argSplitMatcher.start();
				break;
			}
			argStart = argSplitMatcher.end();
			if (i >= inspectors.length-1) {
				break;
			}
			i++;
		}
		return inspectors[i].inspect(argString.substring(argStart, argEnd), at - argStart);
	}
	
	public static @Nullable ReplacementOptions completeArgs(final @NotNull String argString, final int at, final @NotNull Completer @NotNull... completers) {
		final Matcher argSplitMatcher = ARG_SPLIT_PATTERN.matcher(argString);
		int argStart = 0;
		int argEnd = argString.length();
		int i = 0;
		while (argSplitMatcher.find()) {
			if (argSplitMatcher.end() > at) {
				argEnd = argSplitMatcher.start();
				break;
			}
			argStart = argSplitMatcher.end();
			if (i >= completers.length-1) {
				break;
			}
			i++;
		}
		final ReplacementOptions replacements = completers[i].complete(argString.substring(argStart, argEnd), at - argStart);
		return replacements == null ? null : offsetReplacementOptions(replacements, argStart);
	}
	
	public static @Nullable Matcher matchBIdentifierAt(final @NotNull String code, final int at) {
		final Matcher identifierMatcher = B_IDENTIFIER_PATTERN.matcher(code);
		while (identifierMatcher.find() && identifierMatcher.start() < at) {
			if (identifierMatcher.end() >= at) {
				return identifierMatcher;
			}
		}
		return null;
	}
	
	public static @Nullable DisplayData inspectInBExpression(final @NotNull Trace trace, final @NotNull String code, final int at) {
		final Matcher identifierMatcher = CommandUtils.matchBIdentifierAt(code, at);
		if (identifierMatcher == null) {
			return null;
		}
		final String identifier = identifierMatcher.group();
		final IEvalElement formula = trace.getModel().parseFormula(identifier, FormulaExpand.TRUNCATE);
		
		final StringBuilder sbPlain = new StringBuilder();
		final StringBuilder sbMarkdown = new StringBuilder();
		sbPlain.append(UnicodeTranslator.toUnicode(identifier));
		sbPlain.append('\n');
		sbMarkdown.append('$');
		sbMarkdown.append(UnicodeTranslator.toLatex(identifier));
		sbMarkdown.append("$  \n");
		
		final TypeCheckResult type = trace.getStateSpace().typeCheck(formula);
		sbPlain.append("Type: ");
		sbMarkdown.append("**Type:** ");
		if (type.isOk()) {
			sbPlain.append(type.getType());
			sbMarkdown.append('`');
			sbMarkdown.append(type.getType());
			sbMarkdown.append('`');
		} else {
			sbPlain.append("Type error: ");
			sbPlain.append(type.getErrors());
			sbMarkdown.append("*Type error:* ");
			sbMarkdown.append(type.getErrors());
		}
		sbPlain.append('\n');
		sbMarkdown.append("  \n");
		
		final AbstractEvalResult aer = trace.evalCurrent(formula);
		sbPlain.append("Current value: ");
		sbPlain.append(CommandUtils.inlinePlainTextForEvalResult(aer));
		sbMarkdown.append("**Current value:** ");
		sbMarkdown.append(inlineMarkdownForEvalResult(aer));
		
		final DisplayData result = new DisplayData(sbPlain.toString());
		result.putMarkdown(sbMarkdown.toString());
		return result;
	}
	
	public static @NotNull Inspector bExpressionInspector(final @NotNull Trace trace) {
		return (code, at) -> inspectInBExpression(trace, code, at);
	}
	
	public static @NotNull ReplacementOptions completeInBExpression(final @NotNull Trace trace, final @NotNull String code, final int at) {
		final Matcher identifierMatcher = matchBIdentifierAt(code, at);
		// Try to find the identifier that the cursor is in.
		// If the cursor is not on an identifier, default to empty string, i. e. show all possible completions.
		String prefix;
		int start;
		int end;
		if (identifierMatcher == null) {
			prefix = "";
			start = at;
			end = at;
		} else {
			prefix = code.substring(identifierMatcher.start(), at);
			start = identifierMatcher.start();
			end = identifierMatcher.end();
		}
		
		final CompleteIdentifierCommand cmdExact = new CompleteIdentifierCommand(prefix);
		cmdExact.setIncludeKeywords(true);
		trace.getStateSpace().execute(cmdExact);
		// Use LinkedHashSet to remove duplicates while maintaining order.
		final Set<String> completions = new LinkedHashSet<>(cmdExact.getCompletions());
		
		final CompleteIdentifierCommand cmdIgnoreCase = new CompleteIdentifierCommand(prefix);
		cmdIgnoreCase.setIgnoreCase(true);
		cmdIgnoreCase.setIncludeKeywords(true);
		trace.getStateSpace().execute(cmdIgnoreCase);
		completions.addAll(cmdIgnoreCase.getCompletions());
		
		return new ReplacementOptions(new ArrayList<>(completions), start, end);
	}
	
	public static @NotNull Completer bExpressionCompleter(final @NotNull Trace trace) {
		return (code, at) -> completeInBExpression(trace, code, at);
	}
	
	public static @Nullable DisplayData inspectInPreferences(final @NotNull Trace trace, final @NotNull String code, final int at) {
		final Matcher argSplitMatcher = ARG_SPLIT_PATTERN.matcher(code);
		int prefNameStart = 0;
		while (argSplitMatcher.find() && argSplitMatcher.end() <= at) {
			prefNameStart = argSplitMatcher.end();
		}
		final Matcher prefNameMatcher = B_IDENTIFIER_PATTERN.matcher(code);
		prefNameMatcher.region(prefNameStart, code.length());
		if (prefNameMatcher.lookingAt()) {
			final String name = prefNameMatcher.group();
			final GetPreferenceCommand cmdCurrent = new GetPreferenceCommand(name);
			final GetDefaultPreferencesCommand cmdDefaults = new GetDefaultPreferencesCommand();
			trace.getStateSpace().execute(cmdCurrent, cmdDefaults);
			final String currentValue = cmdCurrent.getValue();
			final ProBPreference pref = cmdDefaults.getPreferences()
				.stream()
				.filter(p -> name.equals(p.name))
				.findAny()
				.orElseThrow(NoSuchElementException::new);
			
			final StringBuilder sbPlain = new StringBuilder();
			final StringBuilder sbMarkdown = new StringBuilder();
			sbPlain.append(name);
			sbPlain.append(" = ");
			sbPlain.append(currentValue);
			sbPlain.append(" (");
			sbPlain.append(pref.type);
			sbPlain.append(")\n");
			sbMarkdown.append(name);
			sbMarkdown.append(" = `");
			sbMarkdown.append(currentValue);
			sbMarkdown.append("` (");
			sbMarkdown.append(pref.type);
			sbMarkdown.append(")  \n");
			
			sbPlain.append(pref.description);
			sbPlain.append('\n');
			sbMarkdown.append(pref.description);
			sbMarkdown.append("  \n");
			
			sbPlain.append("Default value: ");
			sbPlain.append(pref.defaultValue);
			sbPlain.append('\n');
			sbMarkdown.append("**Default value:** `");
			sbMarkdown.append(pref.defaultValue);
			sbMarkdown.append("`  \n");
			
			sbPlain.append("Category: ");
			sbPlain.append(pref.category);
			sbPlain.append('\n');
			sbMarkdown.append("**Category:** `");
			sbMarkdown.append(pref.category);
			sbMarkdown.append("`  \n");
			
			final DisplayData result = new DisplayData(sbPlain.toString());
			result.putMarkdown(sbMarkdown.toString());
			return result;
		} else {
			return null;
		}
	}
	
	public static @NotNull Inspector preferencesInspector(final @NotNull Trace trace) {
		return (code, at) -> inspectInPreferences(trace, code, at);
	}
	
	public static @Nullable ReplacementOptions completeInPreferences(final @NotNull Trace trace, final @NotNull String code, final int at) {
		final Matcher argSplitMatcher = ARG_SPLIT_PATTERN.matcher(code);
		int prefNameStart = 0;
		while (argSplitMatcher.find() && argSplitMatcher.end() <= at) {
			prefNameStart = argSplitMatcher.end();
		}
		final Matcher prefNameMatcher = B_IDENTIFIER_PATTERN.matcher(code);
		prefNameMatcher.region(prefNameStart, code.length());
		if (prefNameMatcher.lookingAt() && at <= prefNameMatcher.end()) {
			final String prefix = code.substring(prefNameMatcher.start(), at);
			final GetCurrentPreferencesCommand cmd = new GetCurrentPreferencesCommand();
			trace.getStateSpace().execute(cmd);
			final List<String> prefs = cmd.getPreferences().keySet().stream().filter(s -> s.startsWith(prefix)).sorted().collect(Collectors.toList());
			return new ReplacementOptions(prefs, prefNameMatcher.start(), prefNameMatcher.end());
		} else {
			return null;
		}
	}
	
	public static @NotNull Completer preferencesCompleter(final @NotNull Trace trace) {
		return (code, at) -> completeInPreferences(trace, code, at);
	}
}
