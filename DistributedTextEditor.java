
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.util.concurrent.*;

public class DistributedTextEditor extends JFrame {

	private JTextArea area1 = new JTextArea(20,120);
	private JTextField ipaddress = new JTextField("IP address here");     
	private JTextField portNumber = new JTextField("Server's port number here");     
	private JTextField portNumberSelf = new JTextField("Own port number here");     

	private TextAreaSyncronizer textAreaSyncronizer;
	private Thread ert; 

	private JFileChooser dialog = 
		new JFileChooser(System.getProperty("user.dir"));

	private String currentFile = "Untitled";
	private boolean changed = false;
	private boolean connected = false;
	private DocumentEventCapturer dec = new DocumentEventCapturer();

	private Connector connector = new Connector();
	private Sequencer sequencer;
	private ClientRedirector redirector = new ClientRedirector(connector);

	public DistributedTextEditor() {
		area1.setFont(new Font("Monospaced",Font.PLAIN,12));

		((AbstractDocument)area1.getDocument()).setDocumentFilter(dec);

		Container content = getContentPane();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		JScrollPane scroll1 = 
			new JScrollPane(area1, 
					JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
					JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		content.add(scroll1,BorderLayout.CENTER);

		content.add(ipaddress,BorderLayout.CENTER);	
		content.add(portNumber,BorderLayout.CENTER);	
		content.add(portNumberSelf,BorderLayout.CENTER);	

		JMenuBar JMB = new JMenuBar();
		setJMenuBar(JMB);
		JMenu file = new JMenu("File");
		JMenu edit = new JMenu("Edit");
		JMB.add(file); 
		JMB.add(edit);

		file.add(Listen);
		file.add(Connect);
		file.add(Disconnect);
		file.addSeparator();
		file.add(Save);
		file.add(SaveAs);
		file.add(Quit);

		edit.add(Copy);
		edit.add(Paste);
		edit.getItem(0).setText("Copy");
		edit.getItem(1).setText("Paste");

		Save.setEnabled(false);
		SaveAs.setEnabled(false);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		area1.addKeyListener(k1);
		setTitle("Disconnected");
		setVisible(true);
		connector.startSendThread(dec);
		connector.startReceiveThread();
		textAreaSyncronizer = new TextAreaSyncronizer(connector, area1, dec);
    dec.setTextAreaSyncronizer(textAreaSyncronizer);
		ert = new Thread(textAreaSyncronizer);
		ert.start();
	}

	private KeyListener k1 = new KeyAdapter() {
		public void keyPressed(KeyEvent e) {
			changed = true;
			Save.setEnabled(true);
			SaveAs.setEnabled(true);
		}
	};

	Action Listen = new AbstractAction("Listen") {
		public void actionPerformed(ActionEvent e) {
			saveOld();
			area1.setText("");
			try {
				//String portString = portNumber.getText();
				//int portNumber = Integer.parseInt(portString);
				String portStringSelf = portNumberSelf.getText();
				int portNumberSelf = Integer.parseInt(portStringSelf);
				InetAddress localhost = InetAddress.getLocalHost();
				String localhostAddress = localhost.getHostAddress();
				setTitle("I'm listening on " + localhostAddress + ":" + portNumberSelf);
				changed = false;
				Save.setEnabled(false);
				SaveAs.setEnabled(false);
				sequencer = new Sequencer(DistributedTextEditor.this);
				sequencer.listenForClients(portNumberSelf);
				//TODO: Once the Sequencer has "started up", connect to the Sequencer.
				while (!connector.isConnected()) {
					connector.connectToServer("localhost", portNumberSelf);
				}
				sequencer.startSendThread();
				//connector.listenForClient();
			} catch (UnknownHostException ex) {
				// TODO: Handle exception
			}
		}
	};

	Action Connect = new AbstractAction("Connect") {
		public void actionPerformed(ActionEvent e) {
			String portString = portNumber.getText();
			int portNumber = Integer.parseInt(portString);
			String portStringSelf = portNumberSelf.getText();
			int portNumberSelf = Integer.parseInt(portStringSelf);
			saveOld();
			area1.setText("");
			setTitle("Connecting to " + ipaddress.getText() + ":" + portNumber + "...");
			connector.connectToServer(ipaddress.getText(), portNumber);
			if (connector.isConnected()) {
				setTitle("Connected to " + ipaddress.getText() + ":" + portNumber);
				redirector.start(portNumberSelf);
			} else {
				setTitle("Not able to connect to " + ipaddress.getText() + ":" + portNumber);
			}
			changed = false;
			Save.setEnabled(false);
			SaveAs.setEnabled(false);
		}
	};

	Action Disconnect = new AbstractAction("Disconnect") {
		public void actionPerformed(ActionEvent e) {	
			setTitle("Disconnected");
			connector.disconnect();
			redirector.stop();
		}
	};

	Action Save = new AbstractAction("Save") {
		public void actionPerformed(ActionEvent e) {
			if(!currentFile.equals("Untitled"))
				saveFile(currentFile);
			else
				saveFileAs();
		}
	};

	Action SaveAs = new AbstractAction("Save as...") {
		public void actionPerformed(ActionEvent e) {
			saveFileAs();
		}
	};

	Action Quit = new AbstractAction("Quit") {
		public void actionPerformed(ActionEvent e) {
			saveOld();
			System.exit(0);
		}
	};

	ActionMap m = area1.getActionMap();

	Action Copy = m.get(DefaultEditorKit.copyAction);
	Action Paste = m.get(DefaultEditorKit.pasteAction);

	private void saveFileAs() {
		if(dialog.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
			saveFile(dialog.getSelectedFile().getAbsolutePath());
	}

	private void saveOld() {
		if(changed) {
			if(JOptionPane.showConfirmDialog(this, "Would you like to save "+ currentFile +" ?","Save",JOptionPane.YES_NO_OPTION)== JOptionPane.YES_OPTION)
				saveFile(currentFile);
		}
	}

	private void saveFile(String fileName) {
		try {
			FileWriter w = new FileWriter(fileName);
			area1.write(w);
			w.close();
			currentFile = fileName;
			changed = false;
			Save.setEnabled(false);
		}
		catch(IOException e) {
		}
	}

	public static void main(String[] arg) {
		new DistributedTextEditor();
	}        

	public TextAreaSyncronizer getTextAreaSyncronizer(){
		return this.textAreaSyncronizer;
	}

}
