package org.andresoviedo.app.model3D.controller;

import org.andresoviedo.app.model3D.model.Object3DData;
import org.andresoviedo.app.model3D.services.SceneLoader;
import org.andresoviedo.app.model3D.view.ModelRenderer;
import org.andresoviedo.app.model3D.view.ModelSurfaceView;
import org.andresoviedo.app.util.math.Math3DUtils;

import android.graphics.PointF;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

public class TouchController {

	private static final String TAG = TouchController.class.getName();

	private static final int TOUCH_STATUS_ZOOMING_CAMERA = 1;
	private static final int TOUCH_STATUS_ROTATING_CAMERA = 4;
	private static final int TOUCH_STATUS_MOVING_WORLD = 5;

	private final ModelSurfaceView view;
	private final ModelRenderer mRenderer;

	int pointerCount = 0;
	float x1 = Float.MIN_VALUE;
	float y1 = Float.MIN_VALUE;
	float x2 = Float.MIN_VALUE;
	float y2 = Float.MIN_VALUE;
	float dx1 = Float.MIN_VALUE;
	float dy1 = Float.MIN_VALUE;
	float dx2 = Float.MIN_VALUE;
	float dy2 = Float.MIN_VALUE;

	float length = Float.MIN_VALUE;
	float previousLength = Float.MIN_VALUE;
	float currentPress1 = Float.MIN_VALUE;
	float currentPress2 = Float.MIN_VALUE;

	float rotation = 0;
	int currentSquare = Integer.MIN_VALUE;

	boolean isOneFixedAndOneMoving = false;
	boolean fingersAreClosing = false;
	boolean isRotating = false;

	boolean gestureChanged = false;
	private boolean moving = false;
	private boolean simpleTouch = false;
	private boolean rotationTouch = false;
	private long lastActionTime;
	private int touchDelay = -2;
	private int touchStatus = -1;

	private float previousX1;
	private float previousY1;
	private float previousX2;
	private float previousY2;
	float[] previousVector = new float[4];
	float[] vector = new float[4];
	float[] rotationVector = new float[4];
	private float previousRotationSquare;

	private ScaleGestureDetector mScaleDetector;

	public TouchController(ModelSurfaceView view, ModelRenderer renderer) {
		super();
		this.view = view;
		this.mRenderer = renderer;
		mScaleDetector = new ScaleGestureDetector(view.getContext(), new ScaleListener());
	}

	private class ScaleListener
			extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mRenderer.getCamera().Tobj /= detector.getScaleFactor();

			// Don't let the object get too small or too large.
			mRenderer.getCamera().Tobj = Math.max(2.f, Math.min(mRenderer.getCamera().Tobj, 100.0f));

