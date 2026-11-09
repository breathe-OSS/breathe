# gson fills these in through reflection, so r8 can't rename or strip
# any of the fields
-keep class com.sidharthify.breathe.data.** { *; }
-keep class com.sidharthify.breathe.util.HistoryPeriod { *; }
-keep enum com.sidharthify.breathe.util.HistoryPeriodMode { *; }

# TypeToken subclasses in the cache loader need their generic signature
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-keep class * extends com.google.gson.reflect.TypeToken

# glance looks the widget action callbacks up by class name
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }
