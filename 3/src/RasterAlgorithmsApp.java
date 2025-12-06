import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class RasterAlgorithmsApp extends JFrame {

    private GridPanel gridPanel;
    private JTextArea logArea;
    private JTextField tfX1, tfY1, tfX2, tfY2, tfRadius;
    private JRadioButton rbStep, rbDDA, rbBresenhamLine, rbBresenhamCircle, rbCastlePitteway;
    private JLabel timeLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) { /* ignore */ }
            new RasterAlgorithmsApp().setVisible(true);
        });
    }

    public RasterAlgorithmsApp() {
        setTitle("Базовые растровые алгоритмы + Сглаживание");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 850);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Левая панель управления
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        controlPanel.setPreferredSize(new Dimension(280, 0));

        // Ввод координат
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        inputPanel.setBorder(new TitledBorder("Координаты"));
        tfX1 = new JTextField("0"); tfY1 = new JTextField("0");
        tfX2 = new JTextField("8"); tfY2 = new JTextField("4");
        tfRadius = new JTextField("10");

        inputPanel.add(new JLabel("X1:")); inputPanel.add(tfX1);
        inputPanel.add(new JLabel("Y1:")); inputPanel.add(tfY1);
        inputPanel.add(new JLabel("X2:")); inputPanel.add(tfX2);
        inputPanel.add(new JLabel("Y2:")); inputPanel.add(tfY2);
        inputPanel.add(new JLabel("Радиус R:")); inputPanel.add(tfRadius);
        
        controlPanel.add(inputPanel);
        controlPanel.add(Box.createVerticalStrut(10));

        // Выбор алгоритма
        JPanel radioPanel = new JPanel(new GridLayout(5, 1)); // Увеличили кол-во строк
        radioPanel.setBorder(new TitledBorder("Алгоритм"));
        ButtonGroup bg = new ButtonGroup();
        
        rbStep = new JRadioButton("Пошаговый");
        rbDDA = new JRadioButton("ЦДА (DDA)");
        rbBresenhamLine = new JRadioButton("Брезенхем (Линия)");
        rbCastlePitteway = new JRadioButton("Кастла-Питвея (Сглаживание)");
        rbBresenhamCircle = new JRadioButton("Брезенхем (Окружность)");
        
        bg.add(rbStep); bg.add(rbDDA); bg.add(rbBresenhamLine); 
        bg.add(rbCastlePitteway); bg.add(rbBresenhamCircle);
        
        rbBresenhamLine.setSelected(true);
        
        radioPanel.add(rbStep); 
        radioPanel.add(rbDDA);
        radioPanel.add(rbBresenhamLine); 
        radioPanel.add(rbCastlePitteway);
        radioPanel.add(rbBresenhamCircle);
        
        controlPanel.add(radioPanel);
        controlPanel.add(Box.createVerticalStrut(10));

        // Кнопки и инфо
        JButton btnDraw = new JButton("Построить");
        JButton btnClear = new JButton("Очистить");
        timeLabel = new JLabel("Время: 0 нс");
        
        controlPanel.add(btnDraw);
        controlPanel.add(Box.createVerticalStrut(5));
        controlPanel.add(btnClear);
        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(timeLabel);
        controlPanel.add(Box.createVerticalStrut(10));

        // Лог вычислений
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollLog = new JScrollPane(logArea);
        scrollLog.setBorder(new TitledBorder("Точки (Лог)"));
        controlPanel.add(scrollLog);
        
        // Пояснение про сглаживание
        JTextArea infoArea = new JTextArea("Алгоритм Кастла-Питвея использует целочисленную арифметику для расчета интенсивности двух соседних пикселей, создавая эффект сглаживания (Antialiasing).");
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setEditable(false);
        infoArea.setBackground(controlPanel.getBackground());
        infoArea.setFont(new Font("SansSerif", Font.ITALIC, 11));
        infoArea.setBorder(new EmptyBorder(5, 0, 0, 0));
        controlPanel.add(infoArea);

        add(controlPanel, BorderLayout.WEST);

        // Сетка
        gridPanel = new GridPanel();
        add(gridPanel, BorderLayout.CENTER);

        // Слушатели
        btnDraw.addActionListener(e -> draw());
        btnClear.addActionListener(e -> {
            gridPanel.clearPoints();
            logArea.setText("");
            timeLabel.setText("Время: -");
        });

        rbBresenhamCircle.addActionListener(e -> toggleInputs(false));
        rbStep.addActionListener(e -> toggleInputs(true));
        rbDDA.addActionListener(e -> toggleInputs(true));
        rbBresenhamLine.addActionListener(e -> toggleInputs(true));
        rbCastlePitteway.addActionListener(e -> toggleInputs(true));
    }

    private void toggleInputs(boolean isLine) {
        tfX2.setEnabled(isLine);
        tfY2.setEnabled(isLine);
        tfRadius.setEnabled(!isLine);
    }

    private void draw() {
        try {
            int x1 = Integer.parseInt(tfX1.getText());
            int y1 = Integer.parseInt(tfY1.getText());
            List<Pixel> pixels = new ArrayList<>();
            long startTime = System.nanoTime();

            if (rbBresenhamCircle.isSelected()) {
                int r = Integer.parseInt(tfRadius.getText());
                pixels = RasterAlgorithms.bresenhamCircle(0, 0, r);
                // Сдвиг координат
                List<Pixel> shifted = new ArrayList<>();
                for(Pixel p : pixels) shifted.add(new Pixel(p.x + x1, p.y + y1, p.opacity));
                pixels = shifted;
            } else {
                int x2 = Integer.parseInt(tfX2.getText());
                int y2 = Integer.parseInt(tfY2.getText());

                if (rbStep.isSelected()) pixels = RasterAlgorithms.stepByStep(x1, y1, x2, y2);
                else if (rbDDA.isSelected()) pixels = RasterAlgorithms.dda(x1, y1, x2, y2);
                else if (rbBresenhamLine.isSelected()) pixels = RasterAlgorithms.bresenhamLine(x1, y1, x2, y2);
                else if (rbCastlePitteway.isSelected()) pixels = RasterAlgorithms.castlePitteway(x1, y1, x2, y2);
            }

            long endTime = System.nanoTime();
            timeLabel.setText("Время: " + (endTime - startTime) + " нс");

            // Вывод в лог
            StringBuilder sb = new StringBuilder();
            sb.append("Найдено ").append(pixels.size()).append(" точек:\n");
            for (Pixel p : pixels) {
                // Если прозрачность < 1.0, выводим в лог
                if (p.opacity < 1.0f) {
                    sb.append(String.format("[%d; %d] op=%.2f\n", p.x, p.y, p.opacity));
                } else {
                    sb.append(String.format("[%d; %d]\n", p.x, p.y));
                }
            }
            logArea.setText(sb.toString());
            logArea.setCaretPosition(0);

            gridPanel.setPixels(pixels);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Введите корректные целые числа!");
        }
    }
}

