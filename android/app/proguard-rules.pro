# kotlinx.serialization keeps generated serializers via its own consumer rules.
# Retrofit 3 ships consumer rules. Keep DTO class names for clearer crash logs.
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-keep,includedescriptorclasses class com.movieradar.app.data.remote.**$$serializer { *; }
