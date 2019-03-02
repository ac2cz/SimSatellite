package sim;

import java.io.IOException;
import java.util.Random;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import audio.AudioSink;
import audio.SoundCard;
import audio.WavFile;
import signal.Complex;
import signal.ComplexOscillator;
import signal.CosOscillator;
import uk.me.g4dpz.satellite.GroundStationPosition;
import uk.me.g4dpz.satellite.SatPos;
import uk.me.g4dpz.satellite.Satellite;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

public class Simulate implements Runnable {

	boolean running = true;
	int sampleRate = 192000;
	double fadeDepth = 1; // 1 = full fade, 0 = no fade.
	int hzPerSecond = 1;//33; // Hz to add each second for the pass - 0 = 0ff
	int carrier; // the nominal carrier used to calculate the amount of Doppler
	int fadePeriod; // the number of seconds for 1 revolution
	
	final String[] testTLE = {
	"AO-92",
	"1 43137U 18004AC  19038.71282424  .00000880  00000-0  40233-4 0  9994",
	"2 43137  97.5044 106.7195 0009368   6.1208 354.0142 15.23181223 59552"};
	
	final String[] testTLE2 = {
			"AO-85",
			"1 40967U 15058D   19038.47750005  .00000208  00000-0  39309-4 0 05909",
			"2 40967 064.7741 196.8388 0211383 243.0309 114.9070 14.75868137088377"};
	TLE tle;
	GroundStationPosition groundStation;
	
	public Simulate(int sampleRate, double fadeDepth, int fadePeriod, int carrier) {
		this.sampleRate = sampleRate;
		this.fadeDepth = fadeDepth;
		this.carrier = carrier;
		this.fadePeriod = fadePeriod;
		
		float lat = 45.4920f;
		float lon = -73.5042f;
		float alt = 0;
		groundStation = new GroundStationPosition(lat, lon, alt);	
		tle = new TLE(testTLE);
	}
	
	public void setFadeDepth(double d) {
		fadeDepth = d;
	}
	public void setCarrier(int c) {
		carrier = c;
	}
	
	public double getFade() {
		return fade;
	}
	
	public double getFreq() {
		return carrier + delta;
	}
	
	double delta, fade;
	
