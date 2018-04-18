package com.example.administrator.shbnus;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/4/18.
 */

public class BeanInfo implements Serializable {
    public BeanInfo(String name) {
        this.name = name;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
