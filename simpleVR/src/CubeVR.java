package simple;

import jrtr.*;
import jrtr.glrenderer.*;

import javax.swing.*;
import java.awt.event.*;
import javax.vecmath.*;
import java.lang.reflect.Array;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Implements a simple application that opens a 3D rendering window and
 * shows a rotating cube.
 */
public class CubeVR
{	static float[][] landscape = new float[(int) Math.pow(2,3)+1][(int) Math.pow(2,3)+1];

    static RenderPanel renderPanel;
    static RenderContext renderContext;
    static Shader normalShader;
    static Shader diffuseShader;
    static Material material, frontMaterial, backMaterial, ceilingMaterial, floorMaterial, rightMaterial, leftMaterial;
    static SimpleSceneManager sceneManager;
    static Shape shape, frontShape, backShape,ceilingShape,floorShape,rightShape,leftShape;
    static float currentstep, basicstep;
    static float roomSize  =2;

    /**
     * An extension of {@link GLRenderPanel} or {@link SWRenderPanel} to
     * provide a call-back function for initialization. Here we construct
     * a simple 3D scene and start a timer task to generate an animation.
     */
    public final static class SimpleRenderPanel extends GLRenderPanel
    {
        /**
         * Initialization call-back. We initialize our renderer here.
         *
         * @param r	the render context that is associated with this render panel
         */
        public void init(RenderContext r) {
            renderContext = r;

            // Make a simple geometric object: a cube

            makeCubeShape();


            // Make a scene manager and add the object
            sceneManager = new SimpleSceneManager();
            sceneManager.addShape(backShape);
            sceneManager.addShape(frontShape);
            sceneManager.addShape(leftShape);
            sceneManager.addShape(rightShape);
            sceneManager.addShape(floorShape);
            sceneManager.addShape(ceilingShape);

            // Add the scene to the renderer
            renderContext.setSceneManager(sceneManager);

            // Load some more shaders
            normalShader = renderContext.makeShader();
            try {
                normalShader.load("../jrtr/shaders/normal.vert", "../jrtr/shaders/normal.frag");
            } catch(Exception e) {
                System.out.print("Problem with shader:\n");
                System.out.print(e.getMessage());
            }

            diffuseShader = renderContext.makeShader();
            try {
                diffuseShader.load("../jrtr/shaders/ambient.vert", "../jrtr/shaders/ambient.frag");
            } catch(Exception e) {
                System.out.print("Problem with shader:\n");
                System.out.print(e.getMessage());
            }

            makeCubeMaterial();

            // Register a timer task
            Timer timer = new Timer();
            basicstep = 0.01f;
            currentstep = basicstep;
            timer.scheduleAtFixedRate(new AnimationTask(), 0, 10);
        }

        private void makeCubeMaterial() {
            // Front material
            floorMaterial = new Material();
            floorMaterial.shader = diffuseShader;
            floorMaterial.diffuseMap = renderContext.makeTexture();
            try {
                floorMaterial.diffuseMap.load("../textures/plant.jpg");
            } catch(Exception e) {
                System.out.print("Could not load texture.\n");
                System.out.print(e.getMessage());
            }
            floorShape.setMaterial(floorMaterial);

            ceilingMaterial = new Material();
            ceilingMaterial.shader = diffuseShader;
            ceilingMaterial.diffuseMap = renderContext.makeTexture();
            try {
                ceilingMaterial.diffuseMap.load("../textures/plant.jpg");
            } catch(Exception e) {
                System.out.print("Could not load texture.\n");
                System.out.print(e.getMessage());
            }
            ceilingShape.setMaterial(ceilingMaterial);

            rightMaterial = new Material();
            rightMaterial.shader = diffuseShader;
            rightMaterial.diffuseMap = renderContext.makeTexture();
            try {
                rightMaterial.diffuseMap.load("../textures/plant.jpg");
            } catch(Exception e) {
                System.out.print("Could not load texture.\n");
                System.out.print(e.getMessage());
            }
            rightShape.setMaterial(rightMaterial);

            leftMaterial = new Material();
            leftMaterial.shader = diffuseShader;
            leftMaterial.diffuseMap = renderContext.makeTexture();
            try {
                leftMaterial.diffuseMap.load("../textures/plant.jpg");
            } catch(Exception e) {
                System.out.print("Could not load texture.\n");
                System.out.print(e.getMessage());
            }
            leftShape.setMaterial(leftMaterial);

            rightMaterial = new Material();
            rightMaterial.shader = diffuseShader;
            rightMaterial.diffuseMap = renderContext.makeTexture();
            try {
                rightMaterial.diffuseMap.load("../textures/plant.jpg");
            } catch(Exception e) {
                System.out.print("Could not load texture.\n");
                System.out.print(e.getMessage());
            }
            rightShape.setMaterial(rightMaterial);

            frontMaterial = new Material();
            frontMaterial.shader = diffuseShader;
            frontMaterial.diffuseMap = renderContext.makeTexture();
            try {
                frontMaterial.diffuseMap.load("../textures/plant.jpg");
            } catch(Exception e) {
                System.out.print("Could not load texture.\n");
                System.out.print(e.getMessage());
            }
            frontShape.setMaterial(frontMaterial);

            backMaterial = new Material();
            backMaterial.shader = diffuseShader;
            backMaterial.diffuseMap = renderContext.makeTexture();
            try {
                backMaterial.diffuseMap.load("../textures/glurak.jpg");
            } catch(Exception e) {
                System.out.print("Could not load texture.\n");
                System.out.print(e.getMessage());
            }
            backShape.setMaterial(backMaterial);

        }

