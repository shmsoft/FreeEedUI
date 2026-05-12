import java.io.*;

public class TestExtract {
    public static void main(String[] args) throws Exception {
        String projectOutputPath = "/Users/jatin/Downloads/freeeed_complete_pack/FreeEed/output/freeeed-output/1/output/results";
        String uniqueId = "UPI_00001";
        
        System.out.println("Starting extraction...");
        File dir = new File(projectOutputPath + File.separator + "native");
        String prefix = uniqueId + "_";
        if (dir.exists()) {
            System.out.println("Dir exists");
        } else {
            System.out.println("Dir does NOT exist");
            File zipFile = new File(projectOutputPath + File.separator + "native1.zip");
            System.out.println("ZipFile exists: " + zipFile.exists());
            
            try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zipFile)) {
                java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zf.entries();
                while (entries.hasMoreElements()) {
                    java.util.zip.ZipEntry entry = entries.nextElement();
                    if (entry.getName().startsWith("native/" + prefix)) {
                        System.out.println("Match! " + entry.getName());
                        File tmpFile = new File("tmp", entry.getName().replace("/", "_"));
                        System.out.println("TmpFile path: " + tmpFile.getAbsolutePath());
                        if (!tmpFile.exists()) {
                            System.out.println("Extracting...");
                            tmpFile.getParentFile().mkdirs();
                            try (InputStream is = zf.getInputStream(entry);
                                 FileOutputStream fos = new FileOutputStream(tmpFile)) {
                                org.apache.commons.io.IOUtils.copy(is, fos);
                            }
                        }
                        System.out.println("Done! Length: " + tmpFile.length());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
