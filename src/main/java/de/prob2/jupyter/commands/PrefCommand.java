package de.prob2.jupyter.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import com.google.inject.Inject;

import de.prob.animator.command.ComposedCommand;
import de.prob.animator.command.GetCurrentPreferencesCommand;
import de.prob.animator.command.GetPreferenceCommand;
import de.prob.animator.command.SetPreferenceCommand;
import de.prob.statespace.AnimationSelector;

import de.prob2.jupyter.Command;
import de.prob2.jupyter.CommandUtils;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.PositionalParameter;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PrefCommand implements Command {
	private static final @NotNull PositionalParameter.OptionalMultiple PREFS_PARAM = new PositionalParameter.OptionalMultiple("prefs");
	
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
			final GetCurrentPreferencesCommand cmd = new GetCurrentPreferencesCommand();
			this.animationSelector.getCurrentTrace().getStateSpace().execute(cmd);
			// TreeMap is used to sort the preferences by name.
			new TreeMap<>(cmd.getPreferences()).forEach((k, v) -> {
				sb.append(k);
				sb.append(" = ");
				sb.append(v);
				sb.append('\n');
			});
		} else {
			final List<String> prefsSplit = args.get(PREFS_PARAM);
			if (prefsSplit.get(0).contains("=")) {
				final List<SetPreferenceCommand> cmds = new ArrayList<>();
				CommandUtils.parsePreferences(prefsSplit).forEach((pref, value) -> {
					cmds.add(new SetPreferenceCommand(pref, value));
					sb.append("Preference changed: ");
					sb.append(pref);
					sb.append(" = ");
					sb.append(value);
					sb.append('\n');
				});
				this.animationSelector.getCurrentTrace().getStateSpace().execute(new ComposedCommand(cmds));
			} else {
				final List<GetPreferenceCommand> cmds = new ArrayList<>();
				for (final String arg : prefsSplit) {
					if (arg.contains("=")) {
						throw new UserErrorException(String.format("Cannot view and change preferences in the same command (attempted to assign preference %s)", arg));
					}
					cmds.add(new GetPreferenceCommand(arg));
				}
				this.animationSelector.getCurrentTrace().getStateSpace().execute(new ComposedCommand(cmds));
				for (final GetPreferenceCommand cmd : cmds) {
					sb.append(cmd.getKey());
					sb.append(" = ");
					sb.append(cmd.getValue());
					sb.append('\n');
				}
			}
		}
		return new DisplayData(sb.toString());
	}
	
	@Override
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		return CommandUtils.inspectInPreferences(this.animationSelector.getCurrentTrace(), argString, at);
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		return CommandUtils.completeInPreferences(this.animationSelector.getCurrentTrace(), argString, at);
	}
}