        private void makeCubeShape() {
            //The vertices
            float[] vfront = {-1, -1, 1, 1, -1, 1, 1, 1, 1, -1, 1, 1};
            float[] vleft = {-1, -1, -1, -1, -1, 1, -1, 1, 1, -1, 1, -1};
            float[] vback = {1, -1, -1, -1, -1, -1, -1, 1, -1, 1, 1, -1};
            float[] vright = {1, -1, 1, 1, -1, -1, 1, 1, -1, 1, 1, 1};
            float[] vtop = {1, 1, 1, 1, 1, -1, -1, 1, -1, -1, 1, 1};
            float[] vbottom={ -1, -1, 1, -1, -1, -1, 1, -1, -1, 1, -1, 1};

            // The vertex colors
            float[] cfront = {0.5f, 0, 0, 0.5f, 0, 0, 0.5f, 0, 0, 0.5f, 0, 0};
            float[] cleft = {0, 0.3f, 0, 0, 0.3f, 0, 0, 0.3f, 0, 0, 0.3f, 0};
            float[] cback = {0.5f, 0, 0, 0.5f, 0, 0, 0.5f, 0, 0, 0.5f, 0, 0};
            float[] cright = {0, 0.3f, 0, 0, 0.3f, 0, 0, 0.3f, 0, 0, 0.3f, 0,};
            float[] ctop = {0, 0.3f, 0.3f, 0, 0.3f, 0.3f, 0, 0.3f, 0.3f, 0, 0.3f, 0.3f};
            float[] cbottom = {0, 0.3f, 0.3f, 0, 0.3f, 0.3f, 0, 0.3f,0.3f, 0, 0.3f, 0.3f, };

            // The vertex normals
            float[] nfront = {0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1};
            float[] nleft = {-1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0};
            float[] nback = { 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1};
            float[] nright = {1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0};
            float[] ntop = {0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0};
            float[] nbottom = {0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0 };

            // Texture coordinates
            float[] uvfront ={0,0,1,0,1, 1, 0, 1};
            float[] uvleft = { 0, 0, 1, 0,1, 1, 0, 1};
            float[] uvback = { 0, 0, 1, 0, 1, 1, 0, 1};
            float[] uvright = {0, 0, 1, 0, 1, 1, 0, 1};
            float[] uvtop = {0, 0, 1, 0, 1, 1, 0, 1};
            float[] uvbottom = {0, 0, 1, 0, 1, 1, 0, 1};

            //Front transformations
            for (int i = 0; i < Array.getLength(vfront); i++)
                vfront[i] = vfront[i] * (roomSize);
            for (int i = 0; i < Array.getLength(nfront); i++)
                nfront[i] = nfront[i] * -1.f;

            //Back transformations
            for (int i = 0; i < Array.getLength(vback); i++)
                vback[i] = vback[i] * (roomSize);
            for (int i = 0; i < Array.getLength(nback); i++)
                nback[i] = nback[i] * -1.f;

            //Right transformations
            for (int i = 0; i < Array.getLength(vright); i++)
                vright[i] = vright[i] * (roomSize);
            for (int i = 0; i < Array.getLength(nright); i++)
                nright[i] = nright[i] * -1.f;

            //left transformations
            for (int i = 0; i < Array.getLength(vleft); i++)
                vleft[i] = vleft[i] * (roomSize);
            for (int i = 0; i < Array.getLength(nleft); i++)
                nleft[i] = nleft[i] * -1.f;

            //floor transformation
            for (int i = 0; i < Array.getLength(vbottom); i++)
                vbottom[i] = vbottom[i] * (roomSize);
            for (int i = 0; i < Array.getLength(nbottom); i++)
                nbottom[i] = nbottom[i] * -1.f;

            //ceiling transformations
            for (int i = 0; i < Array.getLength(vtop); i++)
                vtop[i] = vtop[i] * (roomSize);
            for (int i = 0; i < Array.getLength(ntop); i++)
                ntop[i] = ntop[i] * -1.f;


            for (int i = 0; i < Array.getLength(ctop); i++)
                ctop[i] = ctop[i] * 0.5f;

            for (int i = 0; i < Array.getLength(cleft); i++)
                cleft[i] = cleft[i] * 0.5f;

            for (int i = 0; i < Array.getLength(cright); i++)
                cright[i] = cright[i] * 0.5f;

            for (int i = 0; i < Array.getLength(cbottom); i++)
                cbottom[i] = cbottom[i] * 0.5f;

            for (int i = 0; i < Array.getLength(cback); i++)
                cback[i] = cback[i] * 0.5f;

            for (int i = 0; i < Array.getLength(cfront); i++)
                cfront[i] = cfront[i] * 0.5f;


            // Construct a data structure that stores the vertices, their
            // attributes, and the triangle mesh connectivity


            int[] indicesfront ={ 0, 2, 3, 0, 1, 2};
            int[] indicesleft = {0, 2, 3, 0, 1, 2};
            int[] indicesright = {12, 14, 15, 12, 13, 14};
            int[] indicesback = {8, 10, 11, 8, 9, 10};
            int[] indicestop = {16, 18, 19, 16, 17, 18};
            int[] indicesbottom = {20, 22, 23, 20, 21, 22 };



            // The triangles (three vertex indices for each triangle)
            int indices[] = { 0,2,3,0,1,2};

            //Front
            VertexData vertexData = renderContext.makeVertexData(4);
            vertexData.addElement(cfront, VertexData.Semantic.COLOR, 3);
            vertexData.addElement(vfront, VertexData.Semantic.POSITION, 3);
            vertexData.addElement(nfront, VertexData.Semantic.NORMAL, 3);
            vertexData.addElement(uvfront, VertexData.Semantic.TEXCOORD, 2);
            vertexData.addIndices(indices);
            frontShape = new Shape(vertexData);

            //Left
            vertexData = renderContext.makeVertexData(4);
            vertexData.addElement(cleft, VertexData.Semantic.COLOR, 3);
            vertexData.addElement(vleft, VertexData.Semantic.POSITION, 3);
            vertexData.addElement(nleft, VertexData.Semantic.NORMAL, 3);
            vertexData.addElement(uvleft, VertexData.Semantic.TEXCOORD, 2);
            vertexData.addIndices(indices);
            leftShape = new Shape(vertexData);

            //Back
            vertexData = renderContext.makeVertexData(4);
            vertexData.addElement(cback, VertexData.Semantic.COLOR, 3);
            vertexData.addElement(vback, VertexData.Semantic.POSITION, 3);
            vertexData.addElement(nback, VertexData.Semantic.NORMAL, 3);
            vertexData.addElement(uvback, VertexData.Semantic.TEXCOORD, 2);
            vertexData.addIndices(indices);
            backShape = new Shape(vertexData);

            //Right
            vertexData = renderContext.makeVertexData(4);
            vertexData.addElement(cright, VertexData.Semantic.COLOR, 3);
            vertexData.addElement(vright, VertexData.Semantic.POSITION, 3);
            vertexData.addElement(nright, VertexData.Semantic.NORMAL, 3);
            vertexData.addElement(uvright, VertexData.Semantic.TEXCOORD, 2);
            vertexData.addIndices(indices);
            rightShape = new Shape(vertexData);

            //Top
            vertexData = renderContext.makeVertexData(4);
            vertexData.addElement(ctop, VertexData.Semantic.COLOR, 3);
            vertexData.addElement(vtop, VertexData.Semantic.POSITION, 3);
            vertexData.addElement(ntop, VertexData.Semantic.NORMAL, 3);
            vertexData.addElement(uvtop, VertexData.Semantic.TEXCOORD, 2);
            vertexData.addIndices(indices);
            ceilingShape = new Shape(vertexData);

            //Floor
            vertexData = renderContext.makeVertexData(4);
            vertexData.addElement(cbottom, VertexData.Semantic.COLOR, 3);
            vertexData.addElement(vbottom, VertexData.Semantic.POSITION, 3);
            vertexData.addElement(nbottom, VertexData.Semantic.NORMAL, 3);
            vertexData.addElement(uvbottom, VertexData.Semantic.TEXCOORD, 2);
            vertexData.addIndices(indices);
            floorShape = new Shape(vertexData);

        }
    }

