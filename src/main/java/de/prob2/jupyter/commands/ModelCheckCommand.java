package de.prob2.jupyter.commands;

import com.google.inject.Inject;

import de.prob.check.CheckError;
import de.prob.check.ConsistencyChecker;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckErrorUncovered;
import de.prob.check.NotYetFinished;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.StateSpace;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ModelCheckCommand implements Command {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private ModelCheckCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
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
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		if (!argString.isEmpty()) {
			throw new UserErrorException("Unexpected argument: " + argString);
		}
		
		final StateSpace stateSpace = this.animationSelector.getCurrentTrace().getStateSpace();
		final ConsistencyChecker job = new ConsistencyChecker(stateSpace);
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
