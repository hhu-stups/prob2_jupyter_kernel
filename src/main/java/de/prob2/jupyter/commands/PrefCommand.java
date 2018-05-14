package de.prob2.jupyter.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import com.google.inject.Inject;

import de.prob.animator.command.ComposedCommand;
import de.prob.animator.command.GetCurrentPreferencesCommand;
import de.prob.animator.command.GetPreferenceCommand;
import de.prob.animator.command.SetPreferenceCommand;
import de.prob.statespace.AnimationSelector;

import de.prob2.jupyter.ProBKernel;

import io.github.spencerpark.jupyter.messages.DisplayData;

import org.jetbrains.annotations.NotNull;

public final class PrefCommand implements LineCommand {
	private final @NotNull AnimationSelector animationSelector;
	
	@Inject
	private PrefCommand(final @NotNull AnimationSelector animationSelector) {
		super();
		
		this.animationSelector = animationSelector;
	}
	
	@Override
	public @NotNull String getSyntax() {
		return ":pref [NAME ...]\n:pref NAME=VALUE [NAME=VALUE ...]";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "View or change the value of one or more preferences.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ProBKernel kernel, final @NotNull String name, final @NotNull String argString) {
		final List<String> args = CommandUtils.splitArgs(argString);
		final StringBuilder sb = new StringBuilder();
		if (args.isEmpty()) {
			final GetCurrentPreferencesCommand cmd = new GetCurrentPreferencesCommand();
			this.animationSelector.getCurrentTrace().getStateSpace().execute(cmd);
			// TreeMap is used to sort the preferences by name.
			new TreeMap<>(cmd.getPreferences()).forEach((k, v) -> {
				sb.append(k);
				sb.append(" = ");
				sb.append(v);
				sb.append('\n');
			});
		} else if (args.get(0).contains("=")) {
			final List<SetPreferenceCommand> cmds = new ArrayList<>();
			CommandUtils.parsePreferences(name, args).forEach((pref, value) -> {
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
			for (final String arg : args) {
				if (arg.contains("=")) {
					throw new CommandExecutionException(name, String.format("Cannot view and change preferences in the same command (attempted to assign preference %s)", arg));
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
		return new DisplayData(sb.toString());
	}
}
