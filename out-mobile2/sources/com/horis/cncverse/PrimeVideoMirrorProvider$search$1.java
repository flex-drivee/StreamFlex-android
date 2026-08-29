package com.horis.cncverse;

import kotlin.Metadata;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.jvm.internal.ContinuationImpl;
import kotlin.coroutines.jvm.internal.DebugMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
/* JADX INFO: Access modifiers changed from: package-private */
/* compiled from: PrimeVideoMirrorProvider.kt */
@Metadata(k = 3, mv = {2, 3, 0}, xi = 48)
@DebugMetadata(c = "com.horis.cncverse.PrimeVideoMirrorProvider", f = "PrimeVideoMirrorProvider.kt", i = {0, 1, 1, 1}, l = {133, 140}, m = "search", n = {"query", "query", "cookies", "url"}, nl = {135, 601}, s = {"L$0", "L$0", "L$1", "L$2"}, v = 2)
/* loaded from: /sdcard/AndroidIDEProjects/Cloudstream reference/cncverse/CNC Verse Mobile/resources/classes.dex */
public final class PrimeVideoMirrorProvider$search$1 extends ContinuationImpl {
    Object L$0;
    Object L$1;
    Object L$2;
    int label;
    /* synthetic */ Object result;
    final /* synthetic */ PrimeVideoMirrorProvider this$0;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public PrimeVideoMirrorProvider$search$1(PrimeVideoMirrorProvider primeVideoMirrorProvider, Continuation<? super PrimeVideoMirrorProvider$search$1> continuation) {
        super(continuation);
        this.this$0 = primeVideoMirrorProvider;
    }

    @Nullable
    public final Object invokeSuspend(@NotNull Object obj) {
        this.result = obj;
        this.label |= Integer.MIN_VALUE;
        return this.this$0.search(null, (Continuation) this);
    }
}
