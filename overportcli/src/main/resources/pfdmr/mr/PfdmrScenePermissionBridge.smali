.class public final Lcom/google/vr/sdk/samples/permission/PfdmrScenePermissionBridge;
.super Ljava/lang/Object;

.method public static map(Ljava/lang/String;)Ljava/lang/String;
    .registers 3
    const-string v0, "com.oculus.permission.USE_SCENE"
    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v1
    if-eqz v1, :done
    const-string p0, "com.yvr.permission.USE_SCENE"
    :done
    return-object p0
.end method

.method public static mapArray([Ljava/lang/String;)[Ljava/lang/String;
    .registers 5
    invoke-virtual {p0}, [Ljava/lang/String;->clone()Ljava/lang/Object;
    move-result-object v0
    check-cast v0, [Ljava/lang/String;
    const/4 v1, 0x0
    array-length v2, v0
    :loop
    if-ge v1, v2, :done
    aget-object v3, v0, v1
    invoke-static {v3}, Lcom/google/vr/sdk/samples/permission/PfdmrScenePermissionBridge;->map(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v3
    aput-object v3, v0, v1
    add-int/lit8 v1, v1, 0x1
    goto :loop
    :done
    return-object v0
.end method
