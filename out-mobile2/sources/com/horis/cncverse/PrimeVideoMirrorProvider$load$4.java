package com.horis.cncverse;

import com.horis.cncverse.entities.Season;
import com.lagradost.cloudstream3.Episode;
import java.util.ArrayList;
import java.util.Collection;
import kotlin.Metadata;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.Boxing;
import kotlin.coroutines.jvm.internal.DebugMetadata;
import kotlin.coroutines.jvm.internal.SpillingKt;
import kotlin.coroutines.jvm.internal.SuspendLambda;
import kotlin.jvm.functions.Function2;
/* JADX INFO: Access modifiers changed from: package-private */
/* compiled from: PrimeVideoMirrorProvider.kt */
@Metadata(d1 = {"\u0000\f\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\n"}, d2 = {"<anonymous>", "", "it", "Lcom/horis/cncverse/entities/Season;"}, k = 3, mv = {2, 3, 0}, xi = 48)
@DebugMetadata(c = "com.horis.cncverse.PrimeVideoMirrorProvider$load$4", f = "PrimeVideoMirrorProvider.kt", i = {0}, l = {209}, m = "invokeSuspend", n = {"it"}, nl = {-1}, s = {"L$0"}, v = 2)
/* loaded from: /sdcard/AndroidIDEProjects/Cloudstream reference/cncverse/CNC Verse Mobile/resources/classes.dex */
public final class PrimeVideoMirrorProvider$load$4 extends SuspendLambda implements Function2<Season, Continuation<? super Boolean>, Object> {
    final /* synthetic */ ArrayList<Episode> $episodes;
    final /* synthetic */ String $title;
    final /* synthetic */ String $url;
    /* synthetic */ Object L$0;
    Object L$1;
    int label;
    final /* synthetic */ PrimeVideoMirrorProvider this$0;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public PrimeVideoMirrorProvider$load$4(ArrayList<Episode> arrayList, PrimeVideoMirrorProvider primeVideoMirrorProvider, String str, String str2, Continuation<? super PrimeVideoMirrorProvider$load$4> continuation) {
        super(2, continuation);
        this.$episodes = arrayList;
        this.this$0 = primeVideoMirrorProvider;
        this.$title = str;
        this.$url = str2;
    }

    public final Continuation<Unit> create(Object obj, Continuation<?> continuation) {
        Continuation<Unit> primeVideoMirrorProvider$load$4 = new PrimeVideoMirrorProvider$load$4(this.$episodes, this.this$0, this.$title, this.$url, continuation);
        primeVideoMirrorProvider$load$4.L$0 = obj;
        return primeVideoMirrorProvider$load$4;
    }

    public final Object invoke(Season season, Continuation<? super Boolean> continuation) {
        return create(season, continuation).invokeSuspend(Unit.INSTANCE);
    }

    public final Object invokeSuspend(Object $result) {
        Object episodes;
        ArrayList<Episode> arrayList;
        Season it = (Season) this.L$0;
        Object coroutine_suspended = IntrinsicsKt.getCOROUTINE_SUSPENDED();
        switch (this.label) {
            case 0:
                ResultKt.throwOnFailure($result);
                ArrayList<Episode> arrayList2 = this.$episodes;
                this.L$0 = SpillingKt.nullOutSpilledVariable(it);
                this.L$1 = arrayList2;
                this.label = 1;
                episodes = this.this$0.getEpisodes(this.$title, this.$url, it.getId(), 1, (Continuation) this);
                if (episodes == coroutine_suspended) {
                    return coroutine_suspended;
                }
                arrayList = arrayList2;
                break;
            case 1:
                arrayList = (ArrayList) this.L$1;
                ResultKt.throwOnFailure($result);
                episodes = $result;
                break;
            default:
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
        }
        return Boxing.boxBoolean(arrayList.addAll((Collection) episodes));
    }
}
