package com.flappyrevenge.Flappy;

import java.io.IOException;

import android.content.Context;
import android.media.MediaPlayer;

public class FlappyAudio {

  MediaPlayer mp;
  Context context;

       public FlappyAudio(Context ct) {
       this.context = ct;
  }
      public void playClick(){
          mp = MediaPlayer.create(context, R.raw.dino_flying);  
          try {
            mp.prepare();
          } catch (IllegalStateException e) {
            e.printStackTrace();
          } catch (IOException e) {
            e.printStackTrace();
          }
          mp.start();
      }
  
}
