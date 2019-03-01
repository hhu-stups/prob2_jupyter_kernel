package de.prob2.jupyter.commands;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.scripting.ClassicalBFactory;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LoadCellCommand implements Command {
	private final @NotNull ClassicalBFactory classicalBFactory;
	private final @NotNull AnimationSelector animationSelector;
	private final @NotNull Provider<ProBKernel> proBKernelProvider;
	
	@Inject
	private LoadCellCommand(
		final @NotNull ClassicalBFactory classicalBFactory,
		final @NotNull AnimationSelector animationSelector,
		final @NotNull Provider<ProBKernel> proBKernelProvider
	) {
		super();
		
		this.classicalBFactory = classicalBFactory;
		this.animationSelector = animationSelector;
		this.proBKernelProvider = proBKernelProvider;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return "::load [PREF=VALUE ...]\nMACHINE\n...\nEND";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Load the machine source code given in the cell body.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "There must be a newline between the `::load` command name and the machine code.\n\n"
			+ "Any number of preference assignments may be included after `::load` (only on the first line). Preferences can also be changed on a loaded machine using the `:pref` command, however certain preferences do not take full effect when set using `:pref` and must be set when the machine is loaded.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		final String[] split = argString.split("\n", 2);
		if (split.length != 2) {
			throw new UserErrorException("Missing command body");
		}
		final String prefsString = split[0];
		final String body = split[1];
		final List<String> args = CommandUtils.splitArgs(prefsString);
		final Map<String, String> preferences = CommandUtils.parsePreferences(args);
		
		this.animationSelector.changeCurrentAnimation(new Trace(CommandUtils.withSourceCode(body, () ->
			this.classicalBFactory.create("(machine from Jupyter cell)", body).load(preferences)
		)));
		this.proBKernelProvider.get().setCurrentMachineDirectory(Paths.get(""));
		return new DisplayData("Loaded machine: " + this.animationSelector.getCurrentTrace().getStateSpace().getMainComponent());
	}
	
	@Override
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		final int newlinePos = argString.indexOf('\n');
		if (newlinePos == -1 || at < newlinePos) {
			// Cursor is on the first line, provide preference inspections.
			return CommandUtils.inspectInPreferences(this.animationSelector.getCurrentTrace(), argString, at);
		} else {
			// Cursor is in the body, provide B inspections.
			return CommandUtils.inspectInBExpression(this.animationSelector.getCurrentTrace(), argString, at);
		}
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		final int newlinePos = argString.indexOf('\n');
		if (newlinePos == -1 || at < newlinePos) {
			// Cursor is on the first line, provide preference name completions.
			return CommandUtils.completeInPreferences(this.animationSelector.getCurrentTrace(), argString, at);
		} else {
			// Cursor is in the body, provide B completions.
			return CommandUtils.completeInBExpression(this.animationSelector.getCurrentTrace(), argString, at);
		}
	}
}
