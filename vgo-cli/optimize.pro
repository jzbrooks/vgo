-dontobfuscate
-allowaccessmodification
-overloadaggressively
-dontskipnonpubliclibraryclasses
-mergeinterfacesaggressively
-verbose

-keep class com.jzbrooks.vgo.cli.ApplicationKt {
  public static void main(java.lang.String[]);
}