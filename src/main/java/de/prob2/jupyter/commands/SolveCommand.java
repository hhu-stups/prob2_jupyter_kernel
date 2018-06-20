package de.prob2.jupyter.commands;

import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;

import de.prob.animator.command.CbcSolveCommand;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class SolveCommand implements Command {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private SolveCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":solve SOLVER PREDICATE";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Solve a predicate with the specified solver";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String argString) {
		final List<String> split = CommandUtils.splitArgs(argString, 2);
		if (split.size() != 2) {
			throw new UserErrorException("Expected 2 arguments, got 1");
		}
		
		final Trace trace = this.animationSelector.getCurrentTrace();
		final CbcSolveCommand.Solvers solver;
		switch (split.get(0)) {
			case "prob":
				solver = CbcSolveCommand.Solvers.PROB;
				break;
			
			case "kodkod":
				solver = CbcSolveCommand.Solvers.KODKOD;
				break;
			
			case "smt_supported_interpreter":
				solver = CbcSolveCommand.Solvers.SMT_SUPPORTED_INTERPRETER;
				break;
			
			case "z3":
				solver = CbcSolveCommand.Solvers.Z3;
				break;
			
			case "cvc4":
				solver = CbcSolveCommand.Solvers.CVC4;
				break;
			
			default:
				throw new UserErrorException("Unknown solver: " + split.get(0));
		}
		final IEvalElement predicate = trace.getModel().parseFormula(split.get(1), FormulaExpand.EXPAND);
		
		final CbcSolveCommand cmd = new CbcSolveCommand(predicate, solver, this.animationSelector.getCurrentTrace().getCurrentState());
		trace.getStateSpace().execute(cmd);
		return CommandUtils.displayDataForEvalResult(cmd.getValue());
	}
	
	@Override
	public @NotNull ReplacementOptions complete(final @NotNull ProBKernel kernel, final @NotNull String argString, final int at) {
		final int solverNameEnd;
		final Matcher argSplitMatcher = CommandUtils.ARG_SPLIT_PATTERN.matcher(argString);
		if (argSplitMatcher.find()) {
			solverNameEnd = argSplitMatcher.start();
		} else {
			solverNameEnd = argString.length();
		}
		
		if (solverNameEnd < at) {
			// Cursor is in the predicate part of the arguments, provide B completions.
			final ReplacementOptions replacements = CommandUtils.completeInBExpression(this.animationSelector.getCurrentTrace(), argString.substring(solverNameEnd), at - solverNameEnd);
			return CommandUtils.offsetReplacementOptions(replacements, solverNameEnd);
		} else {
			// Cursor is in the solver name.
			final String prefix = argString.substring(0, at);
			final List<String> solverNames = Stream.of("prob", "kodkod", "smt_supported_interpreter", "z3", "cvc4")
				.filter(s -> s.startsWith(prefix))
				.sorted()
				.collect(Collectors.toList());
			return new ReplacementOptions(solverNames, 0, solverNameEnd);
		}
	}
}
