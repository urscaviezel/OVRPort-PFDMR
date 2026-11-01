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

.method private static climb(Landroid/content/Context;Ljava/io/File;)V
    .registers 12
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;,
            Landroid/system/ErrnoException;
        }
    .end annotation

    .line 23
    new-instance v0, Ljava/io/File;

    const-string v1, "user.cfg"

    invoke-direct {v0, p1, v1}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    new-instance v1, Ljava/io/File;

    const-string v2, "user.cfg.pfdmr-stability-before"

    invoke-direct {v1, p1, v2}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    invoke-static {v0}, Lmoe/crx/pfdmr/CommunityInit;->read(Ljava/io/File;)Ljava/lang/String;

    move-result-object v2

    const-string v3, "r_vr_foveated"

    const-string v4, "0"

    invoke-static {v2, v3, v4}, Lmoe/crx/pfdmr/CommunityInit;->set(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    .line 24
    invoke-static {p0}, Lmoe/crx/pfdmr/CommunityInit;->stability(Landroid/content/Context;)Z

    move-result p0

    const-string v5, "r_MultiThreaded"

    const/4 v6, 0x0

    if-eqz p0, :cond_39

    .line 25
    invoke-static {v3, v5, v4}, Lmoe/crx/pfdmr/CommunityInit;->set(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    invoke-virtual {p0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v4

    if-nez v4, :cond_36

    invoke-virtual {v1}, Ljava/io/File;->exists()Z

    move-result v4

    if-nez v4, :cond_36

    invoke-static {v1, v3}, Lmoe/crx/pfdmr/CommunityInit;->write(Ljava/io/File;Ljava/lang/String;)V

    .line 26
    :cond_36
    move-object v3, p0

    goto/16 :goto_cb

    :cond_39
    invoke-virtual {v1}, Ljava/io/File;->exists()Z

    move-result p0

    if-eqz p0, :cond_cb

    .line 27
    invoke-static {v5}, Lmoe/crx/pfdmr/CommunityInit;->setting(Ljava/lang/String;)Ljava/util/regex/Pattern;

    move-result-object p0

    invoke-virtual {p0, v3}, Ljava/util/regex/Pattern;->matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;

    move-result-object p0

    const/4 v4, 0x1

    move v8, v4

    move v7, v6

    .line 28
    :goto_4a
    invoke-virtual {p0}, Ljava/util/regex/Matcher;->find()Z

    move-result v9

    if-eqz v9, :cond_65

    invoke-virtual {p0}, Ljava/util/regex/Matcher;->group()Ljava/lang/String;

    move-result-object v7

    invoke-virtual {v7}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v7

    const-string v9, "r_MultiThreaded = 0"

    invoke-virtual {v7, v9}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v7

    if-nez v7, :cond_63

    move v7, v4

    move v8, v6

    goto :goto_4a

    :cond_63
    move v7, v4

    goto :goto_4a

    .line 29
    :cond_65
    if-eqz v7, :cond_c4

    if-eqz v8, :cond_c4

    invoke-static {v1}, Lmoe/crx/pfdmr/CommunityInit;->read(Ljava/io/File;)Ljava/lang/String;

    move-result-object p0

    invoke-static {v5}, Lmoe/crx/pfdmr/CommunityInit;->setting(Ljava/lang/String;)Ljava/util/regex/Pattern;

    move-result-object v6

    invoke-virtual {v6, p0}, Ljava/util/regex/Pattern;->matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;

    move-result-object p0

    new-instance v6, Ljava/lang/StringBuilder;

    invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V

    :goto_7a
    invoke-virtual {p0}, Ljava/util/regex/Matcher;->find()Z

    move-result v7

    if-eqz v7, :cond_8e

    invoke-virtual {p0}, Ljava/util/regex/Matcher;->group()Ljava/lang/String;

    move-result-object v7

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v7

    const/16 v8, 0xa

    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    goto :goto_7a

    :cond_8e
    invoke-static {v5}, Lmoe/crx/pfdmr/CommunityInit;->setting(Ljava/lang/String;)Ljava/util/regex/Pattern;

    move-result-object p0

    invoke-virtual {p0, v3}, Ljava/util/regex/Pattern;->matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;

    move-result-object p0

    const-string v3, ""

    invoke-virtual {p0, v3}, Ljava/util/regex/Matcher;->replaceAll(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->length()I

    move-result v5

    if-lez v5, :cond_c1

    new-instance v5, Ljava/lang/StringBuilder;

    invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v5, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    const-string v7, "\n"

    invoke-virtual {p0, v7}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result p0

    if-eqz p0, :cond_b4

    goto :goto_b5

    :cond_b4
    move-object v3, v7

    :goto_b5
    invoke-virtual {v5, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    :cond_c1
    move-object v3, p0

    move v6, v4

    goto :goto_cb

    .line 30
    :cond_c4
    const-string p0, "PFDMRCommunity"

    const-string v4, "Keeping externally changed r_MultiThreaded setting"

    invoke-static {p0, v4}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 32
    :cond_cb
    :goto_cb
    invoke-virtual {v3, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-nez p0, :cond_fc

    .line 33
    new-instance p0, Ljava/io/File;

    const-string v4, "user.cfg.pfdmr-before"

    invoke-direct {p0, p1, v4}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    invoke-virtual {v0}, Ljava/io/File;->exists()Z

    move-result v4

    if-eqz v4, :cond_e7

    invoke-virtual {p0}, Ljava/io/File;->exists()Z

    move-result v4

    if-nez v4, :cond_e7

    invoke-static {p0, v2}, Lmoe/crx/pfdmr/CommunityInit;->write(Ljava/io/File;Ljava/lang/String;)V

    .line 34
    :cond_e7
    new-instance p0, Ljava/io/File;

    const-string v2, "user.cfg.pfdmr-tmp"

    invoke-direct {p0, p1, v2}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    invoke-static {p0, v3}, Lmoe/crx/pfdmr/CommunityInit;->write(Ljava/io/File;Ljava/lang/String;)V

    invoke-virtual {p0}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v0}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object p1

    invoke-static {p0, p1}, Landroid/system/Os;->rename(Ljava/lang/String;Ljava/lang/String;)V

    .line 36
    :cond_fc
    if-eqz v6, :cond_10d

    invoke-virtual {v1}, Ljava/io/File;->delete()Z

    move-result p0

    if-eqz p0, :cond_105

    goto :goto_10d

    :cond_105
    new-instance p0, Ljava/io/IOException;

    const-string p1, "Cannot clear completed Climb stability backup"

    invoke-direct {p0, p1}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    throw p0

    .line 37
    :cond_10d
    :goto_10d
    return-void
.end method

.method private static read(Ljava/io/File;)Ljava/lang/String;
    .registers 2
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    .line 12
    invoke-virtual {p0}, Ljava/io/File;->exists()Z

    move-result v0

    if-eqz v0, :cond_10

    new-instance v0, Ljava/io/FileInputStream;

    invoke-direct {v0, p0}, Ljava/io/FileInputStream;-><init>(Ljava/io/File;)V

    invoke-static {v0}, Lmoe/crx/pfdmr/CommunityInit;->read(Ljava/io/InputStream;)Ljava/lang/String;

    move-result-object p0

    goto :goto_12

    :cond_10
    const-string p0, ""

    :goto_12
    return-object p0
.end method

.method private static read(Ljava/io/InputStream;)Ljava/lang/String;
    .registers 6
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    .line 11
    :try_start_0
    new-instance v0, Ljava/io/ByteArrayOutputStream;

    invoke-direct {v0}, Ljava/io/ByteArrayOutputStream;-><init>()V
    :try_end_5
    .catchall {:try_start_0 .. :try_end_5} :catchall_44

    const/16 v1, 0x1000

    :try_start_7
    new-array v1, v1, [B

    :goto_9
    invoke-virtual {p0, v1}, Ljava/io/InputStream;->read([B)I

    move-result v2

    const/4 v3, -0x1

    if-eq v2, v3, :cond_26

    invoke-virtual {v0}, Ljava/io/ByteArrayOutputStream;->size()I

    move-result v3

    add-int/2addr v3, v2

    const/high16 v4, 0x100000

    if-gt v3, v4, :cond_1e

    const/4 v3, 0x0

    invoke-virtual {v0, v1, v3, v2}, Ljava/io/ByteArrayOutputStream;->write([BII)V

    goto :goto_9

    :cond_1e
    new-instance v1, Ljava/io/IOException;

    const-string v2, "Unexpected configuration size"

    invoke-direct {v1, v2}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    throw v1

    :cond_26
    new-instance v1, Ljava/lang/String;

    invoke-virtual {v0}, Ljava/io/ByteArrayOutputStream;->toByteArray()[B

    move-result-object v2

    const-string v3, "UTF-8"

    invoke-direct {v1, v2, v3}, Ljava/lang/String;-><init>([BLjava/lang/String;)V
    :try_end_31
    .catchall {:try_start_7 .. :try_end_31} :catchall_3a

    :try_start_31
    invoke-virtual {v0}, Ljava/io/ByteArrayOutputStream;->close()V
    :try_end_34
    .catchall {:try_start_31 .. :try_end_34} :catchall_44

    if-eqz p0, :cond_39

    invoke-virtual {p0}, Ljava/io/InputStream;->close()V

    :cond_39
    return-object v1

    :catchall_3a
    move-exception v1

    :try_start_3b
    invoke-virtual {v0}, Ljava/io/ByteArrayOutputStream;->close()V
    :try_end_3e
    .catchall {:try_start_3b .. :try_end_3e} :catchall_3f

    goto :goto_43

    :catchall_3f
    move-exception v0

    :try_start_40
    invoke-virtual {v1, v0}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V

    :goto_43
    throw v1
    :try_end_44
    .catchall {:try_start_40 .. :try_end_44} :catchall_44

    :catchall_44
    move-exception v0

    if-eqz p0, :cond_4f

    :try_start_47
    invoke-virtual {p0}, Ljava/io/InputStream;->close()V
    :try_end_4a
    .catchall {:try_start_47 .. :try_end_4a} :catchall_4b

    goto :goto_4f

    :catchall_4b
    move-exception p0

    invoke-virtual {v0, p0}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V

    :cond_4f
    :goto_4f
    throw v0
.end method

.method private static set(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .registers 7

    .line 15
    invoke-static {p1}, Lmoe/crx/pfdmr/CommunityInit;->setting(Ljava/lang/String;)Ljava/util/regex/Pattern;

    move-result-object v0

    invoke-virtual {v0, p0}, Ljava/util/regex/Pattern;->matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;

    move-result-object v0

    invoke-virtual {v0}, Ljava/util/regex/Matcher;->find()Z

    move-result v1

    const-string v2, " = "

    if-eqz v1, :cond_2e

    new-instance p0, Ljava/lang/StringBuilder;

    invoke-direct {p0}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {p0}, Ljava/util/regex/Matcher;->quoteReplacement(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v0, p0}, Ljava/util/regex/Matcher;->replaceAll(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    return-object p0

    :cond_2e
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {p0}, Ljava/lang/String;->isEmpty()Z

    move-result v1

    const-string v3, "\n"

    if-nez v1, :cond_48

    invoke-virtual {p0, v3}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result p0

    if-eqz p0, :cond_46

    goto :goto_48

    :cond_46
    move-object p0, v3

    goto :goto_4a

    :cond_48
    :goto_48
    const-string p0, ""

    :goto_4a
    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    return-object p0
.end method

.method private static setting(Ljava/lang/String;)Ljava/util/regex/Pattern;
    .registers 3

    .line 14
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "(?im)^[\\t ]*"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-static {p0}, Ljava/util/regex/Pattern;->quote(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    const-string v0, "[\\t ]*=[^\\r\\n]*\\r?$"

    invoke-virtual {p0, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {p0}, Ljava/util/regex/Pattern;->compile(Ljava/lang/String;)Ljava/util/regex/Pattern;

    move-result-object p0

    return-object p0
.end method

.method private static stability(Landroid/content/Context;)Z
    .registers 3
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    .line 17
    const/4 v0, 0x0

    :try_start_1
    invoke-virtual {p0}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;

    move-result-object p0

    const-string v1, "pfdmr-climb-stability.txt"

    invoke-virtual {p0, v1}, Landroid/content/res/AssetManager;->open(Ljava/lang/String;)Ljava/io/InputStream;

    move-result-object p0

    invoke-static {p0}, Lmoe/crx/pfdmr/CommunityInit;->read(Ljava/io/InputStream;)Ljava/lang/String;

    move-result-object p0
    :try_end_f
    .catch Ljava/io/FileNotFoundException; {:try_start_1 .. :try_end_f} :catch_2a

    .line 18
    const-string v1, "PFDMR_CLIMB_STABILITY_V1\nenabled=true\n"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_19

    const/4 p0, 0x1

    return p0

    .line 19
    :cond_19
    const-string v1, "PFDMR_CLIMB_STABILITY_V1\nenabled=false\n"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-eqz p0, :cond_22

    return v0

    .line 20
    :cond_22
    new-instance p0, Ljava/io/IOException;

    const-string v0, "Unknown Climb stability command"

    invoke-direct {p0, v0}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    throw p0

    .line 17
    :catch_2a
    move-exception p0

    return v0
.end method

.method private static write(Ljava/io/File;Ljava/lang/String;)V
    .registers 3
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    .line 13
    new-instance v0, Ljava/io/FileOutputStream;

    invoke-direct {v0, p0}, Ljava/io/FileOutputStream;-><init>(Ljava/io/File;)V

    :try_start_5
    const-string p0, "UTF-8"

    invoke-virtual {p1, p0}, Ljava/lang/String;->getBytes(Ljava/lang/String;)[B

    move-result-object p0

    invoke-virtual {v0, p0}, Ljava/io/FileOutputStream;->write([B)V

    invoke-virtual {v0}, Ljava/io/FileOutputStream;->getFD()Ljava/io/FileDescriptor;

    move-result-object p0

    invoke-virtual {p0}, Ljava/io/FileDescriptor;->sync()V
    :try_end_15
    .catchall {:try_start_5 .. :try_end_15} :catchall_19

    invoke-virtual {v0}, Ljava/io/FileOutputStream;->close()V

    return-void

    :catchall_19
    move-exception p0

    :try_start_1a
    invoke-virtual {v0}, Ljava/io/FileOutputStream;->close()V
    :try_end_1d
    .catchall {:try_start_1a .. :try_end_1d} :catchall_1e

    goto :goto_22

    :catchall_1e
    move-exception p1

    invoke-virtual {p0, p1}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V

    :goto_22
    throw p0
.end method


# virtual methods
.method public delete(Landroid/net/Uri;Ljava/lang/String;[Ljava/lang/String;)I
    .registers 4

    .line 47
    const/4 p1, 0x0

    return p1
.end method

.method public getType(Landroid/net/Uri;)Ljava/lang/String;
    .registers 2

    .line 47
    const/4 p1, 0x0

    return-object p1
.end method

.method public insert(Landroid/net/Uri;Landroid/content/ContentValues;)Landroid/net/Uri;
    .registers 3

    .line 47
    const/4 p1, 0x0

    return-object p1
.end method

.method public onCreate()Z
    .registers 7

    .line 39
    const-string v0, "PFDMRCommunity"

    const/4 v1, 0x1

    :try_start_3
    invoke-virtual {p0}, Lmoe/crx/pfdmr/CommunityInit;->getContext()Landroid/content/Context;

    move-result-object v2

    invoke-virtual {v2}, Landroid/content/Context;->getPackageName()Ljava/lang/String;

    move-result-object v3

    const/4 v4, 0x0

    invoke-virtual {v2, v4}, Landroid/content/Context;->getExternalFilesDir(Ljava/lang/String;)Ljava/io/File;

    move-result-object v4

    if-eqz v4, :cond_9b

    .line 40
    const-string v5, "com.crytek.climb"

    invoke-virtual {v3, v5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_1f

    invoke-static {v2, v4}, Lmoe/crx/pfdmr/CommunityInit;->climb(Landroid/content/Context;Ljava/io/File;)V

    goto/16 :goto_95

    .line 41
    :cond_1f
    const-string v2, "unity.SUPERHOT_Team.SUPERHOT_VR_QA"

    invoke-virtual {v3, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_95

    .line 42
    new-instance v2, Ljava/io/File;

    const-string v3, "cloud"

    invoke-direct {v2, v4, v3}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    new-instance v3, Ljava/io/File;

    const-string v4, "data"

    invoke-direct {v3, v2, v4}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    invoke-virtual {v2}, Ljava/io/File;->exists()Z

    move-result v4

    if-nez v4, :cond_4a

    invoke-virtual {v2}, Ljava/io/File;->mkdir()Z

    move-result v4

    if-eqz v4, :cond_42

    goto :goto_4a

    :cond_42
    new-instance v2, Ljava/io/IOException;

    const-string v3, "Cannot create cloud"

    invoke-direct {v2, v3}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    :goto_49
    throw v2

    :cond_4a
    :goto_4a
    invoke-virtual {v2}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v2

    const/16 v4, 0x1f8

    invoke-static {v2, v4}, Landroid/system/Os;->chmod(Ljava/lang/String;I)V

    invoke-virtual {v3}, Ljava/io/File;->exists()Z

    move-result v2

    if-nez v2, :cond_68

    invoke-virtual {v3}, Ljava/io/File;->mkdir()Z

    move-result v2

    if-eqz v2, :cond_60

    goto :goto_68

    :cond_60
    new-instance v2, Ljava/io/IOException;

    const-string v3, "Cannot create data"

    invoke-direct {v2, v3}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    goto :goto_49

    :cond_68
    :goto_68
    invoke-virtual {v3}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v2

    invoke-static {v2, v4}, Landroid/system/Os;->chmod(Ljava/lang/String;I)V

    .line 43
    const-string v2, "pfdmr-probe-"

    const-string v4, ".tmp"

    invoke-static {v2, v4, v3}, Ljava/io/File;->createTempFile(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;)Ljava/io/File;

    move-result-object v2
    :try_end_77
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_77} :catch_a3

    :try_start_77
    new-instance v3, Ljava/io/FileOutputStream;

    invoke-direct {v3, v2}, Ljava/io/FileOutputStream;-><init>(Ljava/io/File;)V
    :try_end_7c
    .catchall {:try_start_77 .. :try_end_7c} :catchall_90

    :try_start_7c
    invoke-virtual {v3, v1}, Ljava/io/FileOutputStream;->write(I)V
    :try_end_7f
    .catchall {:try_start_7c .. :try_end_7f} :catchall_86

    :try_start_7f
    invoke-virtual {v3}, Ljava/io/FileOutputStream;->close()V
    :try_end_82
    .catchall {:try_start_7f .. :try_end_82} :catchall_90

    :try_start_82
    invoke-virtual {v2}, Ljava/io/File;->delete()Z
    :try_end_85
    .catch Ljava/lang/Exception; {:try_start_82 .. :try_end_85} :catch_a3

    goto :goto_95

    :catchall_86
    move-exception v4

    :try_start_87
    invoke-virtual {v3}, Ljava/io/FileOutputStream;->close()V
    :try_end_8a
    .catchall {:try_start_87 .. :try_end_8a} :catchall_8b

    goto :goto_8f

    :catchall_8b
    move-exception v3

    :try_start_8c
    invoke-virtual {v4, v3}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V

    :goto_8f
    throw v4
    :try_end_90
    .catchall {:try_start_8c .. :try_end_90} :catchall_90

    :catchall_90
    move-exception v3

    :try_start_91
    invoke-virtual {v2}, Ljava/io/File;->delete()Z

    throw v3

    .line 45
    :cond_95
    :goto_95
    const-string v2, "App-local preparation complete"

    invoke-static {v0, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 46
    goto :goto_a9

    .line 39
    :cond_9b
    new-instance v2, Ljava/io/IOException;

    const-string v3, "External files unavailable"

    invoke-direct {v2, v3}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    throw v2
    :try_end_a3
    .catch Ljava/lang/Exception; {:try_start_91 .. :try_end_a3} :catch_a3

    .line 46
    :catch_a3
    move-exception v2

    const-string v3, "App-local preparation failed"

    invoke-static {v0, v3, v2}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    :goto_a9
    return v1
.end method

.method public query(Landroid/net/Uri;[Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Landroid/database/Cursor;
    .registers 6

    .line 47
    const/4 p1, 0x0

    return-object p1
.end method

.method public update(Landroid/net/Uri;Landroid/content/ContentValues;Ljava/lang/String;[Ljava/lang/String;)I
    .registers 5

    .line 47
    const/4 p1, 0x0

    return p1
.end method
