import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ImageProcessorApp extends JFrame {

    private BufferedImage originalImage;
    private JTabbedPane tabbedPane;

    // Панели для вкладок
    private ComparisonPanel contrastPanel;
    private ComparisonPanel colorPanel;
    private FilterPanel filterPanel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) { /* ignore */ }
            new ImageProcessorApp().setVisible(true);
        });
    }

    public ImageProcessorApp() {
        setTitle("Обработка изображений: Гистограммы и Фильтры");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 850);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Меню
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Файл");
        JMenuItem openItem = new JMenuItem("Открыть изображение...");
        openItem.addActionListener(e -> openImage());
        fileMenu.add(openItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Вкладки
        tabbedPane = new JTabbedPane();

        // 1: Линейное контрастирование vs Эквализация
        contrastPanel = new ComparisonPanel("Линейное контрастирование", "Эквализация (RGB)");
        tabbedPane.addTab("1. Повышение контраста", contrastPanel);

        // 2: RGB эквализация vs HSV эквализация
        colorPanel = new ComparisonPanel("RGB Эквализация (Поканальная)", "HSV Эквализация (Яркость)");
        tabbedPane.addTab("2. Цветная эквализация", colorPanel);

        // 3: Фильтры
        filterPanel = new FilterPanel();
        tabbedPane.addTab("3. Низкочастотные фильтры", filterPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "bmp", "jpeg"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                originalImage = ImageIO.read(chooser.getSelectedFile());
                if (originalImage == null) throw new IOException("Не удалось прочитать изображение");
                processImages();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Ошибка чтения файла: " + e.getMessage());
            }
        }
    }

    private void processImages() {
        if (originalImage == null) return;

        // Генерация обработанных изображений
        BufferedImage linear = ImageAlgorithms.linearContrastStretching(originalImage);
        BufferedImage histEqRGB = ImageAlgorithms.histogramEqualizationRGB(originalImage);
        BufferedImage histEqHSV = ImageAlgorithms.histogramEqualizationHSV(originalImage);

        // Обновление вкладок
        contrastPanel.updateImages(originalImage, linear, histEqRGB);
        colorPanel.updateImages(originalImage, histEqRGB, histEqHSV);
        filterPanel.setOriginalImage(originalImage);
    }

    // UI КОМПОНЕНТЫ
    //Панель сравнения: Оригинал | Метод 1 | Метод 2
    class ComparisonPanel extends JPanel {
        private ImagePanel pOriginal, pMethod1, pMethod2;
        private HistogramPanel hOriginal, hMethod1, hMethod2;
        
        public ComparisonPanel(String title1, String title2) {
            setLayout(new GridLayout(1, 3, 10, 10));
            setBorder(new EmptyBorder(10, 10, 10, 10));

            add(createColumn("Оригинал", pOriginal = new ImagePanel(), hOriginal = new HistogramPanel()));
            add(createColumn(title1, pMethod1 = new ImagePanel(), hMethod1 = new HistogramPanel()));
            add(createColumn(title2, pMethod2 = new ImagePanel(), hMethod2 = new HistogramPanel()));
        }

        private JPanel createColumn(String title, ImagePanel imgP, HistogramPanel histP) {
            JPanel col = new JPanel(new BorderLayout());
            col.add(new JLabel(title, SwingConstants.CENTER), BorderLayout.NORTH);
            col.add(imgP, BorderLayout.CENTER);
            col.add(histP, BorderLayout.SOUTH);
            return col;
        }

        public void updateImages(BufferedImage orig, BufferedImage m1, BufferedImage m2) {
            pOriginal.setImage(orig); hOriginal.computeHistogram(orig);
            pMethod1.setImage(m1);    hMethod1.computeHistogram(m1);
            pMethod2.setImage(m2);    hMethod2.computeHistogram(m2);
            repaint();
        }
    }

    class FilterPanel extends JPanel {
        private BufferedImage source;
        private ImagePanel pDisplay;
        private JComboBox<String> filterBox;

        public FilterPanel() {
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(10, 10, 10, 10));

            JPanel controls = new JPanel();
            filterBox = new JComboBox<>(new String[]{
                "Оригинал", 
                "Box Blur (3x3)", 
                "Gaussian Blur (3x3)", 
                "Gaussian Blur (5x5)",
                "Strong Box Blur (11x11)" 
            });
            JButton applyBtn = new JButton("Применить");
            
            controls.add(new JLabel("Выберите фильтр:"));
            controls.add(filterBox);
            controls.add(applyBtn);

            pDisplay = new ImagePanel();
            add(controls, BorderLayout.NORTH);
            add(pDisplay, BorderLayout.CENTER);

            applyBtn.addActionListener(e -> applyFilter());
        }

        public void setOriginalImage(BufferedImage img) {
            this.source = img;
            pDisplay.setImage(img);
        }

        private void applyFilter() {
            if (source == null) return;
            int idx = filterBox.getSelectedIndex();
            BufferedImage result = source;

            if (idx == 1) result = ImageAlgorithms.applyBoxBlur(source);
            else if (idx == 2) result = ImageAlgorithms.applyGaussianBlur3x3(source);
            else if (idx == 3) result = ImageAlgorithms.applyGaussianBlur5x5(source);
            else if (idx == 4) result = ImageAlgorithms.applyStrongBlur(source);

            pDisplay.setImage(result);
            repaint();
        }
    }

    // Панель для отрисовки изображения с масштабированием
    class ImagePanel extends JPanel {
        private BufferedImage img;

        public void setImage(BufferedImage img) {
            this.img = img;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img != null) {
                double scale = Math.min((double)getWidth()/img.getWidth(), (double)getHeight()/img.getHeight());
                int w = (int)(img.getWidth() * scale);
                int h = (int)(img.getHeight() * scale);
                int x = (getWidth() - w) / 2;
                int y = (getHeight() - h) / 2;
                
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(img, x, y, w, h, this);
            }
        }
    }

    // Панель гистограммы (по яркости)
    class HistogramPanel extends JPanel {
        private int[] histogram;
        private int maxCount = 0;

        public HistogramPanel() {
            setPreferredSize(new Dimension(200, 120));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        }

        public void computeHistogram(BufferedImage img) {
            if (img == null) return;
            histogram = new int[256];
            maxCount = 0;
            
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int rgb = img.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    // Формула яркости
                    int gray = (int)(0.299 * r + 0.587 * g + 0.114 * b);
                    if (gray > 255) gray = 255;
                    histogram[gray]++;
                }
            }
            for (int c : histogram) maxCount = Math.max(maxCount, c);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (histogram == null) return;
            
            g.setColor(Color.DARK_GRAY);
            int width = getWidth();
            int height = getHeight();
            
            for (int i = 0; i < 256; i++) {
                int x = (int)((i / 256.0) * width);
                int barHeight = (int)(((double)histogram[i] / maxCount) * (height - 5));
                g.drawLine(x, height, x, height - barHeight);
            }
        }
    }
}

