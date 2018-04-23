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
import org.elasticsearch.search.SearchHits;
import org.freeeed.search.web.configuration.Configuration;
import org.freeeed.search.web.dao.cases.CaseDao;
import org.freeeed.search.web.dao.elasticsearch.SearchDao;
import org.freeeed.search.web.model.cases.Case;
import org.freeeed.search.web.session.SearchSessionObject;
import org.freeeed.search.web.session.SessionContext;

import java.util.*;

import static org.freeeed.search.web.service.elasticsearch.ESTagService.Result.ERROR;
import static org.freeeed.search.web.service.elasticsearch.ESTagService.Result.SUCCESS;

public class ESTagService implements TagService {

    private static final Logger LOGGER = Logger.getLogger(ESTagService.class);
    public static final String TAGS_SEARCH_FIELD = "tags-search-field";
    public static final String TAGS_SEPARATOR = ";";
    public static final String DOC_ID = "id";
    public static final int MAX_ROWS_FOR_BULK_DELETE = 10000;
    private Configuration configuration;
    private CaseDao caseDao;
    private SearchDao searchDao;

    public enum Result {
        SUCCESS,
        ERROR
    }

    /**
     * Tag a single document identified by its document id.
     *
     * @param documentId
     * @param tags
     * @return
     */
    @Override
    public Result tagSingleDocument(String documentId, Set<String> tags) {
        Result result = searchDao.updateSingleDocTag(documentId, tags);
        if (result == SUCCESS) {
            updateCaseTags(tags);
        }
        return result;
    }

    /**
     * Tag all documents within the current search page.
     *
     * @param esSession
     * @param tag
     * @return
     */
    @Override
    public Result tagThisPageDocuments(SearchSessionObject esSession, String tag) {
        String indicesName = esSession.getSelectedCase().getEsSourceIndices();
        Set<String> freeTextQueries = esSession.buildFreeTextSearchQuery();
        Set<String> tagsSearchQueries = esSession.buildTagsSearchQuery();
        int from = (esSession.getCurrentPage() - 1) * configuration.getNumberOfRows();
        SearchResponse searchResponse = searchDao.search(indicesName, freeTextQueries, tagsSearchQueries, from, configuration.getNumberOfRows(), new String[]{DOC_ID, TAGS_SEARCH_FIELD});
        tagMultipleDocuments(tag, searchResponse);
        return Result.SUCCESS;
    }

    /**
     * Tag all documents within the current search.
     *
     * @param esSession
     * @param tag
     * @return
     */
    @Override
    public Result tagAllSearchedDocuments(SearchSessionObject esSession, String tag) {
        Set<String> freeTextQueries = esSession.buildFreeTextSearchQuery();
        Set<String> tagsSearchQueries = esSession.buildTagsSearchQuery();
        int rows = esSession.getTotalDocuments();
        String indicesName = esSession.getSelectedCase().getEsSourceIndices();
        SearchResponse searchResponse = searchDao.search(indicesName, freeTextQueries, tagsSearchQueries, 0, rows, new String[]{DOC_ID, TAGS_SEARCH_FIELD});
        tagMultipleDocuments(tag, searchResponse);
        return Result.SUCCESS;
    }

    /**
     * Remove tag from a single document identified by its document id.
     *
     * @param documentId
     * @param tag
     * @return
     */
    @Override
    public Result removeTagFromSingleDocument(SearchSessionObject esSession, String documentId, String tag) {
        String indicesName = esSession.getSelectedCase().getEsSourceIndices();
        SearchResponse searchResponse = searchDao.searchById(indicesName, documentId, new String[]{DOC_ID, TAGS_SEARCH_FIELD});
        SearchHit[] searchHits = searchResponse.getHits().getHits();

        if (searchHits.length > 0) {
            List<String> tagValues = (List<String>) searchHits[0].getSourceAsMap().get(TAGS_SEARCH_FIELD);
            removeAndUpdateTag(documentId, tag, tagValues);

            Set<String> tagQueries = new HashSet<>();
            tagQueries.add(tag);
            SearchResponse response = searchDao.search(indicesName, Collections.emptySet(), tagQueries, 0, 2, new String[]{DOC_ID, TAGS_SEARCH_FIELD});

            if (hasNoDocWithSameTag(documentId, response)) {
                removeCaseTag(tag);
            }
        }

        return SUCCESS;
    }

