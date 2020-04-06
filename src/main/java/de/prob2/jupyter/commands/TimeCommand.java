package de.prob2.jupyter.commands;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.jupyter.Command;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.PositionalParameter;
import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TimeCommand implements Command {
	private static final @NotNull PositionalParameter.RequiredRemainder COMMAND_AND_ARGS_PARAM = new PositionalParameter.RequiredRemainder("commandAndArgs");
	
	private final @NotNull Injector injector;
	
	@Inject
	private TimeCommand(final @NotNull Injector injector) {
		super();
		
		this.injector = injector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":time";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return new Parameters(Collections.singletonList(COMMAND_AND_ARGS_PARAM));
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
	public @NotNull String getHelpBody() {
		return "The time is internally measured using Java's [`System.nanoTime()`](https://docs.oracle.com/javase/8/docs/api/java/lang/System.html#nanoTime--) method. The measured time is displayed with nanosecond precision, but the actual resolution of the measurement is system-dependent and often much less accurate than nanoseconds.\n\n"
			+ "As with any measurement of execution time, there will likely be small differences between two measurements of the same command. The time is measured by the kernel rather than ProB, so it will include some overhead due to processing of the command by the kernel and communication with ProB.";
	}
	
	@Override
	public @Nullable DisplayData run(final @NotNull ParsedArguments args) {
		final ProBKernel kernel = this.injector.getInstance(ProBKernel.class);
		final Stopwatch stopwatch = Stopwatch.createStarted();
		final DisplayData result = kernel.eval(args.get(COMMAND_AND_ARGS_PARAM));
		stopwatch.stop();
		final Duration elapsed = stopwatch.elapsed();
		final String text = String.format("Execution time: %d.%09d seconds", elapsed.get(ChronoUnit.SECONDS), elapsed.get(ChronoUnit.NANOS));
		final DisplayData timeDisplay = new DisplayData(text);
		timeDisplay.putMarkdown(text);
		kernel.display(timeDisplay);
		return result;
	}
	
	@Override
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		return this.injector.getInstance(ProBKernel.class).inspect(argString, at, false);
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		return this.injector.getInstance(ProBKernel.class).complete(argString, at);
	}
}
