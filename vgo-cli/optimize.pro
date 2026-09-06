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

-dontwarn com.google.auto.service.**
-dontwarn gnu.trove.TObjectHashingStrategy
-dontwarn kotlin.annotations.jvm.**
-dontwarn kotlinx.coroutines.future.**
-dontwarn kotlinx.coroutines.internal.intellij.IntellijCoroutines
-dontwarn kotlinx.serialization.**
-dontwarn org.jetbrains.annotations.**
-dontwarn org.jetbrains.kotlin.com.google.errorprone.**
-dontwarn org.jetbrains.kotlin.com.google.j2objc.**
-dontwarn org.kxml2.io.**

# ElementPrinter has a single implementer, so r8 merges it away, but it fails to
# rewrite the makeConcatWithConstants descriptor in the generated toString of the
# ConvertPathsToShapes.Criterion.SmallerOutput data class that holds one. Verifying
# that class then fails with NoClassDefFoundError. Reproduced through r8 9.4.17.
-keep,allowoptimization interface com.jzbrooks.vgo.core.graphic.ElementPrinter
