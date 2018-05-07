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

import de.prob2.jupyter.commands.CommandExecutionException;
import de.prob2.jupyter.commands.HelpCommand;
import de.prob2.jupyter.commands.NoSuchCommandException;
import de.prob2.jupyter.commands.ReplCommand;

import io.github.spencerpark.jupyter.kernel.BaseKernel;
import io.github.spencerpark.jupyter.kernel.LanguageInfo;
import io.github.spencerpark.jupyter.messages.DisplayData;

import org.jetbrains.annotations.NotNull;

public class ProBKernel extends BaseKernel {
	private static final Pattern COMMAND_PATTERN = Pattern.compile("\\s*\\:(.*)");
	
	private final @NotNull Map<@NotNull String, @NotNull ReplCommand> commands;
	private @NotNull Trace trace;
	
	@Inject
	private ProBKernel(final @NotNull ClassicalBFactory classicalBFactory) {
		super();
		
		this.commands = new HashMap<>();
		final ReplCommand help = new HelpCommand();
		this.commands.put("?", help);
		this.commands.put("help", help);
		this.trace = new Trace(classicalBFactory.create("MACHINE repl END").load());
	}
	
	public @NotNull Map<@NotNull String, @NotNull ReplCommand> getCommands() {
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
	
	private @NotNull DisplayData executeCommand(final @NotNull String name, final @NotNull List<@NotNull String> args) {
		final ReplCommand command = this.getCommands().get(name);
		if (command == null) {
			throw new NoSuchCommandException(name);
		}
		return command.run(this, name, args);
	}
	
	@Override
	public @NotNull DisplayData eval(final String expr) {
		assert expr != null;
		
		final Matcher matcher = COMMAND_PATTERN.matcher(expr);
		if (matcher.matches()) {
			final List<String> args = new ArrayList<>(Arrays.asList(matcher.group(1).split("\\s+")));
			// args always contains at least one element, even for an empty string (in that case the only element is an empty string).
			assert args.size() >= 1;
			final String name = args.remove(0);
			return this.executeCommand(name, args);
		} else {
			final AbstractEvalResult result = this.trace.evalCurrent(expr, FormulaExpand.EXPAND);
			return new DisplayData(result.toString());
		}
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
