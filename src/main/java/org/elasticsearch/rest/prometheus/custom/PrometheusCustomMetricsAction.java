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

import java.util.Map;

public class PrometheusCustomMetricsAction {
    private final String name;
    private final String body;
    private final String config;
    private final boolean enabled;
    private double metric;

    public PrometheusCustomMetricsAction(Map<String, Object> config) {
        this.name = String.valueOf(config.get("name"));
        this.body = config.get("body").toString();
        this.config = config.get("transformer_config").toString();
        this.enabled = (boolean) config.get("enabled") && this.body != null && this.config != null;
    }

    public String getBody() {
        return body;
    }

    public String getConfig() {
        return config;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getName() {
        return name;
    }

    public double getMetric() {
        return metric;
    }

    public void setMetric(double metric) {
        this.metric = metric;
    }
}
