#include <pthread.h>
const char *volatile a="com.oculus.systemactivities";
const char *volatile b="ovrp_Update2";
const char *volatile c="ovrp_BeginFrame";
const char *volatile d="ovrp_EndFrame";
void *pool(void *v){pthread_key_t key;pthread_key_create(&key,0);pthread_setspecific(key,v);v=pthread_getspecific(key);pthread_key_delete(key);return v;}

extern double vrapi_GetTimeInSeconds(void);
double bridge_import(void){return vrapi_GetTimeInSeconds();}
