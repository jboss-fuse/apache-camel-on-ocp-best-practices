# This makefile just reduces keystrokes when building the docs locally :-)
# It is not used by GitHub actions (for that, see .github/workflows).
.PHONY: default clean open-preview
default: build ;

build:
	asciidoctor -D localpreview/ --backend=html5 docs/*.adoc
	mv localpreview/README.html localpreview/index.html
	cp -r docs/assets/ localpreview/

open-preview:
	xdg-open ./localpreview/index.html

clean:
	rm -rf ./localpreview/