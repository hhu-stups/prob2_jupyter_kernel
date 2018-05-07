package de.prob2.jupyter.commands;

import java.util.List;

import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.messages.DisplayData;

import org.jetbrains.annotations.NotNull;

public interface CellCommand extends BaseCommand {
	public abstract @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String name, final @NotNull List<@NotNull String> args, final @NotNull String body);
}
