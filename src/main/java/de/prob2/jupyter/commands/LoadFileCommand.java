package de.prob2.jupyter.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

import de.prob.scripting.FactoryProvider;
import de.prob.scripting.ModelFactory;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.CommandUtils;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.PositionalParameter;
import de.prob2.jupyter.ProBKernel;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoadFileCommand implements Command {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(LoadFileCommand.class);
	
	private static final @NotNull PositionalParameter.RequiredSingle FILE_NAME_PARAM = new PositionalParameter.RequiredSingle("fileName");
	private static final @NotNull PositionalParameter.OptionalMultiple PREFS_PARAM = new PositionalParameter.OptionalMultiple("prefs");
	
	private final @NotNull Injector injector;
	private final @NotNull AnimationSelector animationSelector;
	private final @NotNull Provider<ProBKernel> proBKernelProvider;
	
	@Inject
	private LoadFileCommand(
		final @NotNull Injector injector,
		final @NotNull AnimationSelector animationSelector,
		final @NotNull Provider<ProBKernel> proBKernelProvider
	) {
		super();
		
		this.injector = injector;
		this.animationSelector = animationSelector;
		this.proBKernelProvider = proBKernelProvider;
	}
	
	@Override
	public @NotNull String getName() {
		return ":load";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return new Parameters(Arrays.asList(FILE_NAME_PARAM, PREFS_PARAM));
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":load FILENAME [PREF=VALUE ...]";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Load a machine from a file.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "The file path is relative to the kernel's current directory (i. e. the directory in which the notebook is located).\n\n"
			+ "After the file path, you can set the values of one or more ProB preferences that should be applied to the newly loaded machine. Preferences can also be changed using the `:pref` command after a machine has been loaded, however certain preferences do not take full effect when set using `:pref` and must be set when the machine is loaded.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final Path machineFilePath = Paths.get(args.get(FILE_NAME_PARAM));
		final String machineFileName = machineFilePath.getFileName().toString();
		final Path machineFileDirectory = machineFilePath.getParent() == null ? Paths.get("") : machineFilePath.getParent();
		final int dotIndex = machineFileName.lastIndexOf('.');
		if (dotIndex == -1) {
			throw new UserErrorException("File has no extension, unable to determine language: " + machineFileName);
		}
		final String extension = machineFileName.substring(dotIndex+1);
		if (!FactoryProvider.isExtensionKnown(extension)) {
			throw new UserErrorException("Unsupported file type: ." + extension);
		}
		final Map<String, String> preferences = CommandUtils.parsePreferences(args.get(PREFS_PARAM));
		
		try {
			final ModelFactory<?> factory = this.injector.getInstance(FactoryProvider.factoryClassFromExtension(extension));
			this.animationSelector.changeCurrentAnimation(new Trace(factory.extract(machineFilePath.toString()).load(preferences)));
			this.proBKernelProvider.get().setCurrentMachineDirectory(machineFileDirectory);
		} catch (IOException | ModelTranslationError e) {
			throw new RuntimeException(e);
		}
		return new DisplayData("Loaded machine: " + this.animationSelector.getCurrentTrace().getStateSpace().getMainComponent());
	}
	
	@Override
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		return CommandUtils.inspectArgs(
			argString, at,
			(filename, at0) -> null,
			CommandUtils.preferencesInspector(this.animationSelector.getCurrentTrace())
		);
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
							return FactoryProvider.isExtensionKnown(extension);
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
