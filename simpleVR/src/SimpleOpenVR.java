import jrtr.*;
import jrtr.glrenderer.*;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.swing.*;

import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import javax.vecmath.*;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Implements a simple VR application that renders to an HMD via OpenVR. OpenVR
 * functionality is provided by the {@link OpenVRRenderPanel}. Also demonstrates
 * tracking of a VR hand controller.
 */
public class SimpleOpenVR {
	static VRRenderPanel renderPanel;
	static RenderContext renderContext;
	static SimpleSceneManager sceneManager;

	// shapes
	static Shape ball;
	static Shape controllerCube;
	static Shape controllerCubeTriggered;
	static Shape surroundingCube;
	static Shape controllerRacket;
	static Shape textureShape;
	static Shape floor;
	static Shape ceiling;
	static Shape rightWall;
	static Shape leftWall;
	static Shape frontWall;
	static Shape backWall, frontShape, backShape,ceilingShape,floorShape,rightShape,leftShape;
	
	static BufferedImage scoreBackground;

	
	static Material material, ballMaterial, racketMaterial, handMaterial, textureMaterial;
	static Material frontMaterial, backMaterial, ceilingMaterial, floorMaterial, rightMaterial, leftMaterial;
    
	static Shader diffuseShader;

	// stores bounding box for racket. Useful for collision detection with ball.
	static Vector3f racketBoundsMax = new Vector3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
	static Vector3f racketBoundsMin = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);

	// scene-geometry parameters
	static float ballRadius = 0.15f;
	static float roomSize = 2.f;
	static float controllerSize = 0.185f;
	static float racketSize = 0.015f;
	static boolean touched = false;
	static Vector3f previousHand;

	static Vector3f recentBallPos = new Vector3f();
	static Matrix4f recentRacketTransf;
	static Matrix3f initialHandRot;
	static Matrix3f previousHandRot;
	static Matrix3f rotSpeed;
	
	static int[] indicesRoom;

	// additional parameters
	static Vector3f ballSpeed;
	static float initialSpeed;
	static float ballWallReflection = 0.8f;
	static float ballRacketReflection = 0.6f;
	static boolean gameStarted = false;
	static float gravity;
	static long highscore;
	static long counter;
	static float airResistance = 0.999f;//speed of ball is slowed down by this factor every frame
	
	static boolean hitRacket;
	static int floorCounter;
	
	//for the songs
	static int songNumber;
    static ArrayList<String> songs,lastSongs;
    static Clip clip;
    static boolean manually,hasChosenLast;
    //this shows how far back we are in the lastSongs list.
    static int back;
    static int position;
	

	/**
	 * An extension of {@link OpenVRRenderPanel} to provide a call-back function
	 * for initialization.
	 */
	public final static class SimpleVRRenderPanel extends VRRenderPanel {
		private Timer timer; // Timer to trigger animation rendering

		/**
		 * Initialization call-back. We initialize our renderer here.
		 * 
		 * @param r
		 *            the render context that is associated with this render
		 *            panel
		 * 
		 */
		public void init(RenderContext r) {
			renderContext = r;
			previousHandRot = new Matrix3f();
			initialHandRot = new Matrix3f();
			rotSpeed = new Matrix3f();
			activateGravity();
			counter=0;
			hitRacket=false;
			FileInputStream fis;
			try {
				fis = new FileInputStream("highscore.txt");
		        BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
		        String line = reader.readLine();
		        highscore = Long.parseLong(line);
		        System.out.println("Highscore found" + highscore);
			} catch (Exception e) {
				highscore = 0;
				System.out.println("Highscore not found");
			}

			Shader simpleShader = renderContext.makeShader();
			try {
                simpleShader.load("../jrtr/shaders/diffuse.vert", "../jrtr/shaders/diffuse.frag");
//				simpleShader.load("../jrtr/shaders/default.vert", "../jrtr/shaders/default.frag");
            } catch(Exception e) {
                System.out.print("Problem with shader:\n");
                System.out.print(e.getMessage());
            }
			
			try {
//              bufferedImage = ImageIO.read(new File("file:///C:/Users/cg2016_team1/git/VR 5/textures/wood.jpg"));
              scoreBackground = ImageIO.read(new File("../textures/wall4.jpg"));
          } catch (IOException e) {
          	e.printStackTrace();
          }
			
			diffuseShader = simpleShader;
			floorCounter=0;
			 makeCubeShape();
			  ballMaterial = new Material();
		        ballMaterial.shader = simpleShader;
//		        ballMaterial.diffuseMap = renderContext.makeTexture();
		        ballMaterial.texture = renderContext.makeTexture();
		        try {
//		            ballMaterial.diffuseMap.load("../textures/wood.jpg");
		            ballMaterial.texture.load("../textures/ball2.jpg");
		        } catch(Exception e) {
		            System.out.print("Could not load texture.\n");
		            System.out.print(e.getMessage());
		        }
			 makeCubeMaterial();
			// Make a simple geometric object: a cube

			// The vertex positions of the cube
			float v[] = { -1, -1, 1, 1, -1, 1, 1, 1, 1, -1, 1, 1, // front face
					-1, -1, -1, -1, -1, 1, -1, 1, 1, -1, 1, -1, // left face
					1, -1, -1, -1, -1, -1, -1, 1, -1, 1, 1, -1, // back face
					1, -1, 1, 1, -1, -1, 1, 1, -1, 1, 1, 1, // right face
					1, 1, 1, 1, 1, -1, -1, 1, -1, -1, 1, 1, // top face
					-1, -1, 1, -1, -1, -1, 1, -1, -1, 1, -1, 1 }; // bottom face
			// for(int i=0; i<Array.getLength(v); i++) v[i] = v[i] * 0.1f; //
			// make it smaller

			// The vertex colors
			float c[] = { 0.5f, 0, 0, 0.5f, 0, 0, 0.5f, 0, 0, 0.5f, 0, 0, 0, 0.3f, 0, 0, 0.3f, 0, 0, 0.3f, 0, 0, 0.3f,
					0, 0.5f, 0, 0, 0.5f, 0, 0, 0.5f, 0, 0, 0.5f, 0, 0, 0, 0.3f, 0, 0, 0.3f, 0, 0, 0.3f, 0, 0, 0.3f, 0,
					0, 0.3f, 0.3f, 0, 0.3f, 0.3f, 0, 0.3f, 0.3f, 0, 0.3f, 0.3f, 0, 0.3f, 0.3f, 0, 0.3f, 0.3f, 0, 0.3f,
					0.3f, 0, 0.3f, 0.3f, };

			// The vertex normals
			float n[] = { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, // front face
					-1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, // left face
					0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, // back face
					1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, // right face
					0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, // top face
					0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0 }; // bottom face

			// Texture coordinates
			float uv[] = { 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0,
					1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1 };

			// The triangles (three vertex indices for each triangle)
			int indices[] = { 0, 2, 3, 0, 1, 2, // front face
					4, 6, 7, 4, 5, 6, // left face
					8, 10, 11, 8, 9, 10, // back face
					12, 14, 15, 12, 13, 14, // right face
					16, 18, 19, 16, 17, 18, // top face
					20, 22, 23, 20, 21, 22 }; // bottom face
			
			indicesRoom= new int[]{ 3, 2, 0, 2, 1, 0, // front face
					7, 6, 4, 6, 5, 4, // left face
					11, 10, 8, 10, 9, 8, // back face
					15, 14, 12, 14, 13, 12, // right face
					19, 18, 16, 18, 17, 16, // top face
					23, 22, 20, 22, 21, 20 }; // bottom face
			
//			int indicesRoom[] = { 0, 2, 3, 0, 1, 2, // front face
//					4, 6, 7, 4, 5, 6, // left face
//					8, 10, 11, 8, 9, 10, // back face
//					12, 14, 15, 12, 13, 14, // right face
//					16, 18, 19, 16, 17, 18, // top face
//					20, 22, 23, 20, 21, 22 }; // bottom face
//			
//			for(int i = 0; i < indicesRoom.length / 2; i++)
//			{
//			    int temp = indicesRoom[i];
//			    indicesRoom[i] = indicesRoom[indicesRoom.length - i - 1];
//			    indicesRoom[indicesRoom.length - i - 1] = temp;
//			}
			
			
			updateScore();

			// A room around the cube, made out of an other cube
			float[] vRoom = new float[Array.getLength(v)];
			float[] nRoom = new float[Array.getLength(n)];
			float[] cRoom = new float[Array.getLength(c)];
			for (int i = 0; i < Array.getLength(vRoom); i++)
				vRoom[i] = v[i] * (roomSize);
			for (int i = 0; i < Array.getLength(nRoom); i++)
				nRoom[i] = n[i] * -1.f;
			for (int i = 0; i < Array.getLength(cRoom); i++)
				cRoom[i] = c[i] * 0.5f;

			VertexData vertexDataRoom = renderContext.makeVertexData(24);
			vertexDataRoom.addElement(cRoom, VertexData.Semantic.COLOR, 3);
			vertexDataRoom.addElement(vRoom, VertexData.Semantic.POSITION, 3);
			vertexDataRoom.addElement(nRoom, VertexData.Semantic.NORMAL, 3);
			vertexDataRoom.addElement(uv, VertexData.Semantic.TEXCOORD, 2);
			vertexDataRoom.addIndices(indices);

			// A smaller cube to show the tracked VR controller
			float[] vControllerCube = new float[Array.getLength(v)];
			float[] nControllerCube = new float[Array.getLength(n)];
			float[] cControllerCube = new float[Array.getLength(c)];
			for (int i = 0; i < Array.getLength(vRoom); i++)
				vControllerCube[i] = v[i] * controllerSize;
			for (int i = 0; i < Array.getLength(nRoom); i++)
				nControllerCube[i] = n[i];
			for (int i = 0; i < Array.getLength(cRoom); i++)
				cControllerCube[i] = 0.4f;
			VertexData vertexDataControllerCube = renderContext.makeVertexData(24);
			vertexDataControllerCube.addElement(cControllerCube, VertexData.Semantic.COLOR, 3);
			vertexDataControllerCube.addElement(vControllerCube, VertexData.Semantic.POSITION, 3);
			vertexDataControllerCube.addElement(nControllerCube, VertexData.Semantic.NORMAL, 3);
			vertexDataControllerCube.addElement(uv, VertexData.Semantic.TEXCOORD, 2);
			vertexDataControllerCube.addIndices(indices);

			// A smaller cube to show the tracked VR controller (brighter, when
			// triggered)
			float[] cControllerCubeTriggered = new float[Array.getLength(c)];
			for (int i = 0; i < Array.getLength(cRoom); i++)
				cControllerCubeTriggered[i] = 0.8f;
			VertexData vertexDataControllerCubeTriggered = renderContext.makeVertexData(24);
			vertexDataControllerCubeTriggered.addElement(cControllerCubeTriggered, VertexData.Semantic.COLOR, 3);
			vertexDataControllerCubeTriggered.addElement(vControllerCube, VertexData.Semantic.POSITION, 3);
			vertexDataControllerCubeTriggered.addElement(nControllerCube, VertexData.Semantic.NORMAL, 3);
			vertexDataControllerCubeTriggered.addElement(uv, VertexData.Semantic.TEXCOORD, 2);
			vertexDataControllerCubeTriggered.addIndices(indices);

			// same controller cube with different colors, make it long and thin
			float[] vRacket = new float[Array.getLength(v)];
			for (int i = 0; i < Array.getLength(vRoom) / 3; i++) {
				vRacket[3 * i] = (racketSize) * v[3 * i];
				vRacket[3 * i + 1] = 5f * racketSize * v[3 * i + 1];
				vRacket[3 * i + 2] = 20f * racketSize * v[3 * i + 2] - 0.2f;
				racketBoundsMax.x = Math.max(racketBoundsMax.x, vRacket[3 * i]);
				racketBoundsMax.y = Math.max(racketBoundsMax.y, vRacket[3 * i + 1]);
				racketBoundsMax.z = Math.max(racketBoundsMax.z, vRacket[3 * i + 2]);
				racketBoundsMin.x = Math.min(racketBoundsMin.x, vRacket[3 * i]);
				racketBoundsMin.y = Math.min(racketBoundsMin.y, vRacket[3 * i + 1]);
				racketBoundsMin.z = Math.min(racketBoundsMin.z, vRacket[3 * i + 2]);
			}
			VertexData vertexDataRacket = renderContext.makeVertexData(24);
			vertexDataRacket.addElement(cControllerCube, VertexData.Semantic.COLOR, 3);
			vertexDataRacket.addElement(vRacket, VertexData.Semantic.POSITION, 3);
			vertexDataRacket.addElement(nControllerCube, VertexData.Semantic.NORMAL, 3);
			vertexDataRacket.addElement(uv, VertexData.Semantic.TEXCOORD, 2);
			vertexDataRacket.addIndices(indices);

			// The ball
			Sphere ballObj = new Sphere(30, ballRadius, new float[] { 0.5f, 0.4f, 0.1f },
					new float[] { 0.2f, 0.3f, 0.5f });
			VertexData vertexDataBall = renderContext.makeVertexData(ballObj.n);
			vertexDataBall.addElement(ballObj.colors, VertexData.Semantic.COLOR, 3);
			vertexDataBall.addElement(ballObj.vertices, VertexData.Semantic.POSITION, 3);
			vertexDataBall.addElement(ballObj.normals, VertexData.Semantic.NORMAL, 3);
			vertexDataBall.addElement(ballObj.texcoords, VertexData.Semantic.TEXCOORD, 2);
			vertexDataBall.addIndices(ballObj.indices);

			// Make a scene manager and add the objects
			sceneManager = new SimpleSceneManager();
			
			
			
//	        material = new Material();
//	        material.shader = simpleShader;
//	        material.diffuseMap = renderContext.makeTexture();
//	        material.texture = renderContext.makeTexture();
//	        try {
//	            material.diffuseMap.load("../textures/wood.jpg");
//	            material.texture.load("../textures/wood.jpg");
//	        } catch(Exception e) {
//	            System.out.print("Could not load texture.\n");
//	            System.out.print(e.getMessage());
//	        }
	        
	        racketMaterial = new Material();
	        racketMaterial.shader = simpleShader;
//	        racketMaterial.diffuseMap = renderContext.makeTexture();
	        racketMaterial.texture = renderContext.makeTexture();
	        try {
//	            racketMaterial.diffuseMap.load("../textures/wood.jpg");
	            racketMaterial.texture.load("../textures/wood.jpg");
	        } catch(Exception e) {
	            System.out.print("Could not load texture.\n");
	            System.out.print(e.getMessage());
	        }
	        
	        handMaterial = new Material();
	        handMaterial.shader = simpleShader;
//	        handMaterial.diffuseMap = renderContext.makeTexture();
	        handMaterial.texture = renderContext.makeTexture();
	        try {
//	            handMaterial.diffuseMap.load("../textures/wood.jpg");
	            handMaterial.texture.load("../textures/duhueresohn.jpg");
	        } catch(Exception e) {
	            System.out.print("Could not load texture.\n");
	            System.out.print(e.getMessage());
	        }
	           
//			surroundingCube = new Shape(vertexDataRoom);
			controllerCube = new Shape(vertexDataControllerCube);
			controllerCubeTriggered = new Shape(vertexDataControllerCubeTriggered);
			controllerRacket = new Shape(vertexDataRacket);
			ball = new Shape(vertexDataBall);
			textureShape = makeTextureShape();
			
//			surroundingCube.setMaterial(material);
			ball.setMaterial(ballMaterial);
			controllerRacket.setMaterial(racketMaterial);
			controllerCube.setMaterial(handMaterial);


			// Load the racket
			/*
			 * try{ VertexData racketData =
			 * ObjReader.read("../obj/TennisReket.obj",2,renderContext);
			 * controllerRacket = new Shape(racketData); } catch (IOException
			 * e){ e.printStackTrace(); }
			 */
			
			 try{ VertexData racketData =
					  ObjReader.read("../obj/mesh.obj",0.1f,renderContext);
					  controllerCube = new Shape(racketData); } catch (IOException
					  e){ e.printStackTrace(); }
			 
			 try{ VertexData racketData =
					  ObjReader.read("../obj/mesh.obj",0.1f,renderContext);
					  controllerCubeTriggered = new Shape(racketData); } catch (IOException
					  e){ e.printStackTrace(); }
			 
			 Sphere handBall = new Sphere(30, 0.000001f, new float[] { 0.5f, 0.4f, 0.1f },
						new float[] { 0.2f, 0.3f, 0.5f });
				VertexData vertexDatahandBall = renderContext.makeVertexData(ballObj.n);
				vertexDatahandBall.addElement(ballObj.colors, VertexData.Semantic.COLOR, 3);
				vertexDatahandBall.addElement(ballObj.vertices, VertexData.Semantic.POSITION, 3);
				vertexDatahandBall.addElement(ballObj.normals, VertexData.Semantic.NORMAL, 3);
				vertexDatahandBall.addElement(ballObj.texcoords, VertexData.Semantic.TEXCOORD, 2);
				vertexDatahandBall.addIndices(ballObj.indices);
				
				controllerCubeTriggered = new Shape(vertexDatahandBall);
				
				Matrix4f t2  = controllerCubeTriggered.getTransformation();
				t2.mul(0.5f);
				controllerCubeTriggered.setTransformation(t2);
			
				controllerCubeTriggered= new Shape(controllerCube.getVertexData());
			
			 
			
			Light light = new Light();
			sceneManager.addLight(light);

			//sceneManager.addShape(surroundingCube);
			sceneManager.addShape(controllerCube);
			sceneManager.addShape(controllerCubeTriggered);
			sceneManager.addShape(controllerRacket);
			sceneManager.addShape(ball);
			sceneManager.addShape(textureShape);
			sceneManager.addShape(backShape);
            sceneManager.addShape(frontShape);
            sceneManager.addShape(leftShape);
            sceneManager.addShape(rightShape);
            sceneManager.addShape(floorShape);
            sceneManager.addShape(ceilingShape);

			ballSpeed = new Vector3f();

			// Set up the camera
			sceneManager.getCamera().setCenterOfProjection(new Vector3f(0, -0.6f, 0.7f));
			sceneManager.getCamera().setLookAtPoint(new Vector3f(0, -0.6f, 1));
			sceneManager.getCamera().setUpVector(new Vector3f(0, 1, 0));

			// Add the scene to the renderer
			renderContext.setSceneManager(sceneManager);

			resetBallPosition(); // set initial ball position
			recentRacketTransf = visualizeRacket(renderPanel.controllerIndexRacket);
			
			try {
			playSound(-10000, "file:///C:/Users/cg2016_team1/git/VR 5/sounds/tennisVolley.wav");
			}
			catch (IllegalArgumentException iae) {	
			}
			try {
			playSound(-10000, "file:///C:/Users/cg2016_team1/git/VR 5/sounds/Tennis_Serve.wav");
			}
			catch (IllegalArgumentException iae) {	
			}
			
			songs =listMusic();
            songNumber=0;
            back=1;
            manually=false;
            songNumber = (int) (Math.random() * songs.size());
            playSong(3f,"file:///C:\\Users\\cg2016_team1\\git\\VR 5\\sounds\\songs\\"+songs.get(songNumber));


		}
		
		private static BufferedImage createTransformed(
		        BufferedImage image, AffineTransform at)
		    {
		        BufferedImage newImage = new BufferedImage(
		            image.getWidth(), image.getHeight(),
		            BufferedImage.TYPE_INT_RGB);
		        Graphics2D g = newImage.createGraphics();
		        g.transform(at);
		        g.drawImage(image, 0, 0, null);
		        g.dispose();
		        return newImage;
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
		
		protected static ArrayList<String> listMusic(){
            File folder = new File("C:\\Users\\cg2016_team1\\git\\VR 5\\sounds\\songs");
            File[] listOfFiles = folder.listFiles();
            ArrayList<String> songs = new ArrayList<String>();
            lastSongs=new ArrayList<String>();
            back=1;

            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    songs.add(listOfFiles[i].getName());
                    System.out.println(songs.get(i));

                }
            }
            System.out.println("-");
            int size=songs.size();

            ArrayList<String> randomSongs = new ArrayList<String>();
            for(int i=0;i<size;i++){
                randomSongs.add(songs.remove((int) (Math.random()* (songs.size()-1)) ));
                System.out.println(randomSongs.get(i));
            }
            return randomSongs;
        }
		
		public static void stopSong(){
            manually=true;
            clip.stop();
        }

        protected static void startSong(){
            clip.start();
            manually=false;
        }
        
        protected static void playSong(float volume, String urlStr) {

            try {
                clip = AudioSystem.getClip();
                URL url = new URL(urlStr);
                AudioInputStream inputStream = AudioSystem.getAudioInputStream(
                        url);
                if(!clip.isOpen()) {
                    clip.open(inputStream);
                }
                FloatControl gainControl =
                        (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float actualValue = Math.max(gainControl.getMinimum(), volume);
                actualValue = Math.min(actualValue, 6.0206f);
                gainControl.setValue(6.0206f - actualValue);
                gainControl.setValue(gainControl.getMinimum()+70);// Reduce volume by 10 decibels.
                clip.start();
                LineListener listener = new LineListener() {
                    public void update(LineEvent event) {
                        if (event.getType() != LineEvent.Type.STOP || manually) {
                            return;
                        }
                        /*//Linear
                        if (songNumber >= songs.size()) {
                            songNumber = 0;
                        }*/
                        //random play
                       /* if(hasChosenLast && lastSongs.size()>0){
                            playSong(10, "file:///C:\\Users\\smoen\\Documents\\Studium\\Computergrafik\\Übungen\\Übung 1\\Computergrafik-Basecode\\sounds\\" + lastSongs.get(lastSongs.size()-back));
                           // lastSongs.remove(lastSongs.get(lastSongs.size()-1));
                        }
                        else {
                            if (songs.size() <= 0) {
                                songs = listMusic();
                            }*/
                            if(position>=songs.size()){
                                songs=listMusic();
                                position=0;
                            }
                            songNumber = (int) (Math.random() * songs.size());

                            playSong(1f, "file:///C:\\Users\\cg2016_team1\\git\\VR 5\\sounds\\songs\\" + songs.get(songNumber));
                            //also for random
                           // String lastSong = songs.remove(songNumber);
                            //lastSongs.add(lastSong);
                       // }

                    }
                };
                clip.addLineListener(listener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
		
		 private void makeCubeMaterial() {
	            // Front material
	            floorMaterial = new Material();
	            floorMaterial.shader = diffuseShader;
//	            floorMaterial.diffuseMap = renderContext.makeTexture();
	            floorMaterial.texture = renderContext.makeTexture();
	            try {
//	                floorMaterial.diffuseMap.load("../textures/plant.jpg");
	                floorMaterial.texture.load("../textures/floor.jpg");
	            } catch(Exception e) {
	                System.out.print("Could not load texture.floor\n");
	                System.out.print(e.getMessage());
	            }
	            floorShape.setMaterial(floorMaterial);

	            ceilingMaterial = new Material();
	            ceilingMaterial.shader = diffuseShader;
//	            ceilingMaterial.diffuseMap = renderContext.makeTexture();
	            ceilingMaterial.texture = renderContext.makeTexture();
	            try {
//	                ceilingMaterial.diffuseMap.load("../textures/plant.jpg");
	                ceilingMaterial.texture.load("../textures/ceiling.jpg");
	            } catch(Exception e) {
	                System.out.print("Could not load texture.ceiling\n");
	                System.out.print(e.getMessage());
	            }
	            ceilingShape.setMaterial(ceilingMaterial);

	            rightMaterial = new Material();
	            rightMaterial.shader = diffuseShader;
//	            rightMaterial.diffuseMap = renderContext.makeTexture();
	            rightMaterial.texture = renderContext.makeTexture();
	            try {
//	                rightMaterial.diffuseMap.load("../textures/plant.jpg");
	                rightMaterial.texture.load("../textures/wall4.jpg");
	            } catch(Exception e) {
	                System.out.print("Could not load texture.left\n");
	                System.out.print(e.getMessage());
	            }
	            rightShape.setMaterial(rightMaterial);

	            leftMaterial = new Material();
	            leftMaterial.shader = diffuseShader;
//	            leftMaterial.diffuseMap = renderContext.makeTexture();
	            leftMaterial.texture = renderContext.makeTexture();
	            try {
//	                leftMaterial.diffuseMap.load("../textures/plant.jpg");
	                leftMaterial.texture.load("../textures/wall4.jpg");
	            } catch(Exception e) {
	                System.out.print("Could not load textureright.\n");
	                System.out.print(e.getMessage());
	            }
	            leftShape.setMaterial(leftMaterial);

	          

	            frontMaterial = new Material();
	            frontMaterial.shader = diffuseShader;
//	            frontMaterial.diffuseMap = renderContext.makeTexture();
	            frontMaterial.texture = renderContext.makeTexture();
	            try {
//	                frontMaterial.diffuseMap.load("../textures/plant.jpg");
	                frontMaterial.texture.load("../textures/wall4.jpg");
	            } catch(Exception e) {
	                System.out.print("Could not load texture. front\n");
	                System.out.print(e.getMessage());
	            }
	            frontShape.setMaterial(frontMaterial);

	            backMaterial = new Material();
	            backMaterial.shader = diffuseShader;
//	            backMaterial.diffuseMap = renderContext.makeTexture();
	            backMaterial.texture = renderContext.makeTexture();
	            try {
//	                backMaterial.diffuseMap.load("../textures/glurak.jpg");
	                backMaterial.texture.load("../textures/wall4.jpg");
	            } catch(Exception e) {
	                System.out.print("Could not load texture.back \n");
	                System.out.print(e.getMessage());
	            }
	            backShape.setMaterial(backMaterial);

	        }
    

		
		private static void updateScore(){
			//Prepare highscore texture
			
			if(counter > highscore)
			{
				highscore = counter;
			    for (int i = 0; i < 3; ++i) {
			        try {
					    BufferedWriter out = new BufferedWriter(new FileWriter("highscore.txt"));
						out.write(Long.toString(counter));
					    out.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			    }
			}
			String key = "Score: "+counter + "\n";
			String score ="Highscore: " + highscore;
			
            BufferedImage bufferedImage = new BufferedImage(250, 250,
                    BufferedImage.TYPE_INT_RGB);
//            try {
////                bufferedImage = ImageIO.read(new File("file:///C:/Users/cg2016_team1/git/VR 5/textures/wood.jpg"));
//                bufferedImage = ImageIO.read(new File("../textures/wood.jpg"));
//            } catch (IOException e) {
//            	e.printStackTrace();
//            }
            
            
            Graphics graphics = bufferedImage.getGraphics();
            graphics.drawImage(scoreBackground,0,0,null);
             graphics.setColor(Color.RED);
//            graphics.fillRect(0, 0, 250, 250);
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font("Arial Black", Font.BOLD, 20));
          
            graphics.drawString(key, 10, 25);
            graphics.drawString(score,10,80);
         
            bufferedImage = createFlipped(bufferedImage);
            try {
                ImageIO.write(bufferedImage, "png", new File(
                        "../textures/saved.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
//	            textureMaterial.diffuseMap.load("../textures/saved.png");
	            textureMaterial.texture.load("../textures/saved.png");
	        } catch(Exception e) {
	            System.out.print("Could not load texture.\n");
	            System.out.print(e.getMessage());
	        }
            
            
			
		}
		
		private static BufferedImage createFlipped(BufferedImage image)
	    {
	        AffineTransform at = new AffineTransform();
	        at.concatenate(AffineTransform.getScaleInstance(-1, 1));
	        at.concatenate(AffineTransform.getTranslateInstance(-image.getHeight(),0));
	        return createTransformed(image, at);
	    }
		


		private Shape makeTextureShape() {
			float v[] =  {-1, -1, 0.9999f, 1, -1, 0.9999f, 1, 1, 0.9999f, -1, 1, 0.9999f};
			for(int i=0;i<v.length;i++){
				v[i]*=roomSize;
			}
			float n[] = { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1};
			float c[] = {1,0,0, 1,0,0, 1,0,0, 1,0,0};
			float uv[] = {0,0, 1,0, 1,1, 0,1};
			for (int i = 0; i < Array.getLength(n); i++)
				n[i] = n[i] * -1.f;
			
			VertexData vertexData = renderContext.makeVertexData(4);
			vertexData.addElement(c, VertexData.Semantic.COLOR, 3);
			vertexData.addElement(v, VertexData.Semantic.POSITION, 3);
			vertexData.addElement(n, VertexData.Semantic.NORMAL, 3);
			vertexData.addElement(uv, VertexData.Semantic.TEXCOORD, 2);
			
			// The triangles (three vertex indices for each triangle)
			int indices[] = {0,2,3, 0,1,2};
			vertexData.addIndices(indices);
			Shape shape = new Shape(vertexData);
			
			
			Shader diffuseShader = renderContext.makeShader();
            try {
                diffuseShader.load("../jrtr/shaders/diffuse.vert", "../jrtr/shaders/diffuse.frag");
            } catch(Exception e) {
                System.out.print("Problem with shader:\n");
                System.out.print(e.getMessage());
            }
            

//             Make a textureMaterial that can be used for shading
            textureMaterial = new Material();
            textureMaterial.shader = diffuseShader;
//            textureMaterial.diffuseMap = renderContext.makeTexture();
            textureMaterial.texture = renderContext.makeTexture();

            try {
//                textureMaterial.diffuseMap.load("../textures/saved.png");
                textureMaterial.texture.load("../textures/saved.png");
            } catch(Exception e) {
                System.out.print("Could not load texture.\n");
                System.out.print(e.getMessage());
            }
            shape.setMaterial(textureMaterial);
			return shape;
			
		}

		public void dispose() {
			// Stop timer from triggering rendering of animation frames
			// timer.cancel();
			// timer.purge();
		}

		/*
		 * Helper function to visualise the controller corresponding to the
		 * hand. Gives visual feedback when trigger is pressed. Returns the
		 * trafo of the controller.
		 */
		private Matrix4f visualizeHand(int index) {
			Matrix4f handT = new Matrix4f();
			handT.setIdentity();

			if (index != -1) {
				Matrix4f hiddenT = new Matrix4f();
				Shape visibleShape, hiddenShape;

				// To have some feedback when pushing the trigger button we flip
				// the two
				// "trigger" and "untrigger" shapes. The currently hidden object
				// is
				// translated out of the viewfrustum since openGL does not have
				// a direct
				// "make invisible" command for individual shapes w/o changing
				// the jrtr
				// pipeline.
				if (renderPanel.getTriggerTouched(renderPanel.controllerIndexHand)) {
					visibleShape = controllerCubeTriggered;
					hiddenShape = controllerCube;
				} else {
					hiddenShape = controllerCubeTriggered;
					visibleShape = controllerCube;
				}

				// Update pose of hand controller; note that the pose of the
				// hand controller
				// is independent of the scene camera pose, so we include the
				// inverse scene
				// camera matrix here to undo the camera trafo that is
				// automatically applied
				// by the renderer to all scene objects
				handT = new Matrix4f(sceneManager.getCamera().getCameraMatrix());
				handT.invert();
				handT.mul(renderPanel.poseMatrices[index]);
				visibleShape.setTransformation(handT);
				 Matrix4f t = controllerCube.getTransformation();
				 Matrix4f rotY = new Matrix4f();
				 Matrix4f rotZ = new Matrix4f();
				 Matrix4f rotX = new Matrix4f();
				 rotX.rotX((float) Math.toRadians(-90));
				 t.mul(rotX);
				 rotY.rotY((float) Math.toRadians(170));
				 t.mul(rotY);
				 rotZ.rotZ((float) Math.toRadians(0));
				 t.mul(rotZ);
				 controllerCube.setTransformation(t);

				// hidden shape is translated to "oblivion"
				hiddenT = new Matrix4f();
				hiddenT.setIdentity();
				hiddenT.setTranslation(new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE));
				hiddenShape.setTransformation(hiddenT);
				t = controllerCubeTriggered.getTransformation();
				t.mul(rotX);
				t.mul(rotY);
				t.mul(rotZ);
				controllerCubeTriggered.setTransformation(t);
			}
			return handT;
		}

		/*
		 * Helper function to visualise the controller corresponding to the
		 * racket. Returns the trafo of the controller.
		 */
		private Matrix4f visualizeRacket(int index) {
			Matrix4f racketT = new Matrix4f();
			racketT.setIdentity();
			if (index != -1) {
				// current shape follows the controller
				racketT = new Matrix4f(sceneManager.getCamera().getCameraMatrix());
				racketT.invert();
				racketT.mul(renderPanel.poseMatrices[index]);
				controllerRacket.setTransformation(racketT);
				// Matrix4f racketTcopy = new Matrix4f(racketT);
				// Matrix4f racketTcopy2 = new Matrix4f(racketT);
				// racketTcopy.transform(racketBoundsMax);
				// racketTcopy.transform(racketBoundsMin);
			}
			return racketT;
		}

		/*
		 * Helper function: Reset ball Position if we press the side buttons of
		 * the "hand"
		 */
		private void resetBallPosition() {
			// reset Ball Position
			Matrix4f ballInitTrafo = ball.getTransformation();
			ballInitTrafo.setIdentity();
			gameStarted = false;
			hitRacket=false;
			counter=0;
			floorCounter=0;
			updateScore();

			// reset all other class members related to remembering previous
			// positions of objects
			ballSpeed = new Vector3f(0, 0, 0); // shift ball
																// a bit
																// downwards
																// since the
																// camera is
																// at
																// 0,-1,-0.3

			ballInitTrafo.setTranslation(ballSpeed);
		}
		
		private void activateGravity(){
			gravity = 0.0006f;//0.0006f
		}

		private void removeGravity(){
			gravity = 0;
		}
		
		 
		 
		/*
		 * Override from base class. Triggered by 90 FPS animation.
		 */
		public void prepareDisplay() {
		

			// Reset ball position
			if (renderPanel.getSideTouched(renderPanel.controllerIndexHand)) {
				resetBallPosition();
			}
			if(renderPanel.getTriggerTouched(renderPanel.controllerIndexRacket)){
				position++;
				clip.stop();
			}
			if(renderPanel.getSideTouched(renderPanel.controllerIndexRacket)){
				position--;
				clip.stop();
			}

			// get current ball transformation matrix.
			Matrix4f ballTrafo = ball.getTransformation();

			// Get VR tracked poses. Anything using any tracking data from the
			// VR devices must happen *after* waitGetPoses() is called!
			renderPanel.waitGetPoses();

			// Visualise controlling devices
			Matrix4f handTrafo = visualizeHand(renderPanel.controllerIndexHand);
			Matrix4f racketTrafo = visualizeRacket(renderPanel.controllerIndexRacket);

			// Move the thrown ball
			if (!touched && gameStarted) {
				ballSpeed.y -= gravity;
				ballSpeed.scale(airResistance);
			}

			// The reflection with the walls (Walls in brackets are as seen in
			// front of computer)
			if (!touched && gameStarted) {

				Vector3f posBall = new Vector3f(ballTrafo.m03, ballTrafo.m13, ballTrafo.m23);

				// Negative x-wall (Right)
				if ((posBall.x + roomSize) <= ballRadius && ballSpeed.x <= 0) {
					ballSpeed.x *= -1;
					ballSpeed.scale(ballWallReflection);
					scaleRotSpeed(ballWallReflection);
					if (ballSpeed.length() > 0.01f)
						playSound(1f/((ballSpeed.length()+0.0001f)*100), "file:///C:/Users/cg2016_team1/git/VR 5/sounds/tennisVolley.wav");
						
						if(hitRacket){
							counter++;
							floorCounter=0;
							updateScore();
							
							hitRacket=false;
						}
				}

				// Positive x-wall (Left)
				if ((roomSize - posBall.x) <= ballRadius && ballSpeed.x >= 0) {
					ballSpeed.x *= -1;
					ballSpeed.scale(ballWallReflection);
					scaleRotSpeed(ballWallReflection);
					if (ballSpeed.length() > 0.01f)
						playSound(1f/((ballSpeed.length()+0.0001f)*100), "file:///C:/Users/cg2016_team1/git/VR 5/sounds/tennisVolley.wav");
					
					if(hitRacket){
						counter++;
						floorCounter=0;
						updateScore();
						hitRacket=false;
					}
				}

				// Negative y-wall (floor)
				if ((posBall.y + roomSize) <= ballRadius && ballSpeed.y <= 0) {
					ballSpeed.y *= -1;
					ballSpeed.scale(ballWallReflection);
					scaleRotSpeed(ballWallReflection);
					if (Math.abs(ballSpeed.y) < 0.0006f) {
						ballSpeed.y = 0;
						removeGravity();
					}
					if (ballSpeed.length() > 0.01f)
						playSound(1f/((ballSpeed.length()+0.0001f)*100), "file:///C:/Users/cg2016_team1/git/VR 5/sounds/tennisVolley.wav");
					
					floorCounter++;
					if(floorCounter>=2){
						floorCounter=0;
						counter=0;
						updateScore();
					}
				}

//				// Positive y-wall (ceiling)
				if ((roomSize - posBall.y) <= ballRadius && ballSpeed.y >= 0) {
					ballSpeed.y *= -1;
					ballSpeed.scale(ballWallReflection);
					scaleRotSpeed(ballWallReflection);
					if (ballSpeed.length() > 0.01f)
						playSound(1f/((ballSpeed.length()+0.0001f)*100), "file:///C:/Users/cg2016_team1/git/VR 5/sounds/tennisVolley.wav");
				}

				// Negative z-wall (Behind)
				if ((posBall.z + roomSize) <= ballRadius && ballSpeed.z <= 0) {
					ballSpeed.z *= -1;
					ballSpeed.scale(ballWallReflection);
					scaleRotSpeed(ballWallReflection);
					if (ballSpeed.length() > 0.01f)
						playSound(1f/((ballSpeed.length()+0.0001f)*100), "file:///C:/Users/cg2016_team1/git/VR 5/sounds/tennisVolley.wav");
					
					if(hitRacket){
						counter++;
						floorCounter=0;
						updateScore();
						hitRacket=false;
					}
				}

				// Positive z-wall (in Front)
				if ((roomSize - posBall.z) <= ballRadius && ballSpeed.z >= 0) {
					ballSpeed.z *= -1;
					ballSpeed.scale(ballWallReflection);
					scaleRotSpeed(ballWallReflection);
					if (ballSpeed.length() > 0.01f)
						playSound(1f/((ballSpeed.length()+0.0001f)*100), "file:///C:/Users/cg2016_team1/git/VR 5/sounds/tennisVolley.wav");
					
					
					if(hitRacket){
						counter++;
						floorCounter=0;
						updateScore();
						hitRacket=false;
					}
				}

				// Intersection with racket
				Matrix4f ballTrafoBoxSpace = new Matrix4f(ballTrafo);
				Matrix4f invertedRacketMat = new Matrix4f(racketTrafo);
				if(invertedRacketMat.determinant() != 0)
				{
					invertedRacketMat.invert();
				}
				//transform speed into racket coordinates to mirror it on reflection vector
				invertedRacketMat.transform(ballSpeed);
				invertedRacketMat.mul(ballTrafoBoxSpace);

				Vector3f hitPoint = checkBallRacketIntersection(new Matrix4f(invertedRacketMat));
				if (hitPoint != null) {
					Vector3f n = new Vector3f(invertedRacketMat.m03, invertedRacketMat.m13, invertedRacketMat.m23);//somehow, this doesn't equal the transformed posBall
					Vector4f centerBallInRacketCoords = new Vector4f(n.x,n.y,n.z,1f);
					n.sub(hitPoint);
					
					
					//this part is for putting the ball outside the racket. It doesn't work yet
					centerBallInRacketCoords = positionBallOutsideRacket(hitPoint, centerBallInRacketCoords);
					racketTrafo.transform(centerBallInRacketCoords);
					posBall = new Vector3f(centerBallInRacketCoords.x, centerBallInRacketCoords.y, centerBallInRacketCoords.z);
					//until here
					
					 n = new Vector3f(invertedRacketMat.m03, invertedRacketMat.m13, invertedRacketMat.m23);
					 n.sub(hitPoint);
					
					transformSpeed(n);
					addRacketSpeed(hitPoint, racketTrafo, n);
					
					//a haptic feedback) is triggered
					renderPanel.triggerHapticPulse(renderPanel.controllerIndexRacket, 3999);//when ball hits racket
					if (ballSpeed.length() > 0.01f)
					{
						playSound(1f/((ballSpeed.length()+0.0001f)*100-1), "file:///C:/Users/cg2016_team1/git/VR 5/sounds/tennisVolley.wav");
					}
										
					//Increase score
					hitRacket=true;
					
				}
				racketTrafo.transform(ballSpeed);

				posBall.add(ballSpeed);
				ballTrafo.setTranslation(posBall);
				Matrix3f rot = new Matrix3f();
				ballTrafo.getRotationScale(rot);
				scaleRotSpeed(0.9999f);
				rot.mul(rotSpeed);
				ballTrafo.setRotation(rot);
//				rotSpeed.setIdentity();
			}

			if (!touched) {
				if (renderPanel.getTriggerTouched(renderPanel.controllerIndexHand)) {
					Vector3f posHand = new Vector3f(handTrafo.m03, handTrafo.m13, handTrafo.m23);
					Vector3f posBall = new Vector3f(ballTrafo.m03, ballTrafo.m13, ballTrafo.m23);
					posBall.negate();

					Vector3f distance = new Vector3f(posHand);

					distance.add(posBall);

					if (distance.length() <= ballRadius+0.015f) {
						//when ball gets grabbed a haptic feedback with strength 1500 is triggered for as long
						//as it stays grabbed (see also next else condition)
						renderPanel.triggerHapticPulse(renderPanel.controllerIndexHand, 3999);
						handTrafo.getRotationScale(initialHandRot);
						initialHandRot.invert();
						handTrafo.getRotationScale(previousHandRot);
						previousHandRot.invert();
						touched = true;
						gameStarted = true;
						activateGravity();
						previousHand = new Vector3f(posHand);
					}

				}
			} else {
				if (renderPanel.getTriggerTouched(renderPanel.controllerIndexHand)) {
					renderPanel.triggerHapticPulse(renderPanel.controllerIndexHand, 3999);
					Vector3f posHand = new Vector3f(handTrafo.m03, handTrafo.m13, handTrafo.m23);
					Vector3f distance = new Vector3f(posHand);
					distance.sub(previousHand);
					Matrix4f translation = new Matrix4f();
					translation.m03 = distance.x;
					translation.m13 = distance.y;
					translation.m23 = distance.z;
					ballTrafo.add(translation);
					Matrix3f rot = new Matrix3f();
					handTrafo.getRotationScale(rot);
					rot.mul(initialHandRot);
					ballTrafo.setRotation(rot);
					previousHand = new Vector3f(posHand);
					handTrafo.getRotationScale(previousHandRot);
					previousHandRot.invert();

				} else {
					Vector3f posHand = new Vector3f(handTrafo.m03, handTrafo.m13, handTrafo.m23);
					Vector3f distance = new Vector3f(posHand);
					distance.sub(previousHand);
					handTrafo.getRotationScale(rotSpeed);
					rotSpeed.mul(previousHandRot);

					ballSpeed = new Vector3f(distance);
					ballSpeed.scale(1f);
					touched = false;
					recentBallPos = new Vector3f(ballTrafo.m03, handTrafo.m13, handTrafo.m23);

				}
				recentRacketTransf = new Matrix4f(racketTrafo);
			}

			// update ball transformation matrix (right now this only shifts the
			// ball a bit down)
			// ballTrafo.setTranslation(throwingTranslationAccum);
//			System.out.print("figgdinimueter");
		}
		

		private Vector3f getRotAxis(Matrix3f rotMat)
		{
			return new Vector3f(rotMat.m21-rotMat.m12, rotMat.m02-
								rotMat.m20, rotMat.m10-rotMat.m01);
		}
		
		private float getRotAngle(Matrix3f rotMat)
		{
			float trace = rotMat.m00+rotMat.m11+rotMat.m22;
			return (float)(Math.acos(0.5*(trace-1)));
			
		}
		
		private void scaleRotSpeed(float scale)
		{
			float angle = getRotAngle(rotSpeed);
			angle *= scale;
			Vector3f u = getRotAxis(rotSpeed);
			rotSpeed.set(new AxisAngle4f(u.x,u.y,u.z,angle));
		}
		
		
		private void playSound(float volume, String urlStr){
			
			      try {
			        Clip clip = AudioSystem.getClip();
			        URL url =  new URL(urlStr);
			        AudioInputStream inputStream = AudioSystem.getAudioInputStream(
			          url);
			        clip.open(inputStream);
			        FloatControl gainControl = 
			        	    (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			        float actualValue = Math.max(gainControl.getMinimum(), volume);
			        actualValue = Math.min(actualValue, 6.0206f);
			        gainControl.setValue(6.0206f-actualValue); // Reduce volume by 10 decibels.
			        clip.start(); 
			       } catch (Exception e) {
//			       e.printStackTrace();
			      }
			    
		}


		/**
		 * The ball is reflected in a certain direction. This direction is 
		 * generally given by the normal vector. So the ball's speed is simply 
		 * mirrored on the given reflection vector. This doesn't only work for surfaces. 
		 * In order to bounce the ball of a line (edge) point (corner) just 
		 * calculate the ball's center minus the point where it hits the edge
		 * or point and that's your reflection vector.
		 * @param reflectionVector defines direction where ball should bounce to
		 */
		private void transformSpeed(Vector3f reflectionVector)
		{
			Vector3f n = new Vector3f(reflectionVector);
			n.normalize();
			Vector3f axis = new Vector3f();
			axis.cross(ballSpeed, n);
			float dist = n.dot(ballSpeed);
			if (dist < 0) {
				n.scale(2 *dist);
				ballSpeed.sub(n);
				ballSpeed.scale(ballRacketReflection);
				if(axis.length()!= 0)
				{
					float angle = axis.length();
//					angle *= 20*ballSpeed.length();
					Matrix3f rotRacket = new Matrix3f();
					rotRacket.set(new AxisAngle4f(axis.x,axis.y,axis.z,angle));
					rotSpeed.mul(rotRacket);
					scaleRotSpeed(ballWallReflection);
				}
			}
		}
		
		
		/**
		 *This method calculates where the hit point was the last through the current
		 *and the most recent transformation matrix of the racket and adds the 
		 *difference of these two points to the ball's speed vector. Additionally, a rotation
		 *is added to the ball given by where the racket hits the ball. 
		 * @param hitPoint Where the ball hits the racket
		 * @param racketTransf current transformation matrix of the racket
		 * @param dir ball center minus hit point
		 */
		private void addRacketSpeed(Vector3f hitPoint, Matrix4f racketTransf, Vector3f dir)
		{
			dir.normalize();
			Vector4f hit = new Vector4f(hitPoint.x, hitPoint.y, hitPoint.z, 1);
			Vector4f lastHit = new Vector4f(hit);
			Matrix4f racketInv = new Matrix4f(racketTransf);
			racketInv.invert();
			racketTransf.transform(hit);
			recentRacketTransf.transform(lastHit);
			Vector4f hitSpeed = new Vector4f(hit);
			hitSpeed.sub(lastHit);
			Vector3f hitSpeed3f = new Vector3f(hitSpeed.x, hitSpeed.y, hitSpeed.z);
			racketInv.transform(hitSpeed3f);
			Vector3f axis = new Vector3f();
			axis.cross(hitSpeed3f, dir);
			dir.scale(hitSpeed3f.dot(dir));
			dir.scale(0.05f);
			ballSpeed.add(dir);
			if(axis.length()!= 0)
			{
				float angle = axis.length();
				angle *= 20*ballSpeed.length();
				Matrix3f rotRacket = new Matrix3f();
				rotRacket.set(new AxisAngle4f(axis.x,axis.y,axis.z,angle));
				rotSpeed.mul(rotRacket);
				scaleRotSpeed(ballWallReflection);
			}
		}

		/**
		 * Checks if ball hits the racket. From all hit points, the one closest to the center
		 * is determined.
		 * @param ballTrafo Transformation matrix of ball in racket space
		 * @return point where ball hits racket, null if there's no intersection
		 */
		private Vector3f checkBallRacketIntersection(Matrix4f ballTrafo) {
			Vector3f hit = new Vector3f(0,0,0);
			Vector3f bestHit = new Vector3f(0,0,0);
			Vector3f center = new Vector3f(ballTrafo.m03, ballTrafo.m13, ballTrafo.m23);
			
			//firstly, the six planes are checked for intersection
			
			// right plane
			Vector3f min = new Vector3f(racketBoundsMax.x, racketBoundsMin.y, racketBoundsMin.z);
			Vector3f max = new Vector3f(racketBoundsMax.x, racketBoundsMax.y, racketBoundsMax.z);
			Vector3f normal = new Vector3f(1, 0, 0);
			hit = intWithPlane(center, normal, min, max);
			bestHit = hitExchange(hit, center, bestHit);

			// left plane
			min = new Vector3f(racketBoundsMin.x, racketBoundsMin.y, racketBoundsMin.z);
			max = new Vector3f(racketBoundsMin.x, racketBoundsMax.y, racketBoundsMax.z);
			normal = new Vector3f(-1, 0, 0);
			hit = intWithPlane(center, normal, min, max);
			bestHit = hitExchange(hit, center, bestHit);

			// Top plane
			min = new Vector3f(racketBoundsMin.x, racketBoundsMax.y, racketBoundsMin.z);
			max = new Vector3f(racketBoundsMax.x, racketBoundsMax.y, racketBoundsMax.z);
			normal = new Vector3f(0, 1, 0);
			hit = intWithPlane(center, normal, min, max);
			bestHit = hitExchange(hit, center, bestHit);
			
			// Bottom plane
			min = new Vector3f(racketBoundsMin.x, racketBoundsMin.y, racketBoundsMin.z);
			max = new Vector3f(racketBoundsMax.x, racketBoundsMin.y, racketBoundsMax.z);
			normal = new Vector3f(0, -1, 0);
			hit = intWithPlane(center, normal, min, max);
			bestHit = hitExchange(hit, center, bestHit);
			
			// Behind plane
			min = new Vector3f(racketBoundsMin.x, racketBoundsMin.y, racketBoundsMin.z);
			max = new Vector3f(racketBoundsMax.x, racketBoundsMax.y, racketBoundsMin.z);
			normal = new Vector3f(0, 0, -1);
			hit = intWithPlane(center, normal, min, max);
			bestHit = hitExchange(hit, center, bestHit);
			
			// Front plane, not finished yet
			min = new Vector3f(racketBoundsMin.x, racketBoundsMin.y, racketBoundsMax.z);
			max = new Vector3f(racketBoundsMax.x, racketBoundsMax.y, racketBoundsMax.z);
			normal = new Vector3f(0, 0, 1);
			hit = intWithPlane(center, normal, min, max);
			bestHit = hitExchange(hit, center, bestHit);
			
			
			//now the twelve edges are checked for intersection, POSSIBLE MISTAKE HERE THROUGH COPY PASTE
			
			//upper front edge	
			min = new Vector3f(racketBoundsMin.x, racketBoundsMax.y, racketBoundsMax.z);
			max = new Vector3f(racketBoundsMax.x, racketBoundsMax.y, racketBoundsMax.z);
			hit = intWithLine(center, min, max);
			bestHit = hitExchange(hit, center, bestHit);

			//upper right edge
			min = new Vector3f(racketBoundsMax.x, racketBoundsMax.y, racketBoundsMin.z);
			max = new Vector3f(racketBoundsMax.x, racketBoundsMax.y, racketBoundsMax.z);
			hit = intWithLine(center, min, max);
			bestHit = hitExchange(hit, center, bestHit);
			
			//upper back edge
			min = new Vector3f(racketBoundsMin.x, racketBoundsMax.y, racketBoundsMin.z);
			max = new Vector3f(racketBoundsMax.x, racketBoundsMax.y, racketBoundsMin.z);
			hit = intWithLine(center, min, max);
			bestHit = hitExchange(hit, center, bestHit);
			
			//upper left edge
			min = new Vector3f(racketBoundsMin.x, racketBoundsMax.y, racketBoundsMin.z);
			max = new Vector3f(racketBoundsMin.x, racketBoundsMax.y, racketBoundsMax.z);
			hit = intWithLine(center, min, max);
			bestHit = hitExchange(hit, center, bestHit);
			
			
			//middle right front edge
			min = new Vector3f(racketBoundsMax.x, racketBoundsMin.y, racketBoundsMax.z);
			max = new Vector3f(racketBoundsMax.x, racketBoundsMax.y, racketBoundsMax.z);
			hit = intWithLine(center, min, max);
			bestHit = hitExchange(hit, center, bestHit);
			
			//middle right back edge
			min = new Vector3f(racketBoundsMax.x, racketBoundsMin.y, racketBoundsMin.z);
			max = new Vector3f(racketBoundsMax.x, racketBoundsMax.y, racketBoundsMin.z);
			hit = intWithLine(center, min, max);
			bestHit = hitExchange(hit, center, bestHit);
			
			//middle left back edge
			min = new Vector3f(racketBoundsMin.x, racketBoundsMin.y, racketBoundsMin.z);
			max = new Vector3f(racketBoundsMin.x, racketBoundsMax.y, racketBoundsMin.z);
			hit = intWithLine(center, min, max);
			bestHit = hitExchange(hit, center, bestHit);
			
			//middle left front edge
			min = new Vector3f(racketBoundsMin.x, racketBoundsMin.y, racketBoundsMax.z);
			max = new Vector3f(racketBoundsMin.x, racketBoundsMax.y, racketBoundsMax.z);
			hit = intWithLine(center, min, max);
			bestHit = hitExchange(hit, center, bestHit);
			
			
			//lower front edge
			min = new Vector3f(racketBoundsMin.x, racketBoundsMin.y, racketBoundsMax.z);
			max = new Vector3f(racketBoundsMax.x, racketBoundsMin.y, racketBoundsMax.z);
			hit = intWithLine(center, min, max);
			bestHit = hitExchange(hit, center, bestHit);
			
			//lower right edge
			min = new Vector3f(racketBoundsMax.x, racketBoundsMin.y, racketBoundsMin.z);
			max = new Vector3f(racketBoundsMax.x, racketBoundsMin.y, racketBoundsMax.z);
			hit = intWithLine(center, min, max);
			bestHit = hitExchange(hit, center, bestHit);
			
			//lower back edge
			min = new Vector3f(racketBoundsMin.x, racketBoundsMin.y, racketBoundsMin.z);
			max = new Vector3f(racketBoundsMax.x, racketBoundsMin.y, racketBoundsMin.z);
			hit = intWithLine(center, min, max);
			bestHit = hitExchange(hit, center, bestHit);
			
			//lower left edge
			min = new Vector3f(racketBoundsMin.x, racketBoundsMin.y, racketBoundsMin.z);
			max = new Vector3f(racketBoundsMin.x, racketBoundsMin.y, racketBoundsMax.z);
			hit = intWithLine(center, min, max);
			bestHit = hitExchange(hit, center, bestHit);
			
			
			
			//now the corners are checked for intersection

			//upper right front corner
			min = new Vector3f(racketBoundsMax.x, racketBoundsMax.y, racketBoundsMax.z);
			hit = intWithPoint(center, min);
			bestHit = hitExchange(hit, center, bestHit);
			
			//upper right back corner
			min = new Vector3f(racketBoundsMax.x, racketBoundsMax.y, racketBoundsMin.z);
			hit = intWithPoint(center, min);
			bestHit = hitExchange(hit, center, bestHit);
			
			//upper left back corner
			min = new Vector3f(racketBoundsMin.x, racketBoundsMax.y, racketBoundsMin.z);
			hit = intWithPoint(center, min);
			bestHit = hitExchange(hit, center, bestHit);
			
			//upper left front corner
			min = new Vector3f(racketBoundsMin.x, racketBoundsMax.y, racketBoundsMax.z);
			hit = intWithPoint(center, min);
			bestHit = hitExchange(hit, center, bestHit);
			
			//lower right front corner
			min = new Vector3f(racketBoundsMax.x, racketBoundsMin.y, racketBoundsMax.z);
			hit = intWithPoint(center, min);
			bestHit = hitExchange(hit, center, bestHit);
			
			//lower right back corner
			min = new Vector3f(racketBoundsMax.x, racketBoundsMin.y, racketBoundsMin.z);
			hit = intWithPoint(center, min);
			bestHit = hitExchange(hit, center, bestHit);
			
			//lower left back corner
			min = new Vector3f(racketBoundsMin.x, racketBoundsMin.y, racketBoundsMin.z);
			hit = intWithPoint(center, min);
			bestHit = hitExchange(hit, center, bestHit);
			
			//lower left front corner
			min = new Vector3f(racketBoundsMin.x, racketBoundsMin.y, racketBoundsMax.z);
			hit = intWithPoint(center, min);
			bestHit = hitExchange(hit, center, bestHit);
			
			if(bestHit.length() != 0)
			{
				return bestHit;
			}
			
			
			return null;
		}

		/**This method determines the best hit point, i.e. the hit point
		 * that's closest to the ball's center
		 * @param hit the new hit point
		 * @param center the ball's center
		 * @param bestHit the best hit point so far
		 * @return the new best hit point which is either the best hit point or the new hit point
		 */
		private Vector3f hitExchange(Vector3f hit, Vector3f center, Vector3f bestHit) {
			if(hit.length() != 0)
			{
				Vector3f dist = new Vector3f(center);
				Vector3f distBest = new Vector3f(center);
				distBest.sub(bestHit);
				dist.sub(hit);
				if(dist.length() < distBest.length())
				{
					bestHit = new Vector3f(hit);
				}
			}
			return bestHit;
		}

		
		/**
		 * Calculates intersection of a ball given by its center and radius with a finite plane given 
		 * by the maximum and minimum point and its normal which should be a scale of the coordinate vectors
		 * @param center Center of the ball
		 * @param normal normal of the plane, it should have only one coordinate != 0
		 * @param min minimum of plane
		 * @param max maximum of plane
		 * @return the point where the ball hits the plane, (0,0,0) if it doesn't
		 */
		private Vector3f intWithPlane(Vector3f center, Vector3f normal, Vector3f min, Vector3f max)
		{
			Vector3f planePoint = new Vector3f(min);
			planePoint.sub(center);
			planePoint.negate();
			float dist = planePoint.dot(normal);
			if (dist > 0 && dist <= ballRadius) {
				Vector3f anchor = new Vector3f(center);
				normal.normalize();
				normal.scale(dist);
				anchor.sub(normal);
				boolean normx = normal.x != 0 && anchor.y >= min.y && anchor.y <= max.y && anchor.z >= min.z
						&& anchor.z <= max.z;
				boolean normy = normal.y != 0 && anchor.x >= min.x && anchor.x <= max.x 
						&& anchor.z >= min.z && anchor.z <= max.z;
				boolean normz = normal.z != 0 && anchor.y >= min.y && anchor.y <= max.y && anchor.x>= min.x
						&& anchor.x <= max.x;
				if (normx||normy||normz) {
					return anchor;
				}
				// return anchor;//should be a comment, only for testing
			}

			return new Vector3f(0,0,0);
		}
		
		/**
		 * Check if a ball with a given center and radius intersects with a finite line 
		 * (racket edges) given by its two end points min and max, still to be implemented MAYBE NOT CORRECT YET
		 * @param center center of the ball
		 * @param min minimum of line
		 * @param max maximum of line
		 * @return the point where the ball hits the line, (0,0,0) if it doesn't
		 */
		private Vector3f intWithLine(Vector3f center, Vector3f min, Vector3f max)
		{
			Vector3f lineDir = new Vector3f(max);
			lineDir.sub(min);
			float lineLength = lineDir.length();
			lineDir.normalize();
			Vector3f minToCenter = new Vector3f(center);
			minToCenter.sub(min);
			float centerProjLength = minToCenter.dot(lineDir);
			if(centerProjLength >= 0 && centerProjLength <= lineLength)
			{
				Vector3f anchor = new Vector3f(min);
				lineDir.scale(centerProjLength);
				anchor.add(lineDir);
				Vector3f centerToAnchor = new Vector3f(anchor);
				centerToAnchor.sub(center);
				if(centerToAnchor.length() <= ballRadius)
				{
					return anchor;
				}
			}
			return new Vector3f(0,0,0);
		}
		
		/**
		 * Checks if a ball given by its center and radius hits intersects
		 * with a point (corner of racket)
		 * @param center center of the ball
		 * @param point Point the ball might intersect with
		 * @return the point if ball intersects with it, (0,0,0) otherwise
		 */
		private Vector3f intWithPoint(Vector3f center, Vector3f point)
		{
			Vector3f dist = new Vector3f(center);
			dist.sub(point);
			if(dist.length() <= ballRadius)
			{
				return point;
			}
			return new Vector3f(0,0,0);
		}
		
		/**
		 * The idea is not to let the ball go inside the racket. So if an intersection
		 * occurs, the ball should get automatically positioned at a distance of 
		 * ballradius to the hitpoint for perfect collision and without it staying inside the racket
		 * It should function properly but the mistake is probably where it's called
		 * @param hitPoint the point where the ball hits the racket
		 * @param ballCenter the ball's center in racket coordinates
		 */
		private Vector4f positionBallOutsideRacket(Vector3f hitPoint, Vector4f ballCenter4f)
		{
			Vector3f ballCenter = new Vector3f(ballCenter4f.x, ballCenter4f.y, ballCenter4f.z);
			Vector3f dir = new Vector3f(ballCenter);
			dir.sub(hitPoint);
			dir.normalize();
			dir.scale(ballRadius+0.0006f);
			ballCenter = new Vector3f(hitPoint);
			ballCenter.add(dir);
			Vector4f res = new Vector4f(ballCenter);
			res.w = 1;
			return res;
		}
	}
	
	

	/**
	 * The main function opens a 3D rendering window, constructs a simple 3D
	 * scene, and starts a timer task to generate an animation.
	 */
	public static void main(String[] args) {
		// Make a render panel. The init function of the renderPanel
		// (see above) will be called back for initialization.
		renderPanel = new SimpleVRRenderPanel();

		// Make the main window of this application and add the renderer to it
		JFrame jframe = new JFrame("simple");
		jframe.setSize(1680, 1680);
		jframe.setLocationRelativeTo(null); // center of screen
		jframe.getContentPane().add(renderPanel.getCanvas());// put the canvas
																// into a JFrame
																// window

		// Add a mouse listener
		// renderPanel.getCanvas().addMouseListener(new SimpleMouseListener());
		renderPanel.getCanvas().setFocusable(true);

		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jframe.setVisible(true); // show window
	}
}
