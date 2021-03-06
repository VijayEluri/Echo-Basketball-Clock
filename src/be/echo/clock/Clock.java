package be.echo.clock;

import java.awt.*;
import java.awt.LayoutManager;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import static be.echo.clock.Log.log;

public class Clock extends JFrame implements ActionListener {

	private final static int MINUTES_IN_QUARTER = 10;

	private final static int DEFAULT_SHOT_SECONDS = 24;

	private final static int REDUCED_SHOT_SECONDS = 14;

	private final static int PAUZED_SHOT_TIME = -1;

	private final static String RESET24_SHOT_TEXT = ": 24 :";

	private final static String RESET14_SHOT_TEXT = ". 14 .";

	private int gameTime = MINUTES_IN_QUARTER;

	private int shotTime = PAUZED_SHOT_TIME;

	private SoundClip buzzer = new Buzzer();

	private JLabel gameTimeField;

	private JLabel shotTimeField;

	private Toolkit toolkit;

	private ClockAdjuster clockAdjuster = new ClockAdjuster();

	private KeyListener keyListener = new KeyListener();

	static private Timer timer;

	private boolean shotClockEnabled = true;

	private Font normalFont;

	private Font largeFont;

	private GridBagLayout normalLayout = new GridBagLayout();

	public Clock(int minutes) throws Exception {
		super(getHelpText());

		toolkit = getToolkit();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().setBackground(Color.ORANGE);

		gameTimeField = new JLabel();
		gameTimeField.setForeground(Color.BLACK);
		gameTimeField.setHorizontalAlignment(SwingConstants.CENTER);

		Font oldFont = gameTimeField.getFont();
		normalFont = oldFont.deriveFont(380f);
		largeFont = oldFont.deriveFont(500f);
		gameTimeField.setFont(normalFont);

		shotTimeField = new JLabel(RESET24_SHOT_TEXT);
		shotTimeField.setFont(normalFont);
		shotTimeField.setForeground(Color.BLACK);
		shotTimeField.setHorizontalAlignment(SwingConstants.CENTER);

		setNormalLayout();

		setLayout(normalLayout);

		add(gameTimeField);
		add(shotTimeField);

		gameTime = 60 * minutes;
		updateGameClock();
		resetShotClock(RESET24_SHOT_TEXT);
		
		addKeyListener(keyListener);

		MouseListener mouseListener = new MouseListener();
		addMouseListener(mouseListener);
		
		pack();

		setExtendedState(Frame.MAXIMIZED_BOTH);
		Dimension dimension = getSize();
		final float fontSize = ((float) dimension.height - 50) / 2;
		log("font size = " + fontSize);
		final float largeFontSize = ((float) dimension.width / 2);
		log("large font size = " + largeFontSize);

		normalFont = oldFont.deriveFont(fontSize);
		largeFont = oldFont.deriveFont(largeFontSize);
		gameTimeField.setFont(normalFont);
		shotTimeField.setFont(normalFont);
	}

