import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;

public class ColorConverter extends JFrame {

    // панель для отображения цвета
    private JPanel colorDisplayPanel;

    // элементы для модели RGB
    private JSlider rSlider, gSlider, bSlider;
    private JTextField rField, gField, bField;

    // элементы для модели HSV
    private JSlider hSlider, sSlider, vSlider;
    private JTextField hField, sField, vField;

    // элементы для модели CMYK
    private JSlider cSlider, mSlider, ySlider, kSlider;
    private JTextField cField, mField, yField, kField;

    // отслеживание при изменении значений программно
    private boolean isUpdating = false;

    public ColorConverter() {
        super("Конвертер Цветов");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // главная панель
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // панель отображения цвета
        colorDisplayPanel = new JPanel();
        colorDisplayPanel.setPreferredSize(new Dimension(100, 100));
        colorDisplayPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        mainPanel.add(colorDisplayPanel, BorderLayout.NORTH);

        // панель с элементами управления
        JPanel controlsPanel = new JPanel(new GridLayout(1, 3, 10, 10));

        // панели для каждой цветовой модели
        controlsPanel.add(createRgbPanel());
        controlsPanel.add(createHsvPanel());
        controlsPanel.add(createCmykPanel());
        mainPanel.add(controlsPanel, BorderLayout.CENTER);

        // кнопка выбора цвета из палитры
        JButton chooseColorButton = new JButton("Выбрать цвет из палитры");
        chooseColorButton.addActionListener(e -> {
            final JColorChooser colorChooser = new JColorChooser(colorDisplayPanel.getBackground());
            javax.swing.colorchooser.AbstractColorChooserPanel[] defaultPanels = colorChooser.getChooserPanels();
            javax.swing.colorchooser.AbstractColorChooserPanel[] simplePanels = { defaultPanels[0] };
            colorChooser.setChooserPanels(simplePanels);
            JDialog dialog = JColorChooser.createDialog(
                    this,
                    "Выберите цвет",
                    true,
                    colorChooser,
                    okEvent -> {
                        Color chosenColor = colorChooser.getColor();
                        if (chosenColor != null) {
                            isUpdating = true;
                            updateColorFromRgb(chosenColor.getRed(), chosenColor.getGreen(), chosenColor.getBlue());
                            isUpdating = false;
                        }
                    },
                    null);
            dialog.setVisible(true);
        });
        mainPanel.add(chooseColorButton, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        
        // установка начального цвета (черный)
        updateColorFromRgb(0, 0, 0);
    }

    // создание панели для RGB
    private JPanel createRgbPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new TitledBorder("RGB (0-255)"));

        rSlider = new JSlider(0, 255);
        rField = new JTextField(3);
        panel.add(createColorComponentPanel("R:", rSlider, rField));

        gSlider = new JSlider(0, 255);
        gField = new JTextField(3);
        panel.add(createColorComponentPanel("G:", gSlider, gField));

        bSlider = new JSlider(0, 255);
        bField = new JTextField(3);
        panel.add(createColorComponentPanel("B:", bSlider, bField));

