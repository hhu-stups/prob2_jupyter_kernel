package de.prob2.jupyter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

import io.github.spencerpark.jupyter.channels.JupyterConnection;
import io.github.spencerpark.jupyter.kernel.KernelConnectionProperties;

public final class Main {
	private Main() {
		super();
		
		throw new AssertionError();
	}
	
	public static void main(final String[] args) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
		if (args.length != 1) {
			System.err.printf("Expected exactly one argument, not %d%n", args.length);
			System.exit(2);
		}
		
		final Path connectionFile = Paths.get(args[0]);
		final String contents = String.join("\n", Files.readAllLines(connectionFile, StandardCharsets.UTF_8));
		final JupyterConnection conn = new JupyterConnection(KernelConnectionProperties.parse(contents));
		
		final Injector injector = Guice.createInjector(Stage.PRODUCTION, new ProBKernelModule());
		final ProBKernel kernel = injector.getInstance(ProBKernel.class);
		kernel.becomeHandlerForConnection(conn);
		
		conn.connect();
		conn.waitUntilClose();
	}
}
