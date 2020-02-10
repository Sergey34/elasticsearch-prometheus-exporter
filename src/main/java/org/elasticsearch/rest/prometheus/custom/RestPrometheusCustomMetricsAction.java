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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import org.apache.http.HttpHost;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.elasticsearch.action.NodePrometheusMetricsAction.INSTANCE;
import static org.elasticsearch.rest.RestRequest.Method.GET;

/**
 * REST action class for Prometheus Exporter plugin.
 */
@SuppressWarnings("unchecked")
public class RestPrometheusCustomMetricsAction extends BaseRestHandler {

    private final RestClient restClient;

    @Inject
    public RestPrometheusCustomMetricsAction(Settings settings, RestController controller) {
        super(settings);

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


        Stream<PrometheusCustomMetrics> prometheusCustomMetrics
                = Arrays.stream(client.prepareSearch("prometheus_custom_metrics_actions")
                .setSize(1000)
                .get()
                .getHits().getHits())
                .map(SearchHit::getSourceAsMap)
                .map(PrometheusCustomMetricsAction::new)
                .filter(PrometheusCustomMetricsAction::isEnabled)
                .map(executeMetricRequest())
                .flatMap(it -> convertToPrometheusMetrics(it).stream());

        NodePrometheusMetricsRequest metricsRequest = new NodePrometheusMetricsRequest();
        return channel
                -> client.execute(INSTANCE, metricsRequest, new CustomMetricRestResponseListener(
                channel, prometheusCustomMetrics));
    }

    private List<PrometheusCustomMetrics> convertToPrometheusMetrics(PrometheusCustomMetricsAction it) {
        try {
            Map<String, Object> map = new ObjectMapper().readValue(it.getJsonResponse(), Map.class);
            Map<String, Object> aggregations = (Map<String, Object>) map.get("aggregations");
            return convertToPrometheusMetrics(aggregations, it.getName());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }


    private List<PrometheusCustomMetrics> convertToPrometheusMetrics(Map<String, Object> resp, String name) throws IOException {
        List<PrometheusCustomMetrics> prometheusCustomMetrics = new ArrayList<>();
        Properties properties = new JavaPropsMapper().writeValueAsProperties(resp);
        properties.forEach((key, value) -> {
            if (value.toString().matches("-?[0-9]+.?[0-9]*")) {
                double v = Double.parseDouble(value.toString());
                prometheusCustomMetrics.add(new PrometheusCustomMetrics(name + "." + key, v));
            }
        });
        return prometheusCustomMetrics;
    }

    private Function<PrometheusCustomMetricsAction, PrometheusCustomMetricsAction> executeMetricRequest() {
        return it -> {
            Request metricRequest = new Request("POST", it.getEndPoint());
            metricRequest.setJsonEntity(it.getBody());
            try {
                Response response = restClient.performRequest(metricRequest);
                InputStream content = response.getEntity().getContent();
                InputStreamReader in = new InputStreamReader(content, Charset.defaultCharset());
                BufferedReader reader = new BufferedReader(in);
                StringBuilder sb = new StringBuilder();
                String str;
                while ((str = reader.readLine()) != null) {
                    sb.append(str);
                }
                it.setJsonResponse(sb.toString());
            } catch (IOException e) {
                logger.warn("err ", e);
            }
            return it;
        };
    }
}
