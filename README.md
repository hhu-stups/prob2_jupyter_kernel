# ProB 2 Jupyter Kernel

This is a [Jupyter](https://jupyter.org/) kernel for the [ProB animator and model checker](https://www3.hhu.de/stups/prob/), based on the [ProB 2 Java API](https://github.com/bendisposto/prob2) and the [Jupyter JVM BaseKernel](https://github.com/SpencerPark/jupyter-jvm-basekernel) library.

**Note:** This project is still in development, features and functionality are not yet stable and may change at any time.

## Requirements

* Java 8
	* Newer Java versions may work, but are not tested.
* A Python 3 interpeter with Jupyter installed (`python3 -m pip install jupyter`)
	* Tested with CPython 3.6 and jupyter-client 5.2.3.
	* Newer Jupyter versions should also work.
	* Older Jupyter versions may work, as long as they support version 5.0 of the Jupyter message protocol.
	* The Python version does not matter, as long as it is supported by Jupyter.

## Installation

1. Clone this repository (`git clone https://gitlab.cs.uni-duesseldorf.de/dgelessus/prob2-jupyter-kernel.git`) or download an archive from [the repository page](https://gitlab.cs.uni-duesseldorf.de/dgelessus/prob2-jupyter-kernel).
2. If Jupyter is installed in a virtual environment, activate it.
3. In the root directory of the repository, run `./gradlew installKernelSpec`.
	* By default, this looks for a Python interpreter named `python3` in the PATH and uses it to install the kernel spec. If your Python interpreter is named differently, you can pass `-PpythonInterpreter=/path/to/python3` to the `./gradlew` command to use a different Python interpreter for the installation.

To update the kernel, follow the same instructions as for installation. (If you cloned the repository using Git, you can update your existing copy using `git pull` instead of cloning it again.)

**Note:** The compiled kernel jar is currently stored in the Gradle `build` output folder, and the generated kernel spec has this path hardcoded on installation. If the location of the repository directory changes, you must install the kernel spec again.

## Usage

After installation, start the Jupyter Notebook web interface using `python3 -m jupyter notebook`. The ProB 2 kernel can be selected when creating a new notebook.

You can also use the kernel with other frontends, such as `jupyter console` and `jupyter qtconsole`, by specifying `--kernel=prob2` on the command line.

For information on how to use the kernel, run the built-in `:help` command, or see the included [example notebooks](./notebooks).
