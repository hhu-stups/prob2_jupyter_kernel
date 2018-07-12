package de.prob2.jupyter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.exception.ProBError;
import de.prob.scripting.ClassicalBFactory;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

import de.prob2.jupyter.commands.AssertCommand;
import de.prob2.jupyter.commands.BrowseCommand;
import de.prob2.jupyter.commands.Command;
import de.prob2.jupyter.commands.CommandExecutionException;
import de.prob2.jupyter.commands.CommandUtils;
import de.prob2.jupyter.commands.ConstantsCommand;
import de.prob2.jupyter.commands.DotCommand;
import de.prob2.jupyter.commands.EvalCommand;
import de.prob2.jupyter.commands.ExecCommand;
import de.prob2.jupyter.commands.FindCommand;
import de.prob2.jupyter.commands.GotoCommand;
import de.prob2.jupyter.commands.GroovyCommand;
import de.prob2.jupyter.commands.HelpCommand;
import de.prob2.jupyter.commands.InitialiseCommand;
import de.prob2.jupyter.commands.LoadCellCommand;
import de.prob2.jupyter.commands.LoadFileCommand;
import de.prob2.jupyter.commands.NoSuchCommandException;
import de.prob2.jupyter.commands.PrefCommand;
import de.prob2.jupyter.commands.PrettyPrintCommand;
import de.prob2.jupyter.commands.RenderCommand;
import de.prob2.jupyter.commands.ShowCommand;
import de.prob2.jupyter.commands.SolveCommand;
import de.prob2.jupyter.commands.TableCommand;
import de.prob2.jupyter.commands.TimeCommand;
import de.prob2.jupyter.commands.TraceCommand;
import de.prob2.jupyter.commands.TypeCommand;
import de.prob2.jupyter.commands.VersionCommand;
import de.prob2.jupyter.commands.WithSourceCodeException;

