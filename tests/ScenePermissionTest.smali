.class public LScenePermissionTest;
.super Ljava/lang/Object;
.method public static main([Ljava/lang/String;)V
    .registers 6
    const-string v0, "com.oculus.permission.USE_SCENE"
    const-string v1, "android.permission.CAMERA"
    filled-new-array {v0, v1}, [Ljava/lang/String;
    move-result-object v2
    invoke-static {v2}, Lcom/google/vr/sdk/samples/permission/PfdmrScenePermissionBridge;->mapArray([Ljava/lang/String;)[Ljava/lang/String;
    move-result-object v3
    const/4 v4, 0x0
    aget-object v5, v2, v4
    if-ne v5, v0, :fail
    aget-object v5, v3, v4
    const-string v0, "com.yvr.permission.USE_SCENE"
    invoke-virtual {v0, v5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v5
    if-eqz v5, :fail
    const/4 v4, 0x1
    aget-object v5, v3, v4
    if-ne v5, v1, :fail
    const/4 v0, 0x0
    invoke-static {v0}, Lcom/google/vr/sdk/samples/permission/PfdmrScenePermissionBridge;->map(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v1
    if-nez v1, :fail
    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    const-string v1, "PASS: real scene permission mapping, original array preserved, unrelated permissions and null preserved"
    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    return-void
    :fail
    new-instance v0, Ljava/lang/AssertionError;
    invoke-direct {v0}, Ljava/lang/AssertionError;-><init>()V
    throw v0
.end method
