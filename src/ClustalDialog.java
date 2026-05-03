import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * ClustalO Alignment Dialog
 */
public class ClustalDialog extends JDialog {
    private List<SeqEdit.SequenceRecord> sequences;
    private JComboBox<String> outputFormatCombo;
    private JTextArea outputArea;
    private List<SeqEdit.SequenceRecord> resultSequences;

    public ClustalDialog(Frame parent, List<SeqEdit.SequenceRecord> sequences) {
        super(parent, "ClustalO Alignment", true);
        this.sequences = sequences;
        this.resultSequences = new ArrayList<>();
        setSize(900, 700);
        setLayout(new BorderLayout());
        createWidgets();
    }

    private void createWidgets() {
        if (sequences.size() < 2) {
            add(new JLabel("Need at least 2 sequences for alignment", SwingConstants.CENTER), BorderLayout.CENTER);
            JButton closeBtn = new JButton("Close");
            closeBtn.addActionListener(e -> dispose());
            add(closeBtn, BorderLayout.SOUTH);
            return;
        }

        // Options panel
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        optionsPanel.add(new JLabel("Output format:"));
        outputFormatCombo = new JComboBox<>(new String[]{"FASTA", "Clustal", "PHYLIP", "NEXUS"});
        optionsPanel.add(outputFormatCombo);

        optionsPanel.add(Box.createHorizontalStrut(20));
        optionsPanel.add(new JLabel("Sequences: " + sequences.size()));

        JButton alignBtn = new JButton("Run ClustalO");
        alignBtn.addActionListener(e -> runClustal());
        optionsPanel.add(alignBtn);

        add(optionsPanel, BorderLayout.NORTH);

        // Output area
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);

        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusPanel.add(new JLabel("Click 'Run ClustalO' to start alignment"));
        add(statusPanel, BorderLayout.SOUTH);

        // Button panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        JButton loadBtn = new JButton("Load Alignment");
        loadBtn.addActionListener(e -> loadAlignment());
        loadBtn.setEnabled(false);
        btnPanel.add(loadBtn);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        btnPanel.add(closeBtn);

        add(btnPanel, BorderLayout.SOUTH);
    }

    private void runClustal() {
        outputArea.setText("Running ClustalO alignment...\n");
        outputArea.repaint();

        try {
            // Get clustalo path
            String clustaloPath = SeqEdit.getToolPath("clustalo");
            outputArea.append("Using ClustalO: " + clustaloPath + "\n");

            // Write sequences to temp file
            File tempInput = File.createTempFile("seqedit_clustal_", ".fasta");
            PrintWriter writer = new PrintWriter(tempInput);
            for (SeqEdit.SequenceRecord seq : sequences) {
                writer.println(">" + seq.id);
                for (int i = 0; i < seq.sequence.length(); i += 60) {
                    writer.println(seq.sequence.substring(i, Math.min(i + 60, seq.sequence.length())));
                }
            }
            writer.close();

            File tempOutput = File.createTempFile("seqedit_aligned_", ".fasta");

            // Run clustalo (delete output file first to avoid overwrite error)
            if (tempOutput.exists()) tempOutput.delete();
            String format = ((String) outputFormatCombo.getSelectedItem()).toLowerCase();
            ProcessBuilder pb = new ProcessBuilder(
                clustaloPath,
                "-i", tempInput.getAbsolutePath(),
                "-o", tempOutput.getAbsolutePath(),
                "--outfmt", format,
                "--force"
            );

            Process p = pb.start();

            // Read output
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = p.waitFor();

            if (exitCode == 0 && tempOutput.exists()) {
                // Read aligned sequences
                resultSequences = readFastaFile(tempOutput);
                outputArea.append("Alignment completed successfully!\n");
                outputArea.append("Aligned " + resultSequences.size() + " sequences\n\n");

                // Display alignment preview
                outputArea.append("=== Alignment Preview ===\n");
                for (SeqEdit.SequenceRecord seq : resultSequences) {
                    outputArea.append(">" + seq.id + " (" + seq.sequence.length() + " bp)\n");
                    for (int i = 0; i < Math.min(120, seq.sequence.length()); i += 60) {
                        outputArea.append(seq.sequence.substring(i, Math.min(i + 60, seq.sequence.length())) + "\n");
                    }
                    outputArea.append("\n");
                }
            } else {
                outputArea.append("ERROR: ClustalO failed with exit code " + exitCode + "\n");
                StringBuilder error = new StringBuilder();
                while ((line = errorReader.readLine()) != null) {
                    error.append(line).append("\n");
                }
                outputArea.append("Error: " + error.toString());
            }

            tempInput.delete();
            tempOutput.delete();

        } catch (Exception e) {
            outputArea.append("ERROR: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    private List<SeqEdit.SequenceRecord> readFastaFile(File file) {
        List<SeqEdit.SequenceRecord> result = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            String currentId = "";
            StringBuilder currentSeq = new StringBuilder();
            String currentDesc = "";

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith(">")) {
                    if (!currentId.isEmpty() && currentSeq.length() > 0) {
                        result.add(new SeqEdit.SequenceRecord(currentId, currentSeq.toString(), currentDesc));
                    }
                    String header = line.substring(1).trim();
                    int spaceIdx = header.indexOf(' ');
                    if (spaceIdx > 0) {
                        currentId = header.substring(0, spaceIdx);
                        currentDesc = header.substring(spaceIdx + 1);
                    } else {
                        currentId = header;
                    }
                    currentSeq = new StringBuilder();
                } else if (!line.isEmpty()) {
                    currentSeq.append(line.toUpperCase());
                }
            }
            if (!currentId.isEmpty() && currentSeq.length() > 0) {
                result.add(new SeqEdit.SequenceRecord(currentId, currentSeq.toString(), currentDesc));
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private void loadAlignment() {
        if (!resultSequences.isEmpty()) {
            // This would need to update the parent's sequences
            // For now, just show a message
            JOptionPane.showMessageDialog(this,
                "Alignment loaded. Use 'Save' to export the aligned sequences.",
                "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public List<SeqEdit.SequenceRecord> getResult() {
        return resultSequences;
    }
}
