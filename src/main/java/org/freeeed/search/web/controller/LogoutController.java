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

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.freeeed.search.web.WebConstants;
import org.springframework.web.servlet.ModelAndView;

import org.freeeed.search.web.dao.settings.AppSettingsDao;
import org.freeeed.search.web.model.AppSettings;


public class LogoutController extends BaseController {
    private static final Logger log = Logger.getLogger(LogoutController.class);
    private AppSettingsDao appSettingsDao;
    
    @Override
    public ModelAndView execute() {
        AppSettings appSettings = appSettingsDao.loadSettings();
		valueStack.put("appSettingsDao", appSettingsDao);
        log.debug("Logout called!");
        
        HttpSession session = request.getSession();
        session.removeAttribute(WebConstants.LOGGED_SITE_VISITOR_SESSION_KEY);
        
        this.loggedSiteVisitor = null;
        
        return new ModelAndView(WebConstants.LOGOUT_PAGE);
    }
    
    public void setAppSettingsDao(AppSettingsDao appSettingsDao) {
        this.appSettingsDao = appSettingsDao;
    }
    
    public AppSettingsDao getAppSettingsDao() {
        return this.appSettingsDao;
    }


}
