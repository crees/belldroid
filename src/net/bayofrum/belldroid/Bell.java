package net.bayofrum.belldroid;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.media.SoundPool;
import android.view.Gravity;
import android.widget.TextView;

@SuppressLint("ViewConstructor")
public class Bell extends TextView {

	private final int bellNumber, numberOfBells;
	
	private int place;
	public enum Stroke { hand, back };
	private Stroke stroke = Stroke.hand;
		
	public final int soundId;
	private final SoundPool soundPool;
	
	public Bell(Context context, int bellNumber, int maxNumberOfBells,
			int numberOfBells, SoundPool soundPool) {
		super(context);
		this.bellNumber = bellNumber;
		this.numberOfBells = numberOfBells;
		this.soundPool = soundPool;
		place = this.bellNumber;
		this.setText(Integer.toString(this.bellNumber));
		this.setGravity(
				Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);		
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
	
	public int getPlace() {
		return this.place;
	}
	
	public void setPlace(int p) {
		this.place = p;
	}
	
	public Stroke getStroke() {
		return this.stroke;
	}
	
	public void moveUp() {
		this.place++;
	}
	
	public void moveDown() {
		this.place--;
	}
	
	public int ring() {
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
		setText(String.valueOf(this.bellNumber) + String.valueOf(place));

		// Bong!
		soundPool.play(soundId, 0.99f, 0.99f, 1, 0, 1.0f);

		return place;
	}
	
}
