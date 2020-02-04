package de.prob2.jupyter.commands;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.animator.command.GetAllTableCommands;
import de.prob.animator.command.GetTableForVisualizationCommand;
import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.TableData;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.unicode.UnicodeTranslator;
import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TableCommand implements Command {
	private final @NotNull Provider<@NotNull ProBKernel> kernelProvider;
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private TableCommand(final @NotNull Provider<@NotNull ProBKernel> kernelProvider, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.kernelProvider = kernelProvider;
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":table EXPRESSION";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Display an expression as a table.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "Although any expression is accepted, this command is most useful for sets of tuples.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		final Trace trace = this.animationSelector.getCurrentTrace();
		final String code = this.kernelProvider.get().insertLetVariables(argString);
		final IEvalElement formula = CommandUtils.withSourceCode(code, () -> trace.getModel().parseFormula(code, FormulaExpand.EXPAND));
		
		final GetAllTableCommands cmd1 = new GetAllTableCommands(trace.getCurrentState());
		trace.getStateSpace().execute(cmd1);
		final DynamicCommandItem dc = cmd1.getCommands().stream()
			.filter(c -> "expr_as_table".equals(c.getCommand()))
			.findAny()
			.orElseThrow(AssertionError::new);
		final GetTableForVisualizationCommand cmd2 = new GetTableForVisualizationCommand(trace.getCurrentState(), dc, Collections.singletonList(formula));
		trace.getStateSpace().execute(cmd2);
		final TableData table = cmd2.getTable();
		
		final StringBuilder sbPlain = new StringBuilder();
		final StringBuilder sbMarkdown = new StringBuilder();
		
		sbPlain.append(String.join("\t", table.getHeader()));
		sbPlain.append('\n');
		sbMarkdown.append('|');
		sbMarkdown.append(String.join("|", table.getHeader()));
		sbMarkdown.append("|\n|");
		for (int i = 0; i < table.getHeader().size(); i++) {
			sbMarkdown.append("---|");
		}
		sbMarkdown.append('\n');
		
		for (final List<String> row : table.getRows()) {
			sbPlain.append(String.join("\t", row));
			sbPlain.append('\n');
			sbMarkdown.append('|');
			sbMarkdown.append(row.stream().map(s -> '$' + UnicodeTranslator.toLatex(s) + '$').collect(Collectors.joining("|")));
			sbMarkdown.append("|\n");
		}
		
		final DisplayData res = new DisplayData(sbPlain.toString());
		res.putMarkdown(sbMarkdown.toString());
		return res;
	}
	
	@Override
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		return CommandUtils.inspectInBExpression(this.animationSelector.getCurrentTrace(), argString, at);
	}
	
	@Override
	public @NotNull ReplacementOptions complete(final @NotNull String argString, final int at) {
		return CommandUtils.completeInBExpression(this.animationSelector.getCurrentTrace(), argString, at);
	}
}
