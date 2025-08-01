package de.prob2.jupyter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.prob.animator.command.CompleteIdentifierCommand;
import de.prob.animator.command.GetCurrentPreferencesCommand;
import de.prob.animator.command.GetDefaultPreferencesCommand;
import de.prob.animator.command.GetPreferenceCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EnumerationWarning;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.ProBPreference;
import de.prob.animator.domainobjects.TypeCheckResult;
import de.prob.exception.ProBError;
import de.prob.formula.PredicateBuilder;
import de.prob.statespace.Trace;
import de.prob.unicode.UnicodeTranslator;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CommandUtils {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(CommandUtils.class);
	
	private static final @NotNull Pattern BODY_SPLIT_PATTERN = Pattern.compile("\\n");
	public static final @NotNull Pattern ARG_SPLIT_PATTERN = Pattern.compile("\\s+");
	private static final @NotNull Pattern B_IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");
	public static final @NotNull String JUPYTER_RESULT_VARIABLE_NAME = "jUpYtEr_rEsUlT__";
	public static final @NotNull String JUPYTER_DUMMY_VARIABLE_NAME = "jUpYtEr_dUmMy__";
	
	private CommandUtils() {
		super();
		
		throw new AssertionError("Utility class");
	}
	
	/**
	 * <p>
	 * Split an argument string according to the given parameter specification.
	 * The splitting process stops either when the {@code upToPosition} index is exceeded,
	 * when all parameters have been fully parsed,
	 * or when the entire argument string has been consumed.
	 * </p>
	 * <p>
	 * This method only splits the argument string and does not perform any validation,
	 * which means that it will parse any input string without errors
	 * (although the result might not be meaningful).
	 * Use {@link #validateSplitArgs(Parameters, SplitResult)} to validate the result of this method,
	 * or use {@link #parseArgs(Parameters, PositionedString)} to perform splitting and validation in a single call.
	 * </p>
	 * 
	 * @param parameters the parameter specification based on which the arguments should be split
	 * @param argString the argument string to split
	 * @param upToPosition the position in the argument string after which to stop splitting
	 * @return the result of the split operation (see {@link SplitResult} for details)
	 */
	public static @NotNull SplitResult splitArgs(final @NotNull Parameters parameters, final @NotNull PositionedString argString, final int upToPosition) {
		final SplitArguments splitArgs = new SplitArguments(Collections.emptyMap());
		PositionedString remainingArgs = argString;
		if (parameters.getBodyParam().isPresent()) {
			final Matcher bodySplitMatcher = BODY_SPLIT_PATTERN.matcher(remainingArgs.getValue());
			if (bodySplitMatcher.find()) {
				final PositionedString bodyValue = remainingArgs.substring(bodySplitMatcher.end());
				remainingArgs = remainingArgs.substring(0, bodySplitMatcher.start());
				splitArgs.add(parameters.getBodyParam().get(), bodyValue);
			}
		}
		
		Parameter<?> parameterAtPosition = null;
		for (int i = 0; i < parameters.getPositionalParameters().size();) {
			final Parameter<?> param = parameters.getPositionalParameters().get(i);
			if (remainingArgs.getValue().isEmpty()) {
				break;
			}
			
			final Parameter.SplitResult splitSingleArg = param.getSplitter().split(remainingArgs);
			splitArgs.add(param, splitSingleArg.getSplitArg());
			remainingArgs = splitSingleArg.getRemainingArgString();
			
			if (remainingArgs.getValue().isEmpty() || remainingArgs.getStartPosition() > upToPosition) {
				parameterAtPosition = param;
				break;
			}
			
			if (!param.isRepeating()) {
				i++;
			}
		}
		
		if (parameters.getBodyParam().isPresent() && splitArgs.containsKey(parameters.getBodyParam().get())) {
			final List<PositionedString> bodyParamValues = splitArgs.get(parameters.getBodyParam().get());
			assert bodyParamValues.size() == 1;
			if (upToPosition >= bodyParamValues.get(0).getStartPosition()) {
				parameterAtPosition = parameters.getBodyParam().get();
			}
		}
		
		return new SplitResult(splitArgs, parameterAtPosition, remainingArgs);
	}
	
	/**
	 * <p>
	 * Split an argument string according to the given parameter specification.
	 * The splitting process stops either when all parameters have been fully parsed,
	 * or when the entire argument string has been consumed.
	 * </p>
	 * <p>
	 * This method only splits the argument string and does not perform any validation,
	 * which means that it will parse any input string without errors
	 * (although the result might not be meaningful).
	 * Use {@link #validateSplitArgs(Parameters, SplitResult)} to validate the result of this method,
	 * or use {@link #parseArgs(Parameters, PositionedString)} to perform splitting and validation in a single call.
	 * </p>
	 * <p>
	 * This method is equivalent to calling {@link #splitArgs(Parameters, PositionedString, int)} with {@code upToPosition} set so that as much as possible of the argument string is consumed.
	 * </p>
	 *
	 * @param parameters the parameter specification based on which the arguments should be split
	 * @param argString the argument string to split
	 * @return the result of the split operation (see {@link SplitResult} for details)
	 */
	public static @NotNull SplitResult splitArgs(final @NotNull Parameters parameters, final @NotNull PositionedString argString) {
		return splitArgs(parameters, argString, argString.getEndPosition());
	}
	
	private static <T> void validateSplitParameter(final @NotNull ParsedArguments parsed, final @NotNull SplitArguments splitArgs, final @NotNull Parameter<T> param) {
		parsed.put(param, param.getValidator().validate(param, splitArgs.get(param)));
	}
	
	/**
	 * Validate a {@link SplitResult} according to the given parameter specification.
	 * 
	 * @param parameters the parameter specification based on which the split result should be validated
	 * @param split the split result to validate
	 * @return the parsed arguments after validation
	 * @throws UserErrorException if the split arguments are invalid
	 */
	public static @NotNull ParsedArguments validateSplitArgs(final @NotNull Parameters parameters, final SplitResult split) {
		if (!split.getRemaining().getValue().isEmpty()) {
			throw new UserErrorException("Expected at most " + parameters.getPositionalParameters().size() + " arguments, got extra argument: " + split.getRemaining().getValue());
		}
		
		final ParsedArguments parsed = new ParsedArguments(Collections.emptyMap());
		
		for (final Parameter<?> param : parameters.getPositionalParameters()) {
			validateSplitParameter(parsed, split.getArguments(), param);
		}
		
		parameters.getBodyParam().ifPresent(bodyParam -> validateSplitParameter(parsed, split.getArguments(), bodyParam));
		
		return parsed;
	}
	
	/**
	 * Parse an argument string according to the given parameter specification.
	 * This is a shorthand for calling {@link #splitArgs(Parameters, PositionedString)} followed by {@link #validateSplitArgs(Parameters, SplitResult)}.
	 * 
	 * @param parameters the parameter specification based on which the argument string should be parsed
	 * @param argString the argument string to parse
	 * @return the parsed and validated argument string
	 * @throws UserErrorException if the argument string is invalid
	 */
	public static @NotNull ParsedArguments parseArgs(final @NotNull Parameters parameters, final @NotNull PositionedString argString) {
		return validateSplitArgs(parameters, splitArgs(parameters, argString));
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
	
	public static <T> T withSourceCode(final @NotNull IEvalElement formula, final Supplier<T> action) {
		return withSourceCode(formula.getCode(), action);
	}
	
	public static void withSourceCode(final @NotNull IEvalElement formula, final Runnable action) {
		withSourceCode(formula.getCode(), action);
	}
	
	public static @NotNull String insertClassicalBLetVariables(final @NotNull String code, final @NotNull Map<@NotNull String, @NotNull String> variables) {
		if (variables.isEmpty()) {
			return code;
		} else {
			final String varNames = String.join(",", variables.keySet());
			final PredicateBuilder varAssignments = new PredicateBuilder();
			varAssignments.addMap(variables);
			return String.format("LET %s BE %s IN(\n%s\n)END", varNames, varAssignments, code);
		}
	}
	
	public static @NotNull String insertEventBPredicateLetVariables(final @NotNull String predicate, final @NotNull Map<@NotNull String, @NotNull String> variables) {
		if (variables.isEmpty()) {
			return predicate;
		} else {
			final String varNames = String.join(",", variables.keySet());
			final PredicateBuilder varAssignments = new PredicateBuilder();
			varAssignments.addMap(variables);
			varAssignments.add(predicate);
			return String.format("#%s.(\n%s\n)", varNames, varAssignments);
		}
	}
	
	public static @NotNull String insertEventBExpressionLetVariables(final @NotNull String expression, final @NotNull Map<@NotNull String, @NotNull String> variables) {
		if (variables.isEmpty()) {
			return expression;
		} else {
			final String varNames = String.join(",", variables.keySet());
			final PredicateBuilder varAssignments = new PredicateBuilder();
			varAssignments.addMap(variables);
			varAssignments.add(JUPYTER_RESULT_VARIABLE_NAME, expression);
			// this was not working: return String.format("#%s.(\n%s\n)", varNames, varAssignments);
			// generate {dummy |-> jupres|#(x,y).(dummy=1 & x=1 &y=1 & jupres=x+y)}(1)
			return String.format("{%s |-> %s | %s=1 & #%s.(\n%s\n)}(1)",
			                 JUPYTER_DUMMY_VARIABLE_NAME, JUPYTER_RESULT_VARIABLE_NAME,
			                 JUPYTER_DUMMY_VARIABLE_NAME, varNames, varAssignments);
		}
	}
	
	public static @NotNull DisplayData displayDataForEvalResult(final @NotNull AbstractEvalResult aer) {
		final StringBuilder sb = new StringBuilder();
		final StringBuilder sbMarkdown = new StringBuilder();
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
		} else if (aer instanceof EnumerationWarning) {
			throw new ProBError(aer.toString());
		} else if (aer instanceof EvaluationErrorResult) {
			final EvaluationErrorResult result = (EvaluationErrorResult)aer;
			throw new ProBError(result.getResult(), result.getErrorItems());
		} else {
			sb.append(aer);
			sbMarkdown.append(aer);
		}
		
		final DisplayData result = new DisplayData(sb.toString());
		result.putMarkdown(sbMarkdown.toString());
		return result;
	}
	
	public static @NotNull String inlinePlainTextForEvalResult(final @NotNull AbstractEvalResult aer) {
		if (aer instanceof EvalResult) {
			return UnicodeTranslator.toUnicode(((EvalResult)aer).getValue());
		} else if (aer instanceof EvaluationErrorResult) {
			final EvaluationErrorResult result = (EvaluationErrorResult)aer;
			final StringBuilder sb = new StringBuilder("(");
			sb.append(result.getResult());
			if (!result.getErrorItems().isEmpty()) {
				sb.append(": ");
				sb.append(result.getErrorItems().stream()
					.map(ErrorItem::getMessage)
					.collect(Collectors.joining(", ")));
			}
			sb.append(")");
			return sb.toString();
		} else {
			return aer.toString();
		}
	}
	
	public static @NotNull String inlineMarkdownForEvalResult(final @NotNull AbstractEvalResult aer) {
		if (aer instanceof EvaluationErrorResult) {
			// Display errors in italics
			return "*" + inlinePlainTextForEvalResult(aer) + "*";
		} else {
			return inlinePlainTextForEvalResult(aer);
		}
	}
	
	public static @Nullable Matcher matchBIdentifierAt(final @NotNull String code, final int at) {
		final Matcher identifierMatcher = B_IDENTIFIER_PATTERN.matcher(code);
		while (identifierMatcher.find() && identifierMatcher.start() <= at) {
			if (identifierMatcher.end() >= at) {
				return identifierMatcher;
			}
		}
		return null;
	}
	
	private static @NotNull DisplayData formatBExpressionInspectText(final String identifier, final TypeCheckResult type, final AbstractEvalResult currentValue) {
		final StringBuilder sbPlain = new StringBuilder();
		final StringBuilder sbMarkdown = new StringBuilder();
		sbPlain.append(UnicodeTranslator.toUnicode(identifier));
		sbPlain.append('\n');
		sbMarkdown.append('$');
		sbMarkdown.append(UnicodeTranslator.toLatex(identifier));
		sbMarkdown.append("$  \n");
		
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
		
		sbPlain.append("Current value: ");
		sbPlain.append(CommandUtils.inlinePlainTextForEvalResult(currentValue));
		sbMarkdown.append("**Current value:** ");
		sbMarkdown.append(inlineMarkdownForEvalResult(currentValue));
		
		final DisplayData result = new DisplayData(sbPlain.toString());
		result.putMarkdown(sbMarkdown.toString());
		return result;
	}
	
	public static @NotNull Inspector bExpressionInspector(final @NotNull Trace trace) {
		return (code, at) -> {
			final Matcher identifierMatcher = CommandUtils.matchBIdentifierAt(code, at);
			if (identifierMatcher == null) {
				return null;
			}
			final String identifier = identifierMatcher.group();
			// TODO Handle non-default languages (i. e. forced classical B or Event-B parsing) - important for the different meaning of INT in classical B vs. Event-B
			final IEvalElement formula = trace.getModel().parseFormula(identifier, FormulaExpand.TRUNCATE);
			final TypeCheckResult type = trace.getStateSpace().typeCheck(formula);
			final AbstractEvalResult currentValue = trace.evalCurrent(formula);
			
			return formatBExpressionInspectText(identifier, type, currentValue);
		};
	}
	
	public static @NotNull Completer bExpressionCompleter(final @NotNull Trace trace) {
		return (code, at) -> {
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
			cmdExact.addKeywordContext(CompleteIdentifierCommand.KeywordContext.ALL);
			trace.getStateSpace().execute(cmdExact);
			// Use LinkedHashSet to remove duplicates while maintaining order.
			final Set<String> completions = new LinkedHashSet<>();
			for (CompleteIdentifierCommand.Completion completion : cmdExact.getCompletions()) {
				completions.add(completion.getCompletion());
			}
			
			final CompleteIdentifierCommand cmdIgnoreCase = new CompleteIdentifierCommand(prefix);
			cmdIgnoreCase.setIgnoreCase(true);
			cmdIgnoreCase.addKeywordContext(CompleteIdentifierCommand.KeywordContext.ALL);
			trace.getStateSpace().execute(cmdIgnoreCase);
			for (CompleteIdentifierCommand.Completion completion : cmdExact.getCompletions()) {
				completions.add(completion.getCompletion());
			}
			
			return new ReplacementOptions(new ArrayList<>(completions), start, end);
		};
	}
	
	private static @NotNull DisplayData formatPreferenceInspectText(final String name, final String currentValue, final ProBPreference pref) {
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
	}
	
	public static @NotNull Inspector preferenceInspector(final @NotNull Trace trace) {
		return (code, at) -> {
			final Matcher prefNameMatcher = B_IDENTIFIER_PATTERN.matcher(code);
			if (prefNameMatcher.lookingAt() && at <= prefNameMatcher.end()) {
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
				
				return formatPreferenceInspectText(name, currentValue, pref);
			} else {
				return null;
			}
		};
	}
	
	public static @NotNull Completer preferenceCompleter(final @NotNull Trace trace) {
		return (code, at) -> {
			final Matcher prefNameMatcher = B_IDENTIFIER_PATTERN.matcher(code);
			if (prefNameMatcher.lookingAt() && at <= prefNameMatcher.end()) {
				final String prefix = code.substring(prefNameMatcher.start(), at);
				final GetCurrentPreferencesCommand cmd = new GetCurrentPreferencesCommand();
				trace.getStateSpace().execute(cmd);
				final List<String> prefs = cmd.getPreferences().keySet().stream().filter(s -> s.startsWith(prefix)).sorted().collect(Collectors.toList());
				return new ReplacementOptions(prefs, prefNameMatcher.start(), prefNameMatcher.end());
			} else {
				return null;
			}
		};
	}
}
