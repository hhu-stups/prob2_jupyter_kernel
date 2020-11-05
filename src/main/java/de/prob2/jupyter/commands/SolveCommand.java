package de.prob2.jupyter.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.animator.command.CbcSolveCommand;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.CommandUtils;
import de.prob2.jupyter.Parameter;
import de.prob2.jupyter.ParameterCompleters;
import de.prob2.jupyter.ParameterInspectors;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class SolveCommand implements Command {
	private static final @NotNull Parameter.RequiredSingle SOLVER_PARAM = Parameter.required("solver");
	private static final @NotNull Parameter.RequiredSingle PREDICATE_PARAM = Parameter.requiredRemainder("predicate");
	
	private static final @NotNull Map<@NotNull String, CbcSolveCommand.@NotNull Solvers> SOLVERS = Arrays.stream(CbcSolveCommand.Solvers.values())
		.collect(Collectors.toMap(s -> s.name().toLowerCase(), s -> s));
	
	private final @NotNull Provider<@NotNull ProBKernel> kernelProvider;
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private SolveCommand(final @NotNull Provider<@NotNull ProBKernel> kernelProvider, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.kernelProvider = kernelProvider;
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":solve";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return new Parameters(Arrays.asList(SOLVER_PARAM, PREDICATE_PARAM));
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":solve SOLVER PREDICATE";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Solve a predicate with the specified solver.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		final StringBuilder sb = new StringBuilder("The following solvers are currently available:\n\n");
		SOLVERS.keySet().stream().sorted().forEach(solver -> {
			sb.append("* `");
			sb.append(solver);
			sb.append("`\n");
		});
		return sb.toString();
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final ProBKernel kernel = this.kernelProvider.get();
		final Trace trace = this.animationSelector.getCurrentTrace();
		final CbcSolveCommand.Solvers solver = SOLVERS.get(args.get(SOLVER_PARAM));
		if (solver == null) {
			throw new UserErrorException("Unknown solver: " + args.get(SOLVER_PARAM));
		}
		final String code = kernel.insertLetVariables(args.get(PREDICATE_PARAM));
		final IEvalElement predicate = CommandUtils.withSourceCode(code, () -> kernel.parseFormula(code, FormulaExpand.EXPAND));
		
		final CbcSolveCommand cmd = new CbcSolveCommand(predicate, solver, this.animationSelector.getCurrentTrace().getCurrentState());
		trace.getStateSpace().execute(cmd);
		return CommandUtils.displayDataForEvalResult(cmd.getValue());
	}
	
	@Override
	public @NotNull ParameterInspectors getParameterInspectors() {
		return new ParameterInspectors(ImmutableMap.of(
			SOLVER_PARAM, (solverName, at) -> null, // TODO
			PREDICATE_PARAM, CommandUtils.bExpressionInspector(this.animationSelector.getCurrentTrace())
		));
	}
	
	@Override
	public @NotNull ParameterCompleters getParameterCompleters() {
		return new ParameterCompleters(ImmutableMap.of(
			SOLVER_PARAM, (solverName, at) -> {
				final String prefix = solverName.substring(0, at);
				final List<String> solverNames = SOLVERS.keySet().stream()
					.filter(s -> s.startsWith(prefix))
					.sorted()
					.collect(Collectors.toList());
				return new ReplacementOptions(solverNames, 0, solverName.length());
			},
			PREDICATE_PARAM, CommandUtils.bExpressionCompleter(this.animationSelector.getCurrentTrace())
		));
	}
}
