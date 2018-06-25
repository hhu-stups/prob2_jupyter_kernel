package de.prob2.jupyter.commands;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TimeCommand implements Command {
	private static final long NANOSECONDS_PER_SECOND = 1000000000L;
	
	private final @NotNull Injector injector;
	
	@Inject
	private TimeCommand(final @NotNull Injector injector) {
		super();
		
		this.injector = injector;
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
	public @Nullable DisplayData run(final @NotNull String argString) {
		final ProBKernel kernel = this.injector.getInstance(ProBKernel.class);
		final long startTime = System.nanoTime();
		final DisplayData result = kernel.eval(argString);
		final long stopTime = System.nanoTime();
		final long diff = stopTime - startTime;
		final String text = String.format("Execution time: %d.%09d seconds", diff / NANOSECONDS_PER_SECOND, diff % NANOSECONDS_PER_SECOND);
		final DisplayData timeDisplay = new DisplayData(text);
		timeDisplay.putMarkdown(text);
		kernel.display(timeDisplay);
		return result;
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		return this.injector.getInstance(ProBKernel.class).complete(argString, at);
	}
}
