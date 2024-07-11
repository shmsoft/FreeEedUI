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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.apache.commons.io.FileUtils;
import org.freeeed.search.web.model.solr.SolrDocument;
import org.freeeed.search.web.model.solr.SolrEntry;
import org.freeeed.search.web.solr.QuerySearch;
import org.springframework.web.multipart.MultipartFile;

/**
 * 
 * Class FileService.
 * 
 * @author ilazarov.
 *
 */
public class CaseFileService {
    private static final Logger log = Logger.getLogger(CaseFileService.class);
    private static final String FILES_DIR = "files";
    private static final String FILES_TMP_DIR = "tmp";
    private static final String UPLOADED_FOLDER = "/home/freeeed/FreeEedData/uploads/";

    private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    
    /**
     * 
     * Expanding the files for a given case. They should be in zip
     * format and will be unzipped to the case's directory.
     * 
     * @param caseName
     * @param zipFile
     * 
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

    public String CreatSourceFilesFolder() {
        File uploadDir = new File(UPLOADED_FOLDER);
        uploadDir.mkdirs();
        String destinationFolder = UPLOADED_FOLDER + File.separator;
        return destinationFolder;
    }

    public String GetUploaderFolderPath() {
        return UPLOADED_FOLDER + File.separator;
    }
    
    public String uploadFile(MultipartFile file) {
        String destinationFolder = CreatSourceFilesFolder();
        String destinationFile = destinationFolder + df.format(new Date()) + "-" + file.getOriginalFilename();
        File destination = new File(destinationFile);
        try {
            file.transferTo(destination);
            return destinationFile;
        } catch (Exception e) {
            log.error("Problem uploading file: ", e);
            return null;
        }
    }

    public File getNativeFile(String sourceDataLocation, String documentOriginalPath) {
        String fileName = documentOriginalPath.contains(File.separator) ?
                documentOriginalPath.substring(documentOriginalPath.lastIndexOf(File.separator) + 1) : documentOriginalPath;
        fileName = sourceDataLocation + fileName;
        return  new File(fileName);
    }
    public File getNativeFile(String caseName, String documentOriginalPath, String uniqueId) {
        String fileName = documentOriginalPath.contains(File.separator) ?
                documentOriginalPath.substring(documentOriginalPath.lastIndexOf(File.separator) + 1) : documentOriginalPath;
        
        fileName = uniqueId + "_" + fileName;        
                
        File dir = new File(FILES_DIR + File.separator + caseName + File.separator + "native");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.getName().equals(fileName)) {
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
    
    public File getHtmlFile(String projectOutputPath, String documentOriginalPath, String uniqueId) {

        documentOriginalPath = projectOutputPath + File.separator + "html_output" + File.separator + documentOriginalPath + ".html";
                
        File file = new File(documentOriginalPath);
        if (file.exists()) {
            return file;
        }
        return null;
    }

    public File generateHtmlReport(String caseName, List<SolrDocument> elements, List<QuerySearch> queries) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>")
                .append("body { font-family: Arial, sans-serif; margin: 20px; font-size: 12px; }")
                .append("h1 { color: #333; font-size: 18px; }")
                .append("h2 { color: #555; font-size: 16px; }")
                .append("h3 { font-size: 15px; padding: 5; margin: 0px; background: gray; color: white; }")
                .append("ul { list-style-type: none; padding: 0; }")
                .append("li { background: #f1f1f1; margin: 0px 0; padding: 2px; border: 1px solid #ddd; border-radius: 3px; }")
                .append(".highlight-0 { background-color: yellow; }")
                .append(".highlight-1 { background-color: lightgreen; }")
                .append(".highlight-2 { background-color: lightblue; }")
                .append("</style></head><body>");
        html.append("<h1>Documents Report - ").append(elements.size()).append("</h1>");
        html.append("<ul>");

        for (int i = 0; i < elements.size(); i++) {
                SolrDocument element  = elements.get(i);
                html.append("<li><h3><strong>Document ").append(element.getDocumentId()).append("</strong></h3><ul class='document_details'>");
                for (SolrEntry entry : element.getEntries()) {
                    if (entry.getKey().equals("text")) {
                        String fullText = entry.getValue();
                        String highlightedText = highlightText(fullText, queries);
                        String trimmedText = fullText.length() > 300 ? fullText.substring(0, 300) + "..." : fullText;
                        trimmedText = highlightText(trimmedText, queries);
                        html.append("<li>")
                                .append("<strong>").append(entry.getKey()).append("</strong>")
                                .append(": <span id='short-text-").append(i).append("'>").append(trimmedText).append("</span>")
                                .append("<span id='full-text-").append(i).append("' style='display:none;'>").append(highlightedText).append("</span>")
                                .append(" <a href='#' id='toggle-link-").append(i).append("' onclick='toggleText(").append(i).append("); return false;'>Read more</a>")
                                .append("</li>");
                    } else {
                        html.append("<li>")
                                .append("<strong>").append(entry.getKey()).append("</strong>")
                                .append(": ")
                                .append(entry.getValue())
                                .append("</li>");
                    }
                }
                html.append("</ul></li>");
            }
            html.append("</ul></div>");
        html.append("<script>")
                .append("function toggleText(index) {")
                .append("  var shortText = document.getElementById('short-text-' + index);")
                .append("  var fullText = document.getElementById('full-text-' + index);")
                .append("  var link = document.getElementById('toggle-link-' + index);")
                .append("  if (shortText.style.display === 'none') {")
                .append("    shortText.style.display = 'inline';")
                .append("    fullText.style.display = 'none';")
                .append("    link.textContent = 'Read more';")
                .append("  } else {")
                .append("    shortText.style.display = 'none';")
                .append("    fullText.style.display = 'inline';")
                .append("    link.textContent = 'Read less';")
                .append("  }")
                .append("}")
                .append("</script>");

        html.append("</body></html>");

        File file = new File(caseName  + ".html" );

        // Write the HTML content to the file
        try (PrintWriter out = new PrintWriter(file)) {
            out.println(html);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Return the file object
        return file;
    }

    private String highlightText(String text, List<QuerySearch> queries) {
        String highlightedText = text;
        for (int i = 0; i < queries.size(); i++) {
            String query = queries.get(i).getQuery();
            // Escape special characters in the query
            String escapedQuery = Pattern.quote(query);
            // Create a regex pattern with the escaped query
            Pattern pattern = Pattern.compile("(?i)(" + escapedQuery + ")");
            Matcher matcher = pattern.matcher(highlightedText);
            highlightedText = matcher.replaceAll("<span class='highlight-" + i + "'>$1</span>");
        }
        return highlightedText;
    }
    
    public File getHtmlImageFile(String caseName, String documentOriginalPath) {        
        File file = new File(FILES_DIR + File.separator + caseName + File.separator + "html" + File.separator + documentOriginalPath);
        return file;
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
    
    public File getImageFiles(String caseName, List<SolrDocument> docs) {
        List<File> imageFiles = new ArrayList<File>();
        for (SolrDocument doc : docs) {
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
        
        File res = new File(zipFileName);
        return res;
    }
    
    public File getNativeFiles(String caseName, List<SolrDocument> docs) {
        List<File> imageFiles = new ArrayList<>();
        for (SolrDocument doc : docs) {
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
        
        File res = new File(zipFileName);
        return res;
    }

    public File getHtmlReport(String caseName, List<SolrDocument> docs) {
        List<File> imageFiles = new ArrayList<>();
        for (SolrDocument doc : docs) {
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

        File res = new File(zipFileName);
        return res;
    }
    
    public File getNativeFilesFromSource(String source, List<SolrDocument> docs) throws IOException {
        File tmpDir = new File(FILES_TMP_DIR);
        tmpDir.mkdirs();
        
        long ts =  System.currentTimeMillis();
        String zipFileName = FILES_TMP_DIR + File.separator + "nattmp" + ts + ".zip";
        String zipFileDirName = FILES_TMP_DIR + File.separator + "nattmp" + ts;
        File zipFileDir = new File(zipFileDirName);
        
        zipFileDir.mkdirs();
        
        for (SolrDocument doc : docs) {
            getNativeFileFromSource(zipFileDirName, source, doc.getDocumentPath());
        }

        try {
            ZipUtil.createZipFile(zipFileName, zipFileDirName);
        } catch (IOException e) {
            log.error("Problem creating zip file", e);
            return null;
        }
        
        File res = new File(zipFileName);
        return res;
    }
}
