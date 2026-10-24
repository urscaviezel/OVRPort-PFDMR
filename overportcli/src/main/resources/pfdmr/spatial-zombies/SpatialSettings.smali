.class public final Lmoe/crx/pfdmr/SpatialSettings;
.super Ljava/lang/Object;
.source "SpatialSettings.java"


# static fields
.field private static final HOME:Ljava/lang/String; = "com.yvr.spatialhome"

.field private static generation:I

.field private static lastRequest:J


# direct methods
.method static constructor <clinit>()V
    .registers 2

    .line 13
    const-wide/16 v0, -0x2710

    sput-wide v0, Lmoe/crx/pfdmr/SpatialSettings;->lastRequest:J

    return-void
.end method

.method public constructor <init>()V
    .registers 1

    .line 12
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method static synthetic access$000()I
    .registers 1

    .line 12
    sget v0, Lmoe/crx/pfdmr/SpatialSettings;->generation:I

    return v0
.end method

.method static synthetic access$100(Landroid/content/Context;Ljava/lang/String;)V
    .registers 2

    .line 12
    invoke-static {p0, p1}, Lmoe/crx/pfdmr/SpatialSettings;->openSettings(Landroid/content/Context;Ljava/lang/String;)V

    return-void
.end method

.method private static isSettingsPackage(Ljava/lang/String;)Z
    .registers 2

    .line 17
    const-string v0, "com.yvr.spatialhome"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_1b

    const-string v0, "com.oculus.panelapp.settings"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_1b

    .line 18
    const-string v0, "com.oculus.vrshell"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-eqz p0, :cond_19

    goto :goto_1b

    :cond_19
    const/4 p0, 0x0

    goto :goto_1c

    :cond_1b
    :goto_1b
    const/4 p0, 0x1

    .line 17
    :goto_1c
    return p0
.end method

