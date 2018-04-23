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
package org.freeeed.search.web.service.elasticsearch;

import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.freeeed.search.web.dao.elasticsearch.SearchDao;
import org.freeeed.search.web.model.elasticsearch.SearchDocument;
import org.freeeed.search.web.session.SearchSessionObject;
import org.freeeed.search.web.utils.DocumentParser;
import org.freeeed.search.web.utils.WebConstants;
import org.freeeed.search.web.searchviews.SearchResultView;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * Class ESSearchService.
 *
 * @author ilazarov.
 */
public class ESSearchService implements SearchService {

    private static final Logger log = Logger.getLogger(ESSearchService.class);
    private DocumentParser documentParser;
    private SearchDao searchDao;

    /**
     * @param freeTextQueries
     * @param from
     * @param rows
     * @param fields
     * @return
     */
    @Override
    public SearchResultView search(Set<String> freeTextQueries, Set<String> tagQueries, int from, int rows, String[] fields) {
        log.debug("Queries: " + freeTextQueries);
        HttpServletRequest curRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        HttpSession session = curRequest.getSession();
        SearchSessionObject esSession = (SearchSessionObject) session.getAttribute(WebConstants.WEB_SESSION_SEARCH_OBJECT);
        if (Objects.isNull(esSession) || Objects.isNull(esSession.getSelectedCase())) {
            return null;
        }

        String esIndices = esSession.getSelectedCase().getEsSourceIndices();
        SearchResponse searchResponse = searchDao.search(esIndices, freeTextQueries, tagQueries, from, rows, fields);
        if (Objects.nonNull(searchResponse)) {
            return createSearchResult(searchResponse);
        }
        return null;
    }


    private SearchResultView createSearchResult(SearchResponse searchResponse) {
        SearchResultView result = new SearchResultView();
        result.setTotalSize((int) searchResponse.getHits().getTotalHits());

        List<SearchDocument> searchDocuments = new ArrayList<>();
        SearchHit[] hits = searchResponse.getHits().getHits();
        for (SearchHit hit : hits) {
            Map<String, List<String>> data = new HashMap<>();
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();

            sourceAsMap.forEach((k, v) -> {
                List<String> valueList = new ArrayList();
                if (v != null) {
                    if (v instanceof List) {
                        List<String> vals = (List<String>) v;
                        valueList.addAll(vals);
                    } else {
                        valueList.add((String) v);
                    }
                }
                data.put(k, valueList);
            });

            SearchDocument doc = documentParser.createESDocument(data);
            searchDocuments.add(doc);
        }

        result.setDocuments(searchDocuments);
        return result;
    }

    public void setDocumentParser(DocumentParser documentParser) {
        this.documentParser = documentParser;
    }

    public void setSearchDao(SearchDao searchDao) {
        this.searchDao = searchDao;
    }
}
