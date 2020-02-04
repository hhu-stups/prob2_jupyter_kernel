package de.prob2.jupyter.commands;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.animator.command.GetAllDotCommands;
import de.prob.animator.command.GetSvgForVisualizationCommand;
import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DotCommand implements Command {
	private final @NotNull Provider<@NotNull ProBKernel> kernelProvider;
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private DotCommand(final @NotNull Provider<@NotNull ProBKernel> kernelProvider, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.kernelProvider = kernelProvider;
		this.animationSelector = animationSelector;
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
			if (!item.isAvailable()) {
				sb.append(" (**Not available for this machine/state**: ");
				sb.append(item.getAvailable());
				sb.append(')');
			}
			sb.append('\n');
		}
		return sb.toString();
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		final List<String> split = CommandUtils.splitArgs(argString, 2);
		assert !split.isEmpty();
		final String command = split.get(0);
		final List<IEvalElement> args;
		final String code;
		if (split.size() > 1) {
			code = this.kernelProvider.get().insertLetVariables(split.get(1));
			final IEvalElement formula = CommandUtils.withSourceCode(code, () -> this.animationSelector.getCurrentTrace().getModel().parseFormula(code, FormulaExpand.EXPAND));
			args = Collections.singletonList(formula);
		} else {
			code = null;
			args = Collections.emptyList();
		}
		
		final Trace trace = this.animationSelector.getCurrentTrace();
		final GetAllDotCommands cmd1 = new GetAllDotCommands(trace.getCurrentState());
		trace.getStateSpace().execute(cmd1);
		final DynamicCommandItem item = cmd1.getCommands().stream()
			.filter(i -> command.equals(i.getCommand()))
			.findAny()
			.orElseThrow(() -> new UserErrorException("No such dot command: " + command));
		
		final Path outPath;
		try {
			outPath = Files.createTempFile(null, "svg");
		} catch (final IOException e) {
			throw new UncheckedIOException("Failed to create temp file", e);
		}
		final GetSvgForVisualizationCommand cmd2 = new GetSvgForVisualizationCommand(trace.getCurrentState(), item, outPath.toFile(), args);
		// Provide source code (if any) to error highlighter
		final Runnable execute = () -> trace.getStateSpace().execute(cmd2);
		if (code != null) {
			CommandUtils.withSourceCode(code, execute);
		} else {
			execute.run();
		}
		final String svg;
		try (final Stream<String> lines = Files.lines(outPath)) {
			svg = lines.collect(Collectors.joining("\n"));
		} catch (final IOException e) {
			throw new UncheckedIOException("Failed to read dot output", e);
		}
		final DisplayData result = new DisplayData(String.format("<Dot visualization: %s %s>", command, args));
		result.putSVG(svg);
		return result;
	}
	
	@Override
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		return CommandUtils.inspectArgs(
			argString, at,
			(commandName, at0) -> null, // TODO
			CommandUtils.bExpressionInspector(this.animationSelector.getCurrentTrace())
		);
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		return CommandUtils.completeArgs(
			argString, at,
			(commandName, at0) -> {
				final Trace trace = this.animationSelector.getCurrentTrace();
				final GetAllDotCommands cmd = new GetAllDotCommands(trace.getCurrentState());
				trace.getStateSpace().execute(cmd);
				final String prefix = commandName.substring(0, at0);
				final List<String> commands = cmd.getCommands().stream()
					.filter(DynamicCommandItem::isAvailable)
					.map(DynamicCommandItem::getCommand)
					.filter(s -> s.startsWith(prefix))
					.sorted()
					.collect(Collectors.toList());
				return new ReplacementOptions(commands, 0, commandName.length());
			},
			CommandUtils.bExpressionCompleter(this.animationSelector.getCurrentTrace())
		);
	}
}
