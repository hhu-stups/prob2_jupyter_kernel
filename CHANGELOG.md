# Changelog

## [(next version)](./README.md#for-developers)

* Added a `--user` flag to the installer to allow installing the kernel into the user home directory. This allows installing the kernel without `sudo` when not using a virtual environment.
* Added `:let` and `:unlet` commands to (un)define local variables.
	* **Note:** Local variables are currently stored and expanded in text form. Values whose text form is not parsable cannot be stored in local variables, and storing large values may cause performance issues.
* Added support for additional languages and file extensions. The `:load` command now recognizes all languages and file extensions supported by ProB 2.
	* **Note:** Some languages are not fully working yet (for example XTL).
* Added support for Java 11.
* Updated ProB 2 to version 3.2.12.
* Fixed confusing handling of trailing spaces in commands.
* Fixed `:show` not displaying animation images correctly for machines outside of the kernel's working directory.
* Fixed `:trace` not showing the parameters and return values of executed transitions.
* Changed error handling so exception stack traces are no longer shown in the notebook. (They are still logged to the console.)

## [1.0.0](./releases/prob2-jupyter-kernel-1.0.0-all.jar)

* Initial release.
