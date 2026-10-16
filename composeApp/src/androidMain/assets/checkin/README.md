# Local drink photo models

These versioned models are packaged in the Android APK. No runtime download,
Google Play installation, API key, or image upload is required for extraction.
Both are distributed under Apache License 2.0; see `LICENSE.txt` in this directory.

| File | Upstream version | SHA-256 |
| --- | --- | --- |
| efficientdet_lite0.tflite | [EfficientDet-Lite0 int8 v1](https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/int8/1/efficientdet_lite0.tflite) | 0720bf247bd76e6594ea28fa9c6f7c5242be774818997dbbeffc4da460c723bb |
| magic_touch.tflite | [MagicTouch float32 v1](https://storage.googleapis.com/mediapipe-models/interactive_segmenter/magic_touch/float32/1/magic_touch.tflite) | e24338a717c1b7ad8d159666677ef400babb7f33b8ad60c4d96db4ecf694cd25 |

EfficientDet-Lite0 locates cups, glasses and bottles. MagicTouch uses each vessel's
centre to segment that object separately from touching hands and background.
The original image colours are retained, including skin-coloured cups.

References: [object detector](https://ai.google.dev/edge/mediapipe/solutions/vision/object_detector),
[MagicTouch model card](https://storage.googleapis.com/mediapipe-assets/Model%20Card%20MagicTouch.pdf),
[TensorFlow models license](https://github.com/tensorflow/models/blob/master/LICENSE).

Model weights add approximately 11 MB before APK compression. MediaPipe is pinned
to 0.10.35, whose one-shot interactive segmenter supports the compact v1 model.

Coordinate compatibility: the 0.10.35 Java ROI adapter sets `x` and `y` but does
not set the protobuf `Point.normalized` flag. The native graph therefore reads
pixel coordinates. The Android extractor passes pixel centres even though the
Java container is named `NormalizedKeypoint`. Recheck this when updating MediaPipe:
[Java adapter](https://github.com/google-ai-edge/mediapipe/blob/v0.10.35/mediapipe/tasks/java/com/google/mediapipe/tasks/vision/interactivesegmenter/InteractiveSegmenter.java),
[native ROI graph](https://github.com/google-ai-edge/mediapipe/blob/v0.10.35/mediapipe/tasks/cc/vision/interactive_segmenter/interactive_segmenter_graph.cc),
[protobuf definition](https://github.com/google-ai-edge/mediapipe/blob/v0.10.35/mediapipe/tasks/cc/vision/interactive_segmenter/proto/region_of_interest.proto).
