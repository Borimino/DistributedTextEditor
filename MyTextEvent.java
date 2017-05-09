import java.io.*;
import java.util.UUID;
/**
 * 
 * @author Jesper Buus Nielsen
 *
 */
public class MyTextEvent extends MyMessage implements Serializable{
	MyTextEvent(int offset) {
		this.offset = offset;
		this.uuid = UUID.randomUUID();
	}
	private int offset;
	private UUID uuid;
	int getOffset() { return offset; }
	UUID getUUID() { return uuid; }
}
