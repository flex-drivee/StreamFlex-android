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
@DebugMetadata(c = "com.horis.cncverse.DisneyStudioProvider", f = "DisneyStudioProvider.kt", i = {0, 0, 0, 0, 0, 0}, l = {232}, m = "getEpisodes", n = {"title", "eid", "sid", "episodes", "page", "pg"}, nl = {237}, s = {"L$0", "L$1", "L$2", "L$3", "I$0", "I$1"}, v = 2)
/* loaded from: /sdcard/AndroidIDEProjects/Cloudstream reference/cncverse/CNC Verse Mobile/resources/classes.dex */
public final class DisneyStudioProvider$getEpisodes$1 extends ContinuationImpl {
    int I$0;
    int I$1;
    Object L$0;
    Object L$1;
    Object L$2;
    Object L$3;
    int label;
    /* synthetic */ Object result;
    final /* synthetic */ DisneyStudioProvider this$0;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public DisneyStudioProvider$getEpisodes$1(DisneyStudioProvider disneyStudioProvider, Continuation<? super DisneyStudioProvider$getEpisodes$1> continuation) {
        super(continuation);
        this.this$0 = disneyStudioProvider;
    }

    @Nullable
    public final Object invokeSuspend(@NotNull Object obj) {
        Object episodes;
        this.result = obj;
        this.label |= Integer.MIN_VALUE;
        episodes = this.this$0.getEpisodes(null, null, null, 0, (Continuation) this);
        return episodes;
    }
}
