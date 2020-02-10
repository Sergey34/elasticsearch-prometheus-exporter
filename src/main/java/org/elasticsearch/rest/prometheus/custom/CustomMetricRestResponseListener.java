/*
 * Copyright [2016] [Vincent VAN HOLLEBEKE]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.elasticsearch.rest.prometheus.custom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.compuscene.metrics.prometheus.PrometheusMetricsCatalog;
import org.elasticsearch.action.NodePrometheusMetricsResponse;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.RestResponseListener;

import java.util.stream.Stream;

public class CustomMetricRestResponseListener extends RestResponseListener<NodePrometheusMetricsResponse> {
    private final Logger logger;

    private Stream<PrometheusCustomMetrics> prometheusCustomMetrics;

    CustomMetricRestResponseListener(
            RestChannel channel,
            Stream<PrometheusCustomMetrics> prometheusCustomMetrics) {
        super(channel);
        this.prometheusCustomMetrics = prometheusCustomMetrics;
        this.logger = LogManager.getLogger(getClass());
    }

    @Override
    public RestResponse buildResponse(NodePrometheusMetricsResponse response) throws Exception {
        String clusterName = response.getClusterHealth().getClusterName();
        String nodeName = response.getNodeStats().getNode().getName();
        String nodeId = response.getNodeStats().getNode().getId();
        if (logger.isTraceEnabled()) {
            logger.trace("Prepare new Prometheus metric collector for: [{}], [{}], [{}]", clusterName, nodeId,
                    nodeName);
        }
        PrometheusMetricsCatalog catalog = new PrometheusMetricsCatalog(clusterName, nodeName, nodeId, "es_");

        prometheusCustomMetrics.forEach(it -> {
            catalog.registerClusterGauge(it.getName());
            catalog.setClusterGauge(it.getName(), it.getValue());
        });

        return new BytesRestResponse(RestStatus.OK, catalog.toTextFormat());
    }
}
