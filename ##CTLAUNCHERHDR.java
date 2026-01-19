/*
 * CTLauncher v0.9
 * WORKING Minecraft Launcher with Java Version Detection
 * by Team Flames / Samsoft
 * 
 * Compile: javac CTLauncher.java
 * Run: java CTLauncher
 */

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;
import java.security.MessageDigest;

public class CTLauncher extends JFrame {
    
    static final String VER = "0.9";
    static final String NAME = "CTLauncher";
    static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    
    // Colors - Dark Blue Theme
    static final Color C_BG = new Color(20, 24, 36);
    static final Color C_BG2 = new Color(30, 36, 52);
    static final Color C_BTN = new Color(76, 175, 80);
    static final Color C_TEXT = new Color(200, 210, 230);
    static final Color C_DIM = new Color(120, 135, 165);
    static final Color C_WARN = new Color(255, 180, 80);
    static final Color C_BORDER = new Color(45, 55, 75);
    
    // Paths
    final String MC_DIR, VERSIONS_DIR, LIBRARIES_DIR, ASSETS_DIR, NATIVES_DIR;
    
    // UI
    JTextField usernameField;
    JComboBox<String> versionCombo;
    JButton startButton;
    JProgressBar progressBar;
    JLabel statusLabel, javaLabel;
    JTextArea consoleArea;
    JSpinner ramSpinner;
    
    // Data
    Map<String, VersionInfo> versions = new LinkedHashMap<>();
    Map<Integer, String> javaInstalls = new LinkedHashMap<>();
    volatile boolean isRunning = false;
    volatile boolean cancelled = false;
    
    static class VersionInfo {
        String id, type, jsonUrl, mainClass, assetId, assetUrl, clientUrl;
        int javaVersion = 8; // Default to Java 8
        List<LibInfo> libraries = new ArrayList<>();
        String minecraftArgs;
        List<String> gameArgs = new ArrayList<>();
    }
    
    static class LibInfo {
        String name, artifactPath, artifactUrl, nativePath, nativeUrl;
        boolean hasNatives;
    }
    
    public static void main(String[] args) {
        // Force dark blue theme globally
        try {
            UIManager.put("control", new Color(20, 24, 36));
            UIManager.put("text", new Color(200, 210, 230));
            UIManager.put("nimbusBase", new Color(20, 24, 36));
            UIManager.put("nimbusBlueGrey", new Color(30, 36, 52));
            UIManager.put("nimbusFocus", new Color(76, 175, 80));
            UIManager.put("Panel.background", new Color(20, 24, 36));
            UIManager.put("TextField.background", new Color(30, 36, 52));
            UIManager.put("TextField.foreground", new Color(200, 210, 230));
            UIManager.put("TextField.caretForeground", new Color(200, 210, 230));
            UIManager.put("ComboBox.background", new Color(30, 36, 52));
            UIManager.put("ComboBox.foreground", new Color(200, 210, 230));
            UIManager.put("ComboBox.selectionBackground", new Color(76, 175, 80));
            UIManager.put("ComboBox.selectionForeground", new Color(255, 255, 255));
            UIManager.put("List.background", new Color(30, 36, 52));
            UIManager.put("List.foreground", new Color(200, 210, 230));
            UIManager.put("List.selectionBackground", new Color(76, 175, 80));
            UIManager.put("List.selectionForeground", new Color(255, 255, 255));
            UIManager.put("ScrollPane.background", new Color(20, 24, 36));
            UIManager.put("ScrollBar.background", new Color(20, 24, 36));
            UIManager.put("ScrollBar.thumb", new Color(45, 55, 75));
            UIManager.put("ScrollBar.track", new Color(20, 24, 36));
            UIManager.put("Spinner.background", new Color(30, 36, 52));
            UIManager.put("Spinner.foreground", new Color(200, 210, 230));
            UIManager.put("FormattedTextField.background", new Color(30, 36, 52));
            UIManager.put("FormattedTextField.foreground", new Color(200, 210, 230));
            UIManager.put("ProgressBar.background", new Color(15, 18, 28));
            UIManager.put("ProgressBar.foreground", new Color(76, 175, 80));
            UIManager.put("Button.background", new Color(30, 36, 52));
            UIManager.put("Button.foreground", new Color(200, 210, 230));
            UIManager.put("OptionPane.background", new Color(20, 24, 36));
            UIManager.put("OptionPane.messageForeground", new Color(200, 210, 230));
        } catch (Exception e) {}
        SwingUtilities.invokeLater(CTLauncher::new);
    }
    
    public CTLauncher() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        
        if (os.contains("win")) MC_DIR = System.getenv("APPDATA") + File.separator + ".ctlauncher" + File.separator;
        else if (os.contains("mac")) MC_DIR = home + "/Library/Application Support/CTLauncher/";
        else MC_DIR = home + "/.ctlauncher/";
        
        VERSIONS_DIR = MC_DIR + "versions/";
        LIBRARIES_DIR = MC_DIR + "libraries/";
        ASSETS_DIR = MC_DIR + "assets/";
        NATIVES_DIR = MC_DIR + "natives/";
        
        for (String d : new String[]{MC_DIR, VERSIONS_DIR, LIBRARIES_DIR, ASSETS_DIR + "indexes/", ASSETS_DIR + "objects/", NATIVES_DIR})
            new File(d).mkdirs();
        
