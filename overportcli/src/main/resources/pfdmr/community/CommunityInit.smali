.class public final Lmoe/crx/pfdmr/CommunityInit;
.super Landroid/content/ContentProvider;
.source "CommunityInit.java"


# direct methods
.method public constructor <init>()V
    .registers 1

    .line 10
    invoke-direct {p0}, Landroid/content/ContentProvider;-><init>()V

    return-void
.end method


# virtual methods
.method public delete(Landroid/net/Uri;Ljava/lang/String;[Ljava/lang/String;)I
    .registers 4

    .line 36
    const/4 p1, 0x0

    return p1
.end method

.method public getType(Landroid/net/Uri;)Ljava/lang/String;
    .registers 2

    .line 35
    const/4 p1, 0x0

    return-object p1
.end method

.method public insert(Landroid/net/Uri;Landroid/content/ContentValues;)Landroid/net/Uri;
    .registers 3

    .line 35
    const/4 p1, 0x0

    return-object p1
.end method

.method public onCreate()Z
    .registers 13

    .line 13
    const-string v0, "\n"

    const-string v1, "PFDMRCommunity"

    const/4 v2, 0x1

    :try_start_5
    invoke-virtual {p0}, Lmoe/crx/pfdmr/CommunityInit;->getContext()Landroid/content/Context;

    move-result-object v3

    invoke-virtual {v3}, Landroid/content/Context;->getPackageName()Ljava/lang/String;

    move-result-object v4

    const/4 v5, 0x0

    invoke-virtual {v3, v5}, Landroid/content/Context;->getExternalFilesDir(Ljava/lang/String;)Ljava/io/File;

    move-result-object v3

    .line 14
    if-eqz v3, :cond_1a7

    .line 15
    const-string v5, "com.crytek.climb"

    invoke-virtual {v4, v5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_12a

    .line 16
    new-instance v4, Ljava/io/File;

    const-string v5, "user.cfg"

    invoke-direct {v4, v3, v5}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    .line 17
    invoke-virtual {v4}, Ljava/io/File;->exists()Z

    move-result v5
    :try_end_27
    .catch Ljava/lang/Exception; {:try_start_5 .. :try_end_27} :catch_1af

    const-string v6, ""

    const-string v7, "UTF-8"

    if-eqz v5, :cond_71

    :try_start_2d
    invoke-virtual {v4}, Ljava/io/File;->length()J

    move-result-wide v8

    const-wide/32 v10, 0x100000

    cmp-long v5, v8, v10

    if-gtz v5, :cond_69

    new-instance v5, Ljava/io/ByteArrayOutputStream;

    invoke-direct {v5}, Ljava/io/ByteArrayOutputStream;-><init>()V

    new-instance v8, Ljava/io/FileInputStream;

    invoke-direct {v8, v4}, Ljava/io/FileInputStream;-><init>(Ljava/io/File;)V
    :try_end_42
    .catch Ljava/lang/Exception; {:try_start_2d .. :try_end_42} :catch_1af

    const/16 v9, 0x1000

    :try_start_44
    new-array v9, v9, [B

    :goto_46
    invoke-virtual {v8, v9}, Ljava/io/InputStream;->read([B)I

    move-result v10

    const/4 v11, -0x1

    if-eq v10, v11, :cond_52

    const/4 v11, 0x0

    invoke-virtual {v5, v9, v11, v10}, Ljava/io/ByteArrayOutputStream;->write([BII)V
    :try_end_51
    .catchall {:try_start_44 .. :try_end_51} :catchall_5f

    goto :goto_46

    :cond_52
    :try_start_52
    invoke-virtual {v8}, Ljava/io/InputStream;->close()V

    new-instance v8, Ljava/lang/String;

    invoke-virtual {v5}, Ljava/io/ByteArrayOutputStream;->toByteArray()[B

    move-result-object v5

    invoke-direct {v8, v5, v7}, Ljava/lang/String;-><init>([BLjava/lang/String;)V
    :try_end_5e
    .catch Ljava/lang/Exception; {:try_start_52 .. :try_end_5e} :catch_1af

    goto :goto_72

    :catchall_5f
    move-exception v0

    :try_start_60
    invoke-virtual {v8}, Ljava/io/InputStream;->close()V
    :try_end_63
    .catchall {:try_start_60 .. :try_end_63} :catchall_64

    goto :goto_68

    :catchall_64
    move-exception v3

    :try_start_65
    invoke-virtual {v0, v3}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V

    :goto_68
    throw v0

    :cond_69
    new-instance v0, Ljava/io/IOException;

    const-string v3, "Unexpected user.cfg size"

    invoke-direct {v0, v3}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    goto :goto_68

    :cond_71
    move-object v8, v6

    .line 18
    :goto_72
    const-string v5, "(?im)^\\s*r_vr_foveated\\s*=.*$"

    invoke-static {v5}, Ljava/util/regex/Pattern;->compile(Ljava/lang/String;)Ljava/util/regex/Pattern;

    move-result-object v5

    invoke-virtual {v5, v8}, Ljava/util/regex/Pattern;->matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;

    move-result-object v5

    const-string v9, "r_vr_foveated = 0"

    invoke-virtual {v5, v9}, Ljava/util/regex/Matcher;->replaceAll(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v5

    .line 19
    invoke-virtual {v5, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v9

    if-eqz v9, :cond_bc

    const-string v9, "(?im)^\\s*r_vr_foveated\\s*="

    invoke-static {v9}, Ljava/util/regex/Pattern;->compile(Ljava/lang/String;)Ljava/util/regex/Pattern;

    move-result-object v9

    invoke-virtual {v9, v8}, Ljava/util/regex/Pattern;->matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;

    move-result-object v9

    invoke-virtual {v9}, Ljava/util/regex/Matcher;->find()Z

    move-result v9

    if-nez v9, :cond_bc

    new-instance v5, Ljava/lang/StringBuilder;

    invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v5, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v8, v0}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v9

    if-nez v9, :cond_ad

    invoke-virtual {v8}, Ljava/lang/String;->isEmpty()Z

    move-result v9

    if-eqz v9, :cond_ae

    :cond_ad
    move-object v0, v6

    :cond_ae
    invoke-virtual {v5, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    const-string v5, "r_vr_foveated = 0\n"

    invoke-virtual {v0, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    .line 20
    :cond_bc
    invoke-virtual {v5, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_1a0

    .line 21
    new-instance v0, Ljava/io/File;

    const-string v6, "user.cfg.pfdmr-before"

    invoke-direct {v0, v3, v6}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    invoke-virtual {v4}, Ljava/io/File;->exists()Z

    move-result v6

    if-eqz v6, :cond_f6

    invoke-virtual {v0}, Ljava/io/File;->exists()Z

    move-result v6

    if-nez v6, :cond_f6

    new-instance v6, Ljava/io/FileOutputStream;

    invoke-direct {v6, v0}, Ljava/io/FileOutputStream;-><init>(Ljava/io/File;)V
    :try_end_da
    .catch Ljava/lang/Exception; {:try_start_65 .. :try_end_da} :catch_1af

    :try_start_da
    invoke-virtual {v8, v7}, Ljava/lang/String;->getBytes(Ljava/lang/String;)[B

    move-result-object v0

    invoke-virtual {v6, v0}, Ljava/io/FileOutputStream;->write([B)V

    invoke-virtual {v6}, Ljava/io/FileOutputStream;->getFD()Ljava/io/FileDescriptor;

    move-result-object v0

    invoke-virtual {v0}, Ljava/io/FileDescriptor;->sync()V
    :try_end_e8
    .catchall {:try_start_da .. :try_end_e8} :catchall_ec

    :try_start_e8
    invoke-virtual {v6}, Ljava/io/FileOutputStream;->close()V
    :try_end_eb
    .catch Ljava/lang/Exception; {:try_start_e8 .. :try_end_eb} :catch_1af

    goto :goto_f6

    :catchall_ec
    move-exception v0

    :try_start_ed
    invoke-virtual {v6}, Ljava/io/FileOutputStream;->close()V
    :try_end_f0
    .catchall {:try_start_ed .. :try_end_f0} :catchall_f1

    goto :goto_f5

    :catchall_f1
    move-exception v3

    :try_start_f2
    invoke-virtual {v0, v3}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V

    :goto_f5
    throw v0

    .line 22
    :cond_f6
    :goto_f6
    new-instance v0, Ljava/io/File;

    const-string v6, "user.cfg.pfdmr-tmp"

    invoke-direct {v0, v3, v6}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    new-instance v3, Ljava/io/FileOutputStream;

    invoke-direct {v3, v0}, Ljava/io/FileOutputStream;-><init>(Ljava/io/File;)V
    :try_end_102
    .catch Ljava/lang/Exception; {:try_start_f2 .. :try_end_102} :catch_1af

    :try_start_102
    invoke-virtual {v5, v7}, Ljava/lang/String;->getBytes(Ljava/lang/String;)[B

    move-result-object v5

    invoke-virtual {v3, v5}, Ljava/io/FileOutputStream;->write([B)V

    invoke-virtual {v3}, Ljava/io/FileOutputStream;->getFD()Ljava/io/FileDescriptor;

    move-result-object v5

    invoke-virtual {v5}, Ljava/io/FileDescriptor;->sync()V
    :try_end_110
    .catchall {:try_start_102 .. :try_end_110} :catchall_120

    :try_start_110
    invoke-virtual {v3}, Ljava/io/FileOutputStream;->close()V

    invoke-virtual {v0}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v4}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v3

    invoke-static {v0, v3}, Landroid/system/Os;->rename(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_11e
    .catch Ljava/lang/Exception; {:try_start_110 .. :try_end_11e} :catch_1af

    goto/16 :goto_1a0

    :catchall_120
    move-exception v0

    :try_start_121
    invoke-virtual {v3}, Ljava/io/FileOutputStream;->close()V
    :try_end_124
    .catchall {:try_start_121 .. :try_end_124} :catchall_125

    goto :goto_129

    :catchall_125
    move-exception v3

    :try_start_126
    invoke-virtual {v0, v3}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V

    :goto_129
    throw v0

    .line 24
    :cond_12a
    const-string v0, "unity.SUPERHOT_Team.SUPERHOT_VR_QA"

    invoke-virtual {v4, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_1a0

    .line 25
    new-instance v0, Ljava/io/File;

    const-string v4, "cloud"

    invoke-direct {v0, v3, v4}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    new-instance v3, Ljava/io/File;

    const-string v4, "data"

    invoke-direct {v3, v0, v4}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    .line 26
    invoke-virtual {v0}, Ljava/io/File;->exists()Z

    move-result v4

    if-nez v4, :cond_155

    invoke-virtual {v0}, Ljava/io/File;->mkdir()Z

    move-result v4

    if-eqz v4, :cond_14d

    goto :goto_155

    :cond_14d
    new-instance v0, Ljava/io/IOException;

    const-string v3, "Cannot create cloud"

    invoke-direct {v0, v3}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    throw v0

    :cond_155
    :goto_155
    invoke-virtual {v0}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v0

    const/16 v4, 0x1f8

    invoke-static {v0, v4}, Landroid/system/Os;->chmod(Ljava/lang/String;I)V

    .line 27
    invoke-virtual {v3}, Ljava/io/File;->exists()Z

    move-result v0

    if-nez v0, :cond_173

    invoke-virtual {v3}, Ljava/io/File;->mkdir()Z

    move-result v0

    if-eqz v0, :cond_16b

    goto :goto_173

    :cond_16b
    new-instance v0, Ljava/io/IOException;

    const-string v3, "Cannot create data"

    invoke-direct {v0, v3}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    throw v0

    :cond_173
    :goto_173
    invoke-virtual {v3}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v0

    invoke-static {v0, v4}, Landroid/system/Os;->chmod(Ljava/lang/String;I)V

    .line 28
    const-string v0, "pfdmr-probe-"

    const-string v4, ".tmp"

    invoke-static {v0, v4, v3}, Ljava/io/File;->createTempFile(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;)Ljava/io/File;

    move-result-object v0
    :try_end_182
    .catch Ljava/lang/Exception; {:try_start_126 .. :try_end_182} :catch_1af

    :try_start_182
    new-instance v3, Ljava/io/FileOutputStream;

    invoke-direct {v3, v0}, Ljava/io/FileOutputStream;-><init>(Ljava/io/File;)V
    :try_end_187
    .catchall {:try_start_182 .. :try_end_187} :catchall_19b

    :try_start_187
    invoke-virtual {v3, v2}, Ljava/io/FileOutputStream;->write(I)V
    :try_end_18a
    .catchall {:try_start_187 .. :try_end_18a} :catchall_191

    :try_start_18a
    invoke-virtual {v3}, Ljava/io/FileOutputStream;->close()V
    :try_end_18d
    .catchall {:try_start_18a .. :try_end_18d} :catchall_19b

    :try_start_18d
    invoke-virtual {v0}, Ljava/io/File;->delete()Z
    :try_end_190
    .catch Ljava/lang/Exception; {:try_start_18d .. :try_end_190} :catch_1af

    goto :goto_1a1

    :catchall_191
    move-exception v4

    :try_start_192
    invoke-virtual {v3}, Ljava/io/FileOutputStream;->close()V
    :try_end_195
    .catchall {:try_start_192 .. :try_end_195} :catchall_196

    goto :goto_19a

    :catchall_196
    move-exception v3

    :try_start_197
    invoke-virtual {v4, v3}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V

    :goto_19a
    throw v4
    :try_end_19b
    .catchall {:try_start_197 .. :try_end_19b} :catchall_19b

    :catchall_19b
    move-exception v3

    :try_start_19c
    invoke-virtual {v0}, Ljava/io/File;->delete()Z

    throw v3

    .line 24
    :cond_1a0
    :goto_1a0
    nop

    .line 30
    :goto_1a1
    const-string v0, "App-local preparation complete"

    invoke-static {v1, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 31
    goto :goto_1b5

    .line 14
    :cond_1a7
    new-instance v0, Ljava/io/IOException;

    const-string v3, "External files unavailable"

    invoke-direct {v0, v3}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    throw v0
    :try_end_1af
    .catch Ljava/lang/Exception; {:try_start_19c .. :try_end_1af} :catch_1af

    .line 31
    :catch_1af
    move-exception v0

    const-string v3, "App-local preparation failed"

    invoke-static {v1, v3, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    .line 32
    :goto_1b5
    return v2
.end method

.method public query(Landroid/net/Uri;[Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Landroid/database/Cursor;
    .registers 6

    .line 34
    const/4 p1, 0x0

    return-object p1
.end method

.method public update(Landroid/net/Uri;Landroid/content/ContentValues;Ljava/lang/String;[Ljava/lang/String;)I
    .registers 5

    .line 36
    const/4 p1, 0x0

    return p1
.end method
