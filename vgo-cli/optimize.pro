-dontobfuscate
-allowaccessmodification
-overloadaggressively
-dontskipnonpubliclibraryclasses
-mergeinterfacesaggressively
-verbose

-keep class com.jzbrooks.vgo.cli.CommandLineInterface {
  public static void main(java.lang.String[]);
}