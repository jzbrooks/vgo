# ElementPrinter has a single implementer, so r8 merges it away, but it fails to
# rewrite the makeConcatWithConstants descriptor in the generated toString of the
# ConvertPathsToShapes.Criterion.SmallerOutput data class that holds one. Verifying
# that class then fails with NoClassDefFoundError. Reproduced through r8 9.4.17.
-keep,allowoptimization interface com.jzbrooks.vgo.core.graphic.ElementPrinter
