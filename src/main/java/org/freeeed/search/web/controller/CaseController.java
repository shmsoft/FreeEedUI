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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.freeeed.search.files.CaseFileService;
import org.freeeed.search.web.WebConstants;
import org.freeeed.search.web.configuration.Configuration;
import org.freeeed.search.web.dao.cases.CaseDao;
import org.freeeed.search.web.model.Case;
import org.freeeed.search.web.model.User;
import org.freeeed.search.web.solr.SolrCoreService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.freeeed.search.web.StreamGobbler;

/**
 * 
 * Class CaseController.
 * 
 * @author ilazarov.
 *
 */
public class CaseController extends BaseController {
    private static final Logger log = Logger.getLogger(CaseController.class);
    private CaseDao caseDao;
    private Configuration configuration;
    private SolrCoreService solrCoreService;
    private CaseFileService caseFileService;
    String pattern = "HH:mm";
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

    @Override
    public ModelAndView execute() {
        // case creation also remotely identified by a remoteCreation
        String remoteCreation = (String) valueStack.get("remotecasecreation");
        Boolean remoteCaseCreation = remoteCreation != null && remoteCreation.equals("yes");
        if (!remoteCaseCreation
                && !loggedSiteVisitor.getUser().hasRight(User.Right.CASES)) {
            try {
                response.sendRedirect(WebConstants.MAIN_PAGE_REDIRECT);
                return new ModelAndView(WebConstants.CASE_PAGE);
            } catch (IOException e) {
            }
        }
        String isCLIValue = (String) valueStack.get("isCLI");
        Boolean isCLI = isCLIValue != null && isCLIValue.equals("yes");

        List<String> solrCores = solrCoreService.getSolrCores();
        valueStack.put("cores", solrCores);
        valueStack.put("usercase", new Case());

        String action = (String) valueStack.get("action");

        log.debug("Action called: " + action);

        if ("delete".equals(action)) {
            String caseIdStr = (String) valueStack.get("id");

            try {
                Long caseId = Long.parseLong(caseIdStr);
                caseDao.deleteCase(caseId);

                response.sendRedirect(WebConstants.LIST_CASES_PAGE_REDIRECT);
            } catch (Exception e) {
                log.error("Error delete case: " + e.getMessage());
            }
        } else if ("runprocessing".equals(action)) {
            String caseIdStr = (String) valueStack.get("id");
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Long caseId = Long.parseLong(caseIdStr);
            Case c = caseDao.findCase(caseId);
            try {
                executor.submit(() -> {
                    try {
                        // Use jar-based processing
                        runFreeeedProcess(c.getProjectFileLocation(), c);
                    } catch (Exception e) {
                        log.error("Error processing case: " + e.getMessage(), e);
                        c.setStatus("Error: " + e.getMessage());
                        caseDao.saveCase(c);
                    }
                });
                c.setStatus("Processing...");
                response.sendRedirect(WebConstants.LIST_CASES_PAGE_REDIRECT);
            } catch (Exception e) {
                c.setStatus("Error");
                log.error("Error initiating case processing: " + e.getMessage());
            } finally {
                caseDao.saveCase(c);
                executor.shutdown();

            }
        } else if ("edit".equals(action)) {
            try {
                String caseIdStr = (String) valueStack.get("id");
                Long caseId = Long.parseLong(caseIdStr);
                Case c = caseDao.findCase(caseId);

                valueStack.put("usercase", c);
            } catch (Exception e) {
                log.error("Error while edit case: " + e.getMessage());
            }
        } else if ("save".equals(action)) {

            List<String> errors = new ArrayList<String>();

            Long projectId = null;
            String projectIdStr = (String) valueStack.get("projectId");
            if (projectIdStr != null && projectIdStr.length() > 0) {
                try {
                    projectId = Long.parseLong(projectIdStr);
                } catch (Exception e) {
                }
            }
            Long id = null;
            String idStr = (String) valueStack.get("id");
            if (idStr != null && idStr.length() > 0) {
                try {
                    id = Long.parseLong(idStr);
                } catch (Exception e) {
                }
            } else if (isCLI) {
                id = projectId;
            }
            Case c = new Case();
            c.setId(id);
            c.setProjectId(projectId);
            String name = (String) valueStack.get("name");
            if (name == null || !name.matches("[a-zA-Z0-9\\-_ ]+")) {
                errors.add("Name is missing or invalid");
            }
            Date now = new Date();
            String description = (String) valueStack.get("description") + " " +
                    simpleDateFormat.format(now);
            if (!isValidField(description)) {
                errors.add("Description is missing");
            }
            c.setName(name);
            c.setDescription(description);

            valueStack.put("errors", errors);
            valueStack.put("usercase", c);
            String fileOption = (String) valueStack.get("fileOption");

            String dataFolder = "";
            String projectFileFolder = "";
            if (remoteCaseCreation) {
                log.info("Remote case creation request received");
                try {
                    String filesLocation = (String) valueStack.get("filesLocation");
                    String sourceDataLocation = (String) valueStack.get("sourceDataLocation");
                    String solrsource = (String) valueStack.get("solrsource");
                    String projectFileLocation = (String) valueStack.get("projectFileLocation");

                    log.info("Remote case params - filesLocation: " + filesLocation +
                            ", sourceDataLocation: " + sourceDataLocation +
                            ", solrsource: " + solrsource +
                            ", projectFileLocation: " + projectFileLocation +
                            ", name: " + name +
                            ", id: " + id);

                    c.setFilesLocation(filesLocation);
                    c.setSourceDataLocation(sourceDataLocation);
                    c.setSolrSourceCore(solrsource);
                    c.setProjectFileLocation(projectFileLocation);
                    c.setStatus("Completed");

                    Long savedId = caseDao.saveCase(c);
                    log.info("Remote case created successfully: " + c.getName() + ", id=" + savedId + ", Solr core: "
                            + solrsource);

                    // Return success response for remote caller
                    response.setStatus(200);
                    try {
                        response.getWriter().write("Case created successfully with ID: " + savedId);
                    } catch (IOException ioEx) {
                        log.warn("Failed writing success response for remote case creation", ioEx);
                    }
                    return null;
                } catch (Exception e) {
                    log.error("Error during remote case creation", e);
                    errors.add("Failed to create remote case: " + e.getMessage());
                    response.setStatus(500);
                    try {
                        response.getWriter().write("Error: " + e.getMessage());
                    } catch (IOException ioEx) {
                        log.warn("Failed writing error response for remote case creation", ioEx);
                    }
                    return null;
                }
            } else {
                if ("uploadFile".equals(fileOption)) {
                    MultipartFile file = (MultipartFile) valueStack.get("file");
                    if (file == null) {
                        errors.add("Uploaded File is missing or invalid");
                    }
                    String uploadedPath = caseFileService.uploadFile(file);
                    if (uploadedPath == null) {
                        errors.add("Failed to store uploaded file");
                        return new ModelAndView(WebConstants.CASE_PAGE);
                    }
                    dataFolder = uploadedPath;
                    c.setUploadedFile(uploadedPath);
                    File folderProject = new File(dataFolder);
                    c.setCaseGuid(folderProject.getParentFile().getName());
                    projectFileFolder = folderProject.getParentFile().getParentFile().getParent() + "/ProjectFiles";
                    c.setSourceDataLocation(folderProject.getParent());
                    if (!caseFileService.expandCaseFiles(dataFolder)) {
                        errors.add("Not able to use the uploaded file");
                        return new ModelAndView(WebConstants.CASE_PAGE);
                    }
                    folderProject.delete();
                    dataFolder = folderProject.getParent();
                } else {
                    String uploadBasePath = caseFileService.GetUploaderFolderPath();
                    // Remove trailing slashes to normalize
                    while (uploadBasePath.endsWith("/") || uploadBasePath.endsWith(File.separator)) {
                        uploadBasePath = uploadBasePath.substring(0, uploadBasePath.length() - 1);
                    }
                    projectFileFolder = uploadBasePath + File.separator + "ProjectFiles";

                    String sourceDataLocation = (String) valueStack.get("sourceDataLocation");
                    if (sourceDataLocation == null || sourceDataLocation.trim().isEmpty()) {
                        errors.add("Files Location is missing or invalid");
                    }
                    dataFolder = sourceDataLocation;
                    c.setSourceDataLocation(dataFolder);
                }

                if (errors.size() > 0) {
                    return new ModelAndView(WebConstants.CASE_PAGE);
                }

                Path path = Paths.get(projectFileFolder);
                try {
                    Files.createDirectories(path);
                    log.info("Created project file folder: " + projectFileFolder);
                } catch (Exception e) {
                    log.error("Failed to create project folder: " + projectFileFolder, e);
                    errors.add("Error creating project folder: " + e.getMessage()
                            + ". Please check Upload Folder Path in Application Settings.");
                }
                if (errors.size() > 0) {
                    return new ModelAndView(WebConstants.CASE_PAGE);
                }
                c.setStatus("New");
                LocalDateTime currentDateTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm");
                String formattedDateTime = currentDateTime.format(formatter);
                String fileProject = projectFileFolder + "/processed_file_" + formattedDateTime.replace(" ", "")
                        + ".project";

                c.setProjectFileLocation(fileProject);
                projectId = caseDao.saveCase(c);
                try {
                    String projectBaseInfo = readProjectTemplateFile();

                    // Build output paths using the user's requested base output location
                    String baseOutput = "/Users/jatin/Documents/WellomyTech/FreeEed/freeeed-processing/output/freeeed-output";
                    String projectBaseOut = baseOutput + "/" + projectId + "/output";
                    String outputDir = projectBaseOut + "/results";
                    String stagingDir = projectBaseOut + "/staging";
                    String flatInputPath = stagingDir + "/flat-input";

                    // Ensure directories exist
                    Files.createDirectories(Paths.get(outputDir));
                    Files.createDirectories(Paths.get(stagingDir));
                    Files.createDirectories(Paths.get(flatInputPath));

                    // Determine review endpoint dynamically from request
                    String reviewEndpoint = request.getScheme() + "://" + request.getServerName() + ":"
                            + request.getServerPort() + request.getContextPath() + "/";
                    log.info("Using review endpoint: " + reviewEndpoint);

                    projectBaseInfo = projectBaseInfo.replace("{CreatedDate}", formattedDateTime);
                    projectBaseInfo = projectBaseInfo.replace("{ProjectId}", projectId.toString());
                    projectBaseInfo = projectBaseInfo.replace("{inputFile}", dataFolder);
                    projectBaseInfo = projectBaseInfo.replace("{Description}", description);
                    projectBaseInfo = projectBaseInfo.replace("{Name}", name);
                    projectBaseInfo = projectBaseInfo.replace("{ProjectFileLocation}", fileProject);
                    projectBaseInfo = projectBaseInfo.replace("{ai_key}", this.configuration.getApiKey());
                    projectBaseInfo = projectBaseInfo.replace("{ReviewEndpoint}", reviewEndpoint);

                    // Inject configured Solr endpoint into project file
                    try {
                        String solrEndpoint = this.configuration.getSolrEndpoint();
                        // Escape for properties format as in template (http\://host\:port)
                        String escapedSolr = solrEndpoint
                                .replace("\\", "\\\\")
                                .replace("http://", "http\\://")
                                .replace("https://", "https\\://")
                                .replace(":", "\\:");
                        // Replace default value if present
                        projectBaseInfo = projectBaseInfo.replace("solr_endpoint=http\\://localhost\\:8983",
                                "solr_endpoint=" + escapedSolr);
                    } catch (Exception escEx) {
                        log.warn("Failed to inject configured Solr endpoint, using template default", escEx);
                    }

                    // Inject output-related placeholders
                    projectBaseInfo = projectBaseInfo.replace("{OutputDir}", outputDir);
                    projectBaseInfo = projectBaseInfo.replace("{StagingDir}", stagingDir);
                    projectBaseInfo = projectBaseInfo.replace("{FlatInputPath}", flatInputPath);

                    saveProjectFile(projectBaseInfo, fileProject);
                    response.sendRedirect(WebConstants.LIST_CASES_PAGE_REDIRECT);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }
        return new ModelAndView(WebConstants.CASE_PAGE);
    }

    /*
     * DEPRECATED: API-based processing - replaced with jar-based processing
     * Keeping for reference only
     */
    /*
     * private void processViaApi(Case c) {
     * String apiUrl = configuration.getApiUrl();
     * log.info("RunProcessing requested for caseId=" + c.getId() + ", caseName=" +
     * c.getName());
     * if (apiUrl == null || apiUrl.trim().isEmpty()) {
     * log.warn("API URL is not configured; skipping API processing");
     * c.setStatus("Error: API URL not configured");
     * caseDao.saveCase(c);
     * return;
     * }
     * 
     * File fileToSend = null;
     * try {
     * if (c.getUploadedFile() != null) {
     * File uploaded = new File(c.getUploadedFile());
     * log.info("Trying uploaded file path: " + uploaded.getAbsolutePath());
     * if (uploaded.exists()) {
     * fileToSend = uploaded;
     * } else {
     * log.warn("Uploaded file no longer exists, will try to zip sourceDataLocation"
     * );
     * }
     * }
     * if (fileToSend == null && c.getSourceDataLocation() != null) {
     * File src = new File(c.getSourceDataLocation());
     * log.info("Zipping sourceDataLocation: " + src.getAbsolutePath());
     * fileToSend = zipDirectoryToTemp(src);
     * }
     * if (fileToSend == null || !fileToSend.exists()) {
     * throw new
     * IllegalStateException("No file to send to processing API (uploaded ZIP deleted and source folder missing)"
     * );
     * }
     * 
     * String endpoint = apiUrl;
     * if (!endpoint.endsWith("/process/file")) {
     * if (!endpoint.endsWith("/")) {
     * endpoint += "/";
     * }
     * endpoint += "process/file";
     * }
     * log.info("Posting caseId=" + c.getId() + " to processing API: " + endpoint +
     * ", file=" + fileToSend.getAbsolutePath());
     * 
     * int status = postFileMultipart(endpoint, fileToSend, "file",
     * "application/zip");
     * if (status >= 200 && status < 300) {
     * c.setStatus("Completed");
     * log.info("API processing completed for caseId=" + c.getId() +
     * " with status: " + status);
     * } else {
     * c.setStatus("Error: API status " + status);
     * log.error("API processing failed for caseId=" + c.getId() + ": HTTP " +
     * status);
     * }
     * caseDao.saveCase(c);
     * } catch (Exception ex) {
     * log.error("Error while calling processing API for caseId=" + c.getId(), ex);
     * c.setStatus("Error: " + ex.getMessage());
     * caseDao.saveCase(c);
     * }
     * }
     * 
     * private int postFileMultipart(String urlStr, File file, String fieldName,
     * String contentType) throws IOException {
     * String boundary = "----FreeEedBoundary" + System.currentTimeMillis();
     * HttpURLConnection conn = null;
     * OutputStream out = null;
     * BufferedInputStream fileIn = null;
     * try {
     * URL url = new URL(urlStr);
     * conn = (HttpURLConnection) url.openConnection();
     * conn.setDoOutput(true);
     * conn.setDoInput(true);
     * conn.setRequestMethod("POST");
     * conn.setConnectTimeout(10000);
     * conn.setReadTimeout(60000);
     * conn.setRequestProperty("Accept", "application/json");
     * conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" +
     * boundary);
     * 
     * out = new BufferedOutputStream(conn.getOutputStream());
     * String CRLF = "\r\n";
     * String twoHyphens = "--";
     * 
     * // Start boundary
     * out.write((twoHyphens + boundary + CRLF).getBytes("UTF-8"));
     * String header = "Content-Disposition: form-data; name=\"" + fieldName +
     * "\"; filename=\"" + file.getName() + "\"" + CRLF
     * + "Content-Type: " + contentType + CRLF + CRLF;
     * out.write(header.getBytes("UTF-8"));
     * 
     * // File content
     * fileIn = new BufferedInputStream(new FileInputStream(file));
     * byte[] buffer = new byte[8192];
     * int len;
     * while ((len = fileIn.read(buffer)) != -1) {
     * out.write(buffer, 0, len);
     * }
     * out.write(CRLF.getBytes("UTF-8"));
     * 
     * // End boundary
     * String footer = twoHyphens + boundary + twoHyphens + CRLF;
     * out.write(footer.getBytes("UTF-8"));
     * out.flush();
     * 
     * int status = conn.getResponseCode();
     * try (InputStream is = (status >= 200 && status < 300) ? conn.getInputStream()
     * : conn.getErrorStream()) {
     * if (is != null) {
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * byte[] respBuf = new byte[2048];
     * int r;
     * while ((r = is.read(respBuf)) != -1) {
     * baos.write(respBuf, 0, r);
     * }
     * log.info("API response (status " + status + "): " + new
     * String(baos.toByteArray(), "UTF-8"));
     * }
     * }
     * return status;
     * } finally {
     * if (fileIn != null) try { fileIn.close(); } catch (IOException ignored) {}
     * if (out != null) try { out.close(); } catch (IOException ignored) {}
     * if (conn != null) conn.disconnect();
     * }
     * }
     * 
     * private File zipDirectoryToTemp(File dir) throws IOException {
     * if (dir == null || !dir.exists()) return null;
     * File tmpZip = File.createTempFile("case-src-", ".zip");
     * tmpZip.delete();
     * String zipPath = tmpZip.getAbsolutePath();
     * ZipUtil.createZipFile(zipPath, dir.getAbsolutePath());
     * return new File(zipPath);
     * }
     */

    private String runFreeeedProcess(String paramFile, Case c) {
        StringBuilder output = new StringBuilder();
        try {
            // Get the base directory (2 levels up from current working directory)
            Path currentRelativePath = Paths.get("");
            Path currentAbsolutePath = currentRelativePath.toAbsolutePath();
            // Search for FreeEed directory by traversing up
            File currentDirFile = new File(currentAbsolutePath.toString());
            File freeEedDir = null;

            // Traverse up to 5 levels to find FreeEed
            for (int i = 0; i < 5; i++) {
                if (currentDirFile == null)
                    break;
                log.info("Checking for FreeEed in sibling of: " + currentDirFile.getAbsolutePath());

                File checkDir = new File(currentDirFile, "FreeEed");
                if (checkDir.exists() && checkDir.isDirectory()) {
                    freeEedDir = checkDir;
                    break;
                }
                currentDirFile = currentDirFile.getParentFile();
            }

            String jarPath;
            File jarFile;

            if (freeEedDir != null) {
                log.info("Found FreeEed directory at: " + freeEedDir.getAbsolutePath());
                jarPath = freeEedDir.getAbsolutePath()
                        + "/freeeed-processing/target/freeeed-processing-1.0-SNAPSHOT-jar-with-dependencies.jar";
                jarFile = new File(jarPath);

                if (!jarFile.exists()) {
                    jarPath = freeEedDir.getAbsolutePath()
                            + "/target/freeeed-processing-1.0-SNAPSHOT-jar-with-dependencies.jar";
                    jarFile = new File(jarPath);
                }
            } else {
                // Fallback to original logic if not found (though likely won't work if loop
                // failed)
                String currentDirStr = currentAbsolutePath.getParent().toString();
                jarPath = currentDirStr
                        + "/FreeEed/freeeed-processing/target/freeeed-processing-1.0-SNAPSHOT-jar-with-dependencies.jar";
                jarFile = new File(jarPath);
            }

            log.info("Trying to run FreeEedMain, jar exists=" + jarFile.exists() + ", path=" + jarPath);

            if (!jarFile.exists()) {
                String errorMsg = "FreeEed processing jar not found at: " + jarPath;
                log.error(errorMsg);
                c.setStatus("Error: " + errorMsg);
                caseDao.saveCase(c);
                return errorMsg;
            }

            // Verify param file exists
            File paramFileObj = new File(paramFile);
            if (!paramFileObj.exists()) {
                String errorMsg = "Parameter file not found: " + paramFile;
                log.error(errorMsg);
                c.setStatus("Error: " + errorMsg);
                caseDao.saveCase(c);
                return errorMsg;
            }

            // Build command to run FreeEed processing
            String[] command = {
                    "java",
                    "-cp", jarPath,
                    "org.freeeed.main.FreeEedMain",
                    "-param_file", paramFile
            };

            log.info("Running command: " + String.join(" ", command));

            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // Set working directory to FreeEed base directory
            File workingDir;
            if (freeEedDir != null) {
                workingDir = new File(freeEedDir, "freeeed-processing");
                if (!workingDir.exists()) {
                    workingDir = freeEedDir;
                }
            } else {
                workingDir = new File(currentAbsolutePath.getParent().toString() + "/FreeEed/freeeed-processing/");
                if (!workingDir.exists()) {
                    workingDir = new File(currentAbsolutePath.getParent().toString() + "/FreeEed/");
                }
            }
            processBuilder.directory(workingDir);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Capture output
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), output::append);
            Executors.newSingleThreadExecutor().submit(outputGobbler);

            // Wait for process to complete
            int exitCode = process.waitFor();
            log.info("FreeEedMain finished with exitCode=" + exitCode);
            log.info("Process output: " + output.toString());

            if (exitCode == 0) {
                // Determine output directory from project (param) file and set filesLocation
                String outputDir = extractOutputDirFromProjectFile(paramFile);
                if (outputDir != null && !outputDir.trim().isEmpty()) {
                    c.setFilesLocation(outputDir);
                    // quick sanity: check metadata exists
                    File md = new File(outputDir, "metadata1.csv");
                    if (!md.exists()) {
                        log.warn("metadata1.csv not found under output dir: " + md.getAbsolutePath());
                    } else {
                        log.info("Detected processed output: " + md.getAbsolutePath());
                    }
                } else {
                    log.warn("Could not determine output-dir from project file: " + paramFile);
                }

                // Processing succeeded - set the Solr core name for the case
                String solrCore = "shmcloud_" + c.getId();
                c.setSolrSourceCore(solrCore);

                // Verify Solr core exists; log if missing so UI expectations are clear
                try {
                    List<String> cores = solrCoreService.getSolrCores();
                    if (cores != null && cores.contains(solrCore)) {
                        log.info("Verified Solr core exists: " + solrCore);
                    } else {
                        log.warn("Solr core not found after processing: " + solrCore + ". Ensure Solr is running at "
                                + configuration.getSolrEndpoint() + " and FreeEed indexing is enabled.");
                    }
                } catch (Exception verifyEx) {
                    log.warn("Failed to verify Solr cores", verifyEx);
                }

                c.setStatus("Completed");
                log.info("Case " + c.getId() + " processed successfully. Solr core: " + solrCore + ", outputDir="
                        + c.getFilesLocation());
            } else {
                c.setStatus("Error - Exit code: " + exitCode);
                log.error("Process exited with code: " + exitCode + ", output: " + output);
            }
            caseDao.saveCase(c);

        } catch (IOException e) {
            log.error("IOException running FreeEed process", e);
            c.setStatus("Error: " + e.getMessage());
            caseDao.saveCase(c);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted running FreeEed process", e);
            c.setStatus("Error: interrupted");
            caseDao.saveCase(c);
        }
        return output.toString();
    }

    // Extract output-dir value from the generated project (.project) file
    private String extractOutputDirFromProjectFile(String projectFilePath) {
        File f = new File(projectFilePath);
        if (!f.exists())
            return null;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("output-dir=")) {
                    return line.substring("output-dir=".length()).trim();
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read project file for output-dir: " + projectFilePath, e);
        }
        return null;
    }

    private String readProjectTemplateFile() throws IOException {
        String resourcePath = "template.project";

        // Get the resource URL
        ClassLoader classLoader = CaseController.class.getClassLoader();
        URL resourceUrl = classLoader.getResource(resourcePath);
        StringBuilder content = new StringBuilder();
        if (resourceUrl != null) {
            String absolutePath = resourceUrl.getPath();
            try (BufferedReader reader = new BufferedReader(new FileReader(absolutePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
        }

        return content.toString();
    }

    private void saveProjectFile(String content, String fileName) throws IOException {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(content);
        }

    }

    private boolean isValidField(String value) {
        return value != null && !value.isEmpty();
    }

    public void setCaseDao(CaseDao caseDao) {
        this.caseDao = caseDao;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setSolrCoreService(SolrCoreService solrCoreService) {
        this.solrCoreService = solrCoreService;
    }

    public void setCaseFileService(CaseFileService caseFileService) {
        this.caseFileService = caseFileService;
    }
}
