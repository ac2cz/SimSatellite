package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.DecimalFormat;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import sim.Simulate;

@SuppressWarnings("serial")
public class MainWindow extends JFrame implements Runnable, WindowListener, ActionListener {
	
	JButton butStart;
	JTextField txtFadeDepth, txtSampleRate, txtCarrier, txtFadePeriod;
	JCheckBox cbApplyDoppler, cbApplyFade;
	JLabel lblFade, lblFreq;
	static final String FADE = "Fade: ";
	static final String FREQ = "Freq: ";
	
	int sampleRate = 192000;
	Thread simThread;
	Simulate simulation;

	public MainWindow() {
		super("Simulate Downlink Signal");
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		addWindowListener(this);
		setBounds(100, 100, 400, 300);
		
		// TODO - need to add lat lon and keps to this window so that it works for others
		//      - save in a config file
		
		JPanel bottomPanel = new JPanel();
		getContentPane().add(bottomPanel, BorderLayout.SOUTH);
		JLabel lblSampleRate = new JLabel("Sample Rate: " +sampleRate);
		bottomPanel.add(lblSampleRate);
		lblFade = new JLabel(FADE);
		bottomPanel.add(lblFade);
		lblFreq = new JLabel(FREQ);
		bottomPanel.add(lblFreq);

		JPanel panelCenter = new JPanel();
		panelCenter.setLayout(new BoxLayout(panelCenter, BoxLayout.Y_AXIS));
		getContentPane().add(panelCenter, BorderLayout.CENTER);
		
		cbApplyFade = new JCheckBox("Fade Signal");
		cbApplyFade.addActionListener(this);
		panelCenter.add(cbApplyFade);

		JPanel panelFadeDepth = new JPanel();
		JLabel lblFadeDepth = new JLabel("Fade Depth");
		txtFadeDepth = new JTextField("0.5");
		txtFadeDepth.setColumns(5);
		panelFadeDepth.add(lblFadeDepth);
		panelFadeDepth.add(txtFadeDepth);
		panelCenter.add(panelFadeDepth);
		
		JPanel panelFadePeriod = new JPanel();
		JLabel lblFadePeriod = new JLabel("Fade Period (s)");
		txtFadePeriod = new JTextField("36");
		txtFadePeriod.setColumns(5);
		panelFadePeriod.add(lblFadePeriod);
		panelFadePeriod.add(txtFadePeriod);
		panelCenter.add(panelFadePeriod);

		cbApplyDoppler = new JCheckBox("Doppler shift Signal");
		cbApplyDoppler.addActionListener(this);
		panelCenter.add(cbApplyDoppler);

		JPanel panelCarrier = new JPanel();
		JLabel lblCarrier = new JLabel("Carrier Freq (Hz)");
		txtCarrier = new JTextField("435750000");
		txtCarrier.setColumns(10);
		panelCarrier.add(lblCarrier);
		panelCarrier.add(txtCarrier);
		panelCenter.add(panelCarrier);
		
		butStart = new JButton("Start");
		butStart.addActionListener(this);
		panelCenter.add(butStart);
		
		//lineChart = new LineChart("DSP Results");
		//add(lineChart);
		//fftPanel = new FFTPanel(sampleRate, 0, 1024/2);
		//add(fftPanel);
		
	}
	LineChart lineChart;
	FFTPanel fftPanel;
	public void setData(double[] data) {
		lineChart.setData(data);
	}
	private void shutdownWindow() {
		simulation.stopProcessing();
		this.dispose();
		System.exit(0);
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowClosed(WindowEvent e) {
		shutdownWindow();
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void actionPerformed(ActionEvent e) {
//		if (e.getSource() == cbApplyFade) {
//			if (cbApplyFade.isSelected()) {
//				if (simulation != null) {
//					double fadeDepth = 0;
//					try {
//						fadeDepth = Double.parseDouble(txtFadeDepth.getText());
//					} catch (NumberFormatException en) {
//					}
//					simulation.setFadeDepth(fadeDepth);
//				} else {
//					simulation.setFadeDepth(0);
//				}
//			}
//		}
//		if (e.getSource() == cbApplyDoppler) {
//			if (cbApplyDoppler.isSelected()) {
//				if (simulation != null) {
//					int carrier = 0;
//					try {
//						carrier = Integer.parseInt(txtCarrier.getText());
//					} catch (NumberFormatException en) {
//					}
//					simulation.setCarrier(carrier);
//				} else {
//					simulation.setCarrier(0);
//				}
//			}
//		}
		if (e.getSource() == butStart) {
			double fadeDepth = 0;
			int fadePeriod = 0;
			if (cbApplyFade.isSelected()) {
				try {
					fadeDepth = Double.parseDouble(txtFadeDepth.getText());
				} catch (NumberFormatException en) {
				}
				if (fadeDepth > 1) fadeDepth = 1.0;
				txtFadeDepth.setText(""+fadeDepth);
				try {
					fadePeriod = Integer.parseInt(txtFadePeriod.getText());
				} catch (NumberFormatException en) {
				}
				if (fadePeriod < 1) fadePeriod = 1;
				txtFadePeriod.setText(""+fadePeriod);
			}
			int carrier = 0;
			if (cbApplyDoppler.isSelected()) {
				try {
					carrier = Integer.parseInt(txtCarrier.getText());
				} catch (NumberFormatException en) {
				}
				txtCarrier.setText(""+carrier);
			}
			
			simulation = new Simulate(192000, fadeDepth, fadePeriod, carrier);
			simThread = new Thread(simulation);
			simThread.start();
			
			butStart.setEnabled(false);
			txtFadeDepth.setEditable(false);
			txtFadePeriod.setEditable(false);
			txtCarrier.setEditable(false);
			cbApplyFade.setEnabled(false);
			cbApplyDoppler.setEnabled(false);

		}
		
	}
	
	DecimalFormat f2 = new DecimalFormat("0");
	DecimalFormat d3 = new DecimalFormat("0.000");
	
	@Override
	public void run() {
		Thread.currentThread().setName("MainWindow");
		setVisible(true);

		// Runs until we exit
		while(true) {

			// Sleep first to avoid race conditions at start up
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					if (simulation != null) {
						double f = simulation.getFreq();
						lblFreq.setText(FREQ + d3.format(f/1000));
					}
				}
			});
		}
	}

}