// Вспомогательный класс
class Pixel {
    int x, y;
    float opacity; // 0.0 - прозрачный, 1.0 - полностью закрашен

    public Pixel(int x, int y) {
        this(x, y, 1.0f);
    }
    
    public Pixel(int x, int y, float opacity) {
        this.x = x;
        this.y = y;
        this.opacity = opacity;
    }
}

// Алгоритмы
class RasterAlgorithms {

    // 1. Пошаговый
    public static List<Pixel> stepByStep(int x1, int y1, int x2, int y2) {
        List<Pixel> pixels = new ArrayList<>();
        if (x1 == x2 && y1 == y2) {
            pixels.add(new Pixel(x1, y1));
            return pixels;
        }
        int dx = x2 - x1;
        int dy = y2 - y1;
        
        if (Math.abs(dy) > Math.abs(dx)) {
            float k = (float) dx / dy;
            int stepY = (y2 > y1) ? 1 : -1;
            for (int y = y1; y != y2 + stepY; y += stepY) {
                int x = Math.round(x1 + (y - y1) * k);
                pixels.add(new Pixel(x, y));
            }
        } else {
            float k = (float) dy / dx;
            int stepX = (x2 > x1) ? 1 : -1;
            for (int x = x1; x != x2 + stepX; x += stepX) {
                int y = Math.round(y1 + (x - x1) * k);
                pixels.add(new Pixel(x, y));
            }
        }
        return pixels;
    }

