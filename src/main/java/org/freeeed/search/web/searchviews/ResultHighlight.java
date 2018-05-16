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

import org.freeeed.search.web.model.elasticsearch.Entry;
import org.freeeed.search.web.model.elasticsearch.SearchDocument;
import org.freeeed.search.web.model.elasticsearch.Tag;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class ResultHighlight.
 *
 * @author ilazarov
 */
public class ResultHighlight {
    private static final int MAX_COLORS = 10;

    /**
     * Highlight the result for the given keywords.
     *
     * @param data
     * @param searches
     */
    public void highlight(SearchResultView data, List<YourSearchViewObject> searches) {
        int colorIndex;
        for (int i = 0; i < searches.size(); i++) {
            colorIndex = i + 1;
            if (colorIndex > MAX_COLORS) {
                colorIndex = MAX_COLORS;
            }

            YourSearchViewObject querySearch = searches.get(i);
            querySearch.setHighlight(createHightlightClass(colorIndex));

            Set<String> words = querySearch.getKeywords();

            for (String word : words) {
                List<SearchDocument> docs = data.getDocuments();
                for (SearchDocument searchDocument : docs) {
                    List<Entry> entries = searchDocument.getEntries();
                    for (Entry entry : entries) {
                        String highlighted = highlightResults(entry.getValue(), word, colorIndex);
                        entry.setValue(highlighted);
                    }

                    List<Tag> tags = searchDocument.getTags();
                    for (Tag tag : tags) {
                        String highlighted = highlightResults(tag.getValue(), word, colorIndex);
                        tag.setValue(highlighted);
                    }
                }
            }
        }
    }

    /**
     * Find the given words in the result string and add
     * special html code for special visualization.
     *
     * @param result
     * @param word
     * @return
     */
    private String highlightResults(String result, String word, int index) {
        StringBuilder stringBuilder = new StringBuilder();
        if (result == null) {
            return stringBuilder.toString();
        }
        Pattern pattern = Pattern.compile("\\b" + "(?i)" + word + "\\b");
        Matcher matcher = pattern.matcher(result);

        int currentIndex = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            stringBuilder.append(result.substring(currentIndex, start))
                    .append("<span class='" + createHightlightClass(index) + "'>")
                    .append(result.substring(start, end))
                    .append("</span>");
            currentIndex = end;
        }
        stringBuilder.append(result.substring(currentIndex, result.length()));
        return stringBuilder.toString();
    }

    private String createHightlightClass(int colorIndex) {
        return "highlight" + colorIndex;
    }
}