			return true;
		}
	}

	public synchronized boolean onTouchEvent(MotionEvent motionEvent) {
		// MotionEvent reports input details from the touch screen
		// and other input controls. In this case, you are only
		// interested in events where the touch position changed.

		mScaleDetector.onTouchEvent(motionEvent);

		switch (motionEvent.getActionMasked()) {

			// press up
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_HOVER_EXIT:
			case MotionEvent.ACTION_OUTSIDE:
				// this to handle "1 simple touch"
				Log.d(TAG, "ACTION_UP...");
				if (lastActionTime > SystemClock.uptimeMillis() - 250) {
					simpleTouch = true;
				} else {
					rotationTouch = false;
				}
				rotationTouch = false;
				moving = false;
				break;

			// press down
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
			case MotionEvent.ACTION_HOVER_ENTER:
				Log.d(TAG, "ACTION_DOWN...");
				gestureChanged = true;
				touchDelay = 0;
				lastActionTime = SystemClock.uptimeMillis();
				simpleTouch = false;
				rotationTouch = false;
				break;

			// move
			case MotionEvent.ACTION_MOVE:
				Log.d(TAG, "ACTION_MOVE...");
				moving = true;
				simpleTouch = false;
				rotationTouch = true;
				touchDelay++;
				break;
			default:
				Log.w(TAG, "Unknown state: " + motionEvent.getAction());
				gestureChanged = true;
		}

		pointerCount = motionEvent.getPointerCount();

		if (pointerCount == 1) {							// finger 1

			previousX1 = x1;
			previousY1 = y1;

			x1 = motionEvent.getX();
			y1 = motionEvent.getY();


			dx1 = x1 - previousX1;
			dy1 = y1 - previousY1;
		}

		if (pointerCount == 1 && simpleTouch) {
			Log.d("TouchController", "SimpleTouch");
			// calculate the world coordinates where the user is clicking (near plane and far plane)
			float[] hit1 = unproject(x1, y1, 0);
			float[] hit2 = unproject(x1, y1, 1);
			// check if the ray intersect any of our objects and select the nearer
			selectObjectImpl(hit1, hit2);

			// test for object rotation
			mRenderer.getCamera().setObjectPosition();
		} else if (pointerCount == 1 && rotationTouch && touchDelay >= 2) {
			if (Math.abs(x1 - previousX1) > 5 || Math.abs(y1-previousY1) >5 ) {
				Log.d("TouchController", "RotationTouch");
				float[] rotVec = new float[4];
				float width = this.view.getWidth();
				float height = this.view.getHeight();

				Log.d("TouchController", "prevX: " + previousX1 + " prevY: " + previousY1
						+ " X: " + x1 + " Y: " + y1);
				getRotVecTrackBall(rotVec, width, height,
						previousX1 - width/2, height/2 - previousY1, x1 - width/2, height/2 - y1);

				mRenderer.getCamera().setObjectRotation(rotVec);
			}
		}

		view.requestRender();

		return true;

	}

	private void getRotVecTrackBall(float[] rotVec, float width, float height,
									float x1, float y1, float x2, float y2 ) {



		float r = height/2;
		float z1, z2;

		if (x1*x1+y1*y1 <= r*r/2) {
			z1 = (float) Math.sqrt(r*r - x1*x1 - y1*y1);
		} else {
			z1 = (float) (r*r/2/Math.sqrt(x1*x1+y1*y1));
		}

		if (x2*x2+y2*y2 <= r*r/2) {
			z2 = (float) Math.sqrt(r*r - x2*x2 - y2*y2);
		} else {
			z2 = (float) (r*r/2/Math.sqrt(x2*x2+y2*y2));
		}

		// map
		float[] TR = new float[16];
		Matrix.transposeM(TR, 0, mRenderer.getCamera().objRotationMatrix, 0);
		Matrix.multiplyMV(rotVec, 0, TR, 0, new float[]{x1, y1, z1, 1}, 0);
		x1 = rotVec[0]; y1 = rotVec[1]; z1 = rotVec[2];
		Matrix.multiplyMV(rotVec, 0, TR, 0, new float[]{x2, y2, z2, 1}, 0);
		x2 = rotVec[0]; y2 = rotVec[1]; z2 = rotVec[2];

		float len1 = (float) Math.sqrt(x1*x1+y1*y1+z1*z1);
		float len2 = (float) Math.sqrt(x2*x2+y2*y2+z2*z2);

		rotVec[0] = (float) Math.acos((x1*x2+y1*y2+z1*z2)/len1/len2);
		rotVec[1] = y1*z2 - z1*y2;
		rotVec[2] = z1*x2 - x1*z2;
		rotVec[3] = x1*y2 - y1*x2;

		Log.d("TouchController", "R: " + rotVec[0] + " x: " + rotVec[1] + " y: " + rotVec[2] +
				" z: " + rotVec[3]);

	}

	/**
	 * Get the nearest object intersecting the specified ray and selects it
	 * 
	 * @param nearPoint
	 *            the near point in world coordinates
	 * @param farPoint
	 *            the far point in world coordinates
	 */
	private void selectObjectImpl(float[] nearPoint, float[] farPoint) {
		SceneLoader scene = view.getModelActivity().getScene();
		if (scene == null) {
			return;
		}
		Object3DData objectToSelect = null;
		float objectToSelectDistance = Integer.MAX_VALUE;
		for (Object3DData obj : scene.getObjects()) {
			float distance = Math3DUtils.calculateDistanceOfIntersection(nearPoint, farPoint, obj.getPosition(), 1f);
			if (distance != -1) {
				Log.d(TAG, "Hit object " + obj.getId() + " at distance " + distance);
				if (distance < objectToSelectDistance) {
					objectToSelectDistance = distance;
					objectToSelect = obj;
				}
			}
		}
		if (objectToSelect != null) {
			Log.i(TAG, "Selected object " + objectToSelect.getId() + " at distance " + objectToSelectDistance);
			if (scene.getSelectedObject() == objectToSelect) {
				scene.setSelectedObject(null);
			} else {
				scene.setSelectedObject(objectToSelect);
			}
		}
	}

	public float[] unproject(float rx, float ry, float rz) {
		float[] xyzw = { 0, 0, 0, 0 };

		ry = (float) mRenderer.getHeight() - ry;

		int[] viewport = { 0, 0, mRenderer.getWidth(), mRenderer.getHeight() };

		GLU.gluUnProject(rx, ry, rz, mRenderer.getModelViewMatrix(), 0, mRenderer.getModelProjectionMatrix(), 0,
				viewport, 0, xyzw, 0);

		xyzw[0] /= xyzw[3];
		xyzw[1] /= xyzw[3];
		xyzw[2] /= xyzw[3];
		xyzw[3] = 1;
		return xyzw;
	}
}

class TouchScreen {

