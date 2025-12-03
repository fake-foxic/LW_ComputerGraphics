import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeListener;

public class ColorConverterApp extends JFrame {

    // Источники изменений
    private enum ChangeSource {
        RGB, CMYK, HSV, EXTERNAL
    }

    private boolean isUpdating = false;

    private JPanel colorPreviewPanel;
    private JButton paletteButton;

    // RGB
    private JSlider sR, sG, sB;
    private JTextField tR, tG, tB;

    // CMYK
    private JSlider sC, sM, sY, sK;
    private JTextField tC, tM, tY, tK;

    // HSV
    private JSlider sH, sS, sV;
    private JTextField tH, tS, tV;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) { /* ignore */ }
            new ColorConverterApp().setVisible(true);
        });
    }

    public ColorConverterApp() {
        setTitle("Конвертер Цветов");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Верхняя
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 15));
        
        colorPreviewPanel = new JPanel();
        colorPreviewPanel.setPreferredSize(new Dimension(120, 120));
        colorPreviewPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        colorPreviewPanel.setBackground(Color.WHITE);

        paletteButton = new JButton("Выбрать из палитры...");
        paletteButton.setPreferredSize(new Dimension(180, 40));

        topPanel.add(new JLabel("Предпросмотр:"));
        topPanel.add(colorPreviewPanel);
        topPanel.add(paletteButton);

        add(topPanel, BorderLayout.NORTH);

        // Центральная часть
        JPanel mainPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        mainPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

        initComponents();

        mainPanel.add(createModelPanel("RGB Model", 
                new String[]{"Red (0-255)", "Green (0-255)", "Blue (0-255)"}, 
                new JSlider[]{sR, sG, sB}, 
                new JTextField[]{tR, tG, tB}));

        mainPanel.add(createModelPanel("CMYK Model", 
                new String[]{"Cyan (0-100)", "Magenta (0-100)", "Yellow (0-100)", "Key (0-100)"}, 
                new JSlider[]{sC, sM, sY, sK}, 
                new JTextField[]{tC, tM, tY, tK}));

        mainPanel.add(createModelPanel("HSV Model", 
                new String[]{"Hue (0-360)", "Sat (0-100)", "Val (0-100)"}, 
                new JSlider[]{sH, sS, sV}, 
                new JTextField[]{tH, tS, tV}));

        add(mainPanel, BorderLayout.CENTER);

        // Отслеживание
        setupListeners();

        // Начальный цвет
        updateAllFromColor(new Color(204, 163, 82), ChangeSource.EXTERNAL); 
    }

    private void initComponents() {
        // RGB
        sR = createSlider(0, 255); sG = createSlider(0, 255); sB = createSlider(0, 255);
        tR = createTextField(); tG = createTextField(); tB = createTextField();

        // CMYK
        sC = createSlider(0, 100); sM = createSlider(0, 100); sY = createSlider(0, 100); sK = createSlider(0, 100);
        tC = createTextField(); tM = createTextField(); tY = createTextField(); tK = createTextField();

        // HSV
        sH = createSlider(0, 360); sS = createSlider(0, 100); sV = createSlider(0, 100);
        tH = createTextField(); tS = createTextField(); tV = createTextField();
    }

    private JSlider createSlider(int min, int max) {
        JSlider s = new JSlider(min, max);
        s.setValue(min);
        return s;
    }

    private JTextField createTextField() {
        JTextField tf = new JTextField("0");
        tf.setColumns(4);
        tf.setHorizontalAlignment(JTextField.CENTER);
        Dimension dim = new Dimension(60, 25);
        tf.setPreferredSize(dim);
        tf.setMinimumSize(dim); 
        tf.setMaximumSize(dim);
        return tf;
    }

    private JPanel createModelPanel(String title, String[] labels, JSlider[] sliders, JTextField[] fields) {
        JPanel panel = new JPanel();
        panel.setBorder(new CompoundBorder(
                new TitledBorder(null, title, TitledBorder.CENTER, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14)),
                new EmptyBorder(10, 10, 10, 10)
        ));
        
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0; 
            panel.add(new JLabel(labels[i]), gbc);

            gbc.gridx = 1; gbc.weightx = 1.0; 
            panel.add(sliders[i], gbc);

            gbc.gridx = 2; gbc.weightx = 0; 
            panel.add(fields[i], gbc);
        }
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.NORTH);
        return wrapper;
    }

    private void setupListeners() {
        paletteButton.addActionListener(e -> {
            JColorChooser chooser = new JColorChooser(colorPreviewPanel.getBackground());
            AbstractColorChooserPanel[] panels = chooser.getChooserPanels();
            for (AbstractColorChooserPanel p : panels) {
                String name = p.getDisplayName();
                if (!name.equals("Swatches") && !name.equals("Палитра")) {
                   chooser.removeChooserPanel(p);
                }
            }
            JDialog dialog = JColorChooser.createDialog(this, "Выберите цвет", true, chooser, 
                ok -> updateAllFromColor(chooser.getColor(), ChangeSource.EXTERNAL), null);
            dialog.setVisible(true);
        });

        ChangeListener rgbChange = e -> updateFromRGBInputs();
        sR.addChangeListener(rgbChange); sG.addChangeListener(rgbChange); sB.addChangeListener(rgbChange);
        addTextAction(tR, this::validateAndUpdateRGB);
        addTextAction(tG, this::validateAndUpdateRGB);
        addTextAction(tB, this::validateAndUpdateRGB);

        ChangeListener cmykChange = e -> updateFromCMYKInputs();
        sC.addChangeListener(cmykChange); sM.addChangeListener(cmykChange); sY.addChangeListener(cmykChange); sK.addChangeListener(cmykChange);
        addTextAction(tC, this::validateAndUpdateCMYK);
        addTextAction(tM, this::validateAndUpdateCMYK);
        addTextAction(tY, this::validateAndUpdateCMYK);
        addTextAction(tK, this::validateAndUpdateCMYK);

        ChangeListener hsvChange = e -> updateFromHSVInputs();
        sH.addChangeListener(hsvChange); sS.addChangeListener(hsvChange); sV.addChangeListener(hsvChange);
        addTextAction(tH, this::validateAndUpdateHSV);
        addTextAction(tS, this::validateAndUpdateHSV);
        addTextAction(tV, this::validateAndUpdateHSV);
    }

    private void addTextAction(JTextField tf, Runnable action) {
        tf.addActionListener(e -> action.run());
        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                action.run();
            }
        });
    }

    // Методы обработки ввода
    private void updateFromRGBInputs() {
        if (isUpdating) return;
        
        // Синхронизизация полей модели RGB
        tR.setText(String.valueOf(sR.getValue()));
        tG.setText(String.valueOf(sG.getValue()));
        tB.setText(String.valueOf(sB.getValue()));

        // Обновление остальных моделей
        updateAllFromColor(new Color(sR.getValue(), sG.getValue(), sB.getValue()), ChangeSource.RGB);
    }

    private void validateAndUpdateRGB() {
        if (isUpdating) return;
        try {
            int r = clamp(Integer.parseInt(tR.getText()), 0, 255);
            int g = clamp(Integer.parseInt(tG.getText()), 0, 255);
            int b = clamp(Integer.parseInt(tB.getText()), 0, 255);
            sR.setValue(r); sG.setValue(g); sB.setValue(b);
        } catch (Exception e) { 
            tR.setText(String.valueOf(sR.getValue()));
        }
    }

    private void updateFromCMYKInputs() {
        if (isUpdating) return;

        // Синхронизизация полей модели CMYK
        tC.setText(String.valueOf(sC.getValue()));
        tM.setText(String.valueOf(sM.getValue()));
        tY.setText(String.valueOf(sY.getValue()));
        tK.setText(String.valueOf(sK.getValue()));

        double c = sC.getValue() / 100.0;
        double m = sM.getValue() / 100.0;
        double y = sY.getValue() / 100.0;
        double k = sK.getValue() / 100.0;

        int r = (int) Math.round((1 - c) * (1 - k) * 255);
        int g = (int) Math.round((1 - m) * (1 - k) * 255);
        int b = (int) Math.round((1 - y) * (1 - k) * 255);
        
        // Обновление остальных моделей
        updateAllFromColor(new Color(clamp(r,0,255), clamp(g,0,255), clamp(b,0,255)), ChangeSource.CMYK);
    }

    private void validateAndUpdateCMYK() {
        if (isUpdating) return;
        try {
            int c = clamp(Integer.parseInt(tC.getText()), 0, 100);
            int m = clamp(Integer.parseInt(tM.getText()), 0, 100);
            int y = clamp(Integer.parseInt(tY.getText()), 0, 100);
            int k = clamp(Integer.parseInt(tK.getText()), 0, 100);
            sC.setValue(c); sM.setValue(m); sY.setValue(y); sK.setValue(k);
        } catch (Exception e) {
             tC.setText(String.valueOf(sC.getValue()));
        }
    }

    private void updateFromHSVInputs() {
        if (isUpdating) return;
        
        // Синхронизация полей модели HSV
        tH.setText(String.valueOf(sH.getValue()));
        tS.setText(String.valueOf(sS.getValue()));
        tV.setText(String.valueOf(sV.getValue()));

        float h = sH.getValue() / 360.0f;
        float s = sS.getValue() / 100.0f;
        float v = sV.getValue() / 100.0f;
        
        int rgb = Color.HSBtoRGB(h, s, v);
        // Обновление остальных модели
        updateAllFromColor(new Color(rgb), ChangeSource.HSV);
    }

    private void validateAndUpdateHSV() {
        if (isUpdating) return;
        try {
            int h = clamp(Integer.parseInt(tH.getText()), 0, 360);
            int s = clamp(Integer.parseInt(tS.getText()), 0, 100);
            int v = clamp(Integer.parseInt(tV.getText()), 0, 100);
            sH.setValue(h); sS.setValue(s); sV.setValue(v);
        } catch (Exception e) {
            tH.setText(String.valueOf(sH.getValue()));
        }
    }

    // Главный метод пересчета
    private void updateAllFromColor(Color c, ChangeSource source) {
        isUpdating = true; 
        try {
            colorPreviewPanel.setBackground(c);

            // Обновление RGB
            if (source != ChangeSource.RGB) {
                sR.setValue(c.getRed());   tR.setText(String.valueOf(c.getRed()));
                sG.setValue(c.getGreen()); tG.setText(String.valueOf(c.getGreen()));
                sB.setValue(c.getBlue());  tB.setText(String.valueOf(c.getBlue()));
            }

            // Обновление HSV
            if (source != ChangeSource.HSV) {
                float[] hsv = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
                int h = Math.round(hsv[0] * 360);
                int s = Math.round(hsv[1] * 100);
                int v = Math.round(hsv[2] * 100);
                sH.setValue(h); tH.setText(String.valueOf(h));
                sS.setValue(s); tS.setText(String.valueOf(s));
                sV.setValue(v); tV.setText(String.valueOf(v));
            }

            // Обновление CMYK
            if (source != ChangeSource.CMYK) {
                double rNorm = c.getRed() / 255.0;
                double gNorm = c.getGreen() / 255.0;
                double bNorm = c.getBlue() / 255.0;
                
                double kVal = 1.0 - Math.max(rNorm, Math.max(gNorm, bNorm));
                double cVal = 0, mVal = 0, yVal = 0;
                
                if (kVal < 1.0) {
                    cVal = (1.0 - rNorm - kVal) / (1.0 - kVal);
                    mVal = (1.0 - gNorm - kVal) / (1.0 - kVal);
                    yVal = (1.0 - bNorm - kVal) / (1.0 - kVal);
                }

                int cInt = (int) Math.round(cVal * 100);
                int mInt = (int) Math.round(mVal * 100);
                int yInt = (int) Math.round(yVal * 100);
                int kInt = (int) Math.round(kVal * 100);

                sC.setValue(cInt); tC.setText(String.valueOf(cInt));
                sM.setValue(mInt); tM.setText(String.valueOf(mInt));
                sY.setValue(yInt); tY.setText(String.valueOf(yInt));
                sK.setValue(kInt); tK.setText(String.valueOf(kInt));
            }

        } finally {
            isUpdating = false;
        }
    }

    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}