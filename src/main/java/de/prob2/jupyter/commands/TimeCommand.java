package de.prob2.jupyter.commands;

import com.google.inject.Inject;

import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class TimeCommand implements Command {
	private static final long NANOSECONDS_PER_SECOND = 1000000000L;
	
	@Inject
	private TimeCommand() {
		super();
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":time COMMAND [ARGS ...]";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Execute the given command and measure how long it takes to execute.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String argString) {
		final long startTime = System.nanoTime();
		final DisplayData result = kernel.eval(argString);
		final long stopTime = System.nanoTime();
		final long diff = stopTime - startTime;
		System.out.printf("Execution time: %d.%09d seconds%n", diff / NANOSECONDS_PER_SECOND, diff % NANOSECONDS_PER_SECOND);
		return result;
	}
}
