package de.prob2.jupyter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.scripting.ClassicalBFactory;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

import de.prob2.jupyter.commands.BrowseCommand;
import de.prob2.jupyter.commands.CellCommand;
import de.prob2.jupyter.commands.CommandExecutionException;
import de.prob2.jupyter.commands.ConstantsCommand;
import de.prob2.jupyter.commands.EvalCommand;
import de.prob2.jupyter.commands.ExecCommand;
import de.prob2.jupyter.commands.GroovyCommand;
import de.prob2.jupyter.commands.HelpCommand;
import de.prob2.jupyter.commands.InitialiseCommand;
import de.prob2.jupyter.commands.LineCommand;
import de.prob2.jupyter.commands.LoadCellCommand;
import de.prob2.jupyter.commands.LoadFileCommand;
import de.prob2.jupyter.commands.NoSuchCommandException;
import de.prob2.jupyter.commands.PrefCommand;
import de.prob2.jupyter.commands.SolveCommand;
import de.prob2.jupyter.commands.TimeCommand;
import de.prob2.jupyter.commands.VersionCommand;

import io.github.spencerpark.jupyter.kernel.BaseKernel;
import io.github.spencerpark.jupyter.kernel.LanguageInfo;
import io.github.spencerpark.jupyter.messages.DisplayData;

import org.jetbrains.annotations.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProBKernel extends BaseKernel {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProBKernel.class);
	
	private static final Pattern CELL_COMMAND_PATTERN = Pattern.compile("\\s*(\\:\\:[^\\n\\h]*)(?:\\h+([^\\n]*))?(?:\\n(.*))?", Pattern.DOTALL);
	private static final Pattern LINE_COMMAND_PATTERN = Pattern.compile("\\s*(\\:[^\\h]*)(?:\\h+(.*))?");
	
	private final @NotNull AnimationSelector animationSelector;
	
	private final @NotNull Map<@NotNull String, @NotNull LineCommand> lineCommands;
	private final @NotNull Map<@NotNull String, @NotNull CellCommand> cellCommands;
	
	@Inject
	private ProBKernel(final @NotNull Injector injector, final @NotNull ClassicalBFactory classicalBFactory, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
		
		this.lineCommands = new HashMap<>();
		final LineCommand help = injector.getInstance(HelpCommand.class);
		this.lineCommands.put(":?", help);
		this.lineCommands.put(":help", help);
		this.lineCommands.put(":version", injector.getInstance(VersionCommand.class));
		this.lineCommands.put(":eval", injector.getInstance(EvalCommand.class));
		this.lineCommands.put(":solve", injector.getInstance(SolveCommand.class));
		this.lineCommands.put(":load", injector.getInstance(LoadFileCommand.class));
		this.lineCommands.put(":pref", injector.getInstance(PrefCommand.class));
		this.lineCommands.put(":browse", injector.getInstance(BrowseCommand.class));
		this.lineCommands.put(":exec", injector.getInstance(ExecCommand.class));
		this.lineCommands.put(":constants", injector.getInstance(ConstantsCommand.class));
		this.lineCommands.put(":initialise", injector.getInstance(InitialiseCommand.class));
		this.lineCommands.put(":init", injector.getInstance(InitialiseCommand.class));
		this.lineCommands.put(":time", injector.getInstance(TimeCommand.class));
		this.lineCommands.put(":groovy", injector.getInstance(GroovyCommand.class));
		
		this.cellCommands = new HashMap<>();
		this.cellCommands.put("::load", injector.getInstance(LoadCellCommand.class));
		
		this.animationSelector.changeCurrentAnimation(new Trace(classicalBFactory.create("MACHINE repl END").load()));
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
	
	private @NotNull DisplayData executeCellCommand(final @NotNull String name, final @NotNull String argString, final @NotNull String body) {
		final CellCommand command = this.getCellCommands().get(name);
		if (command == null) {
			throw new NoSuchCommandException(name);
		}
		try {
			return command.run(this, argString, body);
		} catch (final UserErrorException e) {
			throw new CommandExecutionException(name, e);
		}
	}
	
	private @NotNull DisplayData executeLineCommand(final @NotNull String name, final @NotNull String argString) {
		final LineCommand command = this.getLineCommands().get(name);
		if (command == null) {
			throw new NoSuchCommandException(name);
		}
		try {
			return command.run(this, argString);
		} catch (final UserErrorException e) {
			throw new CommandExecutionException(name, e);
		}
	}
	
	@Override
	public @NotNull DisplayData eval(final String expr) {
		assert expr != null;
		
		final Matcher cellMatcher = CELL_COMMAND_PATTERN.matcher(expr);
		if (cellMatcher.matches()) {
			final String name = cellMatcher.group(1);
			assert name != null;
			final String argString = cellMatcher.group(2) == null ? "" : cellMatcher.group(2);
			final String body = cellMatcher.group(3) == null ? "" : cellMatcher.group(3);
			return this.executeCellCommand(name, argString, body);
		}
		
		final Matcher lineMatcher = LINE_COMMAND_PATTERN.matcher(expr);
		if (lineMatcher.matches()) {
			final String name = lineMatcher.group(1);
			assert name != null;
			final String argString = lineMatcher.group(2) == null ? "" : lineMatcher.group(2);
			return this.executeLineCommand(name, argString);
		}
		
		return this.executeLineCommand(":eval", expr);
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
		this.animationSelector.getCurrentTrace().getStateSpace().kill();
	}
	
	@Override
	public @NotNull List<@NotNull String> formatError(final Exception e) {
		if (e instanceof UserErrorException) {
			return this.errorStyler.secondaryLines(e.getMessage());
		} else {
			return super.formatError(e);
		}
	}
}
