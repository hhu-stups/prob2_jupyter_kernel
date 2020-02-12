# ProB 2 Jupyter Kernel

[![GitLab CI](https://gitlab.cs.uni-duesseldorf.de/dgelessus/prob2-jupyter-kernel/badges/master/pipeline.svg)](https://gitlab.cs.uni-duesseldorf.de/dgelessus/prob2-jupyter-kernel/pipelines) [![Binder](https://mybinder.org/badge_logo.svg)](https://mybinder.org/v2/git/https%3A%2F%2Fgitlab.cs.uni-duesseldorf.de%2Fdgelessus%2Fprob2-jupyter-kernel.git/master?filepath=notebooks)

This is a [Jupyter](https://jupyter.org/) kernel for the [ProB animator and model checker](https://www3.hhu.de/stups/prob/), based on the [ProB 2 Java API](https://github.com/bendisposto/prob2) and the [Jupyter JVM BaseKernel](https://github.com/SpencerPark/jupyter-jvm-basekernel) library.

**Note:** This project is still in development, features and functionality are not yet stable and may change at any time.

## Requirements

* Java 8 or newer
	* Tested up to Java 11.
* A Python 3 interpeter with Jupyter installed (`python3 -m pip install jupyter`)
	* Tested with CPython 3.6, jupyter-core 4.4.0, jupyter-client 5.2.3, notebook 5.6.0.
	* Newer Jupyter versions should also work.
	* Older Jupyter versions may work, as long as they support version 5.0 of the Jupyter message protocol.
	* The Python version does not matter, as long as it is supported by Jupyter.

## Installation

### For end users

1. Download the latest version of the kernel [here](https://gitlab.cs.uni-duesseldorf.de/dgelessus/prob2-jupyter-kernel/blob/master/releases/prob2-jupyter-kernel-1.0.0-all.jar).
2. If Jupyter is installed in a virtual environment, activate it.
3. Run `java -jar <jarfile> install` to install the kernel. (`<jarfile>` is the name of the jar file that you just downloaded.)
	* If you get a permission error when installing the kernel spec, add the option `--user` after `install`. This will install the kernel spec into your user home instead of the Python install directory (which may not be writable).
	* This assumes that Jupyter can be called using the command `jupyter`. To use a different command in place of `jupyter`, pass it as an argument after `install`, e. g. `java -jar <jarfile> install /path/to/jupyter`.
	* To use a different ProB home directory than the default, pass `-Dprob.home=/path/to/prob/home` before the `-jar` option. (The path must be absolute.)
4. (Optional) The jar file can be deleted after installation.

### For developers

1. Clone this repository (`git clone https://gitlab.cs.uni-duesseldorf.de/dgelessus/prob2-jupyter-kernel.git`) or download an archive from [the repository page](https://gitlab.cs.uni-duesseldorf.de/dgelessus/prob2-jupyter-kernel).
2. If Jupyter is installed in a virtual environment, activate it.
3. In the root directory of the repository, run `./gradlew installKernelSpec`.
	* If Jupyter is installed in a virtual environment and you get an error that the `jupyter` command could not be found/executed, try running the Gradle command with the `--no-daemon` option. This ensures that Gradle sees all environment changes performed by the activation of the virtual environment.
	* If you get a permission error when installing the kernel spec, pass `-PkernelspecUserInstall=true` to the `./gradlew` command. This will install the kernel spec into your user home instead of the Python install directory (which may not be writable).
	* This assumes that Jupyter can be called using the command `jupyter`. To use a different command in place of `jupyter`, you can pass `-PjupyterCommand=/path/to/jupyter` to the `./gradlew` command.
	* To use a different ProB home directory than the default, pass `-PprobHome=/path/to/prob/home` to the `./gradlew` command. (The path must be absolute.)

## Updating

To update from an older version of the kernel, follow the installation instructions above. (It's not necessary to uninstall the old version first.) See the [changelog](./CHANGELOG.md) for a list of changes in each release.

## Uninstalling

To remove the kernel from Jupyter, run `jupyter kernelspec remove prob2`.

If the kernel was installed using `java -jar <jarfile> install`, the kernel jar has been copied into `prob2-<version>/jupyter` in your ProB home directory (`~/.prob` by default). To uninstall the kernel completely, delete this `jupyter` folder.

## Usage

After installation, start the Jupyter Notebook web interface using `jupyter notebook`. The ProB 2 kernel can be selected when creating a new notebook.

You can also use the kernel with other frontends, such as `jupyter console` and `jupyter qtconsole`, by specifying `--kernel=prob2` on the command line.

For information on how to use the kernel, run the built-in `:help` command, or see the included [example notebooks](./notebooks).
