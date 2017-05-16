import java.io.*;

public class SyncronizeTextEvent extends MyTextEvent implements Serializable {
  private String copyText;

  public SyncronizeTextEvent(String copyText) {
    super(0);
    this.copyText = copyText;
  }

  public String getCopyText() {
    return this.copyText;
  }
}
