compiler = pdflatex
build-dir = build
flags = --output-directory=$(build-dir)

all: compile

pdf: compile
	evince main.pdf &

compile: 
	mkdir -p $(build-dir)
	$(compiler) $(flags) main.tex
	mv build/main.pdf .

clean:
	rm -rf $(build-dir)
	rm -f *.pdf

review:
	mkdir -p $(build-dir)
	$(compiler) $(flags) review.tex
	mv build/review.pdf .