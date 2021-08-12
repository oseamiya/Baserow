# Add any ProGuard configurations specific to this
# extension here.

-keep public class com.oseamiya.baserow.Baserow {
    public *;
 }
-keeppackagenames gnu.kawa**, gnu.expr**

-optimizationpasses 4
-allowaccessmodification
-mergeinterfacesaggressively

-repackageclasses 'com/oseamiya/baserow/repack'
-flattenpackagehierarchy
-dontpreverify
