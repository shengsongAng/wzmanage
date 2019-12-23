package com.jysoft.entity;

import java.util.Date;

public class WzMonthReport {
    private String id;
    private String wzName;
    private int wzStock;
    private String unit;
    private Date createDate;
    private String createBy;
    private String month;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWzName() {
        return wzName;
    }

    public void setWzName(String wzName) {
        this.wzName = wzName;
    }

    public int getWzStock() {
        return wzStock;
    }

    public void setWzStock(int wzStock) {
        this.wzStock = wzStock;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }
}
