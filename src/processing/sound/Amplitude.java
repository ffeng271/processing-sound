package processing.sound;

import com.jsyn.ports.UnitOutputPort;
import com.jsyn.unitgen.PeakFollower;

import processing.core.PApplet;

/**
 * This is a volume analyzer. It calculates the root mean square of the
 * amplitude of each audio block and returns that value.
 * 
 * @webref sound
 * @param parent
 *            PApplet: typically use "this"
 */
public class Amplitude extends Analyzer {

	private PeakFollower follower;

	public Amplitude(PApplet parent) {
		super(parent);
		this.follower = new PeakFollower();
		this.follower.halfLife.set(0.1);
	}

	protected void setInput(UnitOutputPort input) {
		Engine.getEngine().add(this.follower);
		this.follower.start();
		this.follower.input.connect(input);
	}

	/**
	 * Queries a value from the analyzer and returns a float between 0. and 1.
	 * 
	 * @webref sound
	 * @return amp An amplitude value between 0-1.
	 **/
	public float analyze() {
		// TODO check if input exists, print warning if not
		return (float) this.follower.current.getValue();
	}
}
