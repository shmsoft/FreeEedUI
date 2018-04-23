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
package org.freeeed.search.web.controller.elasticsearch;

import org.freeeed.search.web.controller.commons.SecureController;
import org.freeeed.search.web.dao.cases.CaseDao;
import org.freeeed.search.web.model.cases.Case;
import org.freeeed.search.web.session.SearchSessionObject;
import org.freeeed.search.web.utils.WebConstants;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Class SearchPageController.
 * <p>
 * Serving the search main page.
 *
 * @author ilazarov.
 */
public class SearchPageController extends SecureController {
    private CaseDao caseDao;

    @Override
    public ModelAndView execute() {
        HttpSession session = this.request.getSession(true);

        SearchSessionObject esSession = (SearchSessionObject)
                session.getAttribute(WebConstants.WEB_SESSION_SEARCH_OBJECT);

        if (esSession != null) {
            esSession.reset();
        }
        if (esSession == null) {
            esSession = new SearchSessionObject();
            session.setAttribute(WebConstants.WEB_SESSION_SEARCH_OBJECT, esSession);
        }

        List<Case> cases = caseDao.listCases();
        valueStack.put("cases", cases);

        if (esSession.getSelectedCase() == null) {
            if (cases.size() > 0) {
                esSession.setSelectedCase(cases.get(0));
            }
        }

        valueStack.put("selectedCase", esSession.getSelectedCase());
        if (esSession.getSelectedCase() != null) {
            valueStack.put("tags", esSession.getSelectedCase().getTags());
        }

        String action = (String) valueStack.get("action");
        if ("changecase".equalsIgnoreCase(action)) {
            String caseIdStr = (String) valueStack.get("id");
            try {
                Case selected = caseDao.findCase(Long.parseLong(caseIdStr));
                esSession.setSelectedCase(selected);
                valueStack.put("selectedCase", esSession.getSelectedCase());
                valueStack.put("tags", esSession.getSelectedCase().getTags());
            } catch (Exception e) {
            }
        }

        return new ModelAndView(WebConstants.SEARCH_PAGE);
    }

    public void setCaseDao(CaseDao caseDao) {
        this.caseDao = caseDao;
    }
}
