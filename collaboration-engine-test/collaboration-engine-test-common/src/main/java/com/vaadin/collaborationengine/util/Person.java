package com.vaadin.collaborationengine.util;

public class Person {

    public enum Diet {
        NONE, GLUTEN_FREE, VEGAN
    }

    private String name;
    private boolean married;
    private Diet diet = Diet.NONE;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isMarried() {
        return married;
    }

    public void setMarried(boolean married) {
        this.married = married;
    }

    public Diet getDiet() {
        return diet;
    }

    public void setDiet(Diet diet) {
        this.diet = diet;
    }
}
