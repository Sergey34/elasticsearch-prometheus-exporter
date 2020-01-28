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

package org.elasticsearch.rest.prometheus.custometrics;

import org.compuscene.metrics.prometheus.PrometheusSettings;
import org.elasticsearch.action.NodePrometheusMetricsRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContentParser;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.prometheus.metrics.MetricRestResponseListener;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.elasticsearch.action.NodePrometheusMetricsAction.INSTANCE;
import static org.elasticsearch.rest.RestRequest.Method.GET;

/**
 * REST action class for Prometheus Exporter plugin.
 */
public class RestPrometheusCustomMetricsAction extends BaseRestHandler {

    private final PrometheusSettings prometheusSettings;

    @Inject
    public RestPrometheusCustomMetricsAction(
            Settings settings,
            ClusterSettings clusterSettings,
            RestController controller) {
        super(settings);
        this.prometheusSettings = new PrometheusSettings(settings, clusterSettings);
        controller.registerHandler(GET, "/_prometheus/custommetrics", this);
    }

    @Override
    public String getName() {
        return "prometheus_metrics_action";
    }

    // This method does not throw any IOException because there are no request parameters to be parsed
    // and processed. This may change in the future.
    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format(Locale.ENGLISH, "Received request for Prometheus metrics from %s",
                    request.getRemoteAddress().toString()));
        }

        NodePrometheusMetricsRequest metricsRequest = new NodePrometheusMetricsRequest();
        return channel
                -> client.execute(INSTANCE, metricsRequest, new CustomMetricRestResponseListener(channel, prometheusSettings));
    }
}
