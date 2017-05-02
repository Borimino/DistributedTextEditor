import javax.swing.JTextArea;
import java.awt.EventQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
  private LinkedBlockingQueue<MyTextEvent> localEvents;
  private JTextArea copyArea;

	public EventReplayer(Connector con, JTextArea area, DocumentEventCapturer dec, JTextArea copyArea) {
		this.con = con;
		this.area = area;
		this.dec = dec;
    this.copyArea = copyArea;
    localEvents = new LinkedBlockingQueue<MyTextEvent>();
	}

	public void run() {
		boolean wasInterrupted = false;
		while (!wasInterrupted) {
			try {
				MyTextEvent mte = con.take();
        if ( mte == null ) continue;
        // If the event is the same as the first event in the localEvents list, then remove the event from local events.

        System.out.println("EventUUID: " + mte.getUUID().toString());
        if (localEvents.peek() != null) {
          System.out.println("LocalEventUUID: " + localEvents.peek().getUUID().toString());

        }
        if (localEvents.peek() != null && localEvents.peek().getUUID().equals(mte.getUUID())) {
            System.out.println("Found match: " + localEvents.peek().getUUID().toString());
            localEvents.take();
        }
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
								copyArea.insert(tie.getText(), tie.getOffset());
								dec.enable();
                resetDisplayedArea();
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
								copyArea.replaceRange(null, tre.getOffset(), tre.getOffset()+tre.getLength());
								dec.enable();
                resetDisplayedArea();
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

  public void addLocalEvent(MyTextEvent event) {
    try {
      localEvents.put(event);
    } catch (InterruptedException e) {
      // TODO: Handle interruption
    }
  }

  private void resetDisplayedArea() {
    dec.disable();
    int caretPosition = area.getCaretPosition();
    area.setText(copyArea.getText());
    for(MyTextEvent event : localEvents) {
      if (event instanceof TextInsertEvent) {
        final TextInsertEvent tie = (TextInsertEvent)event;
        try {
          area.insert(tie.getText(), tie.getOffset());
        } catch (Exception e) {
          System.err.println(e);
          /* We catch all axceptions, as an uncaught exception would make the
           * EDT unwind, which is now healthy.
           */
        }
      } else if (event instanceof TextRemoveEvent) {
        final TextRemoveEvent tre = (TextRemoveEvent)event;
        try {
          area.replaceRange(null, tre.getOffset(), tre.getOffset()+tre.getLength());
        } catch (Exception e) {
          System.err.println(e);
        }
      }
    }
    area.setCaretPosition(caretPosition);
    // TODO: Handle caret errors eg. out of bounds.
    dec.enable();
  }
}

