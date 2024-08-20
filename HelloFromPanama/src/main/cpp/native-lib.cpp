#define DEFAULT_VISIBILITY __attribute__ ((visibility ("default")))

extern "C" DEFAULT_VISIBILITY const char *getString() {
    return "Hello from native!";
}