package com.albom.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.albom.ionosonde.shigaraki.IonogramShigaraki;

import org.jdesktop.swingx.JXDatePicker;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;

public class MainWindow extends JFrame {

	private final String TITLE = "Ionogram Viewer";

	private MainWindow frame;

	private JXDatePicker picker;
	private boolean loaded = false;

	private JFreeChart chart;

	private JComboBox<String> comboHours;
	private JComboBox<String> comboMinutes;

	private static final long serialVersionUID = 1L;

	private LocalDateTime date = LocalDateTime.of(2017, 01, 21, 15, 0);

	private XYSeriesCollection dataset = new XYSeriesCollection();

	private JFreeChart createChart(XYDataset dataset) {

		JFreeChart chart = ChartFactory.createXYLineChart("Shigaraki. ", "Frequency, MHz", "Virtual height, km",
				dataset, PlotOrientation.VERTICAL, false, true, false);

		return chart;
	}

	private void process() {
		IonogramShigaraki iono = load();

		if (iono != null) {
			chart.setTitle("Shigaraki. " + date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd. HH:mm.")));
			plot(chart, iono);
			loaded = true;
		}
	}

	private void errorDate() {
		JOptionPane.showMessageDialog(frame, "Select date.\n", TITLE, JOptionPane.ERROR_MESSAGE);
	}

	private void errorNoLoaded() {
		JOptionPane.showMessageDialog(frame, "File is not loaded.\n", TITLE, JOptionPane.ERROR_MESSAGE);
	}

	private void nextOrPrev(boolean next) {
		int deltaT = 15;
		if (!next) {
			deltaT *= -1;
		}
		if (loaded) {
			date = date.plusMinutes(deltaT);
			int hour = date.getHour();
			int minute = date.getMinute();
			int year = date.getYear();
			int month = date.getMonthValue();
			int day = date.getDayOfMonth();
			comboHours.setSelectedIndex(hour);
			comboMinutes.setSelectedIndex(minute / 15);

			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, month - 1);
			cal.set(Calendar.DAY_OF_MONTH, day);
			picker.setDate(cal.getTime());

			process();

		} else {
			errorNoLoaded();
		}

	}

