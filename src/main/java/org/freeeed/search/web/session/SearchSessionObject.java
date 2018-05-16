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
package org.freeeed.search.web.session;

import org.freeeed.search.web.model.cases.Case;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class SearchSessionObject.
 * <p>
 * Keeps related search data in the web session.
 *
 * @author ilazarov
 */
public class SearchSessionObject {
    private int currentPage;
    private int totalPage;
    private int totalDocuments;
    private List<String> queries = new ArrayList<>();
    private Case selectedCase;
    private List<Boolean> queryTags = new ArrayList<>();

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public int getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(int totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public synchronized void addQuery(String query) {
        if (!query.isEmpty() && !queries.contains(query)) {
            queryTags.add(false);
            queries.add(query);
        }
    }

    public synchronized void removeByIndex(int index) {
        if (index >= 0 && index < queries.size()) {
            queries.remove(index);
            queryTags.remove(index);
        }
    }

    public synchronized void removeAll() {
        queries.clear();
    }

    public synchronized List<String> getQueries() {
        return queries;
    }


    public Set<String> buildFreeTextSearchQuery() {
        Set<String> freeTextQueries = new HashSet<>();
        for (int i = 0, j = 0; i < queries.size() && j < queryTags.size(); i++, j++) {
            if (!queryTags.get(j) && !queries.get(i).equals("*")) {
                String queryBuilder = queries.get(i);
                freeTextQueries.add(queryBuilder);
            }
        }
        return freeTextQueries;
    }

    public Set<String> buildTagsSearchQuery() {
        Set<String> tagQueries = new HashSet<>();
        for (int i = 0, j = 0; i < queries.size() && j < queryTags.size(); i++, j++) {
            if (queryTags.get(j)) {
                String queryBuilder = queries.get(i) + ",";
                tagQueries.add(queryBuilder);
            }
        }
        return tagQueries;
    }

    public void reset() {
        queries.clear();
        currentPage = 1;
        queryTags.clear();
    }

    public Case getSelectedCase() {
        return selectedCase;
    }

    public void setSelectedCase(Case selectedCase) {
        this.selectedCase = selectedCase;
    }

    public void addTagQuery(String tagString) {
        queryTags.add(true);
        queries.add(tagString);
    }
}
