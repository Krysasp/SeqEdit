import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.*;

/**
 * Six-Frame Translation Dialog
 */
public class SixFrameDialog extends JDialog {
    private SeqEdit.SequenceRecord sequence;
    private JTextPane[] framePanes;
    private static final String[] FRAME_NAMES = {"Frame 1 (+1)", "Frame 2 (+2)", "Frame 3 (+3)",
                                                   "Frame 4 (-1)", "Frame 5 (-2)", "Frame 6 (-3)"};

    // Amino acid colors
    private static final Map<Character, Color> AA_COLORS = new HashMap<>();

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
    }

    public SixFrameDialog(Frame parent, SeqEdit.SequenceRecord sequence) {
        super(parent, "Six-Frame Translation - " + sequence.id, false);
        this.sequence = sequence;
        setSize(1000, 800);
        setLayout(new BorderLayout());
        createWidgets();
    }

    private void createWidgets() {
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        infoPanel.add(new JLabel("<html><b>Sequence:</b> " + sequence.id + " &nbsp; <b>Length:</b> " + sequence.sequence.length() + " bp</html>"));
        add(infoPanel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();

        // Create forward frame tabs
        for (int i = 0; i < 3; i++) {
            JPanel panel = createFramePanel(i, true);
            tabbedPane.addTab(FRAME_NAMES[i], panel);
        }

        // Create reverse frame tabs
        for (int i = 0; i < 3; i++) {
            JPanel panel = createFramePanel(i, false);
            tabbedPane.addTab(FRAME_NAMES[i + 3], panel);
        }

        add(tabbedPane, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        btnPanel.add(closeBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private JPanel createFramePanel(int frameOffset, boolean forward) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextPane textPane = new JTextPane();
        textPane.setFont(new Font("Monospaced", Font.PLAIN, 11));
        textPane.setEditable(false);
        textPane.setBackground(new Color(250, 250, 250));

        displayFrame(textPane, frameOffset, forward);

        JScrollPane scrollPane = new JScrollPane(textPane);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void displayFrame(JTextPane textPane, int frameOffset, boolean forward) {
        StyledDocument doc = textPane.getStyledDocument();
        String seq = sequence.sequence.toUpperCase();

        if (!forward) {
            seq = reverseComplement(seq);
        }

        // Skip to frame offset
        String workingSeq = (frameOffset < seq.length()) ? seq.substring(frameOffset) : "";

        try {
            int lineLength = 60;
            for (int i = 0; i < workingSeq.length(); i += lineLength) {
                // Position label
                int pos = i + frameOffset + (forward ? 0 : 0);
                SimpleAttributeSet posAttrs = new SimpleAttributeSet();
                StyleConstants.setForeground(posAttrs, Color.GRAY);
                StyleConstants.setFontSize(posAttrs, 10);
                doc.insertString(doc.getLength(), String.format("%8d  ", pos + 1), posAttrs);

                // DNA line
                String dnaChunk = workingSeq.substring(i, Math.min(i + lineLength, workingSeq.length()));
                SimpleAttributeSet dnaAttrs = new SimpleAttributeSet();
                StyleConstants.setForeground(dnaAttrs, Color.BLACK);
                StyleConstants.setFontSize(dnaAttrs, 11);
                doc.insertString(doc.getLength(), dnaChunk + "\n", dnaAttrs);

                // Protein line (translate)
                int proteinStart = i;
                StringBuilder protein = new StringBuilder();
                for (int j = 0; j < dnaChunk.length() - 2; j += 3) {
                    String codon = dnaChunk.substring(j, Math.min(j + 3, dnaChunk.length()));
                    if (codon.length() == 3) {
                        char aa = SeqEdit.CODON_TABLE.getOrDefault(codon, '?');
                        protein.append(aa);
                    }
                }

                SimpleAttributeSet spaceAttrs = new SimpleAttributeSet();
                StyleConstants.setFontSize(spaceAttrs, 11);
                doc.insertString(doc.getLength(), "         ", spaceAttrs);

                // Color the amino acids
                for (int j = 0; j < protein.length(); j++) {
                    char aa = protein.charAt(j);
                    SimpleAttributeSet aaAttrs = new SimpleAttributeSet();
                    StyleConstants.setFontSize(aaAttrs, 11);
                    StyleConstants.setForeground(aaAttrs, AA_COLORS.getOrDefault(aa, Color.BLACK));
                    if (aa == '*') {
                        StyleConstants.setBold(aaAttrs, true);
                    }
                    doc.insertString(doc.getLength(), String.valueOf(aa), aaAttrs);
                }

                doc.insertString(doc.getLength(), "\n\n", new SimpleAttributeSet());
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private String reverseComplement(String seq) {
        StringBuilder sb = new StringBuilder();
        for (int i = seq.length() - 1; i >= 0; i--) {
            char c = seq.charAt(i);
            switch (c) {
                case 'A': sb.append('T'); break;
                case 'T': sb.append('A'); break;
                case 'G': sb.append('C'); break;
                case 'C': sb.append('G'); break;
                case 'U': sb.append('A'); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }
}
