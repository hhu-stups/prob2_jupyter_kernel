package de.prob2.jupyter.commands;

import java.util.List;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.ParameterCompleters;
import de.prob2.jupyter.ParameterInspectors;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class TraceCommand implements Command {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private TraceCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":trace";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return Parameters.NONE;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":trace";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Display all states and executed operations in the current trace.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "Each state has an index, which can be passed to the `:goto` command to go to that state.\n\n"
			+ "The first state (index -1) is always the root state. All other states are reached from the root state by following previously executed operations.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final Trace trace = this.animationSelector.getCurrentTrace();
		final StringBuilder sbPlain = new StringBuilder("-1: Root state");
		final StringBuilder sbMarkdown = new StringBuilder("* -1: Root state");
		
		if (trace.getCurrent().getIndex() == -1) {
			sbPlain.append(" (current)");
			sbMarkdown.append(" **(current)**");
		}
		
		final List<Transition> transitionList = trace.getTransitionList();
		for (int i = 0; i < transitionList.size(); i++) {
			final Transition transition = transitionList.get(i).evaluate(FormulaExpand.TRUNCATE);
			sbPlain.append('\n');
			sbPlain.append(i);
			sbPlain.append(": ");
			sbPlain.append(transition.getPrettyRep());
			sbMarkdown.append("\n* ");
			sbMarkdown.append(i);
			sbMarkdown.append(": `");
			sbMarkdown.append(transition.getPrettyRep());
			sbMarkdown.append('`');
			
			if (trace.getCurrent().getIndex() == i) {
				sbPlain.append(" (current)");
				sbMarkdown.append(" **(current)**");
			}
		}
		
		final DisplayData result = new DisplayData(sbPlain.toString());
		result.putMarkdown(sbMarkdown.toString());
		return result;
	}
	
	@Override
	public @NotNull ParameterInspectors getParameterInspectors() {
		return ParameterInspectors.NONE;
	}
	
	@Override
	public @NotNull ParameterCompleters getParameterCompleters() {
		return ParameterCompleters.NONE;
	}
}
