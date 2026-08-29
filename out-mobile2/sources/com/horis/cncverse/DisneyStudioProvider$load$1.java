package com.horis.cncverse;

import kotlin.Metadata;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.jvm.internal.ContinuationImpl;
import kotlin.coroutines.jvm.internal.DebugMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
/* JADX INFO: Access modifiers changed from: package-private */
/* compiled from: DisneyStudioProvider.kt */
@Metadata(k = 3, mv = {2, 3, 0}, xi = 48)
@DebugMetadata(c = "com.horis.cncverse.DisneyStudioProvider", f = "DisneyStudioProvider.kt", i = {0, 0, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4}, l = {153, 155, 201, 204, 211}, m = "load$suspendImpl", n = {"$this", "url", "$this", "url", "id", "$this", "url", "id", "data", "episodes", "title", "castList", "cast", "genre", "rating", "suggest", "runTime", "$this", "url", "id", "data", "episodes", "title", "castList", "cast", "genre", "rating", "suggest", "runTime", "$this", "url", "id", "data", "episodes", "title", "castList", "cast", "genre", "rating", "suggest", "type", "runTime"}, nl = {154, 160, 204, 209, -1}, s = {"L$0", "L$1", "L$0", "L$1", "L$2", "L$0", "L$1", "L$2", "L$3", "L$4", "L$5", "L$6", "L$7", "L$8", "L$9", "L$10", "I$0", "L$0", "L$1", "L$2", "L$3", "L$4", "L$5", "L$6", "L$7", "L$8", "L$9", "L$10", "I$0", "L$0", "L$1", "L$2", "L$3", "L$4", "L$5", "L$6", "L$7", "L$8", "L$9", "L$10", "L$11", "I$0"}, v = 2)
/* loaded from: /sdcard/AndroidIDEProjects/Cloudstream reference/cncverse/CNC Verse Mobile/resources/classes.dex */
public final class DisneyStudioProvider$load$1 extends ContinuationImpl {
    int I$0;
    Object L$0;
    Object L$1;
    Object L$10;
    Object L$11;
    Object L$2;
    Object L$3;
    Object L$4;
    Object L$5;
    Object L$6;
    Object L$7;
    Object L$8;
    Object L$9;
    int label;
    /* synthetic */ Object result;
    final /* synthetic */ DisneyStudioProvider this$0;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public DisneyStudioProvider$load$1(DisneyStudioProvider disneyStudioProvider, Continuation<? super DisneyStudioProvider$load$1> continuation) {
        super(continuation);
        this.this$0 = disneyStudioProvider;
    }

    @Nullable
    public final Object invokeSuspend(@NotNull Object obj) {
        this.result = obj;
        this.label |= Integer.MIN_VALUE;
        return DisneyStudioProvider.load$suspendImpl(this.this$0, null, (Continuation) this);
    }
}
