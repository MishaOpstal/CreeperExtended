package nl.mishaopstal.creeperextended.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

/**
 * Integration wrapper for the OpenGL Ghost Renderer with Minecraft
 *
 * This class handles the timing and lifecycle of the ghost effect,
 * ensuring it renders at the right moment in Minecraft's render pipeline.
 */
@Environment(EnvType.CLIENT)
public class MinecraftGhostIntegration {
    public static final MinecraftGhostIntegration INSTANCE = new MinecraftGhostIntegration();

    private final OpenGLGhostRenderer ghostRenderer;
    private boolean started = false;
    private int renderDelayCounter = 0;
    private static final int RENDER_DELAY = 1; // Delay by 1 frame to avoid capturing our own effect

    // Performance settings
    private int frameSkipCounter = 0;
    private int frameSkipInterval = 0; // 0 = render every frame, 1 = every other frame, etc.

    private MinecraftGhostIntegration() {
        this.ghostRenderer = new OpenGLGhostRenderer();
    }

    public synchronized boolean start() {
        if (started) return true;

        // Initialize the OpenGL renderer
        if (!ghostRenderer.initialize()) {
            System.err.println("Failed to initialize OpenGL Ghost Renderer");
            return false;
        }

        // Register render callback - use HUD render as it happens after world rendering
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            onRender();
        });

        // Register tick event for frame counting
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            onTick();
        });

        started = true;
        System.out.println("Ghost renderer started successfully");
        return true;
    }

    public synchronized void stop() {
        if (!started) return;

        ghostRenderer.cleanup();
        started = false;
        System.out.println("Ghost renderer stopped");
    }

    private void onTick() {
        // Update any per-tick logic here if needed
        frameSkipCounter++;
    }

    private void onRender() {
        if (!started) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return;

        // Skip frames for performance if configured
        if (frameSkipInterval > 0 && frameSkipCounter % (frameSkipInterval + 1) != 0) {
            return;
        }

        // Delay rendering by a frame to avoid capturing our own effect
        if (renderDelayCounter < RENDER_DELAY) {
            renderDelayCounter++;
            return;
        }

        try {
            // Save current OpenGL state
            boolean depthTestEnabled = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
            boolean cullFaceEnabled = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE);

            // Disable depth testing and face culling for our overlay
            if (depthTestEnabled) {
                org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
            }
            if (cullFaceEnabled) {
                org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
            }

            // Render the ghost effect
            ghostRenderer.render();

            // Restore OpenGL state
            if (depthTestEnabled) {
                org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
            }
            if (cullFaceEnabled) {
                org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
            }

        } catch (Exception e) {
            System.err.println("Error in ghost renderer: " + e.getMessage());
            e.printStackTrace();
            // Don't stop the renderer on single frame errors
        }
    }

    // Configuration methods that delegate to the OpenGL renderer
    public void setEnabled(boolean enabled) {
        ghostRenderer.setEnabled(enabled);
        if (enabled) {
            renderDelayCounter = 0; // Reset delay when re-enabling
        }
    }

    public void setLayerAlphas(float red, float green, float blue) {
        ghostRenderer.setLayerAlphas(red, green, blue);
    }

    public void setLayerOffsets(float redX, float greenX, float blueX,
                                float redY, float greenY, float blueY) {
        ghostRenderer.setLayerOffsets(redX, greenX, blueX, redY, greenY, blueY);
    }

    public void setColorMasks(float[] redMask, float[] greenMask, float[] blueMask) {
        ghostRenderer.setColorMasks(redMask, greenMask, blueMask);
    }

    /**
     * Set frame skip interval for performance tuning
     * @param interval 0 = render every frame, 1 = every other frame, 2 = every 3rd frame, etc.
     */
    public void setFrameSkipInterval(int interval) {
        this.frameSkipInterval = Math.max(0, interval);
    }

    /**
     * Get current frame skip interval
     */
    public int getFrameSkipInterval() {
        return this.frameSkipInterval;
    }

    /**
     * Check if the renderer is properly initialized and running
     */
    public boolean isRunning() {
        return started;
    }

    /**
     * Convenience method to set up a classic chromatic aberration effect
     */
    public void setupChromaticAberration(float intensity) {
        intensity = Math.max(0.1f, Math.min(2.0f, intensity)); // Clamp between 0.1 and 2.0

        // Set offsets based on intensity
        float baseOffset = 8.0f * intensity;
        setLayerOffsets(
                -baseOffset, baseOffset, 0.0f,  // X offsets: red left, green right, blue center
                0.0f, 0.0f, baseOffset * 0.75f   // Y offsets: red/green same, blue slightly down
        );

        // Set alphas based on intensity
        float baseAlpha = 0.6f * intensity;
        setLayerAlphas(
                baseAlpha * 1.0f,   // Red
                baseAlpha * 0.9f,   // Green (slightly less)
                baseAlpha * 0.7f    // Blue (least)
        );

        // Enhanced color separation for chromatic effect
        setColorMasks(
                new float[]{1.2f, 0.8f, 0.8f}, // Red layer: boost red, reduce others
                new float[]{0.8f, 1.2f, 0.8f}, // Green layer: boost green
                new float[]{0.8f, 0.8f, 1.2f}  // Blue layer: boost blue
        );
    }

    /**
     * Convenience method to set up a retro VHS/glitch effect
     */
    public void setupVHSGlitch(float intensity) {
        intensity = Math.max(0.1f, Math.min(2.0f, intensity));

        // Asymmetric offsets for glitch effect
        float baseOffset = 12.0f * intensity;
        setLayerOffsets(
                -baseOffset * 1.5f, baseOffset * 0.5f, baseOffset * 2.0f,  // X: more dramatic
                baseOffset * 0.3f, -baseOffset * 0.2f, baseOffset * 0.8f   // Y: vertical separation
        );

        // Higher contrast alphas
        float baseAlpha = 0.7f * intensity;
        setLayerAlphas(
                baseAlpha * 1.2f,   // Red: strongest
                baseAlpha * 0.8f,   // Green: medium
                baseAlpha * 1.0f    // Blue: strong
        );

        // More extreme color separation
        setColorMasks(
                new float[]{1.4f, 0.6f, 0.7f}, // Red layer: very red-heavy
                new float[]{0.5f, 1.3f, 0.6f}, // Green layer: green-heavy
                new float[]{0.6f, 0.7f, 1.4f}  // Blue layer: very blue-heavy
        );
    }
}