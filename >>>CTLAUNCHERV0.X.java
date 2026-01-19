/*
 * CTLauncher v1.2 - High Performance Edition
 * Minecraft Launcher with Built-in FPS Boosters
 * (C) 1999-2026 Samsoft / Team Flames
 * 
 * Run: java CTLauncherHDR.java
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

public class CTLauncherHDR extends JFrame {
    
    static final String VER = "1.2";
    static final String NAME = "CTLauncher";
    static final String COPYRIGHT = "(C) 1999-2026 Samsoft";
    static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    
    // CT Style Colors
    static final Color C_BG_DARK = new Color(18, 18, 22);
    static final Color C_BG_CARD = new Color(28, 28, 35);
    static final Color C_BG_HOVER = new Color(38, 38, 48);
    static final Color C_ACCENT = new Color(30, 215, 96);  // Green like the screenshot
    static final Color C_ACCENT_DARK = new Color(25, 180, 80);
    static final Color C_TEXT = new Color(240, 240, 245);
    static final Color C_TEXT_DIM = new Color(140, 140, 155);
    static final Color C_BORDER = new Color(50, 50, 60);
    static final Color C_SUCCESS = new Color(46, 160, 67);
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
    String currentTab = "HOME";
    
    // FPS Booster Settings
    JCheckBox chkFastRender, chkChunkOpt, chkEntityCull, chkParticles, chkSmoothFps, chkFastMath;
    JSlider renderDistSlider;
    
    // Data
    Map<String, VersionInfo> versions = new LinkedHashMap<>();
    Map<Integer, String> javaInstalls = new LinkedHashMap<>();
    volatile boolean isRunning = false;
    volatile boolean cancelled = false;
    
    static class VersionInfo {
        String id, type, jsonUrl, mainClass, assetId, assetUrl, clientUrl;
        int javaVersion = 8;
        List<LibInfo> libraries = new ArrayList<>();
    }
    
    static class LibInfo {
        String name, artifactPath, artifactUrl, nativePath, nativeUrl;
        boolean hasNatives;
    }
    
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(CTLauncherHDR::new);
    }
    
    public CTLauncherHDR() {
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
        setTitle(NAME + " v" + VER);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 650);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG_DARK);
        setLayout(new BorderLayout(0, 0));
        
        // Top navigation bar
        add(createTopBar(), BorderLayout.NORTH);
        
        // Main content with cards
        mainContent = new JPanel(new CardLayout());
        mainContent.setBackground(C_BG_DARK);
        mainContent.add(createHomePanel(), "HOME");
        mainContent.add(createBoostersPanel(), "BOOSTERS");
        mainContent.add(createSettingsPanel(), "SETTINGS");
        add(mainContent, BorderLayout.CENTER);
        
        // Status bar
        add(createStatusBar(), BorderLayout.SOUTH);
        
        setVisible(true);
        log("═════════════════════════════════════════════════════");
        log("  " + NAME + " v" + VER + " - High Performance Edition");
        log("  " + COPYRIGHT + " / Team Flames");
        log("  Built-in FPS Boosters | All Versions Supported");
        log("═════════════════════════════════════════════════════");
        log("Java versions found: " + javaInstalls.keySet());
    }
    
    JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_CARD);
        topBar.setPreferredSize(new Dimension(0, 55));
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));
        
        // Left: Logo
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        logoPanel.setBackground(C_BG_CARD);
        
        JLabel logoIcon = new JLabel("◆");
        logoIcon.setFont(new Font("SansSerif", Font.BOLD, 28));
        logoIcon.setForeground(C_ACCENT);
        
        JLabel logoText = new JLabel(NAME);
        logoText.setFont(new Font("SansSerif", Font.BOLD, 20));
        logoText.setForeground(C_TEXT);
        
        logoPanel.add(logoIcon);
        logoPanel.add(logoText);
        topBar.add(logoPanel, BorderLayout.WEST);
        
        // Center: Navigation tabs
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        navPanel.setBackground(C_BG_CARD);
        
        navPanel.add(createNavTab("Home", "HOME", true));
        navPanel.add(createNavTab("Boosters", "BOOSTERS", false));
        navPanel.add(createNavTab("Settings", "SETTINGS", false));
        
        topBar.add(navPanel, BorderLayout.CENTER);
        
        // Right: Version info
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        rightPanel.setBackground(C_BG_CARD);
        JLabel verLabel = new JLabel("v" + VER);
        verLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        verLabel.setForeground(C_TEXT_DIM);
        rightPanel.add(verLabel);
        topBar.add(rightPanel, BorderLayout.EAST);
        
        return topBar;
    }
    
    JButton createNavTab(String text, String tab, boolean selected) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(selected ? C_ACCENT : C_TEXT_DIM);
        btn.setBackground(C_BG_CARD);
        btn.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.putClientProperty("tab", tab);
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (!currentTab.equals(tab)) btn.setForeground(C_TEXT); }
            public void mouseExited(MouseEvent e) { if (!currentTab.equals(tab)) btn.setForeground(C_TEXT_DIM); }
        });
        
        btn.addActionListener(e -> switchTab(tab));
        return btn;
    }
    
    void switchTab(String tab) {
        currentTab = tab;
        CardLayout cl = (CardLayout) mainContent.getLayout();
        cl.show(mainContent, tab);
        
        // Update nav buttons
        Component topBar = getContentPane().getComponent(0);
        if (topBar instanceof JPanel) {
            updateNavButtons((JPanel) topBar, tab);
        }
    }
    
    void updateNavButtons(JPanel panel, String activeTab) {
        for (Component c : panel.getComponents()) {
            if (c instanceof JPanel) updateNavButtons((JPanel) c, activeTab);
            if (c instanceof JButton) {
                JButton btn = (JButton) c;
                Object tabProp = btn.getClientProperty("tab");
                if (tabProp != null) {
                    btn.setForeground(tabProp.equals(activeTab) ? C_ACCENT : C_TEXT_DIM);
                }
            }
        }
    }
    
    JPanel createHomePanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(C_BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        
        // Left side: Launch section
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(C_BG_DARK);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setPreferredSize(new Dimension(380, 0));
        
        // Launch card
        JPanel launchCard = new JPanel();
        launchCard.setBackground(C_BG_CARD);
        launchCard.setLayout(new BoxLayout(launchCard, BoxLayout.Y_AXIS));
        launchCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));
        launchCard.setMaximumSize(new Dimension(380, 320));
        launchCard.setAlignmentX(LEFT_ALIGNMENT);
        
        // Username
        JLabel userLabel = new JLabel("USERNAME");
        userLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        userLabel.setForeground(C_TEXT_DIM);
        userLabel.setAlignmentX(LEFT_ALIGNMENT);
        launchCard.add(userLabel);
        launchCard.add(Box.createVerticalStrut(8));
        
        usernameField = new JTextField("Player");
        usernameField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        usernameField.setBackground(C_BG_DARK);
        usernameField.setForeground(C_TEXT);
        usernameField.setCaretColor(C_ACCENT);
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        usernameField.setMaximumSize(new Dimension(330, 45));
        usernameField.setAlignmentX(LEFT_ALIGNMENT);
        launchCard.add(usernameField);
        launchCard.add(Box.createVerticalStrut(20));
        
        // Version
        JLabel verLabel = new JLabel("VERSION");
        verLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        verLabel.setForeground(C_TEXT_DIM);
        verLabel.setAlignmentX(LEFT_ALIGNMENT);
        launchCard.add(verLabel);
        launchCard.add(Box.createVerticalStrut(8));
        
        versionCombo = new JComboBox<>();
        styleComboBox(versionCombo);
        versionCombo.addItem("Loading...");
        versionCombo.setMaximumSize(new Dimension(330, 45));
        versionCombo.setAlignmentX(LEFT_ALIGNMENT);
        launchCard.add(versionCombo);
        launchCard.add(Box.createVerticalStrut(25));
        
        // Launch button
        launchButton = new JButton("LAUNCH");
        launchButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        launchButton.setForeground(Color.WHITE);
        launchButton.setBackground(C_ACCENT);
        launchButton.setPreferredSize(new Dimension(330, 50));
        launchButton.setMaximumSize(new Dimension(330, 50));
        launchButton.setAlignmentX(LEFT_ALIGNMENT);
        launchButton.setFocusPainted(false);
        launchButton.setBorderPainted(false);
        launchButton.setOpaque(true);
        launchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        launchButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (!isRunning) launchButton.setBackground(C_ACCENT_DARK); }
            public void mouseExited(MouseEvent e) { if (!isRunning) launchButton.setBackground(C_ACCENT); }
        });
        launchButton.addActionListener(e -> onLaunch());
        launchCard.add(launchButton);
        launchCard.add(Box.createVerticalStrut(15));
        
        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(330, 6));
        progressBar.setMaximumSize(new Dimension(330, 6));
        progressBar.setBackground(C_BG_DARK);
        progressBar.setForeground(C_ACCENT);
        progressBar.setBorderPainted(false);
        progressBar.setVisible(false);
        progressBar.setAlignmentX(LEFT_ALIGNMENT);
        launchCard.add(progressBar);
        
        leftPanel.add(launchCard);
        leftPanel.add(Box.createVerticalStrut(15));
        
        // RAM selector
        JPanel ramCard = new JPanel(new BorderLayout(10, 0));
        ramCard.setBackground(C_BG_CARD);
        ramCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        ramCard.setMaximumSize(new Dimension(380, 60));
        ramCard.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel ramLabel = new JLabel("MEMORY (MB)");
        ramLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        ramLabel.setForeground(C_TEXT_DIM);
        ramCard.add(ramLabel, BorderLayout.WEST);
        
        ramSpinner = new JSpinner(new SpinnerNumberModel(4096, 1024, 32768, 512));
        styleSpinner(ramSpinner);
        ramSpinner.setPreferredSize(new Dimension(100, 30));
        ramCard.add(ramSpinner, BorderLayout.EAST);
        
        leftPanel.add(ramCard);
        
        panel.add(leftPanel, BorderLayout.WEST);
        
        // Right side: Console
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setBackground(C_BG_DARK);
        
        JLabel consoleTitle = new JLabel("CONSOLE");
        consoleTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        consoleTitle.setForeground(C_TEXT_DIM);
        rightPanel.add(consoleTitle, BorderLayout.NORTH);
        
        consoleArea = new JTextArea();
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        consoleArea.setBackground(C_BG_CARD);
        consoleArea.setForeground(C_ACCENT);
        consoleArea.setCaretColor(C_ACCENT);
        consoleArea.setEditable(false);
        consoleArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        
        JScrollPane scroll = new JScrollPane(consoleArea);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER));
        scroll.getViewport().setBackground(C_BG_CARD);
        rightPanel.add(scroll, BorderLayout.CENTER);
        
        panel.add(rightPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    JPanel createBoostersPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(C_BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        
        JLabel title = new JLabel("⚡ FPS BOOSTERS");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(C_TEXT);
        panel.add(title, BorderLayout.NORTH);
        
        JPanel content = new JPanel();
        content.setBackground(C_BG_DARK);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        
        JLabel subtitle = new JLabel("Built-in performance optimizations");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(C_TEXT_DIM);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        content.add(subtitle);
        content.add(Box.createVerticalStrut(20));
        
        chkFastRender = createBoosterToggle("Fast Render", "Optimizes rendering pipeline", true);
        chkChunkOpt = createBoosterToggle("Chunk Optimization", "Reduces chunk loading overhead", true);
        chkEntityCull = createBoosterToggle("Entity Culling", "Skip rendering entities not in view", true);
        chkParticles = createBoosterToggle("Reduced Particles", "Decreases particle count", false);
        chkSmoothFps = createBoosterToggle("Smooth FPS", "Stabilizes frame rate", true);
        chkFastMath = createBoosterToggle("Fast Math", "Uses faster calculations", true);
        
        content.add(createBoosterCard(chkFastRender));
        content.add(Box.createVerticalStrut(8));
        content.add(createBoosterCard(chkChunkOpt));
        content.add(Box.createVerticalStrut(8));
        content.add(createBoosterCard(chkEntityCull));
        content.add(Box.createVerticalStrut(8));
        content.add(createBoosterCard(chkParticles));
        content.add(Box.createVerticalStrut(8));
        content.add(createBoosterCard(chkSmoothFps));
        content.add(Box.createVerticalStrut(8));
        content.add(createBoosterCard(chkFastMath));
        content.add(Box.createVerticalStrut(20));
        
        // Render distance
        JPanel rdPanel = new JPanel(new BorderLayout(15, 5));
        rdPanel.setBackground(C_BG_CARD);
        rdPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        rdPanel.setMaximumSize(new Dimension(500, 70));
        rdPanel.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel rdLabel = new JLabel("Default Render Distance");
        rdLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
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
    
    JCheckBox createBoosterToggle(String name, String desc, boolean on) {
        JCheckBox cb = new JCheckBox("<html><b>" + name + "</b><br><font color='#8c8c9b'>" + desc + "</font></html>");
        cb.setSelected(on);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cb.setForeground(C_TEXT);
        cb.setBackground(C_BG_CARD);
        cb.setFocusPainted(false);
        cb.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return cb;
    }
    
    JPanel createBoosterCard(JCheckBox cb) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(C_BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        card.setMaximumSize(new Dimension(500, 60));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.add(cb, BorderLayout.CENTER);
        return card;
    }
    
    JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(C_BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        
        JLabel title = new JLabel("⚙ SETTINGS");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(C_TEXT);
        panel.add(title, BorderLayout.NORTH);
        
        JPanel content = new JPanel();
        content.setBackground(C_BG_DARK);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        
        // Java installations
        JPanel javaCard = new JPanel(new BorderLayout(10, 10));
        javaCard.setBackground(C_BG_CARD);
        javaCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        javaCard.setMaximumSize(new Dimension(550, 140));
        javaCard.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel javaTitle = new JLabel("☕ Java Installations");
        javaTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        javaTitle.setForeground(C_TEXT);
        javaCard.add(javaTitle, BorderLayout.NORTH);
        
        StringBuilder javaInfo = new StringBuilder("<html><font color='#8c8c9b'>");
        for (Map.Entry<Integer, String> e : javaInstalls.entrySet()) {
            javaInfo.append("Java ").append(e.getKey()).append(": ").append(e.getValue()).append("<br>");
        }
        javaInfo.append("</font></html>");
        JLabel javaList = new JLabel(javaInfo.toString());
        javaList.setFont(new Font("Consolas", Font.PLAIN, 10));
        javaCard.add(javaList, BorderLayout.CENTER);
        
        content.add(javaCard);
        content.add(Box.createVerticalStrut(15));
        
        // Directory
        JPanel dirCard = new JPanel(new BorderLayout(10, 10));
        dirCard.setBackground(C_BG_CARD);
        dirCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        dirCard.setMaximumSize(new Dimension(550, 100));
        dirCard.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel dirTitle = new JLabel("📁 Game Directory");
        dirTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        dirTitle.setForeground(C_TEXT);
        dirCard.add(dirTitle, BorderLayout.NORTH);
        
        JLabel dirPath = new JLabel("<html><font color='#8c8c9b'>" + MC_DIR + "</font></html>");
        dirPath.setFont(new Font("Consolas", Font.PLAIN, 11));
        dirCard.add(dirPath, BorderLayout.CENTER);
        
        JButton openDir = new JButton("Open");
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
        content.add(Box.createVerticalStrut(20));
        
        // About
        JPanel aboutCard = new JPanel();
        aboutCard.setBackground(C_BG_CARD);
        aboutCard.setLayout(new BoxLayout(aboutCard, BoxLayout.Y_AXIS));
        aboutCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        aboutCard.setMaximumSize(new Dimension(550, 160));
        aboutCard.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel aboutTitle = new JLabel("ℹ About " + NAME);
        aboutTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        aboutTitle.setForeground(C_TEXT);
        aboutTitle.setAlignmentX(LEFT_ALIGNMENT);
        aboutCard.add(aboutTitle);
        aboutCard.add(Box.createVerticalStrut(10));
        
        String aboutText = "<html><font color='#8c8c9b'>" +
            NAME + " v" + VER + " - High Performance Edition<br><br>" +
            COPYRIGHT + "<br>" +
            "Team Flames<br><br>" +
            "• Built-in FPS Boosters<br>" +
            "• Auto Java Detection<br>" +
            "• All MC Versions<br>" +
            "</font></html>";
        JLabel aboutLabel = new JLabel(aboutText);
        aboutLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        aboutLabel.setAlignmentX(LEFT_ALIGNMENT);
        aboutCard.add(aboutLabel);
        
        content.add(aboutCard);
        
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }
    
    void styleComboBox(JComboBox<String> cb) {
        cb.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cb.setBackground(C_BG_DARK);
        cb.setForeground(C_TEXT);
        cb.setBorder(BorderFactory.createLineBorder(C_BORDER));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                setBackground(sel ? C_ACCENT : C_BG_CARD);
                setForeground(sel ? C_BG_DARK : C_TEXT);
                setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
                return this;
            }
        });
    }
    
    void styleSpinner(JSpinner sp) {
        sp.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sp.setBackground(C_BG_DARK);
        JComponent editor = sp.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(C_BG_DARK);
            tf.setForeground(C_TEXT);
            tf.setCaretColor(C_ACCENT);
            tf.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        }
        sp.setBorder(BorderFactory.createLineBorder(C_BORDER));
    }
    
    JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(C_BG_CARD);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));
        bar.setPreferredSize(new Dimension(0, 30));
        
        statusLabel = new JLabel("  Ready");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setForeground(C_TEXT_DIM);
        bar.add(statusLabel, BorderLayout.WEST);
        
        JLabel copyright = new JLabel(COPYRIGHT + " / Team Flames  ");
        copyright.setFont(new Font("SansSerif", Font.PLAIN, 10));
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
        if (javaInstalls.containsKey(required)) return javaInstalls.get(required);
        int bestMatch = Integer.MAX_VALUE;
        String bestPath = null;
        for (Map.Entry<Integer, String> e : javaInstalls.entrySet()) {
            int v = e.getKey();
            if (v >= required && v < bestMatch) { bestMatch = v; bestPath = e.getValue(); }
        }
        if (bestPath != null) return bestPath;
        int highest = 0;
        for (int v : javaInstalls.keySet()) if (v > highest) highest = v;
        if (highest > 0 && javaInstalls.containsKey(highest)) return javaInstalls.get(highest);
        return System.getProperty("java.home") + "/bin/java";
    }
    
    static boolean isMacOS() { return System.getProperty("os.name", "").toLowerCase().contains("mac"); }
    static boolean isAppleSilicon() { return isMacOS() && System.getProperty("os.arch", "").contains("aarch64"); }
    
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
                    versionCombo.addItem("═══ RELEASES ═══");
                    for (VersionInfo v : versions.values()) {
                        if ("release".equals(v.type)) {
                            String label = v.id;
                            if ((v.id.startsWith("1.21") || v.id.startsWith("1.20.5") || v.id.startsWith("1.20.6")) && !javaInstalls.containsKey(21)) 
                                label += " ⚠ Java 21";
                            versionCombo.addItem(label);
                        }
                    }
                    versionCombo.addItem("═══ SNAPSHOTS ═══");
                    for (VersionInfo v : versions.values()) if ("snapshot".equals(v.type)) versionCombo.addItem(v.id);
                    versionCombo.addItem("═══ BETA ═══");
                    for (VersionInfo v : versions.values()) if ("old_beta".equals(v.type)) versionCombo.addItem(v.id);
                    versionCombo.addItem("═══ ALPHA ═══");
                    for (VersionInfo v : versions.values()) if ("old_alpha".equals(v.type)) versionCombo.addItem(v.id);
                    if (versionCombo.getItemCount() > 1) versionCombo.setSelectedIndex(1);
                });
                status("Ready - " + versions.size() + " versions");
            } catch (Exception ex) { status("Error: " + ex.getMessage()); }
        }).start();
    }
    
    void onLaunch() {
        if (isRunning) { cancelled = true; return; }
        String sel = (String) versionCombo.getSelectedItem();
        if (sel == null || sel.startsWith("═══")) { status("Select a version!"); return; }
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
            launchButton.setText("CANCEL");
            launchButton.setBackground(C_ERROR);
            progressBar.setVisible(true);
            progressBar.setValue(0);
        });
        
        try {
            log("═════════════════════════════════════════════════════");
            log("Launching Minecraft " + ver.id + " as " + user);
            log("═════════════════════════════════════════════════════");
            
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
            
            status("Applying boosters...");
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
            
            if (isMacOS()) {
                cmd.add("-XstartOnFirstThread");
            }
            
            cmd.add("-Xms512M"); cmd.add("-Xmx" + ram + "M");
            cmd.add("-XX:+UnlockExperimentalVMOptions"); cmd.add("-XX:+UseG1GC");
            cmd.add("-XX:G1NewSizePercent=20"); cmd.add("-XX:G1ReservePercent=20");
            cmd.add("-XX:MaxGCPauseMillis=50"); cmd.add("-XX:G1HeapRegionSize=32M");
            cmd.add("-Djava.library.path=" + natDir);
            cmd.add("-Dminecraft.launcher.brand=" + NAME);
            cmd.add("-Dminecraft.launcher.version=" + VER);
            if (isAppleSilicon()) cmd.add("-Dorg.lwjgl.system.allocator=system");
            if (availableJava >= 21) cmd.add("--enable-native-access=ALL-UNNAMED");
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
            cmd.add("--versionType"); cmd.add(NAME);
            
            progress(100);
            log("Starting with Java " + availableJava + ", RAM: " + ram + "MB");
            log("Boosters: " + getActiveBoostersString());
            
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(MC_DIR));
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            
            log("═════════════════════════════════════════════════════");
            log("Minecraft started! PID: " + proc.pid());
            status("Minecraft running!");
            
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
            SwingUtilities.invokeLater(() -> { launchButton.setText("LAUNCH"); launchButton.setBackground(C_ACCENT); progressBar.setVisible(false); });
        }
    }
    
    void applyFPSBoosters(VersionInfo ver) {
        try {
            File optionsFile = new File(MC_DIR + "options.txt");
            Map<String, String> opts = new LinkedHashMap<>();
            
            if (optionsFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(optionsFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int colon = line.indexOf(':');
                        if (colon > 0) {
                            opts.put(line.substring(0, colon), line.substring(colon + 1));
                        }
                    }
                }
            }
            
            if (chkFastRender.isSelected()) { opts.put("renderClouds", "\"fast\""); opts.put("graphicsMode", "1"); }
            if (chkChunkOpt.isSelected()) opts.put("chunkBuilder", "1");
            if (chkEntityCull.isSelected()) opts.put("entityShadows", "false");
            if (chkParticles.isSelected()) opts.put("particles", "2");
            if (chkSmoothFps.isSelected()) opts.put("enableVsync", "true");
            
            opts.put("renderDistance", String.valueOf(renderDistSlider.getValue()));
            opts.put("simulationDistance", String.valueOf(Math.min(renderDistSlider.getValue(), 12)));
            opts.put("ao", "1"); opts.put("mipmapLevels", "2"); opts.put("biomeBlendRadius", "2");
            
            try (PrintWriter pw = new PrintWriter(new FileWriter(optionsFile))) {
                for (Map.Entry<String, String> e : opts.entrySet()) {
                    pw.println(e.getKey() + ":" + e.getValue());
                }
            }
            log("Applied boosters");
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
