# ProB 2 Jupyter Kernel

This is a [Jupyter](https://jupyter.org/) kernel for the [ProB animator and model checker](https://www3.hhu.de/stups/prob/), based on the [ProB 2 Java API](https://github.com/bendisposto/prob2) and the [Jupyter JVM BaseKernel](https://github.com/SpencerPark/jupyter-jvm-basekernel) library.

**Note:** This project is still in development, features and functionality are not yet stable and may change at any time.

## Requirements

* Java 8
	* Newer Java versions may work, but are not tested.
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
	* This assumes that Jupyter can be called using the command `jupyter`. To use a different command in place of `jupyter`, pass it as an argument after `install`, e. g. `java -jar <jarfile> install /path/to/jupyter`.
	* To use a different ProB home directory than the default, pass `-Dprob.home=/path/to/prob/home` before the `-jar` option. (The path must be absolute.)
4. (Optional) The jar file can be deleted after installation.

To update the kernel, follow the same instructions with the new jar file. 

### For developers

1. Clone this repository (`git clone https://gitlab.cs.uni-duesseldorf.de/dgelessus/prob2-jupyter-kernel.git`) or download an archive from [the repository page](https://gitlab.cs.uni-duesseldorf.de/dgelessus/prob2-jupyter-kernel).
2. If Jupyter is installed in a virtual environment, activate it.
3. In the root directory of the repository, run `./gradlew installKernelSpec`.
	* This assumes that Jupyter can be called using the command `jupyter`. To use a different command in place of `jupyter`, you can pass `-PjupyterCommand=/path/to/jupyter` to the `./gradlew` command.
	* To use a different ProB home directory than the default, pass `-PprobHome=/path/to/prob/home` to the `./gradlew` command. (The path must be absolute.)

To update the kernel, update the source code, then follow the same instructions. To speed up the process, you can usually use `./gradlew shadowJar` instead of `./gradlew installKernelSpec` - the kernel spec does not need to be reinstalled every time, unless any of the kernel spec files (`src/main/resources/de/prob2/jupyter/kernelspecfiles`) have changed.

## Uninstalling

To remove the kernel from Jupyter, run `jupyter kernelspec remove prob2`.

If the kernel was installed using `java -jar <jarfile> install`, the kernel jar has been copied into `prob2-<version>/jupyter` in your ProB home directory (`~/.prob` by default). To uninstall the kernel completely, delete this `jupyter` folder.

## Usage

After installation, start the Jupyter Notebook web interface using `jupyter notebook`. The ProB 2 kernel can be selected when creating a new notebook.

You can also use the kernel with other frontends, such as `jupyter console` and `jupyter qtconsole`, by specifying `--kernel=prob2` on the command line.

For information on how to use the kernel, run the built-in `:help` command, or see the included [example notebooks](./notebooks).
