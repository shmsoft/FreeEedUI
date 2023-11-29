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
import org.freeeed.search.web.model.solr.SolrResult;
import org.freeeed.search.web.session.SolrSessionObject;
import org.freeeed.search.web.solr.KeywordQuerySearch;
import org.freeeed.search.web.solr.QuerySearch;
import org.freeeed.search.web.solr.SolrSearchService;
import org.freeeed.search.web.solr.TagQuerySearch;
import org.freeeed.search.web.view.solr.ResultHighlight;
import org.freeeed.search.web.view.solr.SearchResult;
import org.freeeed.search.web.view.solr.SearchViewPreparer;
import org.freeeed.search.web.view.solr.YourSearchViewObject;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Class SearchController.
 * 
 * Implements the search logic.
 * 
 * @author ilazarov
 *
 */
public class FreeEdAIController extends SecureController {
    private static final Logger log = Logger.getLogger(FreeEdAIController.class);
    
    private Configuration configuration;
    private SolrSearchService solrSearchService;
    private SearchViewPreparer searchViewPreparer;
    private ResultHighlight resultHighlight;
    
    @Override
    public ModelAndView execute() {
        String action = (String) valueStack.get("action");
        log.debug("Search action received: " + action);
        return new ModelAndView(WebConstants.FREEEEDAI_PAGE);
    }
}
