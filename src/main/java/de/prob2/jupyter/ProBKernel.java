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
import com.google.inject.Injector;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.exception.ProBError;
import de.prob.scripting.ClassicalBFactory;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

import de.prob2.jupyter.commands.BrowseCommand;
import de.prob2.jupyter.commands.Command;
import de.prob2.jupyter.commands.CommandExecutionException;
import de.prob2.jupyter.commands.ConstantsCommand;
import de.prob2.jupyter.commands.EvalCommand;
import de.prob2.jupyter.commands.ExecCommand;
import de.prob2.jupyter.commands.GroovyCommand;
import de.prob2.jupyter.commands.HelpCommand;
import de.prob2.jupyter.commands.InitialiseCommand;
import de.prob2.jupyter.commands.LoadCellCommand;
import de.prob2.jupyter.commands.LoadFileCommand;
import de.prob2.jupyter.commands.NoSuchCommandException;
import de.prob2.jupyter.commands.PrefCommand;
import de.prob2.jupyter.commands.PrettyPrintCommand;
import de.prob2.jupyter.commands.RenderCommand;
import de.prob2.jupyter.commands.SolveCommand;
import de.prob2.jupyter.commands.TableCommand;
import de.prob2.jupyter.commands.TimeCommand;
import de.prob2.jupyter.commands.TypeCommand;
import de.prob2.jupyter.commands.VersionCommand;

import io.github.spencerpark.jupyter.kernel.BaseKernel;
import io.github.spencerpark.jupyter.kernel.LanguageInfo;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProBKernel extends BaseKernel {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProBKernel.class);
	
	private static final Pattern COMMAND_PATTERN = Pattern.compile("\\s*(\\:[^\\s]*)(?:\\h*(.*))?", Pattern.DOTALL);
	
	private final @NotNull AnimationSelector animationSelector;
	
	private final @NotNull Map<@NotNull String, @NotNull Command> commands;
	
	@Inject
	private ProBKernel(final @NotNull Injector injector, final @NotNull ClassicalBFactory classicalBFactory, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.setShouldReplaceStdStreams(false);
		
		this.animationSelector = animationSelector;
		
		this.commands = new HashMap<>();
		final Command help = injector.getInstance(HelpCommand.class);
		this.commands.put(":?", help);
		this.commands.put(":help", help);
		this.commands.put(":version", injector.getInstance(VersionCommand.class));
		this.commands.put(":eval", injector.getInstance(EvalCommand.class));
		this.commands.put(":type", injector.getInstance(TypeCommand.class));
		this.commands.put(":table", injector.getInstance(TableCommand.class));
		this.commands.put(":solve", injector.getInstance(SolveCommand.class));
		this.commands.put(":load", injector.getInstance(LoadFileCommand.class));
		this.commands.put("::load", injector.getInstance(LoadCellCommand.class));
		this.commands.put(":pref", injector.getInstance(PrefCommand.class));
		this.commands.put(":browse", injector.getInstance(BrowseCommand.class));
		this.commands.put(":exec", injector.getInstance(ExecCommand.class));
		this.commands.put(":constants", injector.getInstance(ConstantsCommand.class));
		this.commands.put(":initialise", injector.getInstance(InitialiseCommand.class));
		this.commands.put(":init", injector.getInstance(InitialiseCommand.class));
		this.commands.put(":time", injector.getInstance(TimeCommand.class));
		this.commands.put(":groovy", injector.getInstance(GroovyCommand.class));
		this.commands.put("::render", injector.getInstance(RenderCommand.class));
		this.commands.put(":prettyprint", injector.getInstance(PrettyPrintCommand.class));
		
		this.animationSelector.changeCurrentAnimation(new Trace(classicalBFactory.create("MACHINE repl END").load()));
	}
	
	public @NotNull Map<@NotNull String, @NotNull Command> getCommands() {
		return Collections.unmodifiableMap(this.commands);
	}
	
	@Override
	public @NotNull String getBanner() {
		return "ProB Interactive Expression and Predicate Evaluator (on Jupyter)\nType \":help\" for more information.";
	}
	
	@Override
	public @NotNull List<LanguageInfo.@NotNull Help> getHelpLinks() {
		return Collections.singletonList(new LanguageInfo.Help("ProB User Manual", "https://www3.hhu.de/stups/prob/index.php/User_Manual"));
	}
	
	private @NotNull DisplayData executeCommand(final @NotNull String name, final @NotNull String argString) {
		final Command command = this.getCommands().get(name);
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
		
		final Matcher commandMatcher = COMMAND_PATTERN.matcher(expr);
		if (commandMatcher.matches()) {
			final String name = commandMatcher.group(1);
			assert name != null;
			final String argString = commandMatcher.group(2) == null ? "" : commandMatcher.group(2);
			return this.executeCommand(name, argString);
		}
		
		return this.executeCommand(":eval", expr);
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
		try {
			if (e instanceof UserErrorException) {
				return this.errorStyler.secondaryLines(String.valueOf(e.getMessage()));
			} else if (e instanceof ProBError) {
				final ProBError proBError = (ProBError)e;
				final List<String> out = new ArrayList<>(Arrays.asList((
					this.errorStyler.primary("Error from ProB: ")
					+ this.errorStyler.secondary(String.valueOf(proBError.getOriginalMessage()))
				).split("\n")));
				if (proBError.getErrors().isEmpty()) {
					out.addAll(this.errorStyler.primaryLines("ProB returned no error messages.\n"));
				} else if (proBError.getErrors().size() == 1) {
					out.addAll(this.errorStyler.secondaryLines(proBError.getErrors().get(0).toString()));
				} else {
					out.addAll(this.errorStyler.primaryLines(proBError.getErrors().size() + " errors:\n"));
					for (final ErrorItem error : proBError.getErrors()) {
						out.addAll(this.errorStyler.secondaryLines(error.toString()));
					}
				}
				return out;
			} else {
				return super.formatError(e);
			}
		} catch (final RuntimeException e2) {
			LOGGER.error("Exception in error formatting", e2);
			throw e2;
		}
	}
}