    /**
     * remove tag from all docs
     *
     * @param esSession
     * @param tag
     * @return
     */
    @Override
    public Result removeTagFromAllDocs(SearchSessionObject esSession, String tag) {
        try {
            int rows = esSession.getTotalDocuments();
            if (rows == 0) {
                rows = MAX_ROWS_FOR_BULK_DELETE;
            }
            String indicesName = esSession.getSelectedCase().getEsSourceIndices();
            Set<String> tags = new HashSet<>();
            tags.add(tag);
            SearchResponse searchResponse = searchDao.search(indicesName, Collections.emptySet(), tags, 0, rows, new String[]{DOC_ID, TAGS_SEARCH_FIELD});
            SearchHit[] hits = searchResponse.getHits().getHits();
            for (SearchHit hit : hits) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                String documentId = hit.getId();
                List<String> tagsFieldValue = (List<String>) sourceAsMap.get(TAGS_SEARCH_FIELD);
                removeAndUpdateTag(documentId, tag, tagsFieldValue);
            }
            removeCaseTag(tag);
        } catch (Exception ex) {
            LOGGER.error("Exception while removing document: " + DOC_ID, ex);
            return ERROR;

        }
        return SUCCESS;
    }

    //********** utilities **********
    private void removeAndUpdateTag(String documentId, String tag, List<String> tagValues) {
        if (Objects.nonNull(tagValues)) {
            boolean removed = tagValues.remove(tag);
            if (removed) {
                Set<String> uniqueTags = new HashSet<>(tagValues);
                searchDao.updateSingleDocTag(documentId, uniqueTags);
            }
        }
    }

    private static boolean hasNoDocWithSameTag(String recentUpdatedDocId, SearchResponse searchResponse) {
        SearchHits hits = searchResponse.getHits();
        if (hits.getTotalHits() == 1) {
            String docIdWithTag = hits.getHits()[0].getId();
            return docIdWithTag.equals(recentUpdatedDocId);
        }
        return hits.getTotalHits() == 0 ? true : false;
    }

    private void tagMultipleDocuments(String tag, SearchResponse searchResponse) {
        SearchHit[] hits = searchResponse.getHits().getHits();
        for (SearchHit hit : hits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String documentId = hit.getId();
            Object tagsFieldValue = sourceAsMap.get(TAGS_SEARCH_FIELD);
            Set<String> tags = new HashSet<>();
            if (Objects.nonNull(tagsFieldValue)) {
                tags.addAll((List<String>) tagsFieldValue);
            }
            tags.add(tag);
            searchDao.updateSingleDocTag(documentId, tags);
        }
        Set<String> tags = new HashSet<>();
        tags.add(tag);
        updateCaseTags(tags);
    }


    private void updateCaseTags(Set<String> tags) {
        SearchSessionObject esSession = SessionContext.getElasticSearchSession();
        if (esSession != null && esSession.getSelectedCase() != null) {
            Case selectedCase = esSession.getSelectedCase();
            for (String tag : tags) {
                selectedCase.addTag(tag);
            }
            caseDao.saveCase(selectedCase);
        }
    }

    private void removeCaseTag(String tag) {
        SearchSessionObject esSession = SessionContext.getElasticSearchSession();
        if (esSession != null && esSession.getSelectedCase() != null) {
            Case selectedCase = esSession.getSelectedCase();
            selectedCase.removeTag(tag);
            caseDao.saveCase(selectedCase);
        }
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setCaseDao(CaseDao caseDao) {
        this.caseDao = caseDao;
    }

    public void setSearchDao(SearchDao searchDao) {
        this.searchDao = searchDao;
    }
}
