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
package org.freeeed.search.web.controller.commons;

import org.freeeed.search.web.dao.cases.CaseDao;
import org.freeeed.search.web.dao.cases.FSCaseDao;
import org.freeeed.search.web.model.cases.Case;
import org.freeeed.search.web.session.LoggedSiteVisitor;
import org.freeeed.search.web.session.LoggedSiteVisitorAware;
import org.freeeed.search.web.session.SiteVisitor;
import org.freeeed.search.web.session.SiteVisitorAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class BaseController.
 * <p>
 * Base implementation for all controllers.
 * Add additional features as visitor details, value stack for presentations, etc.
 *
 * @author ilazarov
 */
public abstract class BaseController implements Controller, SiteVisitorAware, LoggedSiteVisitorAware {
    protected SiteVisitor siteVisitor;
    protected LoggedSiteVisitor loggedSiteVisitor;
    protected Map<String, Object> valueStack;
    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected CaseDao caseDao;

    @Override
    public void setSiteVisitor(SiteVisitor visitor) {
        this.siteVisitor = visitor;
    }

    public void setLoggedVisitor(LoggedSiteVisitor loggedSiteVisitor) {
        this.loggedSiteVisitor = loggedSiteVisitor;
    }

    public void setCaseDao(CaseDao caseDao) {
        this.caseDao = caseDao;
        System.out.println("Hello There");
    }

    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) throws ServletException, IOException {

        this.request = request;
        this.response = response;

        valueStack = new HashMap<>();
        Enumeration<String> en = request.getParameterNames();
        while (en.hasMoreElements()) {
            String param = en.nextElement();
            valueStack.put(param, request.getParameter(param));
        }
        ModelAndView modelAndView = new ModelAndView();
        try {
            modelAndView = execute();
        } catch (Exception ex) {
            ex.printStackTrace();
        }



        if (caseDao != null) {


            List<Case> cases = caseDao.listCases();
            valueStack.put("cases", cases);




        }

        valueStack.put("visitor", siteVisitor);
        valueStack.put("loggedVisitor", loggedSiteVisitor);

        modelAndView.addAllObjects(valueStack);

        return modelAndView;
    }

    public abstract ModelAndView execute();
}
