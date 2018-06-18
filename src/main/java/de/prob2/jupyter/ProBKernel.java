package de.prob2.jupyter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.exception.ProBError;
import de.prob.scripting.ClassicalBFactory;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

import de.prob2.jupyter.commands.AssertCommand;
import de.prob2.jupyter.commands.BrowseCommand;
import de.prob2.jupyter.commands.Command;
import de.prob2.jupyter.commands.CommandExecutionException;
import de.prob2.jupyter.commands.ConstantsCommand;
import de.prob2.jupyter.commands.EvalCommand;
import de.prob2.jupyter.commands.ExecCommand;
import de.prob2.jupyter.commands.GroovyCommand;
import de.prob2.jupyter.commands.HelpCommand;
import de.prob2.jupyter.commands.InitialiseCommand;
import de.prob2.jupyter.commands.LoadCellCommand;
import de.prob2.jupyter.commands.LoadFileCommand;
import de.prob2.jupyter.commands.NoSuchCommandException;
import de.prob2.jupyter.commands.PrefCommand;
import de.prob2.jupyter.commands.PrettyPrintCommand;
import de.prob2.jupyter.commands.RenderCommand;
import de.prob2.jupyter.commands.SolveCommand;
import de.prob2.jupyter.commands.TableCommand;
import de.prob2.jupyter.commands.TimeCommand;
import de.prob2.jupyter.commands.TypeCommand;
import de.prob2.jupyter.commands.VersionCommand;

import io.github.spencerpark.jupyter.kernel.BaseKernel;
import io.github.spencerpark.jupyter.kernel.LanguageInfo;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;
import io.github.spencerpark.jupyter.kernel.display.mime.MIMEType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProBKernel extends BaseKernel {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(ProBKernel.class);
	
	private static final @NotNull Pattern COMMAND_PATTERN = Pattern.compile("\\s*(\\:[^\\s]*)(?:\\h*(.*))?", Pattern.DOTALL);
	private static final @NotNull Pattern BSYMB_COMMAND_PATTERN = Pattern.compile("\\\\([a-z]+)");
	
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
		final Command help = injector.getInstance(HelpCommand.class);
		this.commands.put(":?", help);
		this.commands.put(":help", help);
		this.commands.put(":version", injector.getInstance(VersionCommand.class));
		this.commands.put(":eval", injector.getInstance(EvalCommand.class));
		this.commands.put(":type", injector.getInstance(TypeCommand.class));
		this.commands.put(":table", injector.getInstance(TableCommand.class));
		this.commands.put(":solve", injector.getInstance(SolveCommand.class));
		this.commands.put(":load", injector.getInstance(LoadFileCommand.class));
		this.commands.put("::load", injector.getInstance(LoadCellCommand.class));
		this.commands.put(":pref", injector.getInstance(PrefCommand.class));
		this.commands.put(":browse", injector.getInstance(BrowseCommand.class));
		this.commands.put(":exec", injector.getInstance(ExecCommand.class));
		this.commands.put(":constants", injector.getInstance(ConstantsCommand.class));
		this.commands.put(":initialise", injector.getInstance(InitialiseCommand.class));
		this.commands.put(":init", injector.getInstance(InitialiseCommand.class));
		this.commands.put(":assert", injector.getInstance(AssertCommand.class));
		this.commands.put(":time", injector.getInstance(TimeCommand.class));
		this.commands.put(":groovy", injector.getInstance(GroovyCommand.class));
		this.commands.put("::render", injector.getInstance(RenderCommand.class));
		this.commands.put(":prettyprint", injector.getInstance(PrettyPrintCommand.class));
		
		this.animationSelector.changeCurrentAnimation(new Trace(classicalBFactory.create("MACHINE repl END").load()));
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
	
	private @Nullable DisplayData executeCommand(final @NotNull String name, final @NotNull String argString) {
		final Command command = this.getCommands().get(name);
		if (command == null) {
			throw new NoSuchCommandException(name);
		}
		final DisplayData result;
		try {
			result = command.run(this, argString);
		} catch (final UserErrorException e) {
			throw new CommandExecutionException(name, e);
		}
		
		if (result != null && result.hasDataForType(MIMEType.TEXT_MARKDOWN)) {
			// Add definitions for any used bsymb LaTeX commands to Markdown output.
			final String markdown = (String)result.getData(MIMEType.TEXT_MARKDOWN);
			final StringBuilder defs = new StringBuilder();
			final Matcher matcher = BSYMB_COMMAND_PATTERN.matcher(markdown);
			while (matcher.find()) {
				defs.append(BSYMB_COMMAND_DEFINITIONS.getOrDefault(matcher.group(1), ""));
			}
			if (defs.length() > 0) {
				result.putMarkdown("$" + defs + "$\n" + markdown);
			}
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
	
	@Override
	public @NotNull List<@NotNull String> formatError(final Exception e) {
		try {
			if (e instanceof UserErrorException) {
				return this.errorStyler.secondaryLines(String.valueOf(e.getMessage()));
			} else if (e instanceof ProBError) {
				final ProBError proBError = (ProBError)e;
				final List<String> out = new ArrayList<>(Arrays.asList((
					this.errorStyler.primary("Error from ProB: ")
					+ this.errorStyler.secondary(String.valueOf(proBError.getOriginalMessage()))
				).split("\n")));
				if (proBError.getErrors() == null) {
					// If the errors list is null rather than empty, don't show the "no error messages" message.
					// (This matches the normal behavior of ProBError.)
				} else if (proBError.getErrors().isEmpty()) {
					out.addAll(this.errorStyler.primaryLines("ProB returned no error messages.\n"));
				} else if (proBError.getErrors().size() == 1) {
					out.addAll(this.errorStyler.secondaryLines(proBError.getErrors().get(0).toString()));
				} else {
					out.addAll(this.errorStyler.primaryLines(proBError.getErrors().size() + " errors:\n"));
					for (final ErrorItem error : proBError.getErrors()) {
						out.addAll(this.errorStyler.secondaryLines(error.toString()));
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
