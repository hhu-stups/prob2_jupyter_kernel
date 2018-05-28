package de.prob2.jupyter.commands;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import de.prob.scripting.ClassicalBFactory;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.messages.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class LoadCellCommand implements Command {
	private final @NotNull ClassicalBFactory classicalBFactory;
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private LoadCellCommand(final @NotNull ClassicalBFactory classicalBFactory, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.classicalBFactory = classicalBFactory;
		this.animationSelector = animationSelector;
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
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String argString) {
		final String[] split = argString.split("\n", 2);
		if (split.length != 2) {
			throw new UserErrorException("Missing command body");
		}
		final String prefsString = split[0];
		final String body = split[1];
		final List<String> args = CommandUtils.splitArgs(prefsString);
		final Map<String, String> preferences = CommandUtils.parsePreferences(args);
		
		this.animationSelector.changeCurrentAnimation(new Trace(this.classicalBFactory.create(body).load(preferences)));
		return new DisplayData("Loaded machine: " + this.animationSelector.getCurrentTrace().getModel());
	}
}
