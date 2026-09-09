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
package org.freeeed.search.web.controller;

import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.freeeed.search.files.CaseFileService;
import org.freeeed.search.web.WebConstants;
import org.freeeed.search.web.model.Case;
import org.freeeed.search.web.model.solr.SolrDocument;
import org.freeeed.search.web.model.solr.SolrEntry;
import org.freeeed.search.web.model.solr.SolrResult;
import org.freeeed.search.web.session.SolrSessionObject;
import org.freeeed.search.web.solr.QuerySearch;
import org.freeeed.search.web.solr.SolrSearchService;
import org.springframework.web.servlet.ModelAndView;

/**
 * 
 * Class CaseFileDownloadController.
 * 
 * @author ilazarov.
 *
 */
public class CaseFileDownloadController extends SecureController {
    private static final Logger log = Logger.getLogger(CaseFileDownloadController.class);
    
    private CaseFileService caseFileService;
    private SolrSearchService searchService;
    
    @Override
    public ModelAndView execute() {
        HttpSession session = this.request.getSession(true);
        SolrSessionObject solrSession = (SolrSessionObject) 
            session.getAttribute(WebConstants.WEB_SESSION_SOLR_OBJECT);
        
        if (solrSession == null || solrSession.getSelectedCase() == null) {
            return new ModelAndView(WebConstants.CASE_FILE_DOWNLOAD);
        }
        
        Case selectedCase = solrSession.getSelectedCase();
        
        String action = (String) valueStack.get("action");
        
        log.debug("Action called: " + action);
        
        File toDownload = null;
        boolean htmlMode = false;
        boolean pdfMode = false;
        org.freeeed.search.files.CaseFileService.MergeResult mergeResult = null;
        
        String docPath = (String) valueStack.get("docPath");
        String docName = (String) valueStack.get("docName");
        String uniqueId = (String) valueStack.get("uniqueId");
        boolean isPreviewPDF = (valueStack.get("ispreviewpdf") != null ? valueStack.get("ispreviewpdf") : "") .equals("1");
        boolean isPreviewImage= (valueStack.get("ispreviewimage") != null ? valueStack.get("ispreviewimage") : "") .equals("1");

        boolean uniqueIdAsName = false;
        try {
            if ("exportNative".equals(action)) {
                toDownload = caseFileService.getNativeFile(selectedCase.getSourceDataLocation(), docPath);
                uniqueIdAsName = true;
            } else if ("exportImage".equals(action)) {
                toDownload = caseFileService.getImageFile(selectedCase.getFilesLocation(), docName, uniqueId);
            } else if ("exportHtml".equals(action)) {
                    String projectPath = selectedCase.getFilesLocation();
                    toDownload = caseFileService.getHtmlFile(projectPath, docName, uniqueId);
                    htmlMode = true;
            } else if ("exportHtmlImage".equals(action)) {
                toDownload = caseFileService.getHtmlImageFile(selectedCase.getName(), docPath);
                htmlMode = true;
            } else if ("exportReport".equals(action)) {
                List<QuerySearch> queries = solrSession.getQueries();
                String query =solrSession.buildSearchQuery();
                int rows = solrSession.getTotalDocuments();
                List<SolrDocument> docs = getDocumentsMetadata(query, 0, rows);
                toDownload = caseFileService.generateHtmlReport(selectedCase.getName(), docs, queries);
                htmlMode = true;
            }
            else if ("exportNativeAll".equals(action)) {
                String query = solrSession.buildSearchQuery();
                int rows = solrSession.getTotalDocuments();
                            
                List<SolrDocument> docs = getDocumentPaths(query, 0, rows);
                
                toDownload = caseFileService.getNativeFiles(selectedCase.getFilesLocation(), selectedCase.getSourceDataLocation(), docs);
                
            } else if ("exportNativeSelected".equals(action)) {
                String docPathsStr = (String) valueStack.get("docPaths");
                String uidsStr = (String) valueStack.get("uniqueIds");
                if (docPathsStr != null && uidsStr != null && !docPathsStr.trim().isEmpty() && !uidsStr.trim().isEmpty()) {
                    String[] paths = docPathsStr.split("\\|\\|\\|");
                    String[] uids = uidsStr.split("\\|\\|\\|");
                    List<SolrDocument> docs = new ArrayList<SolrDocument>();
                    for (int i = 0; i < Math.min(paths.length, uids.length); i++) {
                        SolrDocument doc = new SolrDocument();
                        doc.setDocumentPath(paths[i]);
                        doc.setUniqueId(uids[i]);
                        docs.add(doc);
                    }
                    toDownload = caseFileService.getNativeFiles(selectedCase.getFilesLocation(), selectedCase.getSourceDataLocation(), docs);
                }
            } else if ("exportNativeAllFromSource".equals(action)) {
                String query = solrSession.buildSearchQuery();
                int rows = solrSession.getTotalDocuments();
                
                List<SolrDocument> docs = getDocumentPaths(query, 0, rows);
                
                String source = (String) valueStack.get("source");
                try {
                    source = URLDecoder.decode(source, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                }
                
                toDownload = caseFileService.getNativeFilesFromSource(source, docs);                
            } else if ("exportImageAll".equals(action)) {
                String query = solrSession.buildSearchQuery();
                int rows = solrSession.getTotalDocuments();

                List<SolrDocument> docs = getDocumentPaths(query, 0, rows);
                toDownload = caseFileService.getImageFiles(selectedCase.getFilesLocation(), docs);
            } else if ("exportPdfAll".equals(action)) {
                // Combine every result's PDF rendition into one PDF, in result order.
                String query = solrSession.buildSearchQuery();
                int rows = solrSession.getTotalDocuments();

                List<SolrDocument> docs = getDocumentPaths(query, 0, rows);
                mergeResult = caseFileService.mergePdfs(selectedCase.getFilesLocation(), docs);
                toDownload = mergeResult.getFile();
                pdfMode = true;
            } else if ("exportPdfSelected".equals(action)) {
                // Combine the checked documents' PDF renditions into one PDF.
                String docPathsStr = (String) valueStack.get("docPaths");
                String uidsStr = (String) valueStack.get("uniqueIds");
                if (docPathsStr != null && uidsStr != null && !docPathsStr.trim().isEmpty() && !uidsStr.trim().isEmpty()) {
                    String[] paths = docPathsStr.split("\\|\\|\\|");
                    String[] uids = uidsStr.split("\\|\\|\\|");
                    List<SolrDocument> docs = new ArrayList<SolrDocument>();
                    for (int i = 0; i < Math.min(paths.length, uids.length); i++) {
                        SolrDocument doc = new SolrDocument();
                        doc.setDocumentPath(paths[i]);
                        doc.setUniqueId(uids[i]);
                        docs.add(doc);
                    }
                    mergeResult = caseFileService.mergePdfs(selectedCase.getFilesLocation(), docs);
                    toDownload = mergeResult.getFile();
                    pdfMode = true;
                }
            }
        } catch (Exception e) {
            log.error("Problem sending cotent", e);
            valueStack.put("error", true);
        }
        
        if (toDownload != null) {
            try {
                int length = 0;
                ServletOutputStream outStream = response.getOutputStream();
                String mimetype =  "application/octet-stream";
                if(isPreviewPDF) {
                    mimetype = "application/pdf";
                }
                if(isPreviewImage) {
                    mimetype = "image/jpeg";
                }
                if (htmlMode) {
                    mimetype = "text/html";
                }
                if (pdfMode) {
                    mimetype = "application/pdf";
                }

                response.setContentType(mimetype);
                response.setContentLength((int) toDownload.length());
                String fileName = toDownload.getName();
                if(uniqueIdAsName)
                {
                    String extension = fileName.lastIndexOf('.') != -1 ? fileName.substring(fileName.lastIndexOf('.')) : "";
                    fileName = uniqueId + extension;
                }
                if (pdfMode) {
                    // A friendly name for the combined PDF, not the tmp file name.
                    // If documents were skipped for want of a rendition, say so IN THE
                    // FILENAME -- this is a file download, so there is no page left to
                    // put a banner on, and a partial export that looks complete is the
                    // dangerous case. Also emit the counts as a header for scripting.
                    if (mergeResult != null && mergeResult.isPartial()) {
                        fileName = selectedCase.getName() + "-PARTIAL-"
                                + mergeResult.getAdded() + "of" + mergeResult.getRequested() + ".pdf";
                        response.setHeader("X-FreeEed-Pdf-Merged", mergeResult.getAdded()
                                + " of " + mergeResult.getRequested()
                                + "; " + mergeResult.getMissing() + " had no PDF rendition");
                        log.warn("exportPdf: PARTIAL export -- " + mergeResult.getAdded() + " of "
                                + mergeResult.getRequested() + " documents had a PDF rendition. "
                                + "Enable 'Create PDF images' and reprocess for a complete export.");
                    } else {
                        fileName = selectedCase.getName() + ".pdf";
                    }
                }

                if (!htmlMode && !isPreviewPDF) {
                    // sets HTTP header
                   response.setHeader("Content-Disposition", "attachment; filename=\""
                           + fileName + "\"");
                }
    
                byte[] byteBuffer = new byte[1024];
                DataInputStream in = new DataInputStream(new FileInputStream(
                        toDownload));
    
                // reads the file's bytes and writes them to the response stream
                while ((in != null) && ((length = in.read(byteBuffer)) != -1)) {
                    outStream.write(byteBuffer, 0, length);
                }
    
                in.close();
                outStream.close();
                return null;
            } catch (Exception e) {
                log.error("Problem sending cotent", e);
                if (htmlMode || isPreviewPDF || isPreviewImage) {
                    try {
                        response.setContentType("text/html");
                        response.getWriter().write("<html><body style='padding: 20px; font-family: sans-serif;'><h3>Preview Error</h3><p>Could not read the document content.</p></body></html>");
                        response.getWriter().close();
                        return null;
                    } catch (Exception ex) {}
                }
                valueStack.put("error", true);
            }
        } else {
            if (pdfMode && mergeResult != null && mergeResult.isEmpty()) {
                // Distinguish "no renditions exist" from a generic failure: with
                // "Create PDF images" off nothing is rendered, so a PDF export can
                // never be complete and usually cannot be produced at all.
                log.warn("exportPdf: nothing to merge -- none of " + mergeResult.getRequested()
                        + " document(s) has a PDF rendition. 'Create PDF images' was probably off"
                        + " when this case was processed.");
                valueStack.put("errorMessage", "No PDF renditions exist for these documents, so a PDF"
                        + " export cannot be produced. Enable \"Create PDF images\" and reprocess"
                        + " the case, then try again.");
            }
            if (htmlMode || isPreviewPDF || isPreviewImage) {
                try {
                    response.setContentType("text/html");
                    response.getWriter().write("<html><body style='padding: 20px; font-family: sans-serif;'><h3>Preview Not Available</h3><p>The native file was not found on the server.</p></body></html>");
                    response.getWriter().close();
                    return null;
                } catch (Exception ex) {}
            }
            valueStack.put("error", true);
        }
        
        return new ModelAndView(WebConstants.CASE_FILE_DOWNLOAD);
    }



    private List<SolrDocument> getDocumentPaths(String query, int from, int rows) {
        SolrResult solrResult = searchService.search(query, from, rows, null, false, "id,document_original_path,UPI");
        List<SolrDocument> result = new ArrayList<SolrDocument>(solrResult.getTotalSize());
        result.addAll(solrResult.getDocuments().values());
        return result;
    }

    private List<SolrDocument> getDocumentsMetadata(String query, int from, int rows) {
        SolrResult solrResult = searchService.search(query, from, rows, null, false, "");
        List<SolrDocument> result = new ArrayList<SolrDocument>(solrResult.getTotalSize());
        result.addAll(solrResult.getDocuments().values());
        return result;
    }

    public void setCaseFileService(CaseFileService caseFileService) {
        this.caseFileService = caseFileService;
    }
    
    public void setSearchService(SolrSearchService searchService) {
        this.searchService = searchService;
    }
}