	// these matrices will be used to move and zoom image
	private android.graphics.Matrix matrix = new android.graphics.Matrix();
	private android.graphics.Matrix savedMatrix = new android.graphics.Matrix();
	// we can be in one of these 3 states
	private static final int NONE = 0;
	private static final int DRAG = 1;
	private static final int ZOOM = 2;
	private int mode = NONE;
	// remember some things for zooming
	private PointF start = new PointF();
	private PointF mid = new PointF();
	private float oldDist = 1f;
	private float d = 0f;
	private float newRot = 0f;
	private float[] lastEvent = null;

	public boolean onTouch(View v, MotionEvent event) {
		// handle touch events here
		ImageView view = (ImageView) v;
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			savedMatrix.set(matrix);
			start.set(event.getX(), event.getY());
			mode = DRAG;
			lastEvent = null;
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			oldDist = spacing(event);
			if (oldDist > 10f) {
				savedMatrix.set(matrix);
				midPoint(mid, event);
				mode = ZOOM;
			}
			lastEvent = new float[4];
			lastEvent[0] = event.getX(0);
			lastEvent[1] = event.getX(1);
			lastEvent[2] = event.getY(0);
			lastEvent[3] = event.getY(1);
			d = getRotation(event);
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			mode = NONE;
			lastEvent = null;
			break;
		case MotionEvent.ACTION_MOVE:
			if (mode == DRAG) {
				matrix.set(savedMatrix);
				float dx = event.getX() - start.x;
				float dy = event.getY() - start.y;
				matrix.postTranslate(dx, dy);
			} else if (mode == ZOOM) {
				float newDist = spacing(event);
				if (newDist > 10f) {
					matrix.set(savedMatrix);
					float scale = (newDist / oldDist);
					matrix.postScale(scale, scale, mid.x, mid.y);
				}
				if (lastEvent != null && event.getPointerCount() == 3) {
					newRot = getRotation(event);
					float r = newRot - d;
					float[] values = new float[9];
					matrix.getValues(values);
					float tx = values[2];
					float ty = values[5];
					float sx = values[0];
					float xc = (view.getWidth() / 2) * sx;
					float yc = (view.getHeight() / 2) * sx;
					matrix.postRotate(r, tx + xc, ty + yc);
				}
			}
			break;
		}

		view.setImageMatrix(matrix);
		return true;
	}

	/**
	 * Determine the space between the first two fingers
	 */
	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	/**
	 * Calculate the mid point of the first two fingers
	 */
	private void midPoint(PointF point, MotionEvent event) {
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}

	/**
	 * Calculate the degree to be rotated by.
	 * 
	 * @param event
	 * @return Degrees
	 */
	public static float getRotation(MotionEvent event) {
		double dx = (event.getX(0) - event.getX(1));
		double dy = (event.getY(0) - event.getY(1));
		double radians = Math.atan2(Math.abs(dy), Math.abs(dx));
		double degrees = Math.toDegrees(radians);
		return (float) degrees;
	}

	public static float getRotation360(MotionEvent event) {
		double dx = (event.getX(0) - event.getX(1));
		double dy = (event.getY(0) - event.getY(1));
		double radians = Math.atan2(Math.abs(dy), Math.abs(dx));
		double degrees = Math.toDegrees(radians);
		int square = 1;
		if (dx > 0 && dy == 0) {
			square = 1;
		} else if (dx > 0 && dy < 0) {
			square = 1;
		} else if (dx == 0 && dy < 0) {
			square = 2;
			degrees = 180 - degrees;
		} else if (dx < 0 && dy < 0) {
			square = 2;
			degrees = 180 - degrees;
		} else if (dx < 0 && dy == 0) {
			square = 3;
			degrees = 180 + degrees;
		} else if (dx < 0 && dy > 0) {
			square = 3;
			degrees = 180 + degrees;
		} else if (dx == 0 && dy > 0) {
			square = 4;
			degrees = 360 - degrees;
		} else if (dx > 0 && dy > 0) {
			square = 4;
			degrees = 360 - degrees;
		}
		return (float) degrees;
	}

	public static int getSquare(MotionEvent event) {
		double dx = (event.getX(0) - event.getX(1));
		double dy = (event.getY(0) - event.getY(1));
		int square = 1;
		if (dx > 0 && dy == 0) {
			square = 1;
		} else if (dx > 0 && dy < 0) {
			square = 1;
		} else if (dx == 0 && dy < 0) {
			square = 2;
		} else if (dx < 0 && dy < 0) {
			square = 2;
		} else if (dx < 0 && dy == 0) {
			square = 3;
		} else if (dx < 0 && dy > 0) {
			square = 3;
		} else if (dx == 0 && dy > 0) {
			square = 4;
		} else if (dx > 0 && dy > 0) {
			square = 4;
		}
		return square;
	}
}
