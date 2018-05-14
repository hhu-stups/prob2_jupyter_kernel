package de.prob2.jupyter.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public final class CommandUtils {
	public static @NotNull List<@NotNull String> splitArgs(final @NotNull String args) {
		final String[] split = args.split("\\s+");
		if (split.length == 1 && split[0].isEmpty()) {
			return Collections.emptyList();
		} else {
			return Arrays.asList(split);
		}
	}
}