	public void init() {

		frame = this;

		setTitle(TITLE);

		setDefaultCloseOperation(EXIT_ON_CLOSE);

		chart = createChart(dataset);
		ChartPanel chartPanel = new ChartPanel(chart);

		chartPanel.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));

		chartPanel.setZoomTriggerDistance(Integer.MAX_VALUE);
		chartPanel.setFillZoomRectangle(false);
		chartPanel.setZoomOutlinePaint(new Color(0f, 0f, 0f, 0f));

		CrosshairOverlay crosshairOverlay = new CrosshairOverlay();
		Crosshair xCrosshair = new Crosshair(0, Color.BLUE, new BasicStroke(0f));
		crosshairOverlay.addDomainCrosshair(xCrosshair);
		chartPanel.addOverlay(crosshairOverlay);

		NumberAxis range = (NumberAxis) ((XYPlot) chart.getPlot()).getRangeAxis();
		range.setRange(50, 700);
		range.setTickUnit(new NumberTickUnit(50));

		chartPanel.setPopupMenu(null);

		chartPanel.addChartMouseListener(new ChartMouseListener() {

			@Override
			public void chartMouseClicked(ChartMouseEvent cme) {

				if (loaded) {
					final ChartMouseEvent cmeLocal = cme;
					ChartPanel hostChartPanel = (ChartPanel) cme.getTrigger().getComponent();
					if (null != hostChartPanel) {
						hostChartPanel.repaint();
						java.awt.EventQueue.invokeLater(new Runnable() {
							@Override
							public void run() {
								JFreeChart chart = cmeLocal.getChart();
								XYPlot plot = chart.getXYPlot();
								double crossHairX = plot.getDomainCrosshairValue();
								xCrosshair.setValue(crossHairX);
								String x = String.format(Locale.US, "%3.1f", crossHairX);
								chart.setTitle(
										"Shigaraki. " + date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd. HH:mm."))
												+ " Frequency: " + x + " MHz");

								saveImage();

								StringSelection stringSelection = new StringSelection(x);
								Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
								clpbrd.setContents(stringSelection, null);

							}

							private void saveImage() {
								BufferedImage im = new BufferedImage(chartPanel.getWidth(), chartPanel.getHeight(),
										BufferedImage.TYPE_INT_ARGB);
								chartPanel.paint(im.getGraphics());
								try {
									String fileName = "Shigaraki_"
											+ date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")) + ".png";
									ImageIO.write(im, "PNG", new File(fileName));
								} catch (IOException e) {
								}
							}
						});
					}

				}
			}

			@Override
			public void chartMouseMoved(ChartMouseEvent arg0) {
			}

		});

		XYSeries series = new XYSeries("1");
		for (int f = 20; f < 181; f++) {
			series.add(0.1 * f, 0);
		}
		dataset.addSeries(series);

		picker = new JXDatePicker();
		picker.setAlignmentX(Component.CENTER_ALIGNMENT);
		JButton buttonLoad = new JButton("Load");
		buttonLoad.setAlignmentX(Component.CENTER_ALIGNMENT);

		buttonLoad.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Date d = picker.getDate();
				Calendar cal = Calendar.getInstance();
				try {
					cal.setTime(d);
				} catch (Exception e1) {
					errorDate();
					return;
				}

				int hour = Integer.valueOf(comboHours.getSelectedItem().toString());
				int minute = Integer.valueOf(comboMinutes.getSelectedItem().toString());

				date = LocalDateTime.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
						cal.get(Calendar.DAY_OF_MONTH), hour, minute);

				process();

			}

		});

		JLabel labelDate = new JLabel("Date:");
		JLabel labelTime = new JLabel("Time:");

		String[] hours = new String[24];
		for (int i = 0; i < 24; i++) {
			hours[i] = String.format("%02d", i);
		}

		String[] minutes = new String[4];
		for (int i = 0; i < 4; i++) {
			minutes[i] = String.format("%02d", i * 15);
		}

		comboHours = new JComboBox<>(hours);
		comboMinutes = new JComboBox<>(minutes);

		JButton buttonNext = new JButton("Next >>");
		JButton buttonPrev = new JButton("<< Prev.");

		buttonNext.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				nextOrPrev(true);
			}

		});

		buttonPrev.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				nextOrPrev(false);
			}

		});

		JButton buttonAbout = new JButton("About...");

		buttonAbout.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(frame, TITLE + "\n\n\u00A9 Oleksandr Bogomaz, 2018-2021", TITLE,
						JOptionPane.INFORMATION_MESSAGE);
			}

		});

		JPanel panel = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(1, 1, 1, 1);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		panel.add(labelDate, gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		panel.add(picker, gbc);

		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		panel.add(labelTime, gbc);

		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		panel.add(comboHours, gbc);

		gbc.gridx = 2;
		gbc.gridy = 3;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		panel.add(comboMinutes, gbc);

		gbc.gridx = 1;
		gbc.gridy = 4;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		panel.add(buttonLoad, gbc);

		gbc.gridx = 1;
		gbc.gridy = 6;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		panel.add(buttonPrev, gbc);

		gbc.gridx = 2;
		gbc.gridy = 6;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		panel.add(buttonNext, gbc);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 10;
		gbc.weighty = 10;
		panel.add(chartPanel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 7;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.insets = new Insets(25, 1, 1, 1);
		panel.add(buttonAbout, gbc);

		add(panel);

		pack();

		setVisible(true);
		setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);

	}

	private IonogramShigaraki load() {
		IonogramShigaraki iono = new IonogramShigaraki();
		if (!iono.download(date)) {
			JOptionPane.showMessageDialog(this, "Error loading file!\n", TITLE, JOptionPane.ERROR_MESSAGE);
			return null;
		}
		return iono;
	}

	private void plot(JFreeChart chart, IonogramShigaraki iono) {
		BufferedImage image;
		double[][] data_d = iono.getData();
		double[] freq_d = iono.getFrequencies();
		double[] alt_d = iono.getAltitudes();

		int width = freq_d.length - 2;
		int height = alt_d.length - 3;

		int[] data = new int[width * height];
		int i = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int v = -(int) (5 * (data_d[height - y][x] + 40));
				v = v < 255 ? v : 255;
				v = v > 0 ? v : 0;
				int red = v;
				int green = v;
				int blue = v;
				data[i++] = (red << 16) | (green << 8) | blue;
			}
		}

		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, width, height, data, 0, width);

		chart.getPlot().setBackgroundImage(image);
		chart.getPlot().setBackgroundImageAlpha(1);

		NumberAxis domain = (NumberAxis) ((XYPlot) chart.getPlot()).getDomainAxis();
		domain.setRange(freq_d[0], freq_d[freq_d.length - 1]);
		domain.setTickUnit(new NumberTickUnit(1));

		NumberAxis range = (NumberAxis) ((XYPlot) chart.getPlot()).getRangeAxis();
		range.setRange(alt_d[0], alt_d[alt_d.length - 2]);
		range.setTickUnit(new NumberTickUnit(50));
	}

}
