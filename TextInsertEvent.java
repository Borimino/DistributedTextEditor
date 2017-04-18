import java.io.*;
/**
 * 
 * @author Jesper Buus Nielsen
 *
 */
public class TextInsertEvent extends MyTextEvent implements Serializable {

	private String text;
	
	public TextInsertEvent(int offset, String text) {
		super(offset);
		this.text = text;
	}
	public String getText() { return text; }
}

