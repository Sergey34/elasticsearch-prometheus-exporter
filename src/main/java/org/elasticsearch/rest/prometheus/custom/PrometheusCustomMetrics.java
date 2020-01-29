package org.elasticsearch.rest.prometheus.custom;

public class PrometheusCustomMetrics {
    private String name;
    private double value;

    public PrometheusCustomMetrics(String name, double value) {
        this.name = name;

        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
