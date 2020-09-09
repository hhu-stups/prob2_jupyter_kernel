package de.prob2.jupyter.commands;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.scripting.ClassicalBFactory;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.CommandUtils;
import de.prob2.jupyter.Parameter;
import de.prob2.jupyter.ParameterCompleters;
import de.prob2.jupyter.ParameterInspectors;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class LoadCellCommand implements Command {
	private static final @NotNull Parameter.Multiple PREFS_PARAM = Parameter.optionalMultiple("prefs");
	private static final @NotNull Parameter.RequiredSingle CODE_PARAM = Parameter.body("code");
	
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
		return new Parameters(Collections.singletonList(PREFS_PARAM), CODE_PARAM);
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
		final String body = args.get(CODE_PARAM);
		final Map<String, String> preferences = CommandUtils.parsePreferences(args.get(PREFS_PARAM));
		
		this.proBKernelProvider.get().switchMachine(Paths.get(""), stateSpace -> {
			stateSpace.changePreferences(preferences);
			CommandUtils.withSourceCode(body, () ->
				this.classicalBFactory.create(ProBKernel.LOAD_CELL_MACHINE_NAME, body).loadIntoStateSpace(stateSpace)
			);
			return new Trace(stateSpace);
		});
		return new DisplayData("Loaded machine: " + this.animationSelector.getCurrentTrace().getStateSpace().getMainComponent());
	}
	
	@Override
	public @NotNull ParameterInspectors getParameterInspectors() {
		return new ParameterInspectors(ImmutableMap.of(
			PREFS_PARAM, CommandUtils.preferenceInspector(this.animationSelector.getCurrentTrace()),
			CODE_PARAM, CommandUtils.bExpressionInspector(this.animationSelector.getCurrentTrace())
		));
	}
	
	@Override
	public @NotNull ParameterCompleters getParameterCompleters() {
		return new ParameterCompleters(ImmutableMap.of(
			PREFS_PARAM, CommandUtils.preferenceCompleter(this.animationSelector.getCurrentTrace()),
			CODE_PARAM, CommandUtils.bExpressionCompleter(this.animationSelector.getCurrentTrace())
		));
	}
}
