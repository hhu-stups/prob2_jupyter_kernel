package de.prob2.jupyter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

import io.github.spencerpark.jupyter.channels.JupyterConnection;
import io.github.spencerpark.jupyter.kernel.KernelConnectionProperties;

import org.jetbrains.annotations.Nullable;

public final class Main {
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
				throw die(1);
			}
			return Paths.get(cs.getLocation().toURI());
		} catch (final RuntimeException | URISyntaxException e) {
			System.err.println("Unable to determine location of kernel jar file");
			e.printStackTrace();
			throw die(1, e);
		}
	}
	
	private static Path getDestPath(final Path jarPath) {
		return Paths.get(de.prob.Main.getProBDirectory(), "jupyter", jarPath.getFileName().toString());
	}
	
	private static void copyJar(final Path jarPath, final Path destPath) {
		System.out.println("Installing kernel jar file...");
		System.out.println("Path to kernel jar file: " + jarPath);
		System.out.println("Kernel jar will be copied to: " + destPath);
		try {
			Files.createDirectories(destPath.getParent());
		} catch (final IOException e) {
			System.err.println("Failed to create destination directory");
			e.printStackTrace();
			throw die(1, e);
		}
		try {
			Files.copy(jarPath, destPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (final IOException e) {
			System.err.println("Failed to copy kernel jar file");
			e.printStackTrace();
			throw die(1, e);
		}
		System.out.println("Kernel jar file installed");
	}
	
	private static void createKernelSpec(final Path jarPath, final Path kernelSpecDir) {
		System.out.println("Creating kernel spec...");
		System.out.println("Path to kernel jar file: " + jarPath);
		System.out.println("Kernel spec directory: " + kernelSpecDir);
		Stream.of("kernel.js", "logo-32x32.png", "logo-64x64.png").forEach(name -> {
			System.out.println("Extracting: " + name);
			try (final InputStream is = Main.class.getResourceAsStream("kernelspecfiles/" + name)) {
				Files.copy(is, kernelSpecDir.resolve(name));
			} catch (final IOException e) {
				System.err.println("Failed to extract kernel spec file: " + name);
				e.printStackTrace();
				throw die(1, e);
			}
		});
		
		final String probHome = System.getProperty("prob.home");
		final String probHomeDef;
		if (probHome != null) {
			System.out.println("prob.home is set, adding a corresponding prob.home defintion to kernel.json: " + probHome);
			probHomeDef = String.format("\n\t\t\"-Dprob.home=%s\",", probHome);
		} else {
			System.out.println("prob.home is not set, not adding a prob.home definition to kernel.json");
			probHomeDef = "";
		}
		
		System.out.println("Creating kernel.json");
		try (
			final InputStream is = Main.class.getResourceAsStream("kernelspecfiles/kernel.json.template");
			final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
			final BufferedReader br = new BufferedReader(isr);
		) {
			final String kernelJsonText = String.format(br.lines().collect(Collectors.joining("\n")), probHomeDef, jarPath);
			Files.write(kernelSpecDir.resolve("kernel.json"), Arrays.asList(kernelJsonText.split("\n")));
		} catch (final IOException e) {
			System.err.println("Failed to create kernel.json");
			e.printStackTrace();
			throw die(1, e);
		}
		System.out.println("Kernel spec created");
	}
	
	private static void installKernelSpec(final Path pythonInterpreter, final Path kernelSpecDir) {
		System.out.println("Installing kernel spec...");
		System.out.println("Python interpreter: " + pythonInterpreter);
		System.out.println("Kernel spec directory: " + kernelSpecDir);
		final ProcessBuilder pb = new ProcessBuilder(pythonInterpreter.toString(), "-m", "jupyter", "kernelspec", "install", "--sys-prefix", "--name=prob2", kernelSpecDir.toString());
		pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
		pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
		pb.redirectError(ProcessBuilder.Redirect.INHERIT);
		final Process pythonProcess;
		try {
			pythonProcess = pb.start();
		} catch (final IOException e) {
			System.err.println("Failed to install kernel spec");
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
			throw die(1);
		}
		System.out.println("Kernel spec installed");
	}
	
	private static void install(final Path pythonInterpreter) {
		final Path jarPath = getJarPath();
		final Path destPath = getDestPath(jarPath);
		copyJar(jarPath, destPath);
		try {
			final Path kernelSpecDir = Files.createTempDirectory("prob2kernelspec");
			createKernelSpec(destPath, kernelSpecDir);
			installKernelSpec(pythonInterpreter, kernelSpecDir);
			try (final Stream<Path> contents = Files.list(kernelSpecDir)) {
				contents.forEach(path -> {
					try {
						Files.delete(path);
					} catch (final IOException e) {
						throw new UncheckedIOException(e);
					}
				});
			}
			Files.delete(kernelSpecDir);
		} catch (final IOException | UncheckedIOException e) {
			System.err.println("Failed to create kernel spec");
			e.printStackTrace();
			throw die(1, e);
		}
		System.out.println("The ProB 2 Jupyter kernel has been installed.");
		System.out.println("To use it, start Jupyter Notebook and select \"ProB 2\" when creating a new notebook.");
		System.out.println("This jar file can be safely deleted after installation.");
	}
	
	private static void startKernel(final Path connectionFile) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
		final String contents = String.join("\n", Files.readAllLines(connectionFile, StandardCharsets.UTF_8));
		final JupyterConnection conn = new JupyterConnection(KernelConnectionProperties.parse(contents));
		
		System.setProperty("prob.stdlib", Paths.get(de.prob.Main.getProBDirectory(), "stdlib").toString());
		final Injector injector = Guice.createInjector(Stage.PRODUCTION, new ProBKernelModule());
		final ProBKernel kernel = injector.getInstance(ProBKernel.class);
		kernel.becomeHandlerForConnection(conn);
		
		conn.connect();
		conn.waitUntilClose();
	}
	
	public static void main(final String[] args) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
		if (args.length < 1 || args.length == 1 && "--help".equals(args[0])) {
			System.err.println("Usage: java -jar prob2-jupyter-kernel-all.jar [--help | install PYTHONINTERPRETER | createKernelSpec KERNELSPECDIR | run CONNECTIONFILE]");
			System.err.println("--help: Prints this information.");
			System.err.println("install: Copies the kernel into the ProB home directory, and installs the kernel in the given Python interpreter.");
			System.err.println("\tIf you're not sure which Python interpreter is used by your Jupyter installation, open a Python notebook and run \"import sys; print(sys.executable)\".");
			System.err.println("createKernelSpec: Creates a Jupyter kernel spec for this jar file at the given location.");
			System.err.println("\tThis option is for advanced users or developers, who don't want the jar file to be copied, or who want to install the kernel spec manually.");
			System.err.println("run: Runs the kernel using the given connection file.");
			System.err.println("\tThis option is not meant to be used manually, it is used internally when Jupyter starts the kernel.");
			throw die(2);
		}
		switch (args[0]) {
			case "install":
				if (args.length != 2) {
					System.err.println("install expects exactly one argument, not " + (args.length-1));
					System.err.println("Use --help for more info.");
					throw die(2);
				}
				install(Paths.get(args[1]));
				break;
			
			case "createKernelSpec":
				if (args.length != 2) {
					System.err.println("createKernelSpec expects exactly one argument, not " + (args.length-1));
					System.err.println("Use --help for more info.");
					throw die(2);
				}
				createKernelSpec(getJarPath(), Paths.get(args[1]));
				break;
			
			case "run":
				if (args.length != 2) {
					System.err.println("run expects exactly one argument, not " + (args.length-1));
					System.err.println("Use --help for more info.");
					throw die(2);
				}
				startKernel(Paths.get(args[1]));
				break;
			
			default:
				System.err.println("Unknown subcommand: " + args[0]);
				System.err.println("Use --help for more info.");
				throw die(2);
		}
	}
}
