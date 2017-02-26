package nl.ou.dpd;

import nl.ou.dpd.data.argoxmi.ArgoXMIParser;
import nl.ou.dpd.data.template.TemplatesParser;
import nl.ou.dpd.domain.DesignPattern;
import nl.ou.dpd.domain.SystemUnderConsideration;

import java.util.List;

/**
 * The main class of the Design Pattern Detector application.
 *
 * @author E.M. van Doorn
 * @author Martin de Boer
 */
public final class DesignPatternDetector {

    private static final String USAGE_TXT =
            "\nUsage: \n\tjava -t templateFile -x xmiFile -n maxNumberOfMissingEdges." +
            "\n\tDefault values for templateFile and xmiFile are templates.xml, input.xmi and 0";


    private String templateFileName, xmiFileName;
    private int maxMissingEdges;

    /**
     * Constructor without arguments. This constructor is package scoped for testing purposes.
     */
    DesignPatternDetector() {
        templateFileName = "templates.xml";
        xmiFileName = "input.xmi";
        maxMissingEdges = 0;
    }

    /**
     * The main method of the application. Reads the command line arguments and starts the {@link DesignPatternDetector}
     * application. Usage:
     * <pre>
     *     java -jar DesignPatternDetector [-t] [<template-file>] [-x] [<xmi-file>] [-n] [<max-missing-edges>]
     * </pre>
     * When no arguments are provided, the application will assume defaults, respectively: "templates.xml", "input.xmi",
     * and 0. If the specified (or defaulted) files do not exist, an error will occur.
     *
     * @param args the command line arguments. The arguments are optional, but come in pairs (name/value). If a name
     *             argument (flag) is provided, then a corresponding value argument is expected as well. When a name
     *             argument (flag) "-t" is provided, the following argument is presumed to be the name of the template
     *             file containing the design patterns specification. When a name argument "-x" is provided, the
     *             following argument is presumed to be the xmi-file representing the "system under consideration". And
     *             finalliy, when a name argument "-n" is provided, the following argument is presumed to be the maximum
     *             number of missing edges allowed.
     */
    public static void main(String[] args) {
        new DesignPatternDetector().run(args);
    }

    private void run(String[] args) {
        try {
            System.out.println("Current directory: " + System.getProperty("user.dir"));

            // Parse the arguments
            parseArgs(args);

            // Parse the input files
            final SystemUnderConsideration system = new ArgoXMIParser().parse(xmiFileName);
            final List<DesignPattern> designPatterns = new TemplatesParser().parse(templateFileName);

            // Find a match for each design pattern in dsp
            designPatterns.forEach(dp -> dp.match(system, maxMissingEdges));

        } catch (Throwable t) {

            // Acknowledge the user of the unrecoverable error situation
            t.printStackTrace();
            System.out.println("An unexpected error occurred. Exiting...");

            // Do not call System.ext(). It is a bad habit.
            throw (t);
        }
    }

    private void parseArgs(String[] args) {
        // Every flag should be followed by a value
        if (args.length > 6 || args.length % 2 == 1) {
            throw new IllegalArgumentException("Illegal number of parameters. " + USAGE_TXT);
        }

        for (int i = 0; i < args.length; i += 2) {
            if (args[i].equals("-t")) {
                templateFileName = args[i + 1];
            } else if (args[i].equals("-x")) {
                xmiFileName = args[i + 1];
            } else if (args[i].equals("-n")) {
                maxMissingEdges = Integer.parseInt(args[i + 1]);
            } else {
                throw new IllegalArgumentException("Incorrect parameter: " + args[i] + ". " + USAGE_TXT);
            }
        }
    }
}
