import java.io.*;
import java.util.zip.*;
import java.util.*;

public class TestZip {
    public static void main(String[] args) throws Exception {
        File zipFile = new File("/Users/jatin/Downloads/freeeed_complete_pack/FreeEed/output/freeeed-output/1/output/results/native1.zip");
        System.out.println("Zip exists: " + zipFile.exists());
        String prefix = "native/UPI_00001_";
        String suffix = "";
        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zipFile)) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                if (entry.getName().startsWith(prefix) && entry.getName().endsWith(suffix)) {
                    System.out.println("Match found: " + entry.getName());
                }
            }
        }
    }
}