import io.github.spencerpark.jupyter.kernel.BaseKernel;
import io.github.spencerpark.jupyter.kernel.LanguageInfo;
import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;
import io.github.spencerpark.jupyter.kernel.display.mime.MIMEType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class ProBKernel extends BaseKernel {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(ProBKernel.class);
	
	private static final @NotNull Pattern COMMAND_PATTERN = Pattern.compile("\\s*(\\:[^\\s]*)(?:\\h*(.*))?", Pattern.DOTALL);
	private static final @NotNull Pattern SPACE_PATTERN = Pattern.compile("\\s*");
	private static final @NotNull Pattern BSYMB_COMMAND_PATTERN = Pattern.compile("\\\\([a-z]+)");
	private static final @NotNull Pattern LATEX_FORMULA_PATTERN = Pattern.compile("(\\$\\$?)([^\\$]+)\\1");
	
	private static final @NotNull Map<@NotNull String, @NotNull String> BSYMB_COMMAND_DEFINITIONS;
	static {
		final Map<String, String> map = new HashMap<>();
		map.put("bfalse", "\\newcommand{\\bfalse}{\\mathord\\bot}");
		map.put("btrue", "\\newcommand{\\btrue}{\\mathord\\top}");
		map.put("limp", "\\newcommand{\\limp}{\\mathbin\\Rightarrow}");
		map.put("leqv", "\\newcommand{\\leqv}{\\mathbin\\Leftrightarrow}");
		map.put("qdot", "\\newcommand{\\qdot}{\\mathord{\\mkern1mu\\cdot\\mkern1mu}}");
		map.put("defi", "\\newcommand{\\defi}{\\mathrel{≙}}");
		map.put("pow", "\\newcommand{\\pow}{\\mathop{\\mathbb P\\hbox{}}\\nolimits}");
		map.put("pown", "\\newcommand{\\pown}{\\mathop{\\mathbb P_1}\\nolimits}");
		map.put("cprod", "\\newcommand{\\cprod}{\\mathbin\\times}");
		map.put("bunion", "\\newcommand{\\bunion}{\\mathbin{\\mkern1mu\\cup\\mkern1mu}}");
		map.put("binter", "\\newcommand{\\binter}{\\mathbin{\\mkern1mu\\cap\\mkern1mu}}");
		map.put("union", "\\newcommand{\\union}{\\mathop{\\mathrm{union}}\\nolimits}");
		map.put("inter", "\\newcommand{\\inter}{\\mathop{\\mathrm{inter}}\\nolimits}");
		map.put("Union", "\\newcommand{\\Union}{\\bigcup\\nolimits}");
		map.put("Inter", "\\newcommand{\\Inter}{\\bigcap\\nolimits}");
		map.put("emptyset", "\\renewcommand{\\emptyset}{\\mathord\\varnothing}");
		map.put("rel", "\\newcommand{\\rel}{\\mathbin{<\\mkern-10mu-\\mkern-10mu>}}");
		map.put("trel", "\\newcommand{\\trel}{\\mathbin{<\\mkern-6mu<\\mkern-10mu-\\mkern-10mu>}}");
		map.put("srel", "\\newcommand{\\srel}{\\mathbin{<\\mkern-10mu-\\mkern-10mu>\\mkern-6mu>}}");
		map.put("strel", "\\newcommand{\\strel}{\\mathbin{<\\mkern-6mu<\\mkern-10mu-\\mkern-10mu>\\mkern-6mu>}}");
		map.put("dom", "\\newcommand{\\dom}{\\mathop{\\mathrm{dom}}\\nolimits}");
		map.put("ran", "\\newcommand{\\ran}{\\mathop{\\mathrm{ran}}\\nolimits}");
		map.put("fcomp", "\\newcommand{\\fcomp}{\\mathbin;}");
		map.put("bcomp", "\\newcommand{\\bcomp}{\\circ}");
		map.put("id", "\\newcommand{\\id}{\\mathop{\\mathrm{id}}\\nolimits}");
		map.put("domres", "\\newcommand{\\domres}{\\mathbin◁}");
		map.put("domsub", "\\newcommand{\\domsub}{\\mathbin⩤}");
		map.put("ranres", "\\newcommand{\\ranres}{\\mathbin▷}");
		map.put("ransub", "\\newcommand{\\ransub}{\\mathbin⩥}");
		map.put("ovl", "\\newcommand{\\ovl}{\\mathbin{<\\mkern-11mu+}}");
		map.put("dprod", "\\newcommand{\\dprod}{\\mathbin\\otimes}");
		map.put("prjone", "\\newcommand{\\prjone}{\\mathop{\\mathrm{prj}_1}\\nolimits}");
		map.put("prjtwo", "\\newcommand{\\prjtwo}{\\mathop{\\mathrm{prj}_2}\\nolimits}");
		map.put("pprod", "\\newcommand{\\pprod}{\\mathbin\\mid}");
		map.put("pfun", "\\newcommand{\\pfun}{\\mathbin↦}");
		map.put("tfun", "\\newcommand{\\tfun}{\\mathbin→}");
		map.put("pinj", "\\newcommand{\\pinj}{\\mathbin⤔}");
		map.put("tinj", "\\newcommand{\\tinj}{\\mathbin↣}");
		map.put("psur", "\\newcommand{\\psur}{\\mathbin⤅}");
		map.put("tsur", "\\newcommand{\\tsur}{\\mathbin↠}");
		map.put("tbij", "\\newcommand{\\tbij}{\\mathbin⤖}");
		map.put("nat", "\\newcommand{\\nat}{\\mathord{\\mathbb N}}");
		map.put("natn", "\\newcommand{\\natn}{\\mathord{\\mathbb N_1}}");
		map.put("intg", "\\newcommand{\\intg}{\\mathord{\\mathbb Z}}");
		map.put("upto", "\\newcommand{\\upto}{\\mathbin{.\\mkern1mu.}}");
		map.put("finite", "\\newcommand{\\finite}{\\mathop{\\mathrm{finite}}\\nolimits}");
		map.put("card", "\\newcommand{\\card}{\\mathop{\\mathrm{card}}\\nolimits}");
		map.put("upred", "\\newcommand{\\upred}{\\mathop{\\mathrm{pred}}\\nolimits}");
		map.put("usucc", "\\newcommand{\\usucc}{\\mathop{\\mathrm{succ}}\\nolimits}");
		map.put("expn", "\\newcommand{\\expn}{\\mathbin{\\widehat{\\mkern1em}}}");
		map.put("Bool", "\\newcommand{\\Bool}{\\mathord{\\mathrm{BOOL}}}");
		map.put("bool", "\\newcommand{\\bool}{\\mathop{\\mathrm{bool}}\\nolimits}");
		map.put("bcmeq", "\\newcommand{\\bcmeq}{\\mathrel{:\\mkern1mu=}}");
		map.put("bcmin", "\\newcommand{\\bcmin}{\\mathrel{:\\mkern1mu\\in}}");
		map.put("bcmsuch", "\\newcommand{\\bcmsuch}{\\mathrel{:\\mkern1mu\\mid}}");
		BSYMB_COMMAND_DEFINITIONS = Collections.unmodifiableMap(map);
	}
	
	private final @NotNull AnimationSelector animationSelector;
	
	private final @NotNull Map<@NotNull String, @NotNull Command> commands;
	
	@Inject
	private ProBKernel(final @NotNull Injector injector, final @NotNull ClassicalBFactory classicalBFactory, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.setShouldReplaceStdStreams(false);
		
		this.animationSelector = animationSelector;
		
		this.commands = new HashMap<>();
		this.commands.put(":help", injector.getInstance(HelpCommand.class));
		this.commands.put(":version", injector.getInstance(VersionCommand.class));
		this.commands.put(":eval", injector.getInstance(EvalCommand.class));
		this.commands.put(":type", injector.getInstance(TypeCommand.class));
		this.commands.put(":table", injector.getInstance(TableCommand.class));
		this.commands.put(":solve", injector.getInstance(SolveCommand.class));
		this.commands.put(":load", injector.getInstance(LoadFileCommand.class));
		this.commands.put("::load", injector.getInstance(LoadCellCommand.class));
		this.commands.put(":pref", injector.getInstance(PrefCommand.class));
		this.commands.put(":browse", injector.getInstance(BrowseCommand.class));
		this.commands.put(":trace", injector.getInstance(TraceCommand.class));
		this.commands.put(":exec", injector.getInstance(ExecCommand.class));
		this.commands.put(":constants", injector.getInstance(ConstantsCommand.class));
		this.commands.put(":init", injector.getInstance(InitialiseCommand.class));
		this.commands.put(":goto", injector.getInstance(GotoCommand.class));
		this.commands.put(":find", injector.getInstance(FindCommand.class));
		this.commands.put(":show", injector.getInstance(ShowCommand.class));
		this.commands.put(":dot", injector.getInstance(DotCommand.class));
		this.commands.put(":assert", injector.getInstance(AssertCommand.class));
		this.commands.put(":time", injector.getInstance(TimeCommand.class));
		this.commands.put(":groovy", injector.getInstance(GroovyCommand.class));
		this.commands.put("::render", injector.getInstance(RenderCommand.class));
		this.commands.put(":prettyprint", injector.getInstance(PrettyPrintCommand.class));
		
		this.animationSelector.changeCurrentAnimation(new Trace(classicalBFactory.create("(initial Jupyter machine)", "MACHINE repl END").load()));
	}
	
	public @NotNull Map<@NotNull String, @NotNull Command> getCommands() {
		return Collections.unmodifiableMap(this.commands);
	}
	
	@Override
	public @NotNull String getBanner() {
		return "ProB Interactive Expression and Predicate Evaluator (on Jupyter)\nType \":help\" for more information.";
	}
	
	@Override
	public @NotNull List<LanguageInfo.@NotNull Help> getHelpLinks() {
		return Collections.singletonList(new LanguageInfo.Help("ProB User Manual", "https://www3.hhu.de/stups/prob/index.php/User_Manual"));
	}
	
	private static @NotNull String addBsymbDefinitions(final @NotNull String markdown) {
		final StringBuilder defs = new StringBuilder();
		final Matcher matcher = BSYMB_COMMAND_PATTERN.matcher(markdown);
		while (matcher.find()) {
			defs.append(BSYMB_COMMAND_DEFINITIONS.getOrDefault(matcher.group(1), ""));
		}
		
		if (defs.length() > 0) {
			// Find the first LaTeX formula in the output and add the definitions to it.
			final Matcher latexFormulaMatcher = LATEX_FORMULA_PATTERN.matcher(markdown);
			if (latexFormulaMatcher.find()) {
				// We do this manually instead of using Matcher.replaceFirst to prevent backslashes from being processed.
				return markdown.substring(0, latexFormulaMatcher.start())
					+ latexFormulaMatcher.group(1)
					+ defs
					+ latexFormulaMatcher.group(2)
					+ latexFormulaMatcher.group(1)
					+ markdown.substring(latexFormulaMatcher.end());
			} else {
				// No LaTeX formula found, add an extra one at the start.
				// This can produce an unwanted empty line at the start, so we avoid this method if possible.
				return "$" + defs + "$\n\n" + markdown;
			}
		} else {
			// No definitions needed, so don't modify the output.
			return markdown;
		}
	}
	
	private @Nullable DisplayData executeCommand(final @NotNull String name, final @NotNull String argString) {
		final Command command = this.getCommands().get(name);
		if (command == null) {
			throw new NoSuchCommandException(name);
		}
		final DisplayData result;
		try {
			result = command.run(argString);
		} catch (final UserErrorException e) {
			throw new CommandExecutionException(name, e);
		}
		
		if (result != null && result.hasDataForType(MIMEType.TEXT_MARKDOWN)) {
			// Add definitions for any used bsymb LaTeX commands to Markdown output.
			final String markdown = (String)result.getData(MIMEType.TEXT_MARKDOWN);
			result.putMarkdown(addBsymbDefinitions(markdown));
		}
		
		return result;
	}
	
	@Override
	public @Nullable DisplayData eval(final String expr) {
		assert expr != null;
		
		final Matcher commandMatcher = COMMAND_PATTERN.matcher(expr);
		if (commandMatcher.matches()) {
			final String name = commandMatcher.group(1);
			assert name != null;
			final String argString = commandMatcher.group(2) == null ? "" : commandMatcher.group(2);
			return this.executeCommand(name, argString);
		}
		
		return this.executeCommand(":eval", expr);
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String code, final int at) {
		final Matcher commandMatcher = COMMAND_PATTERN.matcher(code);
		if (commandMatcher.matches()) {
			// The code is a valid command.
			final int argOffset = commandMatcher.start(2);
			if (at <= commandMatcher.end(1)) {
				// The cursor is somewhere in the command name, provide command completions.
				final String prefix = code.substring(commandMatcher.start(1), at);
				return new ReplacementOptions(
					this.getCommands().keySet().stream().filter(s -> s.startsWith(prefix)).sorted().collect(Collectors.toList()),
					commandMatcher.start(1), 
					commandMatcher.end(1)
				);
			} else if (at < commandMatcher.start(2)) {
				// The cursor is in the whitespace between the command name and arguments, don't show anything.
				return null;
			} else {
				// The cursor is somewhere in the command arguments, ask the command to provide completions.
				final String name = commandMatcher.group(1);
				assert name != null;
				final String argString = commandMatcher.group(2) == null ? "" : commandMatcher.group(2);
				if (this.getCommands().containsKey(name)) {
					final ReplacementOptions replacements = this.getCommands().get(name).complete(argString, at - argOffset);
					return replacements == null ? null : CommandUtils.offsetReplacementOptions(replacements, argOffset);
				} else {
					// Invalid command, can't provide any completions.
					return null;
				}
			}
		} else if (SPACE_PATTERN.matcher(code).matches()) {
			// The code contains only whitespace, provide completions from :eval and for command names.
			final List<String> replacementStrings = new ArrayList<>();
			final ReplacementOptions evalReplacements = this.getCommands().get(":eval").complete(code, at);
			if (evalReplacements != null) {
				replacementStrings.addAll(evalReplacements.getReplacements());
			}
			replacementStrings.addAll(this.getCommands().keySet().stream().sorted().collect(Collectors.toList()));
			return new ReplacementOptions(replacementStrings, at, at);
		} else {
			// The code is not a valid command, ask :eval for completions.
			return this.getCommands().get(":eval").complete(code, at);
		}
	}
	
	@Override
	public @NotNull LanguageInfo getLanguageInfo() {
		return new LanguageInfo.Builder("prob")
			.mimetype("text/x-prob2-jupyter-repl")
			.fileExtension(".prob")
			.codemirror("prob2_jupyter_repl")
			.build();
	}
	
	@Override
	public void onShutdown(final boolean isRestarting) {
		this.animationSelector.getCurrentTrace().getStateSpace().kill();
	}
	
	private @NotNull List<@NotNull String> formatErrorSource(final @NotNull List<@NotNull String> sourceLines, final @NotNull ErrorItem.Location location) {
		final List<String> out = new ArrayList<>();
		if (location.getStartLine() < 1 || location.getStartLine() > sourceLines.size()) {
			out.add(this.errorStyler.secondary(String.format("Error start line %d out of bounds (1..%d)", location.getStartLine(), sourceLines.size())));
			return out;
		}
		if (location.getEndLine() < 1 || location.getEndLine() > sourceLines.size()) {
			out.add(this.errorStyler.secondary(String.format("Error end line %d out of bounds (1..%d)", location.getEndLine(), sourceLines.size())));
			return out;
		}
		final List<String> errorLines = sourceLines.subList(location.getStartLine()-1, location.getEndLine());
		assert !errorLines.isEmpty();
		final String firstLine = errorLines.get(0);
		final String lastLine = errorLines.get(errorLines.size() - 1);
		if (location.getStartColumn() < 0 || location.getStartColumn() > firstLine.length()) {
			out.add(this.errorStyler.secondary(String.format("Error start column %d out of bounds (0..%d)", location.getStartColumn(), firstLine.length()-1)));
			return out;
		}
		if (location.getEndColumn() < 0 || location.getEndColumn() > lastLine.length()) {
			out.add(this.errorStyler.secondary(String.format("Error end column %d out of bounds (0..%d)", location.getEndColumn(), lastLine.length()-1)));
			return out;
		}
		if (errorLines.size() == 1) {
			out.add(
				this.errorStyler.primary(firstLine.substring(0, location.getStartColumn()))
				+ this.errorStyler.highlight(firstLine.substring(location.getStartColumn(), location.getEndColumn()))
				+ this.errorStyler.primary(firstLine.substring(location.getEndColumn()))
			);
		} else {
			out.add(
				this.errorStyler.primary(firstLine.substring(0, location.getStartColumn()))
				+ this.errorStyler.highlight(firstLine.substring(location.getStartColumn()))
			);
			errorLines.subList(1, errorLines.size()-1).stream().map(this.errorStyler::highlight).collect(Collectors.toCollection(() -> out));
			out.add(
				this.errorStyler.highlight(lastLine.substring(0, location.getEndColumn()))
				+ this.errorStyler.primary(lastLine.substring(location.getEndColumn()))
			);
		}
		return out;
	}
	
	@Override
	public @NotNull List<@NotNull String> formatError(final Exception e) {
		try {
			LOGGER.warn("Exception while executing command from user", e);
			if (e instanceof UserErrorException) {
				return this.errorStyler.secondaryLines(String.valueOf(e.getMessage()));
			} else if (e instanceof ProBError || e instanceof WithSourceCodeException) {
				final List<String> sourceLines;
				final ProBError proBError;
				if (e instanceof WithSourceCodeException) {
					sourceLines = Arrays.asList(((WithSourceCodeException)e).getSourceCode().split("\n"));
					proBError = ((WithSourceCodeException)e).getCause();
				} else {
					sourceLines = Collections.emptyList();
					proBError = (ProBError)e;
				}
				final List<String> out = new ArrayList<>(Arrays.asList((
					this.errorStyler.primary("Error from ProB: ")
					+ this.errorStyler.secondary(String.valueOf(proBError.getOriginalMessage()))
				).split("\n")));
				if (proBError.getErrors() == null) {
					// If the errors list is null rather than empty, don't show the "no error messages" message.
					// (This matches the normal behavior of ProBError.)
				} else if (proBError.getErrors().isEmpty()) {
					out.addAll(this.errorStyler.primaryLines("ProB returned no error messages.\n"));
				} else {
					if (proBError.getErrors().size() > 1) {
						out.addAll(this.errorStyler.primaryLines(proBError.getErrors().size() + " errors:\n"));
					}
					for (final ErrorItem error : proBError.getErrors()) {
						out.addAll(this.errorStyler.secondaryLines(error.toString()));
						for (final ErrorItem.Location location : error.getLocations()) {
							out.addAll(formatErrorSource(sourceLines, location));
						}
					}
				}
				return out;
			} else {
				return super.formatError(e);
			}
		} catch (final RuntimeException e2) {
			LOGGER.error("Exception in error formatting", e2);
			throw e2;
		}
	}
}