	private void beep(int count) {
		try {
			for (int i = 0 ; i < count ; ++i) {
				toolkit.beep();
				Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			log("interrupted sleep in beep sequence");
		}
	}

	private void toggleClock() {
		if (timer.isRunning()) {
			log("stop at " + getCurrentTimes());
			timer.stop();
			gameTimeField.setForeground(Color.BLACK);
			getContentPane().setBackground(Color.ORANGE);
		} else {
			log("start at " + getCurrentTimes());
			if (shotTimeField.getText().equals(RESET14_SHOT_TEXT)) {
				startShotClock(REDUCED_SHOT_SECONDS);
			} else if (shotTime <= 0) {
				resetShotClock(RESET24_SHOT_TEXT);
			}
			timer.start();
			gameTimeField.setForeground(Color.RED);
			getContentPane().setBackground(Color.WHITE);
		}
	}
	
	private class KeyListener extends KeyAdapter {
		public void keyTyped(KeyEvent e) {
			if (e.getKeyChar() == '\n') {
				resetShotClock(RESET24_SHOT_TEXT);
			} else if (e.getKeyChar() == 'f') {
				resetShotClock(RESET14_SHOT_TEXT);
			}
		}
		public void keyReleased(KeyEvent e) {
			switch (e.getKeyChar()) {
				case '\n':
					startShotClock(DEFAULT_SHOT_SECONDS);
					break;
				case ' ':
					toggleClock();
					break;
				case 'f':
					startShotClock(REDUCED_SHOT_SECONDS);
					break;
			}
		}
		public void keyPressed(KeyEvent event) {
			switch (event.getKeyCode()) {
				case KeyEvent.VK_ESCAPE:
					log("buzz request: timer running = " + timer.isRunning());
					if (! timer.isRunning()) {
						buzzer.play();
					}
					break;
				case KeyEvent.VK_F5:
					log("xpert request: timer running = " + timer.isRunning());
					if (! timer.isRunning()) {
						log("enter xpert: time=" + getCurrentTimes());
						getContentPane().setBackground(Color.GRAY);
						removeKeyListener(this);
						addKeyListener(clockAdjuster);
					}
					break;
				case KeyEvent.VK_F9:
					log("request toggle shot clock");
					if (! timer.isRunning()) {
						toggleShotClock();
					}
					break;
				case KeyEvent.VK_F12:
					if (event.isControlDown()) {
						log("request reset");
						if (! timer.isRunning()) {
							gameTime = 60 * MINUTES_IN_QUARTER;
							updateGameClock();
							resetShotClock(RESET24_SHOT_TEXT);
						}
					}
					break;
			}
		}
	}

	private void setNormalLayout() {
		GridBagConstraints gameClockConstraints = new GridBagConstraints();
		gameClockConstraints.anchor = GridBagConstraints.NORTH;
		gameClockConstraints.gridwidth = GridBagConstraints.REMAINDER;
		gameClockConstraints.weighty = 1;

		GridBagConstraints shotClockConstraints = new GridBagConstraints();
		shotClockConstraints.anchor = GridBagConstraints.SOUTH;
		shotClockConstraints.gridwidth = GridBagConstraints.REMAINDER;

		normalLayout.setConstraints(gameTimeField, gameClockConstraints);
		normalLayout.setConstraints(shotTimeField, shotClockConstraints);
	}

	private void toggleShotClock() {
		log("toggle shot clock");
		if (shotClockEnabled) {
			remove(shotTimeField);
			gameTimeField.setFont(largeFont);
		} else {
			gameTimeField.setFont(normalFont);
			remove(gameTimeField);
			setNormalLayout();
			add(gameTimeField);
			add(shotTimeField);
		}
		shotClockEnabled = ! shotClockEnabled;
		pack();
		setExtendedState(Frame.MAXIMIZED_BOTH);
	}

	private class ClockAdjuster extends KeyAdapter {
		public void keyPressed(KeyEvent event) {
			switch (event.getKeyCode()) {
				case KeyEvent.VK_RIGHT:
					++gameTime;
					updateGameClock();
					break;
				case KeyEvent.VK_LEFT:
					if (gameTime > 0) {
						--gameTime;
						updateGameClock();
					}
					break;
				case KeyEvent.VK_UP:
					++shotTime;
					shotTimeField.setText(Integer.toString(shotTime));
					break;
				case KeyEvent.VK_DOWN:
					if (shotTime > 0) {
						--shotTime;
						shotTimeField.setText(Integer.toString(shotTime));
					}
					break;
				case KeyEvent.VK_F5:
					log("exit xpert: time=" + getCurrentTimes());
					removeKeyListener(this);
					addKeyListener(keyListener);
					getContentPane().setBackground(Color.ORANGE);
					break;
			}
		}
	}

	private String getCurrentTimes() {
		String gameTimeText = gameTimeField.getText();
		String shotTimeText = shotTimeField.getText();
		return "time=" + gameTimeText + ",shot=" + shotTimeText;
	}

	private class MouseListener extends MouseInputAdapter {
		public void mouseClicked(MouseEvent e) {
		}
		public void mousePressed(MouseEvent e) {
			switch (e.getButton()) {
				case MouseEvent.BUTTON1:
					// BEWARE: previously clock toggle
					resetShotClock(RESET24_SHOT_TEXT);
					break;
				case MouseEvent.BUTTON3:
					// BEWARE: previously shot reset
					resetShotClock(RESET14_SHOT_TEXT);
					break;
			}
		}
		public void mouseReleased(MouseEvent e) {
			switch (e.getButton()) {
				case MouseEvent.BUTTON1:
					startShotClock(DEFAULT_SHOT_SECONDS);
					break;
				case MouseEvent.BUTTON3:
					startShotClock(REDUCED_SHOT_SECONDS);
					break;
			}
		}
	}

	private void timeOutShotClock() {
		if (shotClockEnabled) {
			log("shot time out at " + getCurrentTimes());
			buzzer.play();
			timer.stop();
			getContentPane().setBackground(Color.CYAN);
		}
	}

	private void warnTimeOutShot() {
		if (shotClockEnabled) {
			toolkit.beep();
		}
	}
	
	private class TimeUpdater implements Runnable {
		public void run() {
			--gameTime;
			if (gameTime > 0) {
				if (shotTime > 0) {
					--shotTime;
					shotTimeField.setText(Integer.toString(shotTime));
				}
				updateGameClock();
				if (shotTime <= 3 && shotTime > 0) {
					warnTimeOutShot();
				} else if (shotTime == 0) {
					timeOutShotClock();
				}
			} else {
				gameTimeField.setText("0:00");
				timer.stop();
				buzzer.play();
			}
		}
	}

	private void updateGameClock() {
		String minutes = Integer.toString(gameTime / 60);
		int seconds = gameTime % 60;
		String padding = seconds < 10 ? "0" : "";
		String secs = padding + seconds;
		gameTimeField.setText(minutes + ":" + secs);
	}

	private void resetShotClock(String shotClockText) {
		if (! shotClockEnabled) {
			return;
		}
		if (shotTime >= 0) {
			shotTime = PAUZED_SHOT_TIME;
			log("reset/stop shot " + shotClockText + ": " + getCurrentTimes());
			shotTimeField.setText(shotClockText);
		}
	}

	private void startShotClock(int seconds) {
		shotTime = seconds;
		if (timer.isRunning()) {
			shotTimeField.setText(Integer.toString(shotTime));
			log("start shot " + seconds + ": " + getCurrentTimes());
		}
	}

	public static void main(final String[] args) throws Exception {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					int minutes = MINUTES_IN_QUARTER;
					if (args.length == 1) {
						minutes = Integer.parseInt(args[0]);
					}
					Clock frame = new Clock(minutes);
					frame.setVisible(true);
					timer = new Timer(1000, frame);
					timer.setInitialDelay(1000);
				
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	public void actionPerformed(ActionEvent event) {
		SwingUtilities.invokeLater(new TimeUpdater());
	}

	static public String getHelpText() {
		String separator = " | ";
		StringBuilder b = new StringBuilder();
		b.append("<Space>: toggle clock");
		b.append(separator);
		b.append("<Enter>/Mouse L: 24s reset shot");
		b.append(separator);
		b.append("f/Mouse R: 14s reset shot");
		b.append(separator);
		b.append("<ESC>: buzzer");
		b.append(separator);
		b.append("F5: edit time (arrows)");
		b.append(separator);
		b.append("F9: switch shot clock");
		b.append(separator);
		b.append("Ctrl+F12: new quarter!");
		return b.toString();
	}
}
