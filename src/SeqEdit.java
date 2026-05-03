import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class SeqEdit extends JFrame {
    private JList<String> sequenceList;
    private DefaultListModel<String> listModel;
    private JTextPane sequenceDisplay;
    private JScrollPane rightScrollPane;
    private JLabel statusLabel;
    private JLabel infoLabel;
    private JLabel seqInfoLabel;
    private List<SequenceRecord> sequences;
    private boolean colorMode = true;
    private boolean rulerMode = true;
    private File currentFile = null;
    private int currentFontSize = 12;
    private int currentDisplayIndex = -1;

    // Colors for Borland-style UI
    private static final Color BORLAND_BG = new Color(192, 192, 192);
    private static final Color BORLAND_DARK = new Color(128, 128, 128);
    private static final Color BORLAND_LIGHT = new Color(255, 255, 255);

    // Nucleotide colors
    private static final Color COLOR_A = new Color(255, 107, 107);
    private static final Color COLOR_T = new Color(78, 205, 196);
    private static final Color COLOR_G = new Color(69, 183, 209);
    private static final Color COLOR_C = new Color(150, 206, 180);
    private static final Color COLOR_U = new Color(78, 205, 196);
    private static final Color COLOR_N = new Color(204, 204, 204);
    private static final Color COLOR_GAP = new Color(238, 238, 238);
    private static final Color COLOR_DEFAULT = Color.BLACK;

    // Amino acid colors
    private static final Map<Character, Color> AA_COLORS = new HashMap<>();

    // Codon table
    static final Map<String, Character> CODON_TABLE = new HashMap<>();

    static {
        AA_COLORS.put('A', new Color(200, 200, 200));
        AA_COLORS.put('R', new Color(20, 90, 255));
        AA_COLORS.put('N', new Color(0, 220, 220));
        AA_COLORS.put('D', new Color(230, 10, 10));
        AA_COLORS.put('C', new Color(230, 230, 0));
        AA_COLORS.put('Q', new Color(0, 220, 220));
        AA_COLORS.put('E', new Color(230, 10, 10));
        AA_COLORS.put('G', new Color(235, 235, 235));
        AA_COLORS.put('H', new Color(130, 130, 211));
        AA_COLORS.put('I', new Color(15, 130, 15));
        AA_COLORS.put('L', new Color(15, 130, 15));
        AA_COLORS.put('K', new Color(20, 90, 255));
        AA_COLORS.put('M', new Color(230, 230, 0));
        AA_COLORS.put('F', new Color(50, 50, 170));
        AA_COLORS.put('P', new Color(220, 150, 130));
        AA_COLORS.put('S', new Color(250, 150, 0));
        AA_COLORS.put('T', new Color(250, 150, 0));
        AA_COLORS.put('W', new Color(180, 90, 180));
        AA_COLORS.put('Y', new Color(140, 140, 0));
        AA_COLORS.put('V', new Color(15, 130, 15));
        AA_COLORS.put('*', new Color(255, 217, 61));

        String[][] codons = {
            {"TTT", "F"}, {"TTC", "F"}, {"TTA", "L"}, {"TTG", "L"},
            {"CTT", "L"}, {"CTC", "L"}, {"CTA", "L"}, {"CTG", "L"},
            {"ATT", "I"}, {"ATC", "I"}, {"ATA", "I"}, {"ATG", "M"},
            {"GTT", "V"}, {"GTC", "V"}, {"GTA", "V"}, {"GTG", "V"},
            {"TCT", "S"}, {"TCC", "S"}, {"TCA", "S"}, {"TCG", "S"},
            {"CCT", "P"}, {"CCC", "P"}, {"CCA", "P"}, {"CCG", "P"},
            {"ACT", "T"}, {"ACC", "T"}, {"ACA", "T"}, {"ACG", "T"},
            {"GCT", "A"}, {"GCC", "A"}, {"GCA", "A"}, {"GCG", "A"},
            {"TAT", "Y"}, {"TAC", "Y"}, {"TAA", "*"}, {"TAG", "*"},
            {"CAT", "H"}, {"CAC", "H"}, {"CAA", "Q"}, {"CAG", "Q"},
            {"AAT", "N"}, {"AAC", "N"}, {"AAA", "K"}, {"AAG", "K"},
            {"GAT", "D"}, {"GAC", "D"}, {"GAA", "E"}, {"GAG", "E"},
            {"TGT", "C"}, {"TGC", "C"}, {"TGA", "*"}, {"TGG", "W"},
            {"CGT", "R"}, {"CGC", "R"}, {"CGA", "R"}, {"CGG", "R"},
            {"AGT", "S"}, {"AGC", "S"}, {"AGA", "R"}, {"AGG", "R"},
            {"GGT", "G"}, {"GGC", "G"}, {"GGA", "G"}, {"GGG", "G"},
        };
        for (String[] codon : codons) {
            CODON_TABLE.put(codon[0], codon[1].charAt(0));
        }
    }

    public SeqEdit() {
        sequences = new ArrayList<>();
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("SeqEdit - Biological Sequence Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        getContentPane().setBackground(BORLAND_BG);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BORLAND_BG);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);

        JToolBar toolbar = createToolBar();
        mainPanel.add(toolbar, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(250);
        splitPane.setDividerSize(6);
        splitPane.setBackground(BORLAND_BG);
        splitPane.setBorder(createBorlandBorder());

        JPanel leftPanel = createLeftPanel();
        splitPane.setLeftComponent(leftPanel);

        JPanel rightPanel = createRightPanel();
        splitPane.setRightComponent(rightPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);

        JPanel statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        // Key bindings for left/right navigation
        setupKeyBindings();
    }

    private void setupKeyBindings() {
        // Left arrow - previous sequence
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "prevSeq");
        getRootPane().getActionMap().put("prevSeq", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { navigatePrevious(); }
        });

        // Right arrow - next sequence
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "nextSeq");
        getRootPane().getActionMap().put("nextSeq", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { navigateNext(); }
        });
    }

    private Border createBorlandBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORLAND_DARK, 1),
            BorderFactory.createLineBorder(BORLAND_LIGHT, 1)
        );
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BORLAND_BG);
        panel.setBorder(BorderFactory.createTitledBorder(
            createBorlandBorder(), "Sequences",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Dialog", Font.BOLD, 11), Color.BLACK));

        // Search panel
        JPanel searchPanel = new JPanel(new BorderLayout(3, 0));
        searchPanel.setBackground(BORLAND_BG);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        searchPanel.add(searchLabel, BorderLayout.WEST);

        JTextField searchField = new JTextField();
        searchField.setFont(new Font("Dialog", Font.PLAIN, 11));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORLAND_DARK),
            BorderFactory.createEmptyBorder(1, 2, 1, 2)
        ));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterSequences(searchField.getText()); }
            public void removeUpdate(DocumentEvent e) { filterSequences(searchField.getText()); }
            public void changedUpdate(DocumentEvent e) { filterSequences(searchField.getText()); }
        });
        searchPanel.add(searchField, BorderLayout.CENTER);
        panel.add(searchPanel, BorderLayout.NORTH);

        // Sequence list
        listModel = new DefaultListModel<>();
        sequenceList = new JList<>(listModel);
        sequenceList.setFont(new Font("Monospaced", Font.PLAIN, 11));
        sequenceList.setBackground(new Color(255, 255, 255));
        sequenceList.setSelectionBackground(new Color(0, 0, 128));
        sequenceList.setSelectionForeground(Color.WHITE);
        sequenceList.setFixedCellHeight(20);
        sequenceList.setBorder(BorderFactory.createLineBorder(BORLAND_DARK, 1));

        sequenceList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int idx = sequenceList.getSelectedIndex();
                if (idx >= 0 && idx < sequences.size()) {
                    displaySequence(idx);
                }
            }
        });

        JScrollPane leftScroll = new JScrollPane(sequenceList);
        leftScroll.setBorder(createBorlandBorder());
        leftScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        leftScroll.getVerticalScrollBar().setUnitIncrement(20);
        leftScroll.getVerticalScrollBar().setBlockIncrement(60);
        panel.add(leftScroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BORLAND_BG);
        panel.setBorder(BorderFactory.createTitledBorder(
            createBorlandBorder(), "Sequence Display",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Dialog", Font.BOLD, 11), Color.BLACK));

        // Navigation panel
        JPanel navPanel = new JPanel(new BorderLayout());
        navPanel.setBackground(BORLAND_BG);
        navPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // Left navigation button
        JButton prevBtn = new JButton("< Prev");
        prevBtn.setFont(new Font("Dialog", Font.PLAIN, 10));
        prevBtn.setBackground(BORLAND_BG);
        prevBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORLAND_DARK, 1),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        prevBtn.setFocusPainted(false);
        prevBtn.addActionListener(e -> navigatePrevious());

        // Right navigation button
        JButton nextBtn = new JButton("Next >");
        nextBtn.setFont(new Font("Dialog", Font.PLAIN, 10));
        nextBtn.setBackground(BORLAND_BG);
        nextBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORLAND_DARK, 1),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        nextBtn.setFocusPainted(false);
        nextBtn.addActionListener(e -> navigateNext());

        // Sequence info label
        seqInfoLabel = new JLabel("No sequence selected");
        seqInfoLabel.setFont(new Font("Dialog", Font.BOLD, 11));
        seqInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel navBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        navBtns.setBackground(BORLAND_BG);
        navBtns.add(prevBtn);
        navBtns.add(nextBtn);

        navPanel.add(navBtns, BorderLayout.WEST);
        navPanel.add(seqInfoLabel, BorderLayout.CENTER);

        // Add keyboard shortcut labels
        JLabel keyLabel = new JLabel("Use arrow keys to navigate");
        keyLabel.setFont(new Font("Dialog", Font.PLAIN, 9));
        keyLabel.setForeground(Color.GRAY);
        navPanel.add(keyLabel, BorderLayout.EAST);

        panel.add(navPanel, BorderLayout.NORTH);

        // Ruler panel
        RulerPanel ruler = new RulerPanel();
        ruler.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORLAND_DARK, 1),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        panel.add(ruler, BorderLayout.SOUTH);

        // Sequence display - single line, horizontally scrollable
        sequenceDisplay = new JTextPane();
        sequenceDisplay.setFont(new Font("Monospaced", Font.PLAIN, currentFontSize));
        sequenceDisplay.setEditable(true);
        sequenceDisplay.setBackground(Color.WHITE);

        // Popup menu
        JPopupMenu textPopup = new JPopupMenu();
        addPopupItem(textPopup, "Cut", e -> sequenceDisplay.cut());
        addPopupItem(textPopup, "Copy", e -> sequenceDisplay.copy());
        addPopupItem(textPopup, "Paste", e -> sequenceDisplay.paste());
        textPopup.addSeparator();
        addPopupItem(textPopup, "Select All", e -> sequenceDisplay.selectAll());
        sequenceDisplay.setComponentPopupMenu(textPopup);

        rightScrollPane = new JScrollPane(sequenceDisplay);
        rightScrollPane.setBorder(createBorlandBorder());
        rightScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        rightScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        rightScrollPane.getVerticalScrollBar().setUnitIncrement(20);

        panel.add(rightScrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(BORLAND_BG);
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORLAND_DARK, 1),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        infoLabel = new JLabel("No sequences loaded");
        infoLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        statusPanel.add(infoLabel, BorderLayout.EAST);

        return statusPanel;
    }

    private void navigatePrevious() {
        if (sequences.isEmpty()) return;
        int newIdx = currentDisplayIndex - 1;
        if (newIdx < 0) newIdx = sequences.size() - 1; // wrap around
        sequenceList.setSelectedIndex(newIdx);
        sequenceList.ensureIndexIsVisible(newIdx);
        displaySequence(newIdx);
    }

    private void navigateNext() {
        if (sequences.isEmpty()) return;
        int newIdx = currentDisplayIndex + 1;
        if (newIdx >= sequences.size()) newIdx = 0; // wrap around
        sequenceList.setSelectedIndex(newIdx);
        sequenceList.ensureIndexIsVisible(newIdx);
        displaySequence(newIdx);
    }

    private void displaySequence(int idx) {
        if (idx < 0 || idx >= sequences.size()) return;

        currentDisplayIndex = idx;
        SequenceRecord seq = sequences.get(idx);

        // Update sequence info label
        seqInfoLabel.setText("Sequence " + (idx + 1) + " of " + sequences.size() + ": " + seq.id);

        // Display the sequence
        StyledDocument doc = sequenceDisplay.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());

            // Header
            SimpleAttributeSet headerAttrs = new SimpleAttributeSet();
            StyleConstants.setBold(headerAttrs, true);
            StyleConstants.setFontSize(headerAttrs, currentFontSize);
            doc.insertString(doc.getLength(), ">" + seq.id + "\n", headerAttrs);

            // Sequence - single line, horizontally scrollable
            if (colorMode) {
                for (int k = 0; k < seq.sequence.length(); k++) {
                    char c = seq.sequence.charAt(k);
                    SimpleAttributeSet attrs = new SimpleAttributeSet();
                    StyleConstants.setFontSize(attrs, currentFontSize);
                    StyleConstants.setForeground(attrs, getNucleotideColor(c));
                    doc.insertString(doc.getLength(), String.valueOf(c), attrs);
                }
            } else {
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setFontSize(attrs, currentFontSize);
                doc.insertString(doc.getLength(), seq.sequence, attrs);
            }

            doc.insertString(doc.getLength(), "\n", new SimpleAttributeSet());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        sequenceDisplay.setCaretPosition(0);
        setStatus("Viewing sequence: " + seq.id);
    }

    private void refreshDisplay() {
        if (sequences.isEmpty()) {
            listModel.clear();
            seqInfoLabel.setText("No sequence selected");
            try {
                sequenceDisplay.getStyledDocument().remove(0, sequenceDisplay.getStyledDocument().getLength());
            } catch (Exception e) {}
            infoLabel.setText("No sequences loaded");
            currentDisplayIndex = -1;
            revalidate();
            repaint();
            return;
        }

        // Update list model
        listModel.clear();
        for (SequenceRecord seq : sequences) {
            String displayName = seq.id.length() > 25 ? seq.id.substring(0, 25) + "..." : seq.id;
            listModel.addElement(displayName + " (" + seq.sequence.length() + ")");
        }

        int totalLen = sequences.stream().mapToInt(s -> s.sequence.length()).sum();
        infoLabel.setText(sequences.size() + " sequence(s), " + totalLen + " bp/aa total");

        // If no sequence is displayed, show the first one
        if (currentDisplayIndex < 0 && !sequences.isEmpty()) {
            sequenceList.setSelectedIndex(0);
            displaySequence(0);
        } else if (currentDisplayIndex >= 0 && currentDisplayIndex < sequences.size()) {
            displaySequence(currentDisplayIndex);
        }

        revalidate();
        repaint();
    }

    private Color getNucleotideColor(char c) {
        switch (Character.toUpperCase(c)) {
            case 'A': return COLOR_A;
            case 'T': return COLOR_T;
            case 'G': return COLOR_G;
            case 'C': return COLOR_C;
            case 'U': return COLOR_U;
            case 'N': return COLOR_N;
            case '-': return COLOR_GAP;
            case '*': return new Color(255, 217, 61);
            default: return COLOR_DEFAULT;
        }
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(BORLAND_BG);
        menuBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORLAND_DARK),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        JMenu fileMenu = new JMenu("File");
        fileMenu.setFont(new Font("Dialog", Font.PLAIN, 11));
        addMenuItem(fileMenu, "New Sequence", e -> onNewSequence());
        fileMenu.addSeparator();
        addMenuItem(fileMenu, "Open FASTA", e -> onOpenFasta());
        addMenuItem(fileMenu, "Open GenBank", e -> onOpenGenbank());
        fileMenu.addSeparator();
        addMenuItem(fileMenu, "Save", e -> onSave());
        addMenuItem(fileMenu, "Save As...", e -> onSaveAs());
        fileMenu.addSeparator();
        addMenuItem(fileMenu, "Export Alignment (FASTA)", e -> onExportFasta());
        addMenuItem(fileMenu, "Export Alignment (PHYLIP)", e -> onExportPhylip());
        fileMenu.addSeparator();
        addMenuItem(fileMenu, "Quit", e -> onQuit());
        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        editMenu.setFont(new Font("Dialog", Font.PLAIN, 11));
        addMenuItem(editMenu, "Cut", e -> onCut());
        addMenuItem(editMenu, "Copy", e -> onCopy());
        addMenuItem(editMenu, "Paste", e -> onPaste());
        addMenuItem(editMenu, "Select All", e -> onSelectAll());
        editMenu.addSeparator();
        addMenuItem(editMenu, "Insert Bases at Position...", e -> onInsertBases());
        addMenuItem(editMenu, "Delete Bases at Position...", e -> onDeleteBases());
        menuBar.add(editMenu);

        JMenu viewMenu = new JMenu("View");
        viewMenu.setFont(new Font("Dialog", Font.PLAIN, 11));
        JCheckBoxMenuItem colorItem = new JCheckBoxMenuItem("Color Nucleotides", colorMode);
        colorItem.setFont(new Font("Dialog", Font.PLAIN, 11));
        colorItem.addActionListener(e -> {
            colorMode = colorItem.isSelected();
            if (currentDisplayIndex >= 0) displaySequence(currentDisplayIndex);
        });
        viewMenu.add(colorItem);

        JCheckBoxMenuItem rulerItem = new JCheckBoxMenuItem("Show Position Ruler", rulerMode);
        rulerItem.setFont(new Font("Dialog", Font.PLAIN, 11));
        rulerItem.addActionListener(e -> {
            rulerMode = rulerItem.isSelected();
            refreshDisplay();
        });
        viewMenu.add(rulerItem);

        viewMenu.addSeparator();
        addMenuItem(viewMenu, "Zoom In", e -> zoomIn());
        addMenuItem(viewMenu, "Zoom Out", e -> zoomOut());
        menuBar.add(viewMenu);

        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setFont(new Font("Dialog", Font.PLAIN, 11));
        addMenuItem(toolsMenu, "Convert DNA to RNA", e -> onDnaToRna());
        addMenuItem(toolsMenu, "Convert RNA to DNA", e -> onRnaToDna());
        addMenuItem(toolsMenu, "Convert to Amino Acids", e -> onToAa());
        toolsMenu.addSeparator();
        addMenuItem(toolsMenu, "Reverse Complement", e -> onReverseComplement());
        addMenuItem(toolsMenu, "Complement", e -> onComplement());
        addMenuItem(toolsMenu, "Reverse", e -> onReverse());
        toolsMenu.addSeparator();
        addMenuItem(toolsMenu, "Sort Sequences by Length", e -> onSortSequences());
        addMenuItem(toolsMenu, "Rename Sequence", e -> onRenameSequence());
        addMenuItem(toolsMenu, "Remove Gaps", e -> onRemoveGaps());
        menuBar.add(toolsMenu);

        JMenu analysisMenu = new JMenu("Analysis");
        analysisMenu.setFont(new Font("Dialog", Font.PLAIN, 11));
        addMenuItem(analysisMenu, "Codon Table", e -> onCodonTable());
        addMenuItem(analysisMenu, "Sequence Composition", e -> onComposition());
        addMenuItem(analysisMenu, "Six-Frame Translation", e -> onSixFrame());
        addMenuItem(analysisMenu, "Dot Plot", e -> onDotPlot());
        analysisMenu.addSeparator();
        addMenuItem(analysisMenu, "Restriction Mapping", e -> onRestrictionMap());
        addMenuItem(analysisMenu, "Plasmid Drawing", e -> onPlasmidDrawing());
        analysisMenu.addSeparator();
        addMenuItem(analysisMenu, "Phylogenetic Tree", e -> onPhyloTree());
        menuBar.add(analysisMenu);

        JMenu alignMenu = new JMenu("Alignment");
        alignMenu.setFont(new Font("Dialog", Font.PLAIN, 11));
        addMenuItem(alignMenu, "ClustalO Alignment", e -> onClustalAlignment());
        addMenuItem(alignMenu, "Manual Alignment Editor", e -> onManualAlignment());
        menuBar.add(alignMenu);

        JMenu blastMenu = new JMenu("BLAST");
        blastMenu.setFont(new Font("Dialog", Font.PLAIN, 11));
        addMenuItem(blastMenu, "BLASTN (Nucleotide)", e -> onBlastn());
        addMenuItem(blastMenu, "BLASTP (Protein)", e -> onBlastp());
        addMenuItem(blastMenu, "BLASTX", e -> onBlastx());
        menuBar.add(blastMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setFont(new Font("Dialog", Font.PLAIN, 11));
        addMenuItem(helpMenu, "About SeqEdit", e -> onAbout());
        menuBar.add(helpMenu);

        return menuBar;
    }

    private void addMenuItem(JMenu menu, String label, ActionListener listener) {
        JMenuItem item = new JMenuItem(label);
        item.setFont(new Font("Dialog", Font.PLAIN, 11));
        item.addActionListener(listener);
        menu.add(item);
    }

    private JToolBar createToolBar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(BORLAND_BG);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORLAND_DARK),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));

        addToolButton(toolbar, "New", e -> onNewSequence());
        addToolButton(toolbar, "Open", e -> onOpenFasta());
        addToolButton(toolbar, "Save", e -> onSave());
        toolbar.addSeparator();

        addToolButton(toolbar, "Cut", e -> onCut());
        addToolButton(toolbar, "Copy", e -> onCopy());
        addToolButton(toolbar, "Paste", e -> onPaste());
        toolbar.addSeparator();

        addToolButton(toolbar, "Prev", e -> navigatePrevious());
        addToolButton(toolbar, "Next", e -> navigateNext());
        toolbar.addSeparator();

        addToolButton(toolbar, "ClustalO", e -> onClustalAlignment());
        addToolButton(toolbar, "BLAST", e -> onBlastn());
        addToolButton(toolbar, "Tree", e -> onPhyloTree());
        toolbar.addSeparator();

        addToolButton(toolbar, "Zoom+", e -> zoomIn());
        addToolButton(toolbar, "Zoom-", e -> zoomOut());

        return toolbar;
    }

    private JButton addToolButton(JToolBar toolbar, String text, ActionListener listener) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Dialog", Font.PLAIN, 10));
        btn.setBackground(BORLAND_BG);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORLAND_DARK, 1),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        btn.setFocusPainted(false);
        btn.addActionListener(listener);
        toolbar.add(btn);
        return btn;
    }

    private void onNewSequence() {
        String id = JOptionPane.showInputDialog(this, "Enter sequence ID:", "New_Sequence");
        if (id != null && !id.isEmpty()) {
            sequences.add(new SequenceRecord(id, "", "New sequence"));
            refreshDisplay();
            // Select the new sequence
            sequenceList.setSelectedIndex(sequences.size() - 1);
            setStatus("Added new sequence: " + id);
        }
    }

    private void onOpenFasta() {
        JFileChooser chooser = new JFileChooser(currentFile != null ? currentFile.getParent() : ".");
        chooser.setFileFilter(new FileNameExtensionFilter("FASTA files (*.fasta, *.fa, *.fas, *.seq)", "fasta", "fa", "fas", "seq"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadFastaFile(chooser.getSelectedFile());
        }
    }

    private void onOpenGenbank() {
        JFileChooser chooser = new JFileChooser(currentFile != null ? currentFile.getParent() : ".");
        chooser.setFileFilter(new FileNameExtensionFilter("GenBank files (*.gb, *.gbk)", "gb", "gbk"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadGenbankFile(chooser.getSelectedFile());
        }
    }

    private void loadFastaFile(File file) {
        try {
            List<SequenceRecord> loaded = parseFasta(file);
            if (loaded.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No sequences found in file", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            sequences.addAll(loaded);
            currentFile = file;
            refreshDisplay();
            // Select first loaded sequence
            sequenceList.setSelectedIndex(sequences.size() - loaded.size());
            setStatus("Loaded " + loaded.size() + " sequence(s) from " + file.getName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading FASTA file: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void loadGenbankFile(File file) {
        try {
            List<SequenceRecord> loaded = parseGenbank(file);
            if (loaded.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No sequences found in file", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            sequences.addAll(loaded);
            refreshDisplay();
            sequenceList.setSelectedIndex(sequences.size() - loaded.size());
            setStatus("Loaded " + loaded.size() + " sequence(s) from " + file.getName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading GenBank file: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private List<SequenceRecord> parseFasta(File file) throws IOException {
        List<SequenceRecord> result = new ArrayList<>();
        List<String> lines = Files.readAllLines(file.toPath());

        String currentId = null;
        StringBuilder currentSeq = null;
        String currentDesc = "";

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith(">")) {
                if (currentId != null && currentSeq != null && currentSeq.length() > 0) {
                    result.add(new SequenceRecord(currentId, currentSeq.toString(), currentDesc));
                }

                String header = trimmedLine.substring(1).trim();
                String seqOnHeader = "";
                int tabIdx = header.indexOf('\t');
                if (tabIdx >= 0) {
                    seqOnHeader = header.substring(tabIdx + 1).trim();
                    header = header.substring(0, tabIdx).trim();
                }

                int spaceIdx = header.indexOf(' ');
                if (spaceIdx > 0) {
                    currentId = header.substring(0, spaceIdx);
                    currentDesc = header.substring(spaceIdx + 1);
                } else {
                    currentId = header;
                    currentDesc = "";
                }

                currentSeq = new StringBuilder();
                if (!seqOnHeader.isEmpty()) {
                    currentSeq.append(seqOnHeader);
                }
            } else if (!trimmedLine.isEmpty() && currentSeq != null) {
                String seqLine = trimmedLine.replace("\t", "");
                currentSeq.append(seqLine.toUpperCase());
            }
        }

        if (currentId != null && currentSeq != null && currentSeq.length() > 0) {
            result.add(new SequenceRecord(currentId, currentSeq.toString(), currentDesc));
        }

        System.out.println("Parsed " + result.size() + " sequences from " + file.getName());
        return result;
    }

    private List<SequenceRecord> parseGenbank(File file) throws IOException {
        List<SequenceRecord> result = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        StringBuilder seq = new StringBuilder();
        String id = file.getName();
        String desc = "";
        boolean inSeq = false;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("LOCUS")) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length > 1) id = parts[1];
            } else if (line.startsWith("DEFINITION")) {
                desc = line.substring(12).trim();
            } else if (line.startsWith("ORIGIN")) {
                inSeq = true;
            } else if (inSeq) {
                if (line.startsWith("//")) break;
                line = line.replaceAll("[0-9]", "").trim();
                seq.append(line.toUpperCase());
            }
        }
        reader.close();
        if (seq.length() > 0) {
            result.add(new SequenceRecord(id, seq.toString(), desc));
        }
        return result;
    }

    private void onSave() {
        if (currentFile != null) {
            saveToFile(currentFile);
        } else {
            onSaveAs();
        }
    }

    private void onSaveAs() {
        JFileChooser chooser = new JFileChooser(currentFile != null ? currentFile.getParent() : ".");
        chooser.setFileFilter(new FileNameExtensionFilter("FASTA files (*.fasta)", "fasta"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().endsWith(".fasta")) {
                file = new File(file.getAbsolutePath() + ".fasta");
            }
            saveToFile(file);
        }
    }

    private void saveToFile(File file) {
        try {
            PrintWriter writer = new PrintWriter(file);
            for (SequenceRecord seq : sequences) {
                writer.println(">" + seq.id + (seq.description.isEmpty() ? "" : " " + seq.description));
                for (int i = 0; i < seq.sequence.length(); i += 60) {
                    writer.println(seq.sequence.substring(i, Math.min(i + 60, seq.sequence.length())));
                }
            }
            writer.close();
            currentFile = file;
            setStatus("Saved " + sequences.size() + " sequence(s) to " + file.getName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onExportFasta() { onSaveAs(); }

    private void onExportPhylip() {
        JFileChooser chooser = new JFileChooser(currentFile != null ? currentFile.getParent() : ".");
        chooser.setFileFilter(new FileNameExtensionFilter("PHYLIP files (*.phylip)", "phylip"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().endsWith(".phylip")) {
                file = new File(file.getAbsolutePath() + ".phylip");
            }
            try {
                PrintWriter writer = new PrintWriter(file);
                writer.println(sequences.size() + " " + getMaxSeqLength());
                for (SequenceRecord seq : sequences) {
                    String name = seq.id.length() > 10 ? seq.id.substring(0, 10) : seq.id;
                    writer.printf("%-10s  %s%n", name, seq.sequence);
                }
                writer.close();
                setStatus("Exported to PHYLIP format");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error exporting: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private int getMaxSeqLength() {
        int max = 0;
        for (SequenceRecord seq : sequences) {
            max = Math.max(max, seq.sequence.length());
        }
        return max;
    }

    private void onCut() { sequenceDisplay.cut(); }
    private void onCopy() { sequenceDisplay.copy(); }
    private void onPaste() { sequenceDisplay.paste(); }
    private void onSelectAll() { sequenceDisplay.selectAll(); }

    private void onInsertBases() {
        if (sequences.isEmpty()) return;
        String posStr = JOptionPane.showInputDialog(this, "Position (1-based):", "Insert Bases", JOptionPane.QUESTION_MESSAGE);
        if (posStr == null) return;
        String bases = JOptionPane.showInputDialog(this, "Bases to insert:", "Insert Bases", JOptionPane.QUESTION_MESSAGE);
        if (bases == null || bases.isEmpty()) return;

        try {
            int pos = Integer.parseInt(posStr) - 1;
            bases = bases.toUpperCase();
            for (SequenceRecord seq : sequences) {
                if (pos >= 0 && pos <= seq.sequence.length()) {
                    seq.sequence = seq.sequence.substring(0, pos) + bases + seq.sequence.substring(pos);
                }
            }
            refreshDisplay();
            setStatus("Inserted " + bases.length() + " bases at position " + posStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid position", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDeleteBases() {
        if (sequences.isEmpty()) return;
        String startStr = JOptionPane.showInputDialog(this, "Start position (1-based):", "Delete Bases", JOptionPane.QUESTION_MESSAGE);
        if (startStr == null) return;
        String endStr = JOptionPane.showInputDialog(this, "End position or number of bases:", "Delete Bases", JOptionPane.QUESTION_MESSAGE);

        try {
            int start = Integer.parseInt(startStr) - 1;
            int end;
            try {
                end = Integer.parseInt(endStr);
                if (end < start) end = start + 1;
            } catch (NumberFormatException e) {
                end = start + Integer.parseInt(endStr);
            }
            for (SequenceRecord seq : sequences) {
                if (start >= 0 && start < seq.sequence.length()) {
                    end = Math.min(end, seq.sequence.length());
                    seq.sequence = seq.sequence.substring(0, start) + seq.sequence.substring(end);
                }
            }
            refreshDisplay();
            setStatus("Deleted bases from position " + startStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid position", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onRenameSequence() {
        int idx = sequenceList.getSelectedIndex();
        if (idx >= 0 && idx < sequences.size()) {
            String newName = JOptionPane.showInputDialog(this, "New ID:", sequences.get(idx).id);
            if (newName != null && !newName.isEmpty()) {
                sequences.get(idx).id = newName;
                refreshDisplay();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a sequence first", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onRemoveSelectedSeq() {
        int idx = sequenceList.getSelectedIndex();
        if (idx >= 0 && idx < sequences.size()) {
            sequences.remove(idx);
            refreshDisplay();
        }
    }

    private void onDnaToRna() {
        if (sequences.isEmpty()) return;
        for (SequenceRecord seq : sequences) {
            seq.sequence = seq.sequence.replace('T', 'U').replace('t', 'u');
        }
        refreshDisplay();
        setStatus("Converted DNA to RNA");
    }

    private void onRnaToDna() {
        if (sequences.isEmpty()) return;
        for (SequenceRecord seq : sequences) {
            seq.sequence = seq.sequence.replace('U', 'T').replace('u', 't');
        }
        refreshDisplay();
        setStatus("Converted RNA to DNA");
    }

    private void onToAa() {
        if (sequences.isEmpty()) return;
        List<SequenceRecord> newSeqs = new ArrayList<>();
        for (SequenceRecord seq : sequences) {
            String protein = translateSequence(seq.sequence);
            newSeqs.add(new SequenceRecord(seq.id + "_translated", protein, "Translated sequence"));
        }
        sequences = newSeqs;
        refreshDisplay();
        setStatus("Converted sequences to amino acids");
    }

    private String translateSequence(String dna) {
        StringBuilder protein = new StringBuilder();
        for (int i = 0; i <= dna.length() - 3; i += 3) {
            String codon = dna.substring(i, Math.min(i + 3, dna.length()));
            if (codon.length() == 3) {
                char aa = CODON_TABLE.getOrDefault(codon, '?');
                protein.append(aa);
            }
        }
        return protein.toString();
    }

    private void onReverseComplement() {
        if (sequences.isEmpty()) return;
        for (SequenceRecord seq : sequences) {
            seq.sequence = reverseComplement(seq.sequence);
        }
        refreshDisplay();
        setStatus("Applied reverse complement");
    }

    private String reverseComplement(String seq) {
        StringBuilder sb = new StringBuilder();
        for (int i = seq.length() - 1; i >= 0; i--) {
            sb.append(complementChar(seq.charAt(i)));
        }
        return sb.toString();
    }

    private char complementChar(char c) {
        switch (Character.toUpperCase(c)) {
            case 'A': return 'T';
            case 'T': return 'A';
            case 'G': return 'C';
            case 'C': return 'G';
            case 'U': return 'A';
            default: return c;
        }
    }

    private void onComplement() {
        if (sequences.isEmpty()) return;
        for (SequenceRecord seq : sequences) {
            seq.sequence = complement(seq.sequence);
        }
        refreshDisplay();
        setStatus("Applied complement");
    }

    private String complement(String seq) {
        StringBuilder sb = new StringBuilder();
        for (char c : seq.toCharArray()) {
            sb.append(complementChar(c));
        }
        return sb.toString();
    }

    private void onReverse() {
        if (sequences.isEmpty()) return;
        for (SequenceRecord seq : sequences) {
            seq.sequence = new StringBuilder(seq.sequence).reverse().toString();
        }
        refreshDisplay();
        setStatus("Reversed sequences");
    }

    private void onSortSequences() {
        if (sequences.isEmpty()) return;
        sequences.sort((a, b) -> Integer.compare(b.sequence.length(), a.sequence.length()));
        refreshDisplay();
        setStatus("Sorted sequences by length");
    }

    private void onRemoveGaps() {
        if (sequences.isEmpty()) return;
        for (SequenceRecord seq : sequences) {
            seq.sequence = seq.sequence.replace("-", "");
        }
        refreshDisplay();
        setStatus("Removed gaps from sequences");
    }

    private void zoomIn() {
        if (currentFontSize < 20) {
            currentFontSize++;
            if (currentDisplayIndex >= 0) displaySequence(currentDisplayIndex);
            setStatus("Zoomed in: " + currentFontSize + "pt");
        }
    }

    private void zoomOut() {
        if (currentFontSize > 8) {
            currentFontSize--;
            if (currentDisplayIndex >= 0) displaySequence(currentDisplayIndex);
            setStatus("Zoomed out: " + currentFontSize + "pt");
        }
    }

    private void onCodonTable() {
        JDialog dialog = new CodonTableDialog(this);
        dialog.setVisible(true);
    }

    private void onComposition() {
        int idx = sequenceList.getSelectedIndex();
        if (idx < 0) idx = 0;
        if (idx >= 0 && idx < sequences.size()) {
            JDialog dialog = new CompositionDialog(this, sequences.get(idx));
            dialog.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a sequence", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSixFrame() {
        int idx = sequenceList.getSelectedIndex();
        if (idx < 0) idx = 0;
        if (idx >= 0 && idx < sequences.size()) {
            JDialog dialog = new SixFrameDialog(this, sequences.get(idx));
            dialog.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a sequence", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDotPlot() {
        if (sequences.size() < 2) {
            JOptionPane.showMessageDialog(this, "Need at least 2 sequences for dot plot", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JDialog dialog = new DotPlotDialog(this, sequences);
        dialog.setVisible(true);
    }

    private void onRestrictionMap() {
        int idx = sequenceList.getSelectedIndex();
        if (idx < 0) idx = 0;
        if (idx >= 0 && idx < sequences.size()) {
            JDialog dialog = new RestrictionDialog(this, sequences.get(idx));
            dialog.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a sequence", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onPlasmidDrawing() {
        JOptionPane.showMessageDialog(this, "Plasmid drawing feature coming soon!", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onPhyloTree() {
        if (sequences.size() < 2) {
            JOptionPane.showMessageDialog(this, "Need at least 2 sequences for tree", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JDialog dialog = new TreeDialog(this, sequences);
        dialog.setVisible(true);
    }

    private void onClustalAlignment() {
        if (sequences.size() < 2) {
            JOptionPane.showMessageDialog(this, "Need at least 2 sequences for alignment", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            Process p = Runtime.getRuntime().exec("which clustalo");
            if (p.waitFor() != 0) {
                JOptionPane.showMessageDialog(this,
                    "ClustalO not found. Install with: sudo apt install clustalo",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JDialog dialog = new ClustalDialog(this, sequences);
            dialog.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onManualAlignment() {
        JOptionPane.showMessageDialog(this, "Manual alignment editor coming soon!", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onBlastn() {
        int idx = sequenceList.getSelectedIndex();
        if (idx < 0) idx = 0;
        if (idx >= 0 && idx < sequences.size()) {
            JDialog dialog = new BlastDialog(this, sequences.get(idx), "blastn");
            dialog.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a sequence", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onBlastp() {
        int idx = sequenceList.getSelectedIndex();
        if (idx < 0) idx = 0;
        if (idx >= 0 && idx < sequences.size()) {
            JDialog dialog = new BlastDialog(this, sequences.get(idx), "blastp");
            dialog.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a sequence", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onBlastx() {
        int idx = sequenceList.getSelectedIndex();
        if (idx < 0) idx = 0;
        if (idx >= 0 && idx < sequences.size()) {
            JDialog dialog = new BlastDialog(this, sequences.get(idx), "blastx");
            dialog.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a sequence", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onAbout() {
        JOptionPane.showMessageDialog(this,
            "SeqEdit v1.0\n\n" +
            "Sequence Editor for Linux\n" +
            "Developed by Jonathan Chan\n\n" +
            "Affiliation: Institute of Health and Community Medicine\n\n" +
            "Features:\n" +
            "• FASTA and GenBank file support\n" +
            "• Single-sequence display with horizontal scrolling\n" +
            "• Navigation between sequences (arrow keys or buttons)\n" +
            "• Colored nucleotides (A=red, T=cyan, G=blue, C=green)\n" +
            "• Position ruler and zoom\n" +
            "• Reverse complement, translate, DNA/RNA conversion\n" +
            "• Sequence composition analysis\n" +
            "• Six-frame translation\n" +
            "• Dot plot analysis\n" +
            "• Restriction mapping\n" +
            "• Phylogenetic tree viewer\n" +
            "• ClustalO alignment\n" +
            "• BLAST integration",
            "About SeqEdit", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onQuit() {
        dispose();
        System.exit(0);
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void addPopupItem(JPopupMenu menu, String label, ActionListener listener) {
        JMenuItem item = new JMenuItem(label);
        item.setFont(new Font("Dialog", Font.PLAIN, 11));
        item.addActionListener(listener);
        menu.add(item);
    }

    private void filterSequences(String searchTerm) {
        if (sequences.isEmpty()) return;
        // Highlight matching items - for now just refresh
        if (searchTerm.isEmpty()) {
            refreshDisplay();
        }
    }

    class RulerPanel extends JPanel {
        public RulerPanel() {
            setPreferredSize(new Dimension(0, 30));
            setBackground(new Color(240, 240, 240));
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORLAND_DARK, 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
            ));
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!rulerMode) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            JScrollBar hBar = rightScrollPane.getHorizontalScrollBar();
            int scrollValue = hBar.getValue();
            int visibleWidth = hBar.getVisibleAmount();
            int maxValue = hBar.getMaximum();

            if (maxValue <= 0) return;

            int totalNucleotides = getMaxSeqLength();
            if (totalNucleotides <= 0) return;

            double nucPerPixel = (double) totalNucleotides / (double) maxValue;
            int startNuc = (int) (scrollValue * nucPerPixel);
            int endNuc = (int) ((scrollValue + visibleWidth) * nucPerPixel);

            g2.setFont(new Font("Monospaced", Font.PLAIN, 9));

            for (int pos = (startNuc / 10) * 10; pos <= endNuc + 10; pos += 10) {
                int x = (int) ((pos - startNuc) * (width / (double) (endNuc - startNuc + 1)));
                if (x >= 0 && x <= width) {
                    g2.drawLine(x, 18, x, 28);
                    if (pos % 60 == 0) {
                        g2.drawString(String.valueOf(pos + 1), x - 15, 15);
                    }
                }
            }
        }
    }

    static class SequenceRecord {
        String id;
        String sequence;
        String description;

        SequenceRecord(String id, String sequence, String description) {
            this.id = id;
            this.sequence = sequence != null ? sequence.toUpperCase() : "";
            this.description = description != null ? description : "";
        }
    }

    /**
     * Utility method to find tools in local bin/ directory or system PATH.
     * All dialogs should use this method to locate external tools.
     */
    public static String getToolPath(String toolName) {
        // Possible locations relative to working directory
        String[] possiblePaths = {
            "bin/" + toolName,
            "java/bin/" + toolName,
            "SeqEdit/bin/" + toolName,
            "../bin/" + toolName,
            "../java/bin/" + toolName,
            toolName  // Fall back to PATH
        };
        for (String path : possiblePaths) {
            File f = new File(path);
            if (f.exists() && f.canExecute()) return path;
        }
        // Try system PATH
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"which", toolName});
            if (p.waitFor() == 0) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String path = reader.readLine();
                if (path != null) return path.trim();
            }
        } catch (Exception e) {}
        return toolName; // Hope it's in PATH
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SeqEdit app = new SeqEdit();
            app.setVisible(true);
        });
    }
}
