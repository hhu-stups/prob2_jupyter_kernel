package de.prob2.jupyter.commands;

import java.util.Collections;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.jupyter.Command;
import de.prob2.jupyter.Parameter;
import de.prob2.jupyter.ParameterCompleters;
import de.prob2.jupyter.ParameterInspectors;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class UnletCommand implements Command {
	private static final @NotNull Parameter.RequiredSingle NAME_PARAM = Parameter.required("name");
	
	private final @NotNull Injector injector;
	
	@Inject
	public UnletCommand(final @NotNull Injector injector) {
		super();
		
		this.injector = injector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":unlet";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return new Parameters(Collections.singletonList(NAME_PARAM));
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":unlet NAME";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Remove a local variable.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "";
	}
	
	@Override
	public @Nullable DisplayData run(final @NotNull ParsedArguments args) {
		final Map<String, String> variables = this.injector.getInstance(ProBKernel.class).getVariables();
		final String name = args.get(NAME_PARAM);
		if (!variables.containsKey(name)) {
			throw new UserErrorException("There is no local variable " + name);
		}
		variables.remove(name);
		return null;
	}
	
	@Override
	public @NotNull ParameterInspectors getParameterInspectors() {
		// TODO
		return ParameterInspectors.NONE;
	}
	
	@Override
	public @NotNull ParameterCompleters getParameterCompleters() {
		// TODO
		return ParameterCompleters.NONE;
	}
}