    // 2. ЦДА
    public static List<Pixel> dda(int x1, int y1, int x2, int y2) {
        List<Pixel> pixels = new ArrayList<>();
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        
        float xInc = (float) dx / steps;
        float yInc = (float) dy / steps;
        float x = x1;
        float y = y1;

        for (int i = 0; i <= steps; i++) {
            pixels.add(new Pixel(Math.round(x), Math.round(y)));
            x += xInc;
            y += yInc;
        }
        return pixels;
    }

    // 3. Брезенхем (Линия)
    public static List<Pixel> bresenhamLine(int x1, int y1, int x2, int y2) {
        List<Pixel> pixels = new ArrayList<>();
        int w = x2 - x1;
        int h = y2 - y1;
        int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0;
        
        if (w < 0) dx1 = -1; else if (w > 0) dx1 = 1;
        if (h < 0) dy1 = -1; else if (h > 0) dy1 = 1;
        if (w < 0) dx2 = -1; else if (w > 0) dx2 = 1;
        
        int longest = Math.abs(w);
        int shortest = Math.abs(h);
        
        if (!(longest > shortest)) {
            longest = Math.abs(h);
            shortest = Math.abs(w);
            if (h < 0) dy2 = -1; else if (h > 0) dy2 = 1;
            dx2 = 0;
        }
        
        int numerator = longest >> 1;
        for (int i = 0; i <= longest; i++) {
            pixels.add(new Pixel(x1, y1));
            numerator += shortest;
            if (!(numerator < longest)) {
                numerator -= longest;
                x1 += dx1;
                y1 += dy1;
            } else {
                x1 += dx2;
                y1 += dy2;
            }
        }
        return pixels;
    }

    // 4. Алгоритм Кастла-Питвея (Сглаженная линия)
    public static List<Pixel> castlePitteway(int x1, int y1, int x2, int y2) {
        List<Pixel> pixels = new ArrayList<>();
        
        int dx = x2 - x1;
        int dy = y2 - y1;

        // Определяем направление шага
        int stepX = Integer.signum(dx);
        int stepY = Integer.signum(dy);

        dx = Math.abs(dx);
        dy = Math.abs(dy);

        // Если линия вырождена в точку
        if (dx == 0 && dy == 0) {
            pixels.add(new Pixel(x1, y1, 1.0f));
            return pixels;
        }

        // Выясняем, какая ось ведущая (X или Y)
        boolean swap = dy > dx;
        if (swap) {
            int temp = dx;
            dx = dy;
            dy = temp;
        }
        
        int errorAcc = 0; // Накопленная ошибка (числитель дроби errorAcc/dx)
        
        int x = x1;
        int y = y1;

        for (int i = 0; i <= dx; i++) {
            
            float intensity2 = (float) errorAcc / dx; // Насколько мы сместились к соседу
            float intensity1 = 1.0f - intensity2;     // Насколько мы остались на основной линии
            
            pixels.add(new Pixel(x, y, intensity1));
            
            if (swap) {
                pixels.add(new Pixel(x + stepX, y, intensity2));
            } else {
                pixels.add(new Pixel(x, y + stepY, intensity2));
            }

            // Итерация
            errorAcc += dy;
            if (errorAcc >= dx) {
                errorAcc -= dx; 
                if (swap) x += stepX; else y += stepY;
            }
            
            if (swap) y += stepY; else x += stepX;
        }
        return pixels;
    }

    // 5. Брезенхем (Окружность)
    public static List<Pixel> bresenhamCircle(int xc, int yc, int r) {
        List<Pixel> pixels = new ArrayList<>();
        int x = 0;
        int y = r;
        int d = 3 - 2 * r;

        addCirclePoints(xc, yc, x, y, pixels);

        while (y >= x) {
            x++;
            if (d > 0) {
                y--;
                d = d + 4 * (x - y) + 10;
            } else {
                d = d + 4 * x + 6;
            }
            addCirclePoints(xc, yc, x, y, pixels);
        }
        return pixels;
    }

    private static void addCirclePoints(int xc, int yc, int x, int y, List<Pixel> pixels) {
        pixels.add(new Pixel(xc + x, yc + y));
        pixels.add(new Pixel(xc - x, yc + y));
        pixels.add(new Pixel(xc + x, yc - y));
        pixels.add(new Pixel(xc - x, yc - y));
        pixels.add(new Pixel(xc + y, yc + x));
        pixels.add(new Pixel(xc - y, yc + x));
        pixels.add(new Pixel(xc + y, yc - x));
        pixels.add(new Pixel(xc - y, yc - x));
    }
}

