package de.prob2.jupyter.commands;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;

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

public final class DotCommand implements Command {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private DotCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":dot COMMAND [FORMULA]";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Execute and show a dot visualization.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String argString) {
		final List<String> split = CommandUtils.splitArgs(argString, 2);
		assert !split.isEmpty();
		final String command = split.get(0);
		final List<IEvalElement> args;
		if (split.size() > 1) {
			args = Collections.singletonList(this.animationSelector.getCurrentTrace().getModel().parseFormula(split.get(1), FormulaExpand.EXPAND));
		} else {
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
		trace.getStateSpace().execute(cmd2);
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
	public @NotNull ReplacementOptions complete(final @NotNull ProBKernel kernel, final @NotNull String argString, final int at) {
		final int cmdNameEnd;
		final Matcher argSplitMatcher = CommandUtils.ARG_SPLIT_PATTERN.matcher(argString);
		if (argSplitMatcher.find()) {
			cmdNameEnd = argSplitMatcher.start();
		} else {
			cmdNameEnd = argString.length();
		}
		
		if (cmdNameEnd < at) {
			// Cursor is in the formula part of the arguments, provide B completions.
			final ReplacementOptions replacements = CommandUtils.completeInBExpression(this.animationSelector.getCurrentTrace(), argString.substring(cmdNameEnd), at - cmdNameEnd);
			return CommandUtils.offsetReplacementOptions(replacements, cmdNameEnd);
		} else {
			// Cursor is in the first part of the arguments, provide possible command names.
			final Trace trace = this.animationSelector.getCurrentTrace();
			final GetAllDotCommands cmd = new GetAllDotCommands(trace.getCurrentState());
			trace.getStateSpace().execute(cmd);
			final String prefix = argString.substring(0, at);
			final List<String> commands = cmd.getCommands().stream()
				.filter(DynamicCommandItem::isAvailable)
				.map(DynamicCommandItem::getCommand)
				.filter(s -> s.startsWith(prefix))
				.sorted()
				.collect(Collectors.toList());
			return new ReplacementOptions(commands, 0, argString.length());
		}
	}
}
