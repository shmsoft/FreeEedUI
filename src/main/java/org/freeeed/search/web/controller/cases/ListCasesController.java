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
package org.freeeed.search.web.controller.cases;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.freeeed.search.web.controller.commons.SecureController;
import org.freeeed.search.web.utils.WebConstants;
import org.freeeed.search.web.dao.cases.CaseDao;
import org.freeeed.search.web.model.cases.Case;
import org.freeeed.search.web.model.users.User;
import org.springframework.web.servlet.ModelAndView;

/**
 * 
 * Class ListCasesController.
 * 
 * @author ilazarov.
 *
 */
public class ListCasesController extends SecureController {
    private static final Logger log = Logger.getLogger(ListCasesController.class);
    private CaseDao caseDao;

    @Override
    public ModelAndView execute() {
        log.debug("List cases called...");
        
        if (!loggedSiteVisitor.getUser().hasRight(User.Right.CASES)) {
            try {
                response.sendRedirect(WebConstants.MAIN_PAGE_REDIRECT);
            } catch (IOException e) {
            }
        }
        
        List<Case> cases = caseDao.listCases();
        Collections.sort(cases, (o1, o2) -> o1.getName().compareTo(o2.getName()));
        
        valueStack.put("cases", cases);
        
        return new ModelAndView(WebConstants.LIST_CASES_PAGE);
    }

    public void setCaseDao(CaseDao caseDao) {
        this.caseDao = caseDao;
    }
}
