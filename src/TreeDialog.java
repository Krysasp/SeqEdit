import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Phylogenetic Tree Dialog
 * Supports UPGMA, NJ (distance-based) and ML (FastTree)
 * Uses local bin/ tools
 */
public class TreeDialog extends JDialog {
    private java.util.List<SeqEdit.SequenceRecord> sequences;
    private JComboBox<String> methodCombo;
    private JComboBox<String> modelCombo;
    private JTextArea outputArea;
    private JPanel displayPanel;
    private JTextArea treeDisplayArea;

    public TreeDialog(Frame parent, java.util.List<SeqEdit.SequenceRecord> sequences) {
        super(parent, "Phylogenetic Tree Viewer", false);
        this.sequences = sequences;
        setSize(900, 700);
        setLayout(new BorderLayout());
        createWidgets();
    }

    private void createWidgets() {
        // Options panel
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Tree Options"));

        optionsPanel.add(new JLabel("Method:"));
        methodCombo = new JComboBox<>(new String[]{"UPGMA", "NJ (Neighbor-Joining)", "ML (FastTree)"});
        optionsPanel.add(methodCombo);

        optionsPanel.add(new JLabel("  Model:"));
        modelCombo = new JComboBox<>(new String[]{"identity", "blastn", "JC69", "K80", "HKY85"});
        optionsPanel.add(modelCombo);

        // ML params (only used for FastTree)
        optionsPanel.add(new JLabel("  ML params:"));
        JTextField mlParams = new JTextField("-gtr -nt -fastest", 15);
        optionsPanel.add(mlParams);

        JButton buildBtn = new JButton("Build Tree");
        buildBtn.addActionListener(e -> buildTree(mlParams.getText()));
        optionsPanel.add(buildBtn);

        add(optionsPanel, BorderLayout.NORTH);

        // Main display - split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(350);

        // Tree display (text-based Newick)
        treeDisplayArea = new JTextArea();
        treeDisplayArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        treeDisplayArea.setEditable(false);
        JScrollPane treeScroll = new JScrollPane(treeDisplayArea);
        treeScroll.setBorder(BorderFactory.createTitledBorder("Tree (Newick format)"));
        splitPane.setTopComponent(treeScroll);

        // Output area
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        outputArea.setEditable(false);
        JScrollPane outScroll = new JScrollPane(outputArea);
        outScroll.setBorder(BorderFactory.createTitledBorder("Details"));
        splitPane.setBottomComponent(outScroll);

        add(splitPane, BorderLayout.CENTER);

        // Initial message
        treeDisplayArea.append("Click 'Build Tree' to generate phylogenetic tree\n\n");
        treeDisplayArea.append("Methods:\n");
        treeDisplayArea.append("  UPGMA: Unweighted Pair Group Method with Arithmetic Mean\n");
        treeDisplayArea.append("  NJ: Neighbor-Joining (distance-based)\n");
        treeDisplayArea.append("  ML: Maximum Likelihood using FastTree (bin/fasttree)\n");

        // Close button
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        btnPanel.add(closeBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void buildTree(String mlParams) {
        treeDisplayArea.setText("");
        outputArea.setText("");

        if (sequences == null || sequences.size() < 2) {
            outputArea.append("ERROR: Need at least 2 sequences to build a tree.\n");
            return;
        }

        String method = (String) methodCombo.getSelectedItem();

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    if (method.contains("ML") || method.contains("FastTree")) {
                        buildTreeFastTree(mlParams);
                    } else {
                        buildTreeDistanceBased();
                    }
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
            }

            @Override
            protected void done() {
                outputArea.append("\n--- Tree construction complete ---\n");
            }
        };
        worker.execute();
    }

