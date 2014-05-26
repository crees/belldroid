package net.bayofrum.belldroid;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.media.SoundPool;
import android.os.Handler;
import android.view.Gravity;
import android.widget.Button;

@SuppressLint("ViewConstructor")
public class Bell extends Button {

	private final int bellNumber;
	
	private int place;
	public enum Stroke { hand, back };
	private Stroke stroke = Stroke.hand;

	public final int soundId;
	private final SoundPool soundPool;
	
	/**
	 * Returns a Bell object, based on TextView.  Best
	 * stored as a ring of bells; e.g. Bell[] bells.
	 * 
	 * @param context	context
	 * @param bellNumber	The number for this Bell object
	 * @param maxNumberOfBells	Maximum number of Bell objects -- used
	 * 							to find the sound file
	 * @param numberOfBells		Also used to find the sound file
	 * @param soundPool			a SoundPool to which the Bell sound is added
	 */
	public Bell(Context context, int bellNumber, int maxNumberOfBells,
			int numberOfBells, SoundPool soundPool) {
		super(context);
		this.bellNumber = bellNumber;
		this.soundPool = soundPool;
		place = this.bellNumber;
		this.setText(Integer.toString(this.bellNumber));
		this.setGravity(
				Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);		
		this.setClickable(true);
		int soundId = 0;
		AssetManager assets = context.getAssets();
		try {
			AssetFileDescriptor soundfile = assets.openFd("piano-based/" +
					String.valueOf(
							bellNumber + maxNumberOfBells - numberOfBells
					) + ".ogg");
			soundId = soundPool.load(soundfile, 1);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		this.soundId = soundId;
	}
	
	public int getNumber() {
		return this.bellNumber;
	}
	
	public int getPlace() {
		return this.place;
	}
	
	public void setPlace(int p) {
		this.place = p;
	}
	
	public boolean atHandStroke() {
		return this.stroke == Stroke.hand;
	}
	
	public boolean atBackStroke() {
		return this.stroke == Stroke.back;
	}
	
	public void moveUp() {
		this.place++;
	}
	
	public void moveDown() {
		this.place--;
	}
	
	public int ring(Handler bonger, int delay) {
		switch (stroke) {
		case hand:
			this.setTextColor(Color.RED);
			stroke = Stroke.back;
			break;
		case back:
			this.setTextColor(Color.BLACK);
			stroke = Stroke.hand;
			break;
		}

		// Bong!
		bonger.postDelayed(new Runnable() {
			@Override
			public void run() {
				soundPool.play(soundId, 0.99f, 0.99f, 1, 0, 1.0f);
			}
		}, delay);

		return place;
	}
	
}
