-injars         vgo/build/distributions/vgo.zip
-outjars        vgo/build/distributions/vgo-release.zip
-libraryjars    <java.home>/jmods

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