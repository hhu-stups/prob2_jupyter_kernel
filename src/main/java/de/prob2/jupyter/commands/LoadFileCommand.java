package de.prob2.jupyter.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.scripting.CSPFactory;
import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.EventBFactory;
import de.prob.scripting.ModelFactory;
import de.prob.scripting.ModelTranslationError;
import de.prob.scripting.TLAFactory;
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
	
	private static final @NotNull Map<@NotNull String, @NotNull Class<? extends ModelFactory<?>>> EXTENSION_TO_FACTORY_MAP;
	static {
		final Map<String, Class<? extends ModelFactory<?>>> extensionToFactoryMap = new HashMap<>();
		extensionToFactoryMap.put("mch", ClassicalBFactory.class);
		extensionToFactoryMap.put("ref", ClassicalBFactory.class);
		extensionToFactoryMap.put("imp", ClassicalBFactory.class);
		extensionToFactoryMap.put("eventb", EventBFactory.class);
		extensionToFactoryMap.put("bum", EventBFactory.class);
		extensionToFactoryMap.put("buc", EventBFactory.class);
		extensionToFactoryMap.put("csp", CSPFactory.class);
		extensionToFactoryMap.put("cspm", CSPFactory.class);
		extensionToFactoryMap.put("tla", TLAFactory.class);
		// FIXME Not currently possible, because RulesModelFactory does not implement the ModelFactory interface
		//extensionToFactoryMap.put("rmch", RulesModelFactory.class);
		EXTENSION_TO_FACTORY_MAP = Collections.unmodifiableMap(extensionToFactoryMap);
	}
	
	private final @NotNull Injector injector;
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private LoadFileCommand(final @NotNull Injector injector, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.injector = injector;
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
	public @NotNull String getHelpBody() {
		return "The path is relative to the kernel's current directory (i. e. the directory in which the notebook is located).\n\n"
			+ "Any number of preference assignments may be included after the file path. Preferences can also be changed on a loaded machine using the `:pref` command, however certain preferences do not take full effect when set using `:pref` and must be set when the machine is loaded.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		final List<String> args = CommandUtils.splitArgs(argString);
		if (args.isEmpty()) {
			throw new UserErrorException("Missing machine file name");
		}
		
		final String fileName = args.get(0);
		final int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex == -1) {
			throw new UserErrorException("File has no extension, unable to determine language: " + fileName);
		}
		final String extension = fileName.substring(dotIndex+1);
		if (!EXTENSION_TO_FACTORY_MAP.containsKey(extension)) {
			throw new UserErrorException("Unsupported file type: ." + extension);
		}
		final Map<String, String> preferences = CommandUtils.parsePreferences(args.subList(1, args.size()));
		
		try {
			final ModelFactory<?> factory = this.injector.getInstance(EXTENSION_TO_FACTORY_MAP.get(extension));
			this.animationSelector.changeCurrentAnimation(new Trace(factory.extract(fileName).load(preferences)));
		} catch (IOException | ModelTranslationError e) {
			throw new RuntimeException(e);
		}
		return new DisplayData("Loaded machine: " + this.animationSelector.getCurrentTrace().getStateSpace().getMainComponent());
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		return CommandUtils.completeArgs(
			argString, at,
			(filename, at0) -> {
				final String prefix = filename.substring(0, at0);
				final List<String> fileNames;
				try (final Stream<Path> list = Files.list(Paths.get(""))) {
					fileNames = list
						.map(Path::getFileName)
						.map(Object::toString)
						.filter(s -> s.startsWith(prefix))
						.filter(s -> {
							final int dotIndex = s.lastIndexOf('.');
							if (dotIndex == -1) {
								return false;
							}
							final String extension = s.substring(dotIndex+1);
							return EXTENSION_TO_FACTORY_MAP.containsKey(extension);
						})
						.collect(Collectors.toList());
				} catch (final IOException e) {
					LOGGER.warn("Could not list contents of the current directory, cannot provide file name completions for :load", e);
					return null;
				}
				return new ReplacementOptions(fileNames, 0, filename.length());
			},
			CommandUtils.preferencesCompleter(this.animationSelector.getCurrentTrace())
		);
	}
}
