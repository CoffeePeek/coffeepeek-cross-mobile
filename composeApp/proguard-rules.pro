# Keep only app-specific reflection or JNI entry points here.
# Ktor, kotlinx.serialization, Room, Google Play services and AndroidX
# provide their own consumer rules for the app's release build.

# MediaPipe Tasks 0.10.35 does not package its framework consumer rules.
# These Java entry points are called by JNI and must keep their names/members.
# https://github.com/google-ai-edge/mediapipe/blob/v0.10.35/mediapipe/java/com/google/mediapipe/framework/proguard.pgcfg
-keep public interface com.google.mediapipe.framework.* {
    public *;
}
-keep public class com.google.mediapipe.framework.Packet {
    public static *** create(***);
    public long getNativeHandle();
    public void release();
}
-keep public class com.google.mediapipe.framework.PacketCreator {
    *** releaseWithSyncToken(...);
}
-keep public class com.google.mediapipe.framework.MediaPipeException {
    <init>(int, byte[]);
}
-keep class com.google.mediapipe.framework.ProtoUtil$SerializedMessage { *; }

# Graph initializes Flogger through these stack-sensitive methods. Inlining
# either frame causes "no caller found on the stack" before a photo model runs.
-keep class com.google.common.flogger.FluentLogger {
    public static com.google.common.flogger.FluentLogger forEnclosingClass();
}
-keep class com.google.common.flogger.backend.system.StackBasedCallerFinder {
    public java.lang.String findLoggingClass(java.lang.Class);
}

# Protobuf Lite reads fields by their schema names when building task options.
# Keep fields on live messages; unused message classes can still be removed.
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
