package de.prob2.jupyter.commands;

import com.google.inject.Inject;

import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GotoCommand implements Command {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private GotoCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":goto INDEX";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Go to the state with the specified index in the current trace.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "Use the `:trace` command to view the current trace and the indices of its states. Index -1 refers to the root state and is always available.\n\nGoing backwards in the current trace does *not* discard any parts of the trace, so it is possible to go forward again afterwards. However, executing an operation in a state *will* discard any parts of the trace after that state (and replace them with the destination state of the executed transition).";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		final int index = Integer.parseInt(argString);
		final Trace trace = this.animationSelector.getCurrentTrace();
		if (index < -1 || index >= trace.size()) {
			throw new UserErrorException(String.format("Invalid trace index %d, must be in -1..%d", index, trace.size()-1));
		}
		this.animationSelector.changeCurrentAnimation(trace.gotoPosition(index));
		return new DisplayData("Changed to state with index " + index);
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		return null;
	}
}
