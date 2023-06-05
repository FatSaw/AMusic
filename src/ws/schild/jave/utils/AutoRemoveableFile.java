package ws.schild.jave.utils;

import java.io.File;

/**
 * Use this class in a try-with-resources block to automatically delete the referenced file when
 * this goes out of scope.
 *
 * @author mressler
 */
public class AutoRemoveableFile extends File implements AutoCloseable {

  private static final long serialVersionUID = 1270202558229293283L;

  public AutoRemoveableFile(File parent, String child) {
    super(parent, child);
  }

  @Override
  public void close() {
    delete();
  }
}
