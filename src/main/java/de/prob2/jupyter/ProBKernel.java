package de.prob2.jupyter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.MoreObjects;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.ReusableAnimator;
import de.prob.animator.command.ExecuteOperationException;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.EvalExpandMode;
import de.prob.animator.domainobjects.EvalOptions;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob.scripting.ClassicalBFactory;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Language;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.jupyter.commands.AssertCommand;
import de.prob2.jupyter.commands.BrowseCommand;
import de.prob2.jupyter.commands.BsymbCommand;
import de.prob2.jupyter.commands.CheckCommand;
import de.prob2.jupyter.commands.ConstantsCommand;
import de.prob2.jupyter.commands.DotCommand;
import de.prob2.jupyter.commands.EvalCommand;
import de.prob2.jupyter.commands.ExecCommand;
import de.prob2.jupyter.commands.FindCommand;
import de.prob2.jupyter.commands.GotoCommand;
import de.prob2.jupyter.commands.GroovyCommand;
import de.prob2.jupyter.commands.HelpCommand;
import de.prob2.jupyter.commands.InitialiseCommand;
import de.prob2.jupyter.commands.LanguageCommand;
import de.prob2.jupyter.commands.LetCommand;
import de.prob2.jupyter.commands.LoadCellCommand;
import de.prob2.jupyter.commands.LoadFileCommand;
import de.prob2.jupyter.commands.ModelCheckCommand;
import de.prob2.jupyter.commands.PrefCommand;
import de.prob2.jupyter.commands.PrettyPrintCommand;
import de.prob2.jupyter.commands.RenderCommand;
import de.prob2.jupyter.commands.ShowCommand;
import de.prob2.jupyter.commands.SolveCommand;
import de.prob2.jupyter.commands.StatsCommand;
import de.prob2.jupyter.commands.TableCommand;
import de.prob2.jupyter.commands.TimeCommand;
import de.prob2.jupyter.commands.TraceCommand;
import de.prob2.jupyter.commands.TypeCommand;
import de.prob2.jupyter.commands.UnletCommand;
import de.prob2.jupyter.commands.VersionCommand;

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
	/**
	 * Inner class for safe lazy loading of the build info.
	 */
	private static final class BuildInfo {
		static final Properties buildInfo;
		static {
			buildInfo = new Properties();
			try (final Reader reader = new InputStreamReader(
				ProBKernel.class.getResourceAsStream("/de/prob2/jupyter/build.properties"),
				StandardCharsets.UTF_8
			)) {
				buildInfo.load(reader);
			} catch (final IOException e) {
				throw new AssertionError("Failed to load build info", e);
			}
		}
	}
	
	private static final class SplitCommandCall {
		private final @NotNull PositionedString name;
		private final @NotNull PositionedString argString;
		
		private SplitCommandCall(@NotNull final PositionedString name, @NotNull final PositionedString argString) {
			this.name = name;
			this.argString = argString;
		}
		
		private @NotNull PositionedString getName() {
			return this.name;
		}
		
		private @NotNull PositionedString getArgString() {
			return this.argString;
		}
		
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
				.add("name", this.getName())
				.add("argString", this.getArgString())
				.toString();
		}
	}
	
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(ProBKernel.class);
	
	private static final String DEFAULT_MACHINE_NAME = "(empty default machine)";
	private static final String DEFAULT_MACHINE_SOURCE_CODE = "MACHINE repl END";
	public static final String LOAD_CELL_MACHINE_NAME = "(machine from Jupyter cell)";
	private static final @NotNull Pattern COMMAND_PATTERN = Pattern.compile("\\s*(\\:[^\\s]*)(?:\\h*(.*))?", Pattern.DOTALL);
	private static final @NotNull Pattern MACHINE_CODE_PATTERN = Pattern.compile("MACHINE\\W.*", Pattern.DOTALL);
	private static final @NotNull Pattern BSYMB_COMMAND_PATTERN = Pattern.compile("\\\\([a-z]+)");
	private static final @NotNull Pattern LATEX_FORMULA_PATTERN = Pattern.compile("(\\$\\$?)([^\\$]+)\\1");
	
	public static final @NotNull EvalOptions EVAL_OPTIONS = EvalOptions.DEFAULT
		.withProvideStoredLetValues(true)
		.withEvalExpand(EvalExpandMode.EFFICIENT);
	public static final @NotNull EvalOptions TRUNCATE_EVAL_OPTIONS = EVAL_OPTIONS
		.withEvalExpand(EvalExpandMode.NEVER)
		.withExpand(FormulaExpand.TRUNCATE);
	
	private static final @NotNull Collection<@NotNull Class<? extends Command>> COMMAND_CLASSES = Collections.unmodifiableList(Arrays.asList(
		AssertCommand.class,
		BrowseCommand.class,
		BsymbCommand.class,
		CheckCommand.class,
		ConstantsCommand.class,
		DotCommand.class,
		EvalCommand.class,
		ExecCommand.class,
		FindCommand.class,
		GotoCommand.class,
		GroovyCommand.class,
		HelpCommand.class,
		InitialiseCommand.class,
		LanguageCommand.class,
		LetCommand.class,
		LoadCellCommand.class,
		LoadFileCommand.class,
		ModelCheckCommand.class,
		PrefCommand.class,
		PrettyPrintCommand.class,
		RenderCommand.class,
		ShowCommand.class,
		SolveCommand.class,
		StatsCommand.class,
		TableCommand.class,
		TimeCommand.class,
		TraceCommand.class,
		TypeCommand.class,
		UnletCommand.class,
		VersionCommand.class
	));
	
	private static final @NotNull Map<@NotNull String, @NotNull String> BSYMB_COMMAND_DEFINITIONS;
	static {
		final Map<String, String> map = new HashMap<>();
		map.put("bfalse", "\\def\\bfalse{\\mathord\\bot}");
		map.put("btrue", "\\def\\btrue{\\mathord\\top}");
		map.put("limp", "\\def\\limp{\\mathbin\\Rightarrow}");
		map.put("leqv", "\\def\\leqv{\\mathbin\\Leftrightarrow}");
		map.put("qdot", "\\def\\qdot{\\mathord{\\mkern1mu\\cdot\\mkern1mu}}");
		map.put("defi", "\\def\\defi{\\mathrel{≙}}");
		map.put("pow", "\\def\\pow{\\mathop{\\mathbb P\\hbox{}}\\nolimits}");
		map.put("pown", "\\def\\pown{\\mathop{\\mathbb P_1}\\nolimits}");
		map.put("cprod", "\\def\\cprod{\\mathbin\\times}");
		map.put("bunion", "\\def\\bunion{\\mathbin{\\mkern1mu\\cup\\mkern1mu}}");
		map.put("binter", "\\def\\binter{\\mathbin{\\mkern1mu\\cap\\mkern1mu}}");
		map.put("union", "\\def\\union{\\mathop{\\mathrm{union}}\\nolimits}");
		map.put("inter", "\\def\\inter{\\mathop{\\mathrm{inter}}\\nolimits}");
		map.put("Union", "\\def\\Union{\\bigcup\\nolimits}");
		map.put("Inter", "\\def\\Inter{\\bigcap\\nolimits}");
		map.put("emptyset", "\\def\\emptyset{\\mathord\\varnothing}");
		map.put("rel", "\\def\\rel{\\mathbin{<\\mkern-10mu-\\mkern-10mu>}}");
		map.put("trel", "\\def\\trel{\\mathbin{<\\mkern-6mu<\\mkern-10mu-\\mkern-10mu>}}");
		map.put("srel", "\\def\\srel{\\mathbin{<\\mkern-10mu-\\mkern-10mu>\\mkern-6mu>}}");
		map.put("strel", "\\def\\strel{\\mathbin{<\\mkern-6mu<\\mkern-10mu-\\mkern-10mu>\\mkern-6mu>}}");
		map.put("dom", "\\def\\dom{\\mathop{\\mathrm{dom}}\\nolimits}");
		map.put("ran", "\\def\\ran{\\mathop{\\mathrm{ran}}\\nolimits}");
		map.put("fcomp", "\\def\\fcomp{\\mathbin;}");
		map.put("bcomp", "\\def\\bcomp{\\circ}");
		map.put("id", "\\def\\id{\\mathop{\\mathrm{id}}\\nolimits}");
		map.put("domres", "\\def\\domres{\\mathbin◁}");
		map.put("domsub", "\\def\\domsub{\\mathbin⩤}");
		map.put("ranres", "\\def\\ranres{\\mathbin▷}");
		map.put("ransub", "\\def\\ransub{\\mathbin⩥}");
		map.put("ovl", "\\def\\ovl{\\mathbin{<\\mkern-11mu+}}");
		map.put("dprod", "\\def\\dprod{\\mathbin\\otimes}");
		map.put("prjone", "\\def\\prjone{\\mathop{\\mathrm{prj}_1}\\nolimits}");
		map.put("prjtwo", "\\def\\prjtwo{\\mathop{\\mathrm{prj}_2}\\nolimits}");
		map.put("pprod", "\\def\\pprod{\\mathbin\\mid}");
		map.put("pfun", "\\def\\pfun{\\mathbin↦}");
		map.put("tfun", "\\def\\tfun{\\mathbin→}");
		map.put("pinj", "\\def\\pinj{\\mathbin⤔}");
		map.put("tinj", "\\def\\tinj{\\mathbin↣}");
		map.put("psur", "\\def\\psur{\\mathbin⤅}");
		map.put("tsur", "\\def\\tsur{\\mathbin↠}");
		map.put("tbij", "\\def\\tbij{\\mathbin⤖}");
		map.put("nat", "\\def\\nat{\\mathord{\\mathbb N}}");
		map.put("natn", "\\def\\natn{\\mathord{\\mathbb N_1}}");
		map.put("intg", "\\def\\intg{\\mathord{\\mathbb Z}}");
		map.put("upto", "\\def\\upto{\\mathbin{.\\mkern1mu.}}");
		map.put("finite", "\\def\\finite{\\mathop{\\mathrm{finite}}\\nolimits}");
		map.put("card", "\\def\\card{\\mathop{\\mathrm{card}}\\nolimits}");
		map.put("upred", "\\def\\upred{\\mathop{\\mathrm{pred}}\\nolimits}");
		map.put("usucc", "\\def\\usucc{\\mathop{\\mathrm{succ}}\\nolimits}");
		map.put("expn", "\\def\\expn{\\mathbin{\\widehat{\\mkern1em}}}");
		map.put("Bool", "\\def\\Bool{\\mathord{\\mathrm{BOOL}}}");
		map.put("bool", "\\def\\bool{\\mathop{\\mathrm{bool}}\\nolimits}");
		map.put("bcmeq", "\\def\\bcmeq{\\mathrel{:\\mkern1mu=}}");
		map.put("bcmin", "\\def\\bcmin{\\mathrel{:\\mkern1mu\\in}}");
		map.put("bcmsuch", "\\def\\bcmsuch{\\mathrel{:\\mkern1mu\\mid}}");
		BSYMB_COMMAND_DEFINITIONS = Collections.unmodifiableMap(map);
	}
	
	private final @NotNull ClassicalBFactory classicalBFactory;
	private final @NotNull AnimationSelector animationSelector;
	private final @NotNull ReusableAnimator animator;
	
	private final @NotNull Map<@NotNull String, @NotNull Command> commands;
	private final @NotNull ExecutorService commandExecutor;
	private final @NotNull AtomicReference<@NotNull Future<@Nullable DisplayData>> currentCommandFuture;
	private final @NotNull Map<@NotNull String, @NotNull String> variables;
	
	private @NotNull Path currentMachineDirectory;
	private @Nullable Language currentFormulaLanguage;
	private @Nullable String currentCellSourceCode;
	
	@Inject
	private ProBKernel(final @NotNull Injector injector, final @NotNull ClassicalBFactory classicalBFactory, final @NotNull AnimationSelector animationSelector, final @NotNull ReusableAnimator animator) {
		super();
		
		this.setShouldReplaceStdStreams(false);
		
		this.classicalBFactory = classicalBFactory;
		this.animationSelector = animationSelector;
		this.animator = animator;
		
		this.commands = COMMAND_CLASSES.stream()
			.map(injector::getInstance)
			.collect(Collectors.toMap(Command::getName, cmd -> cmd));
		
		this.commandExecutor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "Command Executor"));
		this.currentCommandFuture = new AtomicReference<>(Futures.immediateCancelledFuture());
		this.variables = new HashMap<>();
		
		// These variables need to be initialized here,
		// but they will be overwritten immediately by the switchMachine call.
		this.currentMachineDirectory = Paths.get("");
		this.currentFormulaLanguage = null;
		this.currentCellSourceCode = null;
		this.switchMachine(Paths.get(""), null, this::loadDefaultMachine);
	}
	
	private static @NotNull Properties getBuildInfo() {
		return ProBKernel.BuildInfo.buildInfo;
	}
	
	public static @NotNull String getVersion() {
		return getBuildInfo().getProperty("version");
	}
	
	public static @NotNull String getCommit() {
		return getBuildInfo().getProperty("commit");
	}
	
	public @NotNull Map<@NotNull String, @NotNull Command> getCommands() {
		return Collections.unmodifiableMap(this.commands);
	}
	
	public @NotNull Map<@NotNull String, @NotNull String> getVariables() {
		return this.variables;
	}
	
	public @NotNull Path getCurrentMachineDirectory() {
		return this.currentMachineDirectory;
	}
	
	public void setCurrentMachineDirectory(final @NotNull Path currentMachineDirectory) {
		this.currentMachineDirectory = currentMachineDirectory;
	}
	
	public @Nullable Language getCurrentFormulaLanguage() {
		return this.currentFormulaLanguage;
	}
	
	public void setCurrentFormulaLanguage(final @Nullable Language currentFormulaLanguage) {
		this.currentFormulaLanguage = currentFormulaLanguage;
	}
	
	public void unloadMachine() {
		final Trace oldTrace = this.animationSelector.getCurrentTrace();
		if (oldTrace != null) {
			assert oldTrace.getStateSpace() == this.animator.getCurrentStateSpace();
			this.animationSelector.changeCurrentAnimation(null);
			oldTrace.getStateSpace().kill();
		}
		this.setCurrentMachineDirectory(Paths.get(""));
		this.getVariables().clear();
	}
	
	private @NotNull Trace loadDefaultMachine(final @NotNull StateSpace stateSpace) {
		this.classicalBFactory.create(DEFAULT_MACHINE_NAME, DEFAULT_MACHINE_SOURCE_CODE).loadIntoStateSpace(stateSpace);
		return new Trace(stateSpace);
	}
	
	public void switchMachine(final @NotNull Path machineDirectory, final @Nullable String cellSourceCode, final @NotNull Function<@NotNull StateSpace, @NotNull Trace> newTraceCreator) {
		this.unloadMachine();
		this.setCurrentFormulaLanguage(null);
		this.currentCellSourceCode = cellSourceCode;
		final StateSpace newStateSpace = this.animator.createStateSpace();
		try {
			this.animationSelector.changeCurrentAnimation(newTraceCreator.apply(newStateSpace));
		} catch (final RuntimeException e) {
			newStateSpace.kill();
			final StateSpace defaultStateSpace = this.animator.createStateSpace();
			this.animationSelector.changeCurrentAnimation(this.loadDefaultMachine(defaultStateSpace));
			throw e;
		}
		this.setCurrentMachineDirectory(machineDirectory);
	}
	
	/**
	 * Return whether user-entered formulas are currently being parsed using Event-B syntax.
	 * This is true if the user has explicitly enabled Event-B mode,
	 * or when the default mode is used while an Event-B model is loaded.
	 * 
	 * @return whether formulas are currently parsed as Event-B
	 */
	public boolean isEventBMode() {
		return this.getCurrentFormulaLanguage() == Language.EVENT_B || this.animationSelector.getCurrentTrace().getModel().getLanguage() == Language.EVENT_B;
	}
	
	/**
	 * Parse the given formula code into an {@link IEvalElement}.
	 * The language used for parsing depends on the current formula language (see {@link #getCurrentFormulaLanguage()}.
	 * Unlike {@link #parseFormula(String, FormulaExpand)},
	 * this method does not automatically insert local variables into the code.
	 * 
	 * @param code the formula code
	 * @param expand the expansion mode to use when evaluating the formula
	 * @return the parsed formula
	 */
	public IEvalElement parseFormulaWithoutLetVariables(final String code, final FormulaExpand expand) {
		if (this.getCurrentFormulaLanguage() == null) {
			return this.animationSelector.getCurrentTrace().getModel().parseFormula(code, expand);
		} else {
			switch (this.getCurrentFormulaLanguage()) {
				case CLASSICAL_B:
					return new ClassicalB(code, expand);
				
				case EVENT_B:
					return new EventB(code, expand);
				
				default:
					throw new AssertionError("Unhandled formula parse mode: " + this.getCurrentFormulaLanguage());
			}
		}
	}
	
	/**
	 * Parse the given formula code into an {@link IEvalElement}.
	 * The language used for parsing depends on the current formula language (see {@link #getCurrentFormulaLanguage()}.
	 * Any currently defined local variables are automatically inserted before parsing.
	 * This can be avoided if necessary using {@link #parseFormulaWithoutLetVariables(String, FormulaExpand)}.
	 * 
	 * @param code the formula code
	 * @param expand the expansion mode to use when evaluating the formula
	 * @return the parsed formula
	 */
	public IEvalElement parseFormula(final String code, final FormulaExpand expand) {
		if (this.getVariables().isEmpty()) {
			// Shortcut: if no local variables are defined,
			// the code doesn't need to be changed at all.
			return this.parseFormulaWithoutLetVariables(code, expand);
		} else if (this.isEventBMode()) {
			// Implementing local variables in Event-B mode is not straightforward,
			// because Rodin does not support LET expressions/predicates
			// like ProB does for classical B.
			// For predicates,
			// we can use an existential quantifier in place of LET.
			// For expressions this is not possible,
			// so as a workaround we construct a predicate with a new free variable equal to the expression.
			// This helper predicate is unpacked again after evaluation.
			
			// We don't know yet if the original formula is a predicate or an expression,
			// so try both and use the first one that doesn't result in a syntax error.
			final String predicateCode = CommandUtils.insertEventBPredicateLetVariables(code, this.getVariables());
			try {
				final EventB predicate = (EventB)this.parseFormulaWithoutLetVariables(predicateCode, FormulaExpand.TRUNCATE);
				// The EventB class parses formulas lazily.
				// Force parsing now so we can detect syntax errors.
				predicate.ensureParsed();
				return predicate;
			} catch (EvaluationException exc) {
				LOGGER.trace("Syntax error while inserting let variables around Event-B predicate - trying again as an expression", exc);
				final String expressionAsPredicateCode = CommandUtils.insertEventBExpressionLetVariables(code, this.getVariables());
				return this.parseFormulaWithoutLetVariables(expressionAsPredicateCode, FormulaExpand.TRUNCATE);
			}
		} else {
			// Assume that we are using classical B syntax.
			// This is the case for most other supported languages
			// (the main exception is CSP).
			return this.parseFormulaWithoutLetVariables(CommandUtils.insertClassicalBLetVariables(code, this.getVariables()), expand);
		}
	}
	
	private @NotNull Transition singleTransitionFromPredicate(final @Nullable String predicate, final @NotNull Trace trace, final @NotNull String translatedOpName) {
		final IEvalElement parsedPredicate;
		if (predicate == null) {
			parsedPredicate = this.parseFormulaWithoutLetVariables("1=1", FormulaExpand.EXPAND);
		} else {
			parsedPredicate = this.parseFormula(predicate, FormulaExpand.EXPAND);
		}
		final List<Transition> ops = trace.getStateSpace().transitionFromPredicate(trace.getCurrentState(), translatedOpName, parsedPredicate, 1);
		assert !ops.isEmpty();
		return ops.get(0);
	}
	
	public @NotNull DisplayData executeOperation(final @NotNull String name, final @Nullable String predicate) {
		Trace trace = this.animationSelector.getCurrentTrace();
		final String translatedOpName = Transition.unprettifyName(name);
		final StringBuilder sb = new StringBuilder();
		
		Transition op;
		try {
			op = this.singleTransitionFromPredicate(predicate, trace, translatedOpName);
		} catch (ExecuteOperationException e) {
			boolean tryAgain = false;
			if (
				!trace.getCurrentState().isConstantsSetUp()
				&& !Arrays.asList(Transition.PARTIAL_SETUP_CONSTANTS_NAME, Transition.SETUP_CONSTANTS_NAME).contains(translatedOpName)
			) {
				LOGGER.debug("Trying to set up constants using arbitrary transition");
				final Set<Transition> setupConstantsTransitions = trace.getNextTransitions();
				if (setupConstantsTransitions.isEmpty()) {
					throw new UserErrorException("Machine constants are not set up and no valid constant values were found automatically - cannot execute operation " + name, e);
				}
				final Transition setupConstants = setupConstantsTransitions.iterator().next();
				trace = trace.add(setupConstants);
				trace.getStateSpace().evaluateTransitions(Collections.singleton(setupConstants), FormulaExpand.TRUNCATE);
				sb.append("Machine constants were not set up yet. Automatically set up constants using arbitrary transition: ");
				sb.append(setupConstants.getPrettyRep());
				sb.append('\n');
				tryAgain = true;
			}
			
			if (
				!trace.getCurrentState().isInitialised()
				&& !Arrays.asList(Transition.PARTIAL_SETUP_CONSTANTS_NAME, Transition.SETUP_CONSTANTS_NAME, Transition.INITIALISE_MACHINE_NAME).contains(translatedOpName)
			) {
				LOGGER.debug("Trying to initialize machine using arbitrary transition");
				final Set<Transition> initialisationTransitions = trace.getNextTransitions();
				if (initialisationTransitions.isEmpty()) {
					throw new UserErrorException("Machine is not initialised and no valid initialisation was found automatically - cannot execute operation " + name, e);
				}
				final Transition initialisation = initialisationTransitions.iterator().next();
				trace = trace.add(initialisation);
				trace.getStateSpace().evaluateTransitions(Collections.singleton(initialisation), FormulaExpand.TRUNCATE);
				sb.append("Machine was not initialised yet. Automatically initialised machine using arbitrary transition: ");
				sb.append(initialisation.getPrettyRep());
				sb.append('\n');
				tryAgain = true;
			}
			
			if (tryAgain) {
				op = this.singleTransitionFromPredicate(predicate, trace, translatedOpName);
			} else {
				throw e;
			}
		}
		
		this.animationSelector.changeCurrentAnimation(trace.add(op));
		trace.getStateSpace().evaluateTransitions(Collections.singleton(op), FormulaExpand.TRUNCATE);
		sb.append("Executed operation: ");
		sb.append(op.getPrettyRep());
		return new DisplayData(sb.toString());
	}
	
	/**
	 * Post-process an evaluation result returned by ProB.
	 * Currently this only handles some special cases when local variables are defined,
	 * especially in Event-B mode.
	 * In all other cases,
	 * the result is returned unmodified.
	 * 
	 * @param aer the evaluation result to postprocess
	 * @return the processed evaluation result
	 */
	public @NotNull AbstractEvalResult postprocessEvalResult(final @NotNull AbstractEvalResult aer) {
		if (!this.getVariables().isEmpty() && aer instanceof EvalResult) {
			final EvalResult result = (EvalResult)aer;
			
			final Map<String, String> solutionValues = new HashMap<>(result.getSolutions());
			
			final String resultValue;
			if (this.isEventBMode() && result.getValue().equals(EvalResult.TRUE.getValue()) && result.getSolutions().containsKey(CommandUtils.JUPYTER_RESULT_VARIABLE_NAME)) {
				// Special case for expressions rewritten to predicates by CommandUtils.insertEventBExpressionLetVariables:
				// extract the special result variable and use it in place of the predicate result (TRUE).
				resultValue = solutionValues.remove(CommandUtils.JUPYTER_RESULT_VARIABLE_NAME);
			} else {
				resultValue = result.getValue();
			}
			
			// Special case for predicates or expressions
			// that had local variables inserted using an existential quantification
			// (by insertEventBPredicateLetVariables or insertEventBExpressionLetVariables).
			// ProB sometimes treats variables in a top-level existential quantification
			// as if they were free variables
			// and returns their values as part of the solution.
			// We don't want this for local variables,
			// so manually remove them from the solution again.
			solutionValues.keySet().removeAll(this.getVariables().keySet());
			
			return new EvalResult(resultValue, solutionValues);
		}
		
		return aer;
	}
	
	@Override
	public @NotNull String getBanner() {
		final String version = String.format("Version %s (%s)", getVersion(), getCommit());
		return "ProB Interactive Expression and Predicate Evaluator (on Jupyter)\n" + version + "\nType \":help\" for a list of commands, \":version\" for full version information.";
	}
	
	@Override
	public @NotNull List<LanguageInfo.@NotNull Help> getHelpLinks() {
		return Collections.singletonList(new LanguageInfo.Help("ProB User Manual", "https://prob.hhu.de/w/index.php/User_Manual"));
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
	
	private static @NotNull String addAllBsymbDefinitions() {
		final StringBuilder defs = new StringBuilder("$");
		BSYMB_COMMAND_DEFINITIONS.forEach((k, v) -> {
			defs.append(v);
		});
		defs.append("\\text{All bsymb.sty definitions have been loaded.}$");
		return defs.toString();
	}
	
	private @Nullable DisplayData executeCommand(final @NotNull PositionedString name, final @NotNull PositionedString argString) {
		final Command command = this.getCommands().get(name.getValue());
		if (command == null) {
			throw new NoSuchCommandException(name.getValue());
		}
		final DisplayData result;
		try {
			result = command.run(CommandUtils.parseArgs(command.getParameters(), argString));
		} catch (final UserErrorException e) {
			throw new CommandExecutionException(name.getValue(), e);
		}
		
		if (result != null && result.hasDataForType(MIMEType.TEXT_MARKDOWN)) {
			final String markdown = (String)result.getData(MIMEType.TEXT_MARKDOWN);
			if (command instanceof BsymbCommand) {
				result.putMarkdown(addAllBsymbDefinitions());
			} else {
				// Add definitions for any used bsymb LaTeX commands to Markdown output.
				result.putMarkdown(addBsymbDefinitions(markdown));
			}
		}
		
		return result;
	}
	
	private static boolean isMachineCode(final @NotNull String code) {
		return MACHINE_CODE_PATTERN.matcher(code).matches();
	}
	
	/**
	 * Preprocess the given input by ensuring that it starts with a command name.
	 * If a command name is already present,
	 * the input is returned unchanged.
	 * Otherwise,
	 * an appropriate command is added based on the type of input.
	 * 
	 * @param code the input code to preprocess
	 * @return the input with a command name prefixed if necessary
	 */
	private static @NotNull PositionedString preprocessInput(final @NotNull PositionedString code) {
		final Matcher commandMatcher = COMMAND_PATTERN.matcher(code.getValue());
		final String prefix;
		if (commandMatcher.matches()) {
			// The input already includes a command, so no prefix needs to be added.
			prefix = "";
		} else if (isMachineCode(code.getValue())) {
			// The input appears to be a machine, add a command to load it.
			prefix = "::load\n";
		} else {
			// By default, assume that the input is an expression that should be evaluated.
			prefix = ":eval ";
		}
		// Add the prefix and adjust the start position.
		// This means that the characters from the prefix (if any) will have negative positions,
		// but the characters from the real source code will have the same positions as before.
		return new PositionedString(prefix + code.getValue(), code.getStartPosition() - prefix.length());
	}
	
	/**
	 * Split the given input into a command name and argument string.
	 * The input must include a command name,
	 * which should be ensured by passing it through {@link #preprocessInput(PositionedString)} first.
	 * 
	 * @param code the input, which must contain a command name
	 * @return the input split into a command name and argument string
	 */
	private static @NotNull SplitCommandCall splitCommand(final @NotNull PositionedString code) {
		final Matcher commandMatcher = COMMAND_PATTERN.matcher(code.getValue());
		if (!commandMatcher.matches()) {
			throw new AssertionError("Preprocessed input does not include a command - this should not happen");
		}
		final PositionedString name = code.substring(commandMatcher.start(1), commandMatcher.end(1));
		final PositionedString argString;
		if (commandMatcher.group(2) == null) {
			argString = code.substring(code.getValue().length());
		} else {
			argString = code.substring(commandMatcher.start(2), commandMatcher.end(2));
		}
		return new SplitCommandCall(name, argString);
	}
	
	private @Nullable DisplayData evalInternal(final @NotNull PositionedString code) {
		final SplitCommandCall split = splitCommand(preprocessInput(code));
		return this.executeCommand(split.getName(), split.getArgString());
	}
	
	public @Nullable DisplayData evalOnCurrentThread(final @NotNull String expr) {
		return this.evalInternal(new PositionedString(expr, 0));
	}
	
	@Override
	public @Nullable DisplayData eval(final String expr) {
		assert expr != null;
		
		final Future<DisplayData> oldFuture = this.currentCommandFuture.get();
		assert oldFuture.isDone() || oldFuture.isCancelled();
		
		final Future<DisplayData> future = this.commandExecutor.submit(() -> this.evalOnCurrentThread(expr));
		this.currentCommandFuture.set(future);
		try {
			return future.get();
		} catch (final InterruptedException e) {
			LOGGER.error("Main kernel thread (event loop thread) interrupted while executing command", e);
			Thread.currentThread().interrupt();
			return null;
		} catch (final CancellationException e) {
			LOGGER.info("Command execution cancelled by user", e);
			throw new UserErrorException("Interrupted by user");
		} catch (final ExecutionException e) {
			LOGGER.warn("Exception while executing command", e);
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException)e.getCause();
			} else {
				throw new IllegalStateException("Checked exception thrown by command, this should never happen", e);
			}
		}
	}
	
	private static @Nullable DisplayData inspectCommandArguments(final @NotNull Command command, final @NotNull PositionedString argString, final int at) {
		final SplitResult split = CommandUtils.splitArgs(command.getParameters(), argString, at);
		if (split.getParameterAtPosition().isPresent()) {
			final Optional<Inspector> inspector = command.getParameterInspectors().getInspectorForParameter(split.getParameterAtPosition().get());
			if (inspector.isPresent()) {
				final List<PositionedString> argsAtPosition = split.getArguments().get(split.getParameterAtPosition().get());
				assert !argsAtPosition.isEmpty();
				final PositionedString lastArgument = argsAtPosition.get(argsAtPosition.size() - 1);
				return inspector.get().inspect(lastArgument.getValue(), at - lastArgument.getStartPosition());
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	private @Nullable DisplayData inspectInternal(final @NotNull PositionedString code, final int at) {
		final SplitCommandCall split = splitCommand(preprocessInput(code));
		if (this.getCommands().containsKey(split.getName().getValue())) {
			final Command command = this.getCommands().get(split.getName().getValue());
			if (at <= split.getName().getEndPosition()) {
				// The cursor is somewhere in the command name, show help text for the command.
				return command.renderHelp();
			} else {
				// The cursor is somewhere in the command arguments, ask the command to inspect.
				return inspectCommandArguments(command, split.getArgString(), at);
			}
		} else {
			// Invalid command, can't inspect.
			return null;
		}
	}
	
	@Override
	public @Nullable DisplayData inspect(final @NotNull String code, final int at, final boolean extraDetail) {
		// Note: We ignore the extraDetail parameter, because in practice it is always false. This is because the inspect_request messages sent by Jupyter Notebook always have their detail_level set to 0.
		return this.inspectInternal(new PositionedString(code, 0), at);
	}
	
	private static @NotNull ReplacementOptions offsetReplacementOptions(final @NotNull ReplacementOptions replacements, final int offset) {
		return new ReplacementOptions(
			replacements.getReplacements(),
			replacements.getSourceStart() + offset,
			replacements.getSourceEnd() + offset
		);
	}
	
	private static @Nullable ReplacementOptions completeCommandArguments(final @NotNull Command command, final @NotNull PositionedString argString, final int at) {
		final SplitResult split = CommandUtils.splitArgs(command.getParameters(), argString, at);
		
		final Parameter<?> parameterToComplete;
		final PositionedString argumentToComplete;
		if (split.getParameterAtPosition().isPresent()) {
			// If any arguments were split,
			// use the last parameter for completion.
			parameterToComplete = split.getParameterAtPosition().get();
			final List<PositionedString> argsAtPosition = split.getArguments().get(parameterToComplete);
			assert !argsAtPosition.isEmpty();
			argumentToComplete = argsAtPosition.get(argsAtPosition.size() - 1);
		} else if (!command.getParameters().getPositionalParameters().isEmpty()) {
			// Special case: if the command has parameters,
			// but argument splitting stopped before any arguments were split,
			// perform completion on the first parameter and an empty string at the cursor.
			parameterToComplete = command.getParameters().getPositionalParameters().get(0);
			argumentToComplete = new PositionedString("", at);
		} else {
			// Command has no parameters,
			// so completion in the arguments is not possible.
			return null;
		}
		
		final Optional<Completer> completer = command.getParameterCompleters().getCompleterForParameter(parameterToComplete);
		if (completer.isPresent()) {
			final ReplacementOptions replacements = completer.get().complete(argumentToComplete.getValue(), at - argumentToComplete.getStartPosition());
			return replacements == null ? null : offsetReplacementOptions(replacements, argumentToComplete.getStartPosition());
		} else {
			return null;
		}
	}
	
	private @Nullable ReplacementOptions completeInternal(final @NotNull PositionedString code, final int at) {
		final SplitCommandCall split = splitCommand(preprocessInput(code));
		if (at <= split.getName().getEndPosition()) {
			// The cursor is somewhere in the command name, provide command completions.
			final String prefix = split.getName().substring(0, at - split.getName().getStartPosition()).getValue();
			return new ReplacementOptions(
				this.getCommands().keySet().stream().filter(s -> s.startsWith(prefix)).sorted().collect(Collectors.toList()),
				split.getName().getStartPosition(),
				split.getName().getEndPosition()
			);
		} else {
			// The cursor is somewhere in the command arguments, ask the command to provide completions.
			if (this.getCommands().containsKey(split.getName().getValue())) {
				return completeCommandArguments(this.getCommands().get(split.getName().getValue()), split.getArgString(), at);
			} else {
				// Invalid command, can't provide any completions.
				return null;
			}
		}
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String code, final int at) {
		return this.completeInternal(new PositionedString(code, 0), at);
	}
	
	@Override
	public String isComplete(final String code) {
		final Matcher commandMatcher = COMMAND_PATTERN.matcher(code);
		if (commandMatcher.matches() && commandMatcher.group(1).startsWith("::")) {
			return code.endsWith("\n") ? IS_COMPLETE_MAYBE : "";
		} else {
			// TODO Support line continuation for normal commands
			return IS_COMPLETE_MAYBE;
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
		this.commandExecutor.shutdownNow();
	}
	
	@Override
	public void interrupt() {
		this.currentCommandFuture.get().cancel(true);
		this.animationSelector.getCurrentTrace().getStateSpace().sendInterrupt();
	}
	
	private @NotNull Optional<List<@NotNull String>> sourceLinesForFile(final @NotNull Path filePath, final @Nullable List<@NotNull String> contextSourceLines) throws IOException {
		if (filePath.toString().isEmpty()) {
			return Optional.ofNullable(contextSourceLines);
		} else {
			String fileName = filePath.getFileName().toString();
			if (fileName.endsWith(".mch")) {
				fileName = fileName.substring(0, fileName.length() - 4);
			}
			if (fileName.equals(DEFAULT_MACHINE_NAME)) {
				return Optional.of(Arrays.asList(DEFAULT_MACHINE_SOURCE_CODE.split("\n")));
			} else if (fileName.equals(LOAD_CELL_MACHINE_NAME)) {
				if (currentCellSourceCode == null) {
					return Optional.empty();
				} else {
					return Optional.of(Arrays.asList(this.currentCellSourceCode.split("\n")));
				}
			} else {
				return Optional.of(Files.readAllLines(filePath));
			}
		}
	}
	
	private @NotNull List<@NotNull String> formatErrorSource(final @Nullable List<@NotNull String> contextSourceLines, final @NotNull ErrorItem.Location location) {
		final Optional<List<String>> sourceLinesOptional;
		try {
			sourceLinesOptional = this.sourceLinesForFile(Paths.get(location.getFilename()), contextSourceLines);
		} catch (final IOException e) {
			LOGGER.error("Failed to read source file contents while highlighting errors", e);
			return Collections.singletonList(
				this.errorStyler.primary("// Failed to read source file contents: ")
				+ this.errorStyler.secondary(e.toString())
			);
		}
		if (!sourceLinesOptional.isPresent()) {
			return Collections.singletonList(this.errorStyler.primary("// Source code not known"));
		}
		final List<String> sourceLines = sourceLinesOptional.get();
		
		final List<String> out = new ArrayList<>();
		if (location.getStartLine() < 1 || location.getStartLine() > sourceLines.size()) {
			out.add(this.errorStyler.secondary(String.format("Error start line %d out of bounds (1..%d)", location.getStartLine(), sourceLines.size())));
			return out;
		}
		if (location.getEndLine() < 1 || location.getEndLine() > sourceLines.size()) {
			out.add(this.errorStyler.secondary(String.format("Error end line %d out of bounds (1..%d)", location.getEndLine(), sourceLines.size())));
			return out;
		}
		final int startLineIndex = location.getStartLine() - 1;
		final int endLineIndex = location.getEndLine() - 1;
		final List<String> errorLines = sourceLines.subList(startLineIndex, endLineIndex + 1);
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
		
		if (startLineIndex > 0) {
			out.add(this.errorStyler.primary(sourceLines.get(startLineIndex - 1)));
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
		
		if (endLineIndex < sourceLines.size() - 1) {
			out.add(this.errorStyler.primary(sourceLines.get(endLineIndex + 1)));
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
				final List<String> contextSourceLines;
				final ProBError proBError;
				if (e instanceof WithSourceCodeException) {
					contextSourceLines = Arrays.asList(((WithSourceCodeException)e).getSourceCode().split("\n"));
					proBError = ((WithSourceCodeException)e).getCause();
				} else {
					contextSourceLines = null;
					proBError = (ProBError)e;
				}
				final List<String> out = new ArrayList<>(Arrays.asList((
					this.errorStyler.primary("Error from ProB: ")
					+ this.errorStyler.secondary(String.valueOf(proBError.getOriginalMessage()))
				).split("\n")));
				if (proBError.getErrors().size() > 1) {
					out.addAll(this.errorStyler.primaryLines(proBError.getErrors().size() + " errors:\n"));
				}
				for (final ErrorItem error : proBError.getErrors()) {
					out.addAll(this.errorStyler.secondaryLines(error.toString()));
					for (final ErrorItem.Location location : error.getLocations()) {
						out.addAll(formatErrorSource(contextSourceLines, location));
					}
				}
				return out;
			} else {
				return this.errorStyler.secondaryLines(e.toString());
			}
		} catch (final RuntimeException e2) {
			LOGGER.error("Exception in error formatting", e2);
			throw e2;
		}
	}
}
