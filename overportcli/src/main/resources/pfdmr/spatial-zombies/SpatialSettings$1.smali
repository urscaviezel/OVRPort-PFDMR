.class Lmoe/crx/pfdmr/SpatialSettings$1;
.super Ljava/lang/Object;
.source "SpatialSettings.java"

# interfaces
.implements Ljava/lang/Runnable;


# annotations
.annotation system Ldalvik/annotation/EnclosingMethod;
    value = Lmoe/crx/pfdmr/SpatialSettings;->map(Landroid/app/Activity;Landroid/content/Intent;)Landroid/content/Intent;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x0
    name = null
.end annotation


# instance fields
.field final synthetic val$context:Landroid/content/Context;

.field final synthetic val$owner:Ljava/lang/ref/WeakReference;

.field final synthetic val$request:I


# direct methods
.method constructor <init>(Ljava/lang/ref/WeakReference;ILandroid/content/Context;)V
    .registers 4
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()V"
        }
    .end annotation

    .line 41
    iput-object p1, p0, Lmoe/crx/pfdmr/SpatialSettings$1;->val$owner:Ljava/lang/ref/WeakReference;

    iput p2, p0, Lmoe/crx/pfdmr/SpatialSettings$1;->val$request:I

    iput-object p3, p0, Lmoe/crx/pfdmr/SpatialSettings$1;->val$context:Landroid/content/Context;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public run()V
    .registers 4

    .line 43
    iget-object v0, p0, Lmoe/crx/pfdmr/SpatialSettings$1;->val$owner:Ljava/lang/ref/WeakReference;

    invoke-virtual {v0}, Ljava/lang/ref/WeakReference;->get()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/app/Activity;

    .line 44
    iget v1, p0, Lmoe/crx/pfdmr/SpatialSettings$1;->val$request:I

    # getter for: Lmoe/crx/pfdmr/SpatialSettings;->generation:I
    invoke-static {}, Lmoe/crx/pfdmr/SpatialSettings;->access$000()I

    move-result v2

    if-ne v1, v2, :cond_27

    if-eqz v0, :cond_27

    invoke-virtual {v0}, Landroid/app/Activity;->isFinishing()Z

    move-result v1

    if-nez v1, :cond_27

    invoke-virtual {v0}, Landroid/app/Activity;->isDestroyed()Z

    move-result v0

    if-eqz v0, :cond_1f

    goto :goto_27

    .line 45
    :cond_1f
    iget-object v0, p0, Lmoe/crx/pfdmr/SpatialSettings$1;->val$context:Landroid/content/Context;

    const-string v1, "yvrsettings://settingstab/YVR.Settings.AppSettingsView"

    # invokes: Lmoe/crx/pfdmr/SpatialSettings;->openSettings(Landroid/content/Context;Ljava/lang/String;)V
    invoke-static {v0, v1}, Lmoe/crx/pfdmr/SpatialSettings;->access$100(Landroid/content/Context;Ljava/lang/String;)V

    .line 46
    return-void

    .line 44
    :cond_27
    :goto_27
    return-void
.end method
