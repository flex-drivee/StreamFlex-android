package com.horis.cncverse;

import com.horis.cncverse.entities.Source;
import com.lagradost.cloudstream3.utils.ExtractorApiKt;
import com.lagradost.cloudstream3.utils.ExtractorLink;
import java.util.Map;
import kotlin.Metadata;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.DebugMetadata;
import kotlin.coroutines.jvm.internal.SuspendLambda;
import kotlin.jvm.functions.Function2;
import kotlin.text.StringsKt;
/* JADX INFO: Access modifiers changed from: package-private */
/* compiled from: NetflixMirrorProvider.kt */
@Metadata(d1 = {"\u0000\n\n\u0000\n\u0002\u0010\u0002\n\u0002\u0018\u0002\u0010\u0000\u001a\u00020\u0001*\u00020\u0002H\n"}, d2 = {"<anonymous>", "", "Lcom/lagradost/cloudstream3/utils/ExtractorLink;"}, k = 3, mv = {2, 3, 0}, xi = 48)
@DebugMetadata(c = "com.horis.cncverse.NetflixMirrorProvider$loadLinks$3", f = "NetflixMirrorProvider.kt", i = {}, l = {}, m = "invokeSuspend", n = {}, nl = {}, s = {}, v = 2)
/* loaded from: /sdcard/AndroidIDEProjects/Cloudstream reference/cncverse/CNC Verse Mobile/resources/classes.dex */
public final class NetflixMirrorProvider$loadLinks$3 extends SuspendLambda implements Function2<ExtractorLink, Continuation<? super Unit>, Object> {
    final /* synthetic */ Map<String, String> $playlistHeaders;
    final /* synthetic */ Source $source;
    private /* synthetic */ Object L$0;
    int label;
    final /* synthetic */ NetflixMirrorProvider this$0;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public NetflixMirrorProvider$loadLinks$3(Map<String, String> map, NetflixMirrorProvider netflixMirrorProvider, Source source, Continuation<? super NetflixMirrorProvider$loadLinks$3> continuation) {
        super(2, continuation);
        this.$playlistHeaders = map;
        this.this$0 = netflixMirrorProvider;
        this.$source = source;
    }

    public final Continuation<Unit> create(Object obj, Continuation<?> continuation) {
        Continuation<Unit> netflixMirrorProvider$loadLinks$3 = new NetflixMirrorProvider$loadLinks$3(this.$playlistHeaders, this.this$0, this.$source, continuation);
        netflixMirrorProvider$loadLinks$3.L$0 = obj;
        return netflixMirrorProvider$loadLinks$3;
    }

    public final Object invoke(ExtractorLink extractorLink, Continuation<? super Unit> continuation) {
        return create(extractorLink, continuation).invokeSuspend(Unit.INSTANCE);
    }

    public final Object invokeSuspend(Object $result) {
        ExtractorLink $this$newExtractorLink = (ExtractorLink) this.L$0;
        IntrinsicsKt.getCOROUTINE_SUSPENDED();
        switch (this.label) {
            case 0:
                ResultKt.throwOnFailure($result);
                $this$newExtractorLink.setHeaders(this.$playlistHeaders);
                $this$newExtractorLink.setReferer(this.this$0.getMainUrl() + "/mobile/home?app=1");
                $this$newExtractorLink.setQuality(ExtractorApiKt.getQualityFromName(StringsKt.substringAfter(this.$source.getFile(), "q=", "")));
                return Unit.INSTANCE;
            default:
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
        }
    }
}
