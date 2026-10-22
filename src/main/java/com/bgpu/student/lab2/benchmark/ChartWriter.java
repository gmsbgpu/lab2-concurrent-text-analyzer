package com.bgpu.student.lab2.benchmark;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Рисует три PNG без сторонних библиотек: только JDK (AWT + ImageIO).
 */
public final class ChartWriter {

    private static final int W = 900;
    private static final int H = 600;
    private static final int PAD_L = 80;
    private static final int PAD_R = 220; // место под легенду
    private static final int PAD_T = 60;
    private static final int PAD_B = 80;

    private static final Color[] PALETTE = {
            new Color(0x1f77b4), new Color(0xff7f0e),
            new Color(0x2ca02c), new Color(0xd62728),
            new Color(0x9467bd), new Color(0x8c564b),
            new Color(0xe377c2)
    };

    private ChartWriter() {
    }

    public static void writeAll(Path resultsDir, List<Benchmark.Result> results)
            throws IOException {

        Files.createDirectories(resultsDir);

        // Только корректные варианты идём на графики.
        List<Benchmark.Result> correct = new ArrayList<>();
        for (Benchmark.Result r : results) {
            if (r.correct()) {
                correct.add(r);
            }
        }

        // T1: берём sequential с 1 потоком. Если его нет — минимальное
        // время среди корректных вариантов с 1 потоком.
        double t1 = findT1(correct);

        writeTimeChart(resultsDir.resolve("time.png"), correct);
        writeSpeedupChart(resultsDir.resolve("speedup.png"), correct, t1);
        writeEfficiencyChart(resultsDir.resolve("efficiency.png"), correct, t1);
    }

    private static double findT1(List<Benchmark.Result> results) {
        for (Benchmark.Result r : results) {
            if ("sequential".equals(r.variant()) && r.threads() == 1) {
                return r.medianSeconds();
            }
        }
        double min = Double.POSITIVE_INFINITY;
        for (Benchmark.Result r : results) {
            if (r.threads() == 1) {
                min = Math.min(min, r.medianSeconds());
            }
        }
        if (min == Double.POSITIVE_INFINITY) {
            throw new IllegalStateException(
                    "Нет ни одного корректного результата с 1 потоком");
        }
        return min;
    }

    // ---------- Утилиты ----------

    /** Группировка: variant → (threads → value). */
    private static Map<String, TreeMap<Integer, Double>> group(
            List<Benchmark.Result> results,
            java.util.function.ToDoubleFunction<Benchmark.Result> f) {

        Map<String, TreeMap<Integer, Double>> byVariant = new LinkedHashMap<>();
        for (Benchmark.Result r : results) {
            byVariant.computeIfAbsent(r.variant(), k -> new TreeMap<>())
                    .put(r.threads(), f.applyAsDouble(r));
        }
        return byVariant;
    }

    private static List<Integer> sortedThreads(
            Map<String, TreeMap<Integer, Double>> byVariant) {
        TreeMap<Integer, Double> merged = new TreeMap<>();
        for (TreeMap<Integer, Double> m : byVariant.values()) {
            for (Integer t : m.keySet()) {
                merged.put(t, 0.0);
            }
        }
        return new ArrayList<>(merged.keySet());
    }

