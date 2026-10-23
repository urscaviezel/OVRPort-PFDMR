#pragma once
#include <atomic>
#include <cstdint>
#include <cstring>
#include <mutex>
#include <link.h>
#include <sys/mman.h>
#include <unistd.h>
#include <dlfcn.h>

// The patcher pins all three library digests. Runtime checks additionally pin
// both prologs before installing either hook. Only the first, non-PC-relative
// instruction is relocated; this is deliberately not a general hook engine.
namespace pinball {
inline std::atomic<bool> camera{false};
inline std::atomic<void(*)(void*,float)> tickOriginal{nullptr};
inline std::atomic<float(*)(void*)> ratioOriginal{nullptr};
inline uintptr_t unrealBase=0;
inline bool guardProgress(uintptr_t taskVtable,unsigned char state,uintptr_t base){
    return base && taskVtable==base+0x881e6e0 && state!=4 && state!=6 && state!=8;
}
inline float ratio(void* self){
    uintptr_t task=0,vt=0;unsigned char state=0;
    memcpy(&task,static_cast<char*>(self)+0x70,sizeof(task));
    memcpy(&state,static_cast<char*>(self)+0x49,1);
    if(task&&state!=4&&state!=6&&state!=8){
        memcpy(&vt,reinterpret_cast<void*>(task),sizeof(vt));
        if(guardProgress(vt,state,unrealBase))return 0.0f;
    }
    return ratioOriginal.load(std::memory_order_acquire)(self);
}
inline void tick(void* self,float delta){
    auto field=reinterpret_cast<unsigned char*>(self)+0xa8;
    unsigned char saved=*field;bool suppress=!camera.load(std::memory_order_acquire)&&saved;
    if(suppress)*field=0;
    tickOriginal.load(std::memory_order_acquire)(self,delta);
    if(suppress)*field=saved;
}
struct Hook {
    uint32_t* address=nullptr;void* page=MAP_FAILED;size_t pageSize=0;
    void* trampoline=nullptr;uintptr_t codePage=0;uint32_t branch=0;
};
inline bool prepareHook(Hook& hook,uintptr_t address,void* replacement,const uint32_t (&expected)[4]){
    if(memcmp(reinterpret_cast<void*>(address),expected,sizeof(expected)))return false;
    hook.pageSize=static_cast<size_t>(sysconf(_SC_PAGESIZE));
    hook.address=reinterpret_cast<uint32_t*>(address);
    hook.codePage=address&~(hook.pageSize-1);
    // Hint-only allocation never replaces an existing mapping. Reject allocations
    // outside B's range. A single aligned instruction write avoids torn jumps.
    for(int i=1;i<=120&&hook.page==MAP_FAILED;i++)for(int sign:{-1,1}){
        intptr_t hint=static_cast<intptr_t>(hook.codePage)+sign*static_cast<intptr_t>(i)*1024*1024;
        void* page=mmap(reinterpret_cast<void*>(hint),hook.pageSize,PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS,-1,0);
        if(page==MAP_FAILED)continue;
        intptr_t delta=reinterpret_cast<intptr_t>(page)-static_cast<intptr_t>(address);
        if(delta%4||delta<-(1LL<<27)||delta>=(1LL<<27)){munmap(page,hook.pageSize);continue;}
        hook.page=page;hook.branch=0x14000000|(static_cast<uint32_t>(delta/4)&0x03ffffff);break;
    }
    if(hook.page==MAP_FAILED)return false;
    const uint32_t jump[]={0x58000050,0xd61f0200};
    auto bytes=static_cast<unsigned char*>(hook.page);
    memcpy(bytes,jump,8);memcpy(bytes+8,&replacement,8);
    hook.trampoline=bytes+32;
    memcpy(bytes+32,&expected[0],4);memcpy(bytes+36,jump,8);
    uintptr_t resume=address+4;memcpy(bytes+44,&resume,8);
    __builtin___clear_cache(reinterpret_cast<char*>(bytes),reinterpret_cast<char*>(bytes+52));
    if(mprotect(hook.page,hook.pageSize,PROT_READ|PROT_EXEC)){munmap(hook.page,hook.pageSize);hook.page=MAP_FAILED;return false;}
    return true;
}
inline int findUnreal(dl_phdr_info* info,size_t,void* out){
    const char* name=strrchr(info->dlpi_name,'/');name=name?name+1:info->dlpi_name;
    if(strcmp(name,"libUnreal.so"))return 0;
    // Both hook sites must belong to executable file-backed segments.
    for(uintptr_t site:{uintptr_t(0x5cf5500),uintptr_t(0x6076644)}){
        bool found=false;for(int i=0;i<info->dlpi_phnum;i++){
            const auto& p=info->dlpi_phdr[i];if(p.p_type==PT_LOAD&&(p.p_flags&PF_X)&&site>=p.p_vaddr&&site+16<=p.p_vaddr+p.p_filesz)found=true;
        }if(!found)return 0;
    }
    *static_cast<uintptr_t*>(out)=info->dlpi_addr;return 1;
}
inline bool installHooks(){
#ifdef PFDMR_PINBALL_TEST
    return true;
#else
    static std::mutex mutex;static bool installed=false;std::lock_guard<std::mutex> lock(mutex);
    if(installed)return true;
    uintptr_t base=0;dl_iterate_phdr(findUnreal,&base);if(!base)return false;
    const uint32_t tickSig[]={0xd10203ff,0xa9057bfd,0xa90657f6,0xa9074ff4};
    const uint32_t ratioSig[]={0xfc1d0fe8,0xa9017bfd,0xa9024ff4,0x910043fd};
    Hook first,second;
    bool prepared=prepareHook(first,base+0x5cf5500,reinterpret_cast<void*>(tick),tickSig)
        &&prepareHook(second,base+0x6076644,reinterpret_cast<void*>(ratio),ratioSig);
    auto dispose=[&](){for(auto* h:{&first,&second})if(h->page!=MAP_FAILED)munmap(h->page,h->pageSize);};
    if(!prepared){dispose();return false;}
    if(mprotect(reinterpret_cast<void*>(first.codePage),first.pageSize,PROT_READ|PROT_WRITE|PROT_EXEC)){dispose();return false;}
    if(mprotect(reinterpret_cast<void*>(second.codePage),second.pageSize,PROT_READ|PROT_WRITE|PROT_EXEC)){
        mprotect(reinterpret_cast<void*>(first.codePage),first.pageSize,PROT_READ|PROT_EXEC);dispose();return false;
    }
    // Pin the module and publish callable originals before the atomic branches.
    void* pin=dlopen("libUnreal.so",RTLD_NOW|RTLD_NOLOAD);
    if(!pin){for(auto* h:{&first,&second})mprotect(reinterpret_cast<void*>(h->codePage),h->pageSize,PROT_READ|PROT_EXEC);dispose();return false;}
    unrealBase=base;
    tickOriginal.store(reinterpret_cast<void(*)(void*,float)>(first.trampoline),std::memory_order_release);
    ratioOriginal.store(reinterpret_cast<float(*)(void*)>(second.trampoline),std::memory_order_release);
    for(auto* h:{&first,&second}){
        __atomic_store_n(h->address,h->branch,__ATOMIC_RELEASE);
        __builtin___clear_cache(reinterpret_cast<char*>(h->address),reinterpret_cast<char*>(h->address+1));
        if(mprotect(reinterpret_cast<void*>(h->codePage),h->pageSize,PROT_READ|PROT_EXEC))
            __android_log_print(ANDROID_LOG_ERROR,"PFDMRPinball","Could not restore code-page protection");
    }
    installed=true;return true;
#endif
}
}
