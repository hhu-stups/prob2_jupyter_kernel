package de.prob2.jupyter;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Inspector {
	public abstract @Nullable DisplayData inspect(final @NotNull String argString, final int at);
}
