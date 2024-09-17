/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
