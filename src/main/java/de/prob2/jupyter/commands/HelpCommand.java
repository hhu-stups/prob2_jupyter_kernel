package de.prob2.jupyter.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.messages.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class HelpCommand implements ReplCommand {
	@Override
	public @NotNull String getSyntax() {
		return ":? [COMMAND]\n:help [COMMAND]";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Display help for a specific command, or general help about the REPL.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String name, final @NotNull List<@NotNull String> args) {
		if (args.isEmpty()) {
			final StringBuilder sb = new StringBuilder("Type a valid B expression, or one of the following commands:\n");
			final List<String> names = new ArrayList<>(kernel.getCommands().keySet());
			Collections.sort(names);
			for (final String commandName : names) {
				final ReplCommand command = kernel.getCommands().get(commandName);
				assert command != null;
				sb.append(commandName);
				sb.append(' ');
				sb.append(command.getShortHelp());
				sb.append('\n');
			}
			return new DisplayData(sb.toString());
		} else if (args.size() == 1) {
			String commandName = args.get(0);
			if (!commandName.startsWith(":")) {
				commandName = ':' + commandName;
			}
			final ReplCommand command = kernel.getCommands().get(commandName);
			if (command == null) {
				throw new CommandExecutionException(name, String.format("Cannot display help for unknown command \"%s\"", commandName));
			}
			return new DisplayData(command.getLongHelp());
		} else {
			throw new CommandExecutionException(name, "Expected at most 1 argument, got " + args.size());
		}
	}
}