// Алгоритмы (математика)
class ImageAlgorithms {

    // 1. Линейное контрастирование
    public static BufferedImage linearContrastStretching(BufferedImage src) {
        BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        
        int minR = 255, maxR = 0, minG = 255, maxG = 0, minB = 255, maxB = 0;

        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                if (r < minR) minR = r; if (r > maxR) maxR = r;
                if (g < minG) minG = g; if (g > maxG) maxG = g;
                if (b < minB) minB = b; if (b > maxB) maxB = b;
            }
        }

        if (maxR == minR) maxR++;
        if (maxG == minG) maxG++;
        if (maxB == minB) maxB++;

        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int newR = clamp((r - minR) * 255 / (maxR - minR));
                int newG = clamp((g - minG) * 255 / (maxG - minG));
                int newB = clamp((b - minB) * 255 / (maxB - minB));

                dest.setRGB(x, y, (newR << 16) | (newG << 8) | newB);
            }
        }
        return dest;
    }

    // 2. Эквализация гистограммы RGB
    public static BufferedImage histogramEqualizationRGB(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dest = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        
        int[] histR = new int[256];
        int[] histG = new int[256];
        int[] histB = new int[256];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                histR[(rgb >> 16) & 0xFF]++;
                histG[(rgb >> 8) & 0xFF]++;
                histB[rgb & 0xFF]++;
            }
        }

        int totalPixels = w * h;
        int[] lutR = calculateLUT(histR, totalPixels);
        int[] lutG = calculateLUT(histG, totalPixels);
        int[] lutB = calculateLUT(histB, totalPixels);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                dest.setRGB(x, y, (lutR[r] << 16) | (lutG[g] << 8) | lutB[b]);
            }
        }
        return dest;
    }

    // 3. Эквализация гистограммы HSV
    public static BufferedImage histogramEqualizationHSV(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage dest = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        float[][] hsbArray = new float[width * height][3];
        int[] histV = new int[256];

        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = src.getRGB(x, y);
                Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsbArray[idx]);
                int vInt = (int)(hsbArray[idx][2] * 255);
                if (vInt > 255) vInt = 255;
                histV[vInt]++;
                idx++;
            }
        }

        int[] lutV = calculateLUT(histV, width * height);

        idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float h = hsbArray[idx][0];
                float s = hsbArray[idx][1];
                float oldV = hsbArray[idx][2];
                int vInt = (int)(oldV * 255);
                if (vInt > 255) vInt = 255;
                
                float newV = lutV[vInt] / 255.0f;
                dest.setRGB(x, y, Color.HSBtoRGB(h, s, newV));
                idx++;
            }
        }
        return dest;
    }

    private static int[] calculateLUT(int[] histogram, int totalPixels) {
        int[] lut = new int[256];
        long sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += histogram[i];
            lut[i] = (int) (sum * 255 / totalPixels);
            if (lut[i] > 255) lut[i] = 255;
        }
        return lut;
    }

    // 4. Фильтры

    // Основной метод свертки, который обрабатывает края
    private static BufferedImage convolve(BufferedImage src, float[] kernel, int kernelWidth, int kernelHeight) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dest = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        int radiusX = kernelWidth / 2;
        int radiusY = kernelHeight / 2;

        int[] srcPixels = src.getRGB(0, 0, w, h, null, 0, w);
        int[] destPixels = new int[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                
                float rSum = 0, gSum = 0, bSum = 0;

                for (int ky = 0; ky < kernelHeight; ky++) {
                    for (int kx = 0; kx < kernelWidth; kx++) {
                        
                        // Координаты пикселя-соседа
                        int posX = x + kx - radiusX;
                        int posY = y + ky - radiusY;

                        // CLAMPING: Если вышли за край, берем крайний пиксель
                        if (posX < 0) posX = 0;
                        else if (posX >= w) posX = w - 1;
                        
                        if (posY < 0) posY = 0;
                        else if (posY >= h) posY = h - 1;

                        int rgb = srcPixels[posY * w + posX];
                        float weight = kernel[ky * kernelWidth + kx];

                        rSum += ((rgb >> 16) & 0xFF) * weight;
                        gSum += ((rgb >> 8) & 0xFF) * weight;
                        bSum += (rgb & 0xFF) * weight;
                    }
                }

                int r = clamp((int) rSum);
                int g = clamp((int) gSum);
                int b = clamp((int) bSum);

                destPixels[y * w + x] = (0xFF << 24) | (r << 16) | (g << 8) | b;
            }
        }
        dest.setRGB(0, 0, w, h, destPixels, 0, w);
        return dest;
    }

    // Box Blur (3x3)
    public static BufferedImage applyBoxBlur(BufferedImage src) {
        float weight = 1.0f / 9.0f;
        float[] kernel = new float[9];
        Arrays.fill(kernel, weight);
        return convolve(src, kernel, 3, 3);
    }
    
    // Gaussian Blur (3x3)
    public static BufferedImage applyGaussianBlur3x3(BufferedImage src) {
        float[] kernel = {
            1/16f, 2/16f, 1/16f,
            2/16f, 4/16f, 2/16f,
            1/16f, 2/16f, 1/16f
        };
        return convolve(src, kernel, 3, 3);
    }
    
    // Gaussian Blur (5x5)
    public static BufferedImage applyGaussianBlur5x5(BufferedImage src) {
        float[] kernel = {
            1/256f, 4/256f,  6/256f,  4/256f, 1/256f,
            4/256f, 16/256f, 24/256f, 16/256f, 4/256f,
            6/256f, 24/256f, 36/256f, 24/256f, 6/256f,
            4/256f, 16/256f, 24/256f, 16/256f, 4/256f,
            1/256f, 4/256f,  6/256f,  4/256f, 1/256f
        };
        return convolve(src, kernel, 5, 5);
    }

    // Strong Blur (11x11)
    public static BufferedImage applyStrongBlur(BufferedImage src) {
        int size = 11;
        float weight = 1.0f / (size * size);
        float[] kernel = new float[size * size];
        Arrays.fill(kernel, weight);
        return convolve(src, kernel, 11, 11);
    }

    private static int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }
}