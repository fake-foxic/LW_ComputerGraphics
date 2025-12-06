import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ClippingApp extends JFrame {

    // Элементы интерфейса
    private CanvasPanel canvasPanel;
    private JTextArea logArea;
    private JRadioButton rbLiangBarsky, rbConvexPolygon;
    private JButton btnDrawPoly;
    private JLabel statusLabel;
    
    // Данные сцены
    private List<LineSegment> segments = new ArrayList<>();
    private Rectangle clipRect = null;
    private List<Point2D> clipPolygon = null;
    
    // Состояние
    private boolean isDrawingMode = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}
            new ClippingApp().setVisible(true);
        });
    }

    public ClippingApp() {
        setTitle("Алгоритмы отсечения (FIXED)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 850);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Левая панель
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        controlPanel.setPreferredSize(new Dimension(320, 0));

        JButton btnLoad = new JButton("Загрузить файл...");
        btnLoad.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPanel modePanel = new JPanel(new GridLayout(0, 1));
        modePanel.setBorder(BorderFactory.createTitledBorder("Алгоритм"));
        modePanel.setMaximumSize(new Dimension(300, 100));
        ButtonGroup bg = new ButtonGroup();
        rbLiangBarsky = new JRadioButton("Лианг-Барски (Прямоугольник)", true);
        rbConvexPolygon = new JRadioButton("Выпуклый многоугольник");
        bg.add(rbLiangBarsky);
        bg.add(rbConvexPolygon);
        modePanel.add(rbLiangBarsky);
        modePanel.add(rbConvexPolygon);
        
        JPanel polyTools = new JPanel(new GridLayout(2, 1, 5, 5));
        polyTools.setBorder(BorderFactory.createTitledBorder("Инструменты"));
        polyTools.setMaximumSize(new Dimension(300, 80));
        btnDrawPoly = new JButton("Нарисовать полигон");
        btnDrawPoly.setEnabled(false);
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(0, 100, 0));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        polyTools.add(btnDrawPoly);
        polyTools.add(statusLabel);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollLog = new JScrollPane(logArea);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Лог отсечения"));

        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(btnLoad);
        controlPanel.add(Box.createVerticalStrut(20));
        controlPanel.add(modePanel);
        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(polyTools);
        controlPanel.add(Box.createVerticalStrut(15));
        controlPanel.add(scrollLog);

        add(controlPanel, BorderLayout.WEST);
        canvasPanel = new CanvasPanel();
        add(canvasPanel, BorderLayout.CENTER);

        // События
        btnLoad.addActionListener(e -> loadFromFile());
        
        ActionListener modeListener = e -> {
            boolean isPoly = rbConvexPolygon.isSelected();
            btnDrawPoly.setEnabled(isPoly);
            if (isPoly && clipPolygon == null && clipRect != null) {
                clipPolygon = rectToPoly(clipRect);
            }
            if (!isPoly && isDrawingMode) stopDrawing(); 
            canvasPanel.repaint();
            performClipping();
        };
        rbLiangBarsky.addActionListener(modeListener);
        rbConvexPolygon.addActionListener(modeListener);
        
        btnDrawPoly.addActionListener(e -> startDrawing());
    }

    // Логика рисования
    private void startDrawing() {
        isDrawingMode = true;
        clipPolygon = new ArrayList<>();
        statusLabel.setText("ЛКМ: Точка, ПКМ: Завершить");
        canvasPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        logArea.setText("Режим рисования.\nРасставьте точки по кругу.\nНажмите ПКМ для замыкания.");
        for(LineSegment s : segments) s.visiblePart = null;
        canvasPanel.repaint();
    }
    
    private void stopDrawing() {
        isDrawingMode = false;
        statusLabel.setText(" ");
        canvasPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        
        if (clipPolygon != null && clipPolygon.size() >= 3) {
            if (isConvex(clipPolygon)) {
                performClipping();
            } else {
                JOptionPane.showMessageDialog(this, "Полигон не выпуклый!\nРезультат может быть некорректным.");
                performClipping();
            }
        } else {
            logArea.setText("Полигон не задан.");
        }
        canvasPanel.repaint();
    }

    private List<Point2D> rectToPoly(Rectangle r) {
        List<Point2D> poly = new ArrayList<>();
        poly.add(new Point2D(r.xmin, r.ymax));
        poly.add(new Point2D(r.xmin, r.ymin));
        poly.add(new Point2D(r.xmax, r.ymin));
        poly.add(new Point2D(r.xmax, r.ymax));
        return poly;
    }

    private boolean isConvex(List<Point2D> poly) {
        if (poly.size() < 3) return false;
        boolean hasPos = false, hasNeg = false;
        for (int i = 0; i < poly.size(); i++) {
            Point2D a = poly.get(i);
            Point2D b = poly.get((i + 1) % poly.size());
            Point2D c = poly.get((i + 2) % poly.size());
            double cross = (b.x - a.x)*(c.y - b.y) - (b.y - a.y)*(c.x - b.x);
            if (cross > 0) hasPos = true;
            if (cross < 0) hasNeg = true;
            if (hasPos && hasNeg) return false;
        }
        return true;
    }

    // Загрузка
    private void loadFromFile() {
        JFileChooser fc = new JFileChooser(new File("."));
        fc.setFileFilter(new FileNameExtensionFilter("Текстовые файлы (*.txt)", "txt"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (BufferedReader br = new BufferedReader(new FileReader(fc.getSelectedFile()))) {
                segments.clear();
                String line = br.readLine();
                if (line == null) return;
                
                line = line.trim();
                if (line.contains(" ")) line = line.split("\\s+")[0];
                int n = Integer.parseInt(line);

                for (int i = 0; i < n; i++) {
                    String[] parts = br.readLine().trim().split("\\s+");
                    segments.add(new LineSegment(
                        Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]), Double.parseDouble(parts[3])
                    ));
                }

                String[] rParts = br.readLine().trim().split("\\s+");
                double x1 = Double.parseDouble(rParts[0]);
                double y1 = Double.parseDouble(rParts[1]);
                double x2 = Double.parseDouble(rParts[2]);
                double y2 = Double.parseDouble(rParts[3]);
                
                clipRect = new Rectangle(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2));
                clipPolygon = rectToPoly(clipRect);

                logArea.setText("Файл загружен.\nОтрезков: " + n);
                performClipping();
                canvasPanel.repaint();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage());
            }
        }
    }

    // Выполнение
    private void performClipping() {
        StringBuilder sb = new StringBuilder();
        sb.append("Результаты:\n----------------\n");
        
        if (rbLiangBarsky.isSelected()) {
            if (clipRect == null) sb.append("Нет данных.");
            else {
                for (int i = 0; i < segments.size(); i++) {
                    LineSegment seg = segments.get(i);
                    LineSegment clipped = Clipper.clipLiangBarsky(seg, clipRect);
                    seg.visiblePart = clipped;
                    sb.append(String.format("L%d: %s\n", i+1, (clipped!=null ? "ВИДИМ" : "НЕ ВИДИМ")));
                }
            }
        } else {
            if (clipPolygon == null || clipPolygon.size() < 3) sb.append("Полигон не задан.");
            else {
                sb.append("Полигон: ").append(clipPolygon.size()).append(" вершин\n");
                for (int i = 0; i < segments.size(); i++) {
                    LineSegment seg = segments.get(i);
                    LineSegment clipped = Clipper.clipConvexPolygon(seg, clipPolygon);
                    seg.visiblePart = clipped;
                    sb.append(String.format("L%d: %s\n", i+1, (clipped!=null ? "ВИДИМ" : "НЕ ВИДИМ")));
                }
            }
        }
        logArea.setText(sb.toString());
        logArea.setCaretPosition(0);
    }

    // Холст
    class CanvasPanel extends JPanel {
        private int scale = 40;
        private Point lastMouse;
        private int offsetX = 0, offsetY = 0;

        public CanvasPanel() {
            setBackground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (isDrawingMode) {
                        if (SwingUtilities.isRightMouseButton(e)) stopDrawing();
                        else {
                            double lx = (double)(e.getX() - getWidth()/2 - offsetX) / scale;
                            double ly = (double)(getHeight()/2 + offsetY - e.getY()) / scale;
                            clipPolygon.add(new Point2D(lx, ly));
                            repaint();
                        }
                    } else lastMouse = e.getPoint();
                }
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!isDrawingMode && lastMouse != null) {
                        offsetX += e.getX() - lastMouse.x;
                        offsetY += e.getY() - lastMouse.y;
                        lastMouse = e.getPoint();
                        repaint();
                    }
                }
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    if (!isDrawingMode) {
                        if (e.getWheelRotation() < 0) scale = Math.min(300, scale + 5);
                        else scale = Math.max(5, scale - 5);
                        repaint();
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
            addMouseWheelListener(ma);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int cx = w / 2 + offsetX, cy = h / 2 + offsetY;

            // Сетка
            g2.setColor(new Color(230, 230, 230));
            for (int x = cx % scale; x < w; x += scale) g2.drawLine(x, 0, x, h);
            for (int y = cy % scale; y < h; y += scale) g2.drawLine(0, y, w, y);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1));
            g2.drawLine(cx, 0, cx, h);
            g2.drawLine(0, cy, w, cy);
            g2.drawString("0,0", cx + 5, cy + 15);
            g2.drawString("X", w - 20, cy - 5);
            g2.drawString("Y", cx + 5, 20);

            // Полигоны
            g2.setStroke(new BasicStroke(2));
            if (rbLiangBarsky.isSelected() && clipRect != null) {
                g2.setColor(new Color(0, 100, 255, 50));
                int rx = cx + (int)(clipRect.xmin * scale);
                int ry = cy - (int)(clipRect.ymax * scale);
                int rw = (int)((clipRect.xmax - clipRect.xmin) * scale);
                int rh = (int)((clipRect.ymax - clipRect.ymin) * scale);
                g2.fillRect(rx, ry, rw, rh);
                g2.setColor(Color.BLUE);
                g2.drawRect(rx, ry, rw, rh);
            } else if (rbConvexPolygon.isSelected() && clipPolygon != null && !clipPolygon.isEmpty()) {
                g2.setColor(new Color(0, 150, 0, 50));
                Polygon p = toAwtPolygon(clipPolygon, cx, cy);
                g2.fillPolygon(p);
                g2.setColor(new Color(0, 150, 0));
                g2.drawPolygon(p);
                if (isDrawingMode) {
                    g2.setColor(Color.MAGENTA);
                    for (Point2D pt : clipPolygon) drawPoint(g2, pt.x, pt.y, cx, cy);
                }
            }

            // Отрезки
            for (int i = 0; i < segments.size(); i++) {
                LineSegment seg = segments.get(i);
                g2.setColor(Color.LIGHT_GRAY);
                g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
                drawLine(g2, seg.x1, seg.y1, seg.x2, seg.y2, cx, cy);

                if (seg.visiblePart != null) {
                    g2.setColor(Color.RED);
                    g2.setStroke(new BasicStroke(3));
                    drawLine(g2, seg.visiblePart.x1, seg.visiblePart.y1, seg.visiblePart.x2, seg.visiblePart.y2, cx, cy);
                    drawPoint(g2, seg.visiblePart.x1, seg.visiblePart.y1, cx, cy);
                    drawPoint(g2, seg.visiblePart.x2, seg.visiblePart.y2, cx, cy);
                }
                
                g2.setColor(Color.DARK_GRAY);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                int lx = cx + (int)(seg.x1 * scale);
                int ly = cy - (int)(seg.y1 * scale);
                g2.drawString("L" + (i+1), lx + 5, ly - 5);
            }
            
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString("Масштаб: " + scale, 10, h - 10);
            if (isDrawingMode) {
                g2.setColor(Color.MAGENTA);
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                g2.drawString("РИСОВАНИЕ (ЛКМ - Точка, ПКМ - Завершить)", 10, 25);
            }
        }

        private void drawLine(Graphics2D g2, double x1, double y1, double x2, double y2, int cx, int cy) {
            g2.drawLine(cx + (int)(x1 * scale), cy - (int)(y1 * scale), cx + (int)(x2 * scale), cy - (int)(y2 * scale));
        }
        private void drawPoint(Graphics2D g2, double x, double y, int cx, int cy) {
            g2.fillOval(cx + (int)(x * scale) - 3, cy - (int)(y * scale) - 3, 6, 6);
        }
        private Polygon toAwtPolygon(List<Point2D> pts, int cx, int cy) {
            Polygon p = new Polygon();
            for (Point2D pt : pts) p.addPoint(cx + (int)(pt.x * scale), cy - (int)(pt.y * scale));
            return p;
        }
    }
}

