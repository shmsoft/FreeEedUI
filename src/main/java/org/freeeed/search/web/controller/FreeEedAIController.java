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
import org.freeeed.search.web.solr.SolrSearchService;
import org.freeeed.search.web.view.solr.ResultHighlight;
import org.freeeed.search.web.view.solr.SearchViewPreparer;
import org.springframework.web.servlet.ModelAndView;

/**
 * 
 * Class SearchController.
 * 
 * Implements the search logic.
 * 
 * @author ilazarov
 *
 */
public class FreeEedAIController extends SecureController {
    private static final Logger log = Logger.getLogger(FreeEedAIController.class);

    @Override
    public ModelAndView execute() {
        return new ModelAndView(WebConstants.FREEEEDAI_PAGE);
    }
}
