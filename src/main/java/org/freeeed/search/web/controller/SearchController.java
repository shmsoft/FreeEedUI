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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

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

/**
 * 
 * Class SearchController.
 * 
 * Implements the search logic.
 * 
 * @author ilazarov
 *
 */
public class SearchController extends SecureController {
    private static final Logger log = Logger.getLogger(SearchController.class);
    
    private Configuration configuration;
    private SolrSearchService solrSearchService;
    private SearchViewPreparer searchViewPreparer;
    private ResultHighlight resultHighlight;
    
    @Override
    public ModelAndView execute() {
        String action = (String) valueStack.get("action");
        log.debug("Search action received: " + action);
        
        HttpSession session = this.request.getSession(true);
        
        SolrSessionObject solrSession = (SolrSessionObject) 
            session.getAttribute(WebConstants.WEB_SESSION_SOLR_OBJECT);
    
        if (solrSession == null) {
            solrSession = new SolrSessionObject();
            session.setAttribute(WebConstants.WEB_SESSION_SOLR_OBJECT, solrSession);
        }
        
        int page = 1;

        // Effective page size (issue #74): session override wins, else the
        // configured value; guarded against a stale "fetch-all" setting
        // (e.g. 99999 in an old install's a.dat) that broke pagination.
        int pageSize = solrSession.getPageSize();
        if (pageSize <= 0) {
            pageSize = configuration.getNumberOfRows();
        }
        if (pageSize <= 0 || pageSize > 500) {
            pageSize = 25;
        }
        int rows = pageSize;
        int from = 0;

        if ("search".equals(action)) {
            //setup the query
            // A fresh keyword search leaves Case View (issue #76).
            solrSession.setCaseView(false);
            String search = (String) valueStack.get("query");
            if (search != null && search.length() > 0) {
                KeywordQuerySearch qs = new KeywordQuerySearch(search, solrSearchService, from, rows);
                solrSession.addQuery(qs);
            }

        } else if ("tagsearch".equals(action)) {
            solrSession.setCaseView(false);
            String tag = (String) valueStack.get("tag");
            if (tag != null && tag.length() > 0) {
                TagQuerySearch qs = new TagQuerySearch(tag);
                solrSession.addQuery(qs);
            }

        } else if ("caseview".equals(action)) {
            // Enter Case View, centered on the given document (issue #76):
            // browse the WHOLE case in id-asc order and land on the page that
            // contains this document, so the reviewer sees the docs before and
            // after it regardless of the search filter.
            String anchorId = (String) valueStack.get("id");
            solrSession.setCaseView(true);
            solrSession.setCaseViewAnchorId(anchorId);
            page = anchorPage(anchorId, rows);
            from = (page - 1) * rows;

        } else if ("searchview".equals(action)) {
            // Leave Case View, back to the filtered search results (issue #76).
            solrSession.setCaseView(false);

        } else if ("remove".equals(action)) {
            String idStr = (String) valueStack.get("id");
            try {
                int id = Integer.parseInt(idStr);
                solrSession.removeById(id);
            } catch (Exception e) {
            }

        } else if ("removeall".equals(action)) {
            solrSession.setCaseView(false);
            solrSession.removeAll();
        } else if ("sort".equals(action)) {
            // Change the results-list sort; return to page 1 (issue #74).
            solrSession.setSortField((String) valueStack.get("sort"));
            solrSession.setSortDir((String) valueStack.get("dir"));
        } else if ("pagesize".equals(action)) {
            // Change the page size; return to page 1 (issue #74).
            String sizeStr = (String) valueStack.get("pageSize");
            try {
                int size = Integer.parseInt(sizeStr);
                if (size > 0 && size <= 500) {
                    solrSession.setPageSize(size);
                    pageSize = size;
                    rows = size;
                }
            } catch (Exception e) {
            }
        } else if ("changepage".equals(action)) {
            String pageStr = (String) valueStack.get("page");
            if (pageStr != null) {
                try {
                    page = Integer.parseInt(pageStr);
                    if (page < 1) {
                        page = 1;
                    }

                    if (solrSession != null) {
                        if (page > solrSession.getTotalPage()) {
                            page = solrSession.getTotalPage();
                        }
                    }
                    if (page < 1) {
                        page = 1;
                    }
                } catch (Exception e) {
                }

                from = (page - 1) * rows;
            }
        }
        
        List<YourSearchViewObject> yourSearches = new ArrayList<YourSearchViewObject>();
        
        List<QuerySearch> searches = solrSession.getQueries();
        for (int i = 0; i < searches.size(); i++) {
            QuerySearch querySearch = searches.get(i);
            querySearch.adjust(from, rows);
            
            YourSearchViewObject so = new YourSearchViewObject();
            so.setId(i + 1);
            so.setName(querySearch.getDisplay());
            so.setKeywords(querySearch.getSearchKeywords());
            
            yourSearches.add(so);
        }
        
        boolean caseView = solrSession.isCaseView();
        if (caseView || searches.size() > 0) {

            // Case View browses the whole case in natural order, ignoring the
            // keyword filter (issue #76). Otherwise use the built query + the
            // session's chosen sort.
            String search;
            String sortClause;
            if (caseView) {
                search = "*:*";
                sortClause = "id asc";
            } else {
                search = solrSession.buildSearchQuery();
                sortClause = solrSession.getSortField() + " " + solrSession.getSortDir();
            }
            SolrResult result = solrSearchService.search(search, from, rows, sortClause);
            //if solr returns correct result
            if (result != null) {
                //prepare the view data
                SearchResult resultView = searchViewPreparer.prepareView(result);
                resultHighlight.highlight(resultView, yourSearches);

                valueStack.put("result", resultView);
                valueStack.put("searched", yourSearches);

                solrSession.setCurrentPage(page);

                int total = result.getTotalSize() / rows;
                if (result.getTotalSize() % rows > 0) {
                    total ++;
                }

                solrSession.setTotalPage(total);
                solrSession.setTotalDocuments(result.getTotalSize());


            }
        }
        // Expose the offset + effective page size for the results counter (issue #74).
        valueStack.put("resultFrom", from);
        valueStack.put("pageSize", pageSize);
        // Case View state for the toggle + anchor-row highlight (issue #76).
        valueStack.put("caseView", solrSession.isCaseView());
        valueStack.put("caseViewAnchorId", solrSession.getCaseViewAnchorId());
        setupPagination();
        return new ModelAndView(WebConstants.SEARCH_AJAX_PAGE);
    }

    /**
     * Find which page of the full, id-asc-ordered case contains the given
     * document (issue #76). The document's 1-based rank equals the count of
     * documents whose id is lexicographically &lt;= the anchor id, which matches
     * the "id asc" sort used by Case View. Defaults to page 1 on any problem.
     */
    private int anchorPage(String anchorId, int rows) {
        if (anchorId == null || anchorId.trim().length() == 0 || rows <= 0) {
            return 1;
        }
        // Escape the range endpoint so ids with quotes/backslashes are safe.
        String endpoint = anchorId.replace("\\", "\\\\").replace("\"", "\\\"");
        int rank = solrSearchService.count("id:[* TO \"" + endpoint + "\"]");
        if (rank <= 0) {
            return 1;
        }
        return ((rank - 1) / rows) + 1;
    }

    private void setupPagination() {
        SolrSessionObject session = (SolrSessionObject)
            this.request.getSession(true).getAttribute("solrSession");

        valueStack.put("showPagination", session.getTotalPage() > 1);
        valueStack.put("currentPage",  session.getCurrentPage());
        valueStack.put("totalPage", session.getTotalPage());
        valueStack.put("showPrev", session.getCurrentPage() > 1);
        valueStack.put("showNext", session.getCurrentPage() < session.getTotalPage());
        valueStack.put("sortField", session.getSortField());
        valueStack.put("sortDir", session.getSortDir());
        valueStack.put("searchPerformed", true);
    }
    
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    public void setSearchViewPreparer(SearchViewPreparer searchViewPreparer) {
        this.searchViewPreparer = searchViewPreparer;
    }

    public void setResultHighlight(ResultHighlight resultHighlight) {
        this.resultHighlight = resultHighlight;
    }
}