	public void processAudio() throws LineUnavailableException, UnsupportedAudioFileException, IOException {
		//System.out.println("Launching SimSatellite");
		
		int BUFFER_LENGTH = 4096;
		//WavFile soundCard = new WavFile("C:\\Users\\chris\\Google Drive\\AMSAT\\Husky-sat\\10_MIN_X6.2n_CAN_HEALTH_435800.wav", BUFFER_LENGTH/2, true);
		//WavFile soundCard = new WavFile("C:\\Users\\chris\\Google Drive\\FoxTelem Recordings\\FlightTest\\QSO1 (RF) HDSDR_20141223_052258Z_145998kHz_RF.wav", BUFFER_LENGTH/2, true);
		SoundCard soundCard = new SoundCard(sampleRate, BUFFER_LENGTH/2, true);
		AudioSink sink = new AudioSink(sampleRate, true);
		ComplexOscillator nco = new ComplexOscillator(sampleRate, 5000);
		//CosOscillator fade = new CosOscillator(sampleRate*fadePeriod, 1);  // samples rate *10 = 10s for a revolution
		AntennaPattern quarterWave = new AntennaPattern(sampleRate*fadePeriod, 1);
		
		boolean readingData = true;
		double[] audioBuffer = new double[BUFFER_LENGTH]; 
		double[] IQbuffer2 = new double[BUFFER_LENGTH];
		
		
		int freq = 1;
		int time = 0;
		nco.setFrequency(freq);
		
		while (running && readingData) {
			double[] IQbuffer = soundCard.read();
			if (IQbuffer != null) {
				for (int d=0; d < IQbuffer.length/2; d++) {
					// Try to clean up the DC spike
					double i = dc_filter1(IQbuffer[2*d]);
					double q = dc_filter2(IQbuffer[2*d+1]);
					//double i = IQbuffer[2*d];
					//double q = IQbuffer[2*d+1];

					if (carrier != 0) {
						//NCO Frequency for Doppler
						Complex c = nco.nextSample();
						c.normalize();
						time++;
						if (hzPerSecond != 0)
							if (time % (sampleRate/hzPerSecond) == 0) {
								delta = 0;
								try {

									SatPos pos = calcualteCurrentPosition(tle, groundStation);
									delta = pos.getDopplerFrequency(carrier); // change in hz from base freq
									//System.out.println(delta);
								} catch (PositionCalcException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								//freq = (int)delta;
								//System.out.println("Freq:"+freq);
								nco.setFrequency(-1*delta);
							}

						// Mix in the doppler
						IQbuffer2[2*d] = i*c.geti() + q*c.getq();
						IQbuffer2[2*d+1] = q*c.geti() - i*c.getq();
					} else {
						IQbuffer2[2*d] = i;
						IQbuffer2[2*d+1] = q;						
					}
					fade = 1;
					if (fadeDepth != 0) {
						// Oscillator used to calculate a cosine profile for fade depth
						//f = fade.nextSample();
						//f = (f + 1)/2; // make it a raised cosine
//						f = 1 - f;
						
						fade = quarterWave.nextSample();
						//System.err.println(f);
						
						fade = fade * fadeDepth;
					}
					audioBuffer[2*d] = IQbuffer2[2*d]*fade + dither(1);
					audioBuffer[2*d+1] = IQbuffer2[2*d+1]*fade + dither(1);
				}
				
				sink.write(audioBuffer);
			} else 
				readingData = false;
		}
	}
	
	private static double dither(double amount) {
		return amount*Math.random()/1000;
	}
	
	Random random = new Random();
	/**
	 * A better noise model for satellites is Average White Gaussian Noise
	 * @param amount
	 * @return
	 */
	private double awgn(double amount) {
		double r = random.nextGaussian(); // mean value 0 and std deviation 1
		r = (r + 4) / 8; // Now 0 - 1 ish taking into account tails to +/- 4
		return r/1000;
	}
	
	private double mAlpha = 0.999;
	private double mPreviousInput1 = 0.0d;
	private double mPreviousOutput1 = 0.0d;
	private double mPreviousInput2 = 0.0d;
	private double mPreviousOutput2 = 0.0d;
	
	public double dc_filter1( double currentInput ) {
		double currentOutput = ( currentInput - mPreviousInput1 ) + 
							  ( mAlpha * mPreviousOutput1 );
		
		
		mPreviousInput1 = currentInput;
		mPreviousOutput1 = currentOutput;
		//if (Config.eliminateDC)
		return currentOutput;
		//else return currentInput;
    }
	public double dc_filter2( double currentInput ) {
		double currentOutput = ( currentInput - mPreviousInput2 ) + 
							  ( mAlpha * mPreviousOutput2 );
		
		
		mPreviousInput2 = currentInput;
		mPreviousOutput2 = currentOutput;
		//if (Config.eliminateDC)
		return currentOutput;
		//else return currentInput;
    }
	
	/**
	 * Calculate the current position and cache it
	 * @param tle 
	 * @param GROUND_STATION 
	 * @return
	 * @throws PositionCalcException
	 */
	protected SatPos calcualteCurrentPosition(TLE tle, GroundStationPosition GROUND_STATION) throws PositionCalcException {
		DateTime timeNow = new DateTime(DateTimeZone.UTC);
		SatPos pos = null;
		final Satellite satellite = SatelliteFactory.createSatellite(tle);
        final SatPos satellitePosition = satellite.getPosition(GROUND_STATION, timeNow.toDate());
		return satellitePosition;

	}

	public void stopProcessing() {
		running = false;
	}

	@Override
	public void run() {
		try {
			processAudio();
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
