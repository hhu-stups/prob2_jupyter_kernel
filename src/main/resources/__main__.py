# Installer script for the ProB 2 Jupyter kernel.
# This script can be run by executing the kernel jar file with Python.
# This makes use of Python's zipapp support
# and the fact that jar files are valid zip files.


import sys


if sys.version_info < (2, 7):
	sys.stderr.write("ERROR: This Python installation is too old! At least Python 2.7 is required.\n")
	sys.stderr.write("ERROR: You are using Python " + str(sys.version_info[0]) + "." + str(sys.version_info[1]) + " installed at " + sys.executable + "\n")
	sys.exit(1)


import argparse
import io
import json
import logging
import os.path
import shutil
import tempfile
import zipfile


KERNELSPEC_FILES = [
	"kernel.js",
	"logo-32x32.png",
	"logo-64x64.png",
]

logger = logging.getLogger(__name__)


def create_kernel_json(jar_path, prob_home, dest_dir):
	with io.open(os.path.join(dest_dir, "kernel.json"), "w", encoding="utf-8") as f:
		kernel_argv = ["java"]
		if prob_home is not None:
			kernel_argv.append("-Dprob.home=" + prob_home)
		kernel_argv += ["-jar", jar_path, "run", "{connection_file}"]
		
		kernel_json = {
			"argv": kernel_argv,
			"display_name": "ProB 2",
			"language": "prob",
			"interrupt_mode": "message",
		}
		json.dump(kernel_json, f, ensure_ascii=False, indent=4)


def create_kernelspec(jar_path, prob_home, dest_dir):
	create_kernel_json(jar_path, prob_home, dest_dir)
	
	with zipfile.ZipFile(jar_path) as zf:
		for file in KERNELSPEC_FILES:
			with zf.open("de/prob2/jupyter/kernelspecfiles/" + file) as fin:
				with io.open(os.path.join(dest_dir, file), "wb") as fout:
					shutil.copyfileobj(fin, fout)


def main():
	logging.basicConfig(
		format="%(levelname)s: %(message)s",
		level=logging.INFO,
	)
	
	ap = argparse.ArgumentParser(
		description="To install the kernel, run: python3 %(prog)s install",
		add_help=False,
	)
	
	ap.add_argument("--help", action="help")
	ap.add_argument("--jar-path", help="Run the kernel jar using the specified fixed path (this disables copying the jar into the kernelspec directory)")
	
	subs = ap.add_subparsers(dest="subcommand")
	
	ap_create = subs.add_parser("createKernelSpec", help="Generate a kernelspec for this kernel without installing it")
	ap_create.add_argument("destination", help="Name of the directory into which to generate the kernelspec (the directory must exist already and should be empty)")
	
	ap_install = subs.add_parser("install", help="Install this kernel")
	ap_install.add_argument("--user", action="store_true", help="Install to the per-user kernel registry")
	ap_install.add_argument("--prefix", help="Install to a different prefix than the default")
	
	ns = ap.parse_args()
	
	if ns.jar_path is None:
		jar_path = sys.argv[0]
	else:
		jar_path = ns.jar_path
	
	prob_home = os.environ.get("PROB_HOME", None)
	
	if ns.subcommand is None:
		logger.error("Missing subcommand")
		logger.info("To install the kernel, run: python3 %s install", os.path.basename(sys.argv[0]))
		logger.info("For detailed help, run: python3 %s --help", os.path.basename(sys.argv[0]))
		sys.exit(2)
	elif ns.subcommand == "createKernelSpec":
		create_kernelspec(jar_path, prob_home, ns.destination)
	elif ns.subcommand == "install":
		if ns.prefix is None and not ns.user:
			ns.prefix = sys.prefix
		
		try:
			from jupyter_client import kernelspec
		except ImportError:
			logger.error("This Python installation doesn't have Jupyter installed!")
			logger.error("You are using Python %d.%d installed at %s", sys.version_info[0], sys.version_info[1], sys.executable)
			logger.error("%s", sys.exc_info()[1])
			sys.exit(1)
		
		temp_dir = tempfile.mkdtemp()
		try:
			create_kernelspec(jar_path, prob_home, temp_dir)
			destination = kernelspec.KernelSpecManager().install_kernel_spec(temp_dir, "prob2", user=ns.user, prefix=ns.prefix)
		finally:
			shutil.rmtree(temp_dir)
		
		if not ns.jar_path:
			# If the user didn't override the jar path,
			# copy the jar into the installed kernelspec directory
			# and adjust argv in kernel.json accordingly.
			installed_jar_path = os.path.join(destination, "prob2-jupyter-kernel-all.jar")
			shutil.copyfile(jar_path, installed_jar_path)
			create_kernelspec(installed_jar_path, prob_home, destination)
		
		logger.info("The ProB 2 Jupyter kernel has been installed successfully.")
		logger.info('To use it, start Jupyter Notebook and select "ProB 2" when creating a new notebook.')
		if not ns.jar_path:
			logger.info("This jar file can be safely deleted after installation.")
	else:
		raise AssertionError("Subcommand not handled: " + ns.subcommand)
	
	sys.exit(0)


if __name__ == "__main__":
	sys.exit(main())
