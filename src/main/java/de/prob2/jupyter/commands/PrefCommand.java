package de.prob2.jupyter.commands;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.inject.Inject;

import de.prob.statespace.AnimationSelector;
import de.prob2.jupyter.Command;
import de.prob2.jupyter.CommandUtils;
import de.prob2.jupyter.Parameter;
import de.prob2.jupyter.ParameterCompleters;
import de.prob2.jupyter.ParameterInspectors;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class PrefCommand implements Command {
	private static final @NotNull Parameter.Multiple PREFS_PARAM = Parameter.optionalMultiple("prefs");
	
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private PrefCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getName() {
		return ":pref";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return new Parameters(Collections.singletonList(PREFS_PARAM));
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":pref [NAME ...]\n// or\n:pref NAME=VALUE [NAME=VALUE ...]";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "View or change the value of one or more preferences.";
	}
	
	@Override
	public @NotNull String getHelpBody() {
		return "In the first form, the values of the given preferences are displayed (or all preferences, if no preference names are given). In the second form, the values of the given preferences are changed. The two forms cannot be mixed; it is not possible to view and change preferences in a single command.\n\n"
			+ "Certain preference changes do not take full effect when performed on an already loaded machine. Such preferences must be set when the machine is loaded using the `::load` or `:load` command.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final StringBuilder sb = new StringBuilder();
		if (args.get(PREFS_PARAM).isEmpty()) {
			final Map<String, String> preferences = this.animationSelector.getCurrentTrace().getStateSpace().getCurrentPreferences();
			// TreeMap is used to sort the preferences by name.
			new TreeMap<>(preferences).forEach((k, v) -> {
				sb.append(k);
				sb.append(" = ");
				sb.append(v);
				sb.append('\n');
			});
		} else {
			final List<String> prefsSplit = args.get(PREFS_PARAM);
			if (prefsSplit.get(0).contains("=")) {
				final Map<String, String> newPreferenceValues = CommandUtils.parsePreferences(prefsSplit);
				this.animationSelector.getCurrentTrace().getStateSpace().changePreferences(newPreferenceValues);
				newPreferenceValues.forEach((pref, value) -> {
					sb.append("Preference changed: ");
					sb.append(pref);
					sb.append(" = ");
					sb.append(value);
					sb.append('\n');
				});
			} else {
				for (final String arg : prefsSplit) {
					if (arg.contains("=")) {
						throw new UserErrorException(String.format("Cannot view and change preferences in the same command (attempted to assign preference %s)", arg));
					}
					sb.append(arg);
					sb.append(" = ");
					sb.append(this.animationSelector.getCurrentTrace().getStateSpace().getCurrentPreference(arg));
					sb.append('\n');
				}
			}
		}
		return new DisplayData(sb.toString());
	}
	
	@Override
	public @NotNull ParameterInspectors getParameterInspectors() {
		return new ParameterInspectors(Collections.singletonMap(
			PREFS_PARAM, CommandUtils.preferenceInspector(this.animationSelector.getCurrentTrace())
		));
	}
	
	@Override
	public @NotNull ParameterCompleters getParameterCompleters() {
		return new ParameterCompleters(Collections.singletonMap(
			PREFS_PARAM, CommandUtils.preferenceCompleter(this.animationSelector.getCurrentTrace())
		));
	}
}
