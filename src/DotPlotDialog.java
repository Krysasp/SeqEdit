import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Dot Plot Dialog for pairwise sequence comparison
 */
public class DotPlotDialog extends JDialog {
    private List<SeqEdit.SequenceRecord> sequences;
    private JComboBox<String> seq1Combo;
    private JComboBox<String> seq2Combo;
    private DotPlotPanel plotPanel;
    private JLabel statsLabel;

    public DotPlotDialog(Frame parent, List<SeqEdit.SequenceRecord> sequences) {
        super(parent, "Dot Plot Analysis", false);
        this.sequences = sequences;
        setSize(900, 700);
        setLayout(new BorderLayout());
        createWidgets();
    }

    private void createWidgets() {
        if (sequences.size() < 2) {
            add(new JLabel("Need at least 2 sequences for dot plot", SwingConstants.CENTER), BorderLayout.CENTER);
            JButton closeBtn = new JButton("Close");
            closeBtn.addActionListener(e -> dispose());
            add(closeBtn, BorderLayout.SOUTH);
            return;
        }

        // Options panel
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        optionsPanel.add(new JLabel("Sequence 1:"));
        seq1Combo = new JComboBox<>();
        seq2Combo = new JComboBox<>();
        for (SeqEdit.SequenceRecord seq : sequences) {
            seq1Combo.addItem(seq.id);
            seq2Combo.addItem(seq.id);
        }
        if (sequences.size() >= 2) {
            seq2Combo.setSelectedIndex(1);
        }
        optionsPanel.add(seq1Combo);
        optionsPanel.add(Box.createHorizontalStrut(20));
        optionsPanel.add(new JLabel("Sequence 2:"));
        optionsPanel.add(seq2Combo);

        JButton plotBtn = new JButton("Generate Plot");
        plotBtn.addActionListener(e -> generatePlot());
        optionsPanel.add(plotBtn);

        add(optionsPanel, BorderLayout.NORTH);

        // Plot panel
        plotPanel = new DotPlotPanel();
        add(plotPanel, BorderLayout.CENTER);

        // Stats panel
        statsLabel = new JLabel("Select sequences and click 'Generate Plot'");
        statsLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(statsLabel, BorderLayout.SOUTH);

        // Close button
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        btnPanel.add(closeBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void generatePlot() {
        int idx1 = seq1Combo.getSelectedIndex();
        int idx2 = seq2Combo.getSelectedIndex();

        if (idx1 < 0 || idx2 < 0 || idx1 == idx2) {
            JOptionPane.showMessageDialog(this, "Please select two different sequences", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String seq1 = sequences.get(idx1).sequence.toUpperCase();
        String seq2 = sequences.get(idx2).sequence.toUpperCase();

        plotPanel.setSequences(seq1, seq2);
        plotPanel.repaint();

        // Calculate stats
        int matches = 0;
        int total = Math.min(seq1.length(), seq2.length());
        for (int i = 0; i < total; i++) {
            if (seq1.charAt(i) == seq2.charAt(i)) matches++;
        }

        statsLabel.setText(String.format(
            "Sequence 1: %s (%d bp) | Sequence 2: %s (%d bp) | Identity: %.2f%%",
            sequences.get(idx1).id, seq1.length(),
            sequences.get(idx2).id, seq2.length(),
            total > 0 ? (matches * 100.0 / total) : 0
        ));
    }

    class DotPlotPanel extends JPanel {
        private String seq1 = "";
        private String seq2 = "";
        private static final int DOT_SIZE = 3;
        private static final int MARGIN = 60;

        public void setSequences(String s1, String s2) {
            this.seq1 = s1;
            this.seq2 = s2;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (seq1.isEmpty() || seq2.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            int plotWidth = width - 2 * MARGIN;
            int plotHeight = height - 2 * MARGIN;

            // Draw axes
            g2.drawLine(MARGIN, MARGIN, MARGIN, height - MARGIN);
            g2.drawLine(MARGIN, height - MARGIN, width - MARGIN, height - MARGIN);

            // Labels
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.drawString("5'", MARGIN - 15, height - MARGIN + 15);
            g2.drawString("3'", width - MARGIN + 5, height - MARGIN + 15);
            g2.drawString("5'", MARGIN - 30, MARGIN - 5);
            g2.drawString("3'", MARGIN - 30, MARGIN + 15 + plotHeight);

            if (seq1.isEmpty() || seq2.isEmpty()) return;

            // Draw dots for matches
            g2.setColor(Color.BLUE);
            int len1 = seq1.length();
            int len2 = seq2.length();

            int step1 = Math.max(1, len1 / (plotWidth / 2));
            int step2 = Math.max(1, len2 / (plotHeight / 2));

            for (int i = 0; i < len1; i += step1) {
                for (int j = 0; j < len2; j += step2) {
                    if (seq1.charAt(i) == seq2.charAt(j)) {
                        int x = MARGIN + (i * plotWidth / len1);
                        int y = height - MARGIN - (j * plotHeight / len2);
                        g2.fillRect(x, y, DOT_SIZE, DOT_SIZE);
                    }
                }
            }

            // Title
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.drawString("Dot Plot: " + seq1.length() + " x " + seq2.length() + " bp", width / 2 - 100, 20);
        }
    }
}
