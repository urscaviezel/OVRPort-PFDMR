// SPDX-License-Identifier: GPL-3.0-only
// FramePort thread-key pool for Unity (patch frame.unity_tls_key_pool, GitHub #198).
//
// Bionic gives a process 128 pthread keys. Unity 6 takes about 40 of them (most without a destructor), and on the
// Frame the libraries Lepton and FramePort add (Mesa/Zink, FrameBridge, OVRPort's dispatcher, Valve's Vulkan layers)
// take more than on a Quest. Games with many other native libraries then run out: Wander 0.3570 (Gecko, avatar
// libraries, Firebase) aborted in Firestore's start with "cannot create thread specific key for __cxa_get_globals()".
//
// FramePort renames libunity.so's imports pthread_key_create/_delete and pthread_get/setspecific to the fp_tkey_*
// functions below (same strings, shorter, NUL-padded) and adds this library to its DT_NEEDED. Keys without a
// destructor become slots in one per-thread table that lives under a single real key; keys with a destructor, and
// every other library, keep using real keys. Approach and the original helper: Klownicle (GitHub #198), verified in
// the headset; this is FramePort's own implementation of it.
//
// Pooled keys are TAG + slot (bionic's own keys have bit 31 set, so the ranges can't meet). A slot's generation
// changes at every create, so a value stored under a deleted key isn't seen through a new key that reuses the slot.
// Using a key after deleting it is undefined in POSIX; here it just reads NULL / fails with EINVAL.
#include <android/log.h>
#include <errno.h>
#include <pthread.h>
#include <stdatomic.h>
#include <stdint.h>
#include <stdlib.h>

#define TAG_LOG "FrameBridge"
#define EXPORT __attribute__((visibility("default")))
#define LOG(...) __android_log_print(ANDROID_LOG_INFO, TAG_LOG, __VA_ARGS__)

#define SLOTS 512
#define TAG 0x40000000u

struct table {
    void *value[SLOTS];
    uint32_t generation[SLOTS];
};

static pthread_once_t once = PTHREAD_ONCE_INIT;
static pthread_key_t storage;
static int storage_error;
static _Atomic uint32_t used[SLOTS];       // 0 free, 1 taken
static _Atomic uint32_t generation[SLOTS];  // never 0 for a taken slot
static _Atomic int pooled, logged_full;

static void setup(void) {
    storage_error = pthread_key_create(&storage, free);  // the table goes with its thread
    if (storage_error) LOG("fp_tkeys: no real key for the pool (%d): Unity's keys stay real ones", storage_error);
}

static int slot_of(pthread_key_t key, unsigned *slot) {
    uint32_t k = (uint32_t)key;
    if (k < TAG || k >= TAG + SLOTS) return 0;
    *slot = k - TAG;
    return 1;
}

EXPORT int fp_tkey_create(pthread_key_t *key, void (*destructor)(void *)) {
    if (destructor) return pthread_key_create(key, destructor);
    pthread_once(&once, setup);
    if (storage_error) return pthread_key_create(key, NULL);
    for (unsigned i = 0; i < SLOTS; i++) {
        uint32_t expected = 0;
        if (atomic_compare_exchange_strong(&used[i], &expected, 1)) {
            uint32_t g = atomic_fetch_add(&generation[i], 1) + 1;
            if (!g) atomic_fetch_add(&generation[i], 1);  // skip 0: "never set" in a fresh table
            *key = (pthread_key_t)(TAG + i);
            if (atomic_fetch_add(&pooled, 1) == 0) LOG("fp_tkeys: pooling Unity's thread keys without destructor");
            return 0;
        }
    }
    if (!atomic_exchange(&logged_full, 1)) LOG("fp_tkeys: all %d pooled keys taken, using real keys", SLOTS);
    return pthread_key_create(key, NULL);
}

EXPORT int fp_tkey_delete(pthread_key_t key) {
    unsigned i;
    if (!slot_of(key, &i)) return pthread_key_delete(key);
    uint32_t expected = 1;
    if (!atomic_compare_exchange_strong(&used[i], &expected, 0)) return EINVAL;
    atomic_fetch_sub(&pooled, 1);
    return 0;
}

EXPORT void *fp_tkey_get(pthread_key_t key) {
    unsigned i;
    if (!slot_of(key, &i)) return pthread_getspecific(key);
    struct table *t = pthread_getspecific(storage);
    if (!t || !atomic_load_explicit(&used[i], memory_order_acquire)) return NULL;
    return t->generation[i] == atomic_load_explicit(&generation[i], memory_order_relaxed) ? t->value[i] : NULL;
}

EXPORT int fp_tkey_set(pthread_key_t key, const void *value) {
    unsigned i;
    if (!slot_of(key, &i)) return pthread_setspecific(key, value);
    if (!atomic_load_explicit(&used[i], memory_order_acquire)) return EINVAL;
    struct table *t = pthread_getspecific(storage);
    if (!t) {
        if (!value) return 0;  // nothing stored yet: NULL is what get answers anyway
        t = calloc(1, sizeof(*t));
        if (!t) return ENOMEM;
        int rc = pthread_setspecific(storage, t);
        if (rc) {
            free(t);
            return rc;
        }
    }
    t->value[i] = (void *)value;
    t->generation[i] = atomic_load_explicit(&generation[i], memory_order_relaxed);
    return 0;
}
