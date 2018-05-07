package de.prob2.jupyter;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.scripting.ClassicalBFactory;
import de.prob.statespace.Trace;

import org.jetbrains.annotations.NotNull;

import io.github.spencerpark.jupyter.kernel.BaseKernel;
import io.github.spencerpark.jupyter.kernel.LanguageInfo;
import io.github.spencerpark.jupyter.messages.DisplayData;

public class ProBKernel extends BaseKernel {
	private @NotNull Trace trace;
	
	@Inject
	private ProBKernel(final @NotNull ClassicalBFactory classicalBFactory) {
		super();
		
		this.trace = new Trace(classicalBFactory.create("MACHINE repl END").load());
	}
	
	@Override
	public DisplayData eval(final String expr) {
		assert expr != null;
		
		final AbstractEvalResult result = this.trace.evalCurrent(expr, FormulaExpand.EXPAND);
		return new DisplayData(result.toString());
	}
	
	@Override
	public LanguageInfo getLanguageInfo() {
		return new LanguageInfo.Builder("prob")
			.mimetype("text/x-prob")
			.fileExtension(".prob")
			.build();
	}
	
	@Override
	public void onShutdown(final boolean isRestarting) {
		this.trace.getStateSpace().kill();
	}
}
