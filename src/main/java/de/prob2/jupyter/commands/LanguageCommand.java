package de.prob2.jupyter.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.model.representation.AbstractModel;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Language;
import de.prob.statespace.Trace;
import de.prob2.jupyter.Command;
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
import org.jetbrains.annotations.Nullable;

public final class LanguageCommand implements Command {
	private static final @NotNull Parameter.OptionalSingle LANGUAGE_PARAM = Parameter.optional("language");
	
	private static final @NotNull Map<@NotNull String, @NotNull Language> LANGUAGE_BY_IDENTIFIER_MAP;
	static {
		final Map<String, Language> languageByIdentifierMap = new HashMap<>();
		languageByIdentifierMap.put("default", null);
		languageByIdentifierMap.put("classical_b", Language.CLASSICAL_B);
		languageByIdentifierMap.put("event_b", Language.EVENT_B);
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
	
	private static @NotNull String getLanguageName(final @NotNull Language language) {
		switch (language) {
			case CLASSICAL_B: return "classical B";
			case B_RULES: return "B rules";
			case EVENT_B: return "Event-B";
			case TLA: return "TLA+";
			case ALLOY: return "Alloy";
			case Z: return "Z";
			case CSP: return "CSP-M";
			case XTL: return "XTL Prolog";
			default: return language.toString();
		}
	}
	
	private static @NotNull String describeLanguage(final @Nullable Language language, final @NotNull AbstractModel model) {
		if (language == null) {
			Language lang = model.getLanguage();
			if (lang.getTranslatedTo() != null) {
				// For languages that ProB internally translates to B,
				// such as TLA and Alloy,
				// only the model itself is translated to B.
				// Formulas passed to model.parseFormula must be in classical B syntax,
				// not the original language of the model,
				// so we really only care about the translation target language.
				lang = lang.getTranslatedTo();
			}
			return getLanguageName(lang) + " (default for model)";
		} else {
			return getLanguageName(language) + " (forced)";
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
			final Language language = LANGUAGE_BY_IDENTIFIER_MAP.get(languageName.get());
			kernel.setCurrentFormulaLanguage(language);
			return new DisplayData("Changed language for user input to " + describeLanguage(language, trace.getModel()));
		} else {
			final Language language = kernel.getCurrentFormulaLanguage();
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
