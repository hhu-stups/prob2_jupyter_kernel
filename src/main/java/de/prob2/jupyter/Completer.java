package de.prob2.jupyter;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Completer {
	public abstract @Nullable ReplacementOptions complete(final @NotNull String argString, final int at);
}
