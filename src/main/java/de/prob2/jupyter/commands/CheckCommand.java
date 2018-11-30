package de.prob2.jupyter.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob.animator.command.GetMachineStructureCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.prologast.ASTCategory;
import de.prob.animator.prologast.ASTFormula;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.unicode.UnicodeTranslator;

import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CheckCommand implements Command {
	private static final @NotNull Map<String, String> SECTION_NAME_MAP;
	static {
		final Map<String, String> sectionNameMap = new HashMap<>();
		sectionNameMap.put("properties", "PROPERTIES");
		sectionNameMap.put("invariant", "INVARIANTS");
		sectionNameMap.put("assertions", "ASSERTIONS");
		SECTION_NAME_MAP = Collections.unmodifiableMap(sectionNameMap);
	}
	
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private CheckCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":check WHAT";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Check the machine's properties, invariant, or assertions in the current state.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "The properties/invariant/assertions are checked and displayed in table form. Each row corresponds to a part of the properties/invariant conjunction, or to an assertion.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		if (!SECTION_NAME_MAP.containsKey(argString)) {
			throw new UserErrorException("Don't know how to check " + argString);
		}
		final String categoryName = SECTION_NAME_MAP.get(argString);
		final Trace trace = this.animationSelector.getCurrentTrace();
		final GetMachineStructureCommand cmd = new GetMachineStructureCommand();
		trace.getStateSpace().execute(cmd);
		final Optional<ASTCategory> category = cmd.getPrologASTList().stream()
			.filter(ASTCategory.class::isInstance)
			.map(ASTCategory.class::cast)
			.filter(c -> categoryName.equals(c.getName()))
			.findAny();
		
		if (category.isPresent()) {
			final StringJoiner sjPlain = new StringJoiner("\n");
			final StringJoiner sjMarkdown = new StringJoiner("\n", "|Predicate|Value|\n|---|---|\n", "");
			category.get()
				.getSubnodes()
				.stream()
				.filter(ASTFormula.class::isInstance)
				.map(ASTFormula.class::cast)
				.forEach(f -> {
					final AbstractEvalResult result = trace.evalCurrent(f.getFormula(FormulaExpand.TRUNCATE));
					sjPlain.add(f.getPrettyPrint() + " = " + CommandUtils.inlinePlainTextForEvalResult(result));
					sjMarkdown.add("|$" + UnicodeTranslator.toLatex(f.getPrettyPrint()) + "$|" + CommandUtils.inlineMarkdownForEvalResult(result) + '|');
				});
			final DisplayData result = new DisplayData(sjPlain.toString());
			result.putMarkdown(sjMarkdown.toString());
			return result;
		} else {
			return new DisplayData("Machine has no " + argString);
		}
	}
	
	@Override
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		return null;
	}
	
	@Override
	public @NotNull ReplacementOptions complete(final @NotNull String argString, final int at) {
		final String prefix = argString.substring(0, at);
		return new ReplacementOptions(SECTION_NAME_MAP.keySet()
			.stream()
			.filter(s -> s.startsWith(prefix))
			.collect(Collectors.toList()), 0, argString.length());
	}
}
