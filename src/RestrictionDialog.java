import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Restriction Mapping Dialog
 */
public class RestrictionDialog extends JDialog {
    private SeqEdit.SequenceRecord sequence;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> enzymeCombo;

    // Common restriction enzymes with recognition sites
    private static final Map<String, String> ENZYMES = new LinkedHashMap<>();
    static {
        ENZYMES.put("EcoRI", "GAATTC");
        ENZYMES.put("BamHI", "GGATCC");
        ENZYMES.put("HindIII", "AAGCTT");
        ENZYMES.put("SmaI", "CCCGGG");
        ENZYMES.put("PstI", "CTGCAG");
        ENZYMES.put("SalI", "GTCGAC");
        ENZYMES.put("XhoI", "CTCGAG");
        ENZYMES.put("KpnI", "GGTACC");
        ENZYMES.put("SacI", "GAGCTC");
        ENZYMES.put("NotI", "GCGGCCGC");
        ENZYMES.put("All (Scan All)", "");
    }

    public RestrictionDialog(Frame parent, SeqEdit.SequenceRecord sequence) {
        super(parent, "Restriction Mapping - " + sequence.id, false);
        this.sequence = sequence;
        setSize(800, 600);
        setLayout(new BorderLayout());
        createWidgets();
    }

    private void createWidgets() {
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        infoPanel.add(new JLabel("<html><b>Sequence:</b> " + sequence.id + " &nbsp; <b>Length:</b> " + sequence.sequence.length() + " bp</html>"));
        add(infoPanel, BorderLayout.NORTH);

        // Options panel
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        optionsPanel.add(new JLabel("Enzyme:"));
        enzymeCombo = new JComboBox<>(ENZYMES.keySet().toArray(new String[0]));
        optionsPanel.add(enzymeCombo);

        JButton scanBtn = new JButton("Scan");
        scanBtn.addActionListener(e -> scanEnzymes());
        optionsPanel.add(scanBtn);

        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> {
            tableModel.setRowCount(0);
        });
        optionsPanel.add(clearBtn);

        add(optionsPanel, BorderLayout.NORTH);

        // Results table
        String[] columns = {"Enzyme", "Site", "Position", "Length", "3' Overhang", "5' Overhang"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        resultTable = new JTable(tableModel);
        resultTable.setRowHeight(22);
        resultTable.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(resultTable);
        add(scrollPane, BorderLayout.CENTER);

        // Stats panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statsPanel.add(new JLabel("Select enzyme and click 'Scan'"));

        // Fragment visualization
        JPanel fragPanel = createFragmentPanel();

        add(statsPanel, BorderLayout.SOUTH);

        // Close button
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        btnPanel.add(closeBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // Initial scan with EcoRI
        scanEnzymes();
    }

    private JPanel createFragmentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Fragment Sizes"));
        panel.setPreferredSize(new Dimension(0, 100));

        JTextArea fragArea = new JTextArea();
        fragArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        fragArea.setEditable(false);
        panel.add(new JScrollPane(fragArea), BorderLayout.CENTER);

        return panel;
    }

    private void scanEnzymes() {
        tableModel.setRowCount(0);
        String selected = (String) enzymeCombo.getSelectedItem();
        String seq = sequence.sequence.toUpperCase();

        if ("All (Scan All)".equals(selected)) {
            // Scan all enzymes
            for (Map.Entry<String, String> entry : ENZYMES.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    findSites(entry.getKey(), entry.getValue(), seq);
                }
            }
        } else {
            String site = ENZYMES.get(selected);
            if (site != null) {
                findSites(selected, site, seq);
            }
        }

        // Update stats - find and update the stats label
        Component[] comps = getContentPane().getComponents();
        for (Component c : comps) {
            if (c instanceof JPanel) {
                for (Component inner : ((JPanel)c).getComponents()) {
                    if (inner instanceof JLabel) {
                        ((JLabel)inner).setText("Found " + tableModel.getRowCount() + " restriction site(s)");
                        break;
                    }
                }
            }
        }
    }

    private void findSites(String enzyme, String site, String seq) {
        if (site.isEmpty()) return;

        int pos = 0;
        while ((pos = seq.indexOf(site, pos)) != -1) {
            String overhang3 = get3Overhang(site);
            String overhang5 = get5Overhang(site);
            Object[] row = {enzyme, site, pos + 1, site.length(), overhang3, overhang5};
            tableModel.addRow(row);
            pos += 1;
        }
    }

    private String get3Overhang(String site) {
        // Simple overhang calculation
        int mid = site.length() / 2;
        return site.substring(mid) + " (" + (site.length() - mid) + " bp)";
    }

    private String get5Overhang(String site) {
        int mid = site.length() / 2;
        return site.substring(0, mid) + " (" + mid + " bp)";
    }
}