    private static BufferedImage newCanvas(String title) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString(title, PAD_L, 30);
        return img;
    }

    private static void drawAxes(Graphics2D g, double maxX, double maxY,
                                 String xLabel, String yLabel) {
        int x0 = PAD_L;
        int y0 = H - PAD_B;
        int x1 = W - PAD_R;
        int y1 = PAD_T;

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.2f));
        g.drawLine(x0, y0, x1, y0); // X
        g.drawLine(x0, y0, x0, y1); // Y

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));

        // Деления по X (логарифмическая шкала потоков: 1,2,4,8,...)
        for (int t : new int[]{1, 2, 4, 8, 16, 32, 64}) {
            if (t > maxX) continue;
            int px = (int) mapX(t, maxX, x0, x1);
            g.drawLine(px, y0, px, y0 + 5);
            g.drawString(String.valueOf(t), px - 6, y0 + 20);
        }

        // Деления по Y
        int steps = 5;
        for (int i = 0; i <= steps; i++) {
            double v = maxY * i / steps;
            int py = (int) mapY(v, maxY, y0, y1);
            g.drawLine(x0 - 5, py, x0, py);
            g.drawString(String.format(java.util.Locale.ROOT, "%.2f", v),
                    x0 - 55, py + 4);
            g.setColor(new Color(0xE0E0E0));
            g.drawLine(x0 + 1, py, x1, py);
            g.setColor(Color.BLACK);
        }

        g.drawString(xLabel, (x0 + x1) / 2 - 40, H - 25);
        java.awt.geom.AffineTransform old = g.getTransform();
        g.rotate(-Math.PI / 2);
        g.drawString(yLabel, -(y0 + y1) / 2 - 30, 25);
        g.setTransform(old);
    }

    private static double mapX(double t, double maxX, int x0, int x1) {
        // логарифмическая шкала: log2(t), 1 → 0
        double lt = Math.log(t) / Math.log(2);
        double lmax = Math.log(maxX) / Math.log(2);
        return x0 + (x1 - x0) * (lt / Math.max(lmax, 1e-9));
    }

    private static double mapY(double v, double maxY, int y0, int y1) {
        return y0 - (y0 - y1) * (v / Math.max(maxY, 1e-9));
    }

    private static void drawLegend(Graphics2D g,
                                   Map<String, TreeMap<Integer, Double>> byVariant) {
        int lx = W - PAD_R + 20;
        int ly = PAD_T + 10;
        int i = 0;
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (String variant : byVariant.keySet()) {
            g.setColor(PALETTE[i % PALETTE.length]);
            g.fillRect(lx, ly + i * 22, 14, 14);
            g.setColor(Color.BLACK);
            g.drawString(variant, lx + 20, ly + i * 22 + 12);
            i++;
        }
    }

    // ---------- Три графика ----------

    private static void writeTimeChart(Path file, List<Benchmark.Result> results)
            throws IOException {

        Map<String, TreeMap<Integer, Double>> data =
                group(results, Benchmark.Result::medianSeconds);

        BufferedImage img = newCanvas("Время выполнения (сек)");
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        List<Integer> threads = sortedThreads(data);
        double maxX = threads.isEmpty() ? 1 : threads.get(threads.size() - 1);
        double maxY = 0;
        for (TreeMap<Integer, Double> m : data.values()) {
            for (double v : m.values()) maxY = Math.max(maxY, v);
        }
        maxY *= 1.1;

        drawAxes(g, maxX, maxY, "потоков", "время, с");
        drawLines(g, data, maxX, maxY, v -> v, x -> x);
        drawLegend(g, data);

        g.dispose();
        ImageIO.write(img, "png", file.toFile());
    }

    private static void writeSpeedupChart(Path file,
                                          List<Benchmark.Result> results,
                                          double t1) throws IOException {

        Map<String, TreeMap<Integer, Double>> data =
                group(results, r -> t1 / r.medianSeconds());

        BufferedImage img = newCanvas("Ускорение S_p = T1 / Tp");
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        List<Integer> threads = sortedThreads(data);
        double maxX = threads.isEmpty() ? 1 : threads.get(threads.size() - 1);
        double maxY = 1;
        for (TreeMap<Integer, Double> m : data.values()) {
            for (double v : m.values()) maxY = Math.max(maxY, v);
        }
        maxY = Math.ceil(maxY) + 1;

        drawAxes(g, maxX, maxY, "потоков", "S_p");
        drawLines(g, data, maxX, maxY, v -> v, x -> x);
        drawLegend(g, data);

        g.dispose();
        ImageIO.write(img, "png", file.toFile());
    }

    private static void writeEfficiencyChart(Path file,
                                             List<Benchmark.Result> results,
                                             double t1) throws IOException {

        Map<String, TreeMap<Integer, Double>> data =
                group(results, r -> (t1 / r.medianSeconds()) / r.threads());

        BufferedImage img = newCanvas("Эффективность E_p = (S_p / p) × 100%");
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        List<Integer> threads = sortedThreads(data);
        double maxX = threads.isEmpty() ? 1 : threads.get(threads.size() - 1);
        double maxY = 1.0;

        drawAxes(g, maxX, maxY, "потоков", "E_p");
        drawLines(g, data, maxX, maxY, v -> v, x -> x);
        drawLegend(g, data);

        g.dispose();
        ImageIO.write(img, "png", file.toFile());
    }

    private static void drawLines(
            Graphics2D g,
            Map<String, TreeMap<Integer, Double>> data,
            double maxX, double maxY,
            java.util.function.DoubleUnaryOperator fx,
            java.util.function.DoubleUnaryOperator fy) {

        int x0 = PAD_L;
        int y0 = H - PAD_B;
        int x1 = W - PAD_R;
        int y1 = PAD_T;

        int i = 0;
        for (Map.Entry<String, TreeMap<Integer, Double>> e : data.entrySet()) {
            g.setColor(PALETTE[i % PALETTE.length]);
            g.setStroke(new BasicStroke(2f));

            int prevPx = -1, prevPy = -1;
            for (Map.Entry<Integer, Double> pt : e.getValue().entrySet()) {
                int px = (int) mapX(pt.getKey(), maxX, x0, x1);
                int py = (int) mapY(pt.getValue(), maxY, y0, y1);
                if (prevPx >= 0) {
                    g.drawLine(prevPx, prevPy, px, py);
                }
                prevPx = px;
                prevPy = py;
            }

            // точки
            for (Map.Entry<Integer, Double> pt : e.getValue().entrySet()) {
                int px = (int) mapX(pt.getKey(), maxX, x0, x1);
                int py = (int) mapY(pt.getValue(), maxY, y0, y1);
                g.fillOval(px - 4, py - 4, 8, 8);
            }
            i++;
        }
    }
}