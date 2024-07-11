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
import org.freeeed.search.web.dao.cases.CaseDao;
import org.freeeed.search.web.model.Case;
import org.freeeed.search.web.model.User;
import org.freeeed.search.web.solr.SolrCoreService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;


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
    private SolrCoreService solrCoreService;
    private CaseFileService caseFileService;

    String pattern = "HH:mm";
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

    @Override
    public ModelAndView execute() {
        //case creation also remotely identified by a remoteCreation
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
        }
        else if ("runprocessing".equals(action))
        {
            String caseIdStr = (String) valueStack.get("id");
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Long caseId = Long.parseLong(caseIdStr);
            Case c = caseDao.findCase(caseId);
            try {
                executor.submit(() -> {
                    try {
                        runFreeeedProcess(c.getProjectFileLocation(), c);
                    } catch (Exception e) {
                        log.error("Error processing case: " + e.getMessage());
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
        }
        else if ("edit".equals(action)) {
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
            }
            else if(isCLI)
            {
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
                String filesLocation = (String) valueStack.get("filesLocation");
                c.setFilesLocation(filesLocation);
                String sourceDataLocation = (String) valueStack.get("sourceDataLocation");
                c.setSourceDataLocation(sourceDataLocation);
                String solrsource = (String) valueStack.get("solrsource");
                c.setSolrSourceCore(solrsource);
                String projectFileLocation = (String) valueStack.get("projectFileLocation");
                c.setProjectFileLocation(projectFileLocation);
                c.setStatus("Completed");
                caseDao.saveCase(c);
            } else {
                if ("uploadFile".equals(fileOption)) {
                    MultipartFile file = (MultipartFile) valueStack.get("file");
                    if (file == null) {
                        errors.add("Uploaded File is missing or invalid");
                    }
                    dataFolder = caseFileService.uploadFile(file);
                    File folderProject = new File(dataFolder);
                    projectFileFolder = folderProject.getParent() + "/ProjectFiles";
                    c.setSourceDataLocation(dataFolder);
                } else {

                    projectFileFolder = caseFileService.GetUploaderFolderPath() + "/ProjectFiles";
                    String sourceDataLocation = (String) valueStack.get("sourceDataLocation");
                    if (sourceDataLocation == null) {
                        errors.add("Files Location is missing or invalid");
                    }
                    c.setSourceDataLocation(sourceDataLocation);
                    dataFolder = sourceDataLocation;
                }
                Path path = Paths.get(projectFileFolder);
                try {
                    Files.createDirectories(path);
                } catch (Exception e) {
                    errors.add("Error creation project folder");
                }
                if (errors.size() > 0) {
                    return new ModelAndView(WebConstants.CASE_PAGE);
                }
                c.setStatus("New");
                LocalDateTime currentDateTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm");
                String formattedDateTime = currentDateTime.format(formatter);
                String fileProject = projectFileFolder + "/processed_file_" + formattedDateTime.replace(" ", "") + ".project";

                c.setProjectFileLocation(fileProject);
                projectId = caseDao.saveCase(c);
                try {
                    String projectBaseInfo = readProjectTemplateFile();
                    projectBaseInfo = projectBaseInfo.replace("{CreatedDate}", formattedDateTime);
                    projectBaseInfo = projectBaseInfo.replace("{ProjectId}", projectId.toString());
                    projectBaseInfo = projectBaseInfo.replace("{inputFile}", dataFolder);
                    projectBaseInfo = projectBaseInfo.replace("{Description}", description);
                    projectBaseInfo = projectBaseInfo.replace("{Name}", name);
                    projectBaseInfo = projectBaseInfo.replace("{ProjectFileLocation}", fileProject);

                    saveProjectFile(projectBaseInfo, fileProject);
                    // runFreeeedProcess(fileProject);
                    response.sendRedirect(WebConstants.LIST_CASES_PAGE_REDIRECT);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }
        return new ModelAndView(WebConstants.CASE_PAGE);
    }

    private String runFreeeedProcess(String paramFile, Case c) {

        try {
            Path currentRelativePath = Paths.get("");
            Path currentAbsolutePath = currentRelativePath.toAbsolutePath();
            String currentDir = currentAbsolutePath.getParent().getParent().toString();
            // Construct the command
            String[] command = {
                    "java",
                    "-cp", "freeeed-processing-1.0-SNAPSHOT-jar-with-dependencies.jar",
                    "org.freeeed.main.FreeEedMain",
                    "-param_file", paramFile
            };
            // Create a ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // Set the working directory
            processBuilder.directory(new File(currentDir + "/FreeEed/target/"));
            // Start the process
            Process process = processBuilder.start();

            // Read the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            // Read each line from the BufferedReader and append to the StringBuilder
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
            reader.close();
            // Wait for the process to complete
            int exitCode = process.waitFor();
            return output.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
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

    public void setSolrCoreService(SolrCoreService solrCoreService) {
        this.solrCoreService = solrCoreService;
    }

    public void setCaseFileService(CaseFileService caseFileService) {
        this.caseFileService = caseFileService;
    }
}
