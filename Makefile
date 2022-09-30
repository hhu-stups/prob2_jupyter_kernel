run:
	jupyter notebook
rebuild:
	./gradlew shadowJar
install:
	./gradlew installKernelSpec
USERNAME=
PHOME=/Users/$(USERNAME)/git_root/prob_prolog
JUPYTER=/Users/$(USERNAME)/opt/miniconda3/bin/jupyter
#JUPYTER=/Users/$(USERNAME)/opt/miniconda3/envs/py10/bin/jupyter
installph:
	./gradlew -PprobHome=$(PHOME) -PjupyterCommand=$(JUPYTER) installKernelSpec
installphuser:
    echo "Installing for user, so that, e.g., VSCode sees the kernel"
	./gradlew -PprobHome=$(PHOME) -PjupyterCommand=$(JUPYTER) -PkernelspecUserInstall=true installKernelSpec

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