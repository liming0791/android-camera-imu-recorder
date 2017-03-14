package org.andresoviedo.app.model3D.view;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.andresoviedo.app.camera.CameraManager;
import org.andresoviedo.app.model3D.entities.Camera;
import org.andresoviedo.app.model3D.model.Object3D;
import org.andresoviedo.app.model3D.model.Object3DBuilder;
import org.andresoviedo.app.model3D.model.Object3DData;
import org.andresoviedo.app.model3D.services.SceneLoader;
import org.andresoviedo.app.model3D.util.GLUtil;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.citrus.slam.util.CalibrationHelper;

public class ModelRenderer implements GLSurfaceView.Renderer {

	private final static String TAG = ModelRenderer.class.getName();

	// 3D window (parent component)
	private ModelSurfaceView main;
	// width of the screen
	private int width;
	// height of the screen
	private int height;
	// Out point of view handler
	private Camera camera;

	private Object3DBuilder drawer;
	// The loaded textures
	private Map<byte[], Integer> textures = new HashMap<byte[], Integer>();
	// The corresponding opengl bounding boxes and drawer
	private Map<Object3DData, Object3DData> boundingBoxes = new HashMap<Object3DData, Object3DData>();
	// The corresponding opengl bounding boxes
	private Map<Object3DData, Object3DData> normals = new HashMap<Object3DData, Object3DData>();

	// 3D matrices to project our 3D world
	float[] modelProjectionMatrix = new float[16];
	float[] NDCMatrix = new float[16];
	float[] PerspMatrix = new float[16];


	float[] viewMatrix = new float[16];
	float[] modelMatrix = new float[16];
	float[] modelViewMatrix = new float[16];
	// mvpMatrix is an abbreviation for "Model View Projection Matrix"
	private final float[] mvpMatrix = new float[16];

	private float preW = 0;
	private float preH = 0;

	/**
	 * Construct a new renderer for the specified surface view
	 * 
	 * @param modelSurfaceView
	 *            the 3D window
	 */
	public ModelRenderer(ModelSurfaceView modelSurfaceView) {
		this.main = modelSurfaceView;
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		// Set the background frame color
		GLES20.glClearColor(0.f, 0.f, 0.f, 0.f);

		// Use culling to remove back faces.
		// Don't remove back faces so we can see them
		// GLES20.glEnable(GLES20.GL_CULL_FACE);

		// Enable depth testing for hidden-surface elimination.
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		// Enable blending for combining colors when there is transparency
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		// Lets create our 3D world components
		camera = new Camera();

		// This component will draw the actual models using OpenGL
		drawer = new Object3DBuilder();
	}

	// Camera Axis:
	//   X - Right, Y - Up, Z - Back
	// Image Origin:
	//   Bottom Left
	// Caution: Principal point defined with respect to image origin (0,0) at
	//          top left of top-left pixel (not center, and in different frame
	//          of reference to projection function image)
	void ProjectionMatrixRUB_BottomLeft(float[] P, int w, int h, float fu, float fv,
										float u0, float v0, float zNear, float zFar )
	{
		// http://www.songho.ca/opengl/gl_projectionmatrix.html
		final float L = +(u0) * zNear / -fu;
		final float T = +(v0) * zNear / fv;
		final float R = -(w-u0) * zNear / -fu;
		final float B = -(h-v0) * zNear / fv;

		for (int i = 0; i < 16; i++ ) {
			P[i] = 0;
		}

		P[0*4+0] = 2 * zNear / (R-L);
		P[1*4+1] = 2 * zNear / (T-B);
		P[2*4+2] = -(zFar +zNear) / (zFar - zNear);
		P[2*4+0] = (R+L)/(R-L);
		P[2*4+1] = (T+B)/(T-B);
		P[2*4+3] = -1.0f;
		P[3*4+2] =  -(2*zFar*zNear)/(zFar-zNear);

	}

	private void SetProjectionMatrix() {
		CameraManager cameraManager =
				main.getModelActivity().getCameraManager();
		if (cameraManager == null) return;
		android.hardware.Camera.Size previewSize =
				cameraManager.getPreviewSize();
		if (previewSize == null) return;
		preW = previewSize.height;
		preH = previewSize.width;

		modelProjectionMatrix = CalibrationHelper.getProjectionMatrix(preH, preW, 0.1f, 10000.f);
		float tmp[] = new float[4];
		for (int i = 0; i < 4; i++)
			tmp[i] = modelProjectionMatrix[i*4];
		for (int i = 0; i < 4; i++)
			modelProjectionMatrix[i*4] = modelProjectionMatrix[1+i*4];
		for (int i = 0; i < 4; i++)
			modelProjectionMatrix[1+i*4] = -tmp[i];

		//ProjectionMatrixRUB_BottomLeft(modelProjectionMatrix, (int)preW, (int)preH, 1500, 1500,
		//		preW/2, preH/2, 0.1f, 10000);
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		this.width = width;
		this.height = height;

		Log.i("ModelRenderer", "Surface width: " + width + " height: " + height);

		// Adjust the viewport based on geometry changes, such as screen rotation
		GLES20.glViewport(0, 0, width, height);

		// INFO: Set the camera position (View matrix)
		// The camera has 3 vectors (the position, the vector where we are looking at, and the up position (sky)
		Matrix.setLookAtM(viewMatrix, 0, 0, 0, 0, 0, 0, -1, 0, 1, 0);

		// the projection matrix is the 3D virtual space (cube) that we want to project
		float ratio = (float) width / height;
		Log.d(TAG, "projection: [" + -ratio + "," + ratio + ",-1,1]-near/far[1,10]");
		//Matrix.frustumM(modelProjectionMatrix, 0, -ratio*2, ratio*2, -1*2, 1*2, 1f, 10f);

		if (preW == 0 && preH == 0)
			SetProjectionMatrix();

		// Calculate the projection and view transformation
		Matrix.multiplyMM(mvpMatrix, 0, modelProjectionMatrix, 0, modelViewMatrix, 0);
	}

	@Override
	public void onDrawFrame(GL10 unused) {

		// Draw background color
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		if (preW == 0 && preH == 0)
			SetProjectionMatrix();

		// recalculate mvp matrix according to where we are looking at now
		if (camera.hasChanged()) {

			// set viewMatrix
			Matrix.setLookAtM(viewMatrix, 0,  0, 0, 0,  0, 0, -1,  0, 1, 0);

			Matrix.setIdentityM(modelMatrix, 0);

			// rotation for camera
			Matrix.rotateM(modelMatrix, 0, (float) (-camera.radians * 180 / 3.1415926),
					camera.Xaxis, camera.Yaxis, camera.Zaxis);

			// position for model
			Matrix.rotateM(modelMatrix, 0, (float) (camera.objRadians * 180 / 3.1415926),
					camera.Xobj, camera.Yobj, camera.Zobj);

			// translate for model
			Matrix.translateM(modelMatrix, 0, 0, 0, -camera.Tobj);

			// rotation for model
			//Matrix.rotateM(modelMatrix, 0, (float) (camera.Robj * 180 / 3.1415926), camera.XRobj, camera.YRobj, camera.ZRobj);
			float[] newModelMatrix = new float[16];
			Matrix.multiplyMM(newModelMatrix, 0, modelMatrix, 0, camera.objRotationMatrix, 0);

			Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, newModelMatrix, 0);
			Matrix.multiplyMM(mvpMatrix, 0, modelProjectionMatrix, 0, modelViewMatrix, 0);
			camera.setChanged(false);
		}

		SceneLoader scene = main.getModelActivity().getScene();
		if (scene == null) {
			// scene not ready
			return;
		}

		float[] lightPosInEyeSpace = new float[]{5, 5, 5, 1};
		if (scene.isDrawLighting()) {
//			float[] lightPos = scene.getLightPos();
//			lightPosInEyeSpace = new float[4]{};
//
//			// Do a complete rotation every 10 seconds.
//			long time = SystemClock.uptimeMillis() % 10000L;
//			float angleInDegrees = (360.0f / 10000.0f) * ((int) time);
//
//			Object3DData lightPoint = Object3DBuilder.buildPoint(lightPos);
//			lightPoint.setRotation(new float[] { 0, angleInDegrees, 0 });
//
//			// calculate light matrix
//			float[] mMatrixLight = new float[16];
//			// Calculate position of the light. Rotate and then push into the distance.
//			Matrix.setIdentityM(mMatrixLight, 0);
//			// Matrix.translateM(mMatrixLight, 0, lightPos[0], lightPos[1], lightPos[2]);
//			Matrix.rotateM(mMatrixLight, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
//			// Matrix.translateM(mMatrixLight, 0, 0.0f, 0.0f, 2.0f);
//			float[] mLightPosInWorldSpace = new float[4];
//			Matrix.multiplyMV(mLightPosInWorldSpace, 0, mMatrixLight, 0, lightPos, 0);
//
//			Matrix.multiplyMV(lightPosInEyeSpace, 0, modelViewMatrix, 0, mLightPosInWorldSpace, 0);
//			float[] mvMatrixLight = new float[16];
//			Matrix.multiplyMM(mvMatrixLight, 0, modelViewMatrix, 0, mMatrixLight, 0);
//			float[] mvpMatrixLight = new float[16];
//			Matrix.multiplyMM(mvpMatrixLight, 0, modelProjectionMatrix, 0, mvMatrixLight, 0);
//
//			drawer.getPointDrawer().draw(lightPoint, modelProjectionMatrix, modelViewMatrix, -1, lightPosInEyeSpace);
//			// // Draw a point to indicate the light.
//			// GLES20.glUseProgram(mPointProgramHandle);
//			// drawLight(mvpMatrixLight, lightPos);
		}

		for (Object3DData objData : scene.getObjects()) {
			try {
				boolean changed = objData.isChanged();

				Object3D drawerObject = drawer.getDrawer(objData, scene.isDrawTextures(), scene.isDrawLighting());

				Integer textureId = textures.get(objData.getTextureData());
				if (textureId == null && objData.getTextureData() != null) {
					ByteArrayInputStream textureIs = new ByteArrayInputStream(objData.getTextureData());
					textureId = GLUtil.loadTexture(textureIs);
					textureIs.close();
					textures.put(objData.getTextureData(), textureId);
				}

				if (scene.isDrawWireframe() && objData.getDrawMode() != GLES20.GL_POINTS
						&& objData.getDrawMode() != GLES20.GL_LINES && objData.getDrawMode() != GLES20.GL_LINE_STRIP
						&& objData.getDrawMode() != GLES20.GL_LINE_LOOP) {
					// Only draw wireframes for objects having faces (triangles)
					drawerObject.draw(objData, modelProjectionMatrix, modelViewMatrix, GLES20.GL_LINE_LOOP, 3,
							textureId != null ? textureId : -1, lightPosInEyeSpace);
				} else {

					drawerObject.draw(objData, modelProjectionMatrix, modelViewMatrix,
							textureId != null ? textureId : -1, lightPosInEyeSpace);
				}

				// Draw bounding box
				if (scene.isDrawBoundingBox() || scene.getSelectedObject() == objData) {
					Object3DData boundingBoxData = boundingBoxes.get(objData);
					if (boundingBoxData == null || changed) {
						boundingBoxData = Object3DBuilder.buildBoundingBox(objData);
						boundingBoxes.put(objData, boundingBoxData);
					}
					Object3D boundingBoxDrawer = drawer.getBoundingBoxDrawer();
					boundingBoxDrawer.draw(boundingBoxData, modelProjectionMatrix, modelViewMatrix, -1, null);
				}

				// Draw bounding box
				if (scene.isDrawNormals()) {
					Object3DData normalData = normals.get(objData);
					if (normalData == null || changed) {
						normalData = Object3DBuilder.buildFaceNormals(objData);
						if (normalData != null) {
							// it can be null if object isnt made of triangles
							normals.put(objData, normalData);
						}
					}
					if (normalData != null) {
						Object3D normalsDrawer = drawer.getFaceNormalsDrawer();
						normalsDrawer.draw(normalData, modelProjectionMatrix, modelViewMatrix, -1, null);
					}
				}
				// TODO: enable this only when user wants it
				// obj3D.drawVectorNormals(result, modelViewMatrix);
			} catch (IOException ex) {
				Toast.makeText(main.getModelActivity().getApplicationContext(),
						"There was a problem creating 3D object", Toast.LENGTH_LONG).show();
			}
		}
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public float[] getModelProjectionMatrix() {
		return modelProjectionMatrix;
	}

	public float[] getModelViewMatrix() {
		return modelViewMatrix;
	}

	public Camera getCamera() {
		return camera;
	}
}