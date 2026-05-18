import java.io.*;
import java.util.*;

public class PatchCase {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        File f = new File("data/c.dat");
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
        Map<Long, Object> cases = (Map<Long, Object>) ois.readObject();
        ois.close();
        
        boolean changed = false;
        for (Map.Entry<Long, Object> e : cases.entrySet()) {
            Object c = e.getValue();
            java.lang.reflect.Field f2 = c.getClass().getDeclaredField("solrSourceCore");
            f2.setAccessible(true);
            String current = (String) f2.get(c);
            System.out.println("Case id=" + e.getKey() + " solrSourceCore=" + current);
            if (!"shmcloud_2".equals(current)) {
                f2.set(c, "shmcloud_2");
                System.out.println("  -> fixed to shmcloud_2");
                changed = true;
            }
        }
        
        if (changed) {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
            oos.writeObject(cases);
            oos.close();
            System.out.println("Saved.");
        } else {
            System.out.println("All cores already correct.");
        }
    }
}
