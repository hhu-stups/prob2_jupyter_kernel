package de.prob2.jupyter.commands;

import java.util.Objects;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.google.inject.Inject;

import de.prob.scripting.ScriptEngineProvider;

import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.messages.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class GroovyCommand implements LineCommand {
	private final @NotNull ScriptEngine groovyScriptEngine;
	
	@Inject
	private GroovyCommand(final @NotNull ScriptEngineProvider scriptEngineProvider) {
		super();
		
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
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String argString) {
		this.groovyScriptEngine.put("__console", System.out);
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
}
