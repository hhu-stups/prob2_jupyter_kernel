package de.prob2.jupyter.commands;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.TableData;
import de.prob.animator.domainobjects.TableVisualizationCommand;
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
import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class TableCommand implements Command {
	private static final @NotNull Parameter.RequiredSingle EXPRESSION_PARAM = Parameter.requiredRemainder("expression");
	
	private final @NotNull Provider<@NotNull ProBKernel> kernelProvider;
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private TableCommand(final @NotNull Provider<@NotNull ProBKernel> kernelProvider, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.kernelProvider = kernelProvider;
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":table";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return new Parameters(Collections.singletonList(EXPRESSION_PARAM));
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
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final ProBKernel kernel = this.kernelProvider.get();
		final Trace trace = this.animationSelector.getCurrentTrace();
		final String code = kernel.insertLetVariables(args.get(EXPRESSION_PARAM));
		final IEvalElement formula = CommandUtils.withSourceCode(code, () -> kernel.parseFormula(code, FormulaExpand.EXPAND));
		
		final TableData table = TableVisualizationCommand.getAll(trace.getCurrentState())
			.stream()
			.filter(c -> "expr_as_table".equals(c.getCommand()))
			.findAny()
			.orElseThrow(AssertionError::new)
			.visualize(Collections.singletonList(formula));
		
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
	public @NotNull ParameterInspectors getParameterInspectors() {
		return new ParameterInspectors(Collections.singletonMap(
			EXPRESSION_PARAM, CommandUtils.bExpressionInspector(this.animationSelector.getCurrentTrace())
		));
	}
	
	@Override
	public @NotNull ParameterCompleters getParameterCompleters() {
		return new ParameterCompleters(Collections.singletonMap(
			EXPRESSION_PARAM, CommandUtils.bExpressionCompleter(this.animationSelector.getCurrentTrace())
		));
	}
}
