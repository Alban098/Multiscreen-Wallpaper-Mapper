package imgui;

import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import glwrapper.Texture;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import utils.Screen;
import utils.Shape;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import java.util.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * This class represent the Main rendering thread
 * containing the OpenGl Context
 */
public class Window {

    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    private String glslVersion = null;
    private long windowPtr;

    private Texture screen_texture;

    private Shape shape;
    private final List<Screen> screens;
    private ImInt selectedMode = new ImInt(0);

    public Window() {
       screens = new ArrayList<>();
       shape = new Shape();
    }

    /**
     * Initialize the Window
     */
    public void init() {
        initWindow();
        initImGui();
        imGuiGlfw.init(windowPtr, true);
        imGuiGl3.init(glslVersion);
        screen_texture = new Texture(10,10);
    }

    /**
     * Clean the window and kill every Thread attached
     */
    public void destroy() {
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        screen_texture.cleanUp();
        ImGui.destroyContext();
        Callbacks.glfwFreeCallbacks(windowPtr);
        glfwDestroyWindow(windowPtr);
        glfwTerminate();
    }

    /**
     * Initialize OpenGl and GLFW
     */
    private void initWindow() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit() ) {
            System.exit(-1);
        }

        glslVersion = "#version 130";
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        windowPtr = glfwCreateWindow(1200, 810, "Wallpaper Mapper", NULL, NULL);

        if (windowPtr == NULL) {
            System.exit(-1);
        }

        glfwMakeContextCurrent(windowPtr);
        glfwSwapInterval(1);
        glfwShowWindow(windowPtr);
        GL.createCapabilities();
    }

    /**
     * Initialize ImGui and ImPlot
     */
    private void initImGui() {
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
    }

    /**
     * Execute the rendering Loop
     */
    public void run() {
        while (!glfwWindowShouldClose(windowPtr)) {
            //Clear the screen
            glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            //Start a new ImGui frame
            imGuiGlfw.newFrame();
            ImGui.newFrame();

            renderMainWindow();

            //Render the ImGui Frame
            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            //ImGui and GLFW standard call to render the frame
            if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
                final long backupWindowPtr = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
                ImGui.updatePlatformWindows();
                ImGui.renderPlatformWindowsDefault();
                GLFW.glfwMakeContextCurrent(backupWindowPtr);
            }
            GLFW.glfwSwapBuffers(windowPtr);
            GLFW.glfwPollEvents();
        }
    }

    private void renderMainWindow() {
        ImGui.begin("Mapper");
        if (ImGui.button("Preview")) {
            shape.init(screens, Shape.Mode.get(selectedMode.get()));
            shape.fill(screen_texture);
        }
        ImGui.sameLine();
        if (ImGui.button("Load Image")) {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "Images", "png", ".png", "jpg", ".jpg");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                try {
                    BufferedImage img = ImageIO.read(chooser.getSelectedFile());
                    shape.setInputImage(img);
                    shape.init(screens, Shape.Mode.get(selectedMode.get()));
                    screen_texture.cleanUp();
                    screen_texture = new Texture(img.getWidth(), img.getHeight());
                    shape.fill(screen_texture);
                } catch (Exception ignored) {}
            }
        }
        ImGui.sameLine();
        if (ImGui.button("Add Screen")) {
            Screen screen = new Screen(1920, 1080, 27);
            screens.add(screen);
            shape.init(screens, Shape.Mode.get(selectedMode.get()));
            shape.fill(screen_texture);
        }
        ImGui.sameLine();
        if (ImGui.button("Save")) {
            shape.init(screens, Shape.Mode.get(selectedMode.get()));
            shape.compute();
            shape.save();
            shape.fill(screen_texture);
        }
        ImGui.separator();
        if (ImGui.combo("Mode", selectedMode, Shape.Mode.getModes())) {
            shape.init(screens, Shape.Mode.get(selectedMode.get()));
            shape.fill(screen_texture);
        }
        ImGui.separator();
        for (Screen screen : screens) {
            ImInt resX = new ImInt(screen.getPixelX());
            ImInt resY = new ImInt(screen.getPixelY());
            if (ImGui.inputInt("V - Res. (" + screen.getId() + ")", resX, 10))
                screen.setPixelX(resX.get());
            if (ImGui.inputInt("H - Res. (" + screen.getId() + ")", resY, 10))
                screen.setPixelY(resY.get());
            ImFloat diag = new ImFloat(screen.getDiagonal());
            if (ImGui.inputFloat("Diagonal (" + screen.getId() + ")", diag, 1))
                screen.setDiagonal(diag.get());
            ImGui.separator();
        }

        float aspect = (float)screen_texture.getWidth() / screen_texture.getHeight();
        ImGui.image(screen_texture.getID(), ImGui.getWindowWidth() - 20, (ImGui.getWindowWidth() - 20) / aspect);
        ImGui.end();

    }
}