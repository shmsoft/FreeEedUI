/*
 *
 * Copyright SHMsoft, Inc. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.freeeed.search.files;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.freeeed.search.web.model.elasticsearch.SearchDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Class FileService.
 *
 * @author ilazarov.
 */
public class CaseFileService {
    private static final Logger log = Logger.getLogger(CaseFileService.class);
    private static final String FILES_DIR = "files";
    private static final String FILES_TMP_DIR = "tmp";
    private static final String UPLOAD_DIR = "uploads";
    private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private static final String LOAD_FILE = "loadfile";

    /**
     * Expanding the files for a given case. They should be in zip
     * format and will be unzipped to the case's directory.
     *
     * @param caseName
     * @param zipFile
     * @return true if the expand operation complete successfully.
     */
    public boolean expandCaseFiles(String caseName, String zipFile) {
        try {
            log.debug("Start unzip for case: " + caseName + ", zipFile: " + zipFile);

            File location = new File(FILES_DIR + File.separator + caseName);
            if (!location.exists()) {
                location.mkdirs();
            }

            ZipUtil.unzipFile(zipFile, location.getAbsolutePath());

            return true;
        } catch (Exception e) {
            log.error("Problem unpacking: " + zipFile, e);

            return false;
        }
    }

    public String uploadFile(MultipartFile file) {
        File uploadDir = new File(UPLOAD_DIR);
        uploadDir.mkdirs();

        String destinationFile = UPLOAD_DIR + File.separator + df.format(new Date()) + "-" + file.getOriginalFilename();
        File destination = new File(destinationFile);
        try {
            file.transferTo(destination);

            return destinationFile;
        } catch (Exception e) {
            log.error("Problem uploading file: ", e);

            return null;
        }
    }

    public boolean uploadLoadFile(MultipartFile file, String caseName) {
        File destination = new File(FILES_DIR + File.separator + caseName + File.separator + LOAD_FILE + File.separator + file.getOriginalFilename());
        try {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            file.transferTo(destination);
            return true;
        } catch (Exception e) {
            log.error("Problem uploading file: ", e);

            return false;
        }
    }

    public File getNativeFile(String caseName, String documentOriginalPath, String uniqueId) {
        String fileName = documentOriginalPath.contains(File.separator) ?
                documentOriginalPath.substring(documentOriginalPath.lastIndexOf(File.separator) + 1) : documentOriginalPath;

        fileName = uniqueId + "_" + fileName;

        File dir = new File(FILES_DIR + File.separator + caseName + File.separator + "native");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.getName().endsWith(fileName)) {
                    return file;
                }
            }
        }

        return null;
    }

    public File getNativeFileFromSource(String location, String source, String documentOriginalPath) throws IOException {
        String fileName = source + File.separator + documentOriginalPath;
        File f = new File(fileName);
        if (f.exists()) {
            File newFile = new File(location + File.separator + documentOriginalPath);
            FileUtils.copyFile(f, newFile);
            return newFile;
        } else {
            int extIndex = documentOriginalPath.lastIndexOf(".");
            if (extIndex != -1) {
                String ext = documentOriginalPath.substring(documentOriginalPath.lastIndexOf(".") + 1);
                if ("eml".equalsIgnoreCase(ext)) {
                    fileName = source + File.separator + documentOriginalPath.substring(0, extIndex);
                    f = new File(fileName);
                    if (f.exists()) {
                        File newFile = new File(location + File.separator + documentOriginalPath);
                        FileUtils.copyFile(f, newFile);
                        return newFile;
                    }
                }
            }
        }

        return null;
    }

    public File getHtmlFile(String caseName, String documentOriginalPath, String uniqueId) {
        String fileName = documentOriginalPath.contains(File.separator) ?
                documentOriginalPath.substring(documentOriginalPath.lastIndexOf(File.separator) + 1) : documentOriginalPath;

        fileName = uniqueId + "_" + fileName;

        File dir = new File(FILES_DIR + File.separator + caseName + File.separator + "html");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.getName().endsWith(fileName + ".html")) {
                    return file;
                }
            }
        }

        return null;
    }

    public File getHtmlImageFile(String caseName, String documentOriginalPath) {
        return new File(FILES_DIR + File.separator + caseName + File.separator + "html" + File.separator + documentOriginalPath);
    }

    public File getImageFile(String caseName, String documentOriginalPath, String uniqueId) {
        String fileName = documentOriginalPath.contains(File.separator) ?
                documentOriginalPath.substring(documentOriginalPath.lastIndexOf(File.separator) + 1) : documentOriginalPath;

        fileName = uniqueId + "_" + fileName;

        File dir = new File(FILES_DIR + File.separator + caseName + File.separator + "pdf");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.getName().endsWith(fileName + ".pdf")) {
                    return file;
                }
            }
        }

        return null;
    }

    public File getImageFiles(String caseName, List<SearchDocument> docs) {
        List<File> imageFiles = new ArrayList<File>();
        for (SearchDocument doc : docs) {
            File file = getImageFile(caseName, doc.getDocumentPath(), doc.getUniqueId());
            if (file != null) {
                imageFiles.add(file);
            }
        }

        File tmpDir = new File(FILES_TMP_DIR);
        tmpDir.mkdirs();

        String zipFileName = FILES_TMP_DIR + File.separator + "imgtmp" + System.currentTimeMillis() + ".zip";
        try {
            ZipUtil.createZipFile(zipFileName, imageFiles);
        } catch (IOException e) {
            log.error("Problem creating zip file", e);
            return null;
        }

        return new File(zipFileName);
    }

    public File getNativeFiles(String caseName, List<SearchDocument> docs) {
        List<File> imageFiles = new ArrayList<File>();
        for (SearchDocument doc : docs) {
            File file = getNativeFile(caseName, doc.getDocumentPath(), doc.getUniqueId());
            if (file != null) {
                imageFiles.add(file);
            }
        }

        File tmpDir = new File(FILES_TMP_DIR);
        tmpDir.mkdirs();

        String zipFileName = FILES_TMP_DIR + File.separator + "nattmp" + System.currentTimeMillis() + ".zip";
        try {
            ZipUtil.createZipFile(zipFileName, imageFiles);
        } catch (IOException e) {
            log.error("Problem creating zip file", e);
            return null;
        }

        return new File(zipFileName);
    }

    public File getNativeFilesFromSource(String source, List<SearchDocument> docs) throws IOException {
        File tmpDir = new File(FILES_TMP_DIR);
        tmpDir.mkdirs();

        long ts = System.currentTimeMillis();
        String zipFileName = FILES_TMP_DIR + File.separator + "nattmp" + ts + ".zip";
        String zipFileDirName = FILES_TMP_DIR + File.separator + "nattmp" + ts;
        File zipFileDir = new File(zipFileDirName);

        zipFileDir.mkdirs();

        for (SearchDocument doc : docs) {
            getNativeFileFromSource(zipFileDirName, source, doc.getDocumentPath());
        }

        try {
            ZipUtil.createZipFile(zipFileName, zipFileDirName);
        } catch (IOException e) {
            log.error("Problem creating zip file", e);
            return null;
        }
        return new File(zipFileName);
    }

    public File getTaggedLoadFile(String caseName, Map<String, String> hashDocWithAllTags) {
        File dir = new File(FILES_DIR + File.separator + caseName + File.separator + LOAD_FILE);
        File loadFile = null;
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (!file.getName().equalsIgnoreCase(".DS_Store")) {
                    loadFile = file;
                    break;
                }
            }
        }
        if (loadFile == null) {
            return null;
        }
        String destination = File.separator + FILES_TMP_DIR + File.separator + LOAD_FILE;
        File result = new File(destination);
        if (!result.exists()) {
            result.mkdirs();
        }
        destination += File.separator + new Date().getTime() + ".csv";
        try {
            CSVReader csvReader = new CSVReader(new FileReader(loadFile), '|');
            CSVWriter csvWriter = new CSVWriter(new FileWriter(destination), '|');
            String[] nextLine;
            int hashIndex = getHashIndex(csvReader, csvWriter);
            if (hashIndex != -1) {
                while ((nextLine = csvReader.readNext()) != null) {

                    String hash = nextLine[hashIndex];
                    if (hashDocWithAllTags.containsKey(hash)) {
                        String tag = hashDocWithAllTags.get(hash);
                        if (!tag.isEmpty()) {
                            String[] line = addTag(nextLine, tag);
                            csvWriter.writeNext(line);
                        } else {
                            csvWriter.writeNext(nextLine);
                        }
                    }
                }
            }
            csvReader.close();
            csvWriter.close();
        } catch (Exception ex) {
            log.error("error reading csv", ex);
        }
        return new File(destination);
    }

    private String[] prepareNewHeader(String[] nextLine) {
        String[] line = new String[nextLine.length + 1];
        int i = 0;
        for (; i < nextLine.length; i++) {
            line[i] = nextLine[i];
        }
        line[i] = "Hash";
        return line;
    }

    private String[] addTag(String[] nextLine, String tag) {
        String[] line = new String[nextLine.length + 1];
        int i = 0;
        for (; i < nextLine.length; i++) {
            line[i] = nextLine[i];
        }
        line[i] = tag;
        return line;
    }

    private int getHashIndex(CSVReader csvReader, CSVWriter csvWriter) throws IOException {
        String[] nextLine;
        String[] newHeader = new String[0];
        int index = -1;
        if ((nextLine = csvReader.readNext()) != null) {
            newHeader = new String[nextLine.length + 1];
            int i = 0;
            for (; i < nextLine.length; i++) {
                if ("Hash".equalsIgnoreCase(nextLine[i])) {
                    index = i;
                }
                newHeader[i] = nextLine[i];
            }
            newHeader[i] = "Entry";
        }
        csvWriter.writeNext(newHeader);
        return index;
    }
}
