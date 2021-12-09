package utils;

import glwrapper.Texture;
import org.joml.Rectanglef;
import org.joml.Vector2f;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Shape {

    private final Map<Screen, Vector2f> corners;
    private final Map<Screen, BufferedImage> images;
    private final Map<Screen, Rectanglef> rectangles;

    private BufferedImage inputImage;
    private float aspect;
    private float width;
    private float height;

    public Shape() {
        corners = new HashMap<>();
        images = new HashMap<>();
        rectangles = new HashMap<>();
    }

    public void init(List<Screen> screens, Mode mode) {
        images.clear();
        corners.clear();
        float current_x = 0;
        for (Screen screen : screens)
            if (screen.getSizeY() > height)
                height = screen.getSizeY();
        for (Screen screen : screens) {
            float y = 0;
            switch(mode) {
                case CENTER -> corners.put(screen, new Vector2f(current_x, (height - screen.getSizeY()) / 2));
                case TOP -> corners.put(screen, new Vector2f(current_x, height - screen.getSizeY()));
                case BOTTOM -> corners.put(screen, new Vector2f(current_x, 0));
            }
            width = current_x + screen.getSizeX();
            current_x += screen.getSizeX();
        }
        for (Vector2f point : corners.values()) {
            point.x /= width;
            point.y /= height;
        }
        aspect = width/height;
    }

    public void compute() {
        if (inputImage != null) {
            float x_sample, y_sample;
            Vector2f offsets = calculateOffsets();
            for (Screen screen : corners.keySet()) {
                BufferedImage wallpaper = new BufferedImage(screen.getPixelX(), screen.getPixelY(), inputImage.getType());
                for (int x = 0; x < screen.getPixelX(); x++) {
                    float screen_percent_x = (float) x / screen.getPixelX();
                    float shape_percent_x = screen_percent_x * screen.getSizeX() / width + corners.get(screen).x;
                    x_sample = shape_percent_x * (inputImage.getWidth() - 2 * offsets.x) + offsets.x;
                    for (int y = 0; y < screen.getPixelY(); y++) {
                        float screen_percent_y = (float) y / screen.getPixelY();
                        float shape_percent_y = screen_percent_y * screen.getSizeY() / height + corners.get(screen).y;
                        y_sample = shape_percent_y * (inputImage.getHeight() - 2 * offsets.y) + offsets.y;
                        wallpaper.setRGB(x, wallpaper.getHeight() - 1 - y, inputImage.getRGB((int) x_sample, inputImage.getHeight() - 1 - (int) y_sample));
                    }
                }
                images.put(screen, wallpaper);
            }
        }
    }

    private Vector2f calculateOffsets() {
        float img_aspect = (float) inputImage.getWidth() / inputImage.getHeight();
        Vector2f offsets = new Vector2f();
        if (img_aspect > aspect) {
            offsets.y = 0;
            offsets.x = (inputImage.getWidth() - (inputImage.getHeight() * aspect)) / 2;
        } else {
            offsets.y = (inputImage.getHeight() - (inputImage.getWidth() / aspect)) / 2;
            offsets.x = 0;
        }
        return offsets;
    }

    public void setInputImage(BufferedImage inputImage) {
        this.inputImage = inputImage;
    }

    private void computeRectangles() {
        if (inputImage != null) {
            Vector2f offsets = calculateOffsets();
            for (Screen screen : corners.keySet()) {
                float percent_x_0 = corners.get(screen).x;
                float percent_x_1 = screen.getSizeX() / width + corners.get(screen).x;
                float x_0 = percent_x_0 * (inputImage.getWidth() - 2 * offsets.x) + offsets.x;
                float x_1 = percent_x_1 * (inputImage.getWidth() - 2 * offsets.x) + offsets.x;

                float percent_y_0 = corners.get(screen).y;
                float percent_y_1 = screen.getSizeY() / height + corners.get(screen).y;
                float y_0 = inputImage.getHeight() - 1 - (percent_y_0 * (inputImage.getHeight() - 2 * offsets.y) + offsets.y);
                float y_1 = inputImage.getHeight() - 1 - (percent_y_1 * (inputImage.getHeight() - 2 * offsets.y) + offsets.y);
                rectangles.put(screen, new Rectanglef(x_0, y_1, x_1, y_0));
            }
        }
    }

    public void fill(Texture texture) {
        computeRectangles();
        int size = 6;
        if (inputImage != null) {
            ByteBuffer buffer = BufferUtils.createByteBuffer(inputImage.getWidth() * inputImage.getHeight() * 4);
            for (int y = 0; y < inputImage.getHeight(); y++) {
                for (int x = 0; x < inputImage.getWidth(); x++) {
                    boolean override = false;
                    for (Rectanglef rect : rectangles.values()) {
                        if (x <= rect.maxX + size && x >= rect.minX - size && ((y >= rect.maxY - size && y <= rect.maxY + size) || (y >= rect.minY - size && y <= rect.minY + size)) || (y <= rect.maxY + size && y >= rect.minY - size && ((x >= rect.maxX - size && x <= rect.maxX + size) || (x >= rect.minX - size && x <= rect.minX + size)))) {
                            override = true;
                            break;
                        }
                    }
                    if (!override) {
                        int rgb = inputImage.getRGB(x, y);
                        buffer.put((byte) ((rgb & 0xFF0000) >> 16));
                        buffer.put((byte) ((rgb & 0x00FF00) >> 8));
                        buffer.put((byte) (rgb & 0x0000FF));
                    } else {
                        buffer.put((byte) 255);
                        buffer.put((byte) 0);
                        buffer.put((byte) 0);
                    }
                    buffer.put((byte) 255);
                }
            }
            buffer.flip();
            texture.load(buffer);
        }
    }

    public void save() {
        int index = 0;
        try {
            JFileChooser chooser = new JFileChooser();
            int returnVal = chooser.showSaveDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                for (BufferedImage image : images.values()) {
                    String name = chooser.getSelectedFile().getName().split("\\.")[0] + "_" + index++;
                    ImageIO.write(image, "png", new File(chooser.getSelectedFile().getParent() + "\\" + name + ".png"));
                }
            }
        } catch (IOException ignored) {}
    }

    public enum Mode {
        CENTER,
        TOP,
        BOTTOM;

        public static String[] getModes() {
            String[] modes = new String[values().length];
            for (int i = 0; i < values().length; i++)
                modes[i] = values()[i].name();
            return modes;
        }

        public static Mode get(int i) {
            return values()[i];
        }
    }
}
