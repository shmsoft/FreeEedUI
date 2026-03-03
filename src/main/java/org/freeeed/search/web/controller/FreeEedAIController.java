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

import org.apache.log4j.Logger;
import org.freeeed.search.web.WebConstants;
import org.freeeed.search.web.configuration.Configuration;
import org.freeeed.search.web.dao.cases.CaseDao;
import org.freeeed.search.web.model.Case;
import org.freeeed.search.web.session.SolrSessionObject;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.util.List;


public class FreeEedAIController extends SecureController {
    private static final Logger log = Logger.getLogger(FreeEedAIController.class);
    private Configuration configuration;
    private CaseDao caseDao;

    @Override
    public ModelAndView execute() {

        HttpSession session = this.request.getSession(true);

        SolrSessionObject solrSession = (SolrSessionObject)
                session.getAttribute(WebConstants.WEB_SESSION_SOLR_OBJECT);

        if (solrSession != null) {
            solrSession.reset();
        } else {
            solrSession = new SolrSessionObject();
            session.setAttribute(WebConstants.WEB_SESSION_SOLR_OBJECT, solrSession);
        }

        List<Case> cases = caseDao.listCases();
        valueStack.put("cases", cases);

        // Optional action: changecase (posted from AI Advisor page)
        String action = (String) valueStack.get("action");
        if ("changecase".equalsIgnoreCase(action)) {
            String caseIdStr = (String) valueStack.get("id");
            try {
                Case selected = caseDao.findCase(Long.parseLong(caseIdStr));
                solrSession.setSelectedCase(selected);
                log.info("AI Advisor changed selected case: id=" + selected.getId() + ", projectId=" + selected.getProjectId() + ", name=" + selected.getName());
            } catch (Exception e) {
                log.warn("AI Advisor failed to change case for id=" + caseIdStr, e);
            }
        }

        if (solrSession.getSelectedCase() == null) {
            if (cases.size() > 0) {
                solrSession.setSelectedCase(cases.get(0));
            }
        }

        valueStack.put("selectedCase", solrSession.getSelectedCase());

        String aiApiKey = configuration.getApiKey();
        String aiApiUrl = configuration.getApiUrl();

        valueStack.put("aiApiKey", aiApiKey);
        valueStack.put("aiApiUrl", aiApiUrl);

        if (solrSession.getSelectedCase() != null) {
            Case sc = solrSession.getSelectedCase();
            log.info("AI Advisor render: selectedCase id=" + sc.getId() + ", projectId=" + sc.getProjectId() + ", name=" + sc.getName()
                    + ", aiApiUrl=" + aiApiUrl + ", apiKeyPresent=" + (aiApiKey != null && !aiApiKey.isEmpty()));
        }

        String uri = this.request.getRequestURI() != null ? this.request.getRequestURI() : "";
        String view = uri.contains("piireport") ? WebConstants.PII_REPORT_PAGE
                    : uri.contains("casesummary") ? WebConstants.CASE_SUMMARY_PAGE
                    : WebConstants.FREEEEDAI_PAGE;
        return new ModelAndView(view);
    }
    public void setCaseDao(CaseDao caseDao) {
        this.caseDao = caseDao;
    }
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

}
