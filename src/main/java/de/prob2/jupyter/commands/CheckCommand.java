package de.prob2.jupyter.commands;

import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.model.classicalb.ClassicalBMachine;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractFormulaElement;
import de.prob.model.representation.ConstantsComponent;
import de.prob.model.representation.Machine;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.unicode.UnicodeTranslator;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.CommandUtils;
import de.prob2.jupyter.Parameter;
import de.prob2.jupyter.ParameterCompleters;
import de.prob2.jupyter.ParameterInspectors;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class CheckCommand implements Command {
	private static final @NotNull Parameter.RequiredSingle WHAT_PARAM = Parameter.required("what");
	
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
	public @NotNull Parameters getParameters() {
		return new Parameters(Collections.singletonList(WHAT_PARAM));
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
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final String what = args.get(WHAT_PARAM);
		final Trace trace = this.animationSelector.getCurrentTrace();
		final AbstractElement mainComponent = trace.getStateSpace().getMainComponent();
		final List<? extends AbstractFormulaElement> elements;
		switch (what) {
			case "properties":
				if (!(mainComponent instanceof ConstantsComponent)) {
					throw new UserErrorException("Checking " + what + " is only supported for classical B machines or Event-B contexts");
				}
				elements = ((ConstantsComponent)mainComponent).getAxioms();
				break;
			
			case "invariant":
				if (!(mainComponent instanceof Machine)) {
					throw new UserErrorException("Checking " + what + " is only supported for classical B or Event-B machines");
				}
				elements = ((Machine)mainComponent).getInvariants();
				break;
			
			case "assertions":
				if (!(mainComponent instanceof ClassicalBMachine)) {
					throw new UserErrorException("Checking " + what + " is only supported for classical B machines");
				}
				elements = ((ClassicalBMachine)mainComponent).getAssertions();
				break;
			
			default:
				throw new UserErrorException("Don't know how to check " + what);
		}
		
		final StringJoiner sjPlain = new StringJoiner("\n");
		final StringJoiner sjMarkdown = new StringJoiner("\n", "|Predicate|Value|\n|---|---|\n", "");
		for (final AbstractFormulaElement element : elements) {
			final IEvalElement f = element.getFormula();
			final AbstractEvalResult result = trace.evalCurrent(f);
			sjPlain.add(f.getCode() + " = " + CommandUtils.inlinePlainTextForEvalResult(result));
			sjMarkdown.add("|" + UnicodeTranslator.toUnicode(f.getCode()) + "|" + CommandUtils.inlineMarkdownForEvalResult(result) + '|');
		}
		final DisplayData result = new DisplayData(sjPlain.toString());
		result.putMarkdown(sjMarkdown.toString());
		return result;
	}
	
	@Override
	public @NotNull ParameterInspectors getParameterInspectors() {
		return ParameterInspectors.NONE;
	}
	
	@Override
	public @NotNull ParameterCompleters getParameterCompleters() {
		return new ParameterCompleters(Collections.singletonMap(
			WHAT_PARAM, (argString, at) -> {
				final String prefix = argString.substring(0, at);
				return new ReplacementOptions(Stream.of("properties", "invariant", "assertions")
					.filter(s -> s.startsWith(prefix))
					.collect(Collectors.toList()), 0, argString.length());
			}
		));
	}
}
