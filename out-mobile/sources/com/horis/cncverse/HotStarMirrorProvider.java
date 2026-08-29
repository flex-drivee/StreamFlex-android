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
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.horis.cncverse.HotStarMirrorProvider;
import com.horis.cncverse.entities.PostData;
import com.horis.cncverse.entities.SearchResult;
import com.horis.cncverse.entities.Suggest;
import com.lagradost.cloudstream3.AnimeSearchResponse;
import com.lagradost.cloudstream3.Episode;
import com.lagradost.cloudstream3.HomePageList;
import com.lagradost.cloudstream3.MainAPI;
import com.lagradost.cloudstream3.MainAPIKt;
import com.lagradost.cloudstream3.SearchResponse;
import com.lagradost.cloudstream3.TvType;
import com.lagradost.cloudstream3.ui.settings.Globals;
import com.lagradost.cloudstream3.utils.AppUtils;
import com.lagradost.cloudstream3.utils.ExtractorLink;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kotlin.Metadata;
import kotlin.Pair;
import kotlin.TuplesKt;
import kotlin.Unit;
import kotlin.collections.MapsKt;
import kotlin.collections.SetsKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.text.StringsKt;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;
/* compiled from: HotStarMirrorProvider.kt */
@Metadata(d1 = {"\u0000\u0088\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\"\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u000b\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010$\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\b\u0018\u0000 E2\u00020\u0001:\u0003EFGB\u0007¢\u0006\u0004\b\u0002\u0010\u0003J \u0010\u001c\u001a\u0004\u0018\u00010\u001d2\u0006\u0010\u001e\u001a\u00020\u001f2\u0006\u0010 \u001a\u00020!H\u0096@¢\u0006\u0002\u0010\"J\f\u0010#\u001a\u00020$*\u00020%H\u0002J\u000e\u0010&\u001a\u0004\u0018\u00010'*\u00020%H\u0002J\u001c\u0010(\u001a\b\u0012\u0004\u0012\u00020'0)2\u0006\u0010*\u001a\u00020\nH\u0096@¢\u0006\u0002\u0010+J\u0018\u0010,\u001a\u0004\u0018\u00010-2\u0006\u0010.\u001a\u00020\nH\u0096@¢\u0006\u0002\u0010+J4\u0010/\u001a\b\u0012\u0004\u0012\u0002000)2\u0006\u00101\u001a\u00020\n2\u0006\u00102\u001a\u00020\n2\u0006\u00103\u001a\u00020\n2\u0006\u0010\u001e\u001a\u00020\u001fH\u0082@¢\u0006\u0002\u00104JF\u00105\u001a\u00020\u00162\u0006\u00106\u001a\u00020\n2\u0006\u00107\u001a\u00020\u00162\u0012\u00108\u001a\u000e\u0012\u0004\u0012\u00020:\u0012\u0004\u0012\u00020;092\u0012\u0010<\u001a\u000e\u0012\u0004\u0012\u00020=\u0012\u0004\u0012\u00020;09H\u0096@¢\u0006\u0002\u0010>J\u0012\u0010?\u001a\u0004\u0018\u00010@2\u0006\u0010A\u001a\u00020=H\u0016J\b\u0010B\u001a\u00020;H\u0002J\b\u0010C\u001a\u00020;H\u0002J\u0010\u0010D\u001a\u00020;2\u0006\u0010.\u001a\u00020\nH\u0002R\u001a\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005X\u0096\u0004¢\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u001a\u0010\t\u001a\u00020\nX\u0096\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000eR\u001a\u0010\u000f\u001a\u00020\nX\u0096\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u0010\u0010\f\"\u0004\b\u0011\u0010\u000eR\u001a\u0010\u0012\u001a\u00020\nX\u0096\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u0013\u0010\f\"\u0004\b\u0014\u0010\u000eR\u0014\u0010\u0015\u001a\u00020\u0016X\u0096D¢\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u000e\u0010\u0019\u001a\u00020\nX\u0082\u000e¢\u0006\u0002\n\u0000R\u001a\u0010\u001a\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\n0\u001bX\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006H"}, d2 = {"Lcom/horis/cncverse/HotStarMirrorProvider;", "Lcom/lagradost/cloudstream3/MainAPI;", "<init>", "()V", "supportedTypes", "", "Lcom/lagradost/cloudstream3/TvType;", "getSupportedTypes", "()Ljava/util/Set;", "lang", "", "getLang", "()Ljava/lang/String;", "setLang", "(Ljava/lang/String;)V", "mainUrl", "getMainUrl", "setMainUrl", "name", "getName", "setName", "hasMainPage", "", "getHasMainPage", "()Z", "cookie_value", "headers", "", "getMainPage", "Lcom/lagradost/cloudstream3/HomePageResponse;", "page", "", "request", "Lcom/lagradost/cloudstream3/MainPageRequest;", "(ILcom/lagradost/cloudstream3/MainPageRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "toHomePageList", "Lcom/lagradost/cloudstream3/HomePageList;", "Lorg/jsoup/nodes/Element;", "toSearchResult", "Lcom/lagradost/cloudstream3/SearchResponse;", "search", "", "query", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "load", "Lcom/lagradost/cloudstream3/LoadResponse;", "url", "getEpisodes", "Lcom/lagradost/cloudstream3/Episode;", "title", "eid", "sid", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "loadLinks", "data", "isCasting", "subtitleCallback", "Lkotlin/Function1;", "Lcom/lagradost/cloudstream3/SubtitleFile;", "", "callback", "Lcom/lagradost/cloudstream3/utils/ExtractorLink;", "(Ljava/lang/String;ZLkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getVideoInterceptor", "Lokhttp3/Interceptor;", "extractorLink", "showSubscriptionPopupIfNeeded", "showTelegramPopup", "openInExternalBrowser", "Companion", "Id", "LoadData", "CNC Verse Mobile_debug"}, k = 1, mv = {2, 3, 0}, xi = 48)
@SourceDebugExtension({"SMAP\nHotStarMirrorProvider.kt\nKotlin\n*S Kotlin\n*F\n+ 1 HotStarMirrorProvider.kt\ncom/horis/cncverse/HotStarMirrorProvider\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n+ 4 NiceResponse.kt\ncom/lagradost/nicehttp/NiceResponse\n+ 5 Utils.kt\ncom/horis/cncverse/UtilsKt\n*L\n1#1,616:1\n1586#2:617\n1661#2,3:618\n1642#2,10:621\n1915#2:631\n1916#2:633\n1652#2:634\n1586#2:636\n1661#2,3:637\n1586#2:642\n1661#2,3:643\n1586#2:646\n1661#2,3:647\n1586#2:650\n1661#2,3:651\n777#2:654\n873#2,2:655\n1586#2:657\n1661#2,3:658\n1661#2,3:661\n1661#2,3:665\n777#2:670\n873#2,2:671\n1586#2:673\n1661#2,3:674\n1#3:632\n1#3:677\n62#4:635\n62#4:641\n62#4:664\n62#4:669\n218#5:640\n218#5:668\n*S KotlinDebug\n*F\n+ 1 HotStarMirrorProvider.kt\ncom/horis/cncverse/HotStarMirrorProvider\n*L\n106#1:617\n106#1:618,3\n114#1:621,10\n114#1:631\n114#1:633\n114#1:634\n142#1:636\n142#1:637,3\n169#1:642\n169#1:643,3\n170#1:646\n170#1:647,3\n176#1:650\n176#1:651,3\n177#1:654\n177#1:655,2\n182#1:657\n182#1:658,3\n194#1:661,3\n247#1:665,3\n333#1:670\n333#1:671,2\n333#1:673\n333#1:674,3\n114#1:632\n140#1:635\n164#1:641\n246#1:664\n315#1:669\n153#1:640\n288#1:668\n*E\n"})
/* loaded from: /sdcard/AndroidIDEProjects/Cloudstream reference/cncverse/CNC Verse Mobile/resources/classes.dex */
public final class HotStarMirrorProvider extends MainAPI {
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
    private String name = "HotstarM";
    private final boolean hasMainPage = true;
    @NotNull
    private String cookie_value = "";
    @NotNull
    private final Map<String, String> headers = MapsKt.mapOf(new Pair[]{TuplesKt.to("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"), TuplesKt.to("Accept-Language", "en-IN,en-US;q=0.9,en;q=0.8"), TuplesKt.to("Cache-Control", "max-age=0"), TuplesKt.to("Connection", "keep-alive"), TuplesKt.to("sec-ch-ua", "\"Not(A:Brand\";v=\"8\", \"Chromium\";v=\"144\", \"Android WebView\";v=\"144\""), TuplesKt.to("sec-ch-ua-mobile", "?0"), TuplesKt.to("sec-ch-ua-platform", "\"Android\""), TuplesKt.to("Sec-Fetch-Dest", "document"), TuplesKt.to("Sec-Fetch-Mode", "navigate"), TuplesKt.to("Sec-Fetch-Site", "same-origin"), TuplesKt.to("Sec-Fetch-User", "?1"), TuplesKt.to("Upgrade-Insecure-Requests", "1"), TuplesKt.to("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/144.0.7559.132 Safari/537.36 /OS.Gatu v3.0"), TuplesKt.to("X-Requested-With", "XMLHttpRequest")});

    /* compiled from: HotStarMirrorProvider.kt */
    @Metadata(d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J\u0006\u0010\u0004\u001a\u00020\u0005J\u0006\u0010\u0007\u001a\u00020\u0005J\u0012\u0010\b\u001a\u00020\t2\b\u0010\n\u001a\u0004\u0018\u00010\u000bH\u0002R\u000e\u0010\u0006\u001a\u00020\u0005X\u0082\u000e¢\u0006\u0002\n\u0000R\u001c\u0010\f\u001a\u0004\u0018\u00010\u000bX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\u000e\"\u0004\b\u000f\u0010\u0010R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082T¢\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0005X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0005X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0014X\u0082T¢\u0006\u0002\n\u0000¨\u0006\u0018"}, d2 = {"Lcom/horis/cncverse/HotStarMirrorProvider$Companion;", "", "<init>", "()V", "isCsGuardActive", "", "csGuardWasEverActive", "isCsGuardBlocked", "showCsGuardToast", "", "ctx", "Landroid/content/Context;", "context", "getContext", "()Landroid/content/Context;", "setContext", "(Landroid/content/Context;)V", "OMG10", "", "lastBrowserOpenMs", "", "telegramPopupShown", "subscriptionPopupShown", "BROWSER_DEBOUNCE_MS", "CNC Verse Mobile_debug"}, k = 1, mv = {2, 3, 0}, xi = 48)
    @SourceDebugExtension({"SMAP\nHotStarMirrorProvider.kt\nKotlin\n*S Kotlin\n*F\n+ 1 HotStarMirrorProvider.kt\ncom/horis/cncverse/HotStarMirrorProvider$Companion\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,616:1\n1#2:617\n*E\n"})
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
            To view partially-correct add '--show-bad-code' argument
        */
        public final boolean isCsGuardActive() {
            /*
                r10 = this;
                r0 = 0
                java.lang.String r1 = "android.app.ActivityThread"
                java.lang.Class r1 = java.lang.Class.forName(r1)     // Catch: java.lang.Throwable -> L61
                java.lang.String r2 = "currentActivityThread"
                java.lang.Class[] r3 = new java.lang.Class[r0]     // Catch: java.lang.Throwable -> L61
                java.lang.reflect.Method r2 = r1.getMethod(r2, r3)     // Catch: java.lang.Throwable -> L61
                java.lang.Object[] r3 = new java.lang.Object[r0]     // Catch: java.lang.Throwable -> L61
                r4 = 0
                java.lang.Object r2 = r2.invoke(r4, r3)     // Catch: java.lang.Throwable -> L61
                java.lang.String r3 = "mInstrumentation"
                java.lang.reflect.Field r3 = r1.getDeclaredField(r3)     // Catch: java.lang.Throwable -> L61
                r5 = r3
                r6 = 0
                r7 = 1
                r5.setAccessible(r7)     // Catch: java.lang.Throwable -> L61
                java.lang.Object r5 = r3.get(r2)     // Catch: java.lang.Throwable -> L61
                if (r5 == 0) goto L42
                java.lang.Class r5 = r5.getClass()     // Catch: java.lang.Throwable -> L61
                if (r5 == 0) goto L42
                java.lang.String r5 = r5.getName()     // Catch: java.lang.Throwable -> L61
                if (r5 == 0) goto L42
                java.util.Locale r6 = java.util.Locale.ROOT     // Catch: java.lang.Throwable -> L61
                java.lang.String r5 = r5.toLowerCase(r6)     // Catch: java.lang.Throwable -> L61
                java.lang.String r6 = "toLowerCase(...)"
                kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r5, r6)     // Catch: java.lang.Throwable -> L61
                if (r5 != 0) goto L44
            L42:
                java.lang.String r5 = ""
            L44:
                r6 = r5
                java.lang.CharSequence r6 = (java.lang.CharSequence) r6     // Catch: java.lang.Throwable -> L61
                java.lang.String r8 = "guard"
                java.lang.CharSequence r8 = (java.lang.CharSequence) r8     // Catch: java.lang.Throwable -> L61
                r9 = 2
                boolean r6 = kotlin.text.StringsKt.contains$default(r6, r8, r0, r9, r4)     // Catch: java.lang.Throwable -> L61
                if (r6 != 0) goto L5f
                r6 = r5
                java.lang.CharSequence r6 = (java.lang.CharSequence) r6     // Catch: java.lang.Throwable -> L61
                java.lang.String r8 = "csguard"
                java.lang.CharSequence r8 = (java.lang.CharSequence) r8     // Catch: java.lang.Throwable -> L61
                boolean r4 = kotlin.text.StringsKt.contains$default(r6, r8, r0, r9, r4)     // Catch: java.lang.Throwable -> L61
                if (r4 == 0) goto L62
            L5f:
                r0 = 1
                goto L62
            L61:
                r1 = move-exception
            L62:
                return r0
            */
            throw new UnsupportedOperationException("Method not decompiled: com.horis.cncverse.HotStarMirrorProvider.Companion.isCsGuardActive():boolean");
        }

        public final boolean isCsGuardBlocked() {
            if (isCsGuardActive()) {
                HotStarMirrorProvider.csGuardWasEverActive = true;
            }
            return HotStarMirrorProvider.csGuardWasEverActive;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public final void showCsGuardToast(final Context ctx) {
            if (ctx == null) {
                return;
            }
            new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.horis.cncverse.HotStarMirrorProvider$Companion$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    HotStarMirrorProvider.Companion.showCsGuardToast$lambda$0(ctx);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static final void showCsGuardToast$lambda$0(Context $c) {
            Toast.makeText($c, "🚫 CSGuard detected — Restart CloudStream after removing CSGuard to use CNCRepo", 1).show();
        }

        @Nullable
        public final Context getContext() {
            return HotStarMirrorProvider.context;
        }

        public final void setContext(@Nullable Context context) {
            HotStarMirrorProvider.context = context;
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
    @org.jetbrains.annotations.Nullable
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public java.lang.Object getMainPage(int r26, @org.jetbrains.annotations.NotNull com.lagradost.cloudstream3.MainPageRequest r27, @org.jetbrains.annotations.NotNull kotlin.coroutines.Continuation<? super com.lagradost.cloudstream3.HomePageResponse> r28) {
        /*
            Method dump skipped, instructions count: 408
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.horis.cncverse.HotStarMirrorProvider.getMainPage(int, com.lagradost.cloudstream3.MainPageRequest, kotlin.coroutines.Continuation):java.lang.Object");
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
        return MainAPIKt.newAnimeSearchResponse$default(this, "", AppUtils.INSTANCE.toJson(new Id(id)), (TvType) null, false, new Function1() { // from class: com.horis.cncverse.HotStarMirrorProvider$$ExternalSyntheticLambda14
            public final Object invoke(Object obj) {
                Unit searchResult$lambda$0;
                searchResult$lambda$0 = HotStarMirrorProvider.toSearchResult$lambda$0(id, this, (AnimeSearchResponse) obj);
                return searchResult$lambda$0;
            }
        }, 12, (Object) null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit toSearchResult$lambda$0(String $id, HotStarMirrorProvider this$0, AnimeSearchResponse $this$newAnimeSearchResponse) {
        $this$newAnimeSearchResponse.setPosterUrl("https://imgcdn.kim/hs/v/" + $id + ".jpg");
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
    @org.jetbrains.annotations.Nullable
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public java.lang.Object search(@org.jetbrains.annotations.NotNull java.lang.String r31, @org.jetbrains.annotations.NotNull kotlin.coroutines.Continuation<? super java.util.List<? extends com.lagradost.cloudstream3.SearchResponse>> r32) {
        /*
            Method dump skipped, instructions count: 458
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.horis.cncverse.HotStarMirrorProvider.search(java.lang.String, kotlin.coroutines.Continuation):java.lang.Object");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit search$lambda$0$0(SearchResult $it, HotStarMirrorProvider this$0, AnimeSearchResponse $this$newAnimeSearchResponse) {
        $this$newAnimeSearchResponse.setPosterUrl("https://imgcdn.kim/hs/v/" + $it.getId() + ".jpg");
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
    @org.jetbrains.annotations.Nullable
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public java.lang.Object load(@org.jetbrains.annotations.NotNull java.lang.String r39, @org.jetbrains.annotations.NotNull kotlin.coroutines.Continuation<? super com.lagradost.cloudstream3.LoadResponse> r40) {
        /*
            Method dump skipped, instructions count: 1590
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.horis.cncverse.HotStarMirrorProvider.load(java.lang.String, kotlin.coroutines.Continuation):java.lang.Object");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit load$lambda$4$0(Suggest $it, HotStarMirrorProvider this$0, AnimeSearchResponse $this$newAnimeSearchResponse) {
        $this$newAnimeSearchResponse.setPosterUrl("https://imgcdn.kim/hs/v/" + $it.getId() + ".jpg");
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
        $this$newEpisode.setPosterUrl("https://imgcdn.kim/hsepimg/150/" + $it.getId() + ".jpg");
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
        To view partially-correct add '--show-bad-code' argument
    */
    public final java.lang.Object getEpisodes(java.lang.String r29, java.lang.String r30, java.lang.String r31, int r32, kotlin.coroutines.Continuation<? super java.util.List<com.lagradost.cloudstream3.Episode>> r33) {
        /*
            Method dump skipped, instructions count: 476
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.horis.cncverse.HotStarMirrorProvider.getEpisodes(java.lang.String, java.lang.String, java.lang.String, int, kotlin.coroutines.Continuation):java.lang.Object");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit getEpisodes$lambda$0$0(com.horis.cncverse.entities.Episode $it, Episode $this$newEpisode) {
        $this$newEpisode.setName($it.getT());
        $this$newEpisode.setEpisode(StringsKt.toIntOrNull(StringsKt.replace$default($it.getEp(), "E", "", false, 4, (Object) null)));
        $this$newEpisode.setSeason(StringsKt.toIntOrNull(StringsKt.replace$default($it.getS(), "S", "", false, 4, (Object) null)));
        $this$newEpisode.setPosterUrl("https://imgcdn.kim/hsepimg/" + $it.getId() + ".jpg");
        $this$newEpisode.setRunTime(StringsKt.toIntOrNull(StringsKt.replace$default($it.getTime(), "m", "", false, 4, (Object) null)));
        return Unit.INSTANCE;
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
    @org.jetbrains.annotations.Nullable
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public java.lang.Object loadLinks(@org.jetbrains.annotations.NotNull java.lang.String r39, boolean r40, @org.jetbrains.annotations.NotNull kotlin.jvm.functions.Function1<? super com.lagradost.cloudstream3.SubtitleFile, kotlin.Unit> r41, @org.jetbrains.annotations.NotNull kotlin.jvm.functions.Function1<? super com.lagradost.cloudstream3.utils.ExtractorLink, kotlin.Unit> r42, @org.jetbrains.annotations.NotNull kotlin.coroutines.Continuation<? super java.lang.Boolean> r43) {
        /*
            Method dump skipped, instructions count: 1794
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.horis.cncverse.HotStarMirrorProvider.loadLinks(java.lang.String, boolean, kotlin.jvm.functions.Function1, kotlin.jvm.functions.Function1, kotlin.coroutines.Continuation):java.lang.Object");
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
        return new Interceptor() { // from class: com.horis.cncverse.HotStarMirrorProvider$getVideoInterceptor$1
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

    /* compiled from: HotStarMirrorProvider.kt */
    @Metadata(d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\t\u0010\b\u001a\u00020\u0003HÆ\u0003J\u0013\u0010\t\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003HÆ\u0001J\u0014\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\u0001HÖ\u0083\u0004J\n\u0010\r\u001a\u00020\u000eHÖ\u0081\u0004J\n\u0010\u000f\u001a\u00020\u0003HÖ\u0081\u0004R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007¨\u0006\u0010"}, d2 = {"Lcom/horis/cncverse/HotStarMirrorProvider$Id;", "", "id", "", "<init>", "(Ljava/lang/String;)V", "getId", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "hashCode", "", "toString", "CNC Verse Mobile_debug"}, k = 1, mv = {2, 3, 0}, xi = 48)
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

    /* compiled from: HotStarMirrorProvider.kt */
    @Metadata(d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\n\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u0017\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003¢\u0006\u0004\b\u0005\u0010\u0006J\t\u0010\n\u001a\u00020\u0003HÆ\u0003J\t\u0010\u000b\u001a\u00020\u0003HÆ\u0003J\u001d\u0010\f\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0003HÆ\u0001J\u0014\u0010\r\u001a\u00020\u000e2\b\u0010\u000f\u001a\u0004\u0018\u00010\u0001HÖ\u0083\u0004J\n\u0010\u0010\u001a\u00020\u0011HÖ\u0081\u0004J\n\u0010\u0012\u001a\u00020\u0003HÖ\u0081\u0004R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0004\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\t\u0010\b¨\u0006\u0013"}, d2 = {"Lcom/horis/cncverse/HotStarMirrorProvider$LoadData;", "", "title", "", "id", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V", "getTitle", "()Ljava/lang/String;", "getId", "component1", "component2", "copy", "equals", "", "other", "hashCode", "", "toString", "CNC Verse Mobile_debug"}, k = 1, mv = {2, 3, 0}, xi = 48)
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
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.horis.cncverse.HotStarMirrorProvider$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                HotStarMirrorProvider.showSubscriptionPopupIfNeeded$lambda$0(ctx);
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
            laterTv.setOnClickListener(new View.OnClickListener() { // from class: com.horis.cncverse.HotStarMirrorProvider$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    HotStarMirrorProvider.showSubscriptionPopupIfNeeded$lambda$0$11($this$showSubscriptionPopupIfNeeded_u24lambda_u240_u249, $ctx, dialog, view);
                }
            });
            subscribeTv.setOnClickListener(new View.OnClickListener() { // from class: com.horis.cncverse.HotStarMirrorProvider$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    HotStarMirrorProvider.showSubscriptionPopupIfNeeded$lambda$0$12(dialog, $ctx, view);
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
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.horis.cncverse.HotStarMirrorProvider$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                HotStarMirrorProvider.showTelegramPopup$lambda$0(ctx);
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
            laterTv.setOnClickListener(new View.OnClickListener() { // from class: com.horis.cncverse.HotStarMirrorProvider$$ExternalSyntheticLambda10
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    dialog.dismiss();
                }
            });
            joinTv.setOnClickListener(new View.OnClickListener() { // from class: com.horis.cncverse.HotStarMirrorProvider$$ExternalSyntheticLambda11
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    HotStarMirrorProvider.showTelegramPopup$lambda$0$9(dialog, $ctx, view);
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
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.horis.cncverse.HotStarMirrorProvider$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                HotStarMirrorProvider.openInExternalBrowser$lambda$0(ctx, url);
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
}
