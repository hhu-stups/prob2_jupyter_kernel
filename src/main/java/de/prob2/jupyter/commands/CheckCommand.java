package de.prob2.jupyter.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.model.classicalb.Assertion;
import de.prob.model.representation.AbstractFormulaElement;
import de.prob.model.representation.AbstractTheoremElement;
import de.prob.model.representation.Axiom;
import de.prob.model.representation.Invariant;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.unicode.UnicodeTranslator;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.CommandUtils;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CheckCommand implements Command {
	private static final @NotNull Map<@NotNull String, @NotNull Class<? extends AbstractTheoremElement>> CHILDREN_BASE_CLASS_MAP;
	static {
		final Map<String, Class<? extends AbstractTheoremElement>> childrenBaseClassMap = new HashMap<>();
		childrenBaseClassMap.put("properties", Axiom.class);
		childrenBaseClassMap.put("invariant", Invariant.class);
		childrenBaseClassMap.put("assertions", Assertion.class);
		CHILDREN_BASE_CLASS_MAP = Collections.unmodifiableMap(childrenBaseClassMap);
	}
	
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private CheckCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":check";
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
		if (!CHILDREN_BASE_CLASS_MAP.containsKey(argString)) {
			throw new UserErrorException("Don't know how to check " + argString);
		}
		final Class<? extends AbstractTheoremElement> childrenBaseClass = CHILDREN_BASE_CLASS_MAP.get(argString);
		final Trace trace = this.animationSelector.getCurrentTrace();
		// Find all children of a subclass of childrenBaseClass.
		// This needs to be done manually, because getChildrenOfType only returns children whose class *exactly* matches the given class.
		// For example, getChildrenOfType(Axiom.class) doesn't return children of class Property (which is a subclass of Axiom).
		final List<IEvalElement> formulas = new ArrayList<>();
		trace.getStateSpace().getMainComponent().getChildren().forEach((clazz, children) -> {
			if (childrenBaseClass.isAssignableFrom(clazz)) {
				children.stream()
					.map(childrenBaseClass::cast)
					.map(AbstractFormulaElement::getFormula)
					.collect(Collectors.toCollection(() -> formulas));
			}
		});
		
		final StringJoiner sjPlain = new StringJoiner("\n");
		final StringJoiner sjMarkdown = new StringJoiner("\n", "|Predicate|Value|\n|---|---|\n", "");
		for (final IEvalElement f : formulas) {
			final AbstractEvalResult result = trace.evalCurrent(f);
			sjPlain.add(f.getCode() + " = " + CommandUtils.inlinePlainTextForEvalResult(result));
			sjMarkdown.add("|$" + UnicodeTranslator.toLatex(f.getCode()) + "$|" + CommandUtils.inlineMarkdownForEvalResult(result) + '|');
		}
		final DisplayData result = new DisplayData(sjPlain.toString());
		result.putMarkdown(sjMarkdown.toString());
		return result;
	}
	
	@Override
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		return null;
	}
	
	@Override
	public @NotNull ReplacementOptions complete(final @NotNull String argString, final int at) {
		final String prefix = argString.substring(0, at);
		return new ReplacementOptions(CHILDREN_BASE_CLASS_MAP.keySet()
			.stream()
			.filter(s -> s.startsWith(prefix))
			.collect(Collectors.toList()), 0, argString.length());
	}
}
