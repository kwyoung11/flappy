package com.flappyrevenge.Flappy;

import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.RelativeLayout;

public class FlappyActivity extends Activity {

	FlappyView mBubbleView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.frame);
		final FlappyView bubbleView = new FlappyView(getApplicationContext(),
				BitmapFactory.decodeResource(getResources(), R.drawable.bird),
				BitmapFactory.decodeResource(getResources(), R.drawable.background_bottom),
				BitmapFactory.decodeResource(getResources(), R.drawable.pipe_top),
				BitmapFactory.decodeResource(getResources(), R.drawable.pipe_body));

		relativeLayout.addView(bubbleView);
	}

	// The SurfaceView is a special subclass of View that offers a dedicated
	// drawing surface within the View hierarchy. The aim is to offer this
	// drawing surface to an application's secondary thread, so that the
	// application isn't required to wait until the system's View hierarchy is
	// ready to draw. Instead, a secondary thread that has reference to a
	// SurfaceView can draw to its own Canvas at its own pace.

	// To begin, you need to create a new class that extends SurfaceView.
	// The class should also implement SurfaceHolder.Callback. This subclass is
	// an interface that will notify you with information about the underlying
	// Surface, such as when it is created, changed, or destroyed.

	private class FlappyView extends SurfaceView implements
			SurfaceHolder.Callback {

		// Background.
		private final Bitmap mBackground;
		private final int mBackgroundHeight;

		// Pipes.
		private final Bitmap mPipeTop, mPipeBody;
		private final int mPipeTopWidth, mPipeTopHeight, mPipeBodyWidth;
		private int mPipeBodyHeight;
		private static final float pipeTopScreenRatio = 5.5f;
		private static final float pipeBodyScreenRatio = 6f;

		// Flappy.
		private final Bitmap mFlappy;
		private final int mFlappyHeightAndWidth, mFlappyHeightAndWidthAdj;

		// Display.
		private final DisplayMetrics mDisplay;
		private final int mDisplayWidth, mDisplayHeight;
		private float mX, mY, mDx, mDy, mRotation;
		private final SurfaceHolder mSurfaceHolder;
		private final Paint mPainter = new Paint();

		// Inside your SurfaceView class is also a good place to define your
		// secondary Thread class, which will perform all the drawing procedures
		// to your Canvas.
		private Thread mDrawingThread;

		private static final int MOVE_STEP = 1;
		private static final float ROT_STEP = 1.0f;

		public FlappyView(Context context, Bitmap bitmap, Bitmap background, Bitmap pipeTop,
				Bitmap pipeBody) {
			super(context);

			// Flappy.
			mFlappyHeightAndWidth = (int) getResources().getDimension(
					R.dimen.image_height);
			this.mFlappy = Bitmap.createScaledBitmap(bitmap,
					mFlappyHeightAndWidth, mFlappyHeightAndWidth, false);

			mFlappyHeightAndWidthAdj = mFlappyHeightAndWidth / 2;

			// Display.
			mDisplay = new DisplayMetrics();
			FlappyActivity.this.getWindowManager().getDefaultDisplay()
					.getMetrics(mDisplay);
			mDisplayWidth = mDisplay.widthPixels;
			mDisplayHeight = mDisplay.heightPixels;

			// Background image scaling.
			int bgOriginalWidth = (int) getResources().getDimension(R.dimen.background_width);
			int bgOriginalHeight = (int) getResources().getDimension(R.dimen.background_height);
			mBackgroundHeight = bgOriginalHeight * mDisplayWidth / bgOriginalWidth;
			this.mBackground = Bitmap.createScaledBitmap(background,
					mDisplayWidth, mBackgroundHeight, false);

			// Pipes.
			int pipeTopOriginalWidth = (int) getResources().getDimension(R.dimen.pipe_top_width);
			int pipeTopOriginalHeight = (int) getResources().getDimension(R.dimen.pipe_top_height);
			mPipeTopWidth = Math.round(mDisplayWidth / pipeTopScreenRatio);
			mPipeTopHeight = pipeTopOriginalHeight * mPipeTopWidth / pipeTopOriginalWidth;
			this.mPipeTop = Bitmap.createScaledBitmap(pipeTop, mPipeTopWidth, mPipeTopHeight, false);

			int pipeBodyOriginalWidth = (int) getResources().getDimension(R.dimen.pipe_body_width);
			int pipeBodyOriginalHeight = (int) getResources().getDimension(R.dimen.pipe_body_height);
			mPipeBodyWidth = Math.round(mDisplayWidth / pipeBodyScreenRatio);
			mPipeBodyHeight = pipeBodyOriginalHeight * mPipeBodyWidth / pipeBodyOriginalWidth;
			if (mPipeBodyHeight == 0) {
				mPipeBodyHeight = 1;
			}
			this.mPipeBody = Bitmap.createScaledBitmap(pipeBody, mPipeBodyWidth, mPipeBodyHeight, false);

			Random r = new Random();
			mX = r.nextInt(mDisplayHeight);
			mY = r.nextInt(mDisplayWidth);
			mDx = (float) r.nextInt(mDisplayHeight) / mDisplayHeight;
			mDx *= r.nextInt(2) == 1 ? MOVE_STEP : -1 * MOVE_STEP;
			mDy = (float) r.nextInt(mDisplayWidth) / mDisplayWidth;
			mDy *= r.nextInt(2) == 1 ? MOVE_STEP : -1 * MOVE_STEP;
			mRotation = 1.0f;

			mPainter.setAntiAlias(true);

			// Instead of handling the Surface object directly, you should
			// handle it via a SurfaceHolder. So, when your SurfaceView is
			// initialized, get the SurfaceHolder by calling getHolder().
			mSurfaceHolder = getHolder();
			// You should then notify the SurfaceHolder that you'd like to receive
			// SurfaceHolder callbacks (from SurfaceHolder.Callback) by calling
			// addCallback() (pass it this).
			mSurfaceHolder.addCallback(this);
		}

		private void drawFlappy(Canvas canvas) {
			// Draw backround.
			canvas.drawColor(Color.rgb(112, 196, 206));
			canvas.drawBitmap(mBackground, 0, mDisplayHeight - mBackgroundHeight, mPainter);

			// Draw pipes
			canvas.drawBitmap(mPipeTop, 0, 0, mPainter);

			// Draw bird.
			mRotation += ROT_STEP;
			canvas.rotate(mRotation, mY + mFlappyHeightAndWidthAdj, mX
					+ mFlappyHeightAndWidthAdj);
			canvas.drawBitmap(mFlappy, mY, mX, mPainter);
		}

		private boolean move() {
			mX += mDx;
			mY += mDy;
			if (mX < 0 - mFlappyHeightAndWidth
					|| mX > mDisplayHeight + mFlappyHeightAndWidth
					|| mY < 0 - mFlappyHeightAndWidth
					|| mY > mDisplayWidth + mFlappyHeightAndWidth) {
				return false;
			} else {
				return true;
			}
		}

		// Then override each of the SurfaceHolder.Callback methods inside your SurfaceView class.
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			mDrawingThread = new Thread(new Runnable() {
				@Override
				public void run() {
					Canvas canvas = null;
					// Perform this sequence of locking and unlocking the canvas
					// each time you want to redraw.
					while (!Thread.currentThread().isInterrupted() && move()) {
						// In order to draw to the Surface Canvas from within
						// your second thread, you must pass the thread your
						// SurfaceHandler and retrieve the Canvas with
						// lockCanvas().
						canvas = mSurfaceHolder.lockCanvas();
						if (null != canvas) {
							// You can now take the Canvas given to you by the
							// SurfaceHolder and do your necessary drawing upon it.
							drawFlappy(canvas);
							// Once you're done drawing with the Canvas, call
							// unlockCanvasAndPost(), passing it your Canvas
							// object. The Surface will now draw the Canvas as
							// you left it.
							mSurfaceHolder.unlockCanvasAndPost(canvas);
						}
					}
				}
			});
			mDrawingThread.start();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			if (null != mDrawingThread)
				mDrawingThread.interrupt();
		}

	}
}