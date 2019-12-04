package de.prob2.jupyter.commands;

import java.util.concurrent.atomic.AtomicLong;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.check.CheckError;
import de.prob.check.ConsistencyChecker;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckErrorUncovered;
import de.prob.check.ModelCheckingOptions;
import de.prob.check.NotYetFinished;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.StateSpace;
import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.JupyterIO;
import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ModelCheckCommand implements Command {
	private static final AtomicLong idCounter = new AtomicLong();
	
	private final @NotNull Provider<@NotNull ProBKernel> kernelProvider;
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private ModelCheckCommand(final @NotNull Provider<@NotNull ProBKernel> kernelProvider, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.kernelProvider = kernelProvider;
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":modelcheck";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Run the ProB model checker on the current model.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "If an error state is found, it is made the current state, so that it can be inspected using `:trace`, `:check`, etc.";
	}
	
	private static @NotNull String formatMillisecondsAsSeconds(final long milliseconds) {
		return String.format("%d.%03d", milliseconds / 1000, milliseconds % 1000);
	}
	
	private static @NotNull String formatModelCheckStats(final long timeElapsed, final @Nullable StateSpaceStats stats) {
		final StringBuilder sb = new StringBuilder(formatMillisecondsAsSeconds(timeElapsed));
		sb.append(" sec");
		if (stats != null) {
			sb.append(", ");
			sb.append(stats.getNrProcessedNodes());
			sb.append(" of ");
			sb.append(stats.getNrTotalNodes());
			sb.append(" states processed, ");
			sb.append(stats.getNrTotalTransitions());
			sb.append(" transitions");
		}
		return sb.toString();
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		if (!argString.isEmpty()) {
			throw new UserErrorException("Unexpected argument: " + argString);
		}
		
		final StateSpace stateSpace = this.animationSelector.getCurrentTrace().getStateSpace();
		
		final JupyterIO io = this.kernelProvider.get().getIO();
		
		// Create the DisplayData that will be used to display status updates.
		final String statusDataId = ModelCheckCommand.class.getName() + " status " + idCounter.getAndIncrement();
		final DisplayData data = new DisplayData("");
		data.setDisplayId(statusDataId);
		io.display.display(data);
		
		final ConsistencyChecker job = new ConsistencyChecker(stateSpace, ModelCheckingOptions.DEFAULT, null, new IModelCheckListener() {
			@Override
			public void updateStats(final @NotNull String jobId, final long timeElapsed, final @NotNull IModelCheckingResult result, final @Nullable StateSpaceStats stats) {
				io.display.updateDisplay(statusDataId, new DisplayData(formatModelCheckStats(timeElapsed, stats) + "\n" + result.getMessage()));
			}
			
			@Override
			public void isFinished(final @NotNull String jobId, final long timeElapsed, final @NotNull IModelCheckingResult result, final @Nullable StateSpaceStats stats) {
				// Same as updateStats, except that the result is not included, because the final result will be displayed as the main command result instead of in the status.
				io.display.updateDisplay(statusDataId, new DisplayData(formatModelCheckStats(timeElapsed, stats)));
			}
		});
		
		final IModelCheckingResult result = job.call();
		
		final StringBuilder sbPlain = new StringBuilder();
		if (result instanceof NotYetFinished || result instanceof CheckError) {
			throw new UserErrorException("Model check could not finish properly: " + result);
		} else if (result instanceof ModelCheckErrorUncovered) {
			sbPlain.append("Model check uncovered an error: ");
			sbPlain.append(result.getMessage());
			sbPlain.append("\nUse :trace to view the trace to the error state.");
			this.animationSelector.changeCurrentAnimation(((ModelCheckErrorUncovered)result).getTrace(stateSpace));
		} else {
			sbPlain.append(result.getMessage());
		}
		return new DisplayData(sbPlain.toString());
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
