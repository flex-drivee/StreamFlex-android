package com.horis.cncverse;

import kotlin.Metadata;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.jvm.internal.ContinuationImpl;
import kotlin.coroutines.jvm.internal.DebugMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
/* JADX INFO: Access modifiers changed from: package-private */
/* compiled from: Utils.kt */
@Metadata(k = 3, mv = {2, 3, 0}, xi = 48)
@DebugMetadata(c = "com.horis.cncverse.UtilsKt", f = "Utils.kt", i = {0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3}, l = {263, 273, 284, 285}, m = "bypass", n = {"mainUrl", "savedCookie", "savedTimestamp", "mainUrl", "savedCookie", "addhash", "savedTimestamp", "mainUrl", "savedCookie", "addhash", "verifyCheck", "verifyResponse", "requestBody", "savedTimestamp", "count", "mainUrl", "savedCookie", "addhash", "verifyCheck", "verifyResponse", "requestBody", "savedTimestamp", "count"}, nl = {269, 274, 285, 293}, s = {"L$0", "L$1", "J$0", "L$0", "L$1", "L$2", "J$0", "L$0", "L$1", "L$2", "L$3", "L$4", "L$5", "J$0", "I$0", "L$0", "L$1", "L$2", "L$3", "L$4", "L$5", "J$0", "I$0"}, v = 2)
/* loaded from: /sdcard/AndroidIDEProjects/Cloudstream reference/cncverse/CNC Verse Mobile/resources/classes.dex */
public final class UtilsKt$bypass$1 extends ContinuationImpl {
    int I$0;
    long J$0;
    Object L$0;
    Object L$1;
    Object L$2;
    Object L$3;
    Object L$4;
    Object L$5;
    int label;
    /* synthetic */ Object result;

    /* JADX INFO: Access modifiers changed from: package-private */
    public UtilsKt$bypass$1(Continuation<? super UtilsKt$bypass$1> continuation) {
        super(continuation);
    }

    @Nullable
    public final Object invokeSuspend(@NotNull Object obj) {
        this.result = obj;
        this.label |= Integer.MIN_VALUE;
        return UtilsKt.bypass(null, (Continuation) this);
    }
}
