package imgui;

import glwrapper.Texture;

/**
 * This class represent the Setting window
 * allowing user to change the emulator's behaviour
 */
public class PreviewLayer extends Layer {

    private Texture texture;

    /**
     * Create a new instance of the Layer
     */
    public PreviewLayer() {
        super();
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    /**
     * Render the layer to the screen
     * and propagate user inputs to the emulator
     */
    public void render() {
        ImGui.begin("Settings");
        ImGui.setWindowSize(1000, 1000);

        ImGui.end();
    }
}