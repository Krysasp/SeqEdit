import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

/**
 * BLAST Dialog for sequence similarity searching
 * Uses local bin/ tools (blastn, blastp, blastx)
 */
public class BlastDialog extends JDialog {
    private SeqEdit.SequenceRecord sequence;
    private String blastType;
    private JTextArea outputArea;
    private JComboBox<String> databaseCombo;
    private JTextField evalueField;
    private JCheckBox remoteCheck;

    public BlastDialog(Frame parent, SeqEdit.SequenceRecord sequence, String blastType) {
        super(parent, "BLAST Search - " + sequence.id, false);
        this.sequence = sequence;
        this.blastType = blastType;
        setSize(1000, 700);
        setLayout(new BorderLayout());
        createWidgets();
    }

    private void createWidgets() {
        // Info panel
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        infoPanel.add(new JLabel("<html><b>Sequence:</b> " + sequence.id +
            " &nbsp; <b>Length:</b> " + sequence.sequence.length() +
            " bp &nbsp; <b>BLAST Type:</b> " + blastType.toUpperCase() + "</html>"));
        add(infoPanel, BorderLayout.NORTH);

        // Options panel
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        optionsPanel.add(new JLabel("Database:"));
        databaseCombo = new JComboBox<>(getDatabases());
        optionsPanel.add(databaseCombo);

        optionsPanel.add(Box.createHorizontalStrut(15));
        optionsPanel.add(new JLabel("E-value:"));
        evalueField = new JTextField("1e-3", 8);
        optionsPanel.add(evalueField);

        remoteCheck = new JCheckBox("Remote search", true);
        optionsPanel.add(remoteCheck);

        JButton searchBtn = new JButton("Run BLAST");
        searchBtn.addActionListener(e -> runBlast());
        optionsPanel.add(searchBtn);

        add(optionsPanel, BorderLayout.NORTH);

        // Results area
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);

        // Initial message
        outputArea.append("BLAST Search Setup\n");
        outputArea.append("==================\n\n");
        outputArea.append("Sequence: " + sequence.id + "\n");
        outputArea.append("Type: " + blastType.toUpperCase() + "\n");
        outputArea.append("Length: " + sequence.sequence.length() + " bp\n\n");
        outputArea.append("Click 'Run BLAST' to start the search.\n");
        outputArea.append("Using remote NCBI BLAST for database access.\n\n");

        // Button panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        btnPanel.add(closeBtn);

        add(btnPanel, BorderLayout.SOUTH);
    }

    private String[] getDatabases() {
        switch (blastType) {
            case "blastn":
                return new String[]{"nt", "refseq_rna", "human_genomic", "mouse_genomic"};
            case "blastp":
                return new String[]{"nr", "refseq_protein", "swissprot", "pdb"};
            case "blastx":
                return new String[]{"nr", "refseq_protein", "swissprot"};
            default:
                return new String[]{"nt", "nr"};
        }
    }

    private void runBlast() {
        outputArea.append("\n--- Starting BLAST Search ---\n");
        outputArea.repaint();

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // Write query to temp file
                    File queryFile = File.createTempFile("blast_query_", ".fasta");
                    PrintWriter writer = new PrintWriter(queryFile);
                    writer.println(">" + sequence.id);
                    for (int i = 0; i < sequence.sequence.length(); i += 60) {
                        writer.println(sequence.sequence.substring(i,
                            Math.min(i + 60, sequence.sequence.length())));
                    }
                    writer.close();

                    // Build BLAST command
                    String toolPath = SeqEdit.getToolPath(blastType);
                    publish("Using BLAST tool: " + toolPath + "\n");

                    List<String> cmd = new ArrayList<>();
                    cmd.add(toolPath);
                    cmd.add("-query");
                    cmd.add(queryFile.getAbsolutePath());
                    cmd.add("-evalue");
                    cmd.add(evalueField.getText());

                    if (remoteCheck.isSelected()) {
                        cmd.add("-remote");
                        cmd.add("-db");
                        cmd.add((String) databaseCombo.getSelectedItem());
                    } else {
                        // For local search, use a local database or create one
                        publish("Local BLAST requires a local database.\n");
                        publish("Use 'Remote search' checkbox for NCBI remote BLAST.\n");
                        queryFile.delete();
                        return null;
                    }

                    cmd.add("-outfmt");
                    cmd.add("6"); // Tabular output

                    publish("Running: " + String.join(" ", cmd) + "\n\n");

                    ProcessBuilder pb = new ProcessBuilder(cmd);
                    Process p = pb.start();

                    // Read output
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));
                    BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(p.getErrorStream()));

                    String line;
                    int lineCount = 0;
                    publish("Results:\n");
                    publish(String.format("%-20s %-20s %6s %6s %8s %8s\n",
                        "Subject", "ID", "Ident", "Len", "E-value", "Bits"));
                    publish("------------------------------------------------------------\n");

                    while ((line = reader.readLine()) != null) {
                        if (lineCount++ < 100) { // Limit output
                            String[] fields = line.split("\t");
                            if (fields.length >= 12) {
                                publish(String.format("%-20s %-20s %6s %6s %8s %8s\n",
                                    fields[1].length() > 18 ? fields[1].substring(0, 18) : fields[1],
                                    fields[0].length() > 18 ? fields[0].substring(0, 18) : fields[0],
                                    fields[2], fields[3], fields[10], fields[11]));
                            }
                        }
                    }

                    int exitCode = p.waitFor();
                    if (exitCode != 0) {
                        StringBuilder error = new StringBuilder();
                        while ((line = errorReader.readLine()) != null) {
                            error.append(line).append("\n");
                        }
                        publish("ERROR (exit code " + exitCode + "): " + error.toString() + "\n");
                    } else {
                        publish("\nBLAST search completed successfully.\n");
                    }

                    queryFile.delete();

                } catch (Exception e) {
                    publish("ERROR: " + e.getMessage() + "\n");
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    outputArea.append(msg);
                }
                outputArea.repaint();
            }

            @Override
            protected void done() {
                outputArea.append("\n--- Search Complete ---\n");
            }
        };
        worker.execute();
    }
}
