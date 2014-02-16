package com.flappyrevenge.Flappy;

import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.media.AudioManager;
import android.media.SoundPool;
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
				BitmapFactory.decodeResource(getResources(), R.drawable.dino),
				BitmapFactory.decodeResource(getResources(),
						R.drawable.background_bottom_grass),
				BitmapFactory.decodeResource(getResources(),
						R.drawable.pipe_rim_rocky), BitmapFactory.decodeResource(
						getResources(), R.drawable.pipe_body_rocky));
		relativeLayout.addView(bubbleView);
	}

	@Override
	protected void onResume() {
	    super.onResume();
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
		private boolean lost = false;

		// Background.
		private final Bitmap background;
		private final int backgroundHeight, groundY;

		private final int flappyX;
		private int flappyY;
		private int flappyV;
		private final int flappyG = 1;

		// Pipes.
		private final Bitmap pipeRim, pipeBody;
		private final int pipeRimWidth, pipeRimHeight, pipeBodyWidth,
				pipeBodyHeight = 1;
		private static final float pipeRimScreenRatio = 5.5f;
		private static final float pipeBodyScreenRatio = 6f;
		private static final float pipeGapScreenRatio = 7.5f;
		private static final int pace = 8;
		private static final int forgivenessOffset = 15; // How many pixels should you go into the pipe in order to lose.
		private int pipe1X, pipe2X, pipe1Y, pipe2Y;

		// Flappy.
		private final Bitmap flappy;
		private final int flappyHeight, flappyWidth;
		private int flappyAngle;
		private Matrix flappyMatrix;

		// Display.
		private final DisplayMetrics display;
		private final int displayWidth, displayHeight;
		private final float mX, mY;

		private float mDx;

		private float mDy;

		private final float mRotation;
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

		private final SoundPool sp;
		private final int crashSound;
		private final int flyingSound;
		private final int scoreSound;

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (lost) {
				lost = false;
				initializeFlappy();
			} else if (!started) {
				started = true;
			}

			sp.play(flyingSound, 20, 20, 1, 0, 1.0f);

			flappyY -= displayHeight / 25;
			flappyV = -10;
			flappyAngle = -30;
			// Matrix matrix = new Matrix();
			// matrix.postRotate(-20);
			// Bitmap.createBitmap(flappy, 0, 0, flappy.getWidth(),
			// flappy.getHeight(), matrix, true);
			return false;
		}

		public FlappyView(Context contextIn, Bitmap bitmapIn,
				Bitmap backgroundIn, Bitmap pipeRimIn, Bitmap pipeBodyIn) {
			super(contextIn);

			// Display.
			display = new DisplayMetrics();
			FlappyActivity.this.getWindowManager().getDefaultDisplay()
					.getMetrics(display);
			displayWidth = display.widthPixels;
			displayHeight = display.heightPixels;

			// Flappy.
			int flappyOriginalWidth = (int) getResources().getDimension(
					R.dimen.flappy_width);
			int flappyOriginalHeight = (int) getResources().getDimension(
					R.dimen.flappy_height);
			flappyWidth = displayWidth / 9;
			flappyHeight = flappyOriginalHeight * flappyWidth
					/ flappyOriginalWidth;
			flappy = Bitmap.createScaledBitmap(bitmapIn, flappyWidth,
					flappyHeight, false);
			flappyX = displayWidth / 4;

			// Background image scaling.
			int bgOriginalWidth = (int) getResources().getDimension(
					R.dimen.background_width);
			int bgOriginalHeight = (int) getResources().getDimension(
					R.dimen.background_height);
			int bgGroundHeight = (int) getResources().getDimension(
					R.dimen.ground_height);
			backgroundHeight = bgOriginalHeight * displayWidth
					/ bgOriginalWidth;
			background = Bitmap.createScaledBitmap(backgroundIn, displayWidth,
					backgroundHeight, false);
			int groundHeight = bgGroundHeight * displayWidth / bgOriginalWidth;
			groundY = displayHeight - groundHeight;

			// Pipes - Bottom.
			int pipeTopOriginalWidth = (int) getResources().getDimension(
					R.dimen.pipe_top_width);
			int pipeTopOriginalHeight = (int) getResources().getDimension(
					R.dimen.pipe_top_height);
			pipeRimWidth = Math.round(displayWidth / pipeRimScreenRatio);
			pipeRimHeight = pipeTopOriginalHeight * pipeRimWidth
					/ pipeTopOriginalWidth;
			pipeRim = Bitmap.createScaledBitmap(pipeRimIn, pipeRimWidth,
					pipeRimHeight, false);

			pipeBodyWidth = Math.round(displayWidth / pipeBodyScreenRatio);
			pipeBody = Bitmap.createScaledBitmap(pipeBodyIn, pipeBodyWidth,
					pipeBodyHeight, false);

			initializeFlappy();

			sp = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
		    crashSound = sp.load(contextIn, R.raw.crash, 1);
		    flyingSound = sp.load(contextIn, R.raw.dino_flying, 1);
		    scoreSound = sp.load(contextIn, R.raw.score, 1);

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
			// You should then notify the SurfaceHolder that you'd like to
			// receive
			// SurfaceHolder callbacks (from SurfaceHolder.Callback) by calling
			// addCallback() (pass it this).
			surfaceHolder.addCallback(this);
		}

		private void initializeFlappy() {
		  time = 0;

			// Reinitialize flappy's position.
			flappyY = displayHeight / 2;
			flappyV = 0;
			flappyAngle = -20;

			// Reinitialize pipe X's.
			pipe1X = displayWidth;
			pipe2X = displayWidth + (displayWidth + pipeRimWidth) / 2;
		}

		// Returns the edge of the top pipe.
		private int drawPipes(int pipeX, int pipeY, Canvas canvas) {
			int offset = (pipeRimWidth - pipeBodyWidth) / 2;
			int bottomPipeNeckY = groundY - pipeY;

			// Bottom pipe
			canvas.drawBitmap(pipeRim, pipeX, bottomPipeNeckY - pipeRimHeight,
					painter);
			Bitmap randomBottomBody = Bitmap.createScaledBitmap(pipeBody,
					pipeBodyWidth, pipeY, false);
			canvas.drawBitmap(randomBottomBody, pipeX + offset,
					bottomPipeNeckY, painter);

			// Top pipe
			int topPipeNeckY = bottomPipeNeckY - 2 * pipeRimHeight
					- Math.round(displayHeight / pipeGapScreenRatio);
			canvas.drawBitmap(pipeRim, pipeX, topPipeNeckY, painter);
			Bitmap randomTopBody = Bitmap.createScaledBitmap(pipeBody,
					pipeBodyWidth, topPipeNeckY, false);
			canvas.drawBitmap(randomTopBody, pipeX + offset, 0, painter);

			return topPipeNeckY + pipeRimHeight;
		}

		private void checkPipeEdges() {
			Random random = new Random();
			int newPipeY = random.nextInt(displayHeight / 3)
					+ (displayHeight / 10);

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

		boolean hitPipe(int pipeX, int topPipeEdge) {
			return (flappyX + flappyWidth >= pipeX && flappyX <= pipeX + pipeRimWidth - forgivenessOffset // X in between
			&& (flappyY < topPipeEdge - forgivenessOffset // hit his head
			|| flappyY + flappyHeight > topPipeEdge + Math.round(displayHeight / pipeGapScreenRatio))); // hit his ass
		}

		boolean hitGround() {
		  return flappyY > groundY;
		}

		private void drawFlappy(Canvas canvas) {
			// Draw background.
			canvas.drawColor(Color.rgb(161, 232, 234));
			canvas.drawBitmap(background, 0, displayHeight - backgroundHeight,
					painter);

			checkPipeEdges();
			int topPipe1Edge = drawPipes(pipe1X, pipe1Y, canvas);
			int topPipe2Edge = drawPipes(pipe2X, pipe2Y, canvas);

			// Draw initial game text.
			if (!started && !lost) {
			  Paint instructionPainter = new Paint();
	      instructionPainter.setARGB(220, 75, 70, 210);
	      instructionPainter.setTextSize(150);
	      instructionPainter.setTextAlign(Align.CENTER);
	      canvas.drawText("Tap to start", displayWidth / 2,
	          displayHeight / 3, instructionPainter);
			}

			// Draw the game over screen text.
			if (lost) {
		    // Keeping track of the score.
		    SharedPreferences sp = getSharedPreferences("flappy_prefs", Activity.MODE_PRIVATE);
		    int highScore = sp.getInt("high_score", -1);
		    if (score > highScore) {
		      highScore = score;
		      SharedPreferences.Editor editor = sp.edit();
		      editor.putInt("high_score", highScore);
		      editor.commit();
		    }

		    // Game over message and stuff.
			  Paint lostPainter = new Paint();
        lostPainter.setARGB(225, 205, 133, 63);
        lostPainter.setARGB(220, 75, 70, 210);
        lostPainter.setTextSize(150);
        lostPainter.setTextAlign(Align.CENTER);
        // lostPainter.setShadowLayer(5, 5, 5, 222);
        canvas.drawText("Game Over", displayWidth / 2,
            displayHeight / 5, lostPainter);
        canvas.drawText("Tap to Replay", displayWidth / 2, displayHeight - (displayHeight/2), lostPainter);
        lostPainter.setTextSize(100);
        lostPainter.setARGB(250, 220, 70, 70);
        lostPainter.setTextAlign(Align.CENTER);
        canvas.drawText("High score: " + highScore, displayWidth / 2, displayHeight - (displayHeight/8), lostPainter);
			}


			// Draw Flappy.
			flappyMatrix = new Matrix();
			flappyMatrix.postRotate(flappyAngle);
			Bitmap flappyBitMap = Bitmap.createBitmap(flappy, 0, 0,
					flappy.getWidth(), flappy.getHeight(), flappyMatrix, true);
			canvas.drawBitmap(flappyBitMap, flappyX, flappyY, painter);

			// Draw score.
			int prevScore = score;
			score = Math.max(0,
					(time * pace - (displayWidth / 4 + pipeRimWidth / 4))
							/ ((displayWidth + pipeRimWidth) / 2));
			if (score != prevScore) {
				sp.play(scoreSound, 100, 100, 1, 0, 1.0f);
			}
			Paint scorePainter = new Paint();
			scorePainter.setARGB(220, 255, 70, 70);
			scorePainter.setTextSize(100);
			canvas.drawText(Integer.toString(score), displayWidth / 2,
					displayHeight / 10, scorePainter);

			// Check if Flappy is dead.
			if (!lost && (hitPipe(pipe1X, topPipe1Edge) || hitPipe(pipe2X, topPipe2Edge) || hitGround())) {
				started = false;
				lost = true;

				sp.play(crashSound, 100, 100, 1, 0, 1.0f);
			}

			// Move everything.
			if (started && !lost) {
				time++;
				pipe1X -= pace;
				pipe2X -= pace;
				flappyV += flappyG;
				flappyY += flappyV;
				flappyAngle = flappyV > 0 ? flappyV : 3 * flappyV;
			}
		}

		private boolean move() {
			// mX += mDx;
			// mY += mDy;
			// if (mX < 0 - flappyHeightAndWidth
			// || mX > displayHeight + flappyHeightAndWidth
			// || mY < 0 - flappyHeightAndWidth
			// || mY > displayWidth + flappyHeightAndWidth) {
			// return false;
			// } else {
			// return true;
			// }
			return true;
		}

		// Then override each of the SurfaceHolder.Callback methods inside your
		// SurfaceView class.
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
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
							// SurfaceHolder and do your necessary drawing upon
							// it.
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