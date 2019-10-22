-injars       guacamole/build/libs/guacamole.jar
-outjars      guacamole/build/libs/guacamole_opt.jar
-libraryjars  <java.home>/jmods

-dontobfuscate
-optimizationpasses 5
-allowaccessmodification
-overloadaggressively
-dontskipnonpubliclibraryclasses
-mergeinterfacesaggressively

-keep class com.jzbrooks.guacamole.App {
  public static void main(java.lang.String[]);
}