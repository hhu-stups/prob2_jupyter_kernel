package de.prob2.jupyter.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import de.prob.scripting.ClassicalBFactory;
import de.prob.statespace.Trace;

import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.messages.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class LoadCellCommand implements CellCommand {
	private final @NotNull ClassicalBFactory classicalBFactory;
	
	@Inject
	private LoadCellCommand(final @NotNull ClassicalBFactory classicalBFactory) {
		super();
		
		this.classicalBFactory = classicalBFactory;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return "::load [PREF=VALUE ...]\nMACHINE\n...\nEND";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Load the machine source code from the body.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String name, final @NotNull List<@NotNull String> args, final @NotNull String body) {
		final Map<String, String> preferences = new HashMap<>();
		for (final String arg : args) {
			final String[] split = arg.split("=", 2);
			if (split.length == 1) {
				throw new CommandExecutionException(name, "Missing value for preference " + split[0]);
			}
			preferences.put(split[0], split[1]);
		}
		
		kernel.setTrace(new Trace(this.classicalBFactory.create(body).load(preferences)));
		return new DisplayData("Loaded machine: " + kernel.getTrace().getModel());
	}
}
