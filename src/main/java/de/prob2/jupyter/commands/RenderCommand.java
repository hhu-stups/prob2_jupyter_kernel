package de.prob2.jupyter.commands;

import java.util.Collections;

import com.google.inject.Inject;

import de.prob2.jupyter.Command;
import de.prob2.jupyter.Parameters;
import de.prob2.jupyter.ParsedArguments;
import de.prob2.jupyter.PositionalParameter;
import de.prob2.jupyter.UserErrorException;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RenderCommand implements Command {
	private static final @NotNull PositionalParameter.RequiredRemainder MIME_TYPE_AND_CONTENT_PARAM = new PositionalParameter.RequiredRemainder("mimeTypeAndContent");
	
	@Inject
	private RenderCommand() {
		super();
	}
	
	@Override
	public @NotNull String getName() {
		return "::render";
	}
	
	@Override
	public @NotNull Parameters getParameters() {
		return new Parameters(Collections.singletonList(MIME_TYPE_AND_CONTENT_PARAM));
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
	public @NotNull String getHelpBody() {
		return "This command is intended for debugging the rendering behavior of Jupyter and the kernel, it should not be used in regular notebooks. To include text or images in a notebook, Markdown cells should be used instead.\n\n"
			+ "The command does not place any restrictions on the MIME type or content. A plain text fallback with the raw content is always included, and will be displayed if the frontend does not support the given MIME type.";
	}
	
	@Override
	public @NotNull DisplayData run(final @NotNull ParsedArguments args) {
		final String[] split = args.get(MIME_TYPE_AND_CONTENT_PARAM).split("\n", 2);
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
	public @Nullable DisplayData inspect(final @NotNull String argString, final int at) {
		return null;
	}
	
	@Override
	public @Nullable ReplacementOptions complete(final @NotNull String argString, final int at) {
		return null;
	}
}