        detectJavaInstalls();
        initUI();
        loadVersionManifest();
    }
    
    void detectJavaInstalls() {
        // Detect installed Java versions
        String home = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();
        
        // Current Java
        String currentJava = System.getProperty("java.home") + "/bin/java";
        int currentVer = getMajorVersion(System.getProperty("java.version"));
        javaInstalls.put(currentVer, currentJava);
        
        // Common Java locations
        List<String> searchPaths = new ArrayList<>();
        
        if (os.contains("mac")) {
            searchPaths.add("/Library/Java/JavaVirtualMachines");
            searchPaths.add(home + "/Library/Java/JavaVirtualMachines");
            // Homebrew Apple Silicon
            searchPaths.add("/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home");
            searchPaths.add("/opt/homebrew/Cellar/openjdk");
            searchPaths.add("/opt/homebrew/Cellar/openjdk@21");
            searchPaths.add("/opt/homebrew/Cellar/openjdk@17");
            searchPaths.add("/opt/homebrew/Cellar/openjdk@11");
            searchPaths.add("/opt/homebrew/Cellar/openjdk@8");
            // Homebrew Intel
            searchPaths.add("/usr/local/Cellar/openjdk");
            searchPaths.add("/usr/local/Cellar/openjdk@21");
            searchPaths.add("/usr/local/Cellar/openjdk@17");
            searchPaths.add("/usr/local/Cellar/openjdk@11");
            searchPaths.add("/usr/local/Cellar/openjdk@8");
            searchPaths.add("/usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home");
            searchPaths.add("/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home");
            searchPaths.add("/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home");
            // Mojang's bundled runtime
            searchPaths.add(home + "/Library/Application Support/minecraft/runtime");
        } else if (os.contains("win")) {
            searchPaths.add("C:\\Program Files\\Java");
            searchPaths.add("C:\\Program Files\\Eclipse Adoptium");
            searchPaths.add("C:\\Program Files\\Zulu");
            searchPaths.add("C:\\Program Files\\Microsoft\\jdk-21");
            searchPaths.add("C:\\Program Files\\Microsoft\\jdk-17");
            searchPaths.add(System.getenv("APPDATA") + "\\.minecraft\\runtime");
        } else {
            searchPaths.add("/usr/lib/jvm");
            searchPaths.add(home + "/.sdkman/candidates/java");
            searchPaths.add(home + "/.minecraft/runtime");
        }
        
        for (String path : searchPaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                scanForJava(dir, 0);
            }
        }
    }
    
    void scanForJava(File dir, int depth) {
        if (depth > 4) return;
        
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File f : files) {
            if (f.isDirectory()) {
                // Check for java binary
                File javaBin = new File(f, "bin/java");
                if (!javaBin.exists()) javaBin = new File(f, "Contents/Home/bin/java");
                if (!javaBin.exists()) javaBin = new File(f, "jre/bin/java");
                
                if (javaBin.exists() && javaBin.canExecute()) {
                    int ver = detectJavaVersion(javaBin.getAbsolutePath());
                    if (ver > 0 && !javaInstalls.containsKey(ver)) {
                        javaInstalls.put(ver, javaBin.getAbsolutePath());
                    }
                }
                
                scanForJava(f, depth + 1);
            }
        }
    }
    
    int detectJavaVersion(String javaPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(javaPath, "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            p.waitFor();
            
            if (line != null) {
                // Parse version from output like: openjdk version "21.0.1" or java version "1.8.0_xxx"
                int start = line.indexOf('"');
                if (start != -1) {
                    int end = line.indexOf('"', start + 1);
                    if (end != -1) {
                        return getMajorVersion(line.substring(start + 1, end));
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }
    
    int getMajorVersion(String version) {
        if (version.startsWith("1.")) {
            // Old format: 1.8.0_xxx -> 8
            int dot = version.indexOf('.', 2);
            if (dot != -1) {
                try { return Integer.parseInt(version.substring(2, dot)); } catch (Exception e) {}
            }
        } else {
            // New format: 21.0.1 -> 21
            int dot = version.indexOf('.');
            if (dot == -1) dot = version.length();
            try { return Integer.parseInt(version.substring(0, dot)); } catch (Exception e) {}
        }
        return 0;
    }
    
    int parseMCMajor(String version) {
        // 1.21.1 -> 21, 1.8.9 -> 8, 1.20.4 -> 20
        if (version.startsWith("1.")) {
            int dot = version.indexOf('.', 2);
            if (dot == -1) dot = version.length();
            try { return Integer.parseInt(version.substring(2, dot)); } catch (Exception e) {}
        }
        return 0;
    }
    
    int getRequiredJava(String mcVersion) {
        int mcMajor = parseMCMajor(mcVersion);
        if (mcMajor >= 21) return 21;
        else if (mcMajor >= 18) return 17;
        else if (mcMajor >= 17) return 16;
        else return 8;
    }
    
    String findJavaForVersion(int required) {
        // Find highest available Java version
        int highest = 0;
        for (int v : javaInstalls.keySet()) {
            if (v > highest) highest = v;
        }
        
        // Use highest if it meets requirement
        if (highest >= required && javaInstalls.containsKey(highest)) {
            return javaInstalls.get(highest);
        }
        
        // Exact match
        if (javaInstalls.containsKey(required)) return javaInstalls.get(required);
        
        // Find closest higher version
        for (int v = required; v <= 30; v++) {
            if (javaInstalls.containsKey(v)) return javaInstalls.get(v);
        }
        
        // Fall back to current Java
        return System.getProperty("java.home") + "/bin/java";
    }
    
    void initUI() {
        setTitle(NAME + " v" + VER);
        setSize(550, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBackground(C_BG);
        main.setBorder(new EmptyBorder(20, 30, 20, 30));
        
        // Title
        JLabel title = new JLabel(NAME + " v" + VER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(C_BTN);
        title.setAlignmentX(CENTER_ALIGNMENT);
        main.add(title);
        main.add(Box.createVerticalStrut(5));
        
        JLabel sub = new JLabel("Offline Mode Launcher");
        sub.setForeground(C_DIM);
        sub.setAlignmentX(CENTER_ALIGNMENT);
        main.add(sub);
        main.add(Box.createVerticalStrut(15));
        
        // Java info
        StringBuilder javaInfo = new StringBuilder("Java: ");
        for (Map.Entry<Integer, String> e : javaInstalls.entrySet()) {
            if (javaInfo.length() > 6) javaInfo.append(", ");
            javaInfo.append(e.getKey());
        }
        javaLabel = new JLabel(javaInfo.toString());
        javaLabel.setForeground(C_DIM);
        javaLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        javaLabel.setAlignmentX(CENTER_ALIGNMENT);
        main.add(javaLabel);
        main.add(Box.createVerticalStrut(15));
        
        // Username
        JPanel userPanel = fieldPanel("Username:");
        usernameField = new JTextField("Player");
        styleField(usernameField);
        userPanel.add(usernameField);
        main.add(userPanel);
        main.add(Box.createVerticalStrut(12));
        
        // Version
        JPanel verPanel = fieldPanel("Version:");
        versionCombo = new JComboBox<>();
        versionCombo.setMaximumSize(new Dimension(999, 32));
        versionCombo.setBackground(C_BG2);
        versionCombo.setForeground(C_TEXT);
        versionCombo.setBorder(BorderFactory.createLineBorder(C_BORDER));
        ((JComponent) versionCombo.getRenderer()).setOpaque(true);
        versionCombo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                setBackground(sel ? C_BTN : C_BG2);
                setForeground(C_TEXT);
                return this;
            }
        });
        versionCombo.addItem("Loading...");
        verPanel.add(versionCombo);
        main.add(verPanel);
        main.add(Box.createVerticalStrut(12));
        
        // RAM
        JPanel ramPanel = fieldPanel("RAM (MB):");
        ramSpinner = new JSpinner(new SpinnerNumberModel(2048, 512, 16384, 256));
        ramSpinner.setMaximumSize(new Dimension(100, 32));
        JComponent editor = ramSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(C_BG2);
            tf.setForeground(C_TEXT);
            tf.setCaretColor(C_TEXT);
        }
        ramSpinner.setBackground(C_BG2);
        ramSpinner.setBorder(BorderFactory.createLineBorder(C_BORDER));
        ramPanel.add(ramSpinner);
        main.add(ramPanel);
        main.add(Box.createVerticalStrut(20));
        
        // Start button
        startButton = new JButton("▶  START MINECRAFT");
        startButton.setAlignmentX(CENTER_ALIGNMENT);
        startButton.setMaximumSize(new Dimension(250, 50));
        startButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        startButton.setForeground(Color.WHITE);
        startButton.setBackground(C_BTN);
        startButton.setOpaque(true);
        startButton.setBorderPainted(false);
        startButton.setFocusPainted(false);
        startButton.addActionListener(e -> onStart());
        main.add(startButton);
        main.add(Box.createVerticalStrut(15));
        
        // Progress
        progressBar = new JProgressBar(0, 100);
        progressBar.setAlignmentX(CENTER_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(400, 20));
        progressBar.setStringPainted(true);
        progressBar.setForeground(C_BTN);
        progressBar.setBackground(new Color(15, 18, 28));
        progressBar.setBorder(BorderFactory.createLineBorder(C_BORDER));
        progressBar.setVisible(false);
        main.add(progressBar);
        main.add(Box.createVerticalStrut(8));
        
        // Status
        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(C_DIM);
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        main.add(statusLabel);
        main.add(Box.createVerticalStrut(12));
        
        // Console
        consoleArea = new JTextArea(7, 50);
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        consoleArea.setBackground(new Color(12, 15, 24));
        consoleArea.setForeground(new Color(140, 160, 190));
        consoleArea.setCaretColor(new Color(140, 160, 190));
        JScrollPane scroll = new JScrollPane(consoleArea);
        scroll.setMaximumSize(new Dimension(999, 140));
        scroll.setAlignmentX(CENTER_ALIGNMENT);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER));
        scroll.getViewport().setBackground(new Color(12, 15, 24));
        main.add(scroll);
        
        add(main);
        setVisible(true);
        
        log("CTLauncher v" + VER + " | OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        log("Detected Java versions: " + javaInstalls.keySet());
    }
    
    JPanel fieldPanel(String label) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(999, 32));
        JLabel l = new JLabel(label);
        l.setForeground(C_TEXT);
        l.setPreferredSize(new Dimension(90, 25));
        p.add(l);
        return p;
    }
    
    void styleField(JTextField f) {
        f.setMaximumSize(new Dimension(999, 32));
        f.setBackground(C_BG2);
        f.setForeground(C_TEXT);
        f.setCaretColor(C_TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            new EmptyBorder(5, 10, 5, 10)));
    }
    
    void log(String msg) {
        String line = "[" + String.format("%tT", System.currentTimeMillis()) + "] " + msg;
        System.out.println(line);
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(line + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }
    
    void status(String msg) { SwingUtilities.invokeLater(() -> statusLabel.setText(msg)); log(msg); }
    void progress(int val) { SwingUtilities.invokeLater(() -> progressBar.setValue(val)); }
    
    void loadVersionManifest() {
        new Thread(() -> {
            status("Loading versions...");
            try {
                String json = http(MANIFEST_URL);
                int pos = 0;
                while (true) {
                    int i = json.indexOf("\"id\"", pos);
                    if (i == -1) break;
                    int s = json.lastIndexOf("{", i), e = brace(json, s);
                    if (e == -1) break;
                    String o = json.substring(s, e + 1);
                    VersionInfo v = new VersionInfo();
                    v.id = jstr(o, "id"); v.type = jstr(o, "type"); v.jsonUrl = jstr(o, "url");
                    if (v.id != null && v.jsonUrl != null) versions.put(v.id, v);
                    pos = e + 1;
                }
                SwingUtilities.invokeLater(() -> {
                    versionCombo.removeAllItems();
                    
                    // Releases
                    versionCombo.addItem("═══ RELEASES ═══");
                    for (VersionInfo v : versions.values()) {
                        if ("release".equals(v.type)) {
                            String label = v.id;
                            // Mark versions that need Java 21
                            if (v.id.startsWith("1.21") || v.id.startsWith("1.20.5") || v.id.startsWith("1.20.6") || 
                                v.id.startsWith("24w") || v.id.startsWith("25w") || v.id.startsWith("26w")) {
                                if (!javaInstalls.containsKey(21)) {
                                    label += " (needs Java 21!)";
                                }
                            }
                            versionCombo.addItem(label);
                        }
                    }
                    
                    // Snapshots
                    versionCombo.addItem("═══ SNAPSHOTS ═══");
                    for (VersionInfo v : versions.values()) {
                        if ("snapshot".equals(v.type)) {
                            versionCombo.addItem(v.id);
                        }
                    }
                    
                    // Old Beta
                    versionCombo.addItem("═══ OLD BETA ═══");
                    for (VersionInfo v : versions.values()) {
                        if ("old_beta".equals(v.type)) {
                            versionCombo.addItem(v.id);
                        }
                    }
                    
                    // Old Alpha
                    versionCombo.addItem("═══ OLD ALPHA ═══");
                    for (VersionInfo v : versions.values()) {
                        if ("old_alpha".equals(v.type)) {
                            versionCombo.addItem(v.id);
                        }
                    }
                    
                    // Select first release
                    if (versionCombo.getItemCount() > 1) {
                        versionCombo.setSelectedIndex(1);
                    }
                });
                status("Ready - " + versions.size() + " versions");
            } catch (Exception ex) { status("Error: " + ex.getMessage()); }
        }).start();
    }
    
    void onStart() {
        if (isRunning) { cancelled = true; return; }
        String sel = (String) versionCombo.getSelectedItem();
        if (sel == null || sel.startsWith("═══")) { status("Select a version!"); return; }
        
        // Remove Java warning suffix if present
        if (sel.contains(" (needs")) sel = sel.substring(0, sel.indexOf(" (needs"));
        
        String user = usernameField.getText().trim().replaceAll("[^a-zA-Z0-9_]", "");
        if (user.length() < 3) { status("Username too short!"); return; }
        if (user.length() > 16) user = user.substring(0, 16);
        usernameField.setText(user);
        
        VersionInfo ver = versions.get(sel);
        if (ver == null) { status("Version not found!"); return; }
        
        final String u = user;
        new Thread(() -> runGame(ver, u)).start();
    }
    
    void runGame(VersionInfo ver, String user) {
        isRunning = true; cancelled = false;
        SwingUtilities.invokeLater(() -> { startButton.setText("CANCEL"); startButton.setBackground(new Color(200, 60, 60)); progressBar.setVisible(true); progressBar.setValue(0); });
        
        try {
            log("═══════════════════════════════════════════════════");
            log("Launching Minecraft " + ver.id + " as " + user);
            
            // 1. Version JSON
            status("Downloading version info...");
            String json = http(ver.jsonUrl);
            ver.mainClass = jstr(json, "mainClass");
            ver.assetId = jstr(json, "assets");
            
            // Get required Java version from MC version first, then override from JSON if available
            ver.javaVersion = getRequiredJava(ver.id);
            int ji = json.indexOf("\"javaVersion\"");
            if (ji != -1) {
                int js = json.indexOf("{", ji), je = brace(json, js);
                if (je != -1) {
                    String jv = json.substring(js, je + 1);
                    String mv = jstr(jv, "majorVersion");
                    if (mv != null) {
                        try { ver.javaVersion = Integer.parseInt(mv); } catch (Exception ex) {}
                    }
                }
            }
            log("MC " + ver.id + " -> Java " + ver.javaVersion + " required");
            
            // Check if we have the right Java
            String javaPath = findJavaForVersion(ver.javaVersion);
            int availableJava = detectJavaVersion(javaPath);
            
            if (availableJava < ver.javaVersion) {
                log("WARNING: Java " + ver.javaVersion + " required, but only Java " + availableJava + " available!");
                log("Please install Java " + ver.javaVersion + " or choose an older Minecraft version.");
                log("");
                log("Download Java 21 from: https://adoptium.net/temurin/releases/");
                log("Or use Minecraft 1.20.4 or older (works with Java 17)");
                status("ERROR: Need Java " + ver.javaVersion + "!");
                return;
            }
            log("Using Java: " + javaPath + " (version " + availableJava + ")");
            
            int ai = json.indexOf("\"assetIndex\"");
            if (ai != -1) { int s = json.indexOf("{", ai), e = brace(json, s);
                if (e != -1) { String a = json.substring(s, e+1); ver.assetUrl = jstr(a, "url"); if (ver.assetId == null) ver.assetId = jstr(a, "id"); }}
            if (ver.assetId == null) ver.assetId = "legacy";
            
            int di = json.indexOf("\"downloads\"");
            if (di != -1) { int s = json.indexOf("{", di), e = brace(json, s);
                if (e != -1) { String d = json.substring(s, e+1);
                    int ci = d.indexOf("\"client\"");
                    if (ci != -1) { int cs = d.indexOf("{", ci), ce = brace(d, cs);
                        if (ce != -1) ver.clientUrl = jstr(d.substring(cs, ce+1), "url"); }}}
            
            parseLibs(ver, json);
            ver.minecraftArgs = jstr(json, "minecraftArguments");
            
            int argi = json.indexOf("\"arguments\"");
            if (argi != -1) { int as = json.indexOf("{", argi), ae = brace(json, as);
                if (ae != -1) { String args = json.substring(as, ae+1);
                    int gi = args.indexOf("\"game\"");
                    if (gi != -1) { int gs = args.indexOf("[", gi), ge = bracket(args, gs);
                        if (ge != -1) extractArgs(args.substring(gs+1, ge), ver.gameArgs); }}}
            
            log("Main: " + ver.mainClass + " | Libs: " + ver.libraries.size());
            progress(10);
            if (cancelled) throw new InterruptedException();
            
            // 2. Client JAR
            status("Downloading Minecraft...");
            String jar = VERSIONS_DIR + ver.id + "/" + ver.id + ".jar";
            new File(VERSIONS_DIR + ver.id).mkdirs();
            File jf = new File(jar);
            if (!jf.exists() || jf.length() < 1000000) {
                if (ver.clientUrl == null) throw new Exception("No client URL!");
                download(ver.clientUrl, jar, 10, 30);
            }
            log("Client JAR: " + jf.length() + " bytes");
            
            // Create minecraft.jar symlink/copy for legacy compatibility
            File mcJar = new File(MC_DIR + "bin/minecraft.jar");
            mcJar.getParentFile().mkdirs();
            if (!mcJar.exists() || mcJar.length() != jf.length()) {
                try { Files.copy(jf.toPath(), mcJar.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
                catch (Exception e) { /* ignore */ }
            }
            
            progress(30);
            if (cancelled) throw new InterruptedException();
            
            // 3. Libraries
            status("Downloading libraries...");
            int total = ver.libraries.size(), done = 0, dl = 0;
            for (LibInfo lib : ver.libraries) {
                if (cancelled) throw new InterruptedException();
                done++;
                if (lib.artifactPath != null && lib.artifactUrl != null) {
                    String p = LIBRARIES_DIR + lib.artifactPath;
                    if (!new File(p).exists()) { new File(p).getParentFile().mkdirs(); downloadQuiet(lib.artifactUrl, p); dl++; }}
                if (lib.hasNatives && lib.nativeUrl != null && lib.nativePath != null) {
                    String p = LIBRARIES_DIR + lib.nativePath;
                    if (!new File(p).exists()) { new File(p).getParentFile().mkdirs(); downloadQuiet(lib.nativeUrl, p); dl++; }}
                progress(30 + 30 * done / total);
            }
            log("Downloaded " + dl + " libraries");
            progress(60);
            if (cancelled) throw new InterruptedException();
            
            // 4. Natives
            status("Extracting natives...");
            String natDir = NATIVES_DIR + ver.id + "/";
            new File(natDir).mkdirs();
            int nc = 0;
            for (LibInfo lib : ver.libraries) {
                String jp = lib.hasNatives && lib.nativePath != null ? LIBRARIES_DIR + lib.nativePath :
                    (lib.name != null && lib.name.toLowerCase().contains("lwjgl") && lib.name.toLowerCase().contains("native") && lib.artifactPath != null) ? LIBRARIES_DIR + lib.artifactPath : null;
                if (jp != null && new File(jp).exists()) nc += extractNat(jp, natDir);
            }
            log("Extracted " + nc + " native files");
            progress(75);
            if (cancelled) throw new InterruptedException();
            
            // 5. Assets
            status("Downloading assets...");
            log("Asset ID: " + ver.assetId + " | URL: " + ver.assetUrl);
            String idx = ASSETS_DIR + "indexes/" + ver.assetId + ".json";
            new File(ASSETS_DIR + "indexes/").mkdirs();
            new File(ASSETS_DIR + "objects/").mkdirs();
            
            // Always download asset index if missing
            if (ver.assetUrl != null && !new File(idx).exists()) {
                log("Downloading asset index...");
                if (!downloadQuiet(ver.assetUrl, idx)) {
                    log("Warning: Asset index download failed, trying direct URL...");
                    String directUrl = "https://launchermeta.mojang.com/v1/packages/" + ver.assetId + "/" + ver.assetId + ".json";
                    downloadQuiet(directUrl, idx);
                }
            }
            
            // Parse and download asset objects
            File idxFile = new File(idx);
            if (idxFile.exists()) {
                try {
                    String assetJson = new String(Files.readAllBytes(idxFile.toPath()));
                    int objIdx = assetJson.indexOf("\"objects\"");
                    if (objIdx != -1) {
                        int objStart = assetJson.indexOf("{", objIdx);
                        int objEnd = brace(assetJson, objStart);
                        if (objEnd != -1) {
                            String objects = assetJson.substring(objStart, objEnd + 1);
                            // Find all hashes
                            List<String> hashes = new ArrayList<>();
                            int pos = 0;
                            while (pos < objects.length()) {
                                int hashIdx = objects.indexOf("\"hash\"", pos);
                                if (hashIdx == -1) break;
                                String hash = jstr(objects.substring(hashIdx - 1), "hash");
                                if (hash != null && hash.length() == 40) {
                                    hashes.add(hash);
                                }
                                pos = hashIdx + 10;
                            }
                            
                            log("Assets to download: " + hashes.size());
                            int assetsDl = 0;
                            int assetsTotal = hashes.size();
                            for (int i = 0; i < hashes.size(); i++) {
                                if (cancelled) throw new InterruptedException();
                                String hash = hashes.get(i);
                                String prefix = hash.substring(0, 2);
                                String assetPath = ASSETS_DIR + "objects/" + prefix + "/" + hash;
                                File assetFile = new File(assetPath);
                                if (!assetFile.exists()) {
                                    assetFile.getParentFile().mkdirs();
                                    String url = "https://resources.download.minecraft.net/" + prefix + "/" + hash;
                                    if (downloadQuiet(url, assetPath)) assetsDl++;
                                }
                                if (i % 100 == 0) {
                                    progress(75 + 15 * i / Math.max(1, assetsTotal));
                                    status("Downloading assets... " + i + "/" + assetsTotal);
                                }
                            }
                            log("Downloaded " + assetsDl + " new assets");
                        }
                    }
                } catch (Exception e) {
                    log("Asset parsing error: " + e.getMessage());
                }
            } else {
                log("Warning: No asset index found!");
            }
            progress(90);
            if (cancelled) throw new InterruptedException();
            
            // 6. Launch
            status("Launching Minecraft...");
            int ram = (Integer) ramSpinner.getValue();
            String uuid = genUUID(user);
            
            StringBuilder cp = new StringBuilder();
            for (LibInfo lib : ver.libraries)
                if (lib.artifactPath != null && !lib.hasNatives) { String p = LIBRARIES_DIR + lib.artifactPath;
                    if (new File(p).exists()) { if (cp.length() > 0) cp.append(File.pathSeparator); cp.append(p); }}
            cp.append(File.pathSeparator).append(jar);
            
            List<String> cmd = new ArrayList<>();
            cmd.add(javaPath);  // Use correct Java!
            if (isMac()) cmd.add("-XstartOnFirstThread");
            cmd.add("-Xms512M"); cmd.add("-Xmx" + ram + "M");
            cmd.add("-Djava.library.path=" + natDir);
            cmd.add("-Dminecraft.launcher.brand=" + NAME);
            if (isAppleSilicon()) cmd.add("-Dorg.lwjgl.system.allocator=system");
            cmd.add("-cp"); cmd.add(cp.toString());
            cmd.add(ver.mainClass != null ? ver.mainClass : "net.minecraft.client.main.Main");
            
            // Always use clean hardcoded args - JSON parsing is unreliable
            cmd.add("--username"); cmd.add(user);
            cmd.add("--version"); cmd.add(ver.id);
            cmd.add("--gameDir"); cmd.add(MC_DIR);
            cmd.add("--assetsDir"); cmd.add(ASSETS_DIR);
            cmd.add("--assetIndex"); cmd.add(ver.assetId);
            cmd.add("--uuid"); cmd.add(uuid);
            cmd.add("--accessToken"); cmd.add("0");
            cmd.add("--userType"); cmd.add("legacy");
            cmd.add("--versionType"); cmd.add(ver.type != null ? ver.type : "release");
            
            progress(100);
            log("Starting with Java " + availableJava + ", RAM: " + ram + "MB");
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(MC_DIR));
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            log("═══════════════════════════════════════════════════");
            log("Minecraft started! PID: " + proc.pid());
            status("Minecraft is running!");
            
            new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    String line; while ((line = br.readLine()) != null) log("MC> " + line);
                } catch (Exception ex) {}
                try { int c = proc.waitFor(); log("Exit code: " + c); if (c != 0) status("Crashed (code " + c + ")"); } catch (Exception ex) {}
            }).start();
            
        } catch (InterruptedException ex) { status("Cancelled"); log("Cancelled by user");
        } catch (Exception ex) { status("Error: " + ex.getMessage()); log("ERROR: " + ex); ex.printStackTrace();
        } finally { isRunning = false; SwingUtilities.invokeLater(() -> { startButton.setText("▶  START MINECRAFT"); startButton.setBackground(C_BTN); progressBar.setVisible(false); }); }
    }
    
    void parseLibs(VersionInfo ver, String json) {
        int li = json.indexOf("\"libraries\"");
        if (li == -1) return;
        int as = json.indexOf("[", li), ae = bracket(json, as);
        if (ae == -1) return;
        String arr = json.substring(as, ae + 1);
        String os = osName();
        
        int pos = 0;
        while (pos < arr.length()) {
            int s = arr.indexOf("{", pos);
            if (s == -1) break;
            int e = brace(arr, s);
            if (e == -1) break;
            String o = arr.substring(s, e + 1);
            
            if (o.contains("\"rules\"") && !checkRules(o, os)) { pos = e + 1; continue; }
            
            LibInfo lib = new LibInfo();
            lib.name = jstr(o, "name");
            
            int di = o.indexOf("\"downloads\"");
            if (di != -1) { int ds = o.indexOf("{", di), de = brace(o, ds);
                if (de != -1) { String dl = o.substring(ds, de + 1);
                    int ai = dl.indexOf("\"artifact\"");
                    if (ai != -1) { int xs = dl.indexOf("{", ai), xe = brace(dl, xs);
                        if (xe != -1) { String a = dl.substring(xs, xe+1); lib.artifactPath = jstr(a, "path"); lib.artifactUrl = jstr(a, "url"); }}
                    
                    if (o.contains("\"natives\"")) { int ni = o.indexOf("\"natives\""), ns = o.indexOf("{", ni), ne = brace(o, ns);
                        if (ne != -1) { String n = o.substring(ns, ne+1); String c = jstr(n, os);
                            if (c != null) { c = c.replace("${arch}", is64() ? "64" : "32"); lib.hasNatives = true;
                                int ci = dl.indexOf("\"classifiers\""), cs = dl.indexOf("{", ci), ce = brace(dl, cs);
                                if (ce != -1) { String cls = dl.substring(cs, ce+1);
                                    String[] tryC = isAppleSilicon() ? new String[]{"natives-macos-arm64", c} : new String[]{c};
                                    for (String tc : tryC) { int ti = cls.indexOf("\"" + tc + "\"");
                                        if (ti != -1) { int ts = cls.indexOf("{", ti), te = brace(cls, ts);
                                            if (te != -1) { String t = cls.substring(ts, te+1); lib.nativePath = jstr(t, "path"); lib.nativeUrl = jstr(t, "url"); break; }}}}}}}}}
            
            if (lib.artifactPath == null && lib.name != null) { lib.artifactPath = maven(lib.name); lib.artifactUrl = "https://libraries.minecraft.net/" + lib.artifactPath; }
            if (lib.name != null) ver.libraries.add(lib);
            pos = e + 1;
        }
    }
    
    boolean checkRules(String o, String os) {
        boolean ok = false; int p = 0;
        while (p < o.length()) { int ai = o.indexOf("\"action\"", p); if (ai == -1) break;
            int rs = o.lastIndexOf("{", ai), re = brace(o, rs); if (re == -1) break;
            String r = o.substring(rs, re + 1); String act = jstr(r, "action");
            if (r.contains("\"os\"")) { String ros = jstr(r, "name");
                if (ros != null) { if ("allow".equals(act) && ros.equals(os)) ok = true;
                    else if ("disallow".equals(act) && ros.equals(os)) return false; }}
            else if ("allow".equals(act)) ok = true;
            p = re + 1; }
        return ok;
    }
    
    void extractArgs(String arr, List<String> list) {
        // Parse JSON array properly - skip objects with "rules", only get simple strings
        int pos = 0;
        while (pos < arr.length()) {
            // Skip whitespace
            while (pos < arr.length() && Character.isWhitespace(arr.charAt(pos))) pos++;
            if (pos >= arr.length()) break;
            
            char c = arr.charAt(pos);
            if (c == '[' || c == ',' || c == ']') { pos++; continue; }
            
            // If it's an object { }, skip the entire object (these are conditional args with rules)
            if (c == '{') {
                int depth = 1;
                pos++;
                boolean inStr = false;
                while (pos < arr.length() && depth > 0) {
                    char ch = arr.charAt(pos);
                    if (ch == '"' && (pos == 0 || arr.charAt(pos-1) != '\\')) inStr = !inStr;
                    else if (!inStr) {
                        if (ch == '{') depth++;
                        else if (ch == '}') depth--;
                    }
                    pos++;
                }
                continue;
            }
            
            // If it's a simple string "...", extract it
            if (c == '"') {
                pos++; // skip opening quote
                StringBuilder sb = new StringBuilder();
                while (pos < arr.length() && !(arr.charAt(pos) == '"' && arr.charAt(pos-1) != '\\')) {
                    sb.append(arr.charAt(pos));
                    pos++;
                }
                pos++; // skip closing quote
                
                String val = sb.toString();
                if (!val.isEmpty()) {
                    list.add(val);
                }
            } else {
                pos++;
            }
        }
    }
    
    int extractNat(String jar, String dir) {
        int n = 0;
        try (ZipInputStream z = new ZipInputStream(new FileInputStream(jar))) {
            ZipEntry e; while ((e = z.getNextEntry()) != null) {
                if (e.isDirectory() || e.getName().startsWith("META-INF")) continue;
                String name = e.getName();
                if (name.endsWith(".dll") || name.endsWith(".so") || name.endsWith(".dylib") || name.endsWith(".jnilib")) {
                    File f = new File(dir + new File(name).getName());
                    if (!f.exists()) { Files.copy(z, f.toPath()); n++; }}}
        } catch (Exception ex) {}
        return n;
    }
    
    String replace(String s, VersionInfo v, String u, String uuid) {
        return s.replace("${auth_player_name}", u).replace("${version_name}", v.id)
            .replace("${game_directory}", MC_DIR).replace("${assets_root}", ASSETS_DIR)
            .replace("${assets_index_name}", v.assetId).replace("${auth_uuid}", uuid)
            .replace("${auth_access_token}", "0").replace("${user_type}", "legacy")
            .replace("${version_type}", v.type != null ? v.type : "release").replace("${user_properties}", "{}");
    }
    
    String genUUID(String u) {
        try { MessageDigest md = MessageDigest.getInstance("MD5"); byte[] h = md.digest(("OfflinePlayer:" + u).getBytes());
            StringBuilder sb = new StringBuilder(); for (int i = 0; i < 16; i++) { sb.append(String.format("%02x", h[i])); if (i==3||i==5||i==7||i==9) sb.append("-"); }
            return sb.toString();
        } catch (Exception e) { return UUID.randomUUID().toString(); }
    }
    
    String http(String url) throws Exception {
        for (int retry = 0; retry < 3; retry++) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setRequestProperty("User-Agent", "Mozilla/5.0 " + NAME + "/" + VER);
                c.setRequestProperty("Accept", "*/*");
                c.setConnectTimeout(30000); c.setReadTimeout(120000);
                c.setInstanceFollowRedirects(true);
                int code = c.getResponseCode();
                if (code == 301 || code == 302 || code == 307 || code == 308) {
                    url = c.getHeaderField("Location"); continue;
                }
                if (code != 200) throw new Exception("HTTP " + code + " for " + url);
                try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                    StringBuilder sb = new StringBuilder(); String l; while ((l = r.readLine()) != null) sb.append(l); return sb.toString();
                }
            } catch (Exception e) {
                if (retry == 2) throw e;
                log("Retry " + (retry+1) + " for: " + url);
                Thread.sleep(1000 * (retry + 1));
            }
        }
        throw new Exception("Failed after retries: " + url);
    }
    
    void download(String url, String dest, int p1, int p2) throws Exception {
        for (int retry = 0; retry < 3; retry++) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setRequestProperty("User-Agent", "Mozilla/5.0 " + NAME + "/" + VER);
                c.setConnectTimeout(30000); c.setReadTimeout(120000);
                c.setInstanceFollowRedirects(true);
                int code = c.getResponseCode();
                if (code == 301 || code == 302 || code == 307 || code == 308) {
                    url = c.getHeaderField("Location"); continue;
                }
                if (code != 200) throw new Exception("HTTP " + code);
                long total = c.getContentLengthLong();
                try (InputStream i = c.getInputStream(); FileOutputStream o = new FileOutputStream(dest)) {
                    byte[] b = new byte[16384]; long dl = 0; int n;
                    while ((n = i.read(b)) != -1) { o.write(b, 0, n); dl += n;
                        if (total > 0) progress(p1 + (int)((p2 - p1) * dl / total)); }
                }
                return;
            } catch (Exception e) {
                new File(dest).delete();
                if (retry == 2) throw e;
                log("Download retry " + (retry+1) + ": " + new File(dest).getName());
                Thread.sleep(1000 * (retry + 1));
            }
        }
    }
    
    boolean downloadQuiet(String url, String dest) {
        for (int retry = 0; retry < 3; retry++) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setRequestProperty("User-Agent", "Mozilla/5.0 " + NAME + "/" + VER);
                c.setConnectTimeout(30000); c.setReadTimeout(120000);
                c.setInstanceFollowRedirects(true);
                int code = c.getResponseCode();
                if (code == 301 || code == 302 || code == 307 || code == 308) {
                    url = c.getHeaderField("Location"); continue;
                }
                if (code != 200) throw new Exception("HTTP " + code);
                try (InputStream i = c.getInputStream(); FileOutputStream o = new FileOutputStream(dest)) {
                    byte[] b = new byte[16384]; int n; while ((n = i.read(b)) != -1) o.write(b, 0, n);
                }
                return true;
            } catch (Exception ex) {
                new File(dest).delete();
                if (retry == 2) { log("Failed: " + new File(dest).getName() + " - " + ex.getMessage()); return false; }
                try { Thread.sleep(500 * (retry + 1)); } catch (Exception e) {}
            }
        }
        return false;
    }
    
    String jstr(String j, String k) {
        int i = j.indexOf("\"" + k + "\""); if (i == -1) return null;
        int c = j.indexOf(":", i); if (c == -1) return null;
        int s = -1; for (int x = c + 1; x < j.length(); x++) { char ch = j.charAt(x); if (ch == '"') { s = x + 1; break; } else if (!Character.isWhitespace(ch)) break; }
        if (s == -1) return null;
        int e = s; while (e < j.length() && !(j.charAt(e) == '"' && j.charAt(e-1) != '\\')) e++;
        return j.substring(s, e);
    }
    
    int brace(String s, int i) {
        if (i < 0 || i >= s.length()) return -1;
        int d = 0; boolean q = false;
        for (int x = i; x < s.length(); x++) { char c = s.charAt(x), p = x > 0 ? s.charAt(x-1) : 0;
            if (c == '"' && p != '\\') q = !q;
            else if (!q) { if (c == '{') d++; else if (c == '}') { d--; if (d == 0) return x; }}}
        return -1;
    }
    
    int bracket(String s, int i) {
        if (i < 0 || i >= s.length()) return -1;
        int d = 0; boolean q = false;
        for (int x = i; x < s.length(); x++) { char c = s.charAt(x), p = x > 0 ? s.charAt(x-1) : 0;
            if (c == '"' && p != '\\') q = !q;
            else if (!q) { if (c == '[') d++; else if (c == ']') { d--; if (d == 0) return x; }}}
        return -1;
    }
    
    String maven(String c) {
        String[] p = c.split(":"); if (p.length < 3) return null;
        return p[0].replace('.', '/') + "/" + p[1] + "/" + p[2] + "/" + p[1] + "-" + p[2] + (p.length > 3 ? "-" + p[3] : "") + ".jar";
    }
    
    String osName() { String o = System.getProperty("os.name").toLowerCase(); return o.contains("win") ? "windows" : o.contains("mac") ? "osx" : "linux"; }
    boolean is64() { String a = System.getProperty("os.arch"); return a.contains("64") || a.contains("aarch64"); }
    boolean isMac() { return System.getProperty("os.name").toLowerCase().contains("mac"); }
    boolean isAppleSilicon() { return isMac() && System.getProperty("os.arch").toLowerCase().contains("aarch"); }
}
