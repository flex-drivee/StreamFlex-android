package com.horis.cncverse;

import kotlin.Metadata;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.jvm.internal.ContinuationImpl;
import kotlin.coroutines.jvm.internal.DebugMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
/* JADX INFO: Access modifiers changed from: package-private */
/* compiled from: NetflixMirrorProvider.kt */
@Metadata(k = 3, mv = {2, 3, 0}, xi = 48)
@DebugMetadata(c = "com.horis.cncverse.NetflixMirrorProvider", f = "NetflixMirrorProvider.kt", i = {0, 0, 1, 1, 1}, l = {94, 100}, m = "getMainPage", n = {"request", "page", "request", "cookies", "page"}, nl = {96, 105}, s = {"L$0", "I$0", "L$0", "L$1", "I$0"}, v = 2)
/* loaded from: /sdcard/AndroidIDEProjects/Cloudstream reference/cncverse/CNC Verse Mobile/resources/classes.dex */
public final class NetflixMirrorProvider$getMainPage$1 extends ContinuationImpl {
    int I$0;
    Object L$0;
    Object L$1;
    int label;
    /* synthetic */ Object result;
    final /* synthetic */ NetflixMirrorProvider this$0;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public NetflixMirrorProvider$getMainPage$1(NetflixMirrorProvider netflixMirrorProvider, Continuation<? super NetflixMirrorProvider$getMainPage$1> continuation) {
        super(continuation);
        this.this$0 = netflixMirrorProvider;
    }

    @Nullable
    public final Object invokeSuspend(@NotNull Object obj) {
        this.result = obj;
        this.label |= Integer.MIN_VALUE;
        return this.this$0.getMainPage(0, null, (Continuation) this);
    }
}
