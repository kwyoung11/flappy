package com.flappyrevenge.Flappy;

import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
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
				BitmapFactory.decodeResource(getResources(), R.drawable.pipe_rim),
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
	  private boolean started = false;
	  
		// Background.
		private final Bitmap background;
		private final int backgroundHeight, sandY;
		
		private final int flappyX;
    private int flappyY;
    private int flappyV;
    private final int flappyG = 1;
		
		// Pipes.
		private final Bitmap pipeRim, pipeBody;
		private final int pipeRimWidth, pipeRimHeight, pipeBodyWidth, pipeBodyHeight = 1;
		private static final float pipeRimScreenRatio = 5.5f;
		private static final float pipeBodyScreenRatio = 6f;
	  private static final float pipeGapScreenRatio = 5.5f;
		private static final int pace = 8;
		private int pipe1X, pipe2X, pipe1Y, pipe2Y;

		// Flappy.
		private final Bitmap flappy;
		private final int flappyHeight, flappyWidth;
		private int flappyAngle;
		private Matrix flappyMatrix;

		// Display.
		private final DisplayMetrics display;
		private final int displayWidth, displayHeight;
		private float mX, mY, mDx, mDy, mRotation;
		private final SurfaceHolder surfaceHolder;
		private final Paint painter = new Paint();
		private int score = 0;
		private int time;
		

		// Inside your SurfaceView class is also a good place to define your
		// secondary Thread class, which will perform all the drawing procedures
		// to your Canvas.
		private Thread mDrawingThread;

		private static final int MOVE_STEP = 1;
		private static final float ROT_STEP = 1.0f;

		int mod(int a, int b) {
			return (a % b + b) % b;
		}
		
		@Override
    public boolean onTouchEvent(MotionEvent event) { 
		  if (!started) {
		    started = true;
		    time = 0;
		  }
		  
		  flappyY -= displayHeight / 25;
		  flappyV = -10;
		  flappyAngle = -30;
//		 Matrix matrix = new Matrix();
//     matrix.postRotate(-20);
//     Bitmap.createBitmap(flappy, 0, 0, flappy.getWidth(), flappy.getHeight(), matrix, true);
		 return false;
		}

		public FlappyView(Context contextIn, Bitmap bitmapIn, Bitmap backgroundIn, Bitmap pipeRimIn,
				Bitmap pipeBodyIn) {
			super(contextIn);
			
			// Display.
      display = new DisplayMetrics();
      FlappyActivity.this.getWindowManager().getDefaultDisplay()
          .getMetrics(display);
      displayWidth = display.widthPixels;
      displayHeight = display.heightPixels;
      
			// Flappy.
      int flappyOriginalWidth = (int) getResources().getDimension(R.dimen.flappy_width);
			int flappyOriginalHeight = (int) getResources().getDimension(R.dimen.flappy_height);
			flappyWidth = displayWidth / 9;
			flappyHeight = flappyOriginalHeight * flappyWidth / flappyOriginalWidth;
			flappy = Bitmap.createScaledBitmap(bitmapIn,
					flappyWidth, flappyHeight, false);
			flappyX = displayWidth/4;
			flappyY = displayHeight/2;
			flappyV = 0;
			flappyAngle = -20;

			// Background image scaling.
			int bgOriginalWidth = (int) getResources().getDimension(R.dimen.background_width);
			int bgOriginalHeight = (int) getResources().getDimension(R.dimen.background_height);
			int bgSandHeight = (int) getResources().getDimension(R.dimen.sand_height);
			backgroundHeight = bgOriginalHeight * displayWidth / bgOriginalWidth;
			background = Bitmap.createScaledBitmap(backgroundIn,
					displayWidth, backgroundHeight, false);
			int sandHeight = bgSandHeight * displayWidth / bgOriginalWidth;
			sandY = displayHeight - sandHeight;

			// Pipes - Bottom.
			int pipeTopOriginalWidth = (int) getResources().getDimension(R.dimen.pipe_top_width);
			int pipeTopOriginalHeight = (int) getResources().getDimension(R.dimen.pipe_top_height);
			pipeRimWidth = Math.round(displayWidth / pipeRimScreenRatio);
			pipeRimHeight = pipeTopOriginalHeight * pipeRimWidth / pipeTopOriginalWidth;
			pipeRim = Bitmap.createScaledBitmap(pipeRimIn, pipeRimWidth, pipeRimHeight, false);

			pipeBodyWidth = Math.round(displayWidth / pipeBodyScreenRatio);
			pipeBody = Bitmap.createScaledBitmap(pipeBodyIn, pipeBodyWidth, pipeBodyHeight, false);

			// Pipes - Top.

			
			// Pipe X's.
			pipe1X = displayWidth;
			pipe2X = displayWidth + (displayWidth + pipeRimWidth) / 2;

			Random r = new Random();
			mX = r.nextInt(displayHeight);
			mY = r.nextInt(displayWidth);
			mDx = (float) r.nextInt(displayHeight) / displayHeight;
			mDx *= r.nextInt(2) == 1 ? MOVE_STEP : -1 * MOVE_STEP;
			mDy = (float) r.nextInt(displayWidth) / displayWidth;
			mDy *= r.nextInt(2) == 1 ? MOVE_STEP : -1 * MOVE_STEP;
			mRotation = 1.0f;

			painter.setAntiAlias(true);

			// Instead of handling the Surface object directly, you should
			// handle it via a SurfaceHolder. So, when your SurfaceView is
			// initialized, get the SurfaceHolder by calling getHolder().
			surfaceHolder = getHolder();
			// You should then notify the SurfaceHolder that you'd like to receive
			// SurfaceHolder callbacks (from SurfaceHolder.Callback) by calling
			// addCallback() (pass it this).
			surfaceHolder.addCallback(this);
		}
		
		private void drawPipes(int pipeX, int pipeY, Canvas canvas) {
		  int offset = (pipeRimWidth - pipeBodyWidth) / 2;
		  int bottomPipeNeckY = sandY - pipeY;
		 
		// Bottom pipe 
      canvas.drawBitmap(pipeRim, pipeX, bottomPipeNeckY - pipeRimHeight, painter);
      Bitmap randomBottomBody = Bitmap.createScaledBitmap(pipeBody, pipeBodyWidth, pipeY, false);
      canvas.drawBitmap(randomBottomBody, pipeX + offset, bottomPipeNeckY, painter);
      
      // Top pipe 
      int topPipeNeckY = bottomPipeNeckY - 2 * pipeRimHeight - Math.round(displayHeight / pipeGapScreenRatio);
      canvas.drawBitmap(pipeRim, pipeX, topPipeNeckY, painter);
      Bitmap randomTopBody = Bitmap.createScaledBitmap(pipeBody, pipeBodyWidth, topPipeNeckY, false);
      canvas.drawBitmap(randomTopBody, pipeX + offset, 0, painter);
		}
		
		private void checkPipeEdges() {
		  Random random = new Random();
      int newPipeY = random.nextInt(displayHeight / 3) + (displayHeight / 10);
      
      // Pipe 1
      if (pipe1X <= -pipeRimWidth)
        pipe1X = displayWidth;
      if (pipe1X >= displayWidth)
        pipe1Y = newPipeY;
      
      // Pipe 2
      if (pipe2X <= -pipeRimWidth) 
        pipe2X = displayWidth;
      if (pipe2X >= displayWidth)
        pipe2Y = newPipeY;
		}

		private void drawFlappy(Canvas canvas) {
			// Draw background.
			canvas.drawColor(Color.rgb(112, 196, 206));
			canvas.drawBitmap(background, 0, displayHeight - backgroundHeight, painter);

			checkPipeEdges();
		  drawPipes(pipe1X, pipe1Y, canvas);
	    drawPipes(pipe2X, pipe2Y, canvas);

			// Draw Flappy.
      flappyMatrix = new Matrix();
      flappyMatrix.postRotate(flappyAngle);
      Bitmap flappyBitMap = Bitmap.createBitmap(flappy, 0, 0, flappy.getWidth(), flappy.getHeight(), flappyMatrix, true);
			canvas.drawBitmap(flappyBitMap, flappyX, flappyY, painter);
			
			// Draw score.
			score = Math.max(0, (time * pace - (displayWidth / 4 + pipeBodyWidth)) / ((displayWidth + pipeRimWidth) / 2));
			Paint scorePainter = new Paint();
			scorePainter.setARGB(220, 255, 70, 70);
			scorePainter.setTextSize(100);
			canvas.drawText(Integer.toString(score), displayWidth/2, displayHeight/10, scorePainter);
			
      // Move everything.
      if (started) {
        time++;
        pipe1X -= pace;
        pipe2X -= pace;
  			flappyV += flappyG;
  			flappyY += flappyV;
  			flappyAngle = flappyV > 0 ? flappyV : 3 * flappyV;
      }
    }

		private boolean move() {
//			mX += mDx;
//			mY += mDy;
//			if (mX < 0 - flappyHeightAndWidth
//					|| mX > displayHeight + flappyHeightAndWidth
//					|| mY < 0 - flappyHeightAndWidth
//					|| mY > displayWidth + flappyHeightAndWidth) {
//				return false;
//			} else {
//				return true;
//			}
		  return true;
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
						canvas = surfaceHolder.lockCanvas();
						if (null != canvas) {
							// You can now take the Canvas given to you by the
							// SurfaceHolder and do your necessary drawing upon it.
							drawFlappy(canvas);
							// Once you're done drawing with the Canvas, call
							// unlockCanvasAndPost(), passing it your Canvas
							// object. The Surface will now draw the Canvas as
							// you left it.
							surfaceHolder.unlockCanvasAndPost(canvas);
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