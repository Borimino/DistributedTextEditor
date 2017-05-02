import javax.swing.JTextArea;
import java.awt.EventQueue;

/**
 * 
 * Takes the event recorded by the DocumentEventCapturer and replays
 * them in a JTextArea. The delay of 1 sec is only to make the individual
 * steps in the reply visible to humans.
 * 
 * @author Jesper Buus Nielsen
 *
 */
public class EventReplayer implements Runnable {

	private Connector con;
	private JTextArea area;
	private DocumentEventCapturer dec;

	public EventReplayer(Connector con, JTextArea area, DocumentEventCapturer dec) {
		this.con = con;
		this.area = area;
		this.dec = dec;
	}

	public void run() {
		boolean wasInterrupted = false;
		while (!wasInterrupted) {
			try {
				MyTextEvent mte = con.take();
				if (mte instanceof TextInsertEvent) {
					final TextInsertEvent tie = (TextInsertEvent)mte;
					/* TODO: This should be replaced
					 * We should instead add the event to the COPY, remove the event from the local buffer, if it is in there, and replace the display with the COPY.
					 * We also need to place the cursor in its original position, if possible.
					 * If the cursor is placed outside of the text, it should be set to the last position in the text.
					 */
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							try {
								dec.disable();
								area.insert(tie.getText(), tie.getOffset());				
								dec.enable();
							} catch (Exception e) {
								System.err.println(e);
								/* We catch all axceptions, as an uncaught exception would make the 
								 * EDT unwind, which is now healthy.
								 */
							}
						}
					});
				} else if (mte instanceof TextRemoveEvent) {
					final TextRemoveEvent tre = (TextRemoveEvent)mte;
					/* TODO: Same as above
					 */
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							try {
								dec.disable();
								area.replaceRange(null, tre.getOffset(), tre.getOffset()+tre.getLength());
								dec.enable();
							} catch (Exception e) {
								System.err.println(e);
								/* We catch all axceptions, as an uncaught exception would make the 
								 * EDT unwind, which is now healthy.
								 */
							}
						}
					});
				} 
			} catch (Exception e) {
				e.printStackTrace();
				wasInterrupted = true;
			}
		}
		System.out.println("I'm the thread running the EventReplayer, now I die!");
	}

	public void waitForOneSecond() {
		try {
			Thread.sleep(1000);
		} catch(InterruptedException _) {
		}
	}
}
