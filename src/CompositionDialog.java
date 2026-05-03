import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;

/**
 * Sequence Composition Analysis Dialog
 */
public class CompositionDialog extends JDialog {
    private SeqEdit.SequenceRecord sequence;

    public CompositionDialog(Frame parent, SeqEdit.SequenceRecord sequence) {
        super(parent, "Sequence Composition - " + sequence.id, false);
        this.sequence = sequence;
        setSize(600, 500);
        setLayout(new BorderLayout());
        createWidgets();
    }

    private void createWidgets() {
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        infoPanel.add(new JLabel("<html><b>Sequence:</b> " + sequence.id +
                " &nbsp; <b>Length:</b> " + sequence.sequence.length() + " bp</html>"));
        add(infoPanel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();

        // Nucleotide composition tab
        tabbedPane.addTab("Nucleotide Composition", createNucleotidePanel());

        // Amino acid composition tab (if translated)
        tabbedPane.addTab("Amino Acid Composition", createAAPanel());

        add(tabbedPane, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        btnPanel.add(closeBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private JPanel createNucleotidePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String seq = sequence.sequence.toUpperCase();
        int[] counts = new int[256];
        char[] displayNucl = {'A', 'T', 'G', 'C', 'N', 'U', '-'};
        int otherCount = 0;

        for (char c : seq.toCharArray()) {
            if (c >= 0 && c < 256) {
                if (c == 'A' || c == 'T' || c == 'G' || c == 'C' || c == 'N' || c == 'U' || c == '-') {
                    counts[c]++;
                } else {
                    otherCount++;
                }
            }
        }

        int total = seq.length();

        String[] columns = {"Nucleotide", "Count", "Percentage", "Color"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        Color[] colors = {
            new Color(255, 107, 107), new Color(78, 205, 196), new Color(69, 183, 209),
            new Color(150, 206, 180), new Color(204, 204, 204), new Color(78, 205, 196),
            new Color(238, 238, 238), Color.GRAY
        };

        for (int i = 0; i < displayNucl.length; i++) {
            char c = displayNucl[i];
            int count = counts[c];
            double pct = total > 0 ? (count * 100.0 / total) : 0;
            Object[] row = {String.valueOf(c), count, String.format("%.2f%%", pct), ""};
            model.addRow(row);
        }

        // Add "Other" row
        double otherPct = total > 0 ? (otherCount * 100.0 / total) : 0;
        model.addRow(new Object[]{"Other", otherCount, String.format("%.2f%%", otherPct), ""});

        JTable table = new JTable(model);
        table.setRowHeight(25);

        // Set row colors using a custom renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected && row < colors.length) {
                    c.setBackground(colors[row]);
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Summary
        int a = counts['A'];
        int t = counts['T'];
        int g = counts['G'];
        int c = counts['C'];
        double gc = total > 0 ? ((g + c) * 100.0 / total) : 0;

        JPanel summary = new JPanel(new GridLayout(2, 3, 10, 5));
        summary.setBorder(BorderFactory.createTitledBorder("Summary"));
        summary.add(new JLabel("Total length: " + total + " bp"));
        summary.add(new JLabel("GC content: " + String.format("%.2f%%", gc)));
        summary.add(new JLabel("AT content: " + String.format("%.2f%%", 100 - gc)));
        summary.add(new JLabel("A: " + a + "  T: " + t));
        summary.add(new JLabel("G: " + g + "  C: " + c));
        summary.add(new JLabel("A+T/G+C ratio: " + String.format("%.2f", (a + t) / (double)(g + c + 1))));

        panel.add(summary, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createAAPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Translate sequence
        String protein = translateSequence(sequence.sequence);
        int[] counts = new int[256];

        String aas = "ACDEFGHIKLMNPQRSTVWY*";
        for (char aa : protein.toCharArray()) {
            if (aa >= 0 && aa < 256) {
                counts[aa]++;
            }
        }

        int total = protein.length();

        String[] columns = {"Amino Acid", "Code", "Count", "Percentage", "Color"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        Map<Character, String> aaNames = new HashMap<>();
        aaNames.put('A', "Ala"); aaNames.put('R', "Arg"); aaNames.put('N', "Asn");
        aaNames.put('D', "Asp"); aaNames.put('C', "Cys"); aaNames.put('Q', "Gln");
        aaNames.put('E', "Glu"); aaNames.put('G', "Gly"); aaNames.put('H', "His");
        aaNames.put('I', "Ile"); aaNames.put('L', "Leu"); aaNames.put('K', "Lys");
        aaNames.put('M', "Met"); aaNames.put('F', "Phe"); aaNames.put('P', "Pro");
        aaNames.put('S', "Ser"); aaNames.put('T', "Thr"); aaNames.put('W', "Trp");
        aaNames.put('Y', "Tyr"); aaNames.put('V', "Val"); aaNames.put('*', "Stop");

        for (char aa : aas.toCharArray()) {
            int count = counts[aa];
            double pct = total > 0 ? (count * 100.0 / total) : 0;
            Object[] row = {aaNames.get(aa), String.valueOf(aa), count, String.format("%.2f%%", pct), ""};
            model.addRow(row);
        }

        JTable table = new JTable(model);
        table.setRowHeight(20);

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Summary
        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT));
        summary.setBorder(BorderFactory.createTitledBorder("Translation Summary"));
        summary.add(new JLabel("Protein length: " + total + " aa  |  "));
        summary.add(new JLabel("Molecular weight (approx): " + calculateMW(protein) + " Da"));
        panel.add(summary, BorderLayout.SOUTH);

        return panel;
    }

    private String translateSequence(String dna) {
        StringBuilder protein = new StringBuilder();
        String seq = dna.toUpperCase();
        for (int i = 0; i <= seq.length() - 3; i += 3) {
            String codon = seq.substring(i, Math.min(i + 3, seq.length()));
            if (codon.length() == 3) {
                protein.append(SeqEdit.CODON_TABLE.getOrDefault(codon, '?'));
            }
        }
        return protein.toString();
    }

    private double calculateMW(String protein) {
        // Approximate molecular weight calculation
        Map<Character, Double> weights = new HashMap<>();
        weights.put('A', 89.1); weights.put('R', 174.2); weights.put('N', 132.1);
        weights.put('D', 133.1); weights.put('C', 121.2); weights.put('Q', 146.2);
        weights.put('E', 147.1); weights.put('G', 75.1); weights.put('H', 155.2);
        weights.put('I', 131.2); weights.put('L', 131.2); weights.put('K', 146.2);
        weights.put('M', 149.2); weights.put('F', 165.2); weights.put('P', 115.1);
        weights.put('S', 105.1); weights.put('T', 119.1); weights.put('W', 204.2);
        weights.put('Y', 181.2); weights.put('V', 117.1);

        double mw = 18.0; // water
        for (char aa : protein.toCharArray()) {
            mw += weights.getOrDefault(aa, 0.0);
        }
        return Math.round(mw * 10) / 10.0;
    }
}
