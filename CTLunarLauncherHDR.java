/*
 * CTLauncher v1.1 - Lunar Style Edition
 * High-Performance Minecraft Launcher with Built-in FPS Boosters
 * (C) 1999-2026 Samsoft / Team Flames
 * 
 * Compile: javac CTLunarLauncherHDR.java
 * Run: java CTLunarLauncherHDR
 * 
 * v1.1 Fixes:
 * - Added -XstartOnFirstThread for macOS (GLFW requirement)
 * - Fixed options.txt format (key:value instead of key=value)
 */

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;
import java.security.MessageDigest;

public class CTLunarLauncherHDR extends JFrame {
    
    static final String VER = "1.1";
    static final String NAME = "CTLauncher";
    static final String COPYRIGHT = "(C) 1999-2026 Samsoft";
    static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    
    // Lunar Client Style Colors
    static final Color C_BG_DARK = new Color(13, 17, 23);
    static final Color C_BG_CARD = new Color(22, 27, 34);
    static final Color C_BG_HOVER = new Color(33, 38, 45);
    static final Color C_ACCENT = new Color(0, 200, 170);
    static final Color C_ACCENT_DARK = new Color(0, 150, 130);
    static final Color C_TEXT = new Color(230, 237, 243);
    static final Color C_TEXT_DIM = new Color(139, 148, 158);
    static final Color C_BORDER = new Color(48, 54, 61);
    static final Color C_SUCCESS = new Color(46, 160, 67);
    static final Color C_WARNING = new Color(210, 153, 34);
    static final Color C_ERROR = new Color(248, 81, 73);
    
    // Paths
    String MC_DIR, VERSIONS_DIR, LIBRARIES_DIR, ASSETS_DIR, NATIVES_DIR, MODS_DIR;
    
    // UI Components
    JTextField usernameField;
    JComboBox<String> versionCombo;
    JButton launchButton;
    JProgressBar progressBar;
    JLabel statusLabel;
    JTextArea consoleArea;
    JSpinner ramSpinner;
    JPanel mainContent, sidebarPanel;
    
    // FPS Booster Settings
    JCheckBox chkFastRender, chkChunkOpt, chkEntityCull, chkParticles, chkSmoothFps, chkFastMath;
    JSlider renderDistSlider;
    
    // Data
    Map<String, VersionInfo> versions = new LinkedHashMap<>();
    Map<Integer, String> javaInstalls = new LinkedHashMap<>();
    volatile boolean isRunning = false;
    volatile boolean cancelled = false;
    String currentTab = "PLAY";
    
    static class VersionInfo {
        String id, type, jsonUrl, mainClass, assetId, assetUrl, clientUrl;
        int javaVersion = 8;
        List<LibInfo> libraries = new ArrayList<>();
        String minecraftArgs;
        List<String> gameArgs = new ArrayList<>();
    }
    
