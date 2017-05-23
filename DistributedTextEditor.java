
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

	private InetAddress localAddress;

	private Connector connector = new Connector(this);
	private Sequencer sequencer;
	private ClientRedirector redirector = new ClientRedirector(connector, new Connector(this));

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
				redirector.stop();
				sequencer = new Sequencer(DistributedTextEditor.this);
				sequencer.listenForClients(portNumberSelf);
				while (!connector.isConnected()) {
					connector.connectToServer(InetAddress.getByName("localhost"), portNumberSelf, portNumberSelf);
				}
				sequencer.startSendThread();
			} catch (UnknownHostException ex) {
				// TODO: Handle exception
			}
		}
	};

	Action Connect = new AbstractAction("Connect") {
		public void actionPerformed(ActionEvent e) {
			try {
				String portString = portNumber.getText();
				int portNumber = Integer.parseInt(portString);
				String portStringSelf = portNumberSelf.getText();
				int portNumberSelf = Integer.parseInt(portStringSelf);
				saveOld();
				area1.setText("");
				setTitle("Connecting to " + ipaddress.getText() + ":" + portNumber + "...");
				connector.connectToServer(InetAddress.getByName(ipaddress.getText()), portNumber, portNumberSelf);
				if (connector.isConnected()) {
					setTitle("Connected to " + ipaddress.getText() + ":" + portNumber);
					redirector.start(portNumberSelf);
				} else {
					setTitle("Not able to connect to " + ipaddress.getText() + ":" + portNumber);
				}
				changed = false;
				Save.setEnabled(false);
				SaveAs.setEnabled(false);
			} catch (UnknownHostException ex) {
				ex.printStackTrace();
			}

		}
	};

	Action Disconnect = new AbstractAction("Disconnect") {
		public void actionPerformed(ActionEvent e) {	
			setTitle("Disconnected");
			connector.disconnect();
			redirector.stop();
			if (sequencer != null) {
				sequencer.stop();
				sequencer = null;
			}
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

	public void clientDisconnected(Peer peer) {
		if (sequencer != null) return;

		connector.removePeer(peer);

		try {
			int port = Integer.parseInt(portNumberSelf.getText());
			InetAddress address = InetAddress.getLocalHost();
			Peer me = new Peer(localAddress, port); 
			//System.out.println("Am I the first Peer??");
			//System.out.println(me.toString());
			//System.out.println(connector.getFirstPeer().toString());
			//System.out.println("Well am I??");
			//System.out.println("1");
			//System.out.println("2");
			//System.out.println("3");
			//System.out.println("4");
			//System.out.println("6");


			if (connector.isThisFirstPeer(me)) {
				//System.out.println("I am the first peer!!");

				String localhostAddress = address.getHostAddress();
				setTitle("I'm listening on " + localhostAddress + ":" + port);
				changed = false;
				Save.setEnabled(false);
				SaveAs.setEnabled(false);
				redirector.stop();
				sequencer = new Sequencer(DistributedTextEditor.this);
				sequencer.listenForClients(port);
				while (!connector.isConnected()) {
					connector.connectToServer(InetAddress.getByName("localhost"), port, port);
					try {
						Thread.sleep(10);		// DEBUG
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}


				}
				sequencer.startSendThread();
			} else {
				//System.out.println("I am not the first peer :(");

				Peer first = connector.getFirstPeer();

				for (int i = 0; i < 10; i++) {
					connector.connectToServer(first.getInetAddress(), first.getPort(), port);
					if (connector.isConnected()) break;
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (connector.isConnected()) {
					return;
				} else {
					clientDisconnected(first);
				}
			}
		} catch (UnknownHostException ex) {
			ex.printStackTrace();
		}
	}

	public int getListeningPortNumber() {
			return Integer.parseInt(portNumberSelf.getText());
	}

	public void setLocalAddress(InetAddress address) {
		this.localAddress = address;
	}


}
