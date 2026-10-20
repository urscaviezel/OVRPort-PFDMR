#include <openxr/openxr.h>
#include <string.h>
static XrResult poll_mock(XrInstance i,XrEventDataBuffer *e) {(void)i;(void)e;return XR_EVENT_UNAVAILABLE;}
XRAPI_ATTR XrResult XRAPI_CALL xrGetInstanceProcAddr(XrInstance i,const char *name,PFN_xrVoidFunction *out) {
 (void)i;*out=NULL;
 if(!strcmp(name,"xrGetInstanceProcAddr"))*out=(PFN_xrVoidFunction)xrGetInstanceProcAddr;
 if(!strcmp(name,"xrPollEvent"))*out=(PFN_xrVoidFunction)poll_mock;
 return *out?XR_SUCCESS:XR_ERROR_FUNCTION_UNSUPPORTED;
}
