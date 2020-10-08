package com.vaadin.collaborationengine.util;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TestBean {

    private String value;
    private LocalDate localDate;
    private TestBean testBean;

    private List<TestBean> testBeans = Collections.emptyList();

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

    public void setTestBean(TestBean testBean) {
        this.testBean = testBean;
    }

    public TestBean getTestBean() {
        return testBean;
    }

    public List<TestBean> getTestBeans() {
        return testBeans;
    }

    public void setTestBeans(List<TestBean> testBeans) {
        this.testBeans = testBeans;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        TestBean other = (TestBean) obj;
        return Objects.equals(value, other.value);
    }

}
