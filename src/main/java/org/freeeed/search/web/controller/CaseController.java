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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.freeeed.search.files.CaseFileService;
import org.freeeed.search.web.WebConstants;
import org.freeeed.search.web.dao.cases.CaseDao;
import org.freeeed.search.web.model.Case;
import org.freeeed.search.web.model.ProcessingStatus;
import org.freeeed.search.web.model.User;
import org.freeeed.search.web.solr.SolrCoreService;
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
        String remoteCreation = (String) valueStack.get("removecasecreation");
        
        if ((remoteCreation == null || !remoteCreation.equals("yes")) 
                && !loggedSiteVisitor.getUser().hasRight(User.Right.CASES)) {
            try {
                response.sendRedirect(WebConstants.MAIN_PAGE_REDIRECT);
                return new ModelAndView(WebConstants.CASE_PAGE);
            } catch (IOException e) {
            }
        }
        
        List<String> solrCores = solrCoreService.getSolrCores();
        valueStack.put("cores", solrCores);
        valueStack.put("usercase", new Case());
        
        String action = (String) valueStack.get("action");
        
        log.debug("Action called: " + action);
        if("create".equals(action)){
            return new ModelAndView(WebConstants.CASE_PAGE);
        }
        else if ("delete".equals(action)) {
            String caseIdStr = (String) valueStack.get("id");
            
            try {
                Long caseId = Long.parseLong(caseIdStr);
                caseDao.deleteCase(caseId);
                
                response.sendRedirect(WebConstants.LIST_CASES_PAGE_REDIRECT);
            } catch (Exception e) {
                log.error("Error delete case: " + e.getMessage());
            }
        } else if ("save".equals(action)) {
            
            List<String> errors = new ArrayList<>();

            Case c = new Case();
            String name = (String) valueStack.get("name");
            if (name == null || !name.matches("[a-zA-Z0-9\\-_ ]+")) {
                errors.add("Name is missing or invalid");
            }
            Date now = new Date();
            String description = valueStack.get("description") + " " + simpleDateFormat.format(now);
            if (!isValidField(description)) {
                errors.add("Description is missing");
            }
            c.setName(name);
            c.setDescription(description);

            valueStack.put("errors", errors);
            valueStack.put("usercase", c);
            
            if (!errors.isEmpty()) {
                return new ModelAndView(WebConstants.CASE_PAGE);
            }
            
            String filePath = (String) valueStack.get("filesLocation");
            if (filePath != null && !filePath.isEmpty()) {
                c.setUploadedFile(filePath);
                c.setStatus(ProcessingStatus.PROCESSING_PENDING);
            }
            caseDao.saveCase(c);
            try {
                response.sendRedirect(WebConstants.LIST_CASES_PAGE_REDIRECT);
            } catch (IOException e) {
            }
        }
        
        return new ModelAndView(WebConstants.CASE_PAGE);
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
