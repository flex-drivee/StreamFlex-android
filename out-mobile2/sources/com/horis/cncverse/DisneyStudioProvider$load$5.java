package com.horis.cncverse;

import com.horis.cncverse.entities.PostData;
import com.lagradost.cloudstream3.ActorData;
import com.lagradost.cloudstream3.AnimeSearchResponse;
import com.lagradost.cloudstream3.Score;
import com.lagradost.cloudstream3.TvSeriesLoadResponse;
import java.util.List;
import kotlin.Metadata;
import kotlin.ResultKt;
import kotlin.TuplesKt;
import kotlin.Unit;
import kotlin.collections.MapsKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.Boxing;
import kotlin.coroutines.jvm.internal.DebugMetadata;
import kotlin.coroutines.jvm.internal.SuspendLambda;
import kotlin.jvm.functions.Function2;
import kotlin.text.StringsKt;
/* JADX INFO: Access modifiers changed from: package-private */
/* compiled from: DisneyStudioProvider.kt */
@Metadata(d1 = {"\u0000\n\n\u0000\n\u0002\u0010\u0002\n\u0002\u0018\u0002\u0010\u0000\u001a\u00020\u0001*\u00020\u0002H\n"}, d2 = {"<anonymous>", "", "Lcom/lagradost/cloudstream3/TvSeriesLoadResponse;"}, k = 3, mv = {2, 3, 0}, xi = 48)
@DebugMetadata(c = "com.horis.cncverse.DisneyStudioProvider$load$5", f = "DisneyStudioProvider.kt", i = {}, l = {}, m = "invokeSuspend", n = {}, nl = {}, s = {}, v = 2)
/* loaded from: /sdcard/AndroidIDEProjects/Cloudstream reference/cncverse/CNC Verse Mobile/resources/classes.dex */
public final class DisneyStudioProvider$load$5 extends SuspendLambda implements Function2<TvSeriesLoadResponse, Continuation<? super Unit>, Object> {
    final /* synthetic */ List<ActorData> $cast;
    final /* synthetic */ PostData $data;
    final /* synthetic */ List<String> $genre;
    final /* synthetic */ String $id;
    final /* synthetic */ String $rating;
    final /* synthetic */ int $runTime;
    final /* synthetic */ List<AnimeSearchResponse> $suggest;
    private /* synthetic */ Object L$0;
    int label;
    final /* synthetic */ DisneyStudioProvider this$0;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public DisneyStudioProvider$load$5(String str, DisneyStudioProvider disneyStudioProvider, PostData postData, List<String> list, List<ActorData> list2, String str2, int i, List<AnimeSearchResponse> list3, Continuation<? super DisneyStudioProvider$load$5> continuation) {
        super(2, continuation);
        this.$id = str;
        this.this$0 = disneyStudioProvider;
        this.$data = postData;
        this.$genre = list;
        this.$cast = list2;
        this.$rating = str2;
        this.$runTime = i;
        this.$suggest = list3;
    }

    public final Continuation<Unit> create(Object obj, Continuation<?> continuation) {
        Continuation<Unit> disneyStudioProvider$load$5 = new DisneyStudioProvider$load$5(this.$id, this.this$0, this.$data, this.$genre, this.$cast, this.$rating, this.$runTime, this.$suggest, continuation);
        disneyStudioProvider$load$5.L$0 = obj;
        return disneyStudioProvider$load$5;
    }

    public final Object invoke(TvSeriesLoadResponse tvSeriesLoadResponse, Continuation<? super Unit> continuation) {
        return create(tvSeriesLoadResponse, continuation).invokeSuspend(Unit.INSTANCE);
    }

    public final Object invokeSuspend(Object $result) {
        TvSeriesLoadResponse $this$newTvSeriesLoadResponse = (TvSeriesLoadResponse) this.L$0;
        IntrinsicsKt.getCOROUTINE_SUSPENDED();
        switch (this.label) {
            case 0:
                ResultKt.throwOnFailure($result);
                $this$newTvSeriesLoadResponse.setPosterUrl("https://imgcdn.kim/hs/v/" + this.$id + ".jpg");
                $this$newTvSeriesLoadResponse.setBackgroundPosterUrl("https://imgcdn.kim/hs/h/" + this.$id + ".jpg");
                $this$newTvSeriesLoadResponse.setPosterHeaders(MapsKt.mapOf(TuplesKt.to("Referer", this.this$0.getMainUrl() + "/home")));
                $this$newTvSeriesLoadResponse.setPlot(this.$data.getDesc());
                $this$newTvSeriesLoadResponse.setYear(StringsKt.toIntOrNull(this.$data.getYear()));
                $this$newTvSeriesLoadResponse.setTags(this.$genre);
                $this$newTvSeriesLoadResponse.setActors(this.$cast);
                $this$newTvSeriesLoadResponse.setScore(Score.Companion.from10(this.$rating));
                $this$newTvSeriesLoadResponse.setDuration(Boxing.boxInt(this.$runTime));
                $this$newTvSeriesLoadResponse.setContentRating(this.$data.getUa());
                $this$newTvSeriesLoadResponse.setRecommendations(this.$suggest);
                return Unit.INSTANCE;
            default:
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
        }
    }
}
