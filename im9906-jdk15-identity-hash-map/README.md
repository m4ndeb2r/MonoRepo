# IM9906-Jdk15IdentityHashMap
SE Graduation Assignment Tryout

# Purpose
Tryout project to see if formal analysis of OpenJDK's `IdentityHashMap` (JDK15) is possible with KeY  (http://hg.openjdk.java.net/jdk/jdk15/file/0dabbdfd97e6/src/java.base/share/classes/java/util/IdentityHashMap.java).
This projected is related to the IM9906-VerifyingIdentityHashMap project (see: https://github.com/m4ndeb2r/IM9906-VerifyingIdentityHashMap), containing a JDK7 version of the class including JML-specs, that is being / has been (partly) successfully verified with KeY.

# Setup
To replicate the setup of the project, follow the steps below:
1. create a KeY project in Eclipse (use Eclipse 2020-03, with KeY plugin 2.6.3),
1. prepare the class for verification with KeY,
1. generate stubs for the classes depended on,
1. strip generics from the class under verification,
1. solve any problems after removal of generics,
1. load the class under verification in KeY.

## Create a KeY project in Eclipse
Prerequisites: Eclipse + KeY plugin for Eclipse. The Eclipse version we used is 2020-03. The KeY pluging version is 2.6.3. The Java version used is Java SE 15.
Create a new KeY-project. In the menu, choose:
* File > New > Project,
* choose: KeY > KeY Project,
* click next button,
* choose the defaults, except name (IM9906 - Jdk15IdentityHashMap) and JRE (Java SE 15).

## Prepare the class under verification
In your new (empty) KeY project, follow these steps:
* create a package `java.util` in your src directory,
* in this package, create a new (empty) class named `Jdk15IdentityHashMap`,
* copy the sourcecode from [http://hg.openjdk.java.net/jdk/jdk15/file/0dabbdfd97e6/src/java.base/share/classes/java/util/IdentityHashMap.java] and paste it into your newly created class,
* replace all occurences of the string `IdentityHashMap` in the file with `Jdk15IdentityHashMap` to match the filename of the class,
* save the class.

Note: the class under verification is called `Jdk15IdentityHashMap` instead of its original name `IdentityHashMap` to prevent it from clashing with the name of the original class in the JDK library in the same package.

## Generate stubs for the classes depended on
The class under verification is dependent of a number of library classes. For the purpose of formal analysis, we don't need binaries of these classes, but we do need to generate stubs for them. The KeY plugin is able to do this for us:
* select your project, and right-click on it for the context menu,
* from the context menu, choose Generate Stubs,
* you will be prompted for a directory name (choose "jre") and a location (select radio button "Boot Class Path"),
* click the finish button.

Three packages containing stub classes will be generated: `java.io`, `java.lang`, and `java.util`.

## Remove generics from Jdk15IdentityHashMap
* select your project, and right-click on it for the context menu,
* from the context menu, choose Remove Generics (this will fail the first time, with errors on two lines),
* ...

## Fix some problems after removing generics
* ...
* save the class

Now, the class can be loaded in KeY. 

## Load Jdk15IdentityHashMap in KeY
* select your project, and right-click on it for the context menu
* from the context menu, choose Load Project.