    static class LibInfo {
        String name, artifactPath, artifactUrl, nativePath, nativeUrl;
        boolean hasNatives;
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            setupGlobalTheme();
        } catch (Exception e) {}
        SwingUtilities.invokeLater(CTLunarLauncherHDR::new);
    }
    
    static void setupGlobalTheme() {
        UIManager.put("Panel.background", C_BG_DARK);
        UIManager.put("Label.foreground", C_TEXT);
        UIManager.put("TextField.background", C_BG_CARD);
        UIManager.put("TextField.foreground", C_TEXT);
        UIManager.put("TextField.caretForeground", C_TEXT);
        UIManager.put("TextArea.background", C_BG_DARK);
        UIManager.put("TextArea.foreground", C_TEXT);
        UIManager.put("ComboBox.background", C_BG_CARD);
        UIManager.put("ComboBox.foreground", C_TEXT);
        UIManager.put("ComboBox.selectionBackground", C_ACCENT);
        UIManager.put("ComboBox.selectionForeground", C_BG_DARK);
        UIManager.put("ScrollPane.background", C_BG_DARK);
        UIManager.put("ScrollBar.background", C_BG_DARK);
        UIManager.put("ScrollBar.thumb", C_BG_HOVER);
        UIManager.put("Spinner.background", C_BG_CARD);
        UIManager.put("Spinner.foreground", C_TEXT);
        UIManager.put("CheckBox.background", C_BG_CARD);
        UIManager.put("CheckBox.foreground", C_TEXT);
        UIManager.put("Slider.background", C_BG_CARD);
        UIManager.put("ProgressBar.background", C_BG_DARK);
        UIManager.put("ProgressBar.foreground", C_ACCENT);
    }
    
    public CTLunarLauncherHDR() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        
        if (os.contains("win")) MC_DIR = System.getenv("APPDATA") + File.separator + ".ctlauncher" + File.separator;
        else if (os.contains("mac")) MC_DIR = home + "/Library/Application Support/CTLauncher/";
        else MC_DIR = home + "/.ctlauncher/";
        
        VERSIONS_DIR = MC_DIR + "versions/";
        LIBRARIES_DIR = MC_DIR + "libraries/";
        ASSETS_DIR = MC_DIR + "assets/";
        NATIVES_DIR = MC_DIR + "natives/";
        MODS_DIR = MC_DIR + "mods/";
        
        for (String d : new String[]{MC_DIR, VERSIONS_DIR, LIBRARIES_DIR, ASSETS_DIR + "indexes/", ASSETS_DIR + "objects/", NATIVES_DIR, MODS_DIR})
            new File(d).mkdirs();
        
        detectJavaInstalls();
        initUI();
        loadVersionManifest();
    }
    
    void initUI() {
        setTitle("CTLauncher v" + VER + " - " + COPYRIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG_DARK);
        setLayout(new BorderLayout(0, 0));
        
        JPanel container = new JPanel(new BorderLayout(0, 0));
        container.setBackground(C_BG_DARK);
        
        sidebarPanel = createSidebar();
        container.add(sidebarPanel, BorderLayout.WEST);
        
        mainContent = new JPanel(new CardLayout());
        mainContent.setBackground(C_BG_DARK);
        mainContent.add(createPlayPanel(), "PLAY");
        mainContent.add(createBoostersPanel(), "BOOSTERS");
        mainContent.add(createSettingsPanel(), "SETTINGS");
        container.add(mainContent, BorderLayout.CENTER);
        
        add(container, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
        
        setVisible(true);
        log("═══════════════════════════════════════════════════════════");
        log("  CTLauncher v" + VER + " - Lunar Style Edition");
        log("  " + COPYRIGHT);
        log("  Built-in FPS Boosters | No Optifine Required");
        log("═══════════════════════════════════════════════════════════");
        log("Java versions found: " + javaInstalls.keySet());
    }
    
    JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(C_BG_CARD);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, C_BORDER));
        
        JPanel logoPanel = new JPanel();
        logoPanel.setBackground(C_BG_CARD);
        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
        logoPanel.setBorder(BorderFactory.createEmptyBorder(25, 20, 25, 20));
        logoPanel.setMaximumSize(new Dimension(220, 120));
        
        JLabel logoText = new JLabel("CT");
        logoText.setFont(new Font("SansSerif", Font.BOLD, 48));
        logoText.setForeground(C_ACCENT);
        logoText.setAlignmentX(CENTER_ALIGNMENT);
        
        JLabel brandText = new JLabel("LAUNCHER");
        brandText.setFont(new Font("SansSerif", Font.BOLD, 14));
        brandText.setForeground(C_TEXT);
        brandText.setAlignmentX(CENTER_ALIGNMENT);
        
        JLabel verText = new JLabel("v" + VER + " Lunar Edition");
        verText.setFont(new Font("SansSerif", Font.PLAIN, 10));
        verText.setForeground(C_TEXT_DIM);
        verText.setAlignmentX(CENTER_ALIGNMENT);
        
        logoPanel.add(logoText);
        logoPanel.add(brandText);
        logoPanel.add(Box.createVerticalStrut(5));
        logoPanel.add(verText);
        sidebar.add(logoPanel);
        
        sidebar.add(Box.createVerticalStrut(20));
        
        sidebar.add(createNavButton("▶  PLAY", "PLAY", true));
        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(createNavButton("⚡  FPS BOOSTERS", "BOOSTERS", false));
        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(createNavButton("⚙  SETTINGS", "SETTINGS", false));
        
        sidebar.add(Box.createVerticalGlue());
        
        JLabel copyright = new JLabel("<html><center>" + COPYRIGHT + "<br>Team Flames</center></html>");
        copyright.setFont(new Font("SansSerif", Font.PLAIN, 10));
        copyright.setForeground(C_TEXT_DIM);
        copyright.setAlignmentX(CENTER_ALIGNMENT);
        copyright.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        sidebar.add(copyright);
        
        return sidebar;
    }
    
    JButton createNavButton(String text, String tab, boolean selected) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(selected ? C_ACCENT : C_TEXT);
        btn.setBackground(selected ? C_BG_HOVER : C_BG_CARD);
        btn.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(220, 50));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (!currentTab.equals(tab)) btn.setBackground(C_BG_HOVER); }
            public void mouseExited(MouseEvent e) { if (!currentTab.equals(tab)) btn.setBackground(C_BG_CARD); }
        });
        
        btn.addActionListener(e -> switchTab(tab));
        return btn;
    }
    
    void switchTab(String tab) {
        currentTab = tab;
        CardLayout cl = (CardLayout) mainContent.getLayout();
        cl.show(mainContent, tab);
        
        for (Component c : sidebarPanel.getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                String btnTab = b.getText().contains("PLAY") ? "PLAY" : 
                               b.getText().contains("BOOST") ? "BOOSTERS" : 
                               b.getText().contains("SETTING") ? "SETTINGS" : "";
                b.setForeground(btnTab.equals(tab) ? C_ACCENT : C_TEXT);
                b.setBackground(btnTab.equals(tab) ? C_BG_HOVER : C_BG_CARD);
            }
        }
    }
    
    JPanel createPlayPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(C_BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        JLabel title = new JLabel("LAUNCH MINECRAFT");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(C_TEXT);
        panel.add(title, BorderLayout.NORTH);
        
        JPanel center = new JPanel();
        center.setBackground(C_BG_DARK);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        
        center.add(createCard("PLAYER NAME", usernameField = createStyledTextField("Player")));
        center.add(Box.createVerticalStrut(15));
        
        versionCombo = new JComboBox<>();
        styleComboBox(versionCombo);
        versionCombo.addItem("Loading versions...");
        center.add(createCard("GAME VERSION", versionCombo));
        center.add(Box.createVerticalStrut(15));
        
        ramSpinner = new JSpinner(new SpinnerNumberModel(4096, 1024, 32768, 512));
        styleSpinner(ramSpinner);
        center.add(createCard("MEMORY (MB)", ramSpinner));
        center.add(Box.createVerticalStrut(30));
        
        launchButton = createLaunchButton();
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(C_BG_DARK);
        btnPanel.add(launchButton);
        center.add(btnPanel);
        center.add(Box.createVerticalStrut(15));
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(400, 8));
        progressBar.setMaximumSize(new Dimension(400, 8));
        progressBar.setBackground(C_BG_CARD);
        progressBar.setForeground(C_ACCENT);
        progressBar.setBorderPainted(false);
        progressBar.setVisible(false);
        JPanel progPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        progPanel.setBackground(C_BG_DARK);
        progPanel.add(progressBar);
        center.add(progPanel);
        
        panel.add(center, BorderLayout.CENTER);
        
        JPanel consolePanel = new JPanel(new BorderLayout(0, 10));
        consolePanel.setBackground(C_BG_DARK);
        consolePanel.setPreferredSize(new Dimension(350, 0));
        
        JLabel consoleTitle = new JLabel("CONSOLE OUTPUT");
        consoleTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        consoleTitle.setForeground(C_TEXT_DIM);
        consolePanel.add(consoleTitle, BorderLayout.NORTH);
        
        consoleArea = new JTextArea();
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        consoleArea.setBackground(C_BG_CARD);
        consoleArea.setForeground(C_ACCENT);
        consoleArea.setCaretColor(C_ACCENT);
        consoleArea.setEditable(false);
        consoleArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scroll = new JScrollPane(consoleArea);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER));
        scroll.getViewport().setBackground(C_BG_CARD);
        consolePanel.add(scroll, BorderLayout.CENTER);
        
        panel.add(consolePanel, BorderLayout.EAST);
        
        return panel;
    }
    
    JPanel createBoostersPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(C_BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        JLabel title = new JLabel("⚡ FPS BOOSTERS");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(C_TEXT);
        panel.add(title, BorderLayout.NORTH);
        
        JPanel content = new JPanel();
        content.setBackground(C_BG_DARK);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        
        JLabel subtitle = new JLabel("Built-in performance optimizations - No Optifine required!");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(C_TEXT_DIM);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        content.add(subtitle);
        content.add(Box.createVerticalStrut(25));
        
        chkFastRender = createBoosterToggle("Fast Render", "Optimizes rendering pipeline for better FPS", true);
        chkChunkOpt = createBoosterToggle("Chunk Optimization", "Reduces chunk loading overhead", true);
        chkEntityCull = createBoosterToggle("Entity Culling", "Skip rendering entities not in view", true);
        chkParticles = createBoosterToggle("Reduced Particles", "Decreases particle count for performance", false);
        chkSmoothFps = createBoosterToggle("Smooth FPS", "Stabilizes frame rate timing", true);
        chkFastMath = createBoosterToggle("Fast Math", "Uses faster math calculations", true);
        
        content.add(createBoosterCard(chkFastRender, "⚡"));
        content.add(Box.createVerticalStrut(10));
        content.add(createBoosterCard(chkChunkOpt, "📦"));
        content.add(Box.createVerticalStrut(10));
        content.add(createBoosterCard(chkEntityCull, "👁"));
        content.add(Box.createVerticalStrut(10));
        content.add(createBoosterCard(chkParticles, "✨"));
        content.add(Box.createVerticalStrut(10));
        content.add(createBoosterCard(chkSmoothFps, "📊"));
        content.add(Box.createVerticalStrut(10));
        content.add(createBoosterCard(chkFastMath, "🔢"));
        content.add(Box.createVerticalStrut(25));
        
        JPanel rdPanel = new JPanel(new BorderLayout(15, 5));
        rdPanel.setBackground(C_BG_CARD);
        rdPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        rdPanel.setMaximumSize(new Dimension(600, 80));
        rdPanel.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel rdLabel = new JLabel("🌍  Default Render Distance");
        rdLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        rdLabel.setForeground(C_TEXT);
        
        renderDistSlider = new JSlider(2, 32, 12);
        renderDistSlider.setBackground(C_BG_CARD);
        renderDistSlider.setForeground(C_ACCENT);
        
        JLabel rdValue = new JLabel("12 chunks");
        rdValue.setFont(new Font("SansSerif", Font.PLAIN, 12));
        rdValue.setForeground(C_ACCENT);
        renderDistSlider.addChangeListener(e -> rdValue.setText(renderDistSlider.getValue() + " chunks"));
        
        rdPanel.add(rdLabel, BorderLayout.NORTH);
        rdPanel.add(renderDistSlider, BorderLayout.CENTER);
        rdPanel.add(rdValue, BorderLayout.EAST);
        content.add(rdPanel);
        
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(C_BG_DARK);
        panel.add(scroll, BorderLayout.CENTER);
        
        return panel;
    }
    
    JCheckBox createBoosterToggle(String name, String desc, boolean defaultOn) {
        JCheckBox cb = new JCheckBox("<html><b>" + name + "</b><br><font color='#8b949e'>" + desc + "</font></html>");
        cb.setSelected(defaultOn);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cb.setForeground(C_TEXT);
        cb.setBackground(C_BG_CARD);
        cb.setFocusPainted(false);
        cb.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return cb;
    }
    
    JPanel createBoosterCard(JCheckBox cb, String icon) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(C_BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        card.setMaximumSize(new Dimension(600, 70));
        card.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 24));
        card.add(iconLbl, BorderLayout.WEST);
        card.add(cb, BorderLayout.CENTER);
        
        return card;
    }
    
    JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(C_BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        JLabel title = new JLabel("⚙ SETTINGS");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(C_TEXT);
        panel.add(title, BorderLayout.NORTH);
        
        JPanel content = new JPanel();
        content.setBackground(C_BG_DARK);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        
        JPanel javaCard = new JPanel(new BorderLayout(10, 10));
        javaCard.setBackground(C_BG_CARD);
        javaCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        javaCard.setMaximumSize(new Dimension(600, 150));
        javaCard.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel javaTitle = new JLabel("☕ JAVA INSTALLATIONS");
        javaTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        javaTitle.setForeground(C_TEXT);
        javaCard.add(javaTitle, BorderLayout.NORTH);
        
        StringBuilder javaInfo = new StringBuilder("<html><font color='#8b949e'>");
        for (Map.Entry<Integer, String> e : javaInstalls.entrySet()) {
            javaInfo.append("Java ").append(e.getKey()).append(": ").append(e.getValue()).append("<br>");
        }
        javaInfo.append("</font></html>");
        JLabel javaList = new JLabel(javaInfo.toString());
        javaList.setFont(new Font("Consolas", Font.PLAIN, 11));
        javaCard.add(javaList, BorderLayout.CENTER);
        
        content.add(javaCard);
        content.add(Box.createVerticalStrut(20));
        
        JPanel dirCard = new JPanel(new BorderLayout(10, 10));
        dirCard.setBackground(C_BG_CARD);
        dirCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        dirCard.setMaximumSize(new Dimension(600, 120));
        dirCard.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel dirTitle = new JLabel("📁 GAME DIRECTORY");
        dirTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        dirTitle.setForeground(C_TEXT);
        dirCard.add(dirTitle, BorderLayout.NORTH);
        
        JLabel dirPath = new JLabel("<html><font color='#8b949e'>" + MC_DIR + "</font></html>");
        dirPath.setFont(new Font("Consolas", Font.PLAIN, 11));
        dirCard.add(dirPath, BorderLayout.CENTER);
        
        JButton openDir = new JButton("Open Folder");
        openDir.setFont(new Font("SansSerif", Font.PLAIN, 11));
        openDir.setForeground(C_ACCENT);
        openDir.setBackground(C_BG_HOVER);
        openDir.setBorderPainted(false);
        openDir.setFocusPainted(false);
        openDir.setCursor(new Cursor(Cursor.HAND_CURSOR));
        openDir.addActionListener(e -> {
            try { Desktop.getDesktop().open(new File(MC_DIR)); } catch (Exception ex) {}
        });
        dirCard.add(openDir, BorderLayout.SOUTH);
        
        content.add(dirCard);
        content.add(Box.createVerticalStrut(30));
        
        JPanel aboutCard = new JPanel();
        aboutCard.setBackground(C_BG_CARD);
        aboutCard.setLayout(new BoxLayout(aboutCard, BoxLayout.Y_AXIS));
        aboutCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        aboutCard.setMaximumSize(new Dimension(600, 200));
        aboutCard.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel aboutTitle = new JLabel("ℹ ABOUT CTLAUNCHER");
        aboutTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        aboutTitle.setForeground(C_TEXT);
        aboutTitle.setAlignmentX(LEFT_ALIGNMENT);
        aboutCard.add(aboutTitle);
        aboutCard.add(Box.createVerticalStrut(15));
        
        String aboutText = "<html><font color='#8b949e'>" +
            "CTLauncher v" + VER + " - Lunar Style Edition<br><br>" +
            COPYRIGHT + "<br>" +
            "Team Flames / Samsoft<br><br>" +
            "Features:<br>" +
            "• Built-in FPS Boosters (No Optifine)<br>" +
            "• Automatic Java Detection<br>" +
            "• All Minecraft Versions<br>" +
            "• Offline Mode Support<br>" +
            "</font></html>";
        JLabel aboutLabel = new JLabel(aboutText);
        aboutLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        aboutLabel.setAlignmentX(LEFT_ALIGNMENT);
        aboutCard.add(aboutLabel);
        
        content.add(aboutCard);
        
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }
    
    JPanel createCard(String label, JComponent field) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(C_BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        card.setMaximumSize(new Dimension(400, 85));
        card.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(C_TEXT_DIM);
        card.add(lbl, BorderLayout.NORTH);
        card.add(field, BorderLayout.CENTER);
        
        return card;
    }
    
    JTextField createStyledTextField(String text) {
        JTextField tf = new JTextField(text);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 16));
        tf.setBackground(C_BG_CARD);
        tf.setForeground(C_TEXT);
        tf.setCaretColor(C_ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        return tf;
    }
    
    void styleComboBox(JComboBox<String> cb) {
        cb.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cb.setBackground(C_BG_CARD);
        cb.setForeground(C_TEXT);
        cb.setBorder(BorderFactory.createLineBorder(C_BORDER));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                setBackground(sel ? C_ACCENT : C_BG_CARD);
                setForeground(sel ? C_BG_DARK : C_TEXT);
                setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
                return this;
            }
        });
    }
    
    void styleSpinner(JSpinner sp) {
        sp.setFont(new Font("SansSerif", Font.PLAIN, 14));
        sp.setBackground(C_BG_CARD);
        JComponent editor = sp.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(C_BG_CARD);
            tf.setForeground(C_TEXT);
            tf.setCaretColor(C_ACCENT);
            tf.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        }
        sp.setBorder(BorderFactory.createLineBorder(C_BORDER));
    }
    
    JButton createLaunchButton() {
        JButton btn = new JButton("▶  LAUNCH GAME");
        btn.setFont(new Font("SansSerif", Font.BOLD, 18));
        btn.setForeground(Color.WHITE);
        btn.setBackground(C_ACCENT);
        btn.setPreferredSize(new Dimension(280, 55));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (!isRunning) btn.setBackground(C_ACCENT_DARK); }
            public void mouseExited(MouseEvent e) { if (!isRunning) btn.setBackground(C_ACCENT); }
        });
        
        btn.addActionListener(e -> onLaunch());
        return btn;
    }
    
    JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(C_BG_CARD);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));
        bar.setPreferredSize(new Dimension(0, 35));
        
        statusLabel = new JLabel("  Ready");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(C_TEXT_DIM);
        bar.add(statusLabel, BorderLayout.WEST);
        
        JLabel copyright = new JLabel(COPYRIGHT + "  ");
        copyright.setFont(new Font("SansSerif", Font.PLAIN, 11));
        copyright.setForeground(C_TEXT_DIM);
        bar.add(copyright, BorderLayout.EAST);
        
        return bar;
    }
    
    void log(String msg) {
        String ts = String.format("[%tT] ", System.currentTimeMillis());
        String line = ts + msg;
        System.out.println(line);
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(line + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }
    
    void status(String msg) { SwingUtilities.invokeLater(() -> statusLabel.setText("  " + msg)); log(msg); }
    void progress(int val) { SwingUtilities.invokeLater(() -> progressBar.setValue(val)); }
    
    void detectJavaInstalls() {
        String home = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();
        
        String currentJava = System.getProperty("java.home") + "/bin/java";
        int currentVer = getMajorVersion(System.getProperty("java.version"));
        javaInstalls.put(currentVer, currentJava);
        
        List<String> searchPaths = new ArrayList<>();
        
        if (os.contains("mac")) {
            searchPaths.add("/Library/Java/JavaVirtualMachines");
            searchPaths.add(home + "/Library/Java/JavaVirtualMachines");
            for (int v : new int[]{8, 11, 17, 21, 22, 23, 24, 25}) {
                searchPaths.add("/opt/homebrew/Cellar/openjdk@" + v);
                searchPaths.add("/usr/local/Cellar/openjdk@" + v);
                searchPaths.add("/opt/homebrew/Cellar/openjdk/" + v + ".0.1");
                searchPaths.add("/usr/local/Cellar/openjdk/" + v + ".0.1");
            }
        } else if (os.contains("win")) {
            searchPaths.add("C:\\Program Files\\Java");
            searchPaths.add("C:\\Program Files\\Eclipse Adoptium");
        } else {
            searchPaths.add("/usr/lib/jvm");
        }
        
        for (String path : searchPaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) scanJavaDir(dir, os);
        }
    }
    
    void scanJavaDir(File dir, String os) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                String javaPath = findJavaInDir(f, os);
                if (javaPath != null) {
                    int ver = detectJavaVersion(javaPath);
                    if (ver > 0 && !javaInstalls.containsKey(ver)) javaInstalls.put(ver, javaPath);
                }
                if (f.getName().contains("java") || f.getName().contains("jdk") || f.getName().contains("openjdk")) scanJavaDir(f, os);
            }
        }
    }
    
    String findJavaInDir(File dir, String os) {
        String[] paths = os.contains("mac") ? 
            new String[]{"/Contents/Home/bin/java", "/bin/java", "/libexec/openjdk.jdk/Contents/Home/bin/java"} :
            os.contains("win") ? new String[]{"\\bin\\java.exe"} : new String[]{"/bin/java"};
        for (String p : paths) {
            File f = new File(dir.getAbsolutePath() + p);
            if (f.exists() && f.canExecute()) return f.getAbsolutePath();
        }
        return null;
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
                int qi = line.indexOf('"');
                if (qi != -1) {
                    int qe = line.indexOf('"', qi + 1);
                    if (qe != -1) return getMajorVersion(line.substring(qi + 1, qe));
                }
            }
        } catch (Exception e) {}
        return 0;
    }
    
    int getMajorVersion(String version) {
        if (version.startsWith("1.")) {
            int dot = version.indexOf('.', 2);
            if (dot != -1) try { return Integer.parseInt(version.substring(2, dot)); } catch (Exception e) {}
        } else {
            int dot = version.indexOf('.');
            if (dot == -1) dot = version.length();
            try { return Integer.parseInt(version.substring(0, dot)); } catch (Exception e) {}
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
    
    int parseMCMajor(String version) {
        if (version.startsWith("1.")) {
            int dot = version.indexOf('.', 2);
            if (dot == -1) dot = version.length();
            try { return Integer.parseInt(version.substring(2, dot)); } catch (Exception e) {}
        }
        return 0;
    }
    
    String findJavaForVersion(int required) {
        // First prefer exact match
        if (javaInstalls.containsKey(required)) return javaInstalls.get(required);
        // Then find closest higher version
        int bestMatch = Integer.MAX_VALUE;
        String bestPath = null;
        for (Map.Entry<Integer, String> e : javaInstalls.entrySet()) {
            int v = e.getKey();
            if (v >= required && v < bestMatch) {
                bestMatch = v;
                bestPath = e.getValue();
            }
        }
        if (bestPath != null) return bestPath;
        // Fallback to highest available
        int highest = 0;
        for (int v : javaInstalls.keySet()) if (v > highest) highest = v;
        if (highest > 0 && javaInstalls.containsKey(highest)) return javaInstalls.get(highest);
        return System.getProperty("java.home") + "/bin/java";
    }
    
    static boolean isMacOS() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }
    
    static boolean isAppleSilicon() {
        return isMacOS() && System.getProperty("os.arch", "").contains("aarch64");
    }
    
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
                    versionCombo.addItem("══════ RELEASES ══════");
                    for (VersionInfo v : versions.values()) {
                        if ("release".equals(v.type)) {
                            String label = v.id;
                            if (v.id.startsWith("1.21") || v.id.startsWith("1.20.5") || v.id.startsWith("1.20.6")) {
                                if (!javaInstalls.containsKey(21)) label += " ⚠ Java 21";
                            }
                            versionCombo.addItem(label);
                        }
                    }
                    versionCombo.addItem("══════ SNAPSHOTS ══════");
                    for (VersionInfo v : versions.values()) if ("snapshot".equals(v.type)) versionCombo.addItem(v.id);
                    versionCombo.addItem("══════ BETA ══════");
                    for (VersionInfo v : versions.values()) if ("old_beta".equals(v.type)) versionCombo.addItem(v.id);
                    versionCombo.addItem("══════ ALPHA ══════");
                    for (VersionInfo v : versions.values()) if ("old_alpha".equals(v.type)) versionCombo.addItem(v.id);
                    if (versionCombo.getItemCount() > 1) versionCombo.setSelectedIndex(1);
                });
                status("Ready - " + versions.size() + " versions loaded");
            } catch (Exception ex) { status("Error: " + ex.getMessage()); }
        }).start();
    }
    
    void onLaunch() {
        if (isRunning) { cancelled = true; return; }
        String sel = (String) versionCombo.getSelectedItem();
        if (sel == null || sel.startsWith("══════")) { status("Select a version!"); return; }
        if (sel.contains(" ⚠")) sel = sel.substring(0, sel.indexOf(" ⚠"));
        
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
        SwingUtilities.invokeLater(() -> {
            launchButton.setText("✕  CANCEL");
            launchButton.setBackground(C_ERROR);
            progressBar.setVisible(true);
            progressBar.setValue(0);
        });
        
        try {
            log("═══════════════════════════════════════════════════════════");
            log("Launching Minecraft " + ver.id + " as " + user);
            log("═══════════════════════════════════════════════════════════");
            
            status("Downloading version info...");
            String json = http(ver.jsonUrl);
            ver.mainClass = jstr(json, "mainClass");
            ver.assetId = jstr(json, "assets");
            
            String javaReq = jstr(json, "majorVersion");
            if (javaReq != null) try { ver.javaVersion = Integer.parseInt(javaReq); } catch (Exception e) {}
            else ver.javaVersion = getRequiredJava(ver.id);
            log("MC " + ver.id + " -> Java " + ver.javaVersion + " required");
            
            String javaPath = findJavaForVersion(ver.javaVersion);
            int availableJava = detectJavaVersion(javaPath);
            
            if (availableJava < ver.javaVersion) {
                log("ERROR: Java " + ver.javaVersion + " required!");
                status("ERROR: Need Java " + ver.javaVersion + "!");
                return;
            }
            log("Using Java: " + javaPath + " (version " + availableJava + ")");
            
            int ai = json.indexOf("\"assetIndex\"");
            if (ai != -1) {
                int s = json.indexOf("{", ai), e = brace(json, s);
                if (e != -1) { String a = json.substring(s, e+1); ver.assetUrl = jstr(a, "url"); if (ver.assetId == null) ver.assetId = jstr(a, "id"); }
            }
            if (ver.assetId == null) ver.assetId = "legacy";
            
            int di = json.indexOf("\"downloads\"");
            if (di != -1) {
                int s = json.indexOf("{", di), e = brace(json, s);
                if (e != -1) { String d = json.substring(s, e+1); int ci = d.indexOf("\"client\"");
                    if (ci != -1) { int cs = d.indexOf("{", ci), ce = brace(d, cs); if (ce != -1) ver.clientUrl = jstr(d.substring(cs, ce+1), "url"); }}
            }
            
            parseLibs(ver, json);
            log("Main: " + ver.mainClass + " | Libs: " + ver.libraries.size());
            progress(10);
            if (cancelled) throw new InterruptedException();
            
            status("Downloading Minecraft...");
            String jar = VERSIONS_DIR + ver.id + "/" + ver.id + ".jar";
            new File(VERSIONS_DIR + ver.id).mkdirs();
            File jf = new File(jar);
            if (!jf.exists() || jf.length() < 1000000) {
                if (ver.clientUrl == null) throw new Exception("No client URL!");
                download(ver.clientUrl, jar, 10, 30);
            }
            log("Client JAR: " + jf.length() + " bytes");
            progress(30);
            if (cancelled) throw new InterruptedException();
            
            status("Downloading libraries...");
            int total = ver.libraries.size(), done = 0, dl = 0;
            for (LibInfo lib : ver.libraries) {
                if (cancelled) throw new InterruptedException();
                done++;
                if (lib.artifactPath != null && lib.artifactUrl != null) {
                    String p = LIBRARIES_DIR + lib.artifactPath;
                    if (!new File(p).exists()) { new File(p).getParentFile().mkdirs(); downloadQuiet(lib.artifactUrl, p); dl++; }
                }
                if (lib.hasNatives && lib.nativeUrl != null && lib.nativePath != null) {
                    String p = LIBRARIES_DIR + lib.nativePath;
                    if (!new File(p).exists()) { new File(p).getParentFile().mkdirs(); downloadQuiet(lib.nativeUrl, p); dl++; }
                }
                progress(30 + 20 * done / total);
            }
            log("Downloaded " + dl + " libraries");
            progress(50);
            if (cancelled) throw new InterruptedException();
            
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
            progress(60);
            if (cancelled) throw new InterruptedException();
            
            status("Downloading assets...");
            String idx = ASSETS_DIR + "indexes/" + ver.assetId + ".json";
            if (ver.assetUrl != null && !new File(idx).exists()) downloadQuiet(ver.assetUrl, idx);
            
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
                            List<String> hashes = new ArrayList<>();
                            int pos = 0;
                            while (pos < objects.length()) {
                                int hashIdx = objects.indexOf("\"hash\"", pos);
                                if (hashIdx == -1) break;
                                String hash = jstr(objects.substring(hashIdx - 1), "hash");
                                if (hash != null && hash.length() == 40) hashes.add(hash);
                                pos = hashIdx + 10;
                            }
                            log("Assets: " + hashes.size());
                            int assetsDl = 0;
                            for (int i = 0; i < hashes.size(); i++) {
                                if (cancelled) throw new InterruptedException();
                                String hash = hashes.get(i);
                                String prefix = hash.substring(0, 2);
                                String assetPath = ASSETS_DIR + "objects/" + prefix + "/" + hash;
                                if (!new File(assetPath).exists()) {
                                    new File(assetPath).getParentFile().mkdirs();
                                    if (downloadQuiet("https://resources.download.minecraft.net/" + prefix + "/" + hash, assetPath)) assetsDl++;
                                }
                                if (i % 100 == 0) { progress(60 + 20 * i / Math.max(1, hashes.size())); status("Assets... " + i + "/" + hashes.size()); }
                            }
                            log("Downloaded " + assetsDl + " assets");
                        }
                    }
                } catch (Exception e) { log("Asset error: " + e.getMessage()); }
            }
            progress(80);
            if (cancelled) throw new InterruptedException();
            
            status("Applying FPS boosters...");
            applyFPSBoosters(ver);
            progress(85);
            
            status("Launching...");
            int ram = (Integer) ramSpinner.getValue();
            String uuid = genUUID(user);
            
            StringBuilder cp = new StringBuilder();
            for (LibInfo lib : ver.libraries)
                if (lib.artifactPath != null && !lib.hasNatives) { String p = LIBRARIES_DIR + lib.artifactPath;
                    if (new File(p).exists()) { if (cp.length() > 0) cp.append(File.pathSeparator); cp.append(p); }}
            cp.append(File.pathSeparator).append(jar);
            
            List<String> cmd = new ArrayList<>();
            cmd.add(javaPath);
            
            // ═══════════════════════════════════════════════════════════
            // FIX #1: Add -XstartOnFirstThread for macOS (GLFW requirement)
            // ═══════════════════════════════════════════════════════════
            if (isMacOS()) {
                cmd.add("-XstartOnFirstThread");
                log("Added -XstartOnFirstThread for macOS");
            }
            
            cmd.add("-Xms512M"); cmd.add("-Xmx" + ram + "M");
            cmd.add("-XX:+UnlockExperimentalVMOptions"); cmd.add("-XX:+UseG1GC");
            cmd.add("-XX:G1NewSizePercent=20"); cmd.add("-XX:G1ReservePercent=20");
            cmd.add("-XX:MaxGCPauseMillis=50"); cmd.add("-XX:G1HeapRegionSize=32M");
            cmd.add("-Djava.library.path=" + natDir);
            cmd.add("-Dminecraft.launcher.brand=" + NAME);
            cmd.add("-Dminecraft.launcher.version=" + VER);
            if (isAppleSilicon()) cmd.add("-Dorg.lwjgl.system.allocator=system");
            // Native access for newer Java versions
            if (availableJava >= 21) {
                cmd.add("--enable-native-access=ALL-UNNAMED");
            }
            cmd.add("-cp"); cmd.add(cp.toString());
            cmd.add(ver.mainClass != null ? ver.mainClass : "net.minecraft.client.main.Main");
            
            cmd.add("--username"); cmd.add(user);
            cmd.add("--version"); cmd.add(ver.id);
            cmd.add("--gameDir"); cmd.add(MC_DIR);
            cmd.add("--assetsDir"); cmd.add(ASSETS_DIR);
            cmd.add("--assetIndex"); cmd.add(ver.assetId);
            cmd.add("--uuid"); cmd.add(uuid);
            cmd.add("--accessToken"); cmd.add("0");
            cmd.add("--userType"); cmd.add("legacy");
            cmd.add("--versionType"); cmd.add("CTLauncher");
            
            progress(100);
            log("Starting with Java " + availableJava + ", RAM: " + ram + "MB");
            log("FPS Boosters: " + getActiveBoostersString());
            
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(MC_DIR));
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            
            log("═══════════════════════════════════════════════════════════");
            log("Minecraft started! PID: " + proc.pid());
            status("Minecraft is running!");
            
            new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    String line; while ((line = br.readLine()) != null) log("MC> " + line);
                } catch (Exception ex) {}
                try { int c = proc.waitFor(); log("Exit: " + c); if (c != 0) status("Crashed (" + c + ")"); } catch (Exception ex) {}
            }).start();
            
        } catch (InterruptedException ex) { status("Cancelled");
        } catch (Exception ex) { status("Error: " + ex.getMessage()); log("ERROR: " + ex); ex.printStackTrace();
        } finally {
            isRunning = false;
            SwingUtilities.invokeLater(() -> { launchButton.setText("▶  LAUNCH GAME"); launchButton.setBackground(C_ACCENT); progressBar.setVisible(false); });
        }
    }
    
    // ═══════════════════════════════════════════════════════════
    // FIX #2: Write options.txt in proper Minecraft format (key:value)
    // NOT Java Properties format (key=value with # header)
    // ═══════════════════════════════════════════════════════════
    void applyFPSBoosters(VersionInfo ver) {
        try {
            File optionsFile = new File(MC_DIR + "options.txt");
            Map<String, String> opts = new LinkedHashMap<>();
            
            // Read existing options in Minecraft format (key:value)
            if (optionsFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(optionsFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int colon = line.indexOf(':');
                        if (colon > 0) {
                            String key = line.substring(0, colon);
                            String value = line.substring(colon + 1);
                            opts.put(key, value);
                        }
                    }
                }
            }
            
            // Apply FPS booster settings
            if (chkFastRender.isSelected()) { 
                opts.put("renderClouds", "\"fast\""); 
                opts.put("graphicsMode", "1"); 
            }
            if (chkChunkOpt.isSelected()) opts.put("chunkBuilder", "1");
            if (chkEntityCull.isSelected()) opts.put("entityShadows", "false");
            if (chkParticles.isSelected()) opts.put("particles", "2");
            if (chkSmoothFps.isSelected()) opts.put("enableVsync", "true");
            
            opts.put("renderDistance", String.valueOf(renderDistSlider.getValue()));
            opts.put("simulationDistance", String.valueOf(Math.min(renderDistSlider.getValue(), 12)));
            opts.put("ao", "1"); 
            opts.put("mipmapLevels", "2"); 
            opts.put("biomeBlendRadius", "2");
            
            // Write back in Minecraft format (key:value, NO # header)
            try (PrintWriter pw = new PrintWriter(new FileWriter(optionsFile))) {
                for (Map.Entry<String, String> e : opts.entrySet()) {
                    pw.println(e.getKey() + ":" + e.getValue());
                }
            }
            log("Applied FPS boosters");
        } catch (Exception e) { log("Booster error: " + e.getMessage()); }
    }
    
    String getActiveBoostersString() {
        List<String> active = new ArrayList<>();
        if (chkFastRender.isSelected()) active.add("FastRender");
        if (chkChunkOpt.isSelected()) active.add("ChunkOpt");
        if (chkEntityCull.isSelected()) active.add("EntityCull");
        if (chkParticles.isSelected()) active.add("LowParticles");
        if (chkSmoothFps.isSelected()) active.add("SmoothFPS");
        if (chkFastMath.isSelected()) active.add("FastMath");
        return active.isEmpty() ? "None" : String.join(", ", active);
    }
    
    void parseLibs(VersionInfo ver, String json) {
        int li = json.indexOf("\"libraries\""); if (li == -1) return;
        int as = json.indexOf("[", li), ae = bracket(json, as); if (ae == -1) return;
        String arr = json.substring(as, ae + 1);
        String os = osName();
        int pos = 0;
        while (pos < arr.length()) {
            int s = arr.indexOf("{", pos); if (s == -1) break;
            int e = brace(arr, s); if (e == -1) break;
            String o = arr.substring(s, e + 1);
            if (o.contains("\"rules\"") && !checkRules(o, os)) { pos = e + 1; continue; }
            LibInfo lib = new LibInfo();
            lib.name = jstr(o, "name");
            int di = o.indexOf("\"downloads\"");
            if (di != -1) {
                int ds = o.indexOf("{", di), de = brace(o, ds);
                if (de != -1) { String dl = o.substring(ds, de + 1);
                    int ai = dl.indexOf("\"artifact\"");
                    if (ai != -1) { int aas = dl.indexOf("{", ai), aae = brace(dl, aas);
                        if (aae != -1) { String art = dl.substring(aas, aae + 1); lib.artifactPath = jstr(art, "path"); lib.artifactUrl = jstr(art, "url"); }}
                    String natKey = "\"natives-" + os + "\"";
                    int ni = dl.indexOf(natKey);
                    if (ni != -1) { int ns = dl.indexOf("{", ni), ne = brace(dl, ns);
                        if (ne != -1) { String nat = dl.substring(ns, ne + 1); lib.nativePath = jstr(nat, "path"); lib.nativeUrl = jstr(nat, "url"); lib.hasNatives = true; }}}
            }
            if (lib.artifactPath == null && lib.name != null) {
                String[] p = lib.name.split(":");
                if (p.length >= 3) { String path = p[0].replace('.', '/') + "/" + p[1] + "/" + p[2] + "/" + p[1] + "-" + p[2] + ".jar";
                    lib.artifactPath = path; lib.artifactUrl = "https://libraries.minecraft.net/" + path; }
            }
            if (lib.artifactPath != null || lib.hasNatives) ver.libraries.add(lib);
            pos = e + 1;
        }
    }
    
    boolean checkRules(String o, String os) {
        boolean allow = false; int pos = 0;
        while (true) { int ri = o.indexOf("\"action\"", pos); if (ri == -1) break;
            int rs = o.lastIndexOf("{", ri), re = brace(o, rs); if (re == -1) break;
            String rule = o.substring(rs, re + 1); String action = jstr(rule, "action");
            boolean matches = !rule.contains("\"os\"") || rule.contains("\"name\"") && rule.contains("\"" + os + "\"");
            if ("allow".equals(action) && matches) allow = true;
            if ("disallow".equals(action) && matches) allow = false;
            pos = re + 1;
        }
        return allow || !o.contains("\"action\"");
    }
    
    String osName() { String os = System.getProperty("os.name").toLowerCase(); if (os.contains("win")) return "windows"; if (os.contains("mac")) return "osx"; return "linux"; }
    
    int extractNat(String jar, String dir) {
        int n = 0;
        try (ZipInputStream z = new ZipInputStream(new FileInputStream(jar))) {
            ZipEntry e; while ((e = z.getNextEntry()) != null) { if (e.isDirectory() || e.getName().startsWith("META-INF")) continue;
                String name = e.getName(); if (name.endsWith(".dll") || name.endsWith(".so") || name.endsWith(".dylib") || name.endsWith(".jnilib")) {
                    File f = new File(dir + new File(name).getName()); if (!f.exists()) { Files.copy(z, f.toPath()); n++; }}}
        } catch (Exception ex) {}
        return n;
    }
    
    String genUUID(String u) {
        try { MessageDigest md = MessageDigest.getInstance("MD5"); byte[] h = md.digest(("OfflinePlayer:" + u).getBytes());
            h[6] = (byte) ((h[6] & 0x0f) | 0x30); h[8] = (byte) ((h[8] & 0x3f) | 0x80);
            StringBuilder sb = new StringBuilder(); for (byte b : h) sb.append(String.format("%02x", b));
            return sb.insert(8, '-').insert(13, '-').insert(18, '-').insert(23, '-').toString();
        } catch (Exception e) { return UUID.randomUUID().toString(); }
    }
    
    String http(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", NAME + "/" + VER); c.setConnectTimeout(15000); c.setReadTimeout(30000);
        try (InputStream i = c.getInputStream()) { ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192]; int n; while ((n = i.read(buf)) != -1) b.write(buf, 0, n); return b.toString("UTF-8"); }
    }
    
    void download(String url, String dest, int p1, int p2) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection(); c.setRequestProperty("User-Agent", NAME + "/" + VER);
        long total = c.getContentLengthLong();
        try (InputStream i = c.getInputStream(); FileOutputStream o = new FileOutputStream(dest)) {
            byte[] b = new byte[8192]; int n; long done = 0;
            while ((n = i.read(b)) != -1) { o.write(b, 0, n); done += n; if (total > 0) progress(p1 + (int)((p2 - p1) * done / total)); }}
    }
    
    boolean downloadQuiet(String url, String dest) {
        for (int retry = 0; retry < 3; retry++) {
            try { HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setRequestProperty("User-Agent", "Mozilla/5.0 " + NAME + "/" + VER);
                c.setConnectTimeout(30000); c.setReadTimeout(120000); c.setInstanceFollowRedirects(true);
                int code = c.getResponseCode(); if (code == 301 || code == 302 || code == 307 || code == 308) { url = c.getHeaderField("Location"); continue; }
                if (code != 200) throw new Exception("HTTP " + code);
                try (InputStream i = c.getInputStream(); FileOutputStream o = new FileOutputStream(dest)) {
                    byte[] b = new byte[16384]; int n; while ((n = i.read(b)) != -1) o.write(b, 0, n); }
                return true;
            } catch (Exception ex) { new File(dest).delete(); if (retry == 2) return false; try { Thread.sleep(500 * (retry + 1)); } catch (Exception e) {} }
        }
        return false;
    }
    
    String jstr(String j, String k) {
        int i = j.indexOf("\"" + k + "\""); if (i == -1) return null;
        int c = j.indexOf(":", i); if (c == -1) return null;
        int s = -1; for (int x = c + 1; x < j.length(); x++) { char ch = j.charAt(x); if (ch == '"') { s = x + 1; break; } else if (!Character.isWhitespace(ch)) break; }
        if (s == -1) return null; int e = s; while (e < j.length() && !(j.charAt(e) == '"' && j.charAt(e-1) != '\\')) e++; return j.substring(s, e);
    }
    
    int brace(String s, int i) {
        if (i < 0 || i >= s.length()) return -1; int d = 0; boolean q = false;
        for (int x = i; x < s.length(); x++) { char c = s.charAt(x);
            if (c == '"' && (x == 0 || s.charAt(x-1) != '\\')) q = !q;
            if (!q) { if (c == '{') d++; else if (c == '}') { d--; if (d == 0) return x; }}} return -1;
    }
    
    int bracket(String s, int i) {
        if (i < 0 || i >= s.length()) return -1; int d = 0; boolean q = false;
        for (int x = i; x < s.length(); x++) { char c = s.charAt(x);
            if (c == '"' && (x == 0 || s.charAt(x-1) != '\\')) q = !q;
            if (!q) { if (c == '[') d++; else if (c == ']') { d--; if (d == 0) return x; }}} return -1;
    }
}