    private void buildTreeDistanceBased() {
        outputArea.append("Building distance-based tree (UPGMA/NJ)...\n");
        outputArea.append("Sequences: " + sequences.size() + "\n\n");

        // Calculate pairwise distances
        int n = sequences.size();
        double[][] distMatrix = new double[n][n];

        outputArea.append("Calculating pairwise distances...\n");
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double dist = calculateDistance(sequences.get(i).sequence, sequences.get(j).sequence);
                distMatrix[i][j] = dist;
                distMatrix[j][i] = dist;
            }
        }

        outputArea.append("Distance matrix calculated.\n\n");

        // Build tree based on method
        String method = (String) methodCombo.getSelectedItem();
        String newickTree;

        if (method.contains("UPGMA")) {
            outputArea.append("Building UPGMA tree...\n");
            newickTree = buildUPGMATree(distMatrix);
        } else {
            outputArea.append("Building Neighbor-Joining tree...\n");
            newickTree = buildNJTree(distMatrix);
        }

        treeDisplayArea.setText("");
        treeDisplayArea.append("=== Phylogenetic Tree (Newick format) ===\n\n");
        treeDisplayArea.append(newickTree + ";\n\n");

        // Display distance matrix
        treeDisplayArea.append("\n=== Distance Matrix ===\n");
        treeDisplayArea.append(String.format("%-15s", ""));
        for (int i = 0; i < Math.min(n, 8); i++) {
            treeDisplayArea.append(String.format("%-8s", seqName(sequences.get(i).id)));
        }
        treeDisplayArea.append("\n");

        for (int i = 0; i < Math.min(n, 8); i++) {
            treeDisplayArea.append(String.format("%-15s", seqName(sequences.get(i).id)));
            for (int j = 0; j <= i; j++) {
                treeDisplayArea.append(String.format("%-8.4f", distMatrix[i][j]));
            }
            treeDisplayArea.append("\n");
        }
        if (n > 8) treeDisplayArea.append("... (showing first 8 sequences)\n");

        outputArea.append("Tree construction completed.\n");
    }

    private String seqName(String id) {
        return id.length() > 12 ? id.substring(0, 12) : id;
    }

    private double calculateDistance(String seq1, String seq2) {
        int len = Math.min(seq1.length(), seq2.length());
        if (len == 0) return 1.0;

        int differences = 0;
        for (int i = 0; i < len; i++) {
            if (seq1.charAt(i) != seq2.charAt(i)) {
                differences++;
            }
        }

        String model = (String) modelCombo.getSelectedItem();
        double pDist = (double) differences / len;

        if ("JC69".equals(model)) {
            // Jukes-Cantor correction
            if (pDist >= 0.75) return 3.0;
            return -0.75 * Math.log(1 - (4.0/3.0) * pDist);
        } else if ("K80".equals(model)) {
            // Kimura 2-parameter (simplified)
            return -0.5 * Math.log(1 - 2 * pDist);
        } else {
            // Identity or blastn - just return p-distance
            return pDist;
        }
    }

    private String buildUPGMATree(double[][] distMatrix) {
        int n = distMatrix.length;
        // UPGMA implementation
        List<Integer> activeNodes = new ArrayList<>();
        List<Double> nodeHeights = new ArrayList<>();
        List<String> nodeNames = new ArrayList<>();
        double[][] distances = new double[n][n];

        // Initialize
        for (int i = 0; i < n; i++) {
            activeNodes.add(i);
            nodeHeights.add(0.0);
            nodeNames.add(sequences.get(i).id);
            System.arraycopy(distMatrix[i], 0, distances[i], 0, n);
        }

        List<String> treeParts = new ArrayList<>();

        while (activeNodes.size() > 1) {
            // Find closest pair
            int minI = 0, minJ = 1;
            double minDist = distances[activeNodes.get(0)][activeNodes.get(1)];

            for (int ii = 0; ii < activeNodes.size(); ii++) {
                for (int jj = ii + 1; jj < activeNodes.size(); jj++) {
                    double d = distances[activeNodes.get(ii)][activeNodes.get(jj)];
                    if (d < minDist) {
                        minDist = d;
                        minI = ii;
                        minJ = jj;
                    }
                }
            }

            int nodeI = activeNodes.get(minI);
            int nodeJ = activeNodes.get(minJ);
            double heightI = nodeHeights.get(minI);
            double heightJ = nodeHeights.get(minJ);
            double branchLenI = (minDist / 2) - heightI;
            double branchLenJ = (minDist / 2) - heightJ;

            String newName = "(" + nodeNames.get(minI) + ":" + String.format("%.4f", branchLenI) +
                            "," + nodeNames.get(minJ) + ":" + String.format("%.4f", branchLenJ) + ")";

            // Update distances
            for (int k = 0; k < activeNodes.size(); k++) {
                if (k != minI && k != minJ) {
                    int nodeK = activeNodes.get(k);
                    distances[nodeI][nodeK] = (distances[nodeI][nodeK] + distances[nodeJ][nodeK]) / 2;
                    distances[nodeK][nodeI] = distances[nodeI][nodeK];
                }
            }

            // Remove nodeJ, update nodeI
            activeNodes.remove(minJ);
            nodeNames.remove(minJ);
            nodeHeights.remove(minJ);

            activeNodes.set(minI, -activeNodes.size() - 1); // New internal node ID
            nodeNames.set(minI, newName);
            nodeHeights.set(minI, minDist / 2);
        }

        return nodeNames.get(0);
    }

    private String buildNJTree(double[][] distMatrix) {
        int n = distMatrix.length;
        // Neighbor-Joining implementation (simplified)
        List<Integer> activeNodes = new ArrayList<>();
        List<String> nodeNames = new ArrayList<>();
        double[][] distances = new double[n * 2][n * 2]; // Extra space for internal nodes

        // Initialize
        for (int i = 0; i < n; i++) {
            activeNodes.add(i);
            nodeNames.add(sequences.get(i).id);
            for (int j = 0; j < n; j++) {
                distances[i][j] = distMatrix[i][j];
            }
        }

        int nextInternal = n;
        int totalNodes = n;

        while (activeNodes.size() > 2) {
            int m = activeNodes.size();

            // Calculate net divergence (r_i)
            double[] r = new double[m];
            for (int i = 0; i < m; i++) {
                double sum = 0;
                for (int j = 0; j < m; j++) {
                    sum += distances[activeNodes.get(i)][activeNodes.get(j)];
                }
                r[i] = sum / (m - 2);
            }

            // Find minimum Mij = d_ij - r_i - r_j
            int minI = 0, minJ = 1;
            double minM = distances[activeNodes.get(0)][activeNodes.get(1)] - r[0] - r[1];

            for (int ii = 0; ii < m; ii++) {
                for (int jj = ii + 1; jj < m; jj++) {
                    double val = distances[activeNodes.get(ii)][activeNodes.get(jj)] - r[ii] - r[jj];
                    if (val < minM) {
                        minM = val;
                        minI = ii;
                        minJ = jj;
                    }
                }
            }

            int nodeI = activeNodes.get(minI);
            int nodeJ = activeNodes.get(minJ);

            double branchLenI = (distances[nodeI][nodeJ] + r[minI] - r[minJ]) / 2;
            double branchLenJ = (distances[nodeI][nodeJ] + r[minJ] - r[minI]) / 2;

            // Create new node
            String newName = "(" + nodeNames.get(minI) + ":" + String.format("%.4f", branchLenI) +
                            "," + nodeNames.get(minJ) + ":" + String.format("%.4f", branchLenJ) + ")";

            // Update distances to new node
            distances[nextInternal][nextInternal] = 0;
            for (int k = 0; k < m; k++) {
                if (k != minI && k != minJ) {
                    int nodeK = activeNodes.get(k);
                    distances[nextInternal][nodeK] = (distances[nodeI][nodeK] + distances[nodeJ][nodeK]) / 2;
                    distances[nodeK][nextInternal] = distances[nextInternal][nodeK];
                }
            }

            // Remove old nodes, add new
            activeNodes.remove(Math.max(minI, minJ));
            activeNodes.remove(Math.min(minI, minJ));
            nodeNames.remove(Math.max(minI, minJ));
            nodeNames.remove(Math.min(minI, minJ));

            activeNodes.add(nextInternal);
            nodeNames.add(newName);

            nextInternal++;
            totalNodes++;
        }

        // Last two nodes
        int nodeI = activeNodes.get(0);
        int nodeJ = activeNodes.get(1);
        double dist = distances[nodeI][nodeJ];

        return "(" + nodeNames.get(0) + ":" + String.format("%.4f", dist/2) +
                "," + nodeNames.get(1) + ":" + String.format("%.4f", dist/2) + ")";
    }

    private void buildTreeFastTree(String mlParams) {
        outputArea.append("Building Maximum Likelihood tree using FastTree...\n");

        String fastTreePath = SeqEdit.getToolPath("fasttree");
        outputArea.append("Using FastTree: " + fastTreePath + "\n\n");

        try {
            // Check if FastTree exists
            File fastTreeFile = new File(fastTreePath);
            if (!fastTreeFile.exists() && !fastTreePath.equals("fasttree")) {
                outputArea.append("ERROR: FastTree not found at " + fastTreePath + "\n");
                outputArea.append("Please ensure bin/fasttree exists.\n");
                return;
            }

            // Write sequences to temp file
            File tempInput = File.createTempFile("seqedit_tree_", ".fasta");
            PrintWriter writer = new PrintWriter(tempInput);
            for (SeqEdit.SequenceRecord seq : sequences) {
                writer.println(">" + seq.id);
                for (int i = 0; i < seq.sequence.length(); i += 60) {
                    writer.println(seq.sequence.substring(i, Math.min(i + 60, seq.sequence.length())));
                }
            }
            writer.close();

            outputArea.append("Sequences written to: " + tempInput.getName() + "\n");
            outputArea.append("Running FastTree...\n\n");

            // Build FastTree command
            List<String> cmd = new ArrayList<>();
            cmd.add(fastTreePath);
            cmd.add("-nt"); // nucleotide
            cmd.add("-quiet");
            String[] params = mlParams.split("\\s+");
            for (String p : params) {
                if (!p.isEmpty()) cmd.add(p);
            }
            cmd.add(tempInput.getAbsolutePath());

            // Run FastTree
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.start();

            // Read output (Newick tree)
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            StringBuilder treeOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                treeOutput.append(line).append("\n");
            }

            // Read stderr (FastTree outputs info to stderr)
            StringBuilder errorOutput = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }

            int exitCode = p.waitFor();

            if (exitCode == 0 && treeOutput.length() > 0) {
                String newickTree = treeOutput.toString().trim();
                treeDisplayArea.setText("");
                treeDisplayArea.append("=== ML Tree (FastTree) ===\n\n");
                treeDisplayArea.append(newickTree + "\n\n");
                treeDisplayArea.append("Tree written in Newick format.\n");

                outputArea.append("FastTree completed successfully.\n");
                outputArea.append("Tree length: " + newickTree.length() + " characters\n\n");

                // Display FastTree info
                if (errorOutput.length() > 0) {
                    outputArea.append("FastTree output:\n");
                    outputArea.append(errorOutput.toString() + "\n");
                }
            } else {
                outputArea.append("ERROR: FastTree failed with exit code " + exitCode + "\n");
                if (errorOutput.length() > 0) {
                    outputArea.append("Error output:\n" + errorOutput.toString() + "\n");
                }
            }

            tempInput.delete();

        } catch (Exception e) {
            outputArea.append("ERROR: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }
}
