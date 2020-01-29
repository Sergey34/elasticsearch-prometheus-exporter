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

import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.compuscene.metrics.prometheus.PrometheusSettings;
import org.elasticsearch.action.NodePrometheusMetricsRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import static org.elasticsearch.action.NodePrometheusMetricsAction.INSTANCE;
import static org.elasticsearch.rest.RestRequest.Method.GET;

/**
 * REST action class for Prometheus Exporter plugin.
 */
public class RestPrometheusCustomMetricsAction extends BaseRestHandler {

    private final PrometheusSettings prometheusSettings;
    private final RestClient restClient;

    @Inject
    public RestPrometheusCustomMetricsAction(Settings settings, ClusterSettings clusterSettings, RestController controller) {
        super(settings);
        this.prometheusSettings = new PrometheusSettings(settings, clusterSettings);
        controller.registerHandler(GET, "/_prometheus/custommetrics", this);
        int port = settings.getAsInt("http.port", 9200);
        String host = settings.get("network.host", "0.0.0.0");
        restClient = RestClient.builder(new HttpHost(host, port, "http")).build();
    }

    @Override
    public String getName() {
        return "prometheus_custom_metrics_action";
    }

    // This method does not throw any IOException because there are no request parameters to be parsed
    // and processed. This may change in the future.
    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format(Locale.ENGLISH, "Received request for Prometheus metrics from %s",
                    request.getRemoteAddress().toString()));
        }

        Arrays.stream(client.prepareSearch("prometheus_custom_metrics_actions")
                .setSize(1000)
                .get()
                .getHits().getHits())
                .map(SearchHit::getSourceAsMap)
                .map(PrometheusCustomMetricsAction::new)
                .filter(PrometheusCustomMetricsAction::isEnabled)
                .forEach(it -> {
                    
                });

        Request request2 = new Request("POST", "/seko/_search");
        request2.setJsonEntity("{\"aggs\":{\"2\":{\"terms\":{\"field\":\"qwe\",\"size\":10,\"order\":{\"_count\":\"desc\"}}}},\"size\":0}");
        try {
            Response response = restClient.performRequest(request2);
            StatusLine statusLine = response.getStatusLine();
            logger.warn("12312 {}", statusLine);
        } catch (IOException e) {
            logger.warn("err ", e);
        }

        NodePrometheusMetricsRequest metricsRequest = new NodePrometheusMetricsRequest();
        return channel
                -> client.execute(INSTANCE, metricsRequest, new CustomMetricRestResponseListener(channel, prometheusSettings));
    }
}
