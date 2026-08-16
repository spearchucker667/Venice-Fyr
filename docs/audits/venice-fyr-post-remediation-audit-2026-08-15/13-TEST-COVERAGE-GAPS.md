# Test Coverage Gaps

Current JVM tests prove core chat framing/cancellation, Room repositories, redaction, model parsing, binary image handling, video retrieve variants, and queued-audio types. They do not prove live provider behavior or Android framework behavior.

Highest gaps: non-streaming/multimodal chat, usage/search metadata, multipart image APIs, multipart audio body assertions, all video queue fields, HTTP 402 on each media family, durable job state machines, process recreation, Photo Picker/SAF grants, Keystore on device, large/corrupt media, release/minified runtime, and SDK binary compatibility.

Instrumentation status: BLOCKED/NOT CONFIGURED. No connected-device task or managed virtual device is defined. This audit does not convert JVM/Robolectric results into device claims.
