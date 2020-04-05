-injars       vgo/build/libs/debug/vgo.jar
-outjars      vgo/build/libs/vgo.jar
-libraryjars  <java.home>/jmods

-dontobfuscate
-optimizationpasses 5
-allowaccessmodification
-overloadaggressively
-dontskipnonpubliclibraryclasses
-mergeinterfacesaggressively
-verbose

-keep class com.jzbrooks.vgo.Application {
  public static void main(java.lang.String[]);
}