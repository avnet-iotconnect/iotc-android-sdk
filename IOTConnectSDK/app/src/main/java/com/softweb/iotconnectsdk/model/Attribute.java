package com.softweb.iotconnectsdk.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Attribute {

    @SerializedName("agt")
    @Expose
    private Integer agt;
    @SerializedName("dt")
    @Expose
    private Integer dt;
    @SerializedName("dv")
    @Expose
    private String dv;
    @SerializedName("ln")
    @Expose
    private String ln;
    @SerializedName("sq")
    @Expose
    private Integer sq;
    @SerializedName("tg")
    @Expose
    private String tg;
    @SerializedName("tw")
    @Expose
    private String tw;
    @SerializedName("d")
    @Expose
    private List<D> d = null;
    @SerializedName("p")
    @Expose
    private String p;

    public Integer getAgt() {
        return agt;
    }

    public void setAgt(Integer agt) {
        this.agt = agt;
    }

    public Integer getDt() {
        return dt;
    }

    public void setDt(Integer dt) {
        this.dt = dt;
    }

    public String getDv() {
        return dv;
    }

    public void setDv(String dv) {
        this.dv = dv;
    }

    public String getLn() {
        return ln;
    }

    public void setLn(String ln) {
        this.ln = ln;
    }

    public Integer getSq() {
        return sq;
    }

    public void setSq(Integer sq) {
        this.sq = sq;
    }

    public String getTg() {
        return tg;
    }

    public void setTg(String tg) {
        this.tg = tg;
    }

    public String getTw() {
        return tw;
    }

    public void setTw(String tw) {
        this.tw = tw;
    }

    public List<D> getD() {
        return d;
    }

    public void setD(List<D> d) {
        this.d = d;
    }

    public String getP() {
        return p;
    }

    public void setP(String p) {
        this.p = p;
    }

}
