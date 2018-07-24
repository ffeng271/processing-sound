package processing.sound;

import com.jsyn.unitgen.TriangleOscillator;

import processing.core.PApplet;

/**
 * This is a simple triangle (or "saw") wave oscillator
 * @webref sound
 * @param parent PApplet: typically use "this"
 **/
public class TriOsc extends Oscillator<TriangleOscillator> {
	public TriOsc(PApplet theParent) {
		super(theParent, new TriangleOscillator());
	}
}
