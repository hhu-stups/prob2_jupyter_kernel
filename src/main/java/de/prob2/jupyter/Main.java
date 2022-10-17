package de.prob2.jupyter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

import io.github.spencerpark.jupyter.channels.JupyterConnection;
import io.github.spencerpark.jupyter.kernel.KernelConnectionProperties;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Main {
	private static final @NotNull String GENERIC_PYTHON_ERROR_MESSAGE = "Try running the installation directly using Python, e. g.:\n"
		+ "$ python3 prob2-jupyter-kernel-all.jar install";
	
	private Main() {
		super();
		
		throw new AssertionError();
	}
	
	private static AssertionError die(final int status, final @Nullable Throwable cause) {
		System.exit(status);
		return new AssertionError("Unreachable", cause);
	}
	
	private static AssertionError die(final int status) {
		return die(status, null);
	}
	
	private static Path getJarPath() {
		try {
			final CodeSource cs = Main.class.getProtectionDomain().getCodeSource();
			if (cs == null) {
				System.err.println("Unable to determine location of kernel jar file (CodeSource is null)");
				System.err.println(GENERIC_PYTHON_ERROR_MESSAGE);
				throw die(1);
			}
			return Paths.get(cs.getLocation().toURI());
		} catch (final RuntimeException | URISyntaxException e) {
			System.err.println("Unable to determine location of kernel jar file");
			System.err.println(GENERIC_PYTHON_ERROR_MESSAGE);
			e.printStackTrace();
			throw die(1, e);
		}
	}
	
	/**
	 * Run the install script (__main__.py) bundled inside the jar file.
	 * This makes use of Python's zipapp support
	 * and the fact that jar files are valid zip files.
	 * 
	 * @param args arguments to pass to __main__.py
	 */
	private static void runInstallScript(final String[] args) {
		final List<String> command = new ArrayList<>();
		command.add("python3");
		command.add(getJarPath().toString());
		command.addAll(Arrays.asList(args));
		final ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
		pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
		pb.redirectError(ProcessBuilder.Redirect.INHERIT);
		final Process pythonProcess;
		try {
			pythonProcess = pb.start();
		} catch (final IOException e) {
			System.err.println("Failed to start Python to install the kernel");
			System.err.println(GENERIC_PYTHON_ERROR_MESSAGE);
			e.printStackTrace();
			throw die(1, e);
		}
		final int statusCode;
		try {
			statusCode = pythonProcess.waitFor();
		} catch (final InterruptedException e) {
			System.err.println("Interrupted");
			e.printStackTrace();
			Thread.currentThread().interrupt();
			throw die(1, e);
		}
		if (statusCode != 0) {
			System.err.println("Python exited with status " + statusCode);
			System.err.println(GENERIC_PYTHON_ERROR_MESSAGE);
			throw die(1);
		}
	}
	
	private static void startKernel(final Path connectionFile) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
		final String contents = String.join("\n", Files.readAllLines(connectionFile, StandardCharsets.UTF_8));
		final JupyterConnection conn = new JupyterConnection(KernelConnectionProperties.parse(contents));
		
		final Injector injector = Guice.createInjector(Stage.PRODUCTION, new ProBKernelModule());
		final ProBKernel kernel = injector.getInstance(ProBKernel.class);
		kernel.becomeHandlerForConnection(conn);
		
		conn.connect();
		conn.waitUntilClose();
	}
	
	public static void main(final String[] args) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
		if ("run".equals(args[0])) {
			if (args.length != 2) {
				System.err.println("run expects exactly one argument, not " + (args.length-1));
				System.err.println("Use --help for more info.");
				throw die(2);
			}
			startKernel(Paths.get(args[1]));
		} else {
			runInstallScript(args);
		}
	}
}
