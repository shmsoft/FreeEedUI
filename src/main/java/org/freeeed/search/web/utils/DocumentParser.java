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
package org.freeeed.search.web.utils;

import org.freeeed.search.web.model.elasticsearch.Entry;
import org.freeeed.search.web.model.elasticsearch.SearchDocument;
import org.freeeed.search.web.model.elasticsearch.Tag;
import org.freeeed.search.web.searchviews.SearchResultEntryComparator;
import org.freeeed.search.web.service.elasticsearch.ESTagService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Class DocumentParser.
 *
 * @author nehaojha
 */
public class DocumentParser {

    private SearchResultEntryComparator entriesComparator = new SearchResultEntryComparator();
    private static final String EMPTY = "";

    /**
     * Create a document object based
     * on the provided data. The data is data fields in key -> value format.
     *
     * @param data
     * @return SearchDocument
     */
    public SearchDocument createESDocument(Map<String, List<String>> data) {

        SearchDocument doc = new SearchDocument();
        String documentId = EMPTY;
        String from = EMPTY;
        String subject = EMPTY;
        String date = EMPTY;
        String docPath = EMPTY;
        String uniqueId = EMPTY;
        List<Entry> entries = new ArrayList<>();
        List<Tag> tags = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : data.entrySet()) {
            String name = entry.getKey();
            List<String> allValues = entry.getValue();
            String value = allValues.size() > 0 ? allValues.get(0) : null;

            Entry keyValueNode = new Entry();
            keyValueNode.setKey(name);
            keyValueNode.setValue(value);

            if (!name.equalsIgnoreCase(ESTagService.TAGS_SEARCH_FIELD)) {
                entries.add(keyValueNode);
            }

            switch (name.toLowerCase()) {
                case "upi":
                case "number":
                    uniqueId = value;
                    documentId = value;
                    break;
                case "creator":
                case "message-from":
                case "last-author":
                case "author":
                    from = value;
                    break;
                case "document_original_path":
                    docPath = value;
                    if (Objects.isNull(subject) || subject.isEmpty()) {
                        subject = value;
                    }
                    break;
                case "subject":
                case "hash":
                    subject = value;
                    break;
                case "date":
                case "creation-date":
                case "readabletimestamp":
                    date = setDate(value);
                    break;
                case ESTagService.TAGS_SEARCH_FIELD:
                    updateTags(tags, allValues);
                    break;
            }
        }

        doc.setDocumentId(documentId);
        doc.setFrom(from);
        doc.setSubject(subject);
        doc.setDate(date);
        doc.setTags(tags);
        doc.setDocumentPath(docPath);
        doc.setUniqueId(uniqueId);
        entries.sort(entriesComparator);
        doc.setEntries(entries);
        return doc;
    }

    private void updateTags(List<Tag> tags, List<String> allValues) {
        for (String tagStr : allValues) {
            Tag tag = new Tag();
            tag.setValue(tagStr);
            tag.setName(tagStr);
            tag.setId(tags.size() + 1);
            tags.add(tag);
        }
    }

    private String setDate(String value) {
        String date;
        int timeIndex = value.indexOf("T");
        if (timeIndex != -1) {
            date = value.substring(0, timeIndex);
        } else {
            date = value;
        }
        return date;
    }
}
