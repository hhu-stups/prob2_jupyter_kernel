package de.prob2.jupyter.commands;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.inject.Inject;

import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.messages.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class HelpCommand implements LineCommand {
	@Inject
	private HelpCommand() {
		super();
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":? [COMMAND]\n:help [COMMAND]";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Display help for a specific command, or general help about the REPL.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String argString) {
		final List<String> args = CommandUtils.splitArgs(argString);
		if (args.isEmpty()) {
			final StringBuilder sb = new StringBuilder("Type a valid B expression, or one of the following commands:\n");
			final SortedMap<String, BaseCommand> commands = new TreeMap<>();
			commands.putAll(kernel.getCellCommands());
			commands.putAll(kernel.getLineCommands());
			commands.forEach((commandName, command) -> {
				sb.append(commandName);
				sb.append(' ');
				sb.append(command.getShortHelp());
				sb.append('\n');
			});
			return new DisplayData(sb.toString());
		} else if (args.size() == 1) {
			String commandName = args.get(0);
			// If the user entered a command name without colons, add one or two colons as appropriate.
			// (If the command cannot be found, no colons are added, because we'll error out later anyway.)
			if (!commandName.startsWith(":")) {
				if (kernel.getLineCommands().containsKey(':' + commandName)) {
					commandName = ':' + commandName;
				} else if (kernel.getCellCommands().containsKey("::" + commandName)) {
					commandName = "::" + commandName;
				}
			}
			
			final BaseCommand command;
			if (commandName.startsWith("::")) {
				command = kernel.getCellCommands().get(commandName);
			} else {
				command = kernel.getLineCommands().get(commandName);
			}
			
			if (command == null) {
				throw new UserErrorException(String.format("Cannot display help for unknown command \"%s\"", commandName));
			}
			return new DisplayData(command.getLongHelp());
		} else {
			throw new UserErrorException("Expected at most 1 argument, got " + args.size());
		}
	}
}
