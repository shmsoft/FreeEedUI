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
package org.freeeed.search.web.controller.settings;

import org.apache.log4j.Logger;
import org.freeeed.search.web.controller.commons.SecureController;
import org.freeeed.search.web.utils.WebConstants;
import org.freeeed.search.web.dao.settings.AppSettingsDao;
import org.freeeed.search.web.model.settings.AppSettings;
import org.freeeed.search.web.model.users.User;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Class AppSettingsController.
 *
 * @author ilazarov
 */
public class AppSettingsController extends SecureController {
    private static final Logger log = Logger.getLogger(AppSettingsController.class);
    private AppSettingsDao appSettingsDao;

    @Override
    public ModelAndView execute() {
        if (!loggedSiteVisitor.getUser().hasRight(User.Right.APP_CONFIG)) {
            try {
                response.sendRedirect(WebConstants.MAIN_PAGE_REDIRECT);
                return new ModelAndView(WebConstants.APP_SETTINGS_PAGE);
            } catch (IOException e) {
            }
        }

        AppSettings appSettings = appSettingsDao.loadSettings();
        if (appSettings == null) {
            appSettings = new AppSettings();
        }

        valueStack.put("appSettings", appSettings);

        String action = (String) valueStack.get("action");

        log.debug("Action called: " + action);

        if ("save".equals(action)) {
            List<String> errors = new ArrayList<>();

            String resultsPerPageStr = (String) valueStack.get("results_per_page");
            int resultsPerPage = 0;
            try {
                resultsPerPage = Integer.parseInt(resultsPerPageStr);
            } catch (Exception e) {
                errors.add("Invalid results per page");
            }

            String searchEndpoint = (String) valueStack.get("es_endpoint");
            if (searchEndpoint == null || searchEndpoint.length() == 0) {
                errors.add("Invalid es endpoint");
            }

            appSettings.setResultsPerPage(resultsPerPage);
            appSettings.setEsEndpoint(searchEndpoint);

            List<String> permanentTags = getPermanentTags(valueStack);
            appSettings.setPermanentTags(permanentTags);
            valueStack.put("errors", errors);
            if (errors.size() == 0) {
                appSettingsDao.storeSettings(appSettings);
                valueStack.put("success", true);
            }
        }

        return new ModelAndView(WebConstants.APP_SETTINGS_PAGE);
    }

    private List<String> getPermanentTags(Map<String, Object> valueStack) {
        List<String> tags = new ArrayList<>();
        for (int i = 1; i < 100; i++) {
            Object value = valueStack.get("tag" + i);
            if (Objects.nonNull(value)) {
                tags.add((String) value);
            }
        }
        return tags;
    }

    public void setAppSettingsDao(AppSettingsDao appSettingsDao) {
        this.appSettingsDao = appSettingsDao;
    }
}
