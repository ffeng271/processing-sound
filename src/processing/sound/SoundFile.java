package processing.sound;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.jsyn.data.FloatSample;
import com.jsyn.unitgen.VariableRateDataReader;
import com.jsyn.unitgen.VariableRateMonoReader;
import com.jsyn.unitgen.VariableRateStereoReader;
import com.jsyn.util.SampleLoader;

import fr.delthas.javamp3.Sound;
import processing.core.PApplet;

// calls to amp(), pan() etc affect both the LAST initiated and still running sample, AND all subsequently started ones
/**
* This is a Soundfile Player which allows to play back and manipulate soundfiles. Supported formats are: WAV, AIF/AIFF, MP3.
* @webref sound
* @param parent PApplet: typically use "this"
* @param path Full path to the file or filename for the data path
**/
public class SoundFile extends SoundObject {

	// array of UnitVoices each with a VariableRateStereoReader in
//	private static VoiceAllocator PLAYERS = new VoiceAllocator(null);

	private static Map<String,FloatSample> SAMPLECACHE = new HashMap<String,FloatSample>();

	private FloatSample sample;
	// the soundfile class always has to maintain a pointer to its last player object for panning etc?
	private VariableRateDataReader player;

	private int startFrame = 0;

	// the original library only printed an error if the file wasn't found,
	// but then later threw a NullPointerException when trying to play() the file.
	// this implementation will already through an Exception upon failing to load.
	public SoundFile(PApplet parent, String path) throws IOException {
		super(parent);
		
		// TODO what if it's a URL?
		File f = new File(path);
		this.sample = SAMPLECACHE.get(f.getCanonicalPath());

		if (this.sample == null) {
			InputStream fin = PApplet.createInput(f);

			// if PApplet.createInput() can't find the file or URL, it prints
			// an error message and fin returns null. In this case we can just
			// return this dysfunctional SoundFile object without initialising further
			if (fin == null) {
				return;
			}

			try {
				// load WAV or AIF using JSyn
				this.sample = SampleLoader.loadFloatSample(fin);
			} catch (IOException e) {
				// try parsing as mp3
				Sound mp3 = new Sound(fin);
				try {
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					mp3.decodeFullyInto(os); // this call is expensive
					float data[] = new float[os.size() / 2];
					SampleLoader.decodeLittleI16ToF32(os.toByteArray(), 0, os.size(), data, 0);
					this.sample = new FloatSample(data, mp3.isStereo() ? 2 : 1);
				} finally {
					mp3.close();
				}
			}
			SAMPLECACHE.put(f.getCanonicalPath(), this.sample);
		}
		
		if (this.channels() == 2) {
			this.player = new VariableRateStereoReader();
		} else {
			this.player = new VariableRateMonoReader();
		}

		// needs to be set explicitly
		this.player.rate.set(this.sampleRate());
		this.circuit.setSource(this.player.output);

		// unlike the Oscillator and Noise classes, the sample player units can
		// always stay connected to the JSyn synths, since they make no noise
		// as long as their dataQueue is empty
		super.play(); // doesn't actually start playback, just adds the (silent) units
	}

	public void amp(float amp) {
		// TODO check in [0, 1]
		this.player.amplitude.set(amp);
	}

	/**
	* Returns the number of channels in the soundfile.
	* @webref sound
	* @return Returns the number of channels in the soundfile as an int.
	**/
	public int channels() {
		return this.sample.getChannelsPerFrame();
	}

	/**
	* Cues the playhead to a fixed position in the soundfile. Note that the time parameter supports only integer values. 
	* @webref sound
	* @param time Position to start from as integer seconds.
	**/
	// methCla-based library only supported int here!
	public void cue(float time) {
		this.setStartFrame(time);
	}

	/**
	* Returns the duration of the the soundfile.
	* @webref sound
	* @return Returns the duration of the file in seconds.
	**/
	public float duration() {
		return (float) (this.frames() / this.sample.getFrameRate());
	}

	/**
	* Returns the number of frames/samples of the sound file.
	* @webref sound
	* @return Returns the number of samples of the soundfile as an int.
	**/
	public int frames() {
		return this.sample.getNumFrames();
	}
	
