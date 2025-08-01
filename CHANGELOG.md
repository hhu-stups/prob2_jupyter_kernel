# Changelog

## [(next version)](https://gitlab.cs.uni-duesseldorf.de/api/v4/projects/848/jobs/artifacts/master/raw/build/libs/prob2-jupyter-kernel-1.5.1-SNAPSHOT-all.jar?job=test)

* (no changes yet)

## [1.5.0](https://stups.hhu-hosting.de/downloads/prob2-jupyter/prob2-jupyter-kernel-1.5.0-all.jar)

* Updated to ProB 1.15.0 (ProB Java API version 4.15.0).
* `:show` now also supports VisB visualizations in addition to the classical `ANIMATION_FUNCTION`.

## [1.4.1](https://stups.hhu-hosting.de/downloads/prob2-jupyter/prob2-jupyter-kernel-1.4.1-all.jar)

* Updated to ProB 1.12.1 (ProB Java API version 4.12.1).

## [1.4.0](https://stups.hhu-hosting.de/downloads/prob2-jupyter/prob2-jupyter-kernel-1.4.0-all.jar)

* Updated to ProB 1.12.0 (ProB Java API version 4.12.0).
* Added position information to formula evaluation errors (e. g. type or well-definedness errors).
* Changed `:exec` and `:init` to automatically set up constants and initialize the machine if necessary. Previously this caused an error "Machine is not initialised".
* Rewrote the kernel installation code in Python. This is mostly an internal change and shouldn't affect most users. As a side effect, uninstalling the kernel is now simpler and requires no extra steps after `jupyter kernelspec remove`.
* Disabled LaTeX formatting inside `:check` tables as well, because of the layout issues mentioned below.

## [1.3.0](https://stups.hhu-hosting.de/downloads/prob2-jupyter/prob2-jupyter-kernel-1.3.0-all.jar)

* Added support for Java 17.
* Updated ProB 2 to version 3.15.0.
* Added a `:language` command to allow changing the language used to parse user input. For example `:language event_b` can be used to switch to Event-B syntax when a non-Event-B machine is loaded (or no machine at all).
* Made `:let` syntax more flexible: both `:let name value` and `:let name = value` are now allowed.
* Improved the performance of loading machines by reusing the existing instance of ProB instead of starting a new one for each machine.
* Improved error highlighting for machines loaded from files and not from the notebook.
* Local variables (created using `:let`) are now discarded when a new machine is loaded. Previously local variables would be remembered even across machine loads, which could lead to confusing behavior and errors.
* Significantly refactored the logic for parsing commands and their arguments.
	* This is an internal change and should not affect any user-visible behavior. That is, all inputs that were accepted by previous versions should still be accepted - if any previously valid inputs are no longer accepted, this is a bug.
	* As a side effect, the inspection and code completion features now work better in a few edge cases.
* Fixed a bug where interrupting a command could make the kernel completely stop responding, requiring a manual restart.
* Fixed syntax errors when using local variables (`:let`) in Event-B mode.
* Fixed the `:trace` command sometimes displaying transitions as `null`.
* Disabled LaTeX formatting inside `:table` output, because it leads to various layout issues inside Jupyter Notebook. The table contents are now rendered as plain Unicode text instead.

## [1.2.0](https://stups.hhu-hosting.de/downloads/prob2-jupyter/prob2-jupyter-kernel-1.2.0-all.jar)

* Added support for Java 14.
* Added B parser version information to `:version` output.
* Improved interrupt handling so that only the currently running command is interrupted, rather than terminating the entire kernel. This means that interrupts now no longer reset the kernel state (loaded machine, current animator state, local variables, etc.).
* Updated ProB 2 to version 3.11.0.
* Fixed a parse error when a line comment is used on the last line of an expression while any `:let` variables are defined.
* Fixed detection of B machines in cells without `::load`. Previously only single-line machines were recognized.

## [1.1.0](https://stups.hhu-hosting.de/downloads/prob2-jupyter/prob2-jupyter-kernel-1.1.0-all.jar)

* Added a `--user` flag to the installer to allow installing the kernel into the user home directory. This allows installing the kernel without `sudo` when not using a virtual environment.
* Added `:let` and `:unlet` commands to (un)define local variables.
	* **Note:** Local variables are currently stored and expanded in text form. Values whose text form is not parsable cannot be stored in local variables, and storing large values may cause performance issues.
* Added a `:modelcheck` command to run the ProB model checker.
* Added support for additional languages and file extensions. The `:load` command now recognizes all languages and file extensions supported by ProB 2.
	* **Note:** Some languages are not fully working yet (for example XTL).
* Allowed loading B machines by entering their code directly, without an explicit `::load` command, similar to how this is already allowed with expressions.
* Added a `:bsymb` command to load ProB's custom bsymb.sty LaTeX definitions on demand. After this command is executed, bsymb commands can be used in LaTeX formulas in Markdown cells.
* Added support for Java 11.
* Updated ProB 2 to version 3.10.0.
* Fixed installation problems under Windows (paths in the kernel metadata are now escaped correctly).
* Fixed confusing handling of trailing spaces in commands.
* Fixed `:show` not displaying animation images correctly for machines outside of the kernel's working directory.
* Fixed `:trace` not showing the parameters and return values of executed transitions.
* Changed error handling so exception stack traces are no longer shown in the notebook. (They are still logged to the console.)

## [1.0.0](https://stups.hhu-hosting.de/downloads/prob2-jupyter/prob2-jupyter-kernel-1.0.0-all.jar)

* Initial release.
