package de.prob2.jupyter.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.FormalismType;
import de.prob.statespace.Trace;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.FormulaLanguage;
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

public final class LanguageCommand implements Command {
	private static final @NotNull Parameter.OptionalSingle LANGUAGE_PARAM = Parameter.optional("language");
	
	private static final @NotNull Map<@NotNull String, @NotNull FormulaLanguage> LANGUAGE_BY_IDENTIFIER_MAP;
	static {
		final Map<String, FormulaLanguage> languageByIdentifierMap = new HashMap<>();
		languageByIdentifierMap.put("default", FormulaLanguage.DEFAULT);
		languageByIdentifierMap.put("classical_b", FormulaLanguage.CLASSICAL_B);
		languageByIdentifierMap.put("event_b", FormulaLanguage.EVENT_B);
		LANGUAGE_BY_IDENTIFIER_MAP = Collections.unmodifiableMap(languageByIdentifierMap);
	}
	
	private final @NotNull Provider<@NotNull ProBKernel> kernelProvider;
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private LanguageCommand(final @NotNull Provider<@NotNull ProBKernel> kernelProvider, final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.kernelProvider = kernelProvider;
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":language";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return new Parameters(Collections.singletonList(LANGUAGE_PARAM));
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":language [LANGUAGE]";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Change the language used to parse formulas entered by the user.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "By default, formulas entered by the user are parsed using the language of the currently loaded model. Using this command, the language can be changed, so that for example formulas in Event-B syntax can be evaluated in the context of a classical B machine.\n\nSome features will not work correctly when a non-default language is set, such as `DEFINITIONS` from classical B machines.";
	}
	
	private static @NotNull String describeLanguage(final FormulaLanguage language, final AbstractModel model) {
		switch (language) {
			case DEFAULT:
				final FormalismType formalismType = model.getFormalismType();
				final String languageName;
				if (model instanceof EventBModel) {
					languageName = "Event-B";
				} else if (formalismType == FormalismType.CSP) {
					languageName = "CSP";
				} else if (formalismType == FormalismType.B || formalismType == FormalismType.Z) {
					// Languages that ProB internally translates to B, such as TLA and Alloy,
					// have their formalism type set to B.
					// For such models, only the model itself is translated to B.
					// Formulas passed to model.parseFormula must be in classical B syntax,
					// not the original language of the model.
					// Z models are also internally translated to classical B,
					// but currently have their own formalism type in ProB 2 (FIXME?).
					languageName = "classical B";
				} else {
					// Fallback for future formalism types
					languageName = "unknown (" + formalismType + ")";
				}
				return languageName + " (default for model)";
			
			case CLASSICAL_B:
				return "classical B (forced)";
			
			case EVENT_B:
				return "Event-B (forced)";
			
			default:
				throw new AssertionError("Unhandled formula language: " + language);
		}
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final Optional<String> languageName = args.get(LANGUAGE_PARAM);
		final ProBKernel kernel = this.kernelProvider.get();
		final Trace trace = this.animationSelector.getCurrentTrace();
		if (languageName.isPresent()) {
			if (!LANGUAGE_BY_IDENTIFIER_MAP.containsKey(languageName.get())) {
				throw new UserErrorException("Unknown language: " + languageName.get());
			}
			final FormulaLanguage language = LANGUAGE_BY_IDENTIFIER_MAP.get(languageName.get());
			kernel.setCurrentFormulaLanguage(language);
			return new DisplayData("Changed language for user input to " + describeLanguage(language, trace.getModel()));
		} else {
			final FormulaLanguage language = kernel.getCurrentFormulaLanguage();
			return new DisplayData("Current language for user input is " + describeLanguage(language, trace.getModel()));
		}
	}
	
	@Override
	public @NotNull ParameterInspectors getParameterInspectors() {
		return ParameterInspectors.NONE;
	}
	
	@Override
	public @NotNull ParameterCompleters getParameterCompleters() {
		return new ParameterCompleters(Collections.singletonMap(
			LANGUAGE_PARAM, (argString, at) -> {
				final String prefix = argString.substring(0, at);
				final List<String> replacements = LANGUAGE_BY_IDENTIFIER_MAP.keySet()
					.stream()
					.filter(s -> s.startsWith(prefix))
					.collect(Collectors.toList());
				return new ReplacementOptions(replacements, 0, argString.length());
			}
		));
	}
}
