package de.prob2.jupyter.commands;

import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class HelpCommand implements Command {
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
			final StringBuilder sbMarkdown = new StringBuilder("Type a valid B expression, or one of the following commands:\n\n");
			new TreeMap<>(kernel.getCommands()).forEach((commandName, command) -> {
				sb.append(commandName);
				sb.append(' ');
				sb.append(command.getShortHelp());
				sb.append('\n');
				sbMarkdown.append("* `");
				sbMarkdown.append(commandName);
				sbMarkdown.append("` ");
				sbMarkdown.append(command.getShortHelp());
				sbMarkdown.append('\n');
			});
			final DisplayData result = new DisplayData(sb.toString());
			result.putMarkdown(sbMarkdown.toString());
			return result;
		} else if (args.size() == 1) {
			String commandName = args.get(0);
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
			final DisplayData result = new DisplayData(command.getSyntax() + "\n\n" + command.getShortHelp());
			result.putMarkdown("```\n" + command.getSyntax() + "\n```\n\n" + command.getShortHelp());
			return result;
		} else {
			throw new UserErrorException("Expected at most 1 argument, got " + args.size());
		}
	}
	
	@Override
	public @NotNull ReplacementOptions complete(final @NotNull ProBKernel kernel, final @NotNull String argString, final int at) {
		final String prefix = argString.substring(0, at);
		return new ReplacementOptions(
			kernel.getCommands().keySet().stream().filter(s -> s.startsWith(prefix)).sorted().collect(Collectors.toList()),
			0,
			argString.length()
		);
	}
}
