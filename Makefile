run:
	jupyter notebook
rebuild:
	./gradlew shadowJar
install:
	./gradlew installKernelSpec
USERNAME=
PROB_HOME=/Users/$(USERNAME)/git_root/prob_prolog
PYTHON=/Users/$(USERNAME)/opt/miniconda3/bin/python3
#PYTHON=/Users/$(USERNAME)/opt/miniconda3/envs/py10/bin/python3
installph:
	./gradlew -PpythonCommand=$(PYTHON) installKernelSpec
installphuser:
	echo "Installing for user, so that, e.g., VSCode sees the kernel"
	./gradlew -PprobHome=$(PROB_HOME) -PpythonCommand=$(PYTHON) -PkernelspecUserInstall=true installKernelSpec

remove:
	echo "Deinstalling ProB2 Kernel; you may have to run this twice!
	jupyter kernelspec remove prob2

console:
	jupyter console --kernel prob2

NOTEBOOKS = notebooks/tutorials/prob_solver_intro.ipynb \
               notebooks/manual/ExternalFunctions.ipynb
latex:
	for spec in $(NOTEBOOKS); do \
		echo 'Generating Latex for: '; echo $$spec ; \
		jupyter nbconvert $$spec --to latex ; \
		done
	echo "done"

slides:
	jupyter nbconvert notebooks/presentations/SETS_RODIN18.ipynb --to slides --post serve
slidespdf:
	jupyter nbconvert notebooks/presentations/SETS_RODIN18.ipynb --to latex
	pdflatex notebooks/presentations/SETS_RODIN18.tex