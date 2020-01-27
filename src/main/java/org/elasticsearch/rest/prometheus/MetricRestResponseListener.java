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

package org.elasticsearch.rest.prometheus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.compuscene.metrics.prometheus.PrometheusMetricsCatalog;
import org.compuscene.metrics.prometheus.PrometheusMetricsCollector;
import org.compuscene.metrics.prometheus.PrometheusSettings;
import org.elasticsearch.action.NodePrometheusMetricsResponse;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.RestResponseListener;

public class MetricRestResponseListener extends RestResponseListener<NodePrometheusMetricsResponse> {
    private final Logger logger;

    private PrometheusSettings prometheusSettings;

    MetricRestResponseListener(RestChannel channel, PrometheusSettings prometheusSettings) {
        super(channel);
        this.logger = LogManager.getLogger(getClass());
        this.prometheusSettings = prometheusSettings;
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
        PrometheusMetricsCollector collector = new PrometheusMetricsCollector(
                catalog,
                prometheusSettings.getPrometheusIndices(),
                prometheusSettings.getPrometheusClusterSettings());
        collector.registerMetrics();
        collector.updateMetrics(response.getClusterHealth(), response.getNodeStats(), response.getIndicesStats(),
                response.getClusterStatsData());
        return new BytesRestResponse(RestStatus.OK, collector.getCatalog().toTextFormat());
    }
}
