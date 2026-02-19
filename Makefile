# This makefile just reduces keystrokes when building the docs locally :-)
# It is not used by GitHub actions (for that, see .github/workflows).
.PHONY: default clean open-preview docker-build
default: build ;

build:
	asciidoctor -D localpreview/ --backend=html5 docs/*.adoc
	mv localpreview/README.html localpreview/index.html
	cp -r docs/assets/ localpreview/

open-preview:
	xdg-open ./localpreview/index.html

clean:
	rm -rf ./localpreview/


docker-build:
	docker run --rm -u $$(id -u):$$(id -g) -v $$(pwd):/documents/ asciidoctor/docker-asciidoctor asciidoctor -D localpreview/ --backend=html5 docs/*.adoc
	mv localpreview/README.html localpreview/index.html
	cp -r docs/assets/ localpreview/