    /**
     * A timer task that generates an animation. This task triggers
     * the redrawing of the 3D scene every time it is executed.
     */
    public static class AnimationTask extends TimerTask
    {
        public void run()
        {
            // Update transformation by rotating with angle "currentstep"
            //Front
            Matrix4f t = frontShape.getTransformation();
            Matrix4f rotX = new Matrix4f();
            rotX.rotX(currentstep);
            Matrix4f rotY = new Matrix4f();
            rotY.rotY(currentstep);
            t.mul(rotX);
            t.mul(rotY);
            frontShape.setTransformation(t);

            t = backShape.getTransformation();
            rotX = new Matrix4f();
            rotX.rotX(currentstep);
            rotY = new Matrix4f();
            rotY.rotY(currentstep);
            t.mul(rotX);
            t.mul(rotY);
            backShape.setTransformation(t);

            t = leftShape.getTransformation();
            rotX = new Matrix4f();
            rotX.rotX(currentstep);
            rotY = new Matrix4f();
            rotY.rotY(currentstep);
            t.mul(rotX);
            t.mul(rotY);
            leftShape.setTransformation(t);

            t = rightShape.getTransformation();
            rotX = new Matrix4f();
            rotX.rotX(currentstep);
            rotY = new Matrix4f();
            rotY.rotY(currentstep);
            t.mul(rotX);
            t.mul(rotY);
            rightShape.setTransformation(t);

            t = floorShape.getTransformation();
            rotX = new Matrix4f();
            rotX.rotX(currentstep);
            rotY = new Matrix4f();
            rotY.rotY(currentstep);
            t.mul(rotX);
            t.mul(rotY);
            floorShape.setTransformation(t);

            t = ceilingShape.getTransformation();
            rotX = new Matrix4f();
            rotX.rotX(currentstep);
            rotY = new Matrix4f();
            rotY.rotY(currentstep);
            t.mul(rotX);
            t.mul(rotY);
            ceilingShape.setTransformation(t);


            // Trigger redrawing of the render window
            renderPanel.getCanvas().repaint();
        }
    }

