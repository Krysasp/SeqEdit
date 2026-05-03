# SeqEdit - Biological Sequence Editor

A Java Swing-based biological sequence editor for Linux, ported from the classic BioEdit software by Tom Hall. Features a Borland C++ Builder-style UI with multi-sequence support, phylogenetic tree construction, and BLAST integration.

![SeqEdit Screenshot](docs/screenshot.png) *(to be added)*

## Features

### Sequence Editing
- **FASTA & GenBank Support** - Load and save sequences in standard formats
- **Single-Sequence Display** - View one sequence at a time with horizontal scrolling
- **Sequence Navigation** - Toggle between sequences using Prev/Next buttons or left/right arrow keys
- **Colored Nucleotides** - Visual identification (A=red, T=cyan, G=blue, C=green)
- **Position Ruler** - Visual position indicator with zoom support
- **Zoom** - Adjust font size (8-20pt) for comfortable viewing

### Sequence Tools
- DNA ↔ RNA conversion
- Reverse complement, complement, and reverse operations
- Translate DNA to amino acids (standard genetic code)
- Sort sequences by length
- Rename and remove sequences

### Analysis Tools
- **Codon Table** - Standard genetic code viewer with amino acid colors
- **Sequence Composition** - Nucleotide and amino acid analysis
- **Six-Frame Translation** - Forward and reverse translation frames
- **Dot Plot** - Pairwise sequence comparison visualization
- **Restriction Mapping** - Find restriction enzyme sites in sequences
- **Phylogenetic Tree** - Build trees using:
  - UPGMA (Unweighted Pair Group Method)
  - NJ (Neighbor-Joining)
  - ML (Maximum Likelihood via FastTree)

### Alignment & BLAST
- **ClustalO Integration** - Multiple sequence alignment
- **BLAST Search** - BLASTN, BLASTP, BLASTX via NCBI (remote API or local tools)

## Installation

### Prerequisites

**Required:**
- Java JDK 8 or higher

**Optional (for advanced features):**
- ClustalO: `sudo apt install clustalo`
- FastTree: `conda install fasttree` or `sudo apt install fasttree`
- NCBI BLAST+: `sudo apt install ncbi-blast+`

### Quick Start

```bash
# Clone the repository
git clone https://github.com/yourusername/SeqEdit.git
cd SeqEdit

# Build the project
make

# Run SeqEdit
make run
# or
./run.sh
```

### Manual Build

```bash
cd src
javac -cp "../classes:../lib/*" -d ../classes *.java
cd ..
java -cp "classes:lib/*" SeqEdit
```

## Usage

1. **Open a FASTA file**: File → Open FASTA or click "Open" button
2. **Navigate sequences**: Use Prev/Next buttons or ← → arrow keys
3. **View sequence info**: Sequence name, length, and position shown in header
4. **Edit sequences**: Use Edit menu for cut, copy, paste, insert, or delete
5. **Analyze**: Use Analysis menu for composition, translation, dot plots, etc.
6. **Save**: File → Save or Save As to export in FASTA format

### Keyboard Shortcuts

- **← →** - Navigate between sequences
- **Ctrl+C** - Copy
- **Ctrl+V** - Paste
- **Ctrl+X** - Cut
- **Ctrl+A** - Select All

## Project Structure

```
SeqEdit/
├── src/                    # Java source files
│   ├── SeqEdit.java           # Main application window
│   ├── CodonTableDialog.java # Codon table viewer
│   ├── CompositionDialog.java # Sequence composition analysis
│   ├── SixFrameDialog.java   # Six-frame translation
│   ├── DotPlotDialog.java    # Dot plot analysis
│   ├── RestrictionDialog.java # Restriction mapping
│   ├── TreeDialog.java       # Phylogenetic tree viewer
│   ├── ClustalDialog.java   # ClustalO alignment
│   └── BlastDialog.java      # BLAST search interface
├── bin/                   # External tools (local copies)
│   ├── clustalo             # Clustal Omega aligner
│   ├── fasttree             # FastTree for ML trees
│   ├── blastn               # BLAST nucleotide search
│   ├── blastp               # BLAST protein search
│   ├── blastx               # BLAST translated search
│   └── makeblastdb          # BLAST database builder
├── lib/                   # Java libraries
│   ├── biojava-core-7.1.0.jar
│   └── biojava-genome-7.1.0.jar
├── classes/               # Compiled Java classes
├── examples/              # Example sequence files
│   └── test_sequences.fasta
├── Makefile              # Build script
├── run.sh                # Run script
└── README.md            # This file
```

## Configuration

### Tool Paths

SeqEdit automatically searches for external tools in this order:
1. `bin/` directory (local to the project)
2. System PATH

To use local tools, ensure they are in the `bin/` directory.

### BioJava

BioJava 7.1.0 libraries are included in the `lib/` directory. These are used for advanced sequence analysis features.

## Building from Source

```bash
# Clean build
make rebuild

# Just compile
make compile

# Run after compiling
make run

# Install tools to bin/ (if available on system)
make install-tools
```

## Known Issues & Limitations

- Manual alignment editor: Not yet implemented
- Plasmid drawing: Coming soon
- BLAST may require internet access for remote searches
- Very large sequences (>1MB) may require increased Java heap size

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This software is provided under the same terms as the original BioEdit - free for use by any interested parties.

## Acknowledgments

- Original BioEdit by Tom Hall
- ClustalO team for multiple sequence alignment
- FastTree (Morgan Price) for phylogenetic tree construction
- BioJava project for sequence analysis libraries

## Contact & Support

For bugs, feature requests, or questions, please open an issue on the GitHub repository.

---

**Version**: 1.0
**Last Updated**: November 2025
