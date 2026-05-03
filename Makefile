# Makefile for SeqEdit - Biological Sequence Editor
# Java Swing port of BioEdit

JAVAC = javac
JAVA = java
SRC_DIR = src
CLASS_DIR = classes
LIB_DIR = lib
BIN_DIR = bin
MAIN_CLASS = SeqEdit

# Classpath including lib JARs
CLASSPATH = $(CLASS_DIR):$(wildcard $(LIB_DIR)/*.jar)

# Source files
SOURCES = $(wildcard $(SRC_DIR)/*.java)

# Default target
all: compile

# Compile all Java files
compile: $(SOURCES)
	@mkdir -p $(CLASS_DIR)
	$(JAVAC) -cp "$(CLASSPATH)" -d $(CLASS_DIR) $(SOURCES)
	@echo "Compilation complete."

# Run the application
run: compile
	$(JAVA) -cp "$(CLASSPATH)" $(MAIN_CLASS)

# Clean compiled files
clean:
	rm -rf $(CLASS_DIR)
	@echo "Cleaned up."

# Rebuild
rebuild: clean compile

# Install tools to bin/ (copy from system if available)
install-tools:
	@mkdir -p $(BIN_DIR)
	@cp /usr/bin/clustalo $(BIN_DIR)/clustalo 2>/dev/null || echo "clustalo not found in /usr/bin"
	@which fasttree && cp $$(which fasttree) $(BIN_DIR)/fasttree || echo "fasttree not found"
	@which blastn && cp $$(which blastn) $(BIN_DIR)/blastn || echo "blastn not found"
	@which blastp && cp $$(which blastp) $(BIN_DIR)/blastp || echo "blastp not found"
	@which blastx && cp $$(which blastx) $(BIN_DIR)/blastx || echo "blastx not found"
	@which makeblastdb && cp $$(which makeblastdb) $(BIN_DIR)/makeblastdb || echo "makeblastdb not found"
	@ls -la $(BIN_DIR)/

.PHONY: all compile run clean rebuild install-tools
