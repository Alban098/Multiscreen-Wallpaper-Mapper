package utils;

public class Screen {

    private static int id_provider = 0;

    private int pixel_x;
    private int pixel_y;
    private float aspect_ratio;
    private float diagonal;
    private int id;

    public Screen(int pixel_x, int pixel_y, float diagonal) {
        this.pixel_x = pixel_x;
        this.pixel_y = pixel_y;
        this.aspect_ratio = (float)pixel_x/pixel_y;
        this.diagonal = diagonal;
        this.id = id_provider++;
    }

    public int getPixelX() {
        return pixel_x;
    }

    public int getPixelY() {
        return pixel_y;
    }

    public void setPixelX(int pixel_x) {
        this.pixel_x = pixel_x;
        this.aspect_ratio = (float)pixel_x/pixel_y;
    }

    public void setPixelY(int pixel_y) {
        this.pixel_y = pixel_y;
        this.aspect_ratio = (float)pixel_x/pixel_y;
    }

    public void setDiagonal(float diagonal) {
        this.diagonal = diagonal;
    }

    public float getSizeY() {
        return (float) (diagonal / (Math.sqrt(aspect_ratio * aspect_ratio + 1)));
    }

    public float getSizeX() {
        return (float) (diagonal * aspect_ratio / (Math.sqrt(aspect_ratio * aspect_ratio + 1)));
    }

    public float getDiagonal() {
        return diagonal;
    }

    public int getId() {
        return id;
    }
}