.method public static map(Landroid/app/Activity;Landroid/content/Intent;)Landroid/content/Intent;
    .registers 10

    .line 22
    const/4 v0, 0x0

    if-nez p1, :cond_4

    return-object v0

    .line 23
    :cond_4
    invoke-virtual {p1}, Landroid/content/Intent;->getDataString()Ljava/lang/String;

    move-result-object v1

    .line 24
    invoke-virtual {p1}, Landroid/content/Intent;->getComponent()Landroid/content/ComponentName;

    move-result-object v2

    .line 25
    const/4 v3, 0x1

    if-eqz v1, :cond_17

    const-string v4, "systemux://settings"

    invoke-virtual {v1, v4}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v4

    if-nez v4, :cond_5a

    .line 26
    :cond_17
    invoke-virtual {p1}, Landroid/content/Intent;->getPackage()Ljava/lang/String;

    move-result-object v4

    invoke-static {v4}, Lmoe/crx/pfdmr/SpatialSettings;->isSettingsPackage(Ljava/lang/String;)Z

    move-result v4

    if-nez v4, :cond_5a

    if-eqz v2, :cond_2d

    .line 27
    invoke-virtual {v2}, Landroid/content/ComponentName;->getPackageName()Ljava/lang/String;

    move-result-object v2

    invoke-static {v2}, Lmoe/crx/pfdmr/SpatialSettings;->isSettingsPackage(Ljava/lang/String;)Z

    move-result v2

    if-nez v2, :cond_5a

    :cond_2d
    nop

    .line 28
    invoke-virtual {p1}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object v2

    const-string v4, "android.settings.APPLICATION_DETAILS_SETTINGS"

    invoke-virtual {v4, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_58

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "package:"

    invoke-virtual {v2, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    .line 29
    invoke-virtual {p0}, Landroid/app/Activity;->getPackageName()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v2, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v2, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_58

    goto :goto_5a

    :cond_58
    const/4 v1, 0x0

    goto :goto_5b

    :cond_5a
    :goto_5a
    move v1, v3

    .line 30
    :goto_5b
    if-nez v1, :cond_5e

    return-object p1

    .line 34
    :cond_5e
    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide v1

    .line 35
    sget-wide v4, Lmoe/crx/pfdmr/SpatialSettings;->lastRequest:J

    sub-long v4, v1, v4

    const-wide/16 v6, 0xbb8

    cmp-long p1, v4, v6

    if-gez p1, :cond_6d

    return-object v0

    .line 36
    :cond_6d
    sput-wide v1, Lmoe/crx/pfdmr/SpatialSettings;->lastRequest:J

    .line 37
    sget p1, Lmoe/crx/pfdmr/SpatialSettings;->generation:I

    add-int/2addr p1, v3

    sput p1, Lmoe/crx/pfdmr/SpatialSettings;->generation:I

    .line 38
    new-instance v1, Ljava/lang/ref/WeakReference;

    invoke-direct {v1, p0}, Ljava/lang/ref/WeakReference;-><init>(Ljava/lang/Object;)V

    .line 39
    invoke-virtual {p0}, Landroid/app/Activity;->getApplicationContext()Landroid/content/Context;

    move-result-object v2

    .line 40
    new-instance v3, Landroid/os/Handler;

    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;

    move-result-object v4

    invoke-direct {v3, v4}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V

    .line 41
    new-instance v4, Lmoe/crx/pfdmr/SpatialSettings$1;

    invoke-direct {v4, v1, p1, v2}, Lmoe/crx/pfdmr/SpatialSettings$1;-><init>(Ljava/lang/ref/WeakReference;ILandroid/content/Context;)V

    const-wide/16 v5, 0x4b0

    invoke-virtual {v3, v4, v5, v6}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z

    .line 48
    new-instance v4, Lmoe/crx/pfdmr/SpatialSettings$2;

    invoke-direct {v4, v1, p1, v2}, Lmoe/crx/pfdmr/SpatialSettings$2;-><init>(Ljava/lang/ref/WeakReference;ILandroid/content/Context;)V

    const-wide/16 v1, 0x7d0

    invoke-virtual {v3, v4, v1, v2}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z

    .line 55
    invoke-static {p0}, Lmoe/crx/pfdmr/SpatialSettings;->openFocusAwareMenu(Landroid/app/Activity;)Z

    move-result p0

    if-eqz p0, :cond_a1

    return-object v0

    .line 56
    :cond_a1
    const-string p0, "PfdmrSettings"

    const-string p1, "Focus-aware menu unavailable; falling back to regular Home"

    invoke-static {p0, p1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    .line 57
    new-instance p0, Landroid/content/Intent;

    const-string p1, "android.intent.action.MAIN"

    invoke-direct {p0, p1}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V

    .line 58
    const-string p1, "android.intent.category.HOME"

    invoke-virtual {p0, p1}, Landroid/content/Intent;->addCategory(Ljava/lang/String;)Landroid/content/Intent;

    .line 59
    const/high16 p1, 0x10000000

    invoke-virtual {p0, p1}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;

    .line 60
    return-object p0
.end method

.method private static openFocusAwareMenu(Landroid/app/Activity;)Z
    .registers 7

    .line 67
    const-string v0, "PfdmrSettings"

    const/4 v1, 0x0

    :try_start_3
    const-string v2, "yvr_strategy"

    invoke-virtual {p0, v2}, Landroid/app/Activity;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object p0

    .line 68
    if-nez p0, :cond_c

    return v1

    .line 69
    :cond_c
    const-string v2, "android.os.DeviceStrategyProperty"

    invoke-static {v2}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v2

    .line 70
    new-array v3, v1, [Ljava/lang/Class;

    invoke-virtual {v2, v3}, Ljava/lang/Class;->getConstructor([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;

    move-result-object v3

    new-array v4, v1, [Ljava/lang/Object;

    invoke-virtual {v3, v4}, Ljava/lang/reflect/Constructor;->newInstance([Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    .line 71
    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v4

    const-string v5, "updateStrategyToFAForThirdApps"

    filled-new-array {v2}, [Ljava/lang/Class;

    move-result-object v2

    .line 72
    invoke-virtual {v4, v5, v2}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v2

    filled-new-array {v3}, [Ljava/lang/Object;

    move-result-object v3

    .line 73
    invoke-virtual {v2, p0, v3}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p0

    .line 74
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "PFDM focus-aware menu accepted="

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 75
    sget-object v2, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;

    invoke-virtual {v2, p0}, Ljava/lang/Boolean;->equals(Ljava/lang/Object;)Z

    move-result p0
    :try_end_50
    .catch Ljava/lang/ReflectiveOperationException; {:try_start_3 .. :try_end_50} :catch_53
    .catch Ljava/lang/RuntimeException; {:try_start_3 .. :try_end_50} :catch_51

    return p0

    .line 76
    :catch_51
    move-exception p0

    goto :goto_54

    :catch_53
    move-exception p0

    .line 77
    :goto_54
    const-string v2, "PFDM focus-aware menu call failed"

    invoke-static {v0, v2, p0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    .line 78
    return v1
.end method

.method private static openSettings(Landroid/content/Context;Ljava/lang/String;)V
    .registers 6

    .line 83
    const-string v0, "PfdmrSettings"

    new-instance v1, Landroid/content/Intent;

    const-string v2, "com.yvr.spatialhome.action.OPEN_WIDGET"

    invoke-direct {v1, v2}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V

    .line 84
    const-string v2, "com.yvr.spatialhome"

    invoke-virtual {v1, v2}, Landroid/content/Intent;->setPackage(Ljava/lang/String;)Landroid/content/Intent;

    .line 85
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "{\"packageName\":\"com.yvr.settings\",\"uri\":\""

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    const-string v3, "\"}"

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    const-string v3, "result"

    invoke-virtual {v1, v3, v2}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    .line 86
    :try_start_2c
    invoke-virtual {p0, v1}, Landroid/content/Context;->sendBroadcast(Landroid/content/Intent;)V
    :try_end_2f
    .catch Ljava/lang/RuntimeException; {:try_start_2c .. :try_end_2f} :catch_30

    goto :goto_36

    :catch_30
    move-exception p0

    const-string v1, "Settings navigation unavailable"

    invoke-static {v0, v1, p0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    .line 87
    :goto_36
    new-instance p0, Ljava/lang/StringBuilder;

    invoke-direct {p0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "Opening PFDM settings: "

    invoke-virtual {p0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v0, p0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 88
    return-void
.end method
