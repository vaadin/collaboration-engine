package com.vaadin.collaborationengine.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ReflectionUtils {

    public static List<String> getMethodNames(Class clazz) {
        return Arrays.stream(clazz.getMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())
                        || Modifier.isProtected(method.getModifiers()))
                .map(Method::getName).collect(Collectors.toList());
    }

    public static List<String> getDeclaredMethodNames(Class clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())
                        || Modifier.isProtected(method.getModifiers()))
                .map(Method::getName).collect(Collectors.toList());
    }

}
