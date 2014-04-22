package net.bayofrum.belldroid;

import java.io.IOException;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.media.SoundPool;
import android.view.Gravity;
import android.widget.TextView;

public class Bell extends TextView {

	private final int bellNumber, numberOfBells;
	
	private int place;
	public enum State { huntUp, huntDown, lead, lie, make };
	private State state = State.make;
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
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		this.soundId = soundId;

	}
	
	public int getPlace() {
		return this.place;
	}
	
	public Stroke getStroke() {
		return this.stroke;
	}
	
	public int ring() {
		switch (state) {
		case huntUp:
			if (place++ == 1)
				state = State.lead;
			break;
		case huntDown:
			if (place-- == this.numberOfBells)
				state = State.lie;
			break;
		case lead:
			state = State.huntUp;
			break;
		case lie:
			state = State.huntDown;
			break;
		case make:
			// state = State.huntUp;
			break;
		}
		
		switch (stroke) {
		case hand:
			// this.setText("H");
			this.setTextColor(Color.RED);
			stroke = Stroke.back;
			break;
		case back:
			// this.setText("B");
			this.setTextColor(Color.BLACK);
			stroke = Stroke.hand;
		}
		
		// Bong!
		
		soundPool.play(soundId, 0.99f, 0.99f, 1, 0, 1.0f);

		return place;
	}
	
}
