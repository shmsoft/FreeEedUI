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
package org.freeeed.search.web.solr;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.params.CoreAdminParams;
import org.freeeed.search.web.configuration.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Class SolrCoreService.
 *
 * @author nehaojha
 */
public class SolrCoreService {

    private static final Logger LOGGER = Logger.getLogger(SolrCoreService.class);
    private Configuration configuration;

    /**
     * Return all core names available in Solr.
     *
     * @return
     */
    public List<String> getSolrCores() {
        return listSolrCores();
    }

    private List<String> listSolrCores() {
        String SOLR_URL = configuration.getSolrEndpoint() + "/solr";
        HttpSolrClient solrClient = new HttpSolrClient.Builder(SOLR_URL).build();

        CoreAdminRequest request = new CoreAdminRequest();
        request.setAction(CoreAdminParams.CoreAdminAction.STATUS);

        try {
            CoreAdminResponse cores = request.process(solrClient);
            List<String> coreList = new ArrayList<>();
            for (int i = 0; i < cores.getCoreStatus().size(); i++) {
                coreList.add(cores.getCoreStatus().getName(i));
            }
            return coreList;
        } catch (Exception ex) {
            LOGGER.error("Exception listing solr core ", ex);
        }
        return null;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
