# Keep only app-specific reflection or JNI entry points here.
# Ktor, kotlinx.serialization, Room, Google Play services and AndroidX
# provide their own consumer rules for the app's release build.

# Shelf bag scan (U2-Net-p / MI-GAN): ONNX Runtime JNI looks up TensorInfo and
# related types by name during OrtSession.run. R8 removes them without these rules,
# which aborts with "java_class == null in call to GetMethodID".
# https://onnxruntime.ai/docs/build/android.html#note-proguard-rules-for-r8-minimization-android-app-builds-to-work
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**
