package com.albom.application;

import java.awt.EventQueue;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.albom.gui.MainWindow;

public class IonogramViewerApplication {

	private void run() {

		List<String> proxy = null;
		try {
			proxy = Files.readAllLines(Paths.get("proxy.conf"), Charset.forName("utf-8"));
		} catch (IOException e) {
		}

		if (proxy != null && proxy.size() > 1) {
			System.setProperty("http.proxyHost", proxy.get(0));
			System.setProperty("http.proxyPort", proxy.get(1));
		}

		EventQueue.invokeLater(() -> {
			MainWindow mainWindow = new MainWindow();
			mainWindow.init();
		});
	}

	public static void main(String[] args) {

		IonogramViewerApplication app = new IonogramViewerApplication();
		app.run();
	}

}
