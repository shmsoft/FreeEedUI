package org.freeeed.search.web.controller;/*
 *@created 23/05/2024- 12:08
 *@author neha
 */

import org.apache.log4j.Logger;
import org.freeeed.search.web.WebConstants;
import org.freeeed.search.web.dao.cases.CaseDao;
import org.freeeed.search.web.model.Case;
import org.freeeed.search.web.model.ProcessingStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ProcessingController extends SecureController {

    private static final Logger log = Logger.getLogger(ProcessingController.class);

    private CaseDao caseDao;

    public void setCaseDao(CaseDao caseDao) {
        this.caseDao = caseDao;
    }

    @Override
    public ModelAndView execute() {
        String action = (String) valueStack.get("action");
        if ("process".equals(action)) {
            try {
                String caseIdStr = (String) valueStack.get("id");
                Long caseId = Long.parseLong(caseIdStr);
                Case c = caseDao.findCase(caseId);
                c.setStatus(ProcessingStatus.PROCESSING_IN_PROGRESS);
                caseDao.saveCase(c);
                response.sendRedirect(WebConstants.LIST_CASES_PAGE_REDIRECT);
            } catch (Exception e) {
                log.error("Error while processing case: " + e.getMessage());
            }
        }
        return new ModelAndView(WebConstants.LIST_CASES_PAGE);
    }
}
