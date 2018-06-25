package de.prob2.jupyter.commands;

import com.google.inject.Inject;

import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RenderCommand implements Command {
	@Inject
	private RenderCommand() {
		super();
	}
	
	@Override
	public @NotNull String getSyntax() {
		return "::render MIMETYPE\nCONTENT";
	}
	
	@Override
	public @NotNull String getShortHelp() {
		return "Render some content with the specified MIME type.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull String argString) {
		final String[] split = argString.split("\n", 2);
		if (split.length != 2) {
			throw new UserErrorException("Missing content (the content cannot be placed on the same line as the command)");
		}
		final String mimeType = split[0];
		final String code = split[1];
		if (mimeType.isEmpty()) {
			throw new UserErrorException("Missing MIME type");
		}
		final DisplayData data = new DisplayData(mimeType + ":\n" + code);
		data.putData(mimeType, code);
		return data;
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		return null;
	}
}
