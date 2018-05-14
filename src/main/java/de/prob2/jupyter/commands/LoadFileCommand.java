package de.prob2.jupyter.commands;

import java.io.IOException;
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
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String name, final @NotNull String argString) {
		final List<String> args = CommandUtils.splitArgs(argString);
		if (args.isEmpty()) {
			throw new CommandExecutionException(name, "Missing machine file name");
		}
		
		final String fileName = args.get(0);
		final Map<String, String> preferences = CommandUtils.parsePreferences(name, args.subList(1, args.size()));
		
		try {
			kernel.setTrace(new Trace(this.classicalBFactory.extract(fileName).load(preferences)));
		} catch (IOException | ModelTranslationError e) {
			throw new RuntimeException(e);
		}
		return new DisplayData("Loaded machine: " + kernel.getTrace().getModel());
	}
}