// Визуализация
class GridPanel extends JPanel {
    private int cellSize = 20;
    private List<Pixel> pixelsToDraw = new ArrayList<>();
    
    public GridPanel() {
        setBackground(Color.WHITE);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseWheelListener(e -> {
            if (e.getWheelRotation() < 0) {
                cellSize = Math.min(100, cellSize + 2);
            } else {
                cellSize = Math.max(10, cellSize - 2);
            }
            repaint();
        });
        setToolTipText("Крутите колесико мыши для масштаба");
    }

    public void setPixels(List<Pixel> pixels) {
        this.pixelsToDraw = pixels;
        repaint();
    }

    public void clearPoints() {
        this.pixelsToDraw.clear();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        int w = getWidth();
        int h = getHeight();
        
        Graphics2D g2 = (Graphics2D) g;
        // Включаем поддержку прозрачности
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        int centerX = w / 2;
        int centerY = h / 2;

        // 1. Сетка
        g2.setColor(new Color(230, 230, 230)); 
        for (int x = centerX; x < w; x += cellSize) g2.drawLine(x, 0, x, h);
        for (int x = centerX; x > 0; x -= cellSize) g2.drawLine(x, 0, x, h);
        for (int y = centerY; y < h; y += cellSize) g2.drawLine(0, y, w, y);
        for (int y = centerY; y > 0; y -= cellSize) g2.drawLine(0, y, w, y);

        // 2. Оси и текст
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1));
        g2.drawLine(centerX, 0, centerX, h);
        g2.drawLine(0, centerY, w, centerY);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        FontMetrics fm = g2.getFontMetrics();
        int textOffset = 15; 
        
        int maxStepsX = w / 2 / cellSize + 1;
        int maxStepsY = h / 2 / cellSize + 1;

        // ось X
        for (int i = 1; i < maxStepsX; i++) {
            // ->
            int px = centerX + i * cellSize;
            g2.drawLine(px, centerY - 2, px, centerY + 2);
            String val = String.valueOf(i);
            g2.drawString(val, px - fm.stringWidth(val) / 2, centerY + textOffset);
            
            // <-
            px = centerX - i * cellSize;
            g2.drawLine(px, centerY - 2, px, centerY + 2);
            val = String.valueOf(-i);
            g2.drawString(val, px - fm.stringWidth(val) / 2, centerY + textOffset);
        }

        // ось Y
        for (int i = 1; i < maxStepsY; i++) {
            // +
            int py = centerY - i * cellSize;
            g2.drawLine(centerX - 2, py, centerX + 2, py);
            String val = String.valueOf(i);
            g2.drawString(val, centerX - textOffset - fm.stringWidth(val), py + fm.getAscent() / 2 - 1);
            
            // -
            py = centerY + i * cellSize;
            g2.drawLine(centerX - 2, py, centerX + 2, py);
            val = String.valueOf(-i);
            g2.drawString(val, centerX - textOffset - fm.stringWidth(val), py + fm.getAscent() / 2 - 1);
        }
        
        g2.drawString("0", centerX - 10, centerY + 15);
        g2.drawString("X", w - 15, centerY - 5);
        g2.drawString("Y", centerX + 5, 15);

        // Рисуем пиксели
        int red = 65, green = 105, blue = 225;
        
        for (Pixel p : pixelsToDraw) {
            int screenX = centerX + p.x * cellSize;
            int screenY = centerY - p.y * cellSize - cellSize;
            
            int alpha = Math.round(p.opacity * 255);
            if (alpha < 0) alpha = 0;
            if (alpha > 255) alpha = 255;
            
            if (alpha > 0) {
                g2.setColor(new Color(red, green, blue, alpha));
                g2.fillRect(screenX + 1, screenY + 1, cellSize - 1, cellSize - 1);
            }
        }
        
        // 4. Инфо
        String scaleInfo = "Цена деления: 1 ед.";
        String zoomInfo = "Масштаб: " + cellSize + " px/ед.";
        
        g2.setColor(new Color(255, 255, 220));
        g2.fillRect(5, h - 45, 150, 40);
        g2.setColor(Color.BLACK);
        g2.drawRect(5, h - 45, 150, 40);
        
        g2.drawString(scaleInfo, 10, h - 25);
        g2.drawString(zoomInfo, 10, h - 10);
    }
}