package de.prob2.jupyter.commands;

import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class UnletCommand implements Command {
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
	public @Nullable DisplayData run(final @NotNull String argString) {
		final Map<String, String> variables = this.injector.getInstance(ProBKernel.class).getVariables();
		if (!variables.containsKey(argString)) {
			throw new UserErrorException("There is no local variable " + argString);
		}
		variables.remove(argString);
		return null;
	}
	
	@Override
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		// TODO
		return null;
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		// TODO
		return null;
	}
}
