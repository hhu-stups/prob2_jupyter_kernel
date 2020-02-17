package de.prob2.jupyter.commands;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Command {
	public abstract @NotNull String getName();
	
	public abstract @NotNull String getSyntax();
	
	public abstract @NotNull String getShortHelp();
	
	public abstract @NotNull String getHelpBody();
	
	public default @NotNull DisplayData renderHelp() {
		final StringBuilder sbPlain = new StringBuilder();
		final StringBuilder sbMarkdown = new StringBuilder();
		sbPlain.append(this.getSyntax());
		sbPlain.append('\n');
		sbMarkdown.append("```\n");
		sbMarkdown.append(this.getSyntax());
		sbMarkdown.append("\n```\n\n");
		
		final String shortHelp = this.getShortHelp();
		sbPlain.append(shortHelp);
		sbMarkdown.append(shortHelp);
		final String helpBody = this.getHelpBody();
		if (!helpBody.isEmpty()) {
			sbPlain.append("\n\n");
			sbPlain.append(helpBody);
			sbMarkdown.append("\n\n");
			sbMarkdown.append(helpBody);
		}
		final DisplayData result = new DisplayData(sbPlain.toString());
		result.putMarkdown(sbMarkdown.toString());
		return result;
	}
	
	public abstract @Nullable DisplayData run(final @NotNull String argString);
	
	public abstract @Nullable DisplayData inspect(final @NotNull String argString, final int at);
	
	public abstract @Nullable ReplacementOptions complete(final @NotNull String argString, final int at);
}
