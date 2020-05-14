package de.prob2.jupyter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
		
		final JsonArray kernelJsonArgv = new JsonArray();
		kernelJsonArgv.add("java");
		
		final String probHome = System.getProperty("prob.home");
		if (probHome != null) {
			System.out.println("prob.home is set, adding a corresponding prob.home defintion to kernel.json: " + probHome);
			kernelJsonArgv.add("-Dprob.home=" + probHome);
		} else {
			System.out.println("prob.home is not set, not adding a prob.home definition to kernel.json");
		}
		
		kernelJsonArgv.add("-jar");
		kernelJsonArgv.add(jarPath.toString());
		kernelJsonArgv.add("run");
		kernelJsonArgv.add("{connection_file}");
		
		final JsonObject kernelJsonData = new JsonObject();
		kernelJsonData.add("argv", kernelJsonArgv);
		kernelJsonData.addProperty("display_name", "ProB 2");
		kernelJsonData.addProperty("language", "prob");
		kernelJsonData.addProperty("interrupt_mode", "message");
		
		final Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.serializeNulls()
			.create();
		
		System.out.println("Creating kernel.json");
		try (final Writer writer = Files.newBufferedWriter(kernelSpecDir.resolve("kernel.json"))) {
			gson.toJson(kernelJsonData, writer);
		} catch (final IOException e) {
			System.err.println("Failed to create kernel.json");
			e.printStackTrace();
			throw die(1, e);
		}
		System.out.println("Kernel spec created");
	}
	
	private static void installKernelSpec(final String jupyterCommand, final boolean userInstall, final Path kernelSpecDir) {
		System.out.println("Installing kernel spec...");
		System.out.println("Jupyter command: " + jupyterCommand);
		System.out.println("Kernel spec directory: " + kernelSpecDir);
		final ProcessBuilder pb = new ProcessBuilder(jupyterCommand, "kernelspec", "install", userInstall ? "--user" : "--sys-prefix", "--name=prob2", kernelSpecDir.toString());
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
	
	private static void install(final String jupyterCommand, final boolean userInstall) {
		final Path jarPath = getJarPath();
		final Path destPath = getDestPath(jarPath);
		copyJar(jarPath, destPath);
		try {
			final Path kernelSpecDir = Files.createTempDirectory("prob2kernelspec");
			createKernelSpec(destPath, kernelSpecDir);
			installKernelSpec(jupyterCommand, userInstall, kernelSpecDir);
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
			System.err.println("Usage: java -jar prob2-jupyter-kernel-all.jar [--help | install [--user] [JUPYTER] | createKernelSpec KERNELSPECDIR | run CONNECTIONFILE]");
			System.err.println("--help: Prints this information.");
			System.err.println("install: Copies the kernel into the ProB home directory, and installs the kernel in Jupyter.");
			System.err.println("\tBy default the kernel spec is installed into the sys.prefix of Jupyter's Python installation. If the sys.prefix is not writable (for example when using the system Python installation instead of a virtual environment), the --user flag can be used to install the kernel only for the current user.");
			System.err.println("\tBy default the command \"jupyter\" is used to install the kernel. If the kernel should be installed in a different Jupyter installation, a different Jupyter command can be passed as an argument to the install command.");
			System.err.println("createKernelSpec: Creates a Jupyter kernel spec for this jar file at the given location.");
			System.err.println("\tThis option is for advanced users or developers, who don't want the jar file to be copied, or who want to install the kernel spec manually.");
			System.err.println("run: Runs the kernel using the given connection file.");
			System.err.println("\tThis option is not meant to be used manually, it is used internally when Jupyter starts the kernel.");
			throw die(2);
		}
		switch (args[0]) {
			case "install":
				final boolean userInstall = args.length > 1 && "--user".equals(args[1]);
				final String jupyterCommand;
				if (userInstall) {
					if (args.length > 3) {
						System.err.println("install --user expects at most one argument, not " + (args.length-2));
						System.err.println("Use --help for more info.");
						throw die(2);
					}
					jupyterCommand = args.length > 2 ? args[2] : "jupyter";
				} else {
					if (args.length > 2) {
						System.err.println("install expects at most one argument, not " + (args.length-1));
						System.err.println("Use --help for more info.");
						throw die(2);
					}
					jupyterCommand = args.length > 1 ? args[1] : "jupyter";
				}
				install(jupyterCommand, userInstall);
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
