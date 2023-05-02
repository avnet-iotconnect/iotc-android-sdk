package com.iotconnectsdk.beans;

public class TumblingWindowBean {
    private String attributeName;
    private double min;
    private double max;
    private double sum; // sum attribute value "temp= 10"
    private double avg; // sum/count
    private int count; // number of time client sends data
    private double lv; //last value
    private boolean isMinSet = false;
    private boolean isMaxSet = false;

    private String uniqueId;


    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getSum() {
        return sum;
    }

    public void setSum(double sum) {
        this.sum = sum;
    }

    public double getAvg() {
        return avg;
    }

    public void setAvg(double avg) {
        this.avg = avg;
    }

    public double getLv() {
        return lv;
    }

    public void setLv(double lv) {
        this.lv = lv;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isMinSet() {
        return isMinSet;
    }

    public void setMinSet(boolean minSet) {
        isMinSet = minSet;
    }

    public boolean isMaxSet() {
        return isMaxSet;
    }

    public void setMaxSet(boolean maxSet) {
        isMaxSet = maxSet;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
}
