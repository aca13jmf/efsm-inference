build:
	isabelle build -D .
EFSM:
	isabelle build -d . EFSM

inference:
	isabelle build -d. inference

snippets:
	isabelle build -D . ;\
	sed -n '/\\snip{/,/endsnip/p' EFSM/output/document/*.tex > snippets.tex; \
	sed -n '/\\snip{/,/endsnip/p' EFSM_Inference/output/document/*.tex >> snippets.tex; \
mv snippets.tex ~/Documents/thesis/

clean:
	@find . -name "*thy~*" -exec rm {} \;

commit:
	cd EFSM ; \
	git add -A ; \
	git commit -m "$m" ; \
	git push origin master ; \
	cd .. ; \
	git add -A ; \
	git commit -m "$m" ; \
	git push origin master
