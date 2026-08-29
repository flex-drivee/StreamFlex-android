package com.horis.cncverse;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.horis.cncverse.NetflixMirrorProvider;
import com.horis.cncverse.entities.EpisodesData;
import com.horis.cncverse.entities.PlayList;
import com.horis.cncverse.entities.PlayListItem;
import com.horis.cncverse.entities.PostData;
import com.horis.cncverse.entities.SearchData;
import com.horis.cncverse.entities.SearchResult;
import com.horis.cncverse.entities.Season;
import com.horis.cncverse.entities.Source;
import com.horis.cncverse.entities.Suggest;
import com.horis.cncverse.entities.Tracks;
import com.lagradost.cloudstream3.APIHolder;
import com.lagradost.cloudstream3.Actor;
import com.lagradost.cloudstream3.ActorData;
import com.lagradost.cloudstream3.ActorRole;
import com.lagradost.cloudstream3.AnimeSearchResponse;
import com.lagradost.cloudstream3.Episode;
import com.lagradost.cloudstream3.HomePageList;
import com.lagradost.cloudstream3.HomePageResponse;
import com.lagradost.cloudstream3.LoadResponse;
import com.lagradost.cloudstream3.MainAPI;
import com.lagradost.cloudstream3.MainAPIKt;
import com.lagradost.cloudstream3.MainPageRequest;
import com.lagradost.cloudstream3.ParCollectionsKt;
import com.lagradost.cloudstream3.SearchResponse;
import com.lagradost.cloudstream3.SubtitleFile;
import com.lagradost.cloudstream3.TvType;
import com.lagradost.cloudstream3.ui.settings.Globals;
import com.lagradost.cloudstream3.utils.AppUtils;
import com.lagradost.cloudstream3.utils.ExtractorApiKt;
import com.lagradost.cloudstream3.utils.ExtractorLink;
import com.lagradost.cloudstream3.utils.ExtractorLinkType;
import com.lagradost.nicehttp.NiceResponse;
import com.lagradost.nicehttp.Requests;
import com.lagradost.nicehttp.ResponseParser;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import kotlin.Metadata;
import kotlin.Pair;
import kotlin.ResultKt;
import kotlin.TuplesKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;
import kotlin.collections.SetsKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.Boxing;
import kotlin.coroutines.jvm.internal.SpillingKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Reflection;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.text.Charsets;
import kotlin.text.StringsKt;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
/* compiled from: NetflixMirrorProvider.kt */
@Metadata(d1 = {"\u0000\u008c\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\"\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u000b\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010$\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u0000 E2\u00020\u0001:\u0003EFGB\u0007¢\u0006\u0004\b\u0002\u0010\u0003J \u0010\u001c\u001a\u0004\u0018\u00010\u001d2\u0006\u0010\u001e\u001a\u00020\u001f2\u0006\u0010 \u001a\u00020!H\u0096@¢\u0006\u0002\u0010\"J\f\u0010#\u001a\u00020$*\u00020%H\u0002J\u000e\u0010&\u001a\u0004\u0018\u00010'*\u00020%H\u0002J\u001c\u0010(\u001a\b\u0012\u0004\u0012\u00020'0)2\u0006\u0010*\u001a\u00020\nH\u0096@¢\u0006\u0002\u0010+J\u0018\u0010,\u001a\u0004\u0018\u00010-2\u0006\u0010.\u001a\u00020\nH\u0096@¢\u0006\u0002\u0010+J4\u0010/\u001a\b\u0012\u0004\u0012\u0002000)2\u0006\u00101\u001a\u00020\n2\u0006\u00102\u001a\u00020\n2\u0006\u00103\u001a\u00020\n2\u0006\u0010\u001e\u001a\u00020\u001fH\u0082@¢\u0006\u0002\u00104J\b\u00105\u001a\u000206H\u0002J\b\u00107\u001a\u000206H\u0002J\u0010\u00108\u001a\u0002062\u0006\u0010.\u001a\u00020\nH\u0002JF\u00109\u001a\u00020\u00162\u0006\u0010:\u001a\u00020\n2\u0006\u0010;\u001a\u00020\u00162\u0012\u0010<\u001a\u000e\u0012\u0004\u0012\u00020>\u0012\u0004\u0012\u0002060=2\u0012\u0010?\u001a\u000e\u0012\u0004\u0012\u00020@\u0012\u0004\u0012\u0002060=H\u0096@¢\u0006\u0002\u0010AJ\u0012\u0010B\u001a\u0004\u0018\u00010C2\u0006\u0010D\u001a\u00020@H\u0016R\u001a\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005X\u0096\u0004¢\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u001a\u0010\t\u001a\u00020\nX\u0096\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000eR\u001a\u0010\u000f\u001a\u00020\nX\u0096\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u0010\u0010\f\"\u0004\b\u0011\u0010\u000eR\u001a\u0010\u0012\u001a\u00020\nX\u0096\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u0013\u0010\f\"\u0004\b\u0014\u0010\u000eR\u0014\u0010\u0015\u001a\u00020\u0016X\u0096D¢\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u000e\u0010\u0019\u001a\u00020\nX\u0082\u000e¢\u0006\u0002\n\u0000R\u001a\u0010\u001a\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\n0\u001bX\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006H"}, d2 = {"Lcom/horis/cncverse/NetflixMirrorProvider;", "Lcom/lagradost/cloudstream3/MainAPI;", "<init>", "()V", "supportedTypes", "", "Lcom/lagradost/cloudstream3/TvType;", "getSupportedTypes", "()Ljava/util/Set;", "lang", "", "getLang", "()Ljava/lang/String;", "setLang", "(Ljava/lang/String;)V", "mainUrl", "getMainUrl", "setMainUrl", "name", "getName", "setName", "hasMainPage", "", "getHasMainPage", "()Z", "cookie_value", "headers", "", "getMainPage", "Lcom/lagradost/cloudstream3/HomePageResponse;", "page", "", "request", "Lcom/lagradost/cloudstream3/MainPageRequest;", "(ILcom/lagradost/cloudstream3/MainPageRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "toHomePageList", "Lcom/lagradost/cloudstream3/HomePageList;", "Lorg/jsoup/nodes/Element;", "toSearchResult", "Lcom/lagradost/cloudstream3/SearchResponse;", "search", "", "query", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "load", "Lcom/lagradost/cloudstream3/LoadResponse;", "url", "getEpisodes", "Lcom/lagradost/cloudstream3/Episode;", "title", "eid", "sid", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "showSubscriptionPopupIfNeeded", "", "showTelegramPopup", "openInExternalBrowser", "loadLinks", "data", "isCasting", "subtitleCallback", "Lkotlin/Function1;", "Lcom/lagradost/cloudstream3/SubtitleFile;", "callback", "Lcom/lagradost/cloudstream3/utils/ExtractorLink;", "(Ljava/lang/String;ZLkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getVideoInterceptor", "Lokhttp3/Interceptor;", "extractorLink", "Companion", "Id", "LoadData", "CNC Verse Mobile_debug"}, k = 1, mv = {2, 3, 0}, xi = 48)
@SourceDebugExtension({"SMAP\nNetflixMirrorProvider.kt\nKotlin\n*S Kotlin\n*F\n+ 1 NetflixMirrorProvider.kt\ncom/horis/cncverse/NetflixMirrorProvider\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n+ 4 NiceResponse.kt\ncom/lagradost/nicehttp/NiceResponse\n+ 5 Utils.kt\ncom/horis/cncverse/UtilsKt\n*L\n1#1,616:1\n1586#2:617\n1661#2,3:618\n1642#2,10:621\n1915#2:631\n1916#2:633\n1652#2:634\n1586#2:636\n1661#2,3:637\n1586#2:642\n1661#2,3:643\n1586#2:646\n1661#2,3:647\n1586#2:650\n1661#2,3:651\n777#2:654\n873#2,2:655\n1586#2:657\n1661#2,3:658\n1661#2,3:661\n1661#2,3:665\n777#2:670\n873#2,2:671\n1586#2:673\n1661#2,3:674\n1#3:632\n1#3:677\n62#4:635\n62#4:641\n62#4:664\n62#4:669\n218#5:640\n218#5:668\n*S KotlinDebug\n*F\n+ 1 NetflixMirrorProvider.kt\ncom/horis/cncverse/NetflixMirrorProvider\n*L\n106#1:617\n106#1:618,3\n114#1:621,10\n114#1:631\n114#1:633\n114#1:634\n142#1:636\n142#1:637,3\n168#1:642\n168#1:643,3\n169#1:646\n169#1:647,3\n175#1:650\n175#1:651,3\n176#1:654\n176#1:655,2\n181#1:657\n181#1:658,3\n193#1:661,3\n246#1:665,3\n576#1:670\n576#1:671,2\n576#1:673\n576#1:674,3\n114#1:632\n140#1:635\n163#1:641\n245#1:664\n558#1:669\n152#1:640\n531#1:668\n*E\n"})
/* loaded from: /sdcard/AndroidIDEProjects/Cloudstream reference/cncverse/CNC Verse Mobile/resources/classes.dex */
public final class NetflixMirrorProvider extends MainAPI {
    private static final long BROWSER_DEBOUNCE_MS = 10000;
    @NotNull
    public static final Companion Companion = new Companion(null);
    @NotNull
    private static final String OMG10 = "aHR0cHM6Ly9vbWcxMC5jb20vNC8xMTEwNDQ4OQ==";
    @Nullable
    private static Context context;
    private static volatile boolean csGuardWasEverActive;
    private static volatile long lastBrowserOpenMs;
    private static volatile boolean subscriptionPopupShown;
    private static volatile boolean telegramPopupShown;
    @NotNull
    private final Set<TvType> supportedTypes = SetsKt.setOf(new TvType[]{TvType.Movie, TvType.TvSeries, TvType.Anime, TvType.AsianDrama});
    @NotNull
    private String lang = "ta";
    @NotNull
    private String mainUrl = "https://net52.cc";
    @NotNull
    private String name = "NetflixM";
    private final boolean hasMainPage = true;
    @NotNull
    private String cookie_value = "";
    @NotNull
    private final Map<String, String> headers = MapsKt.mapOf(new Pair[]{TuplesKt.to("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"), TuplesKt.to("Accept-Language", "en-IN,en-US;q=0.9,en;q=0.8"), TuplesKt.to("Cache-Control", "max-age=0"), TuplesKt.to("Connection", "keep-alive"), TuplesKt.to("sec-ch-ua", "\"Not(A:Brand\";v=\"8\", \"Chromium\";v=\"144\", \"Android WebView\";v=\"144\""), TuplesKt.to("sec-ch-ua-mobile", "?0"), TuplesKt.to("sec-ch-ua-platform", "\"Android\""), TuplesKt.to("Sec-Fetch-Dest", "document"), TuplesKt.to("Sec-Fetch-Mode", "navigate"), TuplesKt.to("Sec-Fetch-Site", "same-origin"), TuplesKt.to("Sec-Fetch-User", "?1"), TuplesKt.to("Upgrade-Insecure-Requests", "1"), TuplesKt.to("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/144.0.7559.132 Safari/537.36 /OS.Gatu v3.0"), TuplesKt.to("X-Requested-With", "XMLHttpRequest")});

    /* compiled from: NetflixMirrorProvider.kt */
    @Metadata(d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J\u0006\u0010\u0004\u001a\u00020\u0005J\u0006\u0010\u0007\u001a\u00020\u0005J\u0012\u0010\b\u001a\u00020\t2\b\u0010\n\u001a\u0004\u0018\u00010\u000bH\u0002R\u000e\u0010\u0006\u001a\u00020\u0005X\u0082\u000e¢\u0006\u0002\n\u0000R\u001c\u0010\f\u001a\u0004\u0018\u00010\u000bX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\u000e\"\u0004\b\u000f\u0010\u0010R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082T¢\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0005X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0005X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0014X\u0082T¢\u0006\u0002\n\u0000¨\u0006\u0018"}, d2 = {"Lcom/horis/cncverse/NetflixMirrorProvider$Companion;", "", "<init>", "()V", "isCsGuardActive", "", "csGuardWasEverActive", "isCsGuardBlocked", "showCsGuardToast", "", "ctx", "Landroid/content/Context;", "context", "getContext", "()Landroid/content/Context;", "setContext", "(Landroid/content/Context;)V", "OMG10", "", "lastBrowserOpenMs", "", "telegramPopupShown", "subscriptionPopupShown", "BROWSER_DEBOUNCE_MS", "CNC Verse Mobile_debug"}, k = 1, mv = {2, 3, 0}, xi = 48)
    @SourceDebugExtension({"SMAP\nNetflixMirrorProvider.kt\nKotlin\n*S Kotlin\n*F\n+ 1 NetflixMirrorProvider.kt\ncom/horis/cncverse/NetflixMirrorProvider$Companion\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,616:1\n1#2:617\n*E\n"})
    /* loaded from: /sdcard/AndroidIDEProjects/Cloudstream reference/cncverse/CNC Verse Mobile/resources/classes.dex */
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        /* JADX WARN: Code restructure failed: missing block: B:10:0x0040, code lost:
            if (r5 == null) goto L19;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public final boolean isCsGuardActive() {
            String name;
            Class<?> cls;
            String name2;
            try {
                Class atClass = Class.forName("android.app.ActivityThread");
                Object thread = atClass.getMethod("currentActivityThread", new Class[0]).invoke(null, new Object[0]);
                Field field = atClass.getDeclaredField("mInstrumentation");
                field.setAccessible(true);
                Object obj = field.get(thread);
                if (obj != null && (cls = obj.getClass()) != null && (name2 = cls.getName()) != null) {
                    name = name2.toLowerCase(Locale.ROOT);
                    Intrinsics.checkNotNullExpressionValue(name, "toLowerCase(...)");
                }
                name = "";
                if (!StringsKt.contains$default(name, "guard", false, 2, (Object) null)) {
                    if (!StringsKt.contains$default(name, "csguard", false, 2, (Object) null)) {
                        return false;
                    }
                }
                return true;
            } catch (Throwable th) {
                return false;
            }
        }

        public final boolean isCsGuardBlocked() {
            if (isCsGuardActive()) {
                NetflixMirrorProvider.csGuardWasEverActive = true;
            }
            return NetflixMirrorProvider.csGuardWasEverActive;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public final void showCsGuardToast(final Context ctx) {
            if (ctx == null) {
                return;
            }
            new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.horis.cncverse.NetflixMirrorProvider$Companion$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    NetflixMirrorProvider.Companion.showCsGuardToast$lambda$0(ctx);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static final void showCsGuardToast$lambda$0(Context $c) {
            Toast.makeText($c, "🚫 CSGuard detected — Restart CloudStream after removing CSGuard to use CNCRepo", 1).show();
        }

        @Nullable
        public final Context getContext() {
            return NetflixMirrorProvider.context;
        }

        public final void setContext(@Nullable Context context) {
            NetflixMirrorProvider.context = context;
        }
    }

    @NotNull
    public Set<TvType> getSupportedTypes() {
        return this.supportedTypes;
    }

    @NotNull
    public String getLang() {
        return this.lang;
    }

    public void setLang(@NotNull String str) {
        this.lang = str;
    }

    @NotNull
    public String getMainUrl() {
        return this.mainUrl;
    }

    public void setMainUrl(@NotNull String str) {
        this.mainUrl = str;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public void setName(@NotNull String str) {
        this.name = str;
    }

    public boolean getHasMainPage() {
        return this.hasMainPage;
    }

    /* JADX WARN: Removed duplicated region for block: B:10:0x002c  */
    /* JADX WARN: Removed duplicated region for block: B:12:0x0038  */
    /* JADX WARN: Removed duplicated region for block: B:13:0x004b  */
    /* JADX WARN: Removed duplicated region for block: B:14:0x005d  */
    /* JADX WARN: Removed duplicated region for block: B:24:0x0141 A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:25:0x0142  */
    /* JADX WARN: Removed duplicated region for block: B:29:0x0170 A[LOOP:0: B:27:0x016a->B:29:0x0170, LOOP_END] */
    @Nullable
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public Object getMainPage(int page, @NotNull MainPageRequest request, @NotNull Continuation<? super HomePageResponse> continuation) {
        NetflixMirrorProvider$getMainPage$1 netflixMirrorProvider$getMainPage$1;
        Object obj;
        int page2;
        MainPageRequest request2;
        NetflixMirrorProvider netflixMirrorProvider;
        if (continuation instanceof NetflixMirrorProvider$getMainPage$1) {
            netflixMirrorProvider$getMainPage$1 = (NetflixMirrorProvider$getMainPage$1) continuation;
            if ((netflixMirrorProvider$getMainPage$1.label & Integer.MIN_VALUE) != 0) {
                netflixMirrorProvider$getMainPage$1.label -= Integer.MIN_VALUE;
                Object $result = netflixMirrorProvider$getMainPage$1.result;
                Object coroutine_suspended = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (netflixMirrorProvider$getMainPage$1.label) {
                    case 0:
                        ResultKt.throwOnFailure($result);
                        if (Companion.isCsGuardBlocked()) {
                            Companion.showCsGuardToast(context);
                            return MainAPIKt.newHomePageResponse$default(CollectionsKt.emptyList(), (Boolean) null, 2, (Object) null);
                        }
                        showTelegramPopup();
                        showSubscriptionPopupIfNeeded();
                        String mainUrl = getMainUrl();
                        netflixMirrorProvider$getMainPage$1.L$0 = SpillingKt.nullOutSpilledVariable(request);
                        netflixMirrorProvider$getMainPage$1.L$1 = this;
                        netflixMirrorProvider$getMainPage$1.I$0 = page;
                        netflixMirrorProvider$getMainPage$1.label = 1;
                        Object bypass = UtilsKt.bypass(mainUrl, netflixMirrorProvider$getMainPage$1);
                        if (bypass == coroutine_suspended) {
                            return coroutine_suspended;
                        }
                        obj = bypass;
                        page2 = page;
                        request2 = request;
                        netflixMirrorProvider = this;
                        netflixMirrorProvider.cookie_value = (String) obj;
                        Map cookies = MapsKt.mapOf(new Pair[]{TuplesKt.to("t_hash_t", this.cookie_value), TuplesKt.to("ott", "nf"), TuplesKt.to("hd", "on")});
                        netflixMirrorProvider$getMainPage$1.L$0 = SpillingKt.nullOutSpilledVariable(request2);
                        netflixMirrorProvider$getMainPage$1.L$1 = SpillingKt.nullOutSpilledVariable(cookies);
                        netflixMirrorProvider$getMainPage$1.I$0 = page2;
                        netflixMirrorProvider$getMainPage$1.label = 2;
                        $result = Requests.get$default(UtilsKt.getApp(), getMainUrl() + "/mobile/home?app=1", this.headers, getMainUrl() + "/mobile/home?app=1", (Map) null, cookies, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, netflixMirrorProvider$getMainPage$1, 4072, (Object) null);
                        if ($result != coroutine_suspended) {
                            return coroutine_suspended;
                        }
                        Document document = ((NiceResponse) $result).getDocument();
                        Iterable $this$map$iv = document.select(".tray-container, #top10");
                        Collection destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv, 10));
                        for (Object item$iv$iv : $this$map$iv) {
                            Element it = (Element) item$iv$iv;
                            destination$iv$iv.add(toHomePageList(it));
                        }
                        List items = (List) destination$iv$iv;
                        return MainAPIKt.newHomePageResponse(items, Boxing.boxBoolean(false));
                    case 1:
                        page2 = netflixMirrorProvider$getMainPage$1.I$0;
                        ResultKt.throwOnFailure($result);
                        request2 = (MainPageRequest) netflixMirrorProvider$getMainPage$1.L$0;
                        netflixMirrorProvider = (NetflixMirrorProvider) netflixMirrorProvider$getMainPage$1.L$1;
                        obj = $result;
                        netflixMirrorProvider.cookie_value = (String) obj;
                        Map cookies2 = MapsKt.mapOf(new Pair[]{TuplesKt.to("t_hash_t", this.cookie_value), TuplesKt.to("ott", "nf"), TuplesKt.to("hd", "on")});
                        netflixMirrorProvider$getMainPage$1.L$0 = SpillingKt.nullOutSpilledVariable(request2);
                        netflixMirrorProvider$getMainPage$1.L$1 = SpillingKt.nullOutSpilledVariable(cookies2);
                        netflixMirrorProvider$getMainPage$1.I$0 = page2;
                        netflixMirrorProvider$getMainPage$1.label = 2;
                        $result = Requests.get$default(UtilsKt.getApp(), getMainUrl() + "/mobile/home?app=1", this.headers, getMainUrl() + "/mobile/home?app=1", (Map) null, cookies2, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, netflixMirrorProvider$getMainPage$1, 4072, (Object) null);
                        if ($result != coroutine_suspended) {
                        }
                        break;
                    case 2:
                        int i = netflixMirrorProvider$getMainPage$1.I$0;
                        Map map = (Map) netflixMirrorProvider$getMainPage$1.L$1;
                        MainPageRequest mainPageRequest = (MainPageRequest) netflixMirrorProvider$getMainPage$1.L$0;
                        ResultKt.throwOnFailure($result);
                        Document document2 = ((NiceResponse) $result).getDocument();
                        Iterable $this$map$iv2 = document2.select(".tray-container, #top10");
                        Collection destination$iv$iv2 = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv2, 10));
                        while (r11.hasNext()) {
                        }
                        List items2 = (List) destination$iv$iv2;
                        return MainAPIKt.newHomePageResponse(items2, Boxing.boxBoolean(false));
                    default:
                        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }
            }
        }
        netflixMirrorProvider$getMainPage$1 = new NetflixMirrorProvider$getMainPage$1(this, continuation);
        Object $result2 = netflixMirrorProvider$getMainPage$1.result;
        Object coroutine_suspended2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
        switch (netflixMirrorProvider$getMainPage$1.label) {
        }
    }

    private final HomePageList toHomePageList(Element $this$toHomePageList) {
        String name = $this$toHomePageList.select("h2, span").text();
        Iterable $this$mapNotNull$iv = $this$toHomePageList.select("article, .top10-post");
        Collection destination$iv$iv = new ArrayList();
        for (Object element$iv$iv$iv : $this$mapNotNull$iv) {
            Element it = (Element) element$iv$iv$iv;
            SearchResponse searchResult = toSearchResult(it);
            if (searchResult != null) {
                destination$iv$iv.add(searchResult);
            }
        }
        List items = (List) destination$iv$iv;
        return new HomePageList(name, items, false);
    }

    private final SearchResponse toSearchResult(Element $this$toSearchResult) {
        final String id;
        Element selectFirst = $this$toSearchResult.selectFirst("a");
        if (selectFirst == null || (id = selectFirst.attr("data-post")) == null) {
            id = $this$toSearchResult.attr("data-post");
        }
        return MainAPIKt.newAnimeSearchResponse$default(this, "", AppUtils.INSTANCE.toJson(new Id(id)), (TvType) null, false, new Function1() { // from class: com.horis.cncverse.NetflixMirrorProvider$$ExternalSyntheticLambda9
            public final Object invoke(Object obj) {
                Unit searchResult$lambda$0;
                searchResult$lambda$0 = NetflixMirrorProvider.toSearchResult$lambda$0(id, this, (AnimeSearchResponse) obj);
                return searchResult$lambda$0;
            }
        }, 12, (Object) null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit toSearchResult$lambda$0(String $id, NetflixMirrorProvider this$0, AnimeSearchResponse $this$newAnimeSearchResponse) {
        $this$newAnimeSearchResponse.setPosterUrl("https://imgcdn.kim/poster/v/" + $id + ".jpg");
        $this$newAnimeSearchResponse.setPosterHeaders(MapsKt.mapOf(TuplesKt.to("Referer", this$0.getMainUrl() + "/home")));
        return Unit.INSTANCE;
    }

    /* JADX WARN: Removed duplicated region for block: B:10:0x0029  */
    /* JADX WARN: Removed duplicated region for block: B:12:0x0033  */
    /* JADX WARN: Removed duplicated region for block: B:13:0x0048  */
    /* JADX WARN: Removed duplicated region for block: B:14:0x0058  */
    /* JADX WARN: Removed duplicated region for block: B:24:0x013d A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:25:0x013e  */
    /* JADX WARN: Removed duplicated region for block: B:29:0x017c A[LOOP:0: B:27:0x0176->B:29:0x017c, LOOP_END] */
    @Nullable
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public Object search(@NotNull String query, @NotNull Continuation<? super List<? extends SearchResponse>> continuation) {
        NetflixMirrorProvider$search$1 netflixMirrorProvider$search$1;
        Object obj;
        String query2;
        NetflixMirrorProvider netflixMirrorProvider;
        if (continuation instanceof NetflixMirrorProvider$search$1) {
            netflixMirrorProvider$search$1 = (NetflixMirrorProvider$search$1) continuation;
            if ((netflixMirrorProvider$search$1.label & Integer.MIN_VALUE) != 0) {
                netflixMirrorProvider$search$1.label -= Integer.MIN_VALUE;
                Object $result = netflixMirrorProvider$search$1.result;
                Object coroutine_suspended = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (netflixMirrorProvider$search$1.label) {
                    case 0:
                        ResultKt.throwOnFailure($result);
                        if (Companion.isCsGuardBlocked()) {
                            Companion.showCsGuardToast(context);
                            return CollectionsKt.emptyList();
                        }
                        String mainUrl = getMainUrl();
                        netflixMirrorProvider$search$1.L$0 = query;
                        netflixMirrorProvider$search$1.L$1 = this;
                        netflixMirrorProvider$search$1.label = 1;
                        Object bypass = UtilsKt.bypass(mainUrl, netflixMirrorProvider$search$1);
                        if (bypass == coroutine_suspended) {
                            return coroutine_suspended;
                        }
                        obj = bypass;
                        query2 = query;
                        netflixMirrorProvider = this;
                        netflixMirrorProvider.cookie_value = (String) obj;
                        Map cookies = MapsKt.mapOf(new Pair[]{TuplesKt.to("t_hash_t", this.cookie_value), TuplesKt.to("hd", "on"), TuplesKt.to("ott", "nf")});
                        String url = getMainUrl() + "/mobile/search.php?s=" + query2 + "&t=" + APIHolder.INSTANCE.getUnixTime();
                        netflixMirrorProvider$search$1.L$0 = SpillingKt.nullOutSpilledVariable(query2);
                        netflixMirrorProvider$search$1.L$1 = SpillingKt.nullOutSpilledVariable(cookies);
                        netflixMirrorProvider$search$1.L$2 = SpillingKt.nullOutSpilledVariable(url);
                        netflixMirrorProvider$search$1.label = 2;
                        $result = Requests.get$default(UtilsKt.getApp(), url, (Map) null, getMainUrl() + "/home", (Map) null, cookies, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, netflixMirrorProvider$search$1, 4074, (Object) null);
                        if ($result != coroutine_suspended) {
                            return coroutine_suspended;
                        }
                        NiceResponse this_$iv = (NiceResponse) $result;
                        ResponseParser parser = this_$iv.getParser();
                        Intrinsics.checkNotNull(parser);
                        SearchData data = (SearchData) parser.parse(this_$iv.getText(), Reflection.getOrCreateKotlinClass(SearchData.class));
                        Iterable $this$map$iv = data.getSearchResult();
                        Collection destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv, 10));
                        for (Object item$iv$iv : $this$map$iv) {
                            final SearchResult it = (SearchResult) item$iv$iv;
                            destination$iv$iv.add(MainAPIKt.newAnimeSearchResponse$default(this, it.getT(), AppUtils.INSTANCE.toJson(new Id(it.getId())), (TvType) null, false, new Function1() { // from class: com.horis.cncverse.NetflixMirrorProvider$$ExternalSyntheticLambda11
                                public final Object invoke(Object obj2) {
                                    Unit search$lambda$0$0;
                                    search$lambda$0$0 = NetflixMirrorProvider.search$lambda$0$0(SearchResult.this, this, (AnimeSearchResponse) obj2);
                                    return search$lambda$0$0;
                                }
                            }, 12, (Object) null));
                            data = data;
                        }
                        return (List) destination$iv$iv;
                    case 1:
                        String query3 = (String) netflixMirrorProvider$search$1.L$0;
                        ResultKt.throwOnFailure($result);
                        netflixMirrorProvider = (NetflixMirrorProvider) netflixMirrorProvider$search$1.L$1;
                        query2 = query3;
                        obj = $result;
                        netflixMirrorProvider.cookie_value = (String) obj;
                        Map cookies2 = MapsKt.mapOf(new Pair[]{TuplesKt.to("t_hash_t", this.cookie_value), TuplesKt.to("hd", "on"), TuplesKt.to("ott", "nf")});
                        String url2 = getMainUrl() + "/mobile/search.php?s=" + query2 + "&t=" + APIHolder.INSTANCE.getUnixTime();
                        netflixMirrorProvider$search$1.L$0 = SpillingKt.nullOutSpilledVariable(query2);
                        netflixMirrorProvider$search$1.L$1 = SpillingKt.nullOutSpilledVariable(cookies2);
                        netflixMirrorProvider$search$1.L$2 = SpillingKt.nullOutSpilledVariable(url2);
                        netflixMirrorProvider$search$1.label = 2;
                        $result = Requests.get$default(UtilsKt.getApp(), url2, (Map) null, getMainUrl() + "/home", (Map) null, cookies2, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, netflixMirrorProvider$search$1, 4074, (Object) null);
                        if ($result != coroutine_suspended) {
                        }
                        break;
                    case 2:
                        String str = (String) netflixMirrorProvider$search$1.L$2;
                        Map map = (Map) netflixMirrorProvider$search$1.L$1;
                        String str2 = (String) netflixMirrorProvider$search$1.L$0;
                        ResultKt.throwOnFailure($result);
                        NiceResponse this_$iv2 = (NiceResponse) $result;
                        ResponseParser parser2 = this_$iv2.getParser();
                        Intrinsics.checkNotNull(parser2);
                        SearchData data2 = (SearchData) parser2.parse(this_$iv2.getText(), Reflection.getOrCreateKotlinClass(SearchData.class));
                        Iterable $this$map$iv2 = data2.getSearchResult();
                        Collection destination$iv$iv2 = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv2, 10));
                        while (r11.hasNext()) {
                        }
                        return (List) destination$iv$iv2;
                    default:
                        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }
            }
        }
        netflixMirrorProvider$search$1 = new NetflixMirrorProvider$search$1(this, continuation);
        Object $result2 = netflixMirrorProvider$search$1.result;
        Object coroutine_suspended2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
        switch (netflixMirrorProvider$search$1.label) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit search$lambda$0$0(SearchResult $it, NetflixMirrorProvider this$0, AnimeSearchResponse $this$newAnimeSearchResponse) {
        $this$newAnimeSearchResponse.setPosterUrl("https://imgcdn.kim/poster/v/" + $it.getId() + ".jpg");
        $this$newAnimeSearchResponse.setPosterHeaders(MapsKt.mapOf(TuplesKt.to("Referer", this$0.getMainUrl() + "/home")));
        return Unit.INSTANCE;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:102:0x0595  */
    /* JADX WARN: Removed duplicated region for block: B:103:0x0598  */
    /* JADX WARN: Removed duplicated region for block: B:106:0x0615 A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:107:0x0616  */
    /* JADX WARN: Removed duplicated region for block: B:10:0x002c  */
    /* JADX WARN: Removed duplicated region for block: B:12:0x0034  */
    /* JADX WARN: Removed duplicated region for block: B:13:0x0075  */
    /* JADX WARN: Removed duplicated region for block: B:14:0x00b4  */
    /* JADX WARN: Removed duplicated region for block: B:15:0x00f8  */
    /* JADX WARN: Removed duplicated region for block: B:16:0x010e  */
    /* JADX WARN: Removed duplicated region for block: B:17:0x011b  */
    /* JADX WARN: Removed duplicated region for block: B:23:0x01e9 A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:24:0x01ea  */
    /* JADX WARN: Removed duplicated region for block: B:38:0x0297 A[LOOP:1: B:36:0x0291->B:38:0x0297, LOOP_END] */
    /* JADX WARN: Removed duplicated region for block: B:61:0x0383  */
    /* JADX WARN: Removed duplicated region for block: B:62:0x0392  */
    /* JADX WARN: Removed duplicated region for block: B:65:0x03a6  */
    /* JADX WARN: Removed duplicated region for block: B:70:0x0419  */
    /* JADX WARN: Removed duplicated region for block: B:73:0x0429  */
    /* JADX WARN: Removed duplicated region for block: B:74:0x0450  */
    /* JADX WARN: Removed duplicated region for block: B:92:0x0524  */
    @Nullable
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public Object load(@NotNull String url, @NotNull Continuation<? super LoadResponse> continuation) {
        Continuation netflixMirrorProvider$load$1;
        Continuation $continuation;
        String url2;
        Object obj;
        NetflixMirrorProvider netflixMirrorProvider;
        Map cookies;
        Object obj2;
        Continuation $continuation2;
        String url3;
        String url4;
        final PostData data;
        String cast;
        Iterable emptyList;
        String genre;
        List list;
        String rating;
        Iterable suggest;
        Object obj3;
        List castList;
        List list2;
        List suggest2;
        String url5;
        Continuation $continuation3;
        Object obj4;
        int runTime;
        Object episodes;
        String url6;
        String id;
        String rating2;
        Map cookies2;
        int runTime2;
        PostData data2;
        ArrayList episodes2;
        List genre2;
        String title;
        List cast2;
        ArrayList arrayList;
        List cast3;
        String id2;
        String title2;
        String url7;
        ArrayList episodes3;
        List cast4;
        List genre3;
        String id3;
        ArrayList episodes4;
        String title3;
        String id4;
        List cast5;
        int runTime3;
        PostData data3;
        String title4;
        Iterable split$default;
        Iterable split$default2;
        List genre4;
        List<Season> season;
        List dropLast;
        Object amap;
        ArrayList episodes5;
        List suggest3;
        PostData data4;
        Map cookies3;
        String title5;
        int runTime4;
        String title6;
        List castList2;
        String rating3;
        List genre5;
        List cast6;
        if (continuation instanceof NetflixMirrorProvider$load$1) {
            netflixMirrorProvider$load$1 = (NetflixMirrorProvider$load$1) continuation;
            if ((netflixMirrorProvider$load$1.label & Integer.MIN_VALUE) != 0) {
                netflixMirrorProvider$load$1.label -= Integer.MIN_VALUE;
                $continuation = netflixMirrorProvider$load$1;
                Object $result = $continuation.result;
                Object coroutine_suspended = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch ($continuation.label) {
                    case 0:
                        ResultKt.throwOnFailure($result);
                        String mainUrl = getMainUrl();
                        url2 = url;
                        $continuation.L$0 = url2;
                        $continuation.L$1 = this;
                        $continuation.label = 1;
                        Object bypass = UtilsKt.bypass(mainUrl, $continuation);
                        if (bypass == coroutine_suspended) {
                            return coroutine_suspended;
                        }
                        obj = bypass;
                        netflixMirrorProvider = this;
                        netflixMirrorProvider.cookie_value = (String) obj;
                        String text$iv = url2;
                        String id5 = ((Id) UtilsKt.getJSONParser().parse(text$iv, Reflection.getOrCreateKotlinClass(Id.class))).getId();
                        cookies = MapsKt.mapOf(new Pair[]{TuplesKt.to("t_hash_t", this.cookie_value), TuplesKt.to("hd", "on"), TuplesKt.to("ott", "nf")});
                        $continuation.L$0 = url2;
                        $continuation.L$1 = id5;
                        $continuation.L$2 = SpillingKt.nullOutSpilledVariable(cookies);
                        $continuation.label = 2;
                        obj2 = Requests.get$default(UtilsKt.getApp(), getMainUrl() + "/mobile/post.php?id=" + id5 + "&t=" + APIHolder.INSTANCE.getUnixTime(), this.headers, getMainUrl() + "/home", (Map) null, cookies, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, $continuation, 4072, (Object) null);
                        $continuation2 = $continuation;
                        if (obj2 != coroutine_suspended) {
                            return coroutine_suspended;
                        }
                        String str = url2;
                        url3 = id5;
                        url4 = str;
                        NiceResponse this_$iv = (NiceResponse) obj2;
                        ResponseParser parser = this_$iv.getParser();
                        Intrinsics.checkNotNull(parser);
                        data = (PostData) parser.parse(this_$iv.getText(), Reflection.getOrCreateKotlinClass(PostData.class));
                        ArrayList episodes6 = new ArrayList();
                        String title7 = data.getTitle();
                        cast = data.getCast();
                        if (cast != null || (split$default2 = StringsKt.split$default(cast, new String[]{","}, false, 0, 6, (Object) null)) == null) {
                            emptyList = CollectionsKt.emptyList();
                        } else {
                            Iterable $this$map$iv = split$default2;
                            Collection destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv, 10));
                            for (Object item$iv$iv : $this$map$iv) {
                                destination$iv$iv.add(StringsKt.trim((String) item$iv$iv).toString());
                            }
                            emptyList = (List) destination$iv$iv;
                        }
                        List castList3 = emptyList;
                        List $this$map$iv2 = castList3;
                        int $i$f$map = CollectionsKt.collectionSizeOrDefault($this$map$iv2, 10);
                        Collection destination$iv$iv2 = new ArrayList($i$f$map);
                        Iterable $this$mapTo$iv$iv = $this$map$iv2;
                        for (Object item$iv$iv2 : $this$mapTo$iv$iv) {
                            destination$iv$iv2.add(new ActorData(new Actor((String) item$iv$iv2, (String) null, 2, (DefaultConstructorMarker) null), (ActorRole) null, (String) null, (Actor) null, 14, (DefaultConstructorMarker) null));
                            $this$map$iv2 = $this$map$iv2;
                            $result = $result;
                            $this$mapTo$iv$iv = $this$mapTo$iv$iv;
                        }
                        List cast7 = (List) destination$iv$iv2;
                        genre = data.getGenre();
                        if (genre != null || (split$default = StringsKt.split$default(genre, new String[]{","}, false, 0, 6, (Object) null)) == null) {
                            list = null;
                        } else {
                            Iterable $this$map$iv3 = split$default;
                            Collection destination$iv$iv3 = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv3, 10));
                            for (Object item$iv$iv3 : $this$map$iv3) {
                                destination$iv$iv3.add(StringsKt.trim((String) item$iv$iv3).toString());
                                $this$map$iv3 = $this$map$iv3;
                            }
                            Iterable $this$filter$iv = (List) destination$iv$iv3;
                            Collection destination$iv$iv4 = new ArrayList();
                            for (Object element$iv$iv : $this$filter$iv) {
                                Iterable $this$filter$iv2 = $this$filter$iv;
                                if (((String) element$iv$iv).length() > 0) {
                                    destination$iv$iv4.add(element$iv$iv);
                                }
                                $this$filter$iv = $this$filter$iv2;
                            }
                            list = (List) destination$iv$iv4;
                        }
                        List genre6 = list;
                        String match = data.getMatch();
                        rating = match == null ? StringsKt.replace$default(match, "IMDb ", "", false, 4, (Object) null) : null;
                        int runTime5 = UtilsKt.convertRuntimeToMinutes(String.valueOf(data.getRuntime()));
                        suggest = data.getSuggest();
                        if (suggest == null) {
                            Iterable $this$map$iv4 = suggest;
                            Collection destination$iv$iv5 = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv4, 10));
                            Iterable $this$mapTo$iv$iv2 = $this$map$iv4;
                            for (Object item$iv$iv4 : $this$mapTo$iv$iv2) {
                                Iterable $this$map$iv5 = $this$map$iv4;
                                final Suggest it = (Suggest) item$iv$iv4;
                                destination$iv$iv5.add(MainAPIKt.newAnimeSearchResponse$default(this, "", AppUtils.INSTANCE.toJson(new Id(it.getId())), (TvType) null, false, new Function1() { // from class: com.horis.cncverse.NetflixMirrorProvider$$ExternalSyntheticLambda12
                                    public final Object invoke(Object obj5) {
                                        Unit load$lambda$4$0;
                                        load$lambda$4$0 = NetflixMirrorProvider.load$lambda$4$0(Suggest.this, this, (AnimeSearchResponse) obj5);
                                        return load$lambda$4$0;
                                    }
                                }, 12, (Object) null));
                                $this$map$iv4 = $this$map$iv5;
                                $this$mapTo$iv$iv2 = $this$mapTo$iv$iv2;
                                castList3 = castList3;
                                coroutine_suspended = coroutine_suspended;
                            }
                            obj3 = coroutine_suspended;
                            castList = castList3;
                            list2 = (List) destination$iv$iv5;
                        } else {
                            obj3 = coroutine_suspended;
                            castList = castList3;
                            list2 = null;
                        }
                        suggest2 = list2;
                        if (CollectionsKt.first(data.getEpisodes()) != null) {
                            Boxing.boxBoolean(episodes6.add(MainAPIKt.newEpisode(this, new LoadData(title7, url3), new Function1() { // from class: com.horis.cncverse.NetflixMirrorProvider$$ExternalSyntheticLambda13
                                public final Object invoke(Object obj5) {
                                    Unit load$lambda$5;
                                    load$lambda$5 = NetflixMirrorProvider.load$lambda$5(PostData.this, (Episode) obj5);
                                    return load$lambda$5;
                                }
                            })));
                            cast4 = cast7;
                            $continuation3 = $continuation2;
                            genre3 = genre6;
                            id3 = rating;
                            obj4 = obj3;
                            episodes4 = episodes6;
                            title3 = title7;
                            id4 = url3;
                            cast5 = suggest2;
                            runTime3 = runTime5;
                            data3 = data;
                            title4 = url4;
                            TvType type = CollectionsKt.first(data3.getEpisodes()) != null ? TvType.Movie : TvType.TvSeries;
                            $continuation3.L$0 = SpillingKt.nullOutSpilledVariable(title4);
                            $continuation3.L$1 = SpillingKt.nullOutSpilledVariable(id4);
                            $continuation3.L$2 = SpillingKt.nullOutSpilledVariable(cookies);
                            $continuation3.L$3 = SpillingKt.nullOutSpilledVariable(data3);
                            $continuation3.L$4 = SpillingKt.nullOutSpilledVariable(episodes4);
                            $continuation3.L$5 = SpillingKt.nullOutSpilledVariable(title3);
                            $continuation3.L$6 = SpillingKt.nullOutSpilledVariable(castList);
                            $continuation3.L$7 = SpillingKt.nullOutSpilledVariable(cast4);
                            $continuation3.L$8 = SpillingKt.nullOutSpilledVariable(genre3);
                            $continuation3.L$9 = SpillingKt.nullOutSpilledVariable(id3);
                            $continuation3.L$10 = SpillingKt.nullOutSpilledVariable(cast5);
                            $continuation3.L$11 = SpillingKt.nullOutSpilledVariable(type);
                            $continuation3.I$0 = runTime3;
                            $continuation3.label = 5;
                            String url8 = title4;
                            $result = MainAPIKt.newTvSeriesLoadResponse(this, title3, url8, type, episodes4, new NetflixMirrorProvider$load$5(id4, this, data3, genre3, cast4, id3, runTime3, cast5, null), $continuation3);
                            return $result != obj4 ? obj4 : $result;
                        }
                        Iterable $this$mapTo$iv = CollectionsKt.filterNotNull(data.getEpisodes());
                        ArrayList destination$iv = episodes6;
                        boolean z = false;
                        for (Iterator it2 = $this$mapTo$iv.iterator(); it2.hasNext(); it2 = it2) {
                            Object item$iv = it2.next();
                            final com.horis.cncverse.entities.Episode it3 = (com.horis.cncverse.entities.Episode) item$iv;
                            destination$iv.add(MainAPIKt.newEpisode(this, new LoadData(title7, it3.getId()), new Function1() { // from class: com.horis.cncverse.NetflixMirrorProvider$$ExternalSyntheticLambda14
                                public final Object invoke(Object obj5) {
                                    Unit load$lambda$6$0;
                                    load$lambda$6$0 = NetflixMirrorProvider.load$lambda$6$0(com.horis.cncverse.entities.Episode.this, (Episode) obj5);
                                    return load$lambda$6$0;
                                }
                            }));
                            $this$mapTo$iv = $this$mapTo$iv;
                            z = z;
                        }
                        Integer nextPageShow = data.getNextPageShow();
                        if (nextPageShow == null) {
                            url5 = url4;
                            $continuation3 = $continuation2;
                            obj4 = obj3;
                            runTime = runTime5;
                        } else if (nextPageShow.intValue() == 1) {
                            String url9 = data.getNextPageSeason();
                            Intrinsics.checkNotNull(url9);
                            $continuation2.L$0 = url4;
                            $continuation2.L$1 = url3;
                            $continuation2.L$2 = SpillingKt.nullOutSpilledVariable(cookies);
                            $continuation2.L$3 = data;
                            $continuation2.L$4 = episodes6;
                            $continuation2.L$5 = title7;
                            $continuation2.L$6 = SpillingKt.nullOutSpilledVariable(castList);
                            $continuation2.L$7 = cast7;
                            $continuation2.L$8 = genre6;
                            $continuation2.L$9 = rating;
                            $continuation2.L$10 = suggest2;
                            $continuation2.L$11 = episodes6;
                            $continuation2.I$0 = runTime5;
                            $continuation2.label = 3;
                            Continuation $continuation4 = $continuation2;
                            String url10 = url4;
                            episodes = getEpisodes(title7, url10, url9, 2, $continuation4);
                            $continuation3 = $continuation4;
                            obj4 = obj3;
                            if (episodes == obj4) {
                                return obj4;
                            }
                            url6 = url10;
                            id = url3;
                            rating2 = rating;
                            cookies2 = cookies;
                            runTime2 = runTime5;
                            data2 = data;
                            episodes2 = episodes6;
                            genre2 = genre6;
                            title = title7;
                            cast2 = cast7;
                            arrayList = episodes2;
                            suggest2 = suggest2;
                            arrayList.addAll((Collection) episodes);
                            title2 = title;
                            episodes3 = episodes2;
                            id2 = id;
                            rating = rating2;
                            genre4 = genre2;
                            url7 = url6;
                            data = data2;
                            runTime = runTime2;
                            cookies = cookies2;
                            cast3 = cast2;
                            season = data.getSeason();
                            if (season != null || (dropLast = CollectionsKt.dropLast(season, 1)) == null) {
                                title3 = title2;
                                cast4 = cast3;
                                data3 = data;
                                title4 = url7;
                                cast5 = suggest2;
                                runTime3 = runTime;
                                genre3 = genre4;
                                episodes4 = episodes3;
                                id4 = id2;
                                id3 = rating;
                                TvType type2 = CollectionsKt.first(data3.getEpisodes()) != null ? TvType.Movie : TvType.TvSeries;
                                $continuation3.L$0 = SpillingKt.nullOutSpilledVariable(title4);
                                $continuation3.L$1 = SpillingKt.nullOutSpilledVariable(id4);
                                $continuation3.L$2 = SpillingKt.nullOutSpilledVariable(cookies);
                                $continuation3.L$3 = SpillingKt.nullOutSpilledVariable(data3);
                                $continuation3.L$4 = SpillingKt.nullOutSpilledVariable(episodes4);
                                $continuation3.L$5 = SpillingKt.nullOutSpilledVariable(title3);
                                $continuation3.L$6 = SpillingKt.nullOutSpilledVariable(castList);
                                $continuation3.L$7 = SpillingKt.nullOutSpilledVariable(cast4);
                                $continuation3.L$8 = SpillingKt.nullOutSpilledVariable(genre3);
                                $continuation3.L$9 = SpillingKt.nullOutSpilledVariable(id3);
                                $continuation3.L$10 = SpillingKt.nullOutSpilledVariable(cast5);
                                $continuation3.L$11 = SpillingKt.nullOutSpilledVariable(type2);
                                $continuation3.I$0 = runTime3;
                                $continuation3.label = 5;
                                String url82 = title4;
                                $result = MainAPIKt.newTvSeriesLoadResponse(this, title3, url82, type2, episodes4, new NetflixMirrorProvider$load$5(id4, this, data3, genre3, cast4, id3, runTime3, cast5, null), $continuation3);
                                if ($result != obj4) {
                                }
                            } else {
                                $continuation3.L$0 = url7;
                                $continuation3.L$1 = id2;
                                $continuation3.L$2 = SpillingKt.nullOutSpilledVariable(cookies);
                                $continuation3.L$3 = data;
                                $continuation3.L$4 = episodes3;
                                $continuation3.L$5 = title2;
                                $continuation3.L$6 = SpillingKt.nullOutSpilledVariable(castList);
                                $continuation3.L$7 = cast3;
                                $continuation3.L$8 = genre4;
                                $continuation3.L$9 = rating;
                                $continuation3.L$10 = suggest2;
                                $continuation3.L$11 = null;
                                $continuation3.I$0 = runTime;
                                $continuation3.label = 4;
                                amap = ParCollectionsKt.amap(dropLast, new NetflixMirrorProvider$load$4(episodes3, this, title2, url7, null), $continuation3);
                                if (amap == obj4) {
                                    return obj4;
                                }
                                episodes5 = episodes3;
                                suggest3 = suggest2;
                                data4 = data;
                                cookies3 = cookies;
                                title5 = title2;
                                runTime4 = runTime;
                                title6 = rating;
                                castList2 = castList;
                                rating3 = id2;
                                cast6 = cast3;
                                genre5 = genre4;
                                List list3 = (List) amap;
                                String str2 = title5;
                                title4 = url7;
                                genre3 = genre5;
                                episodes4 = episodes5;
                                title3 = str2;
                                id3 = title6;
                                castList = castList2;
                                runTime3 = runTime4;
                                data3 = data4;
                                cookies = cookies3;
                                cast4 = cast6;
                                cast5 = suggest3;
                                id4 = rating3;
                                TvType type22 = CollectionsKt.first(data3.getEpisodes()) != null ? TvType.Movie : TvType.TvSeries;
                                $continuation3.L$0 = SpillingKt.nullOutSpilledVariable(title4);
                                $continuation3.L$1 = SpillingKt.nullOutSpilledVariable(id4);
                                $continuation3.L$2 = SpillingKt.nullOutSpilledVariable(cookies);
                                $continuation3.L$3 = SpillingKt.nullOutSpilledVariable(data3);
                                $continuation3.L$4 = SpillingKt.nullOutSpilledVariable(episodes4);
                                $continuation3.L$5 = SpillingKt.nullOutSpilledVariable(title3);
                                $continuation3.L$6 = SpillingKt.nullOutSpilledVariable(castList);
                                $continuation3.L$7 = SpillingKt.nullOutSpilledVariable(cast4);
                                $continuation3.L$8 = SpillingKt.nullOutSpilledVariable(genre3);
                                $continuation3.L$9 = SpillingKt.nullOutSpilledVariable(id3);
                                $continuation3.L$10 = SpillingKt.nullOutSpilledVariable(cast5);
                                $continuation3.L$11 = SpillingKt.nullOutSpilledVariable(type22);
                                $continuation3.I$0 = runTime3;
                                $continuation3.label = 5;
                                String url822 = title4;
                                $result = MainAPIKt.newTvSeriesLoadResponse(this, title3, url822, type22, episodes4, new NetflixMirrorProvider$load$5(id4, this, data3, genre3, cast4, id3, runTime3, cast5, null), $continuation3);
                                if ($result != obj4) {
                                }
                            }
                        } else {
                            url5 = url4;
                            $continuation3 = $continuation2;
                            obj4 = obj3;
                            runTime = runTime5;
                        }
                        String str3 = url3;
                        cast3 = cast7;
                        id2 = str3;
                        title2 = title7;
                        url7 = url5;
                        episodes3 = episodes6;
                        genre4 = genre6;
                        season = data.getSeason();
                        if (season != null) {
                        }
                        title3 = title2;
                        cast4 = cast3;
                        data3 = data;
                        title4 = url7;
                        cast5 = suggest2;
                        runTime3 = runTime;
                        genre3 = genre4;
                        episodes4 = episodes3;
                        id4 = id2;
                        id3 = rating;
                        TvType type222 = CollectionsKt.first(data3.getEpisodes()) != null ? TvType.Movie : TvType.TvSeries;
                        $continuation3.L$0 = SpillingKt.nullOutSpilledVariable(title4);
                        $continuation3.L$1 = SpillingKt.nullOutSpilledVariable(id4);
                        $continuation3.L$2 = SpillingKt.nullOutSpilledVariable(cookies);
                        $continuation3.L$3 = SpillingKt.nullOutSpilledVariable(data3);
                        $continuation3.L$4 = SpillingKt.nullOutSpilledVariable(episodes4);
                        $continuation3.L$5 = SpillingKt.nullOutSpilledVariable(title3);
                        $continuation3.L$6 = SpillingKt.nullOutSpilledVariable(castList);
                        $continuation3.L$7 = SpillingKt.nullOutSpilledVariable(cast4);
                        $continuation3.L$8 = SpillingKt.nullOutSpilledVariable(genre3);
                        $continuation3.L$9 = SpillingKt.nullOutSpilledVariable(id3);
                        $continuation3.L$10 = SpillingKt.nullOutSpilledVariable(cast5);
                        $continuation3.L$11 = SpillingKt.nullOutSpilledVariable(type222);
                        $continuation3.I$0 = runTime3;
                        $continuation3.label = 5;
                        String url8222 = title4;
                        $result = MainAPIKt.newTvSeriesLoadResponse(this, title3, url8222, type222, episodes4, new NetflixMirrorProvider$load$5(id4, this, data3, genre3, cast4, id3, runTime3, cast5, null), $continuation3);
                        if ($result != obj4) {
                        }
                        break;
                    case 1:
                        netflixMirrorProvider = (NetflixMirrorProvider) $continuation.L$1;
                        url2 = (String) $continuation.L$0;
                        ResultKt.throwOnFailure($result);
                        obj = $result;
                        netflixMirrorProvider.cookie_value = (String) obj;
                        String text$iv2 = url2;
                        String id52 = ((Id) UtilsKt.getJSONParser().parse(text$iv2, Reflection.getOrCreateKotlinClass(Id.class))).getId();
                        cookies = MapsKt.mapOf(new Pair[]{TuplesKt.to("t_hash_t", this.cookie_value), TuplesKt.to("hd", "on"), TuplesKt.to("ott", "nf")});
                        $continuation.L$0 = url2;
                        $continuation.L$1 = id52;
                        $continuation.L$2 = SpillingKt.nullOutSpilledVariable(cookies);
                        $continuation.label = 2;
                        obj2 = Requests.get$default(UtilsKt.getApp(), getMainUrl() + "/mobile/post.php?id=" + id52 + "&t=" + APIHolder.INSTANCE.getUnixTime(), this.headers, getMainUrl() + "/home", (Map) null, cookies, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, $continuation, 4072, (Object) null);
                        $continuation2 = $continuation;
                        if (obj2 != coroutine_suspended) {
                        }
                        break;
                    case 2:
                        url3 = (String) $continuation.L$1;
                        String url11 = (String) $continuation.L$0;
                        ResultKt.throwOnFailure($result);
                        cookies = (Map) $continuation.L$2;
                        url4 = url11;
                        $continuation2 = $continuation;
                        obj2 = $result;
                        NiceResponse this_$iv2 = (NiceResponse) obj2;
                        ResponseParser parser2 = this_$iv2.getParser();
                        Intrinsics.checkNotNull(parser2);
                        data = (PostData) parser2.parse(this_$iv2.getText(), Reflection.getOrCreateKotlinClass(PostData.class));
                        ArrayList episodes62 = new ArrayList();
                        String title72 = data.getTitle();
                        cast = data.getCast();
                        if (cast != null) {
                            break;
                        }
                        emptyList = CollectionsKt.emptyList();
                        List castList32 = emptyList;
                        List $this$map$iv22 = castList32;
                        int $i$f$map2 = CollectionsKt.collectionSizeOrDefault($this$map$iv22, 10);
                        Collection destination$iv$iv22 = new ArrayList($i$f$map2);
                        Iterable $this$mapTo$iv$iv3 = $this$map$iv22;
                        while (r20.hasNext()) {
                        }
                        List cast72 = (List) destination$iv$iv22;
                        genre = data.getGenre();
                        if (genre != null) {
                            break;
                        }
                        list = null;
                        List genre62 = list;
                        String match2 = data.getMatch();
                        rating = match2 == null ? StringsKt.replace$default(match2, "IMDb ", "", false, 4, (Object) null) : null;
                        int runTime52 = UtilsKt.convertRuntimeToMinutes(String.valueOf(data.getRuntime()));
                        suggest = data.getSuggest();
                        if (suggest == null) {
                        }
                        suggest2 = list2;
                        if (CollectionsKt.first(data.getEpisodes()) != null) {
                        }
                        break;
                    case 3:
                        int runTime6 = $continuation.I$0;
                        arrayList = (ArrayList) $continuation.L$11;
                        rating2 = (String) $continuation.L$9;
                        genre2 = (List) $continuation.L$8;
                        cast2 = (List) $continuation.L$7;
                        title = (String) $continuation.L$5;
                        episodes2 = (ArrayList) $continuation.L$4;
                        data2 = (PostData) $continuation.L$3;
                        cookies2 = (Map) $continuation.L$2;
                        runTime2 = runTime6;
                        id = (String) $continuation.L$1;
                        url6 = (String) $continuation.L$0;
                        ResultKt.throwOnFailure($result);
                        $continuation3 = $continuation;
                        castList = (List) $continuation.L$6;
                        obj4 = coroutine_suspended;
                        suggest2 = (List) $continuation.L$10;
                        episodes = $result;
                        arrayList.addAll((Collection) episodes);
                        title2 = title;
                        episodes3 = episodes2;
                        id2 = id;
                        rating = rating2;
                        genre4 = genre2;
                        url7 = url6;
                        data = data2;
                        runTime = runTime2;
                        cookies = cookies2;
                        cast3 = cast2;
                        season = data.getSeason();
                        if (season != null) {
                        }
                        title3 = title2;
                        cast4 = cast3;
                        data3 = data;
                        title4 = url7;
                        cast5 = suggest2;
                        runTime3 = runTime;
                        genre3 = genre4;
                        episodes4 = episodes3;
                        id4 = id2;
                        id3 = rating;
                        TvType type2222 = CollectionsKt.first(data3.getEpisodes()) != null ? TvType.Movie : TvType.TvSeries;
                        $continuation3.L$0 = SpillingKt.nullOutSpilledVariable(title4);
                        $continuation3.L$1 = SpillingKt.nullOutSpilledVariable(id4);
                        $continuation3.L$2 = SpillingKt.nullOutSpilledVariable(cookies);
                        $continuation3.L$3 = SpillingKt.nullOutSpilledVariable(data3);
                        $continuation3.L$4 = SpillingKt.nullOutSpilledVariable(episodes4);
                        $continuation3.L$5 = SpillingKt.nullOutSpilledVariable(title3);
                        $continuation3.L$6 = SpillingKt.nullOutSpilledVariable(castList);
                        $continuation3.L$7 = SpillingKt.nullOutSpilledVariable(cast4);
                        $continuation3.L$8 = SpillingKt.nullOutSpilledVariable(genre3);
                        $continuation3.L$9 = SpillingKt.nullOutSpilledVariable(id3);
                        $continuation3.L$10 = SpillingKt.nullOutSpilledVariable(cast5);
                        $continuation3.L$11 = SpillingKt.nullOutSpilledVariable(type2222);
                        $continuation3.I$0 = runTime3;
                        $continuation3.label = 5;
                        String url82222 = title4;
                        $result = MainAPIKt.newTvSeriesLoadResponse(this, title3, url82222, type2222, episodes4, new NetflixMirrorProvider$load$5(id4, this, data3, genre3, cast4, id3, runTime3, cast5, null), $continuation3);
                        if ($result != obj4) {
                        }
                        break;
                    case 4:
                        int runTime7 = $continuation.I$0;
                        suggest3 = (List) $continuation.L$10;
                        title6 = (String) $continuation.L$9;
                        castList2 = (List) $continuation.L$6;
                        title5 = (String) $continuation.L$5;
                        episodes5 = (ArrayList) $continuation.L$4;
                        rating3 = (String) $continuation.L$1;
                        runTime4 = runTime7;
                        String url12 = (String) $continuation.L$0;
                        ResultKt.throwOnFailure($result);
                        $continuation3 = $continuation;
                        data4 = (PostData) $continuation.L$3;
                        cookies3 = (Map) $continuation.L$2;
                        genre5 = (List) $continuation.L$8;
                        obj4 = coroutine_suspended;
                        url7 = url12;
                        amap = $result;
                        cast6 = (List) $continuation.L$7;
                        List list32 = (List) amap;
                        String str22 = title5;
                        title4 = url7;
                        genre3 = genre5;
                        episodes4 = episodes5;
                        title3 = str22;
                        id3 = title6;
                        castList = castList2;
                        runTime3 = runTime4;
                        data3 = data4;
                        cookies = cookies3;
                        cast4 = cast6;
                        cast5 = suggest3;
                        id4 = rating3;
                        TvType type22222 = CollectionsKt.first(data3.getEpisodes()) != null ? TvType.Movie : TvType.TvSeries;
                        $continuation3.L$0 = SpillingKt.nullOutSpilledVariable(title4);
                        $continuation3.L$1 = SpillingKt.nullOutSpilledVariable(id4);
                        $continuation3.L$2 = SpillingKt.nullOutSpilledVariable(cookies);
                        $continuation3.L$3 = SpillingKt.nullOutSpilledVariable(data3);
                        $continuation3.L$4 = SpillingKt.nullOutSpilledVariable(episodes4);
                        $continuation3.L$5 = SpillingKt.nullOutSpilledVariable(title3);
                        $continuation3.L$6 = SpillingKt.nullOutSpilledVariable(castList);
                        $continuation3.L$7 = SpillingKt.nullOutSpilledVariable(cast4);
                        $continuation3.L$8 = SpillingKt.nullOutSpilledVariable(genre3);
                        $continuation3.L$9 = SpillingKt.nullOutSpilledVariable(id3);
                        $continuation3.L$10 = SpillingKt.nullOutSpilledVariable(cast5);
                        $continuation3.L$11 = SpillingKt.nullOutSpilledVariable(type22222);
                        $continuation3.I$0 = runTime3;
                        $continuation3.label = 5;
                        String url822222 = title4;
                        $result = MainAPIKt.newTvSeriesLoadResponse(this, title3, url822222, type22222, episodes4, new NetflixMirrorProvider$load$5(id4, this, data3, genre3, cast4, id3, runTime3, cast5, null), $continuation3);
                        if ($result != obj4) {
                        }
                        break;
                    case 5:
                        int i = $continuation.I$0;
                        TvType tvType = (TvType) $continuation.L$11;
                        List list4 = (List) $continuation.L$10;
                        String str4 = (String) $continuation.L$9;
                        List list5 = (List) $continuation.L$8;
                        List list6 = (List) $continuation.L$7;
                        List list7 = (List) $continuation.L$6;
                        String str5 = (String) $continuation.L$5;
                        ArrayList arrayList2 = (ArrayList) $continuation.L$4;
                        PostData postData = (PostData) $continuation.L$3;
                        Map map = (Map) $continuation.L$2;
                        String str6 = (String) $continuation.L$1;
                        String str7 = (String) $continuation.L$0;
                        ResultKt.throwOnFailure($result);
                    default:
                        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }
            }
        }
        netflixMirrorProvider$load$1 = new NetflixMirrorProvider$load$1(this, continuation);
        $continuation = netflixMirrorProvider$load$1;
        Object $result2 = $continuation.result;
        Object coroutine_suspended2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
        switch ($continuation.label) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit load$lambda$4$0(Suggest $it, NetflixMirrorProvider this$0, AnimeSearchResponse $this$newAnimeSearchResponse) {
        $this$newAnimeSearchResponse.setPosterUrl("https://imgcdn.kim/poster/v/" + $it.getId() + ".jpg");
        $this$newAnimeSearchResponse.setPosterHeaders(MapsKt.mapOf(TuplesKt.to("Referer", this$0.getMainUrl() + "/home")));
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit load$lambda$5(PostData $data, Episode $this$newEpisode) {
        $this$newEpisode.setName($data.getTitle());
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit load$lambda$6$0(com.horis.cncverse.entities.Episode $it, Episode $this$newEpisode) {
        $this$newEpisode.setName($it.getT());
        $this$newEpisode.setEpisode(StringsKt.toIntOrNull(StringsKt.replace$default($it.getEp(), "E", "", false, 4, (Object) null)));
        $this$newEpisode.setSeason(StringsKt.toIntOrNull(StringsKt.replace$default($it.getS(), "S", "", false, 4, (Object) null)));
        $this$newEpisode.setPosterUrl("https://imgcdn.kim/nf/v/200/" + $it.getId() + ".jpg");
        $this$newEpisode.setRunTime(StringsKt.toIntOrNull(StringsKt.replace$default($it.getTime(), "m", "", false, 4, (Object) null)));
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:10:0x0029  */
    /* JADX WARN: Removed duplicated region for block: B:12:0x0031  */
    /* JADX WARN: Removed duplicated region for block: B:13:0x005e  */
    /* JADX WARN: Removed duplicated region for block: B:16:0x0149 A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:17:0x014a  */
    /* JADX WARN: Removed duplicated region for block: B:20:0x0172  */
    /* JADX WARN: Removed duplicated region for block: B:25:0x01bc  */
    /* JADX WARN: Removed duplicated region for block: B:28:0x01c7  */
    /* JADX WARN: Removed duplicated region for block: B:29:0x01d3 A[RETURN] */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:17:0x014a -> B:18:0x0150). Please submit an issue!!! */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public final Object getEpisodes(String title, String eid, String sid, int page, Continuation<? super List<Episode>> continuation) {
        NetflixMirrorProvider$getEpisodes$1 netflixMirrorProvider$getEpisodes$1;
        NetflixMirrorProvider netflixMirrorProvider;
        Map cookies;
        NetflixMirrorProvider$getEpisodes$1 netflixMirrorProvider$getEpisodes$12;
        Object obj;
        String title2;
        int page2;
        String eid2;
        ArrayList episodes;
        int pg;
        String sid2;
        Iterable episodes2;
        EpisodesData data;
        String eid3;
        if (continuation instanceof NetflixMirrorProvider$getEpisodes$1) {
            netflixMirrorProvider$getEpisodes$1 = (NetflixMirrorProvider$getEpisodes$1) continuation;
            if ((netflixMirrorProvider$getEpisodes$1.label & Integer.MIN_VALUE) != 0) {
                netflixMirrorProvider$getEpisodes$1.label -= Integer.MIN_VALUE;
                Object $result = netflixMirrorProvider$getEpisodes$1.result;
                Object coroutine_suspended = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (netflixMirrorProvider$getEpisodes$1.label) {
                    case 0:
                        ResultKt.throwOnFailure($result);
                        ArrayList episodes3 = new ArrayList();
                        Map cookies2 = MapsKt.mapOf(new Pair[]{TuplesKt.to("t_hash_t", this.cookie_value), TuplesKt.to("hd", "on"), TuplesKt.to("ott", "nf")});
                        netflixMirrorProvider = this;
                        ArrayList episodes4 = episodes3;
                        cookies = cookies2;
                        int pg2 = page;
                        String sid3 = sid;
                        netflixMirrorProvider$getEpisodes$12 = netflixMirrorProvider$getEpisodes$1;
                        obj = coroutine_suspended;
                        title2 = title;
                        page2 = page;
                        eid2 = eid;
                        NetflixMirrorProvider$getEpisodes$1 netflixMirrorProvider$getEpisodes$13 = netflixMirrorProvider$getEpisodes$12;
                        netflixMirrorProvider$getEpisodes$13.L$0 = title2;
                        netflixMirrorProvider$getEpisodes$13.L$1 = eid2;
                        netflixMirrorProvider$getEpisodes$13.L$2 = sid3;
                        netflixMirrorProvider$getEpisodes$13.L$3 = episodes4;
                        netflixMirrorProvider$getEpisodes$13.L$4 = cookies;
                        netflixMirrorProvider$getEpisodes$13.I$0 = page2;
                        netflixMirrorProvider$getEpisodes$13.I$1 = pg2;
                        NetflixMirrorProvider netflixMirrorProvider2 = netflixMirrorProvider;
                        netflixMirrorProvider$getEpisodes$13.label = 1;
                        episodes = episodes4;
                        pg = pg2;
                        Object obj2 = obj;
                        Object obj3 = Requests.get$default(UtilsKt.getApp(), netflixMirrorProvider.getMainUrl() + "/mobile/episodes.php?s=" + sid3 + "&series=" + eid2 + "&t=" + APIHolder.INSTANCE.getUnixTime() + "&page=" + pg2, netflixMirrorProvider.headers, netflixMirrorProvider.getMainUrl() + "/home", (Map) null, cookies, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, netflixMirrorProvider$getEpisodes$13, 4072, (Object) null);
                        if (obj3 != obj2) {
                            return obj2;
                        }
                        obj = obj2;
                        sid2 = sid3;
                        netflixMirrorProvider = netflixMirrorProvider2;
                        $result = obj3;
                        netflixMirrorProvider$getEpisodes$12 = netflixMirrorProvider$getEpisodes$13;
                        NiceResponse this_$iv = (NiceResponse) $result;
                        ResponseParser parser = this_$iv.getParser();
                        Intrinsics.checkNotNull(parser);
                        EpisodesData data2 = (EpisodesData) parser.parse(this_$iv.getText(), Reflection.getOrCreateKotlinClass(EpisodesData.class));
                        episodes2 = data2.getEpisodes();
                        if (episodes2 != null) {
                            data = data2;
                            eid3 = eid2;
                        } else {
                            Iterable $this$mapTo$iv = episodes2;
                            ArrayList destination$iv = episodes;
                            for (Object item$iv : $this$mapTo$iv) {
                                final com.horis.cncverse.entities.Episode it = (com.horis.cncverse.entities.Episode) item$iv;
                                destination$iv.add(MainAPIKt.newEpisode(netflixMirrorProvider, new LoadData(title2, it.getId()), new Function1() { // from class: com.horis.cncverse.NetflixMirrorProvider$$ExternalSyntheticLambda0
                                    public final Object invoke(Object obj4) {
                                        Unit episodes$lambda$0$0;
                                        episodes$lambda$0$0 = NetflixMirrorProvider.getEpisodes$lambda$0$0(com.horis.cncverse.entities.Episode.this, (Episode) obj4);
                                        return episodes$lambda$0$0;
                                    }
                                }));
                                data2 = data2;
                                eid2 = eid2;
                                $this$mapTo$iv = $this$mapTo$iv;
                            }
                            data = data2;
                            eid3 = eid2;
                        }
                        if (data.getNextPageShow() == 0) {
                            pg2 = pg + 1;
                            sid3 = sid2;
                            episodes4 = episodes;
                            eid2 = eid3;
                            NetflixMirrorProvider$getEpisodes$1 netflixMirrorProvider$getEpisodes$132 = netflixMirrorProvider$getEpisodes$12;
                            netflixMirrorProvider$getEpisodes$132.L$0 = title2;
                            netflixMirrorProvider$getEpisodes$132.L$1 = eid2;
                            netflixMirrorProvider$getEpisodes$132.L$2 = sid3;
                            netflixMirrorProvider$getEpisodes$132.L$3 = episodes4;
                            netflixMirrorProvider$getEpisodes$132.L$4 = cookies;
                            netflixMirrorProvider$getEpisodes$132.I$0 = page2;
                            netflixMirrorProvider$getEpisodes$132.I$1 = pg2;
                            NetflixMirrorProvider netflixMirrorProvider22 = netflixMirrorProvider;
                            netflixMirrorProvider$getEpisodes$132.label = 1;
                            episodes = episodes4;
                            pg = pg2;
                            Object obj22 = obj;
                            Object obj32 = Requests.get$default(UtilsKt.getApp(), netflixMirrorProvider.getMainUrl() + "/mobile/episodes.php?s=" + sid3 + "&series=" + eid2 + "&t=" + APIHolder.INSTANCE.getUnixTime() + "&page=" + pg2, netflixMirrorProvider.headers, netflixMirrorProvider.getMainUrl() + "/home", (Map) null, cookies, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, netflixMirrorProvider$getEpisodes$132, 4072, (Object) null);
                            if (obj32 != obj22) {
                            }
                        } else {
                            return episodes;
                        }
                    case 1:
                        int pg3 = netflixMirrorProvider$getEpisodes$1.I$1;
                        int page3 = netflixMirrorProvider$getEpisodes$1.I$0;
                        Map cookies3 = (Map) netflixMirrorProvider$getEpisodes$1.L$4;
                        ArrayList episodes5 = (ArrayList) netflixMirrorProvider$getEpisodes$1.L$3;
                        sid2 = (String) netflixMirrorProvider$getEpisodes$1.L$2;
                        String eid4 = (String) netflixMirrorProvider$getEpisodes$1.L$1;
                        String title3 = (String) netflixMirrorProvider$getEpisodes$1.L$0;
                        ResultKt.throwOnFailure($result);
                        pg = pg3;
                        episodes = episodes5;
                        title2 = title3;
                        cookies = cookies3;
                        obj = coroutine_suspended;
                        page2 = page3;
                        netflixMirrorProvider$getEpisodes$12 = netflixMirrorProvider$getEpisodes$1;
                        eid2 = eid4;
                        netflixMirrorProvider = this;
                        NiceResponse this_$iv2 = (NiceResponse) $result;
                        ResponseParser parser2 = this_$iv2.getParser();
                        Intrinsics.checkNotNull(parser2);
                        EpisodesData data22 = (EpisodesData) parser2.parse(this_$iv2.getText(), Reflection.getOrCreateKotlinClass(EpisodesData.class));
                        episodes2 = data22.getEpisodes();
                        if (episodes2 != null) {
                        }
                        if (data.getNextPageShow() == 0) {
                        }
                        break;
                    default:
                        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }
            }
        }
        netflixMirrorProvider$getEpisodes$1 = new NetflixMirrorProvider$getEpisodes$1(this, continuation);
        Object $result2 = netflixMirrorProvider$getEpisodes$1.result;
        Object coroutine_suspended2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
        switch (netflixMirrorProvider$getEpisodes$1.label) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit getEpisodes$lambda$0$0(com.horis.cncverse.entities.Episode $it, Episode $this$newEpisode) {
        $this$newEpisode.setName($it.getT());
        $this$newEpisode.setEpisode(StringsKt.toIntOrNull(StringsKt.replace$default($it.getEp(), "E", "", false, 4, (Object) null)));
        $this$newEpisode.setSeason(StringsKt.toIntOrNull(StringsKt.replace$default($it.getS(), "S", "", false, 4, (Object) null)));
        $this$newEpisode.setPosterUrl("https://imgcdn.kim/epimg/150/" + $it.getId() + ".jpg");
        $this$newEpisode.setRunTime(StringsKt.toIntOrNull(StringsKt.replace$default($it.getTime(), "m", "", false, 4, (Object) null)));
        return Unit.INSTANCE;
    }

    private final void showSubscriptionPopupIfNeeded() {
        final Context ctx = context;
        if (ctx == null || subscriptionPopupShown) {
            return;
        }
        try {
            boolean isTV = Globals.INSTANCE.isLayout(2);
            if (isTV) {
                return;
            }
        } catch (Exception e) {
        }
        SharedPreferences sharedPreferences = ctx.getSharedPreferences("CNCVerseSubscription", 0);
        boolean isSubscribed = Intrinsics.areEqual(sharedPreferences != null ? sharedPreferences.getString("mode", "ads") : null, "subscription");
        if (isSubscribed) {
            return;
        }
        SharedPreferences _dontShowPrefs = ctx.getSharedPreferences("CNCVerseSubscription", 0);
        if (_dontShowPrefs.getBoolean("dont_show_ads_popup", false)) {
            subscriptionPopupShown = true;
            return;
        }
        subscriptionPopupShown = true;
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.horis.cncverse.NetflixMirrorProvider$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                NetflixMirrorProvider.showSubscriptionPopupIfNeeded$lambda$0(ctx);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void showSubscriptionPopupIfNeeded$lambda$0(final Context $ctx) {
        try {
            float dp = $ctx.getResources().getDisplayMetrics().density;
            GradientDrawable $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u240 = new GradientDrawable();
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u240.setColor(Color.parseColor("#1A1A2E"));
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u240.setCornerRadius(16.0f * dp);
            LinearLayout root = new LinearLayout($ctx);
            root.setOrientation(1);
            float f = 24;
            root.setPadding((int) (f * dp), (int) (20 * dp), (int) (f * dp), (int) (16 * dp));
            root.setBackground($this$showSubscriptionPopupIfNeeded_u24lambda_u240_u240);
            TextView $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u242 = new TextView($ctx);
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u242.setText("📺 You're in Ads Mode");
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u242.setTextColor(-1);
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u242.setTextSize(17.0f);
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u242.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams it = new LinearLayout.LayoutParams(-1, -2);
            it.bottomMargin = (int) (8 * dp);
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u242.setLayoutParams(it);
            View divider = new View($ctx);
            divider.setBackgroundColor(Color.parseColor("#2D2D4A"));
            LinearLayout.LayoutParams it2 = new LinearLayout.LayoutParams(-1, 1);
            it2.bottomMargin = (int) (12 * dp);
            divider.setLayoutParams(it2);
            TextView $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u244 = new TextView($ctx);
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u244.setText("All CNCVerse extensions currently run with ads.\n\nSubscribe to remove ads from just ₹20/month.\n\nManage via Settings > Extensions > CNCVerse Cloudstream Repo > Subscription Manager.");
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u244.setTextColor(Color.parseColor("#A0A0A8"));
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u244.setTextSize(14.0f);
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u244.setLineSpacing(0.0f, 1.4f);
            LinearLayout.LayoutParams it3 = new LinearLayout.LayoutParams(-1, -2);
            it3.bottomMargin = (int) (18 * dp);
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u244.setLayoutParams(it3);
            LinearLayout btnRow = new LinearLayout($ctx);
            btnRow.setOrientation(0);
            btnRow.setGravity(8388613);
            TextView laterTv = new TextView($ctx);
            laterTv.setText("Maybe Later");
            laterTv.setTextColor(Color.parseColor("#808090"));
            laterTv.setTextSize(14.0f);
            float f2 = 10;
            int p = (int) (f2 * dp);
            laterTv.setPadding(p, p, p, p);
            laterTv.setClickable(true);
            laterTv.setFocusable(true);
            TextView subscribeTv = new TextView($ctx);
            subscribeTv.setText("Subscribe Now");
            subscribeTv.setTextColor(Color.parseColor("#A78BFA"));
            subscribeTv.setTextSize(14.0f);
            subscribeTv.setTypeface(Typeface.DEFAULT_BOLD);
            int p2 = (int) (f2 * dp);
            subscribeTv.setPadding(p2, p2, 0, p2);
            subscribeTv.setClickable(true);
            subscribeTv.setFocusable(true);
            LinearLayout $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u248 = new LinearLayout($ctx);
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u248.setOrientation(0);
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u248.setGravity(8388627);
            LinearLayout.LayoutParams it4 = new LinearLayout.LayoutParams(-1, -2);
            it4.bottomMargin = (int) (f2 * dp);
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u248.setLayoutParams(it4);
            final CheckBox $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u249 = new CheckBox($ctx);
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u249.setChecked(false);
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u249.setButtonTintList(ColorStateList.valueOf(Color.parseColor("#A78BFA")));
            TextView $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u2410 = new TextView($ctx);
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u2410.setText("Don't show me again");
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u2410.setTextColor(Color.parseColor("#A0A0A8"));
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u2410.setTextSize(13.0f);
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u2410.setPadding((int) (6 * dp), 0, 0, 0);
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u248.addView($this$showSubscriptionPopupIfNeeded_u24lambda_u240_u249);
            $this$showSubscriptionPopupIfNeeded_u24lambda_u240_u248.addView($this$showSubscriptionPopupIfNeeded_u24lambda_u240_u2410);
            btnRow.addView(laterTv);
            btnRow.addView(subscribeTv);
            root.addView($this$showSubscriptionPopupIfNeeded_u24lambda_u240_u242);
            root.addView(divider);
            root.addView($this$showSubscriptionPopupIfNeeded_u24lambda_u240_u244);
            root.addView($this$showSubscriptionPopupIfNeeded_u24lambda_u240_u248);
            root.addView(btnRow);
            final AlertDialog dialog = new AlertDialog.Builder($ctx).setView(root).setCancelable(true).create();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(0));
            }
            laterTv.setOnClickListener(new View.OnClickListener() { // from class: com.horis.cncverse.NetflixMirrorProvider$$ExternalSyntheticLambda2
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    NetflixMirrorProvider.showSubscriptionPopupIfNeeded$lambda$0$11($this$showSubscriptionPopupIfNeeded_u24lambda_u240_u249, $ctx, dialog, view);
                }
            });
            subscribeTv.setOnClickListener(new View.OnClickListener() { // from class: com.horis.cncverse.NetflixMirrorProvider$$ExternalSyntheticLambda3
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    NetflixMirrorProvider.showSubscriptionPopupIfNeeded$lambda$0$12(dialog, $ctx, view);
                }
            });
            dialog.show();
        } catch (Exception e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void showSubscriptionPopupIfNeeded$lambda$0$11(CheckBox $dontShowCb, Context $ctx, AlertDialog $dialog, View it) {
        if ($dontShowCb.isChecked()) {
            $ctx.getSharedPreferences("CNCVerseSubscription", 0).edit().putBoolean("dont_show_ads_popup", true).apply();
        }
        $dialog.dismiss();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void showSubscriptionPopupIfNeeded$lambda$0$12(AlertDialog $dialog, Context $ctx, View it) {
        $dialog.dismiss();
        try {
            Intent i = new Intent("android.intent.action.VIEW", Uri.parse("https://cncverse-sub.pages.dev"));
            i.addFlags(268435456);
            $ctx.startActivity(i);
        } catch (Exception e) {
        }
    }

    private final void showTelegramPopup() {
        final Context ctx;
        if (Globals.INSTANCE.isLayout(2) || (ctx = context) == null || telegramPopupShown) {
            return;
        }
        SharedPreferences prefs = ctx.getSharedPreferences("cncverse_prefs", 0);
        if (prefs.getBoolean("telegram_popup_shown", false)) {
            telegramPopupShown = true;
            return;
        }
        telegramPopupShown = true;
        prefs.edit().putBoolean("telegram_popup_shown", true).apply();
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.horis.cncverse.NetflixMirrorProvider$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                NetflixMirrorProvider.showTelegramPopup$lambda$0(ctx);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void showTelegramPopup$lambda$0(final Context $ctx) {
        try {
            float dp = $ctx.getResources().getDisplayMetrics().density;
            GradientDrawable $this$showTelegramPopup_u24lambda_u240_u240 = new GradientDrawable();
            $this$showTelegramPopup_u24lambda_u240_u240.setColor(Color.parseColor("#1A1A2E"));
            $this$showTelegramPopup_u24lambda_u240_u240.setCornerRadius(16.0f * dp);
            LinearLayout root = new LinearLayout($ctx);
            root.setOrientation(1);
            float f = 24;
            root.setPadding((int) (f * dp), (int) (20 * dp), (int) (f * dp), (int) (16 * dp));
            root.setBackground($this$showTelegramPopup_u24lambda_u240_u240);
            TextView $this$showTelegramPopup_u24lambda_u240_u242 = new TextView($ctx);
            $this$showTelegramPopup_u24lambda_u240_u242.setText("💬 Join CNCVerse Community");
            $this$showTelegramPopup_u24lambda_u240_u242.setTextColor(-1);
            $this$showTelegramPopup_u24lambda_u240_u242.setTextSize(17.0f);
            $this$showTelegramPopup_u24lambda_u240_u242.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams it = new LinearLayout.LayoutParams(-1, -2);
            float f2 = 10;
            it.bottomMargin = (int) (f2 * dp);
            $this$showTelegramPopup_u24lambda_u240_u242.setLayoutParams(it);
            View dividerV = new View($ctx);
            dividerV.setBackgroundColor(Color.parseColor("#2D2D4A"));
            LinearLayout.LayoutParams it2 = new LinearLayout.LayoutParams(-1, 1);
            it2.bottomMargin = (int) (14 * dp);
            dividerV.setLayoutParams(it2);
            TextView $this$showTelegramPopup_u24lambda_u240_u244 = new TextView($ctx);
            $this$showTelegramPopup_u24lambda_u240_u244.setText("Join our Telegram group to discuss and share your opinion!");
            $this$showTelegramPopup_u24lambda_u240_u244.setTextColor(Color.parseColor("#A0A0A8"));
            $this$showTelegramPopup_u24lambda_u240_u244.setTextSize(14.0f);
            $this$showTelegramPopup_u24lambda_u240_u244.setLineSpacing(0.0f, 1.4f);
            LinearLayout.LayoutParams it3 = new LinearLayout.LayoutParams(-1, -2);
            it3.bottomMargin = (int) (18 * dp);
            $this$showTelegramPopup_u24lambda_u240_u244.setLayoutParams(it3);
            LinearLayout btnRow = new LinearLayout($ctx);
            btnRow.setOrientation(0);
            btnRow.setGravity(8388613);
            TextView laterTv = new TextView($ctx);
            laterTv.setText("Later");
            laterTv.setTextColor(Color.parseColor("#808090"));
            laterTv.setTextSize(14.0f);
            int p = (int) (f2 * dp);
            laterTv.setPadding(p, p, p, p);
            laterTv.setClickable(true);
            laterTv.setFocusable(true);
            TextView joinTv = new TextView($ctx);
            joinTv.setText("Join Telegram");
            joinTv.setTextColor(Color.parseColor("#5B9BF5"));
            joinTv.setTextSize(14.0f);
            joinTv.setTypeface(Typeface.DEFAULT_BOLD);
            int p2 = (int) (f2 * dp);
            joinTv.setPadding(p2, p2, 0, p2);
            joinTv.setClickable(true);
            joinTv.setFocusable(true);
            btnRow.addView(laterTv);
            btnRow.addView(joinTv);
            root.addView($this$showTelegramPopup_u24lambda_u240_u242);
            root.addView(dividerV);
            root.addView($this$showTelegramPopup_u24lambda_u240_u244);
            root.addView(btnRow);
            final AlertDialog dialog = new AlertDialog.Builder($ctx).setView(root).setCancelable(true).create();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(0));
            }
            laterTv.setOnClickListener(new View.OnClickListener() { // from class: com.horis.cncverse.NetflixMirrorProvider$$ExternalSyntheticLambda5
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    dialog.dismiss();
                }
            });
            joinTv.setOnClickListener(new View.OnClickListener() { // from class: com.horis.cncverse.NetflixMirrorProvider$$ExternalSyntheticLambda6
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    NetflixMirrorProvider.showTelegramPopup$lambda$0$9(dialog, $ctx, view);
                }
            });
            dialog.show();
        } catch (Exception e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void showTelegramPopup$lambda$0$9(AlertDialog $dialog, Context $ctx, View it) {
        $dialog.dismiss();
        try {
            Intent i = new Intent("android.intent.action.VIEW", Uri.parse("https://t.me/cncverse"));
            i.addFlags(268435456);
            $ctx.startActivity(i);
        } catch (Exception e) {
        }
    }

    private final void openInExternalBrowser(final String url) {
        final Context ctx;
        if (Globals.INSTANCE.isLayout(2) || (ctx = context) == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastBrowserOpenMs < BROWSER_DEBOUNCE_MS) {
            return;
        }
        lastBrowserOpenMs = now;
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.horis.cncverse.NetflixMirrorProvider$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                NetflixMirrorProvider.openInExternalBrowser$lambda$0(ctx, url);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void openInExternalBrowser$lambda$0(Context $ctx, String $url) {
        try {
            Intent $this$openInExternalBrowser_u24lambda_u240_u240 = new Intent("android.intent.action.VIEW", Uri.parse($url));
            $this$openInExternalBrowser_u24lambda_u240_u240.addFlags(268435456);
            $ctx.startActivity($this$openInExternalBrowser_u24lambda_u240_u240);
        } catch (Exception e) {
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:10:0x0029  */
    /* JADX WARN: Removed duplicated region for block: B:12:0x0031  */
    /* JADX WARN: Removed duplicated region for block: B:13:0x00c3  */
    /* JADX WARN: Removed duplicated region for block: B:14:0x0123  */
    /* JADX WARN: Removed duplicated region for block: B:15:0x0156  */
    /* JADX WARN: Removed duplicated region for block: B:16:0x0179  */
    /* JADX WARN: Removed duplicated region for block: B:55:0x040a A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:56:0x040b  */
    /* JADX WARN: Removed duplicated region for block: B:60:0x044c  */
    /* JADX WARN: Removed duplicated region for block: B:63:0x046f  */
    /* JADX WARN: Removed duplicated region for block: B:68:0x0533  */
    /* JADX WARN: Removed duplicated region for block: B:80:0x05c0  */
    /* JADX WARN: Removed duplicated region for block: B:85:0x06b5  */
    /* JADX WARN: Removed duplicated region for block: B:87:0x06ee  */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:67:0x0519 -> B:61:0x0469). Please submit an issue!!! */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:77:0x057f -> B:78:0x05ba). Please submit an issue!!! */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:83:0x0664 -> B:84:0x0689). Please submit an issue!!! */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:86:0x06d7 -> B:58:0x0446). Please submit an issue!!! */
    @Nullable
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public Object loadLinks(@NotNull String data, boolean isCasting, @NotNull Function1<? super SubtitleFile, Unit> function1, @NotNull Function1<? super ExtractorLink, Unit> function12, @NotNull Continuation<? super Boolean> continuation) {
        NetflixMirrorProvider$loadLinks$1 netflixMirrorProvider$loadLinks$1;
        Object $result;
        char c;
        char c2;
        Object $result2;
        Function1 subtitleCallback;
        Object callback;
        Object obj;
        Object data2;
        boolean isCasting2;
        NetflixMirrorProvider netflixMirrorProvider;
        boolean z;
        Map playlistHeaders;
        Object obj2;
        NetflixMirrorProvider$loadLinks$1 netflixMirrorProvider$loadLinks$12;
        String data3;
        Object callback2;
        Map cookies;
        String id;
        Function1 subtitleCallback2;
        boolean isCasting3;
        String title;
        String cookieStr;
        PlayList playlist;
        Iterator<PlayListItem> it;
        Map playlistHeaders2;
        Object obj3;
        Continuation $completion;
        Function1 function13;
        Iterator<Source> it2;
        PlayList playlist2;
        Map playlistHeaders3;
        String cookieStr2;
        String data4;
        String title2;
        boolean isCasting4;
        Function1 $result3;
        String data5;
        NetflixMirrorProvider$loadLinks$1 netflixMirrorProvider$loadLinks$13;
        Object $result4;
        Object obj4;
        PlayListItem item;
        Map cookies2;
        Function1 subtitleCallback3;
        Continuation $completion2;
        NetflixMirrorProvider netflixMirrorProvider2;
        Object obj5;
        NetflixMirrorProvider netflixMirrorProvider3;
        String cookieStr3;
        Function1 subtitleCallback4;
        String title3;
        String title4;
        Function1 subtitleCallback5;
        String data6;
        PlayListItem item2;
        Object $result5;
        PlayList playlist3;
        String cookieStr4;
        Continuation $completion3;
        Iterable $this$map$iv;
        int $i$f$map;
        Map cookies3;
        Function1 item$iv$iv;
        Function1 subtitleCallback6;
        NetflixMirrorProvider$loadLinks$1 netflixMirrorProvider$loadLinks$14;
        Iterable $this$mapTo$iv$iv;
        String title5;
        String $result6;
        Iterator it3;
        NetflixMirrorProvider netflixMirrorProvider4;
        String title6;
        int $i$f$mapTo;
        Collection destination$iv$iv;
        boolean isCasting5;
        String id2;
        Function1 callback3;
        Iterator it4;
        NetflixMirrorProvider$loadLinks$1 netflixMirrorProvider$loadLinks$15;
        Map cookies4;
        Collection collection;
        int $i$f$mapTo2;
        String data7;
        Object $result7;
        PlayListItem item3;
        Iterable $this$mapTo$iv$iv2;
        String title7;
        PlayList playlist4;
        String title8;
        Map playlistHeaders4;
        NetflixMirrorProvider netflixMirrorProvider5;
        Object obj6;
        int $i$f$map2;
        Function1 function14;
        boolean isCasting6;
        Collection destination$iv$iv2;
        Object subtitleCallback7;
        Iterable $this$map$iv2;
        if (continuation instanceof NetflixMirrorProvider$loadLinks$1) {
            netflixMirrorProvider$loadLinks$1 = (NetflixMirrorProvider$loadLinks$1) continuation;
            if ((netflixMirrorProvider$loadLinks$1.label & Integer.MIN_VALUE) != 0) {
                netflixMirrorProvider$loadLinks$1.label -= Integer.MIN_VALUE;
                $result = netflixMirrorProvider$loadLinks$1.result;
                Object coroutine_suspended = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (netflixMirrorProvider$loadLinks$1.label) {
                    case 0:
                        c = '\n';
                        c2 = 4;
                        ResultKt.throwOnFailure($result);
                        if (SubscriptionHelper.INSTANCE.isSubscribed(context)) {
                            $result2 = $result;
                        } else {
                            NetflixMirrorProvider $this$loadLinks_u24lambda_u240 = this;
                            final Context _ctx = context;
                            SharedPreferences _prefs = _ctx != null ? _ctx.getSharedPreferences("CNCVerseSubscription", 0) : null;
                            String _mode = _prefs != null ? _prefs.getString("mode", "ads") : null;
                            long _expiresAt = _prefs != null ? _prefs.getLong("expires_at", 0L) : 0L;
                            long _nowSec = System.currentTimeMillis() / 1000;
                            boolean _isSubscribed = Intrinsics.areEqual(_mode, "subscription") && (_expiresAt == 0 || _expiresAt > _nowSec);
                            if (_isSubscribed) {
                                $result2 = $result;
                            } else {
                                if (Intrinsics.areEqual(_mode, "subscription") && _expiresAt > 0 && _expiresAt <= _nowSec) {
                                    new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.horis.cncverse.NetflixMirrorProvider$$ExternalSyntheticLambda7
                                        @Override // java.lang.Runnable
                                        public final void run() {
                                            NetflixMirrorProvider.loadLinks$lambda$0$0(_ctx);
                                        }
                                    });
                                }
                                $result2 = $result;
                                $this$loadLinks_u24lambda_u240.openInExternalBrowser(new String(Base64.decode(OMG10, 0), Charsets.UTF_8));
                            }
                        }
                        String mainUrl = getMainUrl();
                        netflixMirrorProvider$loadLinks$1.L$0 = data;
                        subtitleCallback = function1;
                        netflixMirrorProvider$loadLinks$1.L$1 = subtitleCallback;
                        callback = function12;
                        netflixMirrorProvider$loadLinks$1.L$2 = callback;
                        netflixMirrorProvider$loadLinks$1.L$3 = this;
                        netflixMirrorProvider$loadLinks$1.Z$0 = isCasting;
                        netflixMirrorProvider$loadLinks$1.label = 1;
                        Object bypass = UtilsKt.bypass(mainUrl, netflixMirrorProvider$loadLinks$1);
                        if (bypass != coroutine_suspended) {
                            obj = bypass;
                            data2 = data;
                            isCasting2 = isCasting;
                            netflixMirrorProvider = this;
                            break;
                        } else {
                            return coroutine_suspended;
                        }
                        break;
                    case 1:
                        c = '\n';
                        c2 = 4;
                        boolean isCasting7 = netflixMirrorProvider$loadLinks$1.Z$0;
                        callback = (Function1) netflixMirrorProvider$loadLinks$1.L$2;
                        ResultKt.throwOnFailure($result);
                        $result2 = $result;
                        isCasting2 = isCasting7;
                        data2 = (String) netflixMirrorProvider$loadLinks$1.L$0;
                        netflixMirrorProvider = (NetflixMirrorProvider) netflixMirrorProvider$loadLinks$1.L$3;
                        subtitleCallback = (Function1) netflixMirrorProvider$loadLinks$1.L$1;
                        obj = $result2;
                        break;
                    case 2:
                        isCasting3 = netflixMirrorProvider$loadLinks$1.Z$0;
                        playlistHeaders = (Map) netflixMirrorProvider$loadLinks$1.L$7;
                        cookieStr = (String) netflixMirrorProvider$loadLinks$1.L$6;
                        cookies = (Map) netflixMirrorProvider$loadLinks$1.L$5;
                        id = (String) netflixMirrorProvider$loadLinks$1.L$4;
                        title = (String) netflixMirrorProvider$loadLinks$1.L$3;
                        callback2 = (Function1) netflixMirrorProvider$loadLinks$1.L$2;
                        subtitleCallback2 = (Function1) netflixMirrorProvider$loadLinks$1.L$1;
                        data3 = (String) netflixMirrorProvider$loadLinks$1.L$0;
                        ResultKt.throwOnFailure($result);
                        netflixMirrorProvider$loadLinks$12 = netflixMirrorProvider$loadLinks$1;
                        $result2 = $result;
                        obj2 = coroutine_suspended;
                        z = true;
                        NiceResponse this_$iv = (NiceResponse) $result;
                        ResponseParser parser = this_$iv.getParser();
                        Intrinsics.checkNotNull(parser);
                        playlist = (PlayList) parser.parse(this_$iv.getText(), Reflection.getOrCreateKotlinClass(PlayList.class));
                        it = playlist.iterator();
                        NetflixMirrorProvider netflixMirrorProvider6 = this;
                        String data8 = data3;
                        Function1 subtitleCallback8 = subtitleCallback2;
                        Function1 subtitleCallback9 = callback2;
                        String title9 = title;
                        String title10 = id;
                        Map cookies5 = cookies;
                        String cookieStr5 = cookieStr;
                        playlistHeaders2 = playlistHeaders;
                        obj3 = obj2;
                        $completion = continuation;
                        if (!it.hasNext()) {
                            item2 = it.next();
                            netflixMirrorProvider3 = netflixMirrorProvider6;
                            netflixMirrorProvider$loadLinks$13 = netflixMirrorProvider$loadLinks$12;
                            data6 = data8;
                            subtitleCallback5 = subtitleCallback8;
                            subtitleCallback4 = subtitleCallback9;
                            title4 = title9;
                            title3 = title10;
                            cookies2 = cookies5;
                            cookieStr3 = cookieStr5;
                            it2 = item2.getSources().iterator();
                            $result5 = $result2;
                            if (it2.hasNext()) {
                                Source source = it2.next();
                                String name = netflixMirrorProvider3.getName();
                                String label = source.getLabel();
                                Continuation $completion4 = $completion;
                                PlayList playlist5 = playlist;
                                String str = netflixMirrorProvider3.getMainUrl() + source.getFile();
                                ExtractorLinkType extractorLinkType = ExtractorLinkType.M3U8;
                                netflixMirrorProvider2 = netflixMirrorProvider3;
                                String cookieStr6 = cookieStr3;
                                NetflixMirrorProvider$loadLinks$3 netflixMirrorProvider$loadLinks$3 = new NetflixMirrorProvider$loadLinks$3(playlistHeaders2, netflixMirrorProvider2, source, null);
                                NetflixMirrorProvider$loadLinks$1 netflixMirrorProvider$loadLinks$16 = netflixMirrorProvider$loadLinks$13;
                                netflixMirrorProvider$loadLinks$16.L$0 = SpillingKt.nullOutSpilledVariable(data6);
                                netflixMirrorProvider$loadLinks$16.L$1 = subtitleCallback5;
                                netflixMirrorProvider$loadLinks$16.L$2 = subtitleCallback4;
                                netflixMirrorProvider$loadLinks$16.L$3 = SpillingKt.nullOutSpilledVariable(title4);
                                netflixMirrorProvider$loadLinks$16.L$4 = SpillingKt.nullOutSpilledVariable(title3);
                                netflixMirrorProvider$loadLinks$16.L$5 = SpillingKt.nullOutSpilledVariable(cookies2);
                                netflixMirrorProvider$loadLinks$16.L$6 = SpillingKt.nullOutSpilledVariable(cookieStr6);
                                netflixMirrorProvider$loadLinks$16.L$7 = playlistHeaders2;
                                netflixMirrorProvider$loadLinks$16.L$8 = SpillingKt.nullOutSpilledVariable(playlist5);
                                netflixMirrorProvider$loadLinks$16.L$9 = it;
                                netflixMirrorProvider$loadLinks$16.L$10 = item2;
                                netflixMirrorProvider$loadLinks$16.L$11 = it2;
                                netflixMirrorProvider$loadLinks$16.L$12 = SpillingKt.nullOutSpilledVariable(source);
                                netflixMirrorProvider$loadLinks$16.L$13 = subtitleCallback4;
                                netflixMirrorProvider$loadLinks$16.L$14 = null;
                                netflixMirrorProvider$loadLinks$16.L$15 = null;
                                netflixMirrorProvider$loadLinks$16.L$16 = null;
                                netflixMirrorProvider$loadLinks$16.L$17 = null;
                                netflixMirrorProvider$loadLinks$16.L$18 = null;
                                netflixMirrorProvider$loadLinks$16.Z$0 = isCasting3;
                                netflixMirrorProvider$loadLinks$16.label = 3;
                                obj5 = ExtractorApiKt.newExtractorLink(name, label, str, extractorLinkType, netflixMirrorProvider$loadLinks$3, netflixMirrorProvider$loadLinks$16);
                                if (obj5 == obj3) {
                                    return obj3;
                                }
                                isCasting4 = isCasting3;
                                data5 = data6;
                                item = item2;
                                $result4 = $result5;
                                netflixMirrorProvider$loadLinks$13 = netflixMirrorProvider$loadLinks$16;
                                $completion2 = $completion4;
                                data4 = title3;
                                title2 = title4;
                                $result3 = subtitleCallback5;
                                playlist2 = playlist5;
                                playlistHeaders3 = playlistHeaders2;
                                subtitleCallback3 = subtitleCallback4;
                                cookieStr2 = cookieStr6;
                                obj4 = obj3;
                                function13 = subtitleCallback3;
                                function13.invoke(obj5);
                                $completion = $completion2;
                                obj3 = obj4;
                                netflixMirrorProvider3 = netflixMirrorProvider2;
                                playlist = playlist2;
                                playlistHeaders2 = playlistHeaders3;
                                cookieStr3 = cookieStr2;
                                subtitleCallback4 = subtitleCallback3;
                                title3 = data4;
                                title4 = title2;
                                subtitleCallback5 = $result3;
                                data6 = data5;
                                item2 = item;
                                isCasting3 = isCasting4;
                                $result5 = $result4;
                                if (it2.hasNext()) {
                                    Continuation $completion5 = $completion;
                                    playlist3 = playlist;
                                    NetflixMirrorProvider$loadLinks$1 netflixMirrorProvider$loadLinks$17 = netflixMirrorProvider$loadLinks$13;
                                    NetflixMirrorProvider netflixMirrorProvider7 = netflixMirrorProvider3;
                                    String cookieStr7 = cookieStr3;
                                    Iterable tracks = item2.getTracks();
                                    if (tracks != null) {
                                        Iterable $this$filter$iv = tracks;
                                        Collection destination$iv$iv3 = new ArrayList();
                                        for (Object element$iv$iv : $this$filter$iv) {
                                            Tracks it5 = (Tracks) element$iv$iv;
                                            boolean isCasting8 = isCasting3;
                                            Iterable $this$filter$iv2 = $this$filter$iv;
                                            if (Intrinsics.areEqual(it5.getKind(), "captions")) {
                                                destination$iv$iv3.add(element$iv$iv);
                                            }
                                            $this$filter$iv = $this$filter$iv2;
                                            isCasting3 = isCasting8;
                                        }
                                        boolean isCasting9 = isCasting3;
                                        Iterable $this$map$iv3 = (List) destination$iv$iv3;
                                        Iterator it6 = $this$map$iv3.iterator();
                                        cookieStr4 = cookieStr7;
                                        $completion3 = $completion5;
                                        $this$map$iv = $this$map$iv3;
                                        $i$f$map = 0;
                                        cookies3 = cookies2;
                                        item$iv$iv = subtitleCallback4;
                                        subtitleCallback6 = subtitleCallback5;
                                        $result2 = $result5;
                                        netflixMirrorProvider$loadLinks$14 = netflixMirrorProvider$loadLinks$17;
                                        $this$mapTo$iv$iv = $this$map$iv3;
                                        title5 = title4;
                                        $result6 = data6;
                                        it3 = it6;
                                        netflixMirrorProvider4 = netflixMirrorProvider7;
                                        title6 = title3;
                                        $i$f$mapTo = 0;
                                        destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv3, 10));
                                        isCasting5 = isCasting9;
                                        if (it3.hasNext()) {
                                            Object item$iv$iv2 = it3.next();
                                            Tracks track = (Tracks) item$iv$iv2;
                                            id2 = title6;
                                            Iterable $this$mapTo$iv$iv3 = $this$mapTo$iv$iv;
                                            String valueOf = String.valueOf(track.getLabel());
                                            String title11 = title5;
                                            String title12 = ExtractorApiKt.httpsify(StringsKt.replace$default(String.valueOf(track.getFile()), "\\", "", false, 4, (Object) null));
                                            PlayListItem item4 = item2;
                                            Object obj7 = obj3;
                                            netflixMirrorProvider$loadLinks$14.L$0 = SpillingKt.nullOutSpilledVariable($result6);
                                            netflixMirrorProvider$loadLinks$14.L$1 = subtitleCallback6;
                                            netflixMirrorProvider$loadLinks$14.L$2 = item$iv$iv;
                                            netflixMirrorProvider$loadLinks$14.L$3 = SpillingKt.nullOutSpilledVariable(title11);
                                            netflixMirrorProvider$loadLinks$14.L$4 = SpillingKt.nullOutSpilledVariable(id2);
                                            netflixMirrorProvider$loadLinks$14.L$5 = SpillingKt.nullOutSpilledVariable(cookies3);
                                            netflixMirrorProvider$loadLinks$14.L$6 = SpillingKt.nullOutSpilledVariable(cookieStr4);
                                            netflixMirrorProvider$loadLinks$14.L$7 = playlistHeaders2;
                                            netflixMirrorProvider$loadLinks$14.L$8 = SpillingKt.nullOutSpilledVariable(playlist3);
                                            netflixMirrorProvider$loadLinks$14.L$9 = it;
                                            netflixMirrorProvider$loadLinks$14.L$10 = SpillingKt.nullOutSpilledVariable(item4);
                                            netflixMirrorProvider$loadLinks$14.L$11 = SpillingKt.nullOutSpilledVariable($this$map$iv);
                                            netflixMirrorProvider$loadLinks$14.L$12 = SpillingKt.nullOutSpilledVariable($this$mapTo$iv$iv3);
                                            netflixMirrorProvider$loadLinks$14.L$13 = destination$iv$iv;
                                            netflixMirrorProvider$loadLinks$14.L$14 = it3;
                                            netflixMirrorProvider$loadLinks$14.L$15 = SpillingKt.nullOutSpilledVariable(item$iv$iv2);
                                            netflixMirrorProvider$loadLinks$14.L$16 = SpillingKt.nullOutSpilledVariable(track);
                                            netflixMirrorProvider$loadLinks$14.L$17 = subtitleCallback6;
                                            netflixMirrorProvider$loadLinks$14.L$18 = destination$iv$iv;
                                            netflixMirrorProvider$loadLinks$14.Z$0 = isCasting5;
                                            netflixMirrorProvider$loadLinks$14.I$0 = $i$f$map;
                                            netflixMirrorProvider$loadLinks$14.I$1 = $i$f$mapTo;
                                            netflixMirrorProvider$loadLinks$14.I$2 = 0;
                                            netflixMirrorProvider$loadLinks$14.label = 4;
                                            Object newSubtitleFile = MainAPIKt.newSubtitleFile(valueOf, title12, new NetflixMirrorProvider$loadLinks$5$1(netflixMirrorProvider4, null), netflixMirrorProvider$loadLinks$14);
                                            if (newSubtitleFile == obj7) {
                                                return obj7;
                                            }
                                            NetflixMirrorProvider$loadLinks$1 netflixMirrorProvider$loadLinks$18 = netflixMirrorProvider$loadLinks$14;
                                            callback3 = item$iv$iv;
                                            it4 = it3;
                                            netflixMirrorProvider$loadLinks$15 = netflixMirrorProvider$loadLinks$18;
                                            cookies4 = cookies3;
                                            collection = destination$iv$iv;
                                            $i$f$mapTo2 = $i$f$mapTo;
                                            data7 = $result6;
                                            $result7 = $result2;
                                            item3 = item4;
                                            $this$mapTo$iv$iv2 = $this$mapTo$iv$iv3;
                                            title7 = title11;
                                            playlist4 = playlist3;
                                            title8 = cookieStr4;
                                            playlistHeaders4 = playlistHeaders2;
                                            netflixMirrorProvider5 = netflixMirrorProvider4;
                                            $result = newSubtitleFile;
                                            obj6 = obj7;
                                            $i$f$map2 = $i$f$map;
                                            function14 = subtitleCallback6;
                                            isCasting6 = isCasting5;
                                            destination$iv$iv2 = collection;
                                            subtitleCallback7 = function14;
                                            $this$map$iv2 = $this$map$iv;
                                            function14.invoke($result);
                                            collection.add(Unit.INSTANCE);
                                            $this$map$iv = $this$map$iv2;
                                            subtitleCallback6 = subtitleCallback7;
                                            obj3 = obj6;
                                            destination$iv$iv = destination$iv$iv2;
                                            item$iv$iv = callback3;
                                            netflixMirrorProvider4 = netflixMirrorProvider5;
                                            $i$f$map = $i$f$map2;
                                            isCasting5 = isCasting6;
                                            netflixMirrorProvider$loadLinks$14 = netflixMirrorProvider$loadLinks$15;
                                            $i$f$mapTo = $i$f$mapTo2;
                                            $this$mapTo$iv$iv = $this$mapTo$iv$iv2;
                                            it3 = it4;
                                            title6 = id2;
                                            item2 = item3;
                                            playlist3 = playlist4;
                                            playlistHeaders2 = playlistHeaders4;
                                            cookieStr4 = title8;
                                            cookies3 = cookies4;
                                            title5 = title7;
                                            $result6 = data7;
                                            $result2 = $result7;
                                            if (it3.hasNext()) {
                                                String id3 = title6;
                                                String title13 = title5;
                                                ArrayList arrayList = (List) destination$iv$iv;
                                                $completion = $completion3;
                                                playlist = playlist3;
                                                subtitleCallback8 = subtitleCallback6;
                                                isCasting3 = isCasting5;
                                                subtitleCallback9 = item$iv$iv;
                                                netflixMirrorProvider6 = netflixMirrorProvider4;
                                                netflixMirrorProvider$loadLinks$12 = netflixMirrorProvider$loadLinks$14;
                                                data8 = $result6;
                                                title10 = id3;
                                                title9 = title13;
                                                cookieStr5 = cookieStr4;
                                                cookies5 = cookies3;
                                                if (!it.hasNext()) {
                                                    return Boxing.boxBoolean(z);
                                                }
                                            }
                                        }
                                    } else {
                                        cookieStr5 = cookieStr7;
                                        $completion = $completion5;
                                        cookies5 = cookies2;
                                        title10 = title3;
                                        title9 = title4;
                                        subtitleCallback9 = subtitleCallback4;
                                        subtitleCallback8 = subtitleCallback5;
                                        data8 = data6;
                                        $result2 = $result5;
                                        netflixMirrorProvider$loadLinks$12 = netflixMirrorProvider$loadLinks$17;
                                        netflixMirrorProvider6 = netflixMirrorProvider7;
                                        playlist = playlist3;
                                        if (!it.hasNext()) {
                                        }
                                    }
                                }
                            }
                        }
                    case 3:
                        boolean isCasting10 = netflixMirrorProvider$loadLinks$1.Z$0;
                        function13 = (Function1) netflixMirrorProvider$loadLinks$1.L$13;
                        Source source2 = (Source) netflixMirrorProvider$loadLinks$1.L$12;
                        it2 = (Iterator) netflixMirrorProvider$loadLinks$1.L$11;
                        PlayListItem item5 = (PlayListItem) netflixMirrorProvider$loadLinks$1.L$10;
                        playlist2 = (PlayList) netflixMirrorProvider$loadLinks$1.L$8;
                        playlistHeaders3 = (Map) netflixMirrorProvider$loadLinks$1.L$7;
                        cookieStr2 = (String) netflixMirrorProvider$loadLinks$1.L$6;
                        data4 = (String) netflixMirrorProvider$loadLinks$1.L$4;
                        title2 = (String) netflixMirrorProvider$loadLinks$1.L$3;
                        isCasting4 = isCasting10;
                        Object callback4 = (Function1) netflixMirrorProvider$loadLinks$1.L$2;
                        Object subtitleCallback10 = (Function1) netflixMirrorProvider$loadLinks$1.L$1;
                        ResultKt.throwOnFailure($result);
                        $result3 = subtitleCallback10;
                        data5 = (String) netflixMirrorProvider$loadLinks$1.L$0;
                        netflixMirrorProvider$loadLinks$13 = netflixMirrorProvider$loadLinks$1;
                        $result4 = $result;
                        obj4 = coroutine_suspended;
                        item = item5;
                        it = (Iterator) netflixMirrorProvider$loadLinks$1.L$9;
                        cookies2 = (Map) netflixMirrorProvider$loadLinks$1.L$5;
                        z = true;
                        subtitleCallback3 = callback4;
                        $completion2 = continuation;
                        netflixMirrorProvider2 = this;
                        obj5 = $result4;
                        function13.invoke(obj5);
                        $completion = $completion2;
                        obj3 = obj4;
                        netflixMirrorProvider3 = netflixMirrorProvider2;
                        playlist = playlist2;
                        playlistHeaders2 = playlistHeaders3;
                        cookieStr3 = cookieStr2;
                        subtitleCallback4 = subtitleCallback3;
                        title3 = data4;
                        title4 = title2;
                        subtitleCallback5 = $result3;
                        data6 = data5;
                        item2 = item;
                        isCasting3 = isCasting4;
                        $result5 = $result4;
                        if (it2.hasNext()) {
                        }
                        break;
                    case 4:
                        int i = netflixMirrorProvider$loadLinks$1.I$2;
                        int $i$f$mapTo3 = netflixMirrorProvider$loadLinks$1.I$1;
                        $i$f$map2 = netflixMirrorProvider$loadLinks$1.I$0;
                        isCasting6 = netflixMirrorProvider$loadLinks$1.Z$0;
                        Tracks tracks2 = (Tracks) netflixMirrorProvider$loadLinks$1.L$16;
                        Object obj8 = netflixMirrorProvider$loadLinks$1.L$15;
                        Iterable $this$mapTo$iv$iv4 = (Iterable) netflixMirrorProvider$loadLinks$1.L$12;
                        Iterable $this$map$iv4 = (Iterable) netflixMirrorProvider$loadLinks$1.L$11;
                        PlayListItem item6 = (PlayListItem) netflixMirrorProvider$loadLinks$1.L$10;
                        Iterator<PlayListItem> it7 = (Iterator) netflixMirrorProvider$loadLinks$1.L$9;
                        playlist4 = (PlayList) netflixMirrorProvider$loadLinks$1.L$8;
                        playlistHeaders4 = (Map) netflixMirrorProvider$loadLinks$1.L$7;
                        title8 = (String) netflixMirrorProvider$loadLinks$1.L$6;
                        cookies4 = (Map) netflixMirrorProvider$loadLinks$1.L$5;
                        String id4 = (String) netflixMirrorProvider$loadLinks$1.L$4;
                        title7 = (String) netflixMirrorProvider$loadLinks$1.L$3;
                        Function1 callback5 = (Function1) netflixMirrorProvider$loadLinks$1.L$2;
                        Function1 subtitleCallback11 = (Function1) netflixMirrorProvider$loadLinks$1.L$1;
                        ResultKt.throwOnFailure($result);
                        $completion3 = continuation;
                        data7 = (String) netflixMirrorProvider$loadLinks$1.L$0;
                        $result7 = $result;
                        $this$mapTo$iv$iv2 = $this$mapTo$iv$iv4;
                        it4 = (Iterator) netflixMirrorProvider$loadLinks$1.L$14;
                        z = true;
                        netflixMirrorProvider5 = this;
                        obj6 = coroutine_suspended;
                        collection = (Collection) netflixMirrorProvider$loadLinks$1.L$18;
                        function14 = (Function1) netflixMirrorProvider$loadLinks$1.L$17;
                        it = it7;
                        netflixMirrorProvider$loadLinks$15 = netflixMirrorProvider$loadLinks$1;
                        $i$f$mapTo2 = $i$f$mapTo3;
                        item3 = item6;
                        id2 = id4;
                        subtitleCallback7 = subtitleCallback11;
                        callback3 = callback5;
                        $this$map$iv2 = $this$map$iv4;
                        destination$iv$iv2 = (Collection) netflixMirrorProvider$loadLinks$1.L$13;
                        function14.invoke($result);
                        collection.add(Unit.INSTANCE);
                        $this$map$iv = $this$map$iv2;
                        subtitleCallback6 = subtitleCallback7;
                        obj3 = obj6;
                        destination$iv$iv = destination$iv$iv2;
                        item$iv$iv = callback3;
                        netflixMirrorProvider4 = netflixMirrorProvider5;
                        $i$f$map = $i$f$map2;
                        isCasting5 = isCasting6;
                        netflixMirrorProvider$loadLinks$14 = netflixMirrorProvider$loadLinks$15;
                        $i$f$mapTo = $i$f$mapTo2;
                        $this$mapTo$iv$iv = $this$mapTo$iv$iv2;
                        it3 = it4;
                        title6 = id2;
                        item2 = item3;
                        playlist3 = playlist4;
                        playlistHeaders2 = playlistHeaders4;
                        cookieStr4 = title8;
                        cookies3 = cookies4;
                        title5 = title7;
                        $result6 = data7;
                        $result2 = $result7;
                        if (it3.hasNext()) {
                        }
                        break;
                    default:
                        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }
                netflixMirrorProvider.cookie_value = (String) obj;
                String text$iv = data2;
                LoadData loadData = (LoadData) UtilsKt.getJSONParser().parse(text$iv, Reflection.getOrCreateKotlinClass(LoadData.class));
                String title14 = loadData.component1();
                String id5 = loadData.component2();
                Map cookies6 = MapsKt.mapOf(new Pair[]{TuplesKt.to("t_hash_t", this.cookie_value), TuplesKt.to("ott", "nf"), TuplesKt.to("hd", "on")});
                String cookieStr8 = CollectionsKt.joinToString$default(cookies6.entrySet(), "; ", (CharSequence) null, (CharSequence) null, 0, (CharSequence) null, new Function1() { // from class: com.horis.cncverse.NetflixMirrorProvider$$ExternalSyntheticLambda8
                    public final Object invoke(Object obj9) {
                        CharSequence loadLinks$lambda$1;
                        loadLinks$lambda$1 = NetflixMirrorProvider.loadLinks$lambda$1((Map.Entry) obj9);
                        return loadLinks$lambda$1;
                    }
                }, 30, (Object) null);
                Pair[] pairArr = new Pair[13];
                pairArr[0] = TuplesKt.to("Accept", "*/*");
                z = true;
                pairArr[1] = TuplesKt.to("Accept-Language", "en-IN,en-US;q=0.9,en;q=0.8");
                pairArr[2] = TuplesKt.to("Connection", "keep-alive");
                pairArr[3] = TuplesKt.to("Cookie", cookieStr8);
                pairArr[c2] = TuplesKt.to("Referer", getMainUrl() + "/mobile/home?app=1");
                pairArr[5] = TuplesKt.to("sec-ch-ua", "\"Android WebView\";v=\"149\", \"Chromium\";v=\"149\", \"Not)A;Brand\";v=\"24\"");
                pairArr[6] = TuplesKt.to("sec-ch-ua-mobile", "?0");
                pairArr[7] = TuplesKt.to("sec-ch-ua-platform", "\"Android\"");
                pairArr[8] = TuplesKt.to("Sec-Fetch-Dest", "empty");
                pairArr[9] = TuplesKt.to("Sec-Fetch-Mode", "cors");
                pairArr[c] = TuplesKt.to("Sec-Fetch-Site", "same-origin");
                pairArr[11] = TuplesKt.to("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/149.0.7827.91 Safari/537.36 /OS.Gatu v3.0");
                pairArr[12] = TuplesKt.to("X-Requested-With", "app.netmirror.netmirrornew");
                Map playlistHeaders5 = MapsKt.mapOf(pairArr);
                String data9 = data2;
                netflixMirrorProvider$loadLinks$1.L$0 = SpillingKt.nullOutSpilledVariable(data9);
                netflixMirrorProvider$loadLinks$1.L$1 = subtitleCallback;
                netflixMirrorProvider$loadLinks$1.L$2 = callback;
                netflixMirrorProvider$loadLinks$1.L$3 = SpillingKt.nullOutSpilledVariable(title14);
                netflixMirrorProvider$loadLinks$1.L$4 = SpillingKt.nullOutSpilledVariable(id5);
                netflixMirrorProvider$loadLinks$1.L$5 = SpillingKt.nullOutSpilledVariable(cookies6);
                netflixMirrorProvider$loadLinks$1.L$6 = SpillingKt.nullOutSpilledVariable(cookieStr8);
                netflixMirrorProvider$loadLinks$1.L$7 = playlistHeaders5;
                netflixMirrorProvider$loadLinks$1.Z$0 = isCasting2;
                netflixMirrorProvider$loadLinks$1.label = 2;
                boolean isCasting11 = isCasting2;
                Function1 subtitleCallback12 = subtitleCallback;
                playlistHeaders = playlistHeaders5;
                obj2 = coroutine_suspended;
                Object callback6 = callback;
                netflixMirrorProvider$loadLinks$12 = netflixMirrorProvider$loadLinks$1;
                $result = Requests.get$default(UtilsKt.getApp(), getMainUrl() + "/mobile/playlist.php?id=" + id5 + "&t=" + title14 + "&tm=" + APIHolder.INSTANCE.getUnixTime(), playlistHeaders, getMainUrl() + "/mobile/home?app=1", (Map) null, cookies6, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, netflixMirrorProvider$loadLinks$12, 4072, (Object) null);
                if ($result != obj2) {
                    return obj2;
                }
                data3 = data9;
                callback2 = callback6;
                cookies = cookies6;
                id = id5;
                subtitleCallback2 = subtitleCallback12;
                isCasting3 = isCasting11;
                title = title14;
                cookieStr = cookieStr8;
                NiceResponse this_$iv2 = (NiceResponse) $result;
                ResponseParser parser2 = this_$iv2.getParser();
                Intrinsics.checkNotNull(parser2);
                playlist = (PlayList) parser2.parse(this_$iv2.getText(), Reflection.getOrCreateKotlinClass(PlayList.class));
                it = playlist.iterator();
                NetflixMirrorProvider netflixMirrorProvider62 = this;
                String data82 = data3;
                Function1 subtitleCallback82 = subtitleCallback2;
                Function1 subtitleCallback92 = callback2;
                String title92 = title;
                String title102 = id;
                Map cookies52 = cookies;
                String cookieStr52 = cookieStr;
                playlistHeaders2 = playlistHeaders;
                obj3 = obj2;
                $completion = continuation;
                if (!it.hasNext()) {
                }
            }
        }
        netflixMirrorProvider$loadLinks$1 = new NetflixMirrorProvider$loadLinks$1(this, continuation);
        $result = netflixMirrorProvider$loadLinks$1.result;
        Object coroutine_suspended2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
        switch (netflixMirrorProvider$loadLinks$1.label) {
        }
        netflixMirrorProvider.cookie_value = (String) obj;
        String text$iv2 = data2;
        LoadData loadData2 = (LoadData) UtilsKt.getJSONParser().parse(text$iv2, Reflection.getOrCreateKotlinClass(LoadData.class));
        String title142 = loadData2.component1();
        String id52 = loadData2.component2();
        Map cookies62 = MapsKt.mapOf(new Pair[]{TuplesKt.to("t_hash_t", this.cookie_value), TuplesKt.to("ott", "nf"), TuplesKt.to("hd", "on")});
        String cookieStr82 = CollectionsKt.joinToString$default(cookies62.entrySet(), "; ", (CharSequence) null, (CharSequence) null, 0, (CharSequence) null, new Function1() { // from class: com.horis.cncverse.NetflixMirrorProvider$$ExternalSyntheticLambda8
            public final Object invoke(Object obj9) {
                CharSequence loadLinks$lambda$1;
                loadLinks$lambda$1 = NetflixMirrorProvider.loadLinks$lambda$1((Map.Entry) obj9);
                return loadLinks$lambda$1;
            }
        }, 30, (Object) null);
        Pair[] pairArr2 = new Pair[13];
        pairArr2[0] = TuplesKt.to("Accept", "*/*");
        z = true;
        pairArr2[1] = TuplesKt.to("Accept-Language", "en-IN,en-US;q=0.9,en;q=0.8");
        pairArr2[2] = TuplesKt.to("Connection", "keep-alive");
        pairArr2[3] = TuplesKt.to("Cookie", cookieStr82);
        pairArr2[c2] = TuplesKt.to("Referer", getMainUrl() + "/mobile/home?app=1");
        pairArr2[5] = TuplesKt.to("sec-ch-ua", "\"Android WebView\";v=\"149\", \"Chromium\";v=\"149\", \"Not)A;Brand\";v=\"24\"");
        pairArr2[6] = TuplesKt.to("sec-ch-ua-mobile", "?0");
        pairArr2[7] = TuplesKt.to("sec-ch-ua-platform", "\"Android\"");
        pairArr2[8] = TuplesKt.to("Sec-Fetch-Dest", "empty");
        pairArr2[9] = TuplesKt.to("Sec-Fetch-Mode", "cors");
        pairArr2[c] = TuplesKt.to("Sec-Fetch-Site", "same-origin");
        pairArr2[11] = TuplesKt.to("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/149.0.7827.91 Safari/537.36 /OS.Gatu v3.0");
        pairArr2[12] = TuplesKt.to("X-Requested-With", "app.netmirror.netmirrornew");
        Map playlistHeaders52 = MapsKt.mapOf(pairArr2);
        String data92 = data2;
        netflixMirrorProvider$loadLinks$1.L$0 = SpillingKt.nullOutSpilledVariable(data92);
        netflixMirrorProvider$loadLinks$1.L$1 = subtitleCallback;
        netflixMirrorProvider$loadLinks$1.L$2 = callback;
        netflixMirrorProvider$loadLinks$1.L$3 = SpillingKt.nullOutSpilledVariable(title142);
        netflixMirrorProvider$loadLinks$1.L$4 = SpillingKt.nullOutSpilledVariable(id52);
        netflixMirrorProvider$loadLinks$1.L$5 = SpillingKt.nullOutSpilledVariable(cookies62);
        netflixMirrorProvider$loadLinks$1.L$6 = SpillingKt.nullOutSpilledVariable(cookieStr82);
        netflixMirrorProvider$loadLinks$1.L$7 = playlistHeaders52;
        netflixMirrorProvider$loadLinks$1.Z$0 = isCasting2;
        netflixMirrorProvider$loadLinks$1.label = 2;
        boolean isCasting112 = isCasting2;
        Function1 subtitleCallback122 = subtitleCallback;
        playlistHeaders = playlistHeaders52;
        obj2 = coroutine_suspended2;
        Object callback62 = callback;
        netflixMirrorProvider$loadLinks$12 = netflixMirrorProvider$loadLinks$1;
        $result = Requests.get$default(UtilsKt.getApp(), getMainUrl() + "/mobile/playlist.php?id=" + id52 + "&t=" + title142 + "&tm=" + APIHolder.INSTANCE.getUnixTime(), playlistHeaders, getMainUrl() + "/mobile/home?app=1", (Map) null, cookies62, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, netflixMirrorProvider$loadLinks$12, 4072, (Object) null);
        if ($result != obj2) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void loadLinks$lambda$0$0(Context $_ctx) {
        Toast.makeText($_ctx, "⚠️(Opening ads) Subscription expired. If you have renewed your subscription, please re-verify it in Subscription Manager.", 1).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final CharSequence loadLinks$lambda$1(Map.Entry it) {
        return ((String) it.getKey()) + '=' + ((String) it.getValue());
    }

    @Nullable
    public Interceptor getVideoInterceptor(@NotNull ExtractorLink extractorLink) {
        return new Interceptor() { // from class: com.horis.cncverse.NetflixMirrorProvider$getVideoInterceptor$1
            public Response intercept(Interceptor.Chain chain) {
                Request request = chain.request();
                if (StringsKt.contains$default(request.url().toString(), ".m3u8", false, 2, (Object) null)) {
                    Request newRequest = request.newBuilder().header("Cookie", "hd=on").build();
                    return chain.proceed(newRequest);
                }
                return chain.proceed(request);
            }
        };
    }

    /* compiled from: NetflixMirrorProvider.kt */
    @Metadata(d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\t\u0010\b\u001a\u00020\u0003HÆ\u0003J\u0013\u0010\t\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003HÆ\u0001J\u0014\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\u0001HÖ\u0083\u0004J\n\u0010\r\u001a\u00020\u000eHÖ\u0081\u0004J\n\u0010\u000f\u001a\u00020\u0003HÖ\u0081\u0004R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007¨\u0006\u0010"}, d2 = {"Lcom/horis/cncverse/NetflixMirrorProvider$Id;", "", "id", "", "<init>", "(Ljava/lang/String;)V", "getId", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "hashCode", "", "toString", "CNC Verse Mobile_debug"}, k = 1, mv = {2, 3, 0}, xi = 48)
    /* loaded from: /sdcard/AndroidIDEProjects/Cloudstream reference/cncverse/CNC Verse Mobile/resources/classes.dex */
    public static final class Id {
        @NotNull
        private final String id;

        public static /* synthetic */ Id copy$default(Id id, String str, int i, Object obj) {
            if ((i & 1) != 0) {
                str = id.id;
            }
            return id.copy(str);
        }

        @NotNull
        public final String component1() {
            return this.id;
        }

        @NotNull
        public final Id copy(@NotNull String str) {
            return new Id(str);
        }

        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            return (obj instanceof Id) && Intrinsics.areEqual(this.id, ((Id) obj).id);
        }

        public int hashCode() {
            return this.id.hashCode();
        }

        @NotNull
        public String toString() {
            return "Id(id=" + this.id + ')';
        }

        public Id(@NotNull String id) {
            this.id = id;
        }

        @NotNull
        public final String getId() {
            return this.id;
        }
    }

    /* compiled from: NetflixMirrorProvider.kt */
    @Metadata(d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\n\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u0017\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003¢\u0006\u0004\b\u0005\u0010\u0006J\t\u0010\n\u001a\u00020\u0003HÆ\u0003J\t\u0010\u000b\u001a\u00020\u0003HÆ\u0003J\u001d\u0010\f\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0003HÆ\u0001J\u0014\u0010\r\u001a\u00020\u000e2\b\u0010\u000f\u001a\u0004\u0018\u00010\u0001HÖ\u0083\u0004J\n\u0010\u0010\u001a\u00020\u0011HÖ\u0081\u0004J\n\u0010\u0012\u001a\u00020\u0003HÖ\u0081\u0004R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0004\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\t\u0010\b¨\u0006\u0013"}, d2 = {"Lcom/horis/cncverse/NetflixMirrorProvider$LoadData;", "", "title", "", "id", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V", "getTitle", "()Ljava/lang/String;", "getId", "component1", "component2", "copy", "equals", "", "other", "hashCode", "", "toString", "CNC Verse Mobile_debug"}, k = 1, mv = {2, 3, 0}, xi = 48)
    /* loaded from: /sdcard/AndroidIDEProjects/Cloudstream reference/cncverse/CNC Verse Mobile/resources/classes.dex */
    public static final class LoadData {
        @NotNull
        private final String id;
        @NotNull
        private final String title;

        public static /* synthetic */ LoadData copy$default(LoadData loadData, String str, String str2, int i, Object obj) {
            if ((i & 1) != 0) {
                str = loadData.title;
            }
            if ((i & 2) != 0) {
                str2 = loadData.id;
            }
            return loadData.copy(str, str2);
        }

        @NotNull
        public final String component1() {
            return this.title;
        }

        @NotNull
        public final String component2() {
            return this.id;
        }

        @NotNull
        public final LoadData copy(@NotNull String str, @NotNull String str2) {
            return new LoadData(str, str2);
        }

        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof LoadData) {
                LoadData loadData = (LoadData) obj;
                return Intrinsics.areEqual(this.title, loadData.title) && Intrinsics.areEqual(this.id, loadData.id);
            }
            return false;
        }

        public int hashCode() {
            return (this.title.hashCode() * 31) + this.id.hashCode();
        }

        @NotNull
        public String toString() {
            return "LoadData(title=" + this.title + ", id=" + this.id + ')';
        }

        public LoadData(@NotNull String title, @NotNull String id) {
            this.title = title;
            this.id = id;
        }

        @NotNull
        public final String getId() {
            return this.id;
        }

        @NotNull
        public final String getTitle() {
            return this.title;
        }
    }
}