        addRgbListeners();
        return panel;
    }

    // создание панели для HSV
    private JPanel createHsvPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new TitledBorder("HSV (H: 0-360, S/V: 0-100)"));

        hSlider = new JSlider(0, 360);
        hField = new JTextField(3);
        panel.add(createColorComponentPanel("H:", hSlider, hField));

        sSlider = new JSlider(0, 100);
        sField = new JTextField(3);
        panel.add(createColorComponentPanel("S:", sSlider, sField));

        vSlider = new JSlider(0, 100);
        vField = new JTextField(3);
        panel.add(createColorComponentPanel("V:", vSlider, vField));
        
        addHsvListeners();
        return panel;
    }

    // создание панели для CMYK
    private JPanel createCmykPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new TitledBorder("CMYK (0-100)"));

        cSlider = new JSlider(0, 100);
        cField = new JTextField(3);
        panel.add(createColorComponentPanel("C:", cSlider, cField));

        mSlider = new JSlider(0, 100);
        mField = new JTextField(3);
        panel.add(createColorComponentPanel("M:", mSlider, mField));

        ySlider = new JSlider(0, 100);
        yField = new JTextField(3);
        panel.add(createColorComponentPanel("Y:", ySlider, yField));
        
        kSlider = new JSlider(0, 100);
        kField = new JTextField(3);
        panel.add(createColorComponentPanel("K:", kSlider, kField));

        addCmykListeners();
        return panel;
    }
    
    // метод для создания строки с компонентом цвета (метка, ползунок, поле)
    private JPanel createColorComponentPanel(String label, JSlider slider, JTextField field) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel(label));
        panel.add(slider);
        panel.add(field);
        return panel;
    }

    // добавление слушателей для RGB
    private void addRgbListeners() {
        ChangeListener listener = e -> {
            if (!isUpdating) {
                updateColorFromRgb(rSlider.getValue(), gSlider.getValue(), bSlider.getValue());
            }
        };
        rSlider.addChangeListener(listener);
        gSlider.addChangeListener(listener);
        bSlider.addChangeListener(listener);

        ActionListener actionListener = e -> {
             if (!isUpdating) {
                try {
                    int r = Integer.parseInt(rField.getText());
                    int g = Integer.parseInt(gField.getText());
                    int b = Integer.parseInt(bField.getText());
                    updateColorFromRgb(r, g, b);
                } catch (NumberFormatException ex) {
                    // ошибка, если введено не число
                }
            }
        };
        rField.addActionListener(actionListener);
        gField.addActionListener(actionListener);
        bField.addActionListener(actionListener);
    }
    
    // добавление слушателей для HSV
    private void addHsvListeners() {
        ChangeListener listener = e -> {
            if (!isUpdating && ((JSlider) e.getSource()).getValueIsAdjusting()) {
                updateColorFromHsv();
            }
        };
        hSlider.addChangeListener(listener);
        sSlider.addChangeListener(listener);
        vSlider.addChangeListener(listener);

        ActionListener actionListener = e -> {
            if (!isUpdating) {
                try {
                    int h = Integer.parseInt(hField.getText());
                    int s = Integer.parseInt(sField.getText());
                    int v = Integer.parseInt(vField.getText());
                    // метод обновления с параметрами
                    updateColorFromHsv(h, s, v);
                } catch (NumberFormatException ex) {
                    // ошибка, если введено не число
                }
            }
        };
        hField.addActionListener(actionListener);
        sField.addActionListener(actionListener);
        vField.addActionListener(actionListener);
    }

    // добавление слушателей для CMYK
    private void addCmykListeners() {
        ChangeListener listener = e -> {
            if (!isUpdating && ((JSlider) e.getSource()).getValueIsAdjusting()) {
                updateColorFromCmyk();
            }
        };
        cSlider.addChangeListener(listener);
        mSlider.addChangeListener(listener);
        ySlider.addChangeListener(listener);
        kSlider.addChangeListener(listener);

        ActionListener actionListener = e -> {
            if (!isUpdating) {
                try {
                    int c = Integer.parseInt(cField.getText());
                    int m = Integer.parseInt(mField.getText());
                    int y = Integer.parseInt(yField.getText());
                    int k = Integer.parseInt(kField.getText());
                    // Вызываем новый метод обновления с параметрами
                    updateColorFromCmyk(c, m, y, k);
                } catch (NumberFormatException ex) {
                    // ошибка, если введено не число
                }
            }
        };
        cField.addActionListener(actionListener);
        mField.addActionListener(actionListener);
        yField.addActionListener(actionListener);
        kField.addActionListener(actionListener);
    }

    // обновление значений
    private void updateColorFromRgb(int r, int g, int b) {
        // ограничения на значения в пределах 0-255
        r = Math.min(255, Math.max(0, r));
        g = Math.min(255, Math.max(0, g));
        b = Math.min(255, Math.max(0, b));
        
        // RGB -> HSV
        float[] hsv = new float[3];
        Color.RGBtoHSB(r, g, b, hsv);

        // RGB -> CMYK
        float[] cmyk = rgbToCmyk(r, g, b);

        updateUIComponents(r, g, b, hsv, cmyk);
    }
    
    private void updateColorFromHsv(int h, int s, int v) {
        h = Math.min(360, Math.max(0, h));
        s = Math.min(100, Math.max(0, s));
        v = Math.min(100, Math.max(0, v));

        float hue = h / 360f;
        float saturation = s / 100f;
        float value = v / 100f;

        int rgbInt = Color.HSBtoRGB(hue, saturation, value);
        Color color = new Color(rgbInt);

        updateColorFromRgb(color.getRed(), color.getGreen(), color.getBlue());
    }

    private void updateColorFromHsv() {
        updateColorFromHsv(hSlider.getValue(), sSlider.getValue(), vSlider.getValue());
    }

    private void updateColorFromCmyk(int c, int m, int y, int k) {
    // Ограничиваем значения
    c = Math.min(100, Math.max(0, c));
    m = Math.min(100, Math.max(0, m));
    y = Math.min(100, Math.max(0, y));
    k = Math.min(100, Math.max(0, k));

    float cyan = c / 100f;
    float magenta = m / 100f;
    float yellow = y / 100f;
    float black = k / 100f;

    int[] rgb = cmykToRgb(cyan, magenta, yellow, black);
    updateColorFromRgb(rgb[0], rgb[1], rgb[2]);
}

    private void updateColorFromCmyk() {
        updateColorFromCmyk(cSlider.getValue(), mSlider.getValue(), ySlider.getValue(), kSlider.getValue());
    }
    
    // обновление элементов интерфейса
    private void updateUIComponents(int r, int g, int b, float[] hsv, float[] cmyk) {
        isUpdating = true;

        // цвет панели
        colorDisplayPanel.setBackground(new Color(r, g, b));

        // RGB
        rSlider.setValue(r); rField.setText(String.valueOf(r));
        gSlider.setValue(g); gField.setText(String.valueOf(g));
        bSlider.setValue(b); bField.setText(String.valueOf(b));

        // HSV
        int hue = Math.round(hsv[0] * 360);
        int sat = Math.round(hsv[1] * 100);
        int val = Math.round(hsv[2] * 100);
        hSlider.setValue(hue); hField.setText(String.valueOf(hue));
        sSlider.setValue(sat); sField.setText(String.valueOf(sat));
        vSlider.setValue(val); vField.setText(String.valueOf(val));
        
        // CMYK
        int cyan = Math.round(cmyk[0] * 100);
        int magenta = Math.round(cmyk[1] * 100);
        int yellow = Math.round(cmyk[2] * 100);
        int black = Math.round(cmyk[3] * 100);
        cSlider.setValue(cyan); cField.setText(String.valueOf(cyan));
        mSlider.setValue(magenta); mField.setText(String.valueOf(magenta));
        ySlider.setValue(yellow); yField.setText(String.valueOf(yellow));
        kSlider.setValue(black); kField.setText(String.valueOf(black));

        isUpdating = false;
    }

    // методы перевода
    private float[] rgbToCmyk(int r, int g, int b) {
        if (r == 0 && g == 0 && b == 0) {
            return new float[]{0, 0, 0, 1};
        }
        float r_prime = r / 255f;
        float g_prime = g / 255f;
        float b_prime = b / 255f;
        float k = 1 - Math.max(r_prime, Math.max(g_prime, b_prime));
        float c = (1 - r_prime - k) / (1 - k);
        float m = (1 - g_prime - k) / (1 - k);
        float y = (1 - b_prime - k) / (1 - k);
        return new float[]{c, m, y, k};
    }

    private int[] cmykToRgb(float c, float m, float y, float k) {
        int r = Math.round(255 * (1 - c) * (1 - k));
        int g = Math.round(255 * (1 - m) * (1 - k));
        int b = Math.round(255 * (1 - y) * (1 - k));
        return new int[]{r, g, b};
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ColorConverter().setVisible(true);
        });
    }
}