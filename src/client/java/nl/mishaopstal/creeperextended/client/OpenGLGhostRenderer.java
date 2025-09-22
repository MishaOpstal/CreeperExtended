package nl.mishaopstal.creeperextended.client;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Direct OpenGL Ghost Renderer - bypasses Minecraft's rendering system entirely
 *
 * This creates a chromatic aberration/ghosting effect by:
 * 1. Capturing the current framebuffer
 * 2. Creating multiple offset copies with color channel separation
 * 3. Blending them back onto the screen
 */
public class OpenGLGhostRenderer {
    // Use OpenGL 3.2 compatible shaders instead of 3.3 core
    private static final String VERTEX_SHADER_SOURCE = """
        #version 150
        in vec2 aPos;
        in vec2 aTexCoord;

        out vec2 TexCoord;

        uniform vec2 offset;

        void main() {
            gl_Position = vec4(aPos + offset, 0.0, 1.0);
            TexCoord = aTexCoord;
        }
        """;

    private static final String FRAGMENT_SHADER_SOURCE = """
        #version 150
        out vec4 FragColor;

        in vec2 TexCoord;
        uniform sampler2D screenTexture;
        uniform vec3 colorMask;
        uniform float alpha;

        void main() {
            vec4 texColor = texture(screenTexture, TexCoord);
            FragColor = vec4(texColor.rgb * colorMask, texColor.a * alpha);
        }
        """;

    // OpenGL resources (use 0 as 'not created' for LWJGL)
    private int shaderProgram = 0;
    private int VAO = 0;
    private int VBO = 0;
    private int EBO = 0;
    private int captureTexture = 0;

    // Uniforms
    private int offsetUniform = -1;
    private int colorMaskUniform = -1;
    private int alphaUniform = -1;
    private int screenTextureUniform = -1;

    // Configuration
    private boolean enabled = true;
    private boolean initialized = false;

    // Layer settings: [red, green, blue]
    private final float[] layerAlphas = {0.6f, 0.55f, 0.4f};
    private final float[] offsetsX = {-8.0f, 8.0f, 0.0f};
    private final float[] offsetsY = {0.0f, 0.0f, 6.0f};
    private final float[][] colorMasks = {
            {1.1f, 0.92f, 0.92f}, // Red layer - boost red, reduce others
            {0.92f, 1.1f, 0.92f}, // Green layer - boost green
            {0.92f, 0.92f, 1.1f}  // Blue layer - boost blue
    };

    // Screen dimensions
    private int screenWidth = 0;
    private int screenHeight = 0;

    public OpenGLGhostRenderer() {}

    /**
     * Initialize the renderer. This method will safely return false if there's no OpenGL context yet.
     * Call it again later from a proper render/context thread when the context exists.
     */
    public boolean initialize() {
        if (initialized) return true;

        // IMPORTANT: don't call any native GL functions unless we have a context.
        // GL.getCapabilities() returns null when no context exists (or no capabilities were created).
        if (GL.getCapabilities() == null) {
            System.err.println("OpenGL context not available yet; skipping ghost renderer initialization.");
            return false;
        }

        try {
            // Now it's safe to query GL strings
            String version = GL11.glGetString(GL11.GL_VERSION);
            if (version == null || version.isEmpty()) {
                System.err.println("No OpenGL version string (context might be invalid).");
                return false;
            }

            System.out.println("Initializing Ghost Renderer with OpenGL version: " + version);

            // Create and compile shaders
            int vertexShader = createShader(GL20.GL_VERTEX_SHADER, VERTEX_SHADER_SOURCE);
            int fragmentShader = createShader(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_SOURCE);

            if (vertexShader == 0 || fragmentShader == 0) {
                System.err.println("Failed to create shaders");
                return false;
            }

            // Create shader program
            shaderProgram = GL20.glCreateProgram();
            if (shaderProgram == 0) {
                System.err.println("Failed to create shader program");
                GL20.glDeleteShader(vertexShader);
                GL20.glDeleteShader(fragmentShader);
                return false;
            }

            GL20.glAttachShader(shaderProgram, vertexShader);
            GL20.glAttachShader(shaderProgram, fragmentShader);
            GL20.glLinkProgram(shaderProgram);

            // Check for linking errors
            if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                String error = GL20.glGetProgramInfoLog(shaderProgram);
                System.err.println("Shader program linking failed: " + error);
                GL20.glDeleteShader(vertexShader);
                GL20.glDeleteShader(fragmentShader);
                GL20.glDeleteProgram(shaderProgram);
                shaderProgram = 0;
                return false;
            }

            // Clean up individual shaders
            GL20.glDeleteShader(vertexShader);
            GL20.glDeleteShader(fragmentShader);

            // Get uniform locations
            offsetUniform = GL20.glGetUniformLocation(shaderProgram, "offset");
            colorMaskUniform = GL20.glGetUniformLocation(shaderProgram, "colorMask");
            alphaUniform = GL20.glGetUniformLocation(shaderProgram, "alpha");
            screenTextureUniform = GL20.glGetUniformLocation(shaderProgram, "screenTexture");

            if (offsetUniform == -1 || colorMaskUniform == -1 || alphaUniform == -1 || screenTextureUniform == -1) {
                System.err.println("Failed to locate shader uniforms");
                GL20.glDeleteProgram(shaderProgram);
                shaderProgram = 0;
                return false;
            }

            // Create quad geometry (full screen quad)
            float[] vertices = {
                    // positions   // texture coords
                    -1.0f,  1.0f,  0.0f, 1.0f, // top left
                    -1.0f, -1.0f,  0.0f, 0.0f, // bottom left
                    1.0f, -1.0f,  1.0f, 0.0f, // bottom right
                    1.0f,  1.0f,  1.0f, 1.0f  // top right
            };

            int[] indices = {
                    0, 1, 2,  // first triangle
                    0, 2, 3   // second triangle
            };

            // Create VAO, VBO, EBO
            VAO = GL30.glGenVertexArrays();
            VBO = GL15.glGenBuffers();
            EBO = GL15.glGenBuffers();

            if (VAO == 0 || VBO == 0 || EBO == 0) {
                System.err.println("Failed to generate OpenGL buffers");
                cleanup();
                return false;
            }

            GL30.glBindVertexArray(VAO);

            // Upload vertex data
            FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
            vertexBuffer.put(vertices).flip();

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, VBO);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

            // Upload index data
            IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
            indexBuffer.put(indices).flip();

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, EBO);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);

            // Configure vertex attributes
            final int stride = 4 * Float.BYTES;
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, stride, 0L);
            GL20.glEnableVertexAttribArray(0);

            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 2L * Float.BYTES);
            GL20.glEnableVertexAttribArray(1);

            GL30.glBindVertexArray(0);

            // Check for any OpenGL errors during setup
            int error = GL11.glGetError();
            if (error != GL11.GL_NO_ERROR) {
                System.err.println("OpenGL error during initialization: " + error);
                cleanup();
                return false;
            }

            initialized = true;
            System.out.println("Ghost renderer initialized successfully");
            return true;

        } catch (Exception e) {
            System.err.println("Exception during ghost renderer initialization: " + e.getMessage());
            e.printStackTrace();
            cleanup();
            return false;
        }
    }

    private int createShader(int type, String source) {
        try {
            int shader = GL20.glCreateShader(type);
            if (shader == 0) {
                System.err.println("Failed to create shader of type: " + type);
                return 0;
            }

            GL20.glShaderSource(shader, source);
            GL20.glCompileShader(shader);

            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                String error = GL20.glGetShaderInfoLog(shader);
                System.err.println("Shader compilation failed (" +
                        (type == GL20.GL_VERTEX_SHADER ? "vertex" : "fragment") +
                        "): " + error);
                GL20.glDeleteShader(shader);
                return 0;
            }

            return shader;
        } catch (Exception e) {
            System.err.println("Exception creating shader: " + e.getMessage());
            return 0;
        }
    }

    public void updateScreenSize(int width, int height) {
        if (width == screenWidth && height == screenHeight) return;
        if (width <= 0 || height <= 0) return;

        screenWidth = width;
        screenHeight = height;

        // Recreate capture texture with new dimensions
        createCaptureTexture();
    }

    private void createCaptureTexture() {
        if (screenWidth <= 0 || screenHeight <= 0) return;

        try {
            // Delete old texture if exists
            if (captureTexture > 0) {
                GL11.glDeleteTextures(captureTexture);
                captureTexture = 0;
            }

            // Create new texture
            captureTexture = GL11.glGenTextures();
            if (captureTexture == 0) {
                System.err.println("Failed to generate capture texture");
                return;
            }

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, captureTexture);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, screenWidth, screenHeight,
                    0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        } catch (Exception e) {
            System.err.println("Exception creating capture texture: " + e.getMessage());
        }
    }

    public void render() {
        if (!enabled || !initialized || shaderProgram == 0) return;

        try {
            // Get current viewport to determine screen size
            IntBuffer viewport = BufferUtils.createIntBuffer(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            int currentWidth = viewport.get(2);
            int currentHeight = viewport.get(3);

            updateScreenSize(currentWidth, currentHeight);

            if (captureTexture == 0 || currentWidth <= 0 || currentHeight <= 0) return;

            // Step 1: Capture current framebuffer to texture
            captureFramebuffer();

            // Step 2: Enable blending for ghost effect
            boolean blendingWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
            if (!blendingWasEnabled) {
                GL11.glEnable(GL11.GL_BLEND);
            }
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            // Step 3: Use our shader program
            GL20.glUseProgram(shaderProgram);

            // Bind our texture and VAO
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, captureTexture);
            GL20.glUniform1i(screenTextureUniform, 0);
            GL30.glBindVertexArray(VAO);

            // Step 4: Draw ghost layers
            for (int i = 0; i < 3; i++) {
                if (layerAlphas[i] <= 0.0f) continue;

                // Convert pixel offsets to normalized coordinates
                float normalizedOffsetX = (offsetsX[i] * 2.0f) / currentWidth;
                float normalizedOffsetY = (offsetsY[i] * 2.0f) / currentHeight;

                // Set uniforms for this layer
                GL20.glUniform2f(offsetUniform, normalizedOffsetX, -normalizedOffsetY); // Flip Y
                GL20.glUniform3f(colorMaskUniform, colorMasks[i][0], colorMasks[i][1], colorMasks[i][2]);
                GL20.glUniform1f(alphaUniform, layerAlphas[i]);

                // Draw the quad
                GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);
            }

            // Step 5: Cleanup
            GL30.glBindVertexArray(0);
            GL20.glUseProgram(0);

            // Restore blending state
            if (!blendingWasEnabled) {
                GL11.glDisable(GL11.GL_BLEND);
            }

        } catch (Exception e) {
            System.err.println("Exception during render: " + e.getMessage());
            // Don't cleanup on render errors, just skip this frame
        }
    }

    private void captureFramebuffer() {
        try {
            if (captureTexture <= 0) return;
            // Copy current framebuffer content to our texture
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, captureTexture);
            GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, 0, 0, screenWidth, screenHeight, 0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        } catch (Exception e) {
            System.err.println("Exception capturing framebuffer: " + e.getMessage());
        }
    }

    // Configuration methods
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setLayerAlphas(float red, float green, float blue) {
        this.layerAlphas[0] = Math.max(0.0f, Math.min(1.0f, red));
        this.layerAlphas[1] = Math.max(0.0f, Math.min(1.0f, green));
        this.layerAlphas[2] = Math.max(0.0f, Math.min(1.0f, blue));
    }

    public void setLayerOffsets(float redX, float greenX, float blueX,
                                float redY, float greenY, float blueY) {
        this.offsetsX[0] = redX;
        this.offsetsX[1] = greenX;
        this.offsetsX[2] = blueX;
        this.offsetsY[0] = redY;
        this.offsetsY[1] = greenY;
        this.offsetsY[2] = blueY;
    }

    public void setColorMasks(float[] redMask, float[] greenMask, float[] blueMask) {
        if (redMask.length >= 3) System.arraycopy(redMask, 0, colorMasks[0], 0, 3);
        if (greenMask.length >= 3) System.arraycopy(greenMask, 0, colorMasks[1], 0, 3);
        if (blueMask.length >= 3) System.arraycopy(blueMask, 0, colorMasks[2], 0, 3);
    }

    public void cleanup() {
        try {
            if (shaderProgram > 0) {
                GL20.glDeleteProgram(shaderProgram);
                shaderProgram = 0;
            }

            if (VAO > 0) {
                GL30.glDeleteVertexArrays(VAO);
                VAO = 0;
            }

            if (VBO > 0) {
                GL15.glDeleteBuffers(VBO);
                VBO = 0;
            }

            if (EBO > 0) {
                GL15.glDeleteBuffers(EBO);
                EBO = 0;
            }

            if (captureTexture > 0) {
                GL11.glDeleteTextures(captureTexture);
                captureTexture = 0;
            }

            initialized = false;
            System.out.println("Ghost renderer cleaned up");
        } catch (Exception e) {
            System.err.println("Exception during cleanup: " + e.getMessage());
        }
    }
}
