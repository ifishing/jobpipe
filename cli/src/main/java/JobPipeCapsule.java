import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JobPipeCapsule extends Capsule {
  protected JobPipeCapsule(Capsule pred) {
    super(pred);
  }

  @Override
  protected <T> T attribute(Map.Entry<String, T> attr) {
    if (attr == ATTR_APP_CLASS_PATH) {
      final List<Object> args = new ArrayList<>(super.attribute(ATTR_APP_CLASS_PATH));
      ArrayList<String> dirs = new ArrayList<>();
      dirs.add(new File(getJarFile().toFile().getParent(), "lib").getAbsolutePath());
      String dirsString = System.getProperty("jobpipe.cp");
      if (dirsString != null && !dirsString.isEmpty()) {
        dirs.addAll(Arrays.asList(dirsString.split(":")));
      }
      for (String dir : dirs) {
        File file = new File(dir);
        if (file.exists() && file.isDirectory()) {
          for (File f : file.listFiles()) {
            if (!f.isDirectory()) {
              args.add(f.getAbsolutePath());
            }
          }
        } else if (file.exists()) {
          args.add(file.getAbsolutePath());
        }
      }
      return (T) args;
    }
    return super.attribute(attr);
  }
}
