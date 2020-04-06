package de.prob2.jupyter.commands;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.scripting.ClassicalBFactory;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.CommandUtils;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.PositionalParameter;
import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LoadCellCommand implements Command {
	private static final @NotNull PositionalParameter.RequiredRemainder PREFS_AND_CODE_PARAM = new PositionalParameter.RequiredRemainder("prefsAndCode");
	
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
	public @NotNull String getName() {
		return "::load";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return new Parameters(Collections.singletonList(PREFS_AND_CODE_PARAM));
	}
	
	@Override
	public @NotNull String getSyntax() {
		return "MACHINE\n...\nEND\n\n// or\n\n::load [PREF=VALUE ...]\nMACHINE\n...\nEND";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Load a B machine from the given source code.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "Normally you do not need to explicitly call `::load` to load a machine from a cell. If you input the source code for a B machine without any command before it, it is loaded automatically.\n\n"
			+ "There must be a newline between the `::load` command name and the machine code.\n\n"
			+ "If you use an explicit `::load` command, there must be a newline between `::load` and the machine source code. On the same line as `::load`, you can set the values of one or more ProB preferences that should be applied to the newly loaded machine. Preferences can also be changed using the `:pref` command after a machine has been loaded, however certain preferences do not take full effect when set using `:pref` and must be set when the machine is loaded.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final String[] split = args.get(PREFS_AND_CODE_PARAM).split("\n", 2);
		if (split.length != 2) {
			throw new UserErrorException("Missing command body");
		}
		final String prefsString = split[0];
		final String body = split[1];
		final List<String> prefsSplit = CommandUtils.splitArgs(prefsString);
		final Map<String, String> preferences = CommandUtils.parsePreferences(prefsSplit);
		
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
