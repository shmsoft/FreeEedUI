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

import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.freeeed.search.web.configuration.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Class ESIndicesService.
 *
 * @author nehaojha
 */
public class ESIndicesService implements IndicesService {

    private static final Logger LOGGER = Logger.getLogger(ESIndicesService.class);
    private Configuration configuration;

    /**
     * Return all indices names available in Elastic Search.
     *
     * @return
     */
    @Override
    public List<String> getESIndices() {
        return listESIndices();
    }

    private List<String> listESIndices() {
        String url = configuration.getESEndpoint();
        List<String> indices = new ArrayList<>();
        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(HttpHost.create(url)))) {
            Response response = client.getLowLevelClient().performRequest("GET", "_cat/indices?v&h=index");
            String indicesString = EntityUtils.toString(response.getEntity());
            String[] allIndices = indicesString.split("\n");
            for (int i = 1; i < allIndices.length; i++) {
                indices.add(allIndices[i]);
            }
        } catch (Exception ex) {
            LOGGER.error("exception while listing indices " + ex);
        }
        return indices;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
