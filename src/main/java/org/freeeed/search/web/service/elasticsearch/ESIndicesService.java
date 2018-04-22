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
import org.freeeed.search.web.configuration.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Class ESIndicesService.
 *
 * @author nehaojha
 */
public class ESIndicesService {

    private static final Logger LOGGER = Logger.getLogger(ESIndicesService.class);
    private Configuration configuration;

    /**
     * Return all indices names available in Elastic Search.
     *
     * @return
     */
    public List<String> getESIndices() {
        return listESIndices();
    }

    private List<String> listESIndices() {
        //TODO list all indices
        return Arrays.asList("shmcloud_1");
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
