import java.util.Random;

import javax.sound.sampled.LineUnavailableException;

import audio.SoundCard;
import gui.MainWindow;
import signal.ComplexOscillator;
import signal.CosOscillator;
import sim.Simulate;
import signal.Complex;
import audio.AudioSink;

public class SimSatMain {

	public static void main(String[] args) throws LineUnavailableException {
		MainWindow window = new MainWindow();
		//testNoise(window);
		Thread simMonitor = new Thread(window);
		simMonitor.start();
	}
	
	private static void testNoise(MainWindow window) {
		Random random = new Random();
		
		double[] noise = new double[1024];
		// Values are -1 to 1.  So if we divide in N bins we can plot pdf
		int N = 256;
		double[] probDensityFunc = new double[N];
		for (int i=0; i< noise.length; i++) {
			noise[i] = random.nextGaussian();
			noise[i] = (noise[i] + 4) / 8; // Now 0 - 1 ish
//			noise[i] = Math.random();
			int bin = (int) (N*noise[i]);
			//System.out.println(bin);
			if (bin >=0 && bin < N)
				probDensityFunc[bin] += 1;

		}
		
		window.setData(noise);
	}
	
}
