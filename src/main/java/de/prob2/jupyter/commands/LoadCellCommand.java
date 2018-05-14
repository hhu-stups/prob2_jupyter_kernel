package de.prob2.jupyter.commands;

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
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String name, final @NotNull String argString, final @NotNull String body) {
		final List<String> args = CommandUtils.splitArgs(argString);
		final Map<String, String> preferences = CommandUtils.parsePreferences(name, args);
		
		kernel.setTrace(new Trace(this.classicalBFactory.create(body).load(preferences)));
		return new DisplayData("Loaded machine: " + kernel.getTrace().getModel());
	}
}
