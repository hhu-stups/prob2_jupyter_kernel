# ProB 2 Jupyter Kernel

[![GitLab CI](https://gitlab.cs.uni-duesseldorf.de/general/stups/prob2-jupyter-kernel/badges/master/pipeline.svg)](https://gitlab.cs.uni-duesseldorf.de/general/stups/prob2-jupyter-kernel/pipelines) [![Binder](https://mybinder.org/badge_logo.svg)](https://mybinder.org/v2/git/https%3A%2F%2Fgitlab.cs.uni-duesseldorf.de%2Fgeneral%2Fstups%2Fprob2-jupyter-kernel.git/master?filepath=notebooks)

This is a [Jupyter](https://jupyter.org/) kernel for the [ProB animator and model checker](https://prob.hhu.de/w/), based on the [ProB 2 Java API](https://github.com/hhu-stups/prob2_kernel) and the [Jupyter JVM BaseKernel](https://github.com/SpencerPark/jupyter-jvm-basekernel) library.

**Note:** The project is still in development, but the syntax and commands accepted by the kernel are relatively stable. Small breaking changes in future versions are possible, but unlikely.

## Downloads

* **[Download the latest version of the ProB 2 Jupyter kernel here](https://stups.hhu-hosting.de/downloads/prob2-jupyter/prob2-jupyter-kernel-1.5.0-all.jar).**
* Download links for previous versions can be found in the [changelog].
* A [snapshot build](https://gitlab.cs.uni-duesseldorf.de/api/v4/projects/848/jobs/artifacts/master/raw/build/libs/prob2-jupyter-kernel-1.4.2-SNAPSHOT-all.jar?job=test) of the latest development version is also available. **Warning:** this is an unstable version that can contain bugs or breaking changes.

See the [requirements](#requirements) and [installation instructions](#installation) for information on how to install and use the downloaded jar file.

## Requirements

* Java 8 or newer
	* Tested up to Java 21.
* A Python 3 interpreter with [Jupyter installed](https://jupyter.org/install)
	* Tested with CPython 3.8 and Jupyter core/Notebook 6.x.
	* Older Python and Jupyter versions should also work (see below), but if possible please use the current versions of Python and Jupyter.
	* This kernel requires version 5.0 of the kernel message protocol, which is supported by all versions of Jupyter (since the split from IPython, i. e. 4.x and later), as well as IPython Notebook 3.x.
	* The Python version does not matter, as long as it is supported by the Jupyter version in use. Jupyter Notebook 6.x (the current version) supports Python 3.5 and later. Older Jupyter versions also support Python 2.7 (before Jupyter 6.0), and 3.4 and 3.3 (before Juypter 5.3.0).

## Installation

### For end users

1. Ensure that all [requirements](#requirements) are installed.
2. [Download the latest version of the kernel](#downloads).
3. If Jupyter is installed in a virtual environment, activate it.
4. Run `python3 <jarfile> install` to install the kernel. (`<jarfile>` is the name of the jar file that you just downloaded. Yes, you need to run the jar file using Python.)
	* If you get a permission error when installing the kernel spec, add the option `--user` after `install`. This will install the kernel spec into your user home instead of the Python install directory (which may not be writable).
	* To use a different ProB home directory than the default, set the environment variable `PROB_HOME` to the desired path. (The path must be absolute.)
5. (Optional) The jar file can be deleted after installation.

### For developers

1. Ensure that all [requirements](#requirements) are installed.
2. Clone this repository (`git clone https://gitlab.cs.uni-duesseldorf.de/general/stups/prob2-jupyter-kernel.git`) or download an archive from [the repository page](https://gitlab.cs.uni-duesseldorf.de/general/stups/prob2-jupyter-kernel).
3. If Jupyter is installed in a virtual environment, activate it.
4. In the root directory of the repository, run `./gradlew installKernelSpec`.
	* If Jupyter is installed in a virtual environment and you get an error that Jupyter could not be found, try running the Gradle command with the `--no-daemon` option. This ensures that Gradle sees all environment changes performed by the activation of the virtual environment.
	* If you get a permission error when installing the kernel spec, pass `-PkernelspecUserInstall=true` to the `./gradlew` command. This will install the kernel spec into your user home instead of the Python install directory (which may not be writable).
	* This assumes that Python can be called using the command `python3`. To use a different command in place of `python3`, you can pass `-PpythonCommand=/path/to/python` to the `./gradlew` command.
	* To use a different ProB home directory than the default, set the environment variable `PROB_HOME` to the desired path. (The path must be absolute.)

## Updating

To update from an older version of the kernel, follow the installation instructions above. (It's not necessary to uninstall the old version first.) See the [changelog] for a list of changes in each release.

## Uninstalling

To remove the kernel from Jupyter, run `jupyter kernelspec remove prob2`.

For previous versions of the kernel (1.3.0 and older),
you may also need to manually delete the kernel jar file from the ProB home directory (under `~/.prob` by default).
It should be located in a `jupyter` subdirectory.

## Usage

After installation, start the Jupyter Notebook web interface using `jupyter notebook`. The ProB 2 kernel can be selected when creating a new notebook.

You can also use the kernel with other frontends. You may need to manually specify the ProB 2 kernel's ID (`prob2`). For example, to run the kernel using Jupyter's [console](https://jupyter-console.readthedocs.io/) and [Qt console](https://qtconsole.readthedocs.io/) frontends:

```sh
$ jupyter console --kernel=prob2
$ jupyter qtconsole --kernel=prob2
```

For information on how to use the kernel, run the built-in `:help` command, or see the included [example notebooks](./notebooks).

## Citing

An article about the ProB Jupyter kernel has been [published at ABZ'2020](https://link.springer.com/epdf/10.1007/978-3-030-48077-6_19?sharing_token=Nbvsl1StbEqfKGPhJwLMb_e4RwlQNchNByi7wbcMAY4yDpL76P5EGFEyHUVQToM3cE0JT8SrX5kUcY5Tx3NbNk7ZRhAullHYDeKKl9C6z3f2jS5d0JEraXScv4hxlPmpP-17XOXseltcKfZbcq05hOnhHWx78Wll4QMNCK8E115bSCQ7acchJqrow-mu5nzV) ([short link](https://rdcu.be/b4rql)).

```bibtex
@inproceedings{GelessusLeuschel:abz2020,
	author = {David Gelessus and Michael Leuschel},
	title = {{ProB} and {Jupyter} for Logic, Set Theory, Theoretical Computer Science and Formal Methods},
	booktitle = {Proceedings ABZ 2020},
	editor = "Raschke, Alexander and M{\'e}ry, Dominique and Houdek, Frank",
	year = {2020},
	series = {LNCS 12071},
	pages = "248--254",
	isbn = "978-3-030-48077-6"
}
```

[changelog]: ./CHANGELOG.md
