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
package org.freeeed.search.web.searchviews;

import java.util.List;

import org.freeeed.search.web.model.elasticsearch.SearchDocument;

public class SearchResultView {
    private int totalSize;
    private List<SearchDocument> documents;
    
    public int getTotalSize() {
        return totalSize;
    }
    
    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }
    
    public List<SearchDocument> getDocuments() {
        return documents;
    }
    
    public void setDocuments(List<SearchDocument> documents) {
        this.documents = documents;
    }
}
