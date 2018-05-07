package de.prob2.jupyter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.scripting.ClassicalBFactory;
import de.prob.statespace.Trace;

import de.prob2.jupyter.commands.CellCommand;
import de.prob2.jupyter.commands.CommandExecutionException;
import de.prob2.jupyter.commands.EchoCellCommand;
import de.prob2.jupyter.commands.HelpCommand;
import de.prob2.jupyter.commands.LineCommand;
import de.prob2.jupyter.commands.NoSuchCommandException;

import io.github.spencerpark.jupyter.kernel.BaseKernel;
import io.github.spencerpark.jupyter.kernel.LanguageInfo;
import io.github.spencerpark.jupyter.messages.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class ProBKernel extends BaseKernel {
	private static final Pattern CELL_COMMAND_PATTERN = Pattern.compile("\\s*(\\:\\:[^\n]*)(?:\n(.*))?", Pattern.DOTALL);
	private static final Pattern LINE_COMMAND_PATTERN = Pattern.compile("\\s*(\\:.*)");
	
	private final @NotNull Map<@NotNull String, @NotNull LineCommand> lineCommands;
	private final @NotNull Map<@NotNull String, @NotNull CellCommand> cellCommands;
	private @NotNull Trace trace;
	
	@Inject
	private ProBKernel(final @NotNull ClassicalBFactory classicalBFactory) {
		super();
		
		this.lineCommands = new HashMap<>();
		final LineCommand help = new HelpCommand();
		this.lineCommands.put(":?", help);
		this.lineCommands.put(":help", help);
		
		this.cellCommands = new HashMap<>();
		this.cellCommands.put("::echo", new EchoCellCommand());
		
		this.trace = new Trace(classicalBFactory.create("MACHINE repl END").load());
	}
	
	public @NotNull Map<@NotNull String, @NotNull CellCommand> getCellCommands() {
		return Collections.unmodifiableMap(this.cellCommands);
	}
	
	public @NotNull Map<@NotNull String, @NotNull LineCommand> getLineCommands() {
		return Collections.unmodifiableMap(this.lineCommands);
	}
	
	@Override
	public @NotNull String getBanner() {
		return "ProB Interactive Expression and Predicate Evaluator (on Jupyter)\nType \":help\" for more information.";
	}
	
	@Override
	public @NotNull List<LanguageInfo.@NotNull Help> getHelpLinks() {
		return Collections.singletonList(new LanguageInfo.Help("ProB User Manual", "https://www3.hhu.de/stups/prob/index.php/User_Manual"));
	}
	
	private @NotNull DisplayData executeCellCommand(final @NotNull String name, final @NotNull List<@NotNull String> args, final @NotNull String body) {
		final CellCommand command = this.getCellCommands().get(name);
		if (command == null) {
			throw new NoSuchCommandException(name);
		}
		return command.run(this, name, args, body);
	}
	
	private @NotNull DisplayData executeLineCommand(final @NotNull String name, final @NotNull List<@NotNull String> args) {
		final LineCommand command = this.getLineCommands().get(name);
		if (command == null) {
			throw new NoSuchCommandException(name);
		}
		return command.run(this, name, args);
	}
	
	@Override
	public @NotNull DisplayData eval(final String expr) {
		assert expr != null;
		
		final Matcher cellMatcher = CELL_COMMAND_PATTERN.matcher(expr);
		if (cellMatcher.matches()) {
			final List<String> args = new ArrayList<>(Arrays.asList(cellMatcher.group(1).split("\\s+")));
			// args always contains at least one element, even for an empty string (in that case the only element is an empty string).
			assert args.size() >= 1;
			final String name = args.remove(0);
			final String body = cellMatcher.group(2) == null ? "" : cellMatcher.group(2);
			return this.executeCellCommand(name, args, body);
		}
		
		final Matcher lineMatcher = LINE_COMMAND_PATTERN.matcher(expr);
		if (lineMatcher.matches()) {
			final List<String> args = new ArrayList<>(Arrays.asList(lineMatcher.group(1).split("\\s+")));
			// args always contains at least one element, even for an empty string (in that case the only element is an empty string).
			assert args.size() >= 1;
			final String name = args.remove(0);
			return this.executeLineCommand(name, args);
		}
		
		final AbstractEvalResult result = this.trace.evalCurrent(expr, FormulaExpand.EXPAND);
		return new DisplayData(result.toString());
	}
	
	@Override
	public @NotNull LanguageInfo getLanguageInfo() {
		return new LanguageInfo.Builder("prob")
			.mimetype("text/x-prob")
			.fileExtension(".prob")
			.build();
	}
	
	@Override
	public void onShutdown(final boolean isRestarting) {
		this.trace.getStateSpace().kill();
	}
	
	@Override
	public @NotNull List<@NotNull String> formatError(final Exception e) {
		if (e instanceof NoSuchCommandException || e instanceof CommandExecutionException) {
			return this.errorStyler.secondaryLines(e.getMessage());
		} else {
			return super.formatError(e);
		}
	}
}
