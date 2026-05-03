import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Codon Table Dialog - Displays standard genetic code
 */
public class CodonTableDialog extends JDialog {
    // Standard genetic code
    private static final String[][] CODONS = {
        {"TTT", "F", "Phe"}, {"TTC", "F", "Phe"}, {"TTA", "L", "Leu"}, {"TTG", "L", "Leu"},
        {"CTT", "L", "Leu"}, {"CTC", "L", "Leu"}, {"CTA", "L", "Leu"}, {"CTG", "L", "Leu"},
        {"ATT", "I", "Ile"}, {"ATC", "I", "Ile"}, {"ATA", "I", "Ile"}, {"ATG", "M", "Met"},
        {"GTT", "V", "Val"}, {"GTC", "V", "Val"}, {"GTA", "V", "Val"}, {"GTG", "V", "Val"},
        {"TCT", "S", "Ser"}, {"TCC", "S", "Ser"}, {"TCA", "S", "Ser"}, {"TCG", "S", "Ser"},
        {"CCT", "P", "Pro"}, {"CCC", "P", "Pro"}, {"CCA", "P", "Pro"}, {"CCG", "P", "Pro"},
        {"ACT", "T", "Thr"}, {"ACC", "T", "Thr"}, {"ACA", "T", "Thr"}, {"ACG", "T", "Thr"},
        {"GCT", "A", "Ala"}, {"GCC", "A", "Ala"}, {"GCA", "A", "Ala"}, {"GCG", "A", "Ala"},
        {"TAT", "Y", "Tyr"}, {"TAC", "Y", "Tyr"}, {"TAA", "*", "Stop"}, {"TAG", "*", "Stop"},
        {"CAT", "H", "His"}, {"CAC", "H", "His"}, {"CAA", "Q", "Gln"}, {"CAG", "Q", "Gln"},
        {"AAT", "N", "Asn"}, {"AAC", "N", "Asn"}, {"AAA", "K", "Lys"}, {"AAG", "K", "Lys"},
        {"GAT", "D", "Asp"}, {"GAC", "D", "Asp"}, {"GAA", "E", "Glu"}, {"GAG", "E", "Glu"},
        {"TGT", "C", "Cys"}, {"TGC", "C", "Cys"}, {"TGA", "*", "Stop"}, {"TGG", "W", "Trp"},
        {"CGT", "R", "Arg"}, {"CGC", "R", "Arg"}, {"CGA", "R", "Arg"}, {"CGG", "R", "Arg"},
        {"AGT", "S", "Ser"}, {"AGC", "S", "Ser"}, {"AGA", "R", "Arg"}, {"AGG", "R", "Arg"},
        {"GGT", "G", "Gly"}, {"GGC", "G", "Gly"}, {"GGA", "G", "Gly"}, {"GGG", "G", "Gly"},
    };

    // Amino acid colors
    private static final Map<String, Color> AA_COLORS = new HashMap<>();
    static {
        AA_COLORS.put("A", new Color(200, 200, 200));
        AA_COLORS.put("R", new Color(20, 90, 255));
        AA_COLORS.put("N", new Color(0, 220, 220));
        AA_COLORS.put("D", new Color(230, 10, 10));
        AA_COLORS.put("C", new Color(230, 230, 0));
        AA_COLORS.put("Q", new Color(0, 220, 220));
        AA_COLORS.put("E", new Color(230, 10, 10));
        AA_COLORS.put("G", new Color(235, 235, 235));
        AA_COLORS.put("H", new Color(130, 130, 211));
        AA_COLORS.put("I", new Color(15, 130, 15));
        AA_COLORS.put("L", new Color(15, 130, 15));
        AA_COLORS.put("K", new Color(20, 90, 255));
        AA_COLORS.put("M", new Color(230, 230, 0));
        AA_COLORS.put("F", new Color(50, 50, 170));
        AA_COLORS.put("P", new Color(220, 150, 130));
        AA_COLORS.put("S", new Color(250, 150, 0));
        AA_COLORS.put("T", new Color(250, 150, 0));
        AA_COLORS.put("W", new Color(180, 90, 180));
        AA_COLORS.put("Y", new Color(140, 140, 0));
        AA_COLORS.put("V", new Color(15, 130, 15));
        AA_COLORS.put("*", new Color(255, 217, 61));
    }

    public CodonTableDialog(Frame parent) {
        super(parent, "Codon Table (Standard Genetic Code)", false);
        setSize(800, 600);
        setLayout(new BorderLayout());
        createWidgets();
    }

    private void createWidgets() {
        JTabbedPane tabbedPane = new JTabbedPane();

        // Codon table tab
        tabbedPane.addTab("Codon Table", createCodonTablePanel());

        // Sequence analysis tab
        tabbedPane.addTab("Usage Notes", createNotesPanel());

        add(tabbedPane, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        btnPanel.add(closeBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private JPanel createCodonTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Codon", "AA", "Name", "Codon", "AA", "Name",
                           "Codon", "AA", "Name", "Codon", "AA", "Name"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        // Add codons in rows of 4 columns each
        for (int i = 0; i < CODONS.length; i += 4) {
            Object[] row = new Object[12];
            for (int j = 0; j < 4 && (i + j) < CODONS.length; j++) {
                row[j * 3] = CODONS[i + j][0];
                row[j * 3 + 1] = CODONS[i + j][1];
                row[j * 3 + 2] = CODONS[i + j][2];
            }
            model.addRow(row);
        }

        JTable table = new JTable(model);
        table.setRowHeight(25);
        table.setFont(new Font("Monospaced", Font.PLAIN, 11));
        table.getTableHeader().setVisible(false);

        // Color the amino acid column
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    if (column % 3 == 1 && value != null) {
                        String aa = value.toString();
                        c.setForeground(AA_COLORS.getOrDefault(aa, Color.BLACK));
                        setFont(new Font("Monospaced", Font.BOLD, 11));
                    } else {
                        c.setForeground(Color.BLACK);
                        setFont(new Font("Monospaced", Font.PLAIN, 11));
                    }
                }
                setHorizontalAlignment(column % 3 == 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
                return c;
            }
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createNotesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea notes = new JTextArea();
        notes.setFont(new Font("Monospaced", Font.PLAIN, 11));
        notes.setEditable(false);
        notes.setText(
            "Standard Genetic Code (NCBI)\n" +
            "==========================\n\n" +
            "Amino Acids and Their Codes:\n" +
            "  A = Alanine      R = Arginine     N = Asparagine\n" +
            "  D = Aspartic      C = Cysteine     Q = Glutamine\n" +
            "  E = Glutamic      G = Glycine      H = Histidine\n" +
            "  I = Isoleucine    L = Leucine      K = Lysine\n" +
            "  M = Methionine    F = Phenylalanine P = Proline\n" +
            "  S = Serine        T = Threonine    W = Tryptophan\n" +
            "  Y = Tyrosine      V = Valine        * = Stop codon\n\n" +
            "Start Codons: ATG (Met)\n" +
            "Stop Codons: TAA, TAG, TGA\n\n" +
            "Note: This is the standard genetic code used by most\n" +
            "organisms. Mitochondrial and some prokaryotic organisms\n" +
            "may use variant genetic codes.\n"
        );
        panel.add(new JScrollPane(notes), BorderLayout.CENTER);
        return panel;
    }
}
