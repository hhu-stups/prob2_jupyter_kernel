package de.prob2.jupyter.commands;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LetCommand implements Command {
	private final @NotNull Injector injector;
	
	@Inject
	public LetCommand(final @NotNull Injector injector) {
		super();
		
		this.injector = injector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":let NAME VALUE";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Set the value of a local variable.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "Variables are available in all states and are not affected by machine loads. A variable created by `:let` shadows any identifier from the machine with the same name.\n\n**Note:** Local variables are currently stored in text form. Values must have a syntactically valid text representation, and large values may cause performance issues.";
	}
	
	@Override
	public @Nullable DisplayData run(final @NotNull String argString) {
		final List<String> split = CommandUtils.splitArgs(argString, 2);
		if (split.size() != 2) {
			throw new UserErrorException("Expected 2 arguments, not " + split.size());
		}
		this.injector.getInstance(ProBKernel.class).getVariables().put(split.get(0), split.get(1));
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
