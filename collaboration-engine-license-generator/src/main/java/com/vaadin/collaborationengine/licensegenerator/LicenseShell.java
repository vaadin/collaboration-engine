/*
 * Copyright (C) 2021 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine.licensegenerator;

import java.time.LocalDate;

/**
 * The shell-component which provides the methods invokable from the CLI.
 */
public class LicenseShell {

    static final LicenseGenerator GENERATOR = new LicenseGenerator();

    /**
     * The main method of the shell application.
     *
     * @param args
     *            the application arguments
     */
    public static void main(String... args) {
        if (args.length < 3) {
            printUsage();
            System.exit(-1);
        }
        String owner = args[0];
        int quota = Integer.parseInt(args[1]);
        LocalDate endDate = LocalDate.parse(args[2]);

        String license = GENERATOR.generateLicense(owner, quota, endDate);
        System.out.println(license);
    }

    private static void printUsage() {
        System.out.println(
                "Usage: java -jar <jarfile> 'Owner name' 2000 2022-12-31");
    }
}
