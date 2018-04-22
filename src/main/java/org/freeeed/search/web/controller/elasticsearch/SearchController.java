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

import org.apache.log4j.Logger;
import org.freeeed.search.web.controller.commons.SecureController;
import org.freeeed.search.web.utils.WebConstants;
import org.freeeed.search.web.configuration.Configuration;
import org.freeeed.search.web.service.elasticsearch.ESSearchService;
import org.freeeed.search.web.session.SearchSessionObject;
import org.freeeed.search.web.searchviews.ResultHighlight;
import org.freeeed.search.web.searchviews.SearchResultView;
import org.freeeed.search.web.searchviews.YourSearchViewObject;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Class SearchController.
 * <p>
 * Implements the search logic.
 *
 * @author ilazarov
 */
public class SearchController extends SecureController {
    private static final Logger log = Logger.getLogger(SearchController.class);

    private Configuration configuration;
    private ESSearchService ESSearchService;
    private ResultHighlight resultHighlight;

    @Override
    public ModelAndView execute() {
        String action = (String) valueStack.get("action");
        log.debug("Search action received: " + action);

        HttpSession session = this.request.getSession(true);

        SearchSessionObject esSession = (SearchSessionObject) session.getAttribute(WebConstants.WEB_SESSION_SEARCH_OBJECT);

        if (Objects.isNull(esSession)) {
            esSession = new SearchSessionObject();
            session.setAttribute(WebConstants.WEB_SESSION_SEARCH_OBJECT, esSession);
        }

        int page = 1;
        int rows = configuration.getNumberOfRows();
        int from = 0;

        if ("search".equals(action)) {
            String searchString = (String) valueStack.get("query");
            if (Objects.nonNull(searchString) && !searchString.isEmpty()) {
                esSession.addQuery(searchString);
            }
        } else if ("tagsearch".equals(action)) {
            String tagString = (String) valueStack.get("tag");
            if (Objects.nonNull(tagString) && !tagString.isEmpty()) {
                esSession.addTagQuery(tagString);
            }

        } else if ("remove".equals(action)) {
            String id = (String) valueStack.get("id");
            try {
                int index = Integer.parseInt(id);
                esSession.removeByIndex(index);
            } catch (Exception e) {
            }

        } else if ("removeall".equals(action)) {
            esSession.removeAll();
        } else if ("changepage".equals(action)) {
            String pageStr = (String) valueStack.get("page");
            if (pageStr != null) {
                try {
                    page = Integer.parseInt(pageStr);
                    if (page < 1) {
                        page = 1;
                    }

                    if (esSession != null) {
                        if (page > esSession.getTotalPage()) {
                            page = esSession.getTotalPage();
                        }
                    }
                } catch (Exception ex) {
                }
                from = (page - 1) * configuration.getNumberOfRows();
            }
        }

        List<YourSearchViewObject> yourSearches = new ArrayList<>();

        List<String> searches = esSession.getQueries();
        for (int i = 0; i < searches.size(); i++) {
            YourSearchViewObject searchViewObject = new YourSearchViewObject();
            searchViewObject.setId(i + 1);
            searchViewObject.setName(searches.get(i));
            yourSearches.add(searchViewObject);
        }

        if (searches.size() > 0) {
            Set<String> freeTextQueries = esSession.buildFreeTextSearchQuery();
            Set<String> tagsSearchQueries = esSession.buildTagsSearchQuery();
            SearchResultView resultView = ESSearchService.search(freeTextQueries, tagsSearchQueries, from, rows, null);
            if (resultView != null) {
                //prepare the view data
                resultHighlight.highlight(resultView, yourSearches);
                valueStack.put("result", resultView);
                valueStack.put("searched", yourSearches);
                esSession.setCurrentPage(page);

                int total = resultView.getTotalSize() / configuration.getNumberOfRows();
                if (resultView.getTotalSize() % configuration.getNumberOfRows() > 0) {
                    total++;
                }

                esSession.setTotalPage(total);
                esSession.setTotalDocuments(resultView.getTotalSize());
                setupPagination();
            }
        }

        return new ModelAndView(WebConstants.SEARCH_AJAX_PAGE);
    }

    private void setupPagination() {
        SearchSessionObject session = (SearchSessionObject) this.request.getSession(true).getAttribute(WebConstants.WEB_SESSION_SEARCH_OBJECT);
        valueStack.put("showPagination", session.getTotalPage() > 1);
        valueStack.put("currentPage", session.getCurrentPage());
        valueStack.put("showPrev", session.getCurrentPage() > 1);
        valueStack.put("showNext", session.getCurrentPage() < session.getTotalPage());
        valueStack.put("searchPerformed", true);
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setESSearchService(ESSearchService ESSearchService) {
        this.ESSearchService = ESSearchService;
    }

    public void setResultHighlight(ResultHighlight resultHighlight) {
        this.resultHighlight = resultHighlight;
    }
}
