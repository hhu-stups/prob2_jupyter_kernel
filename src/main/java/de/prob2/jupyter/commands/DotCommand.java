package de.prob2.jupyter.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.animator.command.GetAllDotCommands;
import de.prob.animator.domainobjects.DotVisualizationCommand;
import de.prob.animator.domainobjects.DynamicCommandItem;
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

public final class DotCommand implements Command {
	private static final @NotNull Parameter.RequiredSingle COMMAND_PARAM = Parameter.required("command");
	private static final @NotNull Parameter.OptionalSingle FORMULA_PARAM = Parameter.optionalRemainder("formula");
	
	private final @NotNull Provider<@NotNull ProBKernel> kernelProvider;
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private DotCommand(final @NotNull Provider<@NotNull ProBKernel> kernelProvider, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.kernelProvider = kernelProvider;
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":dot";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return new Parameters(Arrays.asList(COMMAND_PARAM, FORMULA_PARAM));
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":dot COMMAND [FORMULA]";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Execute and show a dot visualisation.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		final StringBuilder sb = new StringBuilder("The following dot visualisation commands are available:\n\n");
		final Trace trace = this.animationSelector.getCurrentTrace();
		final GetAllDotCommands cmd = new GetAllDotCommands(trace.getCurrentState());
		trace.getStateSpace().execute(cmd);
		for (final DynamicCommandItem item : cmd.getCommands()) {
			sb.append("* `");
			sb.append(item.getCommand());
			sb.append("` - ");
			sb.append(item.getName());
			sb.append(": ");
			sb.append(item.getDescription());
			sb.append('\n');
		}
		return sb.toString();
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final String command = args.get(COMMAND_PARAM);
		final List<IEvalElement> dotCommandArgs;
		final String code;
		if (args.get(FORMULA_PARAM).isPresent()) {
			final ProBKernel kernel = this.kernelProvider.get();
			code = kernel.insertLetVariables(args.get(FORMULA_PARAM).get());
			final IEvalElement formula = CommandUtils.withSourceCode(code, () -> kernel.parseFormula(code, FormulaExpand.EXPAND));
			dotCommandArgs = Collections.singletonList(formula);
		} else {
			code = null;
			dotCommandArgs = Collections.emptyList();
		}
		
		final Trace trace = this.animationSelector.getCurrentTrace();
		final DotVisualizationCommand dotCommand = DotVisualizationCommand.getAll(trace.getCurrentState())
			.stream()
			.filter(i -> command.equals(i.getCommand()))
			.findAny()
			.orElseThrow(() -> new UserErrorException("No such dot command: " + command));
		
		// Provide source code (if any) to error highlighter
		final Supplier<String> execute = () -> dotCommand.visualizeAsSvgToString(dotCommandArgs);
		final String svg;
		if (code != null) {
			svg = CommandUtils.withSourceCode(code, execute);
		} else {
			svg = execute.get();
		}
		final DisplayData result = new DisplayData(String.format("<Dot visualization: %s %s>", command, dotCommandArgs));
		result.putSVG(svg);
		return result;
	}
	
	@Override
	public @NotNull ParameterInspectors getParameterInspectors() {
		return new ParameterInspectors(ImmutableMap.of(
			COMMAND_PARAM, (commandName, at) -> null, // TODO
			FORMULA_PARAM, CommandUtils.bExpressionInspector(this.animationSelector.getCurrentTrace())
		));
	}
	
	@Override
	public @NotNull ParameterCompleters getParameterCompleters() {
		return new ParameterCompleters(ImmutableMap.of(
			COMMAND_PARAM, (commandName, at) -> {
				final Trace trace = this.animationSelector.getCurrentTrace();
				final GetAllDotCommands cmd = new GetAllDotCommands(trace.getCurrentState());
				trace.getStateSpace().execute(cmd);
				final String prefix = commandName.substring(0, at);
				final List<String> commands = DotVisualizationCommand.getAll(trace.getCurrentState())
					.stream()
					.filter(DynamicCommandItem::isAvailable)
					.map(DynamicCommandItem::getCommand)
					.filter(s -> s.startsWith(prefix))
					.sorted()
					.collect(Collectors.toList());
				return new ReplacementOptions(commands, 0, commandName.length());
			},
			FORMULA_PARAM, CommandUtils.bExpressionCompleter(this.animationSelector.getCurrentTrace())
		));
	}
}