	private boolean setStartFrame(float time) {
		if (time < 0) {
			PApplet.println("Gotta be positive");
			return false;
		}
		int startFrame = Math.round(this.sampleRate() * time);
		if (startFrame >= this.frames()) {
			PApplet.println("Can't cue past of end of sample (total duration is " + this.duration() + "s)");
			return false;
		}
		this.startFrame = startFrame;
		return true;
	}

	/**
	* Jump to a specific position in the file while continuing to play.
	* @webref sound
	* @param time Position to jump to as a float in seconds.
	**/
	public void jump(float time) {
		if (this.setStartFrame(time)) {
			this.stop();
			this.play(); // TODO what if the file wasn't playing when jump() was called?
		}
	}
	
	/**
	* Starts the playback of a soundfile to loop.
	* @webref sound
	**/	
	public void loop() {
		this.player.dataQueue.queueLoop(this.sample,
				this.startFrame,
				this.frames() - this.startFrame);
	}

	public void loop(float rate) {
		this.rate(rate);
		this.loop();
	}

	public void loop(float rate, float amp) {
		this.rate(rate);
		this.amp(amp);
		this.loop();
	}

	public void loop(float rate, float pos, float amp) {
		this.pan(pos);
		this.loop(rate, amp);
	}

	public void loop(float rate, float pos, float amp, float add) {
		this.add(add);
		this.loop(rate, pos, amp);
	}

	public void loop(float rate, float pos, float amp, float add, float cue) {
		this.cue(cue);
		this.loop(rate, pos, amp, add);
	}

	// panning wasn't originally supported for stereo files, but it is now

	/**
	* Starts the playback of a soundfile. Only plays the soundfile once.
	* @webref sound
	**/
	public void play() {
		// when called on a soundfile already running, the original library triggered a second (concurrent) playback
		this.player.dataQueue.queue(this.sample,
				this.startFrame,
				this.frames() - this.startFrame);
	}

	/**
	* Change the playback rate of the soundfile.
	* @webref sound
	* @param rate This method changes the playback rate of the soundfile. 1 is the original speed. 0.5 is half speed and one octave down. 2 is double the speed and one octave up. 
	**/
	public void rate(float rate) {
		// TODO check rate > 0
		// 1.0 = original
		this.player.rate.set(this.sampleRate() * rate);
	}

	/**
	* Returns the sample rate of the soundfile.
	* @webref sound
	* @return Returns the sample rate of the soundfile as an int.
	**/
	public int sampleRate() {
		return (int) Math.round(this.sample.getFrameRate());
	}

	/**
	* Set multiple parameters at once
	* @webref sound
	* @param rate The playback rate of the original file. 
	* @param pos The panoramic position of the player as a float from -1.0 to 1.0.
	* @param amp The amplitude of the player as a value between 0.0 and 1.0.
	* @param add A value for modulating other audio signals.
	**/
	public void set(float rate, float pos, float amp, float add) {
		this.rate(rate);
		this.pan(pos);
		this.amp(amp);
		this.add(add);
	}

	/**
	* Stops the player
	* @webref sound
	**/
	public void stop() {
		this.player.dataQueue.clear();
	}

	// new methods go here

	/**
	 * Get current sound file playback position in seconds.
	 * @return The current position of the sound file playback in seconds (TODO seconds at which sample rate?)
	 */
	public float position() {
		// progress in sample seconds or current-rate-playback seconds??
		return this.player.dataQueue.getFrameCount() / (float) this.sampleRate();
	}

	/**
	 * Get current sound file playback position in percent.
	 * @return The current position of the sound file playback in percent (a value between 0 and 100).
	 */
	public float percent() {
		return 100f * this.player.dataQueue.getFrameCount() / (float) this.frames();
	}

	/**
	 * Check whether this soundfile is currently playing.
	 * @return `true` if the soundfile is currently playing, `false` if it is not.
	 */
	public boolean isPlaying() {
		// overrides the SoundObject's default implementation
		return this.player.dataQueue.hasMore();
	}
}
