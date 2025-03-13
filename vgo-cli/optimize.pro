-dontobfuscate
-allowaccessmodification
-overloadaggressively
-dontskipnonpubliclibraryclasses
-mergeinterfacesaggressively
-verbose

-keep class com.jzbrooks.vgo.cli.CommandLineInterface {
  public static void main(java.lang.String[]);
}

# Used in an EnumMap inside tools-sdk code. It is required for converting vectors with clip paths.
-keep,allowoptimization enum com.android.ide.common.vectordrawable.SvgNode$ClipRule {
  public static **[] $VALUES;
  public static com.android.ide.common.vectordrawable.SvgNode$ClipRule[] values();
  public static com.android.ide.common.vectordrawable.SvgNode$ClipRule valueOf(java.lang.String);
}