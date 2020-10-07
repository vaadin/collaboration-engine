package com.vaadin.collaborationengine.util;

import java.time.LocalDate;

public class TestBean {

    private String value;
    private LocalDate localDate;

    public TestBean() {
    }

    public TestBean(String value) {
        this.value = value;
    }

    public TestBean(String value, LocalDate localDate) {
        this(value);
        this.localDate = localDate;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }
}