    /**
     * A mouse listener for the main window of this application. This can be
     * used to process mouse events.
     */
    public static class SimpleMouseListener implements MouseListener
    {
        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}
        public void mouseClicked(MouseEvent e) {}
    }

    /**
     * A key listener for the main window. Use this to process key events.
     * Currently this provides the following controls:
     * 's': stop animation
     * 'p': play animation
     * '+': accelerate rotation
     * '-': slow down rotation
     * 'd': default shader
     * 'n': shader using surface normals
     * 'm': use a material for shading
     */
    public static class SimpleKeyListener implements KeyListener
    {
        public void keyPressed(KeyEvent e)
        {
            switch(e.getKeyChar())
            {
                case 's': {
                    // Stop animation
                    currentstep = 0;
                    break;
                }
                case 'p': {
                    // Resume animation
                    currentstep = basicstep;
                    break;
                }
                case '+': {
                    // Accelerate roation
                    currentstep += basicstep;
                    break;
                }
                case '-': {
                    // Slow down rotation
                    currentstep -= basicstep;
                    break;
                }
                case 'n': {
                    // Remove material from cylindershape, and set "normal" shader
                    shape.setMaterial(null);
                    renderContext.useShader(normalShader);
                    break;
                }
                case 'd': {
                    // Remove material from cylindershape, and set "default" shader
                    shape.setMaterial(null);
                    renderContext.useDefaultShader();
                    break;
                }
                case 'm': {
                    // Set a material for more complex shading of the cylindershape
                    if(shape.getMaterial() == null) {
                        shape.setMaterial(material);
                    } else
                    {
                        shape.setMaterial(null);
                        renderContext.useDefaultShader();
                    }
                    break;
                }
            }

            // Trigger redrawing
            renderPanel.getCanvas().repaint();
        }

        public void keyReleased(KeyEvent e)
        {
        }

        public void keyTyped(KeyEvent e)
        {
        }

    }

    /**
     * The main function opens a 3D rendering window, implemented by the class
     * {@link SimpleRenderPanel}. {@link SimpleRenderPanel} is then called backed
     * for initialization automatically. It then constructs a simple 3D scene,
     * and starts a timer task to generate an animation.
     */
    public static void main(String[] args)
    {
        // Make a render panel. The init function of the renderPanel
        // (see above) will be called back for initialization.
        renderPanel = new SimpleRenderPanel();

        // Make the main window of this application and add the renderer to it
        JFrame jframe = new JFrame("simple");
        jframe.setSize(500, 500);
        jframe.setLocationRelativeTo(null); // toruscenter of screen
        jframe.getContentPane().add(renderPanel.getCanvas());// put the canvas into a JFrame window

        // Add a mouse and key listener
        renderPanel.getCanvas().addMouseListener(new SimpleMouseListener());
        renderPanel.getCanvas().addKeyListener(new SimpleKeyListener());
        renderPanel.getCanvas().setFocusable(true);

        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jframe.setVisible(true); // show window
    }





    }
