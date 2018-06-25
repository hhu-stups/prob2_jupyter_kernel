package de.prob2.jupyter.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;

import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoadFileCommand implements Command {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(LoadFileCommand.class);
	
	private final @NotNull ClassicalBFactory classicalBFactory;
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private LoadFileCommand(final @NotNull ClassicalBFactory classicalBFactory, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.classicalBFactory = classicalBFactory;
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":load FILENAME [PREF=VALUE ...]";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Load the machine from the given path.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		final List<String> args = CommandUtils.splitArgs(argString);
		if (args.isEmpty()) {
			throw new UserErrorException("Missing machine file name");
		}
		
		final String fileName = args.get(0);
		final Map<String, String> preferences = CommandUtils.parsePreferences(args.subList(1, args.size()));
		
		try {
			this.animationSelector.changeCurrentAnimation(new Trace(this.classicalBFactory.extract(fileName).load(preferences)));
		} catch (IOException | ModelTranslationError e) {
			throw new RuntimeException(e);
		}
		return new DisplayData("Loaded machine: " + this.animationSelector.getCurrentTrace().getModel());
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		final int fileNameEnd;
		final Matcher argSplitMatcher = CommandUtils.ARG_SPLIT_PATTERN.matcher(argString);
		if (argSplitMatcher.find()) {
			fileNameEnd = argSplitMatcher.start();
		} else {
			fileNameEnd = argString.length();
		}
		
		if (fileNameEnd < at) {
			// Cursor is in the preferences, provide preference name completions.
			final ReplacementOptions replacements = CommandUtils.completeInPreferences(this.animationSelector.getCurrentTrace(), argString.substring(fileNameEnd), at - fileNameEnd);
			return replacements == null ? null : CommandUtils.offsetReplacementOptions(replacements, fileNameEnd);
		} else {
			// Cursor is in the file name, provide machine files from the current directory as completions.
			final String prefix = argString.substring(0, at);
			final List<String> fileNames;
			try (final Stream<Path> list = Files.list(Paths.get(""))) {
				fileNames = list
					.map(Path::getFileName)
					.map(Object::toString)
					.filter(s -> s.startsWith(prefix) && (s.endsWith(".mch") || s.endsWith(".ref") || s.endsWith(".imp")))
					.collect(Collectors.toList());
			} catch (final IOException e) {
				LOGGER.warn("Could not list contents of the current directory, cannot provide file name completions for :load", e);
				return null;
			}
			return new ReplacementOptions(fileNames, 0, fileNameEnd);
		}
	}
}
