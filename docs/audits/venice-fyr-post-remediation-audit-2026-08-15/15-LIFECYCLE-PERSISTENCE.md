# Lifecycle and Persistence

Chat ViewModel ownership survives configuration change, conversation/model identity uses `SavedStateHandle`/Room, and cancellation writes a terminal non-completed status. Room message/conversation updates are transactional and profile-scoped.

Not proven on device: rotation, background/foreground, process recreation, and stream completion during navigation. Image prompt/model/input/result are ViewModel-only; process death loses them. Generated images are cache files, not durable gallery records. Audio/video queue state has no Room/WorkManager representation. These are P2/P1 work packages, not hidden successes.
