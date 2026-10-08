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
import org.freeeed.search.web.configuration.Configuration;
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
    private Configuration configuration;

    private static final Logger log = Logger.getLogger(CaseFileService.class);
    private static final String FILES_DIR = "files";
    private static final String FILES_TMP_DIR = "tmp";

    private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    
    /**
     * 
     * Expanding the files for a given case. They should be in zip
     * format and will be unzipped to the case's directory.
     * 
     * @param zipFile
     * 
     * @return true if the expand operation complete successfully.
     */
    public boolean expandCaseFiles(String zipFile) {
        try {
            File location = new File(zipFile);
            ZipUtil.unzipFile(zipFile, location.getParent());
            return true;
        } catch (Exception e) {
            log.error("Problem unpacking: " + zipFile, e);
            
            return false;
        }
    }

    public String CreatSourceFilesFolder() {
        File uploadDir = new File(GetUploaderFolderPath() + java.util.UUID.randomUUID() + File.separator);
        uploadDir.mkdirs();
        String destinationFolder = uploadDir.getAbsolutePath();
        return destinationFolder;
    }

    public String GetUploaderFolderPath() {
        String path = this.configuration.getUploadFolderPath();
        String basePath = (path != null && path.trim().length() > 0) ? path : System.getProperty("user.home", ".") + File.separator + "FreeEedData";
        // Normalize: remove trailing slashes
        while (basePath.endsWith("/") || basePath.endsWith(File.separator)) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        File baseDir = new File(basePath);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        String uploads = basePath + File.separator + "uploads";
        File uploadsDir = new File(uploads);
        if (!uploadsDir.exists()) {
            uploadsDir.mkdirs();
        }
        return uploads;
    }
    
    public String uploadFile(MultipartFile file) {
        String destinationFolder = CreatSourceFilesFolder();
        String destinationFile = destinationFolder  + File.separator + df.format(new Date()) + "-" + file.getOriginalFilename();
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
        File file = new File(documentOriginalPath);
        if (!file.exists()) {
            file = new File(sourceDataLocation);
            String fileName = file.getParent() + File.separator + documentOriginalPath;
            file = new File(fileName);
        }
        return file.exists() ? file : null;
    }
    public File getNativeFile(String projectOutputPath, String documentOriginalPath, String uniqueId) {
        return getNativeFile(projectOutputPath, null, documentOriginalPath, uniqueId);
    }
    
    public File getNativeFile(String projectOutputPath, String sourceDataLocation, String documentOriginalPath, String uniqueId) {
        log.info("getNativeFile called: projectOutputPath=" + projectOutputPath + ", uniqueId=" + uniqueId);
        if (uniqueId == null || uniqueId.isEmpty()) return null;
        File dir = new File(projectOutputPath + File.separator + "native");
        String prefix = uniqueId + "_";
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith(prefix)) {
                        return file;
                    }
                }
            }
        } else {
            // Try extracting from native1.zip
            File extracted = extractFromZip(projectOutputPath + File.separator + "native1.zip", "native/" + prefix, "");
            if (extracted != null) {
                log.info("Extracted file: " + extracted.getAbsolutePath());
                return extracted;
            }
        }
        
        // Fallback to sourceDataLocation
        log.info("Falling back to sourceDataLocation for: " + documentOriginalPath);
        return getNativeFile(sourceDataLocation, documentOriginalPath);
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
    
    public File getImageFile(String projectOutputPath, String documentOriginalPath, String uniqueId) {
        if (uniqueId == null || uniqueId.isEmpty()) return null;
        String prefix = uniqueId + "_";
        // Generated PDF renditions from imaging ("Create PDF") land in the
        // "images" folder (older layouts used "pdf"). Check exploded dirs first,
        // then native1.zip. NOTE: they are NOT in "native/" -- that holds the
        // originals; only a doc whose native was already a PDF appears there.
        for (String sub : new String[] {"images", "pdf"}) {
            File dir = new File(projectOutputPath + File.separator + sub);
            if (dir.exists()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().startsWith(prefix) && file.getName().endsWith(".pdf")) {
                            return file;
                        }
                    }
                }
            }
        }
        // Fallback: pull the rendition out of native1.zip (under images/).
        File fromZip = extractFromZip(projectOutputPath + File.separator + "native1.zip", "images/" + prefix, ".pdf");
        if (fromZip != null) {
            return fromZip;
        }
        // Legacy: a doc whose native was already a PDF may only exist under native/.
        return extractFromZip(projectOutputPath + File.separator + "native1.zip", "native/" + prefix, ".pdf");
    }

    private File extractFromZip(String zipFilePath, String prefix, String suffix) {
        log.info("extractFromZip called: zipFilePath=" + zipFilePath + ", prefix=" + prefix);
        File zipFile = new File(zipFilePath);
        if (!zipFile.exists()) {
            log.info("Zip file does not exist: " + zipFilePath);
            return null;
        }
        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zipFile)) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                if (entry.getName().startsWith(prefix) && entry.getName().endsWith(suffix)) {
                    File tmpFile = new File(FILES_TMP_DIR, entry.getName().replace("/", "_"));
                    log.info("Found match in zip! Extracting to: " + tmpFile.getAbsolutePath());
                    if (!tmpFile.exists()) {
                        tmpFile.getParentFile().mkdirs();
                        try (java.io.InputStream is = zf.getInputStream(entry);
                             java.io.FileOutputStream fos = new java.io.FileOutputStream(tmpFile)) {
                            org.apache.commons.io.IOUtils.copy(is, fos);
                        }
                    }
                    return tmpFile;
                }
            }
            log.info("No match found in zip for prefix=" + prefix);
        } catch (Exception e) {
            log.error("Error extracting from zip: " + zipFilePath, e);
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
    
    /**
     * Merge the per-document PDF renditions of the given documents into a single
     * PDF, in the order provided. Reuses the PDFs already produced during imaging
     * (found via {@link #getImageFile}) rather than re-rendering -- fast, and the
     * output is exactly what was reviewed.
     *
     * Documents with no PDF rendition are SKIPPED. That is easy to hit without
     * realising it: with "Create PDF images" off nothing is rendered, yet
     * getImageFile still falls back to natives that were already PDFs, so an
     * "export all" can succeed while containing only a fraction of the results.
     * The counts are therefore returned to the caller, which must tell the user --
     * a partial export that looks complete is dangerous in review.
     */
    public MergeResult mergePdfs(String projectOutputPath, List<SolrDocument> docs) {
        File tmpDir = new File(FILES_TMP_DIR);
        tmpDir.mkdirs();
        String outName = FILES_TMP_DIR + File.separator + "pdftmp" + System.currentTimeMillis() + ".pdf";

        org.apache.pdfbox.multipdf.PDFMergerUtility merger = new org.apache.pdfbox.multipdf.PDFMergerUtility();
        merger.setDestinationFileName(outName);

        int added = 0, missing = 0;
        for (SolrDocument doc : docs) {
            File pdf = getImageFile(projectOutputPath, doc.getDocumentPath(), doc.getUniqueId());
            if (pdf != null && pdf.exists() && pdf.getName().toLowerCase().endsWith(".pdf")) {
                try {
                    merger.addSource(pdf);
                    added++;
                } catch (Exception e) {
                    log.error("Problem adding PDF rendition for uniqueId=" + doc.getUniqueId(), e);
                    missing++;
                }
            } else {
                missing++;
            }
        }

        if (added == 0) {
            log.warn("mergePdfs: no PDF renditions found for the " + docs.size() + " requested document(s)");
            return new MergeResult(null, 0, missing, docs.size());
        }

        try {
            // Merge in main memory (no scratch temp file). setupTempFileOnly()
            // writes a scratch file to the JVM temp dir, which isn't reliably
            // available under Tomcat and failed with "No such file or directory".
            merger.mergeDocuments(org.apache.pdfbox.io.MemoryUsageSetting.setupMainMemoryOnly());
        } catch (IOException e) {
            log.error("Problem merging PDFs", e);
            return new MergeResult(null, 0, docs.size(), docs.size());
        }

        log.info("mergePdfs: merged " + added + " PDF(s), " + missing + " without a rendition");
        return new MergeResult(new File(outName), added, missing, docs.size());
    }

    // ------------------------------------------------------------------
    // Redaction rendering (FOIA manual redaction -- docs/decisions/redaction.md)
    // ------------------------------------------------------------------

    /** Number of pages in a document's PDF rendition, or 0 if none / unreadable. */
    public int pdfPageCount(String projectOutputPath, String documentOriginalPath, String uniqueId) {
        File pdf = getImageFile(projectOutputPath, documentOriginalPath, uniqueId);
        if (pdf == null || !pdf.exists()) {
            return 0;
        }
        try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.pdmodel.PDDocument.load(pdf)) {
            return doc.getNumberOfPages();
        } catch (Exception e) {
            log.error("Could not read page count for uniqueId=" + uniqueId, e);
            return 0;
        }
    }

    /**
     * Rasterize one page of a document's PDF rendition to a PNG, for the review
     * viewer to draw redaction boxes over. This is a throw-away preview image --
     * the rendition and native are never modified. Returns null if there is no
     * rendition or the page is out of range.
     */
    public File renderRenditionPagePng(String projectOutputPath, String documentOriginalPath,
                                       String uniqueId, int page1Based, int dpi) {
        File pdf = getImageFile(projectOutputPath, documentOriginalPath, uniqueId);
        if (pdf == null || !pdf.exists()) {
            return null;
        }
        new File(FILES_TMP_DIR).mkdirs();
        try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.pdmodel.PDDocument.load(pdf)) {
            int idx = page1Based - 1;
            if (idx < 0 || idx >= doc.getNumberOfPages()) {
                return null;
            }
            org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
            java.awt.image.BufferedImage image =
                    renderer.renderImageWithDPI(idx, dpi, org.apache.pdfbox.rendering.ImageType.RGB);
            File out = new File(FILES_TMP_DIR, "redpage_" + safe(uniqueId) + "_" + page1Based + "_" + dpi + ".png");
            // Backstop: this is an unredacted page image (exempt content). The caller
            // deletes it after serving; deleteOnExit ensures it never outlives the JVM
            // even if that send fails.
            out.deleteOnExit();
            javax.imageio.ImageIO.write(image, "png", out);
            return out;
        } catch (Exception e) {
            log.error("Could not render rendition page for uniqueId=" + uniqueId + " page=" + page1Based, e);
            return null;
        }
    }

    private String safe(String s) {
        return s == null ? "x" : s.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    /**
     * Produce the redacted <b>release set</b>: for every requested document, each
     * page of its PDF rendition is rasterized, the accepted redaction boxes are
     * <b>burned in</b> (filled, with the exemption code printed on the box), and
     * the page is rebuilt as an image -- so the released PDF has <b>no recoverable
     * text layer</b> under a redaction (true removal, not an overlay). A redaction
     * log is prepended as a cover page. Originals/renditions are untouched.
     *
     * Documents without a PDF rendition are skipped and counted, exactly like
     * {@link #mergePdfs}, so the caller can flag a partial release.
     */
    public MergeResult mergeRedactedPdfs(String projectOutputPath, String caseName,
                                         List<SolrDocument> docs,
                                         java.util.Map<String, java.util.List<org.freeeed.search.web.model.redaction.Redaction>> byDoc,
                                         int dpi) {
        new File(FILES_TMP_DIR).mkdirs();
        String outName = FILES_TMP_DIR + File.separator + "redtmp" + System.currentTimeMillis() + ".pdf";

        int added = 0, missing = 0, totalBoxes = 0;
        java.util.List<String> logLines = new ArrayList<String>();

        try (org.apache.pdfbox.pdmodel.PDDocument out = new org.apache.pdfbox.pdmodel.PDDocument()) {
            for (SolrDocument doc : docs) {
                File pdf = getImageFile(projectOutputPath, doc.getDocumentPath(), doc.getUniqueId());
                if (pdf == null || !pdf.exists() || !pdf.getName().toLowerCase().endsWith(".pdf")) {
                    missing++;
                    continue;
                }
                // Redactions are keyed by uniqueId -- the stable per-document key the
                // viewer, the rendition lookup (getImageFile) and selected-export all share.
                java.util.List<org.freeeed.search.web.model.redaction.Redaction> boxes = byDoc.get(doc.getUniqueId());
                try (org.apache.pdfbox.pdmodel.PDDocument src = org.apache.pdfbox.pdmodel.PDDocument.load(pdf)) {
                    org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(src);
                    int pages = src.getNumberOfPages();
                    int docBoxes = 0;
                    java.util.Set<String> docCodes = new java.util.LinkedHashSet<String>();
                    for (int p = 0; p < pages; p++) {
                        java.awt.image.BufferedImage image =
                                renderer.renderImageWithDPI(p, dpi, org.apache.pdfbox.rendering.ImageType.RGB);
                        int applied = burnBoxes(image, boxes, p + 1, docCodes);
                        docBoxes += applied;
                        addImagePage(out, image, dpi);
                    }
                    added++;
                    totalBoxes += docBoxes;
                    String label = doc.getDocumentPath() != null && !doc.getDocumentPath().isEmpty()
                            ? new File(doc.getDocumentPath()).getName() : doc.getDocumentId();
                    logLines.add(label + " -- " + docBoxes + " redaction(s)"
                            + (docCodes.isEmpty() ? "" : "; codes: " + String.join(", ", docCodes)));
                } catch (Exception e) {
                    log.error("Problem redacting rendition for uniqueId=" + doc.getUniqueId(), e);
                    missing++;
                }
            }

            if (added == 0) {
                return new MergeResult(null, 0, missing, docs.size());
            }

            // Prepend the redaction-log cover page (first page of the release).
            prependLogPage(out, caseName, added, totalBoxes, logLines);

            out.save(outName);
        } catch (Exception e) {
            log.error("Problem building redacted PDF", e);
            return new MergeResult(null, 0, docs.size(), docs.size());
        }

        log.info("mergeRedactedPdfs: " + added + " doc(s), " + totalBoxes + " redaction(s), "
                + missing + " without a rendition");
        File result = new File(outName);
        result.deleteOnExit(); // don't let the generated release PDF linger past JVM exit
        return new MergeResult(result, added, missing, docs.size());
    }

    /** Burn the boxes for one page into the raster. Returns how many were applied. */
    private int burnBoxes(java.awt.image.BufferedImage image,
                          java.util.List<org.freeeed.search.web.model.redaction.Redaction> boxes,
                          int page1Based, java.util.Set<String> codesSeen) {
        if (boxes == null || boxes.isEmpty()) {
            return 0;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        java.awt.Graphics2D g = image.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int applied = 0;
        try {
            for (org.freeeed.search.web.model.redaction.Redaction r : boxes) {
                if (r.getPage() != page1Based) {
                    continue;
                }
                int rx = (int) Math.round(r.getX() * w);
                int ry = (int) Math.round(r.getY() * h);
                int rw = (int) Math.round(r.getW() * w);
                int rh = (int) Math.round(r.getH() * h);
                if (rw <= 0 || rh <= 0) {
                    continue;
                }
                g.setColor(java.awt.Color.BLACK);
                g.fillRect(rx, ry, rw, rh);
                // Label the box with the exemption basis (never a plain black box).
                String code = r.getExemptionCode();
                if (code != null && !code.trim().isEmpty()) {
                    codesSeen.add(code.trim());
                    int fontSize = Math.max(8, Math.min(rh - 4, Math.round(h * 0.014f)));
                    g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, fontSize));
                    g.setColor(java.awt.Color.WHITE);
                    java.awt.FontMetrics fm = g.getFontMetrics();
                    String text = code.trim();
                    if (fm.stringWidth(text) <= rw - 4 && fm.getHeight() <= rh) {
                        int tx = rx + 3;
                        int ty = ry + fm.getAscent() + Math.max(1, (rh - fm.getHeight()) / 2);
                        g.drawString(text, tx, ty);
                    }
                }
                applied++;
            }
        } finally {
            g.dispose();
        }
        return applied;
    }

    /** Append an image as a full page (image-only -> no recoverable text). */
    private void addImagePage(org.apache.pdfbox.pdmodel.PDDocument out,
                              java.awt.image.BufferedImage image, int dpi) throws IOException {
        float wPt = image.getWidth() * 72f / dpi;
        float hPt = image.getHeight() * 72f / dpi;
        org.apache.pdfbox.pdmodel.PDPage page =
                new org.apache.pdfbox.pdmodel.PDPage(new org.apache.pdfbox.pdmodel.common.PDRectangle(wPt, hPt));
        out.addPage(page);
        org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject img =
                org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory.createFromImage(out, image, 0.8f);
        try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                     new org.apache.pdfbox.pdmodel.PDPageContentStream(out, page)) {
            cs.drawImage(img, 0, 0, wPt, hPt);
        }
    }

    /** Build the redaction-log cover page and move it to the front of the release. */
    private void prependLogPage(org.apache.pdfbox.pdmodel.PDDocument out, String caseName,
                                int docCount, int totalBoxes, java.util.List<String> logLines) throws IOException {
        org.apache.pdfbox.pdmodel.PDPage page =
                new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER);
        float margin = 54f;
        float y = org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER.getHeight() - margin;
        float width = org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER.getWidth() - 2 * margin;
        try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                     new org.apache.pdfbox.pdmodel.PDPageContentStream(out, page)) {
            y = writeLine(cs, org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 16, margin, y, "FreeEed -- Redaction Log");
            y -= 6;
            y = writeLine(cs, org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10, margin, y,
                    "Case: " + (caseName == null ? "" : caseName));
            y = writeLine(cs, org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10, margin, y,
                    "Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new Date()));
            y = writeLine(cs, org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10, margin, y,
                    "Release: " + docCount + " document(s), " + totalBoxes + " redaction(s) applied.");
            y -= 6;
            y = writeLine(cs, org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_OBLIQUE, 9, margin, y,
                    "Redacted content is permanently removed from the released pages (rasterized image,");
            y = writeLine(cs, org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_OBLIQUE, 9, margin, y,
                    "no recoverable text under a redaction). Each box prints its exemption basis.");
            y -= 10;
            int shown = 0;
            for (String line : logLines) {
                if (y < margin + 24) {
                    y = writeLine(cs, org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 9, margin, y,
                            "... and " + (logLines.size() - shown) + " more (full list omitted for space).");
                    break;
                }
                for (String wrapped : wrap(line, 95)) {
                    y = writeLine(cs, org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 9, margin, y, wrapped);
                }
                shown++;
            }
        }
        // Insert the finished log page at the front (content stream is already closed).
        out.getPages().insertBefore(page, out.getPage(0));
    }

    private float writeLine(org.apache.pdfbox.pdmodel.PDPageContentStream cs,
                            org.apache.pdfbox.pdmodel.font.PDType1Font font, int size,
                            float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitizeForPdf(text));
        cs.endText();
        return y - (size + 3);
    }

    // WinAnsi (PDType1Font) cannot encode arbitrary characters; keep the log to a safe subset.
    private String sanitizeForPdf(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c >= 32 && c < 127 ? c : '?');
        }
        return sb.toString();
    }

    private java.util.List<String> wrap(String s, int max) {
        java.util.List<String> out = new ArrayList<String>();
        if (s == null) {
            return out;
        }
        while (s.length() > max) {
            int cut = s.lastIndexOf(' ', max);
            if (cut <= 0) {
                cut = max;
            }
            out.add(s.substring(0, cut));
            s = s.substring(cut).trim();
        }
        out.add(s);
        return out;
    }

    /**
     * Outcome of {@link #mergePdfs}: the merged file (null if nothing could be
     * merged) plus how many documents made it in and how many were skipped, so
     * the caller can warn instead of silently handing over a partial export.
     */
    public static class MergeResult {
        private final File file;
        private final int added;
        private final int missing;
        private final int requested;

        public MergeResult(File file, int added, int missing, int requested) {
            this.file = file;
            this.added = added;
            this.missing = missing;
            this.requested = requested;
        }

        public File getFile() { return file; }
        public int getAdded() { return added; }
        public int getMissing() { return missing; }
        public int getRequested() { return requested; }
        public boolean isPartial() { return missing > 0 && added > 0; }
        public boolean isEmpty() { return file == null || added == 0; }
    }

    public File getNativeFiles(String projectOutputPath, String sourceDataLocation, List<SolrDocument> docs) {
        List<File> imageFiles = new ArrayList<>();
        for (SolrDocument doc : docs) {
            File file = getNativeFile(projectOutputPath, sourceDataLocation, doc.getDocumentPath(), doc.getUniqueId());
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

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

}
