package audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import signal.Tools;

public class AudioSink {
	
	AudioFormat audioFormat;
	SourceDataLine sourceDataLine;
	boolean createStereoOutputFromMono = true;
	boolean stereoInput = false;
	
	/**
	 * Setup an output audio device.  Assumed to be stereo output.
	 * @param sampleRate
	 * @param stereoInput - true if we are providing a stream of stereo byte e.g. IQ, otherwise assumed to be mono in
	 * @throws LineUnavailableException
	 */
	public AudioSink(int sampleRate, boolean stereoInput) throws LineUnavailableException {		
		audioFormat = SoundCard.getAudioFormat(sampleRate);
		this.stereoInput = stereoInput;
		DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
		sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
		sourceDataLine.open(audioFormat);
		sourceDataLine.start();
	}
	
	public void write(double[] f) {
		byte[] audioData;

		if (audioFormat.getChannels() == 2) createStereoOutputFromMono = true;
		if (stereoInput) {
			createStereoOutputFromMono = false;  // set to false so we copy bytes one for one into the right channels
			audioData = new byte[f.length*audioFormat.getFrameSize()/2];
		} else {
			audioData = new byte[f.length*audioFormat.getFrameSize()];
		}

		Tools.getBytesFromDoubles(f, f.length, createStereoOutputFromMono, audioData); 
		write(audioData);
	}
	
	public void write(byte[] myData) {
		sourceDataLine.write(myData, 0, myData.length);
	}

	public void stop() {
		sourceDataLine.drain();
		sourceDataLine.close();
	}
}
