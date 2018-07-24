package processing.sound;

import processing.core.PApplet;

/**
 * AudioDevice allows for configuring the audio server. If you need a low
 * latency server you can reduce the buffer size. Allowed values are power of 2.
 * For changing the sample rate pass the appropriate value in the constructor.
 * 
 * @webref sound
 * @param parent
 *            PApplet: typically use "this"
 * @param sampleRate
 *            Sets the sample rate (default 44100).
 * @param bufferSize
 *            Sets the buffer size (not used).
 **/
public class AudioDevice {

	public AudioDevice(PApplet theParent, int sampleRate, int bufferSize) {
		Engine.printWarning("the AudioDevice class is not used anymore, please have a look at the functions of the new Engine class instead.");
		Engine e = new Engine(theParent);
		e.sampleRate(sampleRate);
		// bufferSize is ignored - the parameter was necessary for the original library's FFT to work
	}
}
