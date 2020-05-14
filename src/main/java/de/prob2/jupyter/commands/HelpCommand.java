package de.prob2.jupyter.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.jupyter.Command;
import de.prob2.jupyter.Parameter;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class HelpCommand implements Command {
	private static final @NotNull Parameter.OptionalSingle COMMAND_NAME_PARAM = Parameter.optional("commandName");
	
	private static final @NotNull Map<@NotNull String, @NotNull List<@NotNull Class<? extends Command>>> COMMAND_CLASS_CATEGORIES;
	static {
		final Map<String, List<Class<? extends Command>>> commandCategories = new LinkedHashMap<>();
		commandCategories.put("Evaluation", Collections.unmodifiableList(Arrays.asList(
			EvalCommand.class,
			SolveCommand.class,
			TableCommand.class,
			TypeCommand.class,
			PrettyPrintCommand.class,
			LetCommand.class,
			UnletCommand.class,
			AssertCommand.class
		)));
		commandCategories.put("Animation", Collections.unmodifiableList(Arrays.asList(
			LoadCellCommand.class,
			LoadFileCommand.class,
			ConstantsCommand.class,
			InitialiseCommand.class,
			ExecCommand.class,
			BrowseCommand.class,
			TraceCommand.class,
			GotoCommand.class,
			FindCommand.class
		)));
		commandCategories.put("Visualisation", Collections.unmodifiableList(Arrays.asList(
			ShowCommand.class,
			DotCommand.class
		)));
		commandCategories.put("Verification", Collections.unmodifiableList(Arrays.asList(
			CheckCommand.class,
			ModelCheckCommand.class
		)));
		COMMAND_CLASS_CATEGORIES = Collections.unmodifiableMap(commandCategories);
	}
	
	private final @NotNull Injector injector;
	
	@Inject
	private HelpCommand(final @NotNull Injector injector) {
		super();
		
		this.injector = injector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":help";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return new Parameters(Collections.singletonList(COMMAND_NAME_PARAM));
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":help [COMMAND]";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Display help for a specific command, or general help about the REPL.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final ProBKernel kernel = this.injector.getInstance(ProBKernel.class);
		if (!args.get(COMMAND_NAME_PARAM).isPresent()) {
			final StringBuilder sb = new StringBuilder("Enter a B expression or predicate to evaluate it. To load a B machine, enter its source code directly, or use :load to load an external machine file.\nYou can also use any of the following commands. For more help on a particular command, run :help commandname.\n");
			final StringBuilder sbMarkdown = new StringBuilder("Enter a B expression or predicate to evaluate it. To load a B machine, enter its source code directly, or use `:load` to load an external machine file.\n\nYou can also use any of the following commands. For more help on a particular command, run `:help commandname`.\n");
			
			final Map<Class<? extends Command>, Command> commandsByClass = kernel.getCommands()
				.values()
				.stream()
				.collect(Collectors.toMap(
					command -> command.getClass().asSubclass(Command.class),
					command -> command
				));
			final Map<String, List<Command>> commandCategories = new LinkedHashMap<>();
			COMMAND_CLASS_CATEGORIES.forEach((categoryName, commandClasses) ->
				commandCategories.put(
					categoryName,
					commandClasses.stream()
						.map(commandsByClass::get)
						.collect(Collectors.toList())
				)
			);
			final List<Command> uncategorizedCommands = new ArrayList<>(kernel.getCommands().values());
			commandCategories.values().forEach(uncategorizedCommands::removeAll);
			uncategorizedCommands.sort(Comparator.comparing(Command::getName));
			commandCategories.put("Other", uncategorizedCommands);
			
			commandCategories.forEach((categoryName, commands) -> {
				sb.append("\n");
				sb.append(categoryName);
				sb.append(":\n");
				sbMarkdown.append("\n## ");
				sbMarkdown.append(categoryName);
				sbMarkdown.append("\n\n");
				
				commands.forEach(command -> {
					sb.append(command.getName());
					sb.append(" - ");
					sb.append(command.getShortHelp());
					sb.append('\n');
					sbMarkdown.append("* `");
					sbMarkdown.append(command.getName());
					sbMarkdown.append("` - ");
					sbMarkdown.append(command.getShortHelp());
					sbMarkdown.append('\n');
				});
			});
			
			final DisplayData result = new DisplayData(sb.toString());
			result.putMarkdown(sbMarkdown.toString());
			return result;
		} else {
			String commandName = args.get(COMMAND_NAME_PARAM).get();
			// If the user entered a command name without colons, add one or two colons as appropriate.
			// (If the command cannot be found, no colons are added, because we'll error out later anyway.)
			if (!commandName.startsWith(":")) {
				if (kernel.getCommands().containsKey(':' + commandName)) {
					commandName = ':' + commandName;
				} else if (kernel.getCommands().containsKey("::" + commandName)) {
					commandName = "::" + commandName;
				}
			}
			
			final Command command = kernel.getCommands().get(commandName);
			if (command == null) {
				throw new UserErrorException(String.format("Cannot display help for unknown command \"%s\"", commandName));
			}
			return command.renderHelp();
		}
	}
	
	@Override
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		return null;
	}
	
	@Override
	public @NotNull ReplacementOptions complete(final @NotNull String argString, final int at) {
		final String prefix = argString.substring(0, at);
		return new ReplacementOptions(
			this.injector.getInstance(ProBKernel.class)
				.getCommands()
				.keySet()
				.stream()
				.filter(s -> s.startsWith(prefix))
				.sorted()
				.collect(Collectors.toList()),
			0,
			argString.length()
		);
	}
}
