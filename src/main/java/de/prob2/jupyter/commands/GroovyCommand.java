package de.prob2.jupyter.commands;

import java.util.Objects;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.scripting.ScriptEngineProvider;

import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GroovyCommand implements Command {
	private final @NotNull Injector injector;
	private final @NotNull ScriptEngine groovyScriptEngine;
	
	@Inject
	private GroovyCommand(final @NotNull Injector injector, final @NotNull ScriptEngineProvider scriptEngineProvider) {
		super();
		
		this.injector = injector;
		this.groovyScriptEngine = scriptEngineProvider.get();
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":groovy EXPRESSION";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Evaluate the given Groovy expression.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "The standard ProB 2 Groovy environment is used to evaluate the expression, so ProB 2's global `api` and `animations` objects may be used to load machines and manipulate traces.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		this.groovyScriptEngine.put("__console", this.injector.getInstance(ProBKernel.class).getIO().out);
		final Object result;
		try {
			result = this.groovyScriptEngine.eval(argString);
		} catch (ScriptException e) {
			throw new RuntimeException(e);
		} finally {
			this.groovyScriptEngine.put("__console", null);
		}
		return new DisplayData(Objects.toString(result));
	}
	
	@Override
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		return null;
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		return null;
	}
}
