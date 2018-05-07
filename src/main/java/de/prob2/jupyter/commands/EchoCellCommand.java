package de.prob2.jupyter.commands;

import java.util.List;

import com.google.inject.Inject;

import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.messages.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class EchoCellCommand implements CellCommand {
	@Inject
	private EchoCellCommand() {
		super();
	}
	
	@Override
	public @NotNull String getSyntax() {
		return "::echo [ARGS [...]]\nBODY";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Echoes the arguments and body back.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String name, final @NotNull List<@NotNull String> args, final @NotNull String body) {
		return new DisplayData(String.format("Name: %s\nArguments: %s\nBody:\n%s", name, args, body));
	}
}
