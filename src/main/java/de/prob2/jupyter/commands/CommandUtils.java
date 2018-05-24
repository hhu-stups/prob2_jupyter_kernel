package de.prob2.jupyter.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.prob2.jupyter.UserErrorException;

import org.jetbrains.annotations.NotNull;

public final class CommandUtils {
	public static @NotNull List<@NotNull String> splitArgs(final @NotNull String args) {
		final String[] split = args.split("\\h+");
		if (split.length == 1 && split[0].isEmpty()) {
			return Collections.emptyList();
		} else {
			return Arrays.asList(split);
		}
	}
	
	public static @NotNull Map<@NotNull String, @NotNull String> parsePreferences(final @NotNull String name, final @NotNull List<@NotNull String> args) {
		final Map<String, String> preferences = new HashMap<>();
		for (final String arg : args) {
			final String[] split = arg.split("=", 2);
			if (split.length == 1) {
				throw new UserErrorException("Missing value for preference " + split[0]);
			}
			preferences.put(split[0], split[1]);
		}
		return preferences;
	}
}
