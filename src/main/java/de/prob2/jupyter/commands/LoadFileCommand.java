package de.prob2.jupyter.commands;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.Trace;

import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.messages.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class LoadFileCommand implements LineCommand {
	private final ClassicalBFactory classicalBFactory;
	
	@Inject
	private LoadFileCommand(final ClassicalBFactory classicalBFactory) {
		super();
		
		this.classicalBFactory = classicalBFactory;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":load FILENAME [PREF=VALUE ...]";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Load the machine from the given path.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String name, final @NotNull List<@NotNull String> args) {
		if (args.isEmpty()) {
			throw new CommandExecutionException(name, "Missing machine file name");
		}
		
		final String fileName = args.get(0);
		final List<String> prefArgs = args.subList(1, args.size());
		final Map<String, String> preferences = new HashMap<>();
		for (final String arg : prefArgs) {
			final String[] split = arg.split("=", 2);
			if (split.length == 1) {
				throw new CommandExecutionException(name, "Missing value for preference " + split[0]);
			}
			preferences.put(split[0], split[1]);
		}
		
		try {
			kernel.setTrace(new Trace(this.classicalBFactory.extract(fileName).load(preferences)));
		} catch (IOException | ModelTranslationError e) {
			throw new RuntimeException(e);
		}
		return new DisplayData("Loaded machine: " + kernel.getTrace().getModel());
	}
}