// Данные
class Point2D {
    double x, y;
    Point2D(double x, double y) { this.x = x; this.y = y; }
}
class LineSegment {
    double x1, y1, x2, y2;
    LineSegment visiblePart;
    LineSegment(double x1, double y1, double x2, double y2) {
        this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
    }
}
class Rectangle {
    double xmin, ymin, xmax, ymax;
    Rectangle(double xmin, double ymin, double xmax, double ymax) {
        this.xmin = xmin; this.ymin = ymin; this.xmax = xmax; this.ymax = ymax;
    }
}

// Математика
class Clipper {

    public static LineSegment clipLiangBarsky(LineSegment seg, Rectangle rect) {
        double t0 = 0.0, t1 = 1.0;
        double dx = seg.x2 - seg.x1;
        double dy = seg.y2 - seg.y1;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {seg.x1 - rect.xmin, rect.xmax - seg.x1, seg.y1 - rect.ymin, rect.ymax - seg.y1};

        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) {
                if (q[i] < 0) return null;
            } else {
                double t = q[i] / p[i];
                if (p[i] < 0) {
                    if (t > t1) return null;
                    if (t > t0) t0 = t;
                } else {
                    if (t < t0) return null;
                    if (t < t1) t1 = t;
                }
            }
        }
        if (t0 > t1) return null;
        return new LineSegment(seg.x1 + t0 * dx, seg.y1 + t0 * dy, seg.x1 + t1 * dx, seg.y1 + t1 * dy);
    }

    public static LineSegment clipConvexPolygon(LineSegment seg, List<Point2D> poly) {
        // Центр полигона
        double cx = 0, cy = 0;
        for (Point2D p : poly) { cx += p.x; cy += p.y; }
        cx /= poly.size();
        cy /= poly.size();

        double t0 = 0.0, t1 = 1.0;
        double dx = seg.x2 - seg.x1;
        double dy = seg.y2 - seg.y1;

        for (int i = 0; i < poly.size(); i++) {
            Point2D p1 = poly.get(i);
            Point2D p2 = poly.get((i + 1) % poly.size());

            double edgeDx = p2.x - p1.x;
            double edgeDy = p2.y - p1.y;
            // Исходная нормаль
            double nx = -edgeDy;
            double ny = edgeDx;

            // Разворачиваем нормаль К ЦЕНТРУ
            if (nx * (cx - p1.x) + ny * (cy - p1.y) < 0) {
                nx = -nx; ny = -ny;
            }

            double wx = seg.x1 - p1.x;
            double wy = seg.y1 - p1.y;
            double d_n = dx * nx + dy * ny;
            double w_n = wx * nx + wy * ny;

            if (Math.abs(d_n) < 1e-9) {
                if (w_n < 0) return null;
            } else {
                double t = -w_n / d_n;
                if (d_n > 0) {
                    if (t > t0) t0 = t;
                } else {
                    if (t < t1) t1 = t;
                }
            }
        }

        if (t0 > t1) return null;
        return new LineSegment(seg.x1 + t0 * dx, seg.y1 + t0 * dy, seg.x1 + t1 * dx, seg.y1 + t1 * dy);
    }
}