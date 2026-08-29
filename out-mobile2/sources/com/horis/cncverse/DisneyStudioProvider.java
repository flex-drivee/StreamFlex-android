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
import com.horis.cncverse.DisneyStudioProvider;
import com.horis.cncverse.entities.EpisodesData;
import com.horis.cncverse.entities.PlayList;
import com.horis.cncverse.entities.PlayListItem;
import com.horis.cncverse.entities.PostData;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
/* compiled from: DisneyStudioProvider.kt */
@Metadata(d1 = {"\u0000|\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\"\n\u0002\u0018\u0002\n\u0002\b\u000e\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010$\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\b\u0016\u0018\u0000 C2\u00020\u0001:\u0003CDEB\u0017\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003¢\u0006\u0004\b\u0005\u0010\u0006J\u0014\u0010\u001e\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\u001dH\u0002J \u0010\u001f\u001a\u0004\u0018\u00010 2\u0006\u0010!\u001a\u00020\"2\u0006\u0010#\u001a\u00020$H\u0096@¢\u0006\u0002\u0010%J\f\u0010&\u001a\u00020'*\u00020(H\u0002J\u000e\u0010)\u001a\u0004\u0018\u00010**\u00020(H\u0002J\u0018\u0010+\u001a\u0004\u0018\u00010,2\u0006\u0010-\u001a\u00020\u0003H\u0096@¢\u0006\u0002\u0010.J4\u0010/\u001a\b\u0012\u0004\u0012\u000201002\u0006\u00102\u001a\u00020\u00032\u0006\u00103\u001a\u00020\u00032\u0006\u00104\u001a\u00020\u00032\u0006\u0010!\u001a\u00020\"H\u0082@¢\u0006\u0002\u00105JF\u00106\u001a\u00020\u00182\u0006\u00107\u001a\u00020\u00032\u0006\u00108\u001a\u00020\u00182\u0012\u00109\u001a\u000e\u0012\u0004\u0012\u00020;\u0012\u0004\u0012\u00020<0:2\u0012\u0010=\u001a\u000e\u0012\u0004\u0012\u00020>\u0012\u0004\u0012\u00020<0:H\u0096@¢\u0006\u0002\u0010?J\b\u0010@\u001a\u00020<H\u0002J\b\u0010A\u001a\u00020<H\u0002J\u0010\u0010B\u001a\u00020<2\u0006\u0010-\u001a\u00020\u0003H\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004¢\u0006\u0002\n\u0000R\u001a\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0096\u0004¢\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u001a\u0010\f\u001a\u00020\u0003X\u0096\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\u000e\"\u0004\b\u000f\u0010\u0010R\u001a\u0010\u0011\u001a\u00020\u0003X\u0096\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u000e\"\u0004\b\u0013\u0010\u0010R\u001a\u0010\u0014\u001a\u00020\u0003X\u0096\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u0015\u0010\u000e\"\u0004\b\u0016\u0010\u0010R\u0014\u0010\u0017\u001a\u00020\u0018X\u0096D¢\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001aR\u000e\u0010\u001b\u001a\u00020\u0003X\u0082\u000e¢\u0006\u0002\n\u0000R\u001a\u0010\u001c\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\u001dX\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006F"}, d2 = {"Lcom/horis/cncverse/DisneyStudioProvider;", "Lcom/lagradost/cloudstream3/MainAPI;", "studio", "", "displayName", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V", "supportedTypes", "", "Lcom/lagradost/cloudstream3/TvType;", "getSupportedTypes", "()Ljava/util/Set;", "lang", "getLang", "()Ljava/lang/String;", "setLang", "(Ljava/lang/String;)V", "mainUrl", "getMainUrl", "setMainUrl", "name", "getName", "setName", "hasMainPage", "", "getHasMainPage", "()Z", "cookie_value", "headers", "", "buildCookies", "getMainPage", "Lcom/lagradost/cloudstream3/HomePageResponse;", "page", "", "request", "Lcom/lagradost/cloudstream3/MainPageRequest;", "(ILcom/lagradost/cloudstream3/MainPageRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "toHomePageList", "Lcom/lagradost/cloudstream3/HomePageList;", "Lorg/jsoup/nodes/Element;", "toSearchResult", "Lcom/lagradost/cloudstream3/SearchResponse;", "load", "Lcom/lagradost/cloudstream3/LoadResponse;", "url", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getEpisodes", "", "Lcom/lagradost/cloudstream3/Episode;", "title", "eid", "sid", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "loadLinks", "data", "isCasting", "subtitleCallback", "Lkotlin/Function1;", "Lcom/lagradost/cloudstream3/SubtitleFile;", "", "callback", "Lcom/lagradost/cloudstream3/utils/ExtractorLink;", "(Ljava/lang/String;ZLkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "showSubscriptionPopupIfNeeded", "showTelegramPopup", "openInExternalBrowser", "Companion", "Id", "LoadData", "CNC Verse Mobile_debug"}, k = 1, mv = {2, 3, 0}, xi = 48)
@SourceDebugExtension({"SMAP\nDisneyStudioProvider.kt\nKotlin\n*S Kotlin\n*F\n+ 1 DisneyStudioProvider.kt\ncom/horis/cncverse/DisneyStudioProvider\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n+ 4 Utils.kt\ncom/horis/cncverse/UtilsKt\n+ 5 NiceResponse.kt\ncom/lagradost/nicehttp/NiceResponse\n*L\n1#1,585:1\n1586#2:586\n1661#2,3:587\n1642#2,10:590\n1915#2:600\n1916#2:602\n1652#2:603\n1586#2:606\n1661#2,3:607\n1586#2:610\n1661#2,3:611\n1586#2:614\n1661#2,3:615\n777#2:618\n873#2,2:619\n1586#2:621\n1661#2,3:622\n1661#2,3:625\n1661#2,3:629\n777#2:634\n873#2,2:635\n1586#2:637\n1661#2,3:638\n1#3:601\n1#3:641\n218#4:604\n218#4:632\n62#5:605\n62#5:628\n62#5:633\n*S KotlinDebug\n*F\n+ 1 DisneyStudioProvider.kt\ncom/horis/cncverse/DisneyStudioProvider\n*L\n114#1:586\n114#1:587,3\n122#1:590,10\n122#1:600\n122#1:602\n122#1:603\n165#1:606\n165#1:607,3\n166#1:610\n166#1:611,3\n172#1:614\n172#1:615,3\n173#1:618\n173#1:619,2\n178#1:621\n178#1:622,3\n190#1:625,3\n238#1:629,3\n324#1:634\n324#1:635,2\n324#1:637\n324#1:638,3\n122#1:601\n154#1:604\n279#1:632\n160#1:605\n237#1:628\n306#1:633\n*E\n"})
/* loaded from: /sdcard/AndroidIDEProjects/Cloudstream reference/cncverse/CNC Verse Mobile/resources/classes.dex */
public class DisneyStudioProvider extends MainAPI {
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
    private String name;
    @NotNull
    private final String studio;
    @NotNull
    private final Set<TvType> supportedTypes = SetsKt.setOf(new TvType[]{TvType.Movie, TvType.TvSeries, TvType.Anime, TvType.AsianDrama});
    @NotNull
    private String lang = "ta";
    @NotNull
    private String mainUrl = "https://net52.cc";
    private final boolean hasMainPage = true;
    @NotNull
    private String cookie_value = "";
    @NotNull
    private final Map<String, String> headers = MapsKt.mapOf(new Pair[]{TuplesKt.to("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"), TuplesKt.to("Accept-Language", "en-IN,en-US;q=0.9,en;q=0.8"), TuplesKt.to("Cache-Control", "max-age=0"), TuplesKt.to("Connection", "keep-alive"), TuplesKt.to("sec-ch-ua", "\"Not(A:Brand\";v=\"8\", \"Chromium\";v=\"144\", \"Android WebView\";v=\"144\""), TuplesKt.to("sec-ch-ua-mobile", "?0"), TuplesKt.to("sec-ch-ua-platform", "\"Android\""), TuplesKt.to("Sec-Fetch-Dest", "document"), TuplesKt.to("Sec-Fetch-Mode", "navigate"), TuplesKt.to("Sec-Fetch-Site", "same-origin"), TuplesKt.to("Sec-Fetch-User", "?1"), TuplesKt.to("Upgrade-Insecure-Requests", "1"), TuplesKt.to("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/144.0.7559.132 Safari/537.36 /OS.Gatu v3.0"), TuplesKt.to("X-Requested-With", "app.netmirror.netmirrornew")});

    @Nullable
    public Object getMainPage(int i, @NotNull MainPageRequest mainPageRequest, @NotNull Continuation<? super HomePageResponse> continuation) {
        return getMainPage$suspendImpl(this, i, mainPageRequest, continuation);
    }

    @Nullable
    public Object load(@NotNull String str, @NotNull Continuation<? super LoadResponse> continuation) {
        return load$suspendImpl(this, str, continuation);
    }

    @Nullable
    public Object loadLinks(@NotNull String str, boolean z, @NotNull Function1<? super SubtitleFile, Unit> function1, @NotNull Function1<? super ExtractorLink, Unit> function12, @NotNull Continuation<? super Boolean> continuation) {
        return loadLinks$suspendImpl(this, str, z, function1, function12, continuation);
    }

    public DisneyStudioProvider(@NotNull String studio, @NotNull String displayName) {
        this.studio = studio;
        this.name = displayName;
    }

    /* compiled from: DisneyStudioProvider.kt */
    @Metadata(d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J\u0006\u0010\u0004\u001a\u00020\u0005J\u0006\u0010\u0007\u001a\u00020\u0005J\u0012\u0010\b\u001a\u00020\t2\b\u0010\n\u001a\u0004\u0018\u00010\u000bH\u0002R\u000e\u0010\u0006\u001a\u00020\u0005X\u0082\u000e¢\u0006\u0002\n\u0000R\u001c\u0010\f\u001a\u0004\u0018\u00010\u000bX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\u000e\"\u0004\b\u000f\u0010\u0010R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082T¢\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0005X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0005X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0014X\u0082T¢\u0006\u0002\n\u0000¨\u0006\u0018"}, d2 = {"Lcom/horis/cncverse/DisneyStudioProvider$Companion;", "", "<init>", "()V", "isCsGuardActive", "", "csGuardWasEverActive", "isCsGuardBlocked", "showCsGuardToast", "", "ctx", "Landroid/content/Context;", "context", "getContext", "()Landroid/content/Context;", "setContext", "(Landroid/content/Context;)V", "OMG10", "", "lastBrowserOpenMs", "", "telegramPopupShown", "subscriptionPopupShown", "BROWSER_DEBOUNCE_MS", "CNC Verse Mobile_debug"}, k = 1, mv = {2, 3, 0}, xi = 48)
    @SourceDebugExtension({"SMAP\nDisneyStudioProvider.kt\nKotlin\n*S Kotlin\n*F\n+ 1 DisneyStudioProvider.kt\ncom/horis/cncverse/DisneyStudioProvider$Companion\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,585:1\n1#2:586\n*E\n"})
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
                DisneyStudioProvider.csGuardWasEverActive = true;
            }
            return DisneyStudioProvider.csGuardWasEverActive;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public final void showCsGuardToast(final Context ctx) {
            if (ctx == null) {
                return;
            }
            new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.horis.cncverse.DisneyStudioProvider$Companion$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    DisneyStudioProvider.Companion.showCsGuardToast$lambda$0(ctx);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static final void showCsGuardToast$lambda$0(Context $c) {
            Toast.makeText($c, "🚫 CSGuard detected — Restart CloudStream after removing CSGuard to use CNCRepo", 1).show();
        }

        @Nullable
        public final Context getContext() {
            return DisneyStudioProvider.context;
        }

        public final void setContext(@Nullable Context context) {
            DisneyStudioProvider.context = context;
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

    private final Map<String, String> buildCookies() {
        Map cookies = MapsKt.mutableMapOf(new Pair[]{TuplesKt.to("t_hash_t", this.cookie_value), TuplesKt.to("ott", "dp"), TuplesKt.to("hd", "on")});
        if (this.studio.length() > 0) {
            cookies.put("studio", this.studio);
        }
        return cookies;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:10:0x002a  */
    /* JADX WARN: Removed duplicated region for block: B:12:0x0035  */
    /* JADX WARN: Removed duplicated region for block: B:13:0x0048  */
    /* JADX WARN: Removed duplicated region for block: B:14:0x0064  */
    /* JADX WARN: Removed duplicated region for block: B:24:0x0125 A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:25:0x0126  */
    /* JADX WARN: Removed duplicated region for block: B:29:0x0155 A[LOOP:0: B:27:0x014f->B:29:0x0155, LOOP_END] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static /* synthetic */ Object getMainPage$suspendImpl(DisneyStudioProvider $this, int page, MainPageRequest request, Continuation<? super HomePageResponse> continuation) {
        DisneyStudioProvider$getMainPage$1 disneyStudioProvider$getMainPage$1;
        int page2;
        MainPageRequest request2;
        Object obj;
        DisneyStudioProvider $this2;
        DisneyStudioProvider $this3;
        DisneyStudioProvider $this4 = $this;
        if (continuation instanceof DisneyStudioProvider$getMainPage$1) {
            disneyStudioProvider$getMainPage$1 = (DisneyStudioProvider$getMainPage$1) continuation;
            if ((disneyStudioProvider$getMainPage$1.label & Integer.MIN_VALUE) != 0) {
                disneyStudioProvider$getMainPage$1.label -= Integer.MIN_VALUE;
                Object $result = disneyStudioProvider$getMainPage$1.result;
                Object coroutine_suspended = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (disneyStudioProvider$getMainPage$1.label) {
                    case 0:
                        ResultKt.throwOnFailure($result);
                        if (Companion.isCsGuardBlocked()) {
                            Companion.showCsGuardToast(context);
                            return MainAPIKt.newHomePageResponse$default(CollectionsKt.emptyList(), (Boolean) null, 2, (Object) null);
                        }
                        $this4.showTelegramPopup();
                        $this4.showSubscriptionPopupIfNeeded();
                        String mainUrl = $this4.getMainUrl();
                        disneyStudioProvider$getMainPage$1.L$0 = $this4;
                        disneyStudioProvider$getMainPage$1.L$1 = SpillingKt.nullOutSpilledVariable(request);
                        disneyStudioProvider$getMainPage$1.L$2 = $this4;
                        page2 = page;
                        disneyStudioProvider$getMainPage$1.I$0 = page2;
                        disneyStudioProvider$getMainPage$1.label = 1;
                        Object bypass = UtilsKt.bypass(mainUrl, disneyStudioProvider$getMainPage$1);
                        if (bypass == coroutine_suspended) {
                            return coroutine_suspended;
                        }
                        request2 = request;
                        obj = bypass;
                        $this2 = $this4;
                        $this4.cookie_value = (String) obj;
                        Map<String, String> buildCookies = $this2.buildCookies();
                        disneyStudioProvider$getMainPage$1.L$0 = $this2;
                        disneyStudioProvider$getMainPage$1.L$1 = SpillingKt.nullOutSpilledVariable(request2);
                        disneyStudioProvider$getMainPage$1.L$2 = null;
                        disneyStudioProvider$getMainPage$1.I$0 = page2;
                        disneyStudioProvider$getMainPage$1.label = 2;
                        DisneyStudioProvider $this5 = $this2;
                        $result = Requests.get$default(UtilsKt.getApp(), $this2.getMainUrl() + "/mobile/home?app=1", $this2.headers, $this2.getMainUrl() + "/mobile/home?app=1", (Map) null, buildCookies, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, disneyStudioProvider$getMainPage$1, 4072, (Object) null);
                        if ($result != coroutine_suspended) {
                            return coroutine_suspended;
                        }
                        $this3 = $this5;
                        Document document = ((NiceResponse) $result).getDocument();
                        Iterable $this$map$iv = document.select(".tray-container, #top10");
                        Collection destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv, 10));
                        for (Object item$iv$iv : $this$map$iv) {
                            Element it = (Element) item$iv$iv;
                            destination$iv$iv.add($this3.toHomePageList(it));
                        }
                        List items = (List) destination$iv$iv;
                        return MainAPIKt.newHomePageResponse(items, Boxing.boxBoolean(false));
                    case 1:
                        int page3 = disneyStudioProvider$getMainPage$1.I$0;
                        DisneyStudioProvider $this6 = (DisneyStudioProvider) disneyStudioProvider$getMainPage$1.L$0;
                        ResultKt.throwOnFailure($result);
                        $this2 = $this6;
                        $this4 = (DisneyStudioProvider) disneyStudioProvider$getMainPage$1.L$2;
                        page2 = page3;
                        request2 = (MainPageRequest) disneyStudioProvider$getMainPage$1.L$1;
                        obj = $result;
                        $this4.cookie_value = (String) obj;
                        Map<String, String> buildCookies2 = $this2.buildCookies();
                        disneyStudioProvider$getMainPage$1.L$0 = $this2;
                        disneyStudioProvider$getMainPage$1.L$1 = SpillingKt.nullOutSpilledVariable(request2);
                        disneyStudioProvider$getMainPage$1.L$2 = null;
                        disneyStudioProvider$getMainPage$1.I$0 = page2;
                        disneyStudioProvider$getMainPage$1.label = 2;
                        DisneyStudioProvider $this52 = $this2;
                        $result = Requests.get$default(UtilsKt.getApp(), $this2.getMainUrl() + "/mobile/home?app=1", $this2.headers, $this2.getMainUrl() + "/mobile/home?app=1", (Map) null, buildCookies2, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, disneyStudioProvider$getMainPage$1, 4072, (Object) null);
                        if ($result != coroutine_suspended) {
                        }
                        break;
                    case 2:
                        int i = disneyStudioProvider$getMainPage$1.I$0;
                        MainPageRequest mainPageRequest = (MainPageRequest) disneyStudioProvider$getMainPage$1.L$1;
                        $this3 = (DisneyStudioProvider) disneyStudioProvider$getMainPage$1.L$0;
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
        disneyStudioProvider$getMainPage$1 = new DisneyStudioProvider$getMainPage$1($this4, continuation);
        Object $result2 = disneyStudioProvider$getMainPage$1.result;
        Object coroutine_suspended2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
        switch (disneyStudioProvider$getMainPage$1.label) {
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
        return MainAPIKt.newAnimeSearchResponse$default(this, "", AppUtils.INSTANCE.toJson(new Id(id)), (TvType) null, false, new Function1() { // from class: com.horis.cncverse.DisneyStudioProvider$$ExternalSyntheticLambda4
            public final Object invoke(Object obj) {
                Unit searchResult$lambda$0;
                searchResult$lambda$0 = DisneyStudioProvider.toSearchResult$lambda$0(id, this, (AnimeSearchResponse) obj);
                return searchResult$lambda$0;
            }
        }, 12, (Object) null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit toSearchResult$lambda$0(String $id, DisneyStudioProvider this$0, AnimeSearchResponse $this$newAnimeSearchResponse) {
        $this$newAnimeSearchResponse.setPosterUrl("https://imgcdn.kim/hs/v/" + $id + ".jpg");
        $this$newAnimeSearchResponse.setPosterHeaders(MapsKt.mapOf(TuplesKt.to("Referer", this$0.getMainUrl() + "/home")));
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:102:0x058c  */
    /* JADX WARN: Removed duplicated region for block: B:103:0x058f  */
    /* JADX WARN: Removed duplicated region for block: B:106:0x05fb A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:107:0x05fc  */
    /* JADX WARN: Removed duplicated region for block: B:10:0x002a  */
    /* JADX WARN: Removed duplicated region for block: B:12:0x0034  */
    /* JADX WARN: Removed duplicated region for block: B:13:0x006e  */
    /* JADX WARN: Removed duplicated region for block: B:14:0x00aa  */
    /* JADX WARN: Removed duplicated region for block: B:15:0x00ee  */
    /* JADX WARN: Removed duplicated region for block: B:16:0x0107  */
    /* JADX WARN: Removed duplicated region for block: B:17:0x011d  */
    /* JADX WARN: Removed duplicated region for block: B:23:0x01de A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:24:0x01df  */
    /* JADX WARN: Removed duplicated region for block: B:38:0x0289 A[LOOP:1: B:36:0x0283->B:38:0x0289, LOOP_END] */
    /* JADX WARN: Removed duplicated region for block: B:61:0x0375  */
    /* JADX WARN: Removed duplicated region for block: B:62:0x0384  */
    /* JADX WARN: Removed duplicated region for block: B:65:0x0398  */
    /* JADX WARN: Removed duplicated region for block: B:70:0x040b  */
    /* JADX WARN: Removed duplicated region for block: B:73:0x041b  */
    /* JADX WARN: Removed duplicated region for block: B:74:0x044a  */
    /* JADX WARN: Removed duplicated region for block: B:92:0x0513  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static /* synthetic */ Object load$suspendImpl(DisneyStudioProvider $this, String url, Continuation<? super LoadResponse> continuation) {
        Continuation disneyStudioProvider$load$1;
        Continuation $continuation;
        String url2;
        Object obj;
        DisneyStudioProvider $this2;
        Object obj2;
        Object obj3;
        String id;
        Object obj4;
        String rating;
        final DisneyStudioProvider $this3;
        final PostData data;
        String title;
        String cast;
        List emptyList;
        List cast2;
        String genre;
        ArrayList arrayList;
        Iterable suggest;
        Object obj5;
        List castList;
        ArrayList arrayList2;
        int runTime;
        Object obj6;
        Object episodes;
        String id2;
        ArrayList episodes2;
        List list;
        String id3;
        String url3;
        int runTime2;
        List castList2;
        List suggest2;
        DisneyStudioProvider $this4;
        String title2;
        ArrayList arrayList3;
        String url4;
        String url5;
        String title3;
        DisneyStudioProvider $this5;
        ArrayList episodes3;
        ArrayList episodes4;
        PostData data2;
        ArrayList suggest3;
        ArrayList genre2;
        String rating2;
        DisneyStudioProvider $this6;
        String id4;
        ArrayList episodes5;
        List cast3;
        int runTime3;
        PostData data3;
        List castList3;
        Iterable split$default;
        Iterable split$default2;
        ArrayList arrayList4;
        List<Season> season;
        List dropLast;
        List suggest4;
        DisneyStudioProvider $this7;
        List cast4;
        ArrayList episodes6;
        String url6;
        List genre3;
        String title4;
        String title5;
        List castList4;
        DisneyStudioProvider $this8 = $this;
        if (continuation instanceof DisneyStudioProvider$load$1) {
            disneyStudioProvider$load$1 = (DisneyStudioProvider$load$1) continuation;
            if ((disneyStudioProvider$load$1.label & Integer.MIN_VALUE) != 0) {
                disneyStudioProvider$load$1.label -= Integer.MIN_VALUE;
                $continuation = disneyStudioProvider$load$1;
                Object $result = $continuation.result;
                Object coroutine_suspended = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch ($continuation.label) {
                    case 0:
                        ResultKt.throwOnFailure($result);
                        String mainUrl = $this8.getMainUrl();
                        $continuation.L$0 = $this8;
                        url2 = url;
                        $continuation.L$1 = url2;
                        $continuation.L$2 = $this8;
                        $continuation.label = 1;
                        Object bypass = UtilsKt.bypass(mainUrl, $continuation);
                        if (bypass == coroutine_suspended) {
                            return coroutine_suspended;
                        }
                        obj = bypass;
                        $this2 = $this8;
                        $this8.cookie_value = (String) obj;
                        String text$iv = url2;
                        String id5 = ((Id) UtilsKt.getJSONParser().parse(text$iv, Reflection.getOrCreateKotlinClass(Id.class))).getId();
                        Map<String, String> buildCookies = $this2.buildCookies();
                        $continuation.L$0 = $this2;
                        $continuation.L$1 = url2;
                        $continuation.L$2 = id5;
                        $continuation.label = 2;
                        String url7 = url2;
                        DisneyStudioProvider $this9 = $this2;
                        obj2 = coroutine_suspended;
                        obj3 = Requests.get$default(UtilsKt.getApp(), $this2.getMainUrl() + "/mobile/hs/post.php?id=" + id5 + "&t=" + APIHolder.INSTANCE.getUnixTime(), $this2.headers, $this2.getMainUrl() + "/home", (Map) null, buildCookies, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, $continuation, 4072, (Object) null);
                        $continuation = $continuation;
                        if (obj3 != obj2) {
                            return obj2;
                        }
                        id = id5;
                        obj4 = obj3;
                        rating = url7;
                        $this3 = $this9;
                        NiceResponse this_$iv = (NiceResponse) obj4;
                        ResponseParser parser = this_$iv.getParser();
                        Intrinsics.checkNotNull(parser);
                        data = (PostData) parser.parse(this_$iv.getText(), Reflection.getOrCreateKotlinClass(PostData.class));
                        ArrayList episodes7 = new ArrayList();
                        title = data.getTitle();
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
                        List castList5 = emptyList;
                        List $this$map$iv2 = castList5;
                        boolean z = false;
                        Collection destination$iv$iv2 = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv2, 10));
                        for (Object item$iv$iv2 : $this$map$iv2) {
                            destination$iv$iv2.add(new ActorData(new Actor((String) item$iv$iv2, (String) null, 2, (DefaultConstructorMarker) null), (ActorRole) null, (String) null, (Actor) null, 14, (DefaultConstructorMarker) null));
                            $this$map$iv2 = $this$map$iv2;
                            $result = $result;
                            z = z;
                        }
                        cast2 = (List) destination$iv$iv2;
                        genre = data.getGenre();
                        if (genre != null || (split$default = StringsKt.split$default(genre, new String[]{","}, false, 0, 6, (Object) null)) == null) {
                            arrayList = null;
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
                            arrayList = (List) destination$iv$iv4;
                        }
                        ArrayList arrayList5 = arrayList;
                        String match = data.getMatch();
                        String rating3 = match == null ? StringsKt.replace$default(match, "IMDb ", "", false, 4, (Object) null) : null;
                        int runTime4 = UtilsKt.convertRuntimeToMinutes(String.valueOf(data.getRuntime()));
                        suggest = data.getSuggest();
                        if (suggest == null) {
                            Iterable $this$map$iv4 = suggest;
                            int $i$f$map = CollectionsKt.collectionSizeOrDefault($this$map$iv4, 10);
                            Collection destination$iv$iv5 = new ArrayList($i$f$map);
                            Iterable $this$mapTo$iv$iv = $this$map$iv4;
                            for (Object item$iv$iv4 : $this$mapTo$iv$iv) {
                                Iterable $this$map$iv5 = $this$map$iv4;
                                final Suggest it = (Suggest) item$iv$iv4;
                                destination$iv$iv5.add(MainAPIKt.newAnimeSearchResponse$default($this3, "", AppUtils.INSTANCE.toJson(new Id(it.getId())), (TvType) null, false, new Function1() { // from class: com.horis.cncverse.DisneyStudioProvider$$ExternalSyntheticLambda5
                                    public final Object invoke(Object obj7) {
                                        Unit load$lambda$4$0;
                                        load$lambda$4$0 = DisneyStudioProvider.load$lambda$4$0(Suggest.this, $this3, (AnimeSearchResponse) obj7);
                                        return load$lambda$4$0;
                                    }
                                }, 12, (Object) null));
                                $this$map$iv4 = $this$map$iv5;
                                castList5 = castList5;
                                $this$mapTo$iv$iv = $this$mapTo$iv$iv;
                                obj2 = obj2;
                            }
                            obj5 = obj2;
                            castList = castList5;
                            arrayList2 = (List) destination$iv$iv5;
                        } else {
                            obj5 = obj2;
                            castList = castList5;
                            arrayList2 = null;
                        }
                        ArrayList arrayList6 = arrayList2;
                        if (CollectionsKt.first(data.getEpisodes()) != null) {
                            Boxing.boxBoolean(episodes7.add(MainAPIKt.newEpisode($this3, new LoadData(title, id), new Function1() { // from class: com.horis.cncverse.DisneyStudioProvider$$ExternalSyntheticLambda6
                                public final Object invoke(Object obj7) {
                                    Unit load$lambda$5;
                                    load$lambda$5 = DisneyStudioProvider.load$lambda$5(PostData.this, (Episode) obj7);
                                    return load$lambda$5;
                                }
                            })));
                            suggest3 = arrayList6;
                            genre2 = arrayList5;
                            rating2 = rating3;
                            $this6 = $this3;
                            id4 = id;
                            episodes5 = episodes7;
                            cast3 = cast2;
                            runTime3 = runTime4;
                            data3 = data;
                            castList3 = castList;
                            obj6 = obj5;
                            List suggest5 = data3.getEpisodes();
                            TvType type = CollectionsKt.first(suggest5) != null ? TvType.Movie : TvType.TvSeries;
                            $continuation.L$0 = SpillingKt.nullOutSpilledVariable($this6);
                            $continuation.L$1 = SpillingKt.nullOutSpilledVariable(rating);
                            $continuation.L$2 = SpillingKt.nullOutSpilledVariable(id4);
                            $continuation.L$3 = SpillingKt.nullOutSpilledVariable(data3);
                            $continuation.L$4 = SpillingKt.nullOutSpilledVariable(episodes5);
                            $continuation.L$5 = SpillingKt.nullOutSpilledVariable(title);
                            $continuation.L$6 = SpillingKt.nullOutSpilledVariable(castList3);
                            $continuation.L$7 = SpillingKt.nullOutSpilledVariable(cast3);
                            $continuation.L$8 = SpillingKt.nullOutSpilledVariable(genre2);
                            $continuation.L$9 = SpillingKt.nullOutSpilledVariable(rating2);
                            $continuation.L$10 = SpillingKt.nullOutSpilledVariable(suggest3);
                            $continuation.L$11 = SpillingKt.nullOutSpilledVariable(type);
                            $continuation.I$0 = runTime3;
                            $continuation.label = 5;
                            Object newTvSeriesLoadResponse = MainAPIKt.newTvSeriesLoadResponse($this6, title, rating, type, episodes5, new DisneyStudioProvider$load$5(id4, $this6, data3, genre2, cast3, rating2, runTime3, suggest3, null), $continuation);
                            return newTvSeriesLoadResponse != obj6 ? obj6 : newTvSeriesLoadResponse;
                        }
                        Iterable $this$mapTo$iv = CollectionsKt.filterNotNull(data.getEpisodes());
                        ArrayList destination$iv = episodes7;
                        boolean z2 = false;
                        Iterator it2 = $this$mapTo$iv.iterator();
                        while (it2.hasNext()) {
                            Object item$iv = it2.next();
                            Iterable $this$mapTo$iv2 = $this$mapTo$iv;
                            final com.horis.cncverse.entities.Episode it3 = (com.horis.cncverse.entities.Episode) item$iv;
                            destination$iv.add(MainAPIKt.newEpisode($this3, new LoadData(title, it3.getId()), new Function1() { // from class: com.horis.cncverse.DisneyStudioProvider$$ExternalSyntheticLambda7
                                public final Object invoke(Object obj7) {
                                    Unit load$lambda$6$0;
                                    load$lambda$6$0 = DisneyStudioProvider.load$lambda$6$0(com.horis.cncverse.entities.Episode.this, (Episode) obj7);
                                    return load$lambda$6$0;
                                }
                            }));
                            $this$mapTo$iv = $this$mapTo$iv2;
                            z2 = z2;
                            it2 = it2;
                            runTime4 = runTime4;
                        }
                        int runTime5 = runTime4;
                        Integer nextPageShow = data.getNextPageShow();
                        if (nextPageShow == null) {
                            runTime = runTime5;
                            obj6 = obj5;
                        } else if (nextPageShow.intValue() == 1) {
                            String nextPageSeason = data.getNextPageSeason();
                            Intrinsics.checkNotNull(nextPageSeason);
                            $continuation.L$0 = $this3;
                            $continuation.L$1 = rating;
                            $continuation.L$2 = id;
                            $continuation.L$3 = data;
                            $continuation.L$4 = episodes7;
                            $continuation.L$5 = title;
                            $continuation.L$6 = SpillingKt.nullOutSpilledVariable(castList);
                            $continuation.L$7 = cast2;
                            $continuation.L$8 = arrayList5;
                            $continuation.L$9 = rating3;
                            $continuation.L$10 = arrayList6;
                            $continuation.L$11 = episodes7;
                            $continuation.I$0 = runTime5;
                            $continuation.label = 3;
                            episodes = $this3.getEpisodes(title, rating, nextPageSeason, 2, $continuation);
                            obj6 = obj5;
                            if (episodes == obj6) {
                                return obj6;
                            }
                            id2 = id;
                            episodes2 = episodes7;
                            list = arrayList5;
                            id3 = rating3;
                            url3 = rating;
                            runTime2 = runTime5;
                            castList2 = castList;
                            suggest2 = arrayList6;
                            $this4 = $this3;
                            title2 = title;
                            arrayList3 = episodes2;
                            arrayList3.addAll((Collection) episodes);
                            $this5 = $this4;
                            url4 = url3;
                            arrayList4 = suggest2;
                            castList = castList2;
                            title3 = title2;
                            episodes3 = episodes2;
                            url5 = id2;
                            data2 = data;
                            episodes4 = list;
                            season = data2.getSeason();
                            if (season != null || (dropLast = CollectionsKt.dropLast(season, 1)) == null) {
                                suggest3 = arrayList4;
                                runTime3 = runTime2;
                                $this6 = $this5;
                                title = title3;
                                rating = url4;
                                rating2 = id3;
                                genre2 = episodes4;
                                cast3 = cast2;
                                data3 = data2;
                                id4 = url5;
                                castList3 = castList;
                                episodes5 = episodes3;
                                List suggest52 = data3.getEpisodes();
                                TvType type2 = CollectionsKt.first(suggest52) != null ? TvType.Movie : TvType.TvSeries;
                                $continuation.L$0 = SpillingKt.nullOutSpilledVariable($this6);
                                $continuation.L$1 = SpillingKt.nullOutSpilledVariable(rating);
                                $continuation.L$2 = SpillingKt.nullOutSpilledVariable(id4);
                                $continuation.L$3 = SpillingKt.nullOutSpilledVariable(data3);
                                $continuation.L$4 = SpillingKt.nullOutSpilledVariable(episodes5);
                                $continuation.L$5 = SpillingKt.nullOutSpilledVariable(title);
                                $continuation.L$6 = SpillingKt.nullOutSpilledVariable(castList3);
                                $continuation.L$7 = SpillingKt.nullOutSpilledVariable(cast3);
                                $continuation.L$8 = SpillingKt.nullOutSpilledVariable(genre2);
                                $continuation.L$9 = SpillingKt.nullOutSpilledVariable(rating2);
                                $continuation.L$10 = SpillingKt.nullOutSpilledVariable(suggest3);
                                $continuation.L$11 = SpillingKt.nullOutSpilledVariable(type2);
                                $continuation.I$0 = runTime3;
                                $continuation.label = 5;
                                Object newTvSeriesLoadResponse2 = MainAPIKt.newTvSeriesLoadResponse($this6, title, rating, type2, episodes5, new DisneyStudioProvider$load$5(id4, $this6, data3, genre2, cast3, rating2, runTime3, suggest3, null), $continuation);
                                if (newTvSeriesLoadResponse2 != obj6) {
                                }
                            } else {
                                $continuation.L$0 = $this5;
                                $continuation.L$1 = url4;
                                $continuation.L$2 = url5;
                                $continuation.L$3 = data2;
                                $continuation.L$4 = episodes3;
                                $continuation.L$5 = title3;
                                $continuation.L$6 = SpillingKt.nullOutSpilledVariable(castList);
                                $continuation.L$7 = cast2;
                                $continuation.L$8 = episodes4;
                                $continuation.L$9 = id3;
                                $continuation.L$10 = arrayList4;
                                $continuation.L$11 = null;
                                $continuation.I$0 = runTime2;
                                $continuation.label = 4;
                                $result = ParCollectionsKt.amap(dropLast, new DisneyStudioProvider$load$4(episodes3, $this5, title3, url4, null), $continuation);
                                if ($result == obj6) {
                                    return obj6;
                                }
                                DisneyStudioProvider disneyStudioProvider = $this5;
                                suggest4 = arrayList4;
                                $this7 = disneyStudioProvider;
                                cast4 = cast2;
                                episodes6 = episodes3;
                                url6 = url4;
                                genre3 = episodes4;
                                title4 = title3;
                                title5 = id3;
                                castList4 = castList;
                                List list2 = (List) $result;
                                $this6 = $this7;
                                runTime3 = runTime2;
                                suggest3 = suggest4;
                                rating2 = title5;
                                genre2 = genre3;
                                cast3 = cast4;
                                title = title4;
                                data3 = data2;
                                id4 = url5;
                                rating = url6;
                                castList3 = castList4;
                                episodes5 = episodes6;
                                List suggest522 = data3.getEpisodes();
                                TvType type22 = CollectionsKt.first(suggest522) != null ? TvType.Movie : TvType.TvSeries;
                                $continuation.L$0 = SpillingKt.nullOutSpilledVariable($this6);
                                $continuation.L$1 = SpillingKt.nullOutSpilledVariable(rating);
                                $continuation.L$2 = SpillingKt.nullOutSpilledVariable(id4);
                                $continuation.L$3 = SpillingKt.nullOutSpilledVariable(data3);
                                $continuation.L$4 = SpillingKt.nullOutSpilledVariable(episodes5);
                                $continuation.L$5 = SpillingKt.nullOutSpilledVariable(title);
                                $continuation.L$6 = SpillingKt.nullOutSpilledVariable(castList3);
                                $continuation.L$7 = SpillingKt.nullOutSpilledVariable(cast3);
                                $continuation.L$8 = SpillingKt.nullOutSpilledVariable(genre2);
                                $continuation.L$9 = SpillingKt.nullOutSpilledVariable(rating2);
                                $continuation.L$10 = SpillingKt.nullOutSpilledVariable(suggest3);
                                $continuation.L$11 = SpillingKt.nullOutSpilledVariable(type22);
                                $continuation.I$0 = runTime3;
                                $continuation.label = 5;
                                Object newTvSeriesLoadResponse22 = MainAPIKt.newTvSeriesLoadResponse($this6, title, rating, type22, episodes5, new DisneyStudioProvider$load$5(id4, $this6, data3, genre2, cast3, rating2, runTime3, suggest3, null), $continuation);
                                if (newTvSeriesLoadResponse22 != obj6) {
                                }
                            }
                        } else {
                            runTime = runTime5;
                            obj6 = obj5;
                        }
                        url4 = rating;
                        url5 = id;
                        id3 = rating3;
                        title3 = title;
                        $this5 = $this3;
                        episodes3 = episodes7;
                        episodes4 = arrayList5;
                        runTime2 = runTime;
                        data2 = data;
                        arrayList4 = arrayList6;
                        season = data2.getSeason();
                        if (season != null) {
                        }
                        suggest3 = arrayList4;
                        runTime3 = runTime2;
                        $this6 = $this5;
                        title = title3;
                        rating = url4;
                        rating2 = id3;
                        genre2 = episodes4;
                        cast3 = cast2;
                        data3 = data2;
                        id4 = url5;
                        castList3 = castList;
                        episodes5 = episodes3;
                        List suggest5222 = data3.getEpisodes();
                        TvType type222 = CollectionsKt.first(suggest5222) != null ? TvType.Movie : TvType.TvSeries;
                        $continuation.L$0 = SpillingKt.nullOutSpilledVariable($this6);
                        $continuation.L$1 = SpillingKt.nullOutSpilledVariable(rating);
                        $continuation.L$2 = SpillingKt.nullOutSpilledVariable(id4);
                        $continuation.L$3 = SpillingKt.nullOutSpilledVariable(data3);
                        $continuation.L$4 = SpillingKt.nullOutSpilledVariable(episodes5);
                        $continuation.L$5 = SpillingKt.nullOutSpilledVariable(title);
                        $continuation.L$6 = SpillingKt.nullOutSpilledVariable(castList3);
                        $continuation.L$7 = SpillingKt.nullOutSpilledVariable(cast3);
                        $continuation.L$8 = SpillingKt.nullOutSpilledVariable(genre2);
                        $continuation.L$9 = SpillingKt.nullOutSpilledVariable(rating2);
                        $continuation.L$10 = SpillingKt.nullOutSpilledVariable(suggest3);
                        $continuation.L$11 = SpillingKt.nullOutSpilledVariable(type222);
                        $continuation.I$0 = runTime3;
                        $continuation.label = 5;
                        Object newTvSeriesLoadResponse222 = MainAPIKt.newTvSeriesLoadResponse($this6, title, rating, type222, episodes5, new DisneyStudioProvider$load$5(id4, $this6, data3, genre2, cast3, rating2, runTime3, suggest3, null), $continuation);
                        if (newTvSeriesLoadResponse222 != obj6) {
                        }
                        break;
                    case 1:
                        DisneyStudioProvider disneyStudioProvider2 = (DisneyStudioProvider) $continuation.L$2;
                        url2 = (String) $continuation.L$1;
                        DisneyStudioProvider $this10 = (DisneyStudioProvider) $continuation.L$0;
                        ResultKt.throwOnFailure($result);
                        $this2 = $this10;
                        $this8 = disneyStudioProvider2;
                        obj = $result;
                        $this8.cookie_value = (String) obj;
                        String text$iv2 = url2;
                        String id52 = ((Id) UtilsKt.getJSONParser().parse(text$iv2, Reflection.getOrCreateKotlinClass(Id.class))).getId();
                        Map<String, String> buildCookies2 = $this2.buildCookies();
                        $continuation.L$0 = $this2;
                        $continuation.L$1 = url2;
                        $continuation.L$2 = id52;
                        $continuation.label = 2;
                        String url72 = url2;
                        DisneyStudioProvider $this92 = $this2;
                        obj2 = coroutine_suspended;
                        obj3 = Requests.get$default(UtilsKt.getApp(), $this2.getMainUrl() + "/mobile/hs/post.php?id=" + id52 + "&t=" + APIHolder.INSTANCE.getUnixTime(), $this2.headers, $this2.getMainUrl() + "/home", (Map) null, buildCookies2, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, $continuation, 4072, (Object) null);
                        $continuation = $continuation;
                        if (obj3 != obj2) {
                        }
                        break;
                    case 2:
                        String url8 = (String) $continuation.L$1;
                        DisneyStudioProvider $this11 = (DisneyStudioProvider) $continuation.L$0;
                        ResultKt.throwOnFailure($result);
                        $this3 = $this11;
                        obj2 = coroutine_suspended;
                        id = (String) $continuation.L$2;
                        rating = url8;
                        obj4 = $result;
                        NiceResponse this_$iv2 = (NiceResponse) obj4;
                        ResponseParser parser2 = this_$iv2.getParser();
                        Intrinsics.checkNotNull(parser2);
                        data = (PostData) parser2.parse(this_$iv2.getText(), Reflection.getOrCreateKotlinClass(PostData.class));
                        ArrayList episodes72 = new ArrayList();
                        title = data.getTitle();
                        cast = data.getCast();
                        if (cast != null) {
                            break;
                        }
                        emptyList = CollectionsKt.emptyList();
                        List castList52 = emptyList;
                        List $this$map$iv22 = castList52;
                        boolean z3 = false;
                        Collection destination$iv$iv22 = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv22, 10));
                        while (r17.hasNext()) {
                        }
                        cast2 = (List) destination$iv$iv22;
                        genre = data.getGenre();
                        if (genre != null) {
                            break;
                        }
                        arrayList = null;
                        ArrayList arrayList52 = arrayList;
                        String match2 = data.getMatch();
                        String rating32 = match2 == null ? StringsKt.replace$default(match2, "IMDb ", "", false, 4, (Object) null) : null;
                        int runTime42 = UtilsKt.convertRuntimeToMinutes(String.valueOf(data.getRuntime()));
                        suggest = data.getSuggest();
                        if (suggest == null) {
                        }
                        ArrayList arrayList62 = arrayList2;
                        if (CollectionsKt.first(data.getEpisodes()) != null) {
                        }
                        break;
                    case 3:
                        int runTime6 = $continuation.I$0;
                        arrayList3 = (ArrayList) $continuation.L$11;
                        suggest2 = (List) $continuation.L$10;
                        id3 = (String) $continuation.L$9;
                        list = (List) $continuation.L$8;
                        cast2 = (List) $continuation.L$7;
                        castList2 = (List) $continuation.L$6;
                        String title6 = (String) $continuation.L$5;
                        episodes2 = (ArrayList) $continuation.L$4;
                        data = (PostData) $continuation.L$3;
                        id2 = (String) $continuation.L$2;
                        String url9 = (String) $continuation.L$1;
                        $this4 = (DisneyStudioProvider) $continuation.L$0;
                        ResultKt.throwOnFailure($result);
                        obj6 = coroutine_suspended;
                        title2 = title6;
                        runTime2 = runTime6;
                        url3 = url9;
                        episodes = $result;
                        arrayList3.addAll((Collection) episodes);
                        $this5 = $this4;
                        url4 = url3;
                        arrayList4 = suggest2;
                        castList = castList2;
                        title3 = title2;
                        episodes3 = episodes2;
                        url5 = id2;
                        data2 = data;
                        episodes4 = list;
                        season = data2.getSeason();
                        if (season != null) {
                        }
                        suggest3 = arrayList4;
                        runTime3 = runTime2;
                        $this6 = $this5;
                        title = title3;
                        rating = url4;
                        rating2 = id3;
                        genre2 = episodes4;
                        cast3 = cast2;
                        data3 = data2;
                        id4 = url5;
                        castList3 = castList;
                        episodes5 = episodes3;
                        List suggest52222 = data3.getEpisodes();
                        TvType type2222 = CollectionsKt.first(suggest52222) != null ? TvType.Movie : TvType.TvSeries;
                        $continuation.L$0 = SpillingKt.nullOutSpilledVariable($this6);
                        $continuation.L$1 = SpillingKt.nullOutSpilledVariable(rating);
                        $continuation.L$2 = SpillingKt.nullOutSpilledVariable(id4);
                        $continuation.L$3 = SpillingKt.nullOutSpilledVariable(data3);
                        $continuation.L$4 = SpillingKt.nullOutSpilledVariable(episodes5);
                        $continuation.L$5 = SpillingKt.nullOutSpilledVariable(title);
                        $continuation.L$6 = SpillingKt.nullOutSpilledVariable(castList3);
                        $continuation.L$7 = SpillingKt.nullOutSpilledVariable(cast3);
                        $continuation.L$8 = SpillingKt.nullOutSpilledVariable(genre2);
                        $continuation.L$9 = SpillingKt.nullOutSpilledVariable(rating2);
                        $continuation.L$10 = SpillingKt.nullOutSpilledVariable(suggest3);
                        $continuation.L$11 = SpillingKt.nullOutSpilledVariable(type2222);
                        $continuation.I$0 = runTime3;
                        $continuation.label = 5;
                        Object newTvSeriesLoadResponse2222 = MainAPIKt.newTvSeriesLoadResponse($this6, title, rating, type2222, episodes5, new DisneyStudioProvider$load$5(id4, $this6, data3, genre2, cast3, rating2, runTime3, suggest3, null), $continuation);
                        if (newTvSeriesLoadResponse2222 != obj6) {
                        }
                        break;
                    case 4:
                        runTime2 = $continuation.I$0;
                        suggest4 = (List) $continuation.L$10;
                        title5 = (String) $continuation.L$9;
                        genre3 = (List) $continuation.L$8;
                        cast4 = (List) $continuation.L$7;
                        castList4 = (List) $continuation.L$6;
                        title4 = (String) $continuation.L$5;
                        episodes6 = (ArrayList) $continuation.L$4;
                        data2 = (PostData) $continuation.L$3;
                        String id6 = (String) $continuation.L$2;
                        String url10 = (String) $continuation.L$1;
                        $this7 = (DisneyStudioProvider) $continuation.L$0;
                        ResultKt.throwOnFailure($result);
                        obj6 = coroutine_suspended;
                        url6 = url10;
                        url5 = id6;
                        List list22 = (List) $result;
                        $this6 = $this7;
                        runTime3 = runTime2;
                        suggest3 = suggest4;
                        rating2 = title5;
                        genre2 = genre3;
                        cast3 = cast4;
                        title = title4;
                        data3 = data2;
                        id4 = url5;
                        rating = url6;
                        castList3 = castList4;
                        episodes5 = episodes6;
                        List suggest522222 = data3.getEpisodes();
                        TvType type22222 = CollectionsKt.first(suggest522222) != null ? TvType.Movie : TvType.TvSeries;
                        $continuation.L$0 = SpillingKt.nullOutSpilledVariable($this6);
                        $continuation.L$1 = SpillingKt.nullOutSpilledVariable(rating);
                        $continuation.L$2 = SpillingKt.nullOutSpilledVariable(id4);
                        $continuation.L$3 = SpillingKt.nullOutSpilledVariable(data3);
                        $continuation.L$4 = SpillingKt.nullOutSpilledVariable(episodes5);
                        $continuation.L$5 = SpillingKt.nullOutSpilledVariable(title);
                        $continuation.L$6 = SpillingKt.nullOutSpilledVariable(castList3);
                        $continuation.L$7 = SpillingKt.nullOutSpilledVariable(cast3);
                        $continuation.L$8 = SpillingKt.nullOutSpilledVariable(genre2);
                        $continuation.L$9 = SpillingKt.nullOutSpilledVariable(rating2);
                        $continuation.L$10 = SpillingKt.nullOutSpilledVariable(suggest3);
                        $continuation.L$11 = SpillingKt.nullOutSpilledVariable(type22222);
                        $continuation.I$0 = runTime3;
                        $continuation.label = 5;
                        Object newTvSeriesLoadResponse22222 = MainAPIKt.newTvSeriesLoadResponse($this6, title, rating, type22222, episodes5, new DisneyStudioProvider$load$5(id4, $this6, data3, genre2, cast3, rating2, runTime3, suggest3, null), $continuation);
                        if (newTvSeriesLoadResponse22222 != obj6) {
                        }
                        break;
                    case 5:
                        int i = $continuation.I$0;
                        TvType tvType = (TvType) $continuation.L$11;
                        List list3 = (List) $continuation.L$10;
                        String str = (String) $continuation.L$9;
                        List list4 = (List) $continuation.L$8;
                        List list5 = (List) $continuation.L$7;
                        List list6 = (List) $continuation.L$6;
                        String str2 = (String) $continuation.L$5;
                        ArrayList arrayList7 = (ArrayList) $continuation.L$4;
                        PostData postData = (PostData) $continuation.L$3;
                        String str3 = (String) $continuation.L$2;
                        String str4 = (String) $continuation.L$1;
                        DisneyStudioProvider disneyStudioProvider3 = (DisneyStudioProvider) $continuation.L$0;
                        ResultKt.throwOnFailure($result);
                        return $result;
                    default:
                        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }
            }
        }
        disneyStudioProvider$load$1 = new DisneyStudioProvider$load$1($this8, continuation);
        $continuation = disneyStudioProvider$load$1;
        Object $result2 = $continuation.result;
        Object coroutine_suspended2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
        switch ($continuation.label) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit load$lambda$4$0(Suggest $it, DisneyStudioProvider this$0, AnimeSearchResponse $this$newAnimeSearchResponse) {
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
    /* JADX WARN: Removed duplicated region for block: B:10:0x002a  */
    /* JADX WARN: Removed duplicated region for block: B:12:0x0032  */
    /* JADX WARN: Removed duplicated region for block: B:13:0x005a  */
    /* JADX WARN: Removed duplicated region for block: B:16:0x011b A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:17:0x011c  */
    /* JADX WARN: Removed duplicated region for block: B:20:0x0149  */
    /* JADX WARN: Removed duplicated region for block: B:25:0x0195  */
    /* JADX WARN: Removed duplicated region for block: B:28:0x01a2  */
    /* JADX WARN: Removed duplicated region for block: B:29:0x01ae A[RETURN] */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:17:0x011c -> B:18:0x0127). Please submit an issue!!! */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public final Object getEpisodes(String title, String eid, String sid, int page, Continuation<? super List<Episode>> continuation) {
        DisneyStudioProvider$getEpisodes$1 disneyStudioProvider$getEpisodes$1;
        DisneyStudioProvider disneyStudioProvider;
        DisneyStudioProvider disneyStudioProvider2;
        Object obj;
        ArrayList episodes;
        int page2;
        DisneyStudioProvider$getEpisodes$1 disneyStudioProvider$getEpisodes$12;
        String title2;
        String eid2;
        int pg;
        String sid2;
        Iterable episodes2;
        String eid3;
        EpisodesData data;
        int page3;
        if (continuation instanceof DisneyStudioProvider$getEpisodes$1) {
            disneyStudioProvider$getEpisodes$1 = (DisneyStudioProvider$getEpisodes$1) continuation;
            if ((disneyStudioProvider$getEpisodes$1.label & Integer.MIN_VALUE) != 0) {
                disneyStudioProvider$getEpisodes$1.label -= Integer.MIN_VALUE;
                disneyStudioProvider = this;
                Object $result = disneyStudioProvider$getEpisodes$1.result;
                Object coroutine_suspended = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (disneyStudioProvider$getEpisodes$1.label) {
                    case 0:
                        ResultKt.throwOnFailure($result);
                        ArrayList episodes3 = new ArrayList();
                        disneyStudioProvider2 = disneyStudioProvider;
                        obj = coroutine_suspended;
                        episodes = episodes3;
                        int pg2 = page;
                        String sid3 = sid;
                        page2 = page;
                        disneyStudioProvider$getEpisodes$12 = disneyStudioProvider$getEpisodes$1;
                        title2 = title;
                        eid2 = eid;
                        Map<String, String> buildCookies = disneyStudioProvider2.buildCookies();
                        disneyStudioProvider$getEpisodes$12.L$0 = title2;
                        disneyStudioProvider$getEpisodes$12.L$1 = eid2;
                        disneyStudioProvider$getEpisodes$12.L$2 = sid3;
                        disneyStudioProvider$getEpisodes$12.L$3 = episodes;
                        disneyStudioProvider$getEpisodes$12.I$0 = page2;
                        disneyStudioProvider$getEpisodes$12.I$1 = pg2;
                        String title3 = title2;
                        disneyStudioProvider$getEpisodes$12.label = 1;
                        DisneyStudioProvider disneyStudioProvider3 = disneyStudioProvider2;
                        Object obj2 = obj;
                        ArrayList episodes4 = episodes;
                        DisneyStudioProvider$getEpisodes$1 disneyStudioProvider$getEpisodes$13 = disneyStudioProvider$getEpisodes$12;
                        pg = pg2;
                        Object obj3 = Requests.get$default(UtilsKt.getApp(), disneyStudioProvider2.getMainUrl() + "/mobile/hs/episodes.php?s=" + sid3 + "&series=" + eid2 + "&t=" + APIHolder.INSTANCE.getUnixTime() + "&page=" + pg2, disneyStudioProvider2.headers, disneyStudioProvider2.getMainUrl() + "/home", (Map) null, buildCookies, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, disneyStudioProvider$getEpisodes$13, 4072, (Object) null);
                        if (obj3 != obj2) {
                            return obj2;
                        }
                        disneyStudioProvider2 = disneyStudioProvider3;
                        obj = obj2;
                        sid2 = sid3;
                        $result = obj3;
                        disneyStudioProvider$getEpisodes$12 = disneyStudioProvider$getEpisodes$13;
                        episodes = episodes4;
                        title2 = title3;
                        NiceResponse this_$iv = (NiceResponse) $result;
                        ResponseParser parser = this_$iv.getParser();
                        Intrinsics.checkNotNull(parser);
                        EpisodesData data2 = (EpisodesData) parser.parse(this_$iv.getText(), Reflection.getOrCreateKotlinClass(EpisodesData.class));
                        episodes2 = data2.getEpisodes();
                        if (episodes2 != null) {
                            eid3 = eid2;
                            data = data2;
                            page3 = page2;
                        } else {
                            Iterable $this$mapTo$iv = episodes2;
                            ArrayList destination$iv = episodes;
                            for (Object item$iv : $this$mapTo$iv) {
                                String eid4 = eid2;
                                final com.horis.cncverse.entities.Episode it = (com.horis.cncverse.entities.Episode) item$iv;
                                destination$iv.add(MainAPIKt.newEpisode(disneyStudioProvider2, new LoadData(title2, it.getId()), new Function1() { // from class: com.horis.cncverse.DisneyStudioProvider$$ExternalSyntheticLambda8
                                    public final Object invoke(Object obj4) {
                                        Unit episodes$lambda$0$0;
                                        episodes$lambda$0$0 = DisneyStudioProvider.getEpisodes$lambda$0$0(com.horis.cncverse.entities.Episode.this, (Episode) obj4);
                                        return episodes$lambda$0$0;
                                    }
                                }));
                                eid2 = eid4;
                                data2 = data2;
                                page2 = page2;
                            }
                            eid3 = eid2;
                            data = data2;
                            page3 = page2;
                        }
                        if (data.getNextPageShow() == 0) {
                            page2 = page3;
                            sid3 = sid2;
                            pg2 = pg + 1;
                            eid2 = eid3;
                            Map<String, String> buildCookies2 = disneyStudioProvider2.buildCookies();
                            disneyStudioProvider$getEpisodes$12.L$0 = title2;
                            disneyStudioProvider$getEpisodes$12.L$1 = eid2;
                            disneyStudioProvider$getEpisodes$12.L$2 = sid3;
                            disneyStudioProvider$getEpisodes$12.L$3 = episodes;
                            disneyStudioProvider$getEpisodes$12.I$0 = page2;
                            disneyStudioProvider$getEpisodes$12.I$1 = pg2;
                            String title32 = title2;
                            disneyStudioProvider$getEpisodes$12.label = 1;
                            DisneyStudioProvider disneyStudioProvider32 = disneyStudioProvider2;
                            Object obj22 = obj;
                            ArrayList episodes42 = episodes;
                            DisneyStudioProvider$getEpisodes$1 disneyStudioProvider$getEpisodes$132 = disneyStudioProvider$getEpisodes$12;
                            pg = pg2;
                            Object obj32 = Requests.get$default(UtilsKt.getApp(), disneyStudioProvider2.getMainUrl() + "/mobile/hs/episodes.php?s=" + sid3 + "&series=" + eid2 + "&t=" + APIHolder.INSTANCE.getUnixTime() + "&page=" + pg2, disneyStudioProvider2.headers, disneyStudioProvider2.getMainUrl() + "/home", (Map) null, buildCookies2, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, disneyStudioProvider$getEpisodes$132, 4072, (Object) null);
                            if (obj32 != obj22) {
                            }
                        } else {
                            return episodes;
                        }
                    case 1:
                        int pg3 = disneyStudioProvider$getEpisodes$1.I$1;
                        int page4 = disneyStudioProvider$getEpisodes$1.I$0;
                        ArrayList episodes5 = (ArrayList) disneyStudioProvider$getEpisodes$1.L$3;
                        sid2 = (String) disneyStudioProvider$getEpisodes$1.L$2;
                        String eid5 = (String) disneyStudioProvider$getEpisodes$1.L$1;
                        String title4 = (String) disneyStudioProvider$getEpisodes$1.L$0;
                        ResultKt.throwOnFailure($result);
                        disneyStudioProvider$getEpisodes$12 = disneyStudioProvider$getEpisodes$1;
                        eid2 = eid5;
                        episodes = episodes5;
                        obj = coroutine_suspended;
                        page2 = page4;
                        pg = pg3;
                        title2 = title4;
                        disneyStudioProvider2 = disneyStudioProvider;
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
        disneyStudioProvider = this;
        disneyStudioProvider$getEpisodes$1 = new DisneyStudioProvider$getEpisodes$1(disneyStudioProvider, continuation);
        Object $result2 = disneyStudioProvider$getEpisodes$1.result;
        Object coroutine_suspended2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
        switch (disneyStudioProvider$getEpisodes$1.label) {
        }
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

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:10:0x0029  */
    /* JADX WARN: Removed duplicated region for block: B:12:0x0033  */
    /* JADX WARN: Removed duplicated region for block: B:13:0x00c4  */
    /* JADX WARN: Removed duplicated region for block: B:14:0x012b  */
    /* JADX WARN: Removed duplicated region for block: B:15:0x0166  */
    /* JADX WARN: Removed duplicated region for block: B:16:0x0190  */
    /* JADX WARN: Removed duplicated region for block: B:55:0x0423 A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:56:0x0424  */
    /* JADX WARN: Removed duplicated region for block: B:60:0x0463  */
    /* JADX WARN: Removed duplicated region for block: B:63:0x048d  */
    /* JADX WARN: Removed duplicated region for block: B:68:0x054a  */
    /* JADX WARN: Removed duplicated region for block: B:81:0x05d9  */
    /* JADX WARN: Removed duplicated region for block: B:86:0x06d8  */
    /* JADX WARN: Removed duplicated region for block: B:89:0x0718  */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:67:0x0539 -> B:61:0x0487). Please submit an issue!!! */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:78:0x0599 -> B:79:0x05d3). Please submit an issue!!! */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:84:0x0681 -> B:85:0x06aa). Please submit an issue!!! */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:87:0x0701 -> B:88:0x0714). Please submit an issue!!! */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static /* synthetic */ Object loadLinks$suspendImpl(DisneyStudioProvider $this, String data, boolean isCasting, Function1<? super SubtitleFile, Unit> function1, Function1<? super ExtractorLink, Unit> function12, Continuation<? super Boolean> continuation) {
        DisneyStudioProvider$loadLinks$1 disneyStudioProvider$loadLinks$1;
        Object $result;
        char c;
        char c2;
        Object $result2;
        Function1 subtitleCallback;
        Function1 callback;
        String data2;
        boolean isCasting2;
        Object obj;
        DisneyStudioProvider $this2;
        boolean z;
        boolean isCasting3;
        Map playlistHeaders;
        DisneyStudioProvider$loadLinks$1 disneyStudioProvider$loadLinks$12;
        Object obj2;
        Object cookies;
        String data3;
        Function1 callback2;
        Function1 subtitleCallback2;
        String title;
        String cookieStr;
        String id;
        Iterator<PlayListItem> it;
        String data4;
        Function1 subtitleCallback3;
        DisneyStudioProvider $this3;
        Continuation $completion;
        Function1 callback3;
        Iterator<Source> it2;
        PlayListItem item;
        Iterator<PlayListItem> it3;
        PlayList playlist;
        Map playlistHeaders2;
        String cookieStr2;
        boolean isCasting4;
        DisneyStudioProvider$loadLinks$1 disneyStudioProvider$loadLinks$13;
        String title2;
        String title3;
        String data5;
        Function1 callback4;
        Object cookies2;
        Function1 subtitleCallback4;
        String cookieStr3;
        Object cookies3;
        String id2;
        String data6;
        boolean isCasting5;
        Continuation $completion2;
        String cookieStr4;
        Collection destination$iv$iv;
        boolean isCasting6;
        int $i$f$map;
        Iterable $this$map$iv;
        PlayListItem item2;
        Function1 subtitleCallback5;
        Iterator it4;
        DisneyStudioProvider $this4;
        Iterable $this$mapTo$iv$iv;
        int $i$f$mapTo;
        Object $result3;
        DisneyStudioProvider $this5;
        Object newSubtitleFile;
        Function1 callback5;
        int $i$f$map2;
        Object item$iv$iv;
        Iterator<PlayListItem> it5;
        int $i$f$mapTo2;
        Iterable $this$map$iv2;
        boolean isCasting7;
        Collection collection;
        Function1 function13;
        Function1 subtitleCallback6;
        String title4;
        String cookieStr5;
        String title5;
        String id3;
        PlayList playlist2;
        Collection destination$iv$iv2;
        Map playlistHeaders3;
        Iterator it6;
        Continuation $completion3;
        PlayListItem item3;
        Iterable $this$mapTo$iv$iv2;
        DisneyStudioProvider $this6 = $this;
        if (continuation instanceof DisneyStudioProvider$loadLinks$1) {
            disneyStudioProvider$loadLinks$1 = (DisneyStudioProvider$loadLinks$1) continuation;
            if ((disneyStudioProvider$loadLinks$1.label & Integer.MIN_VALUE) != 0) {
                disneyStudioProvider$loadLinks$1.label -= Integer.MIN_VALUE;
                $result = disneyStudioProvider$loadLinks$1.result;
                Object coroutine_suspended = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (disneyStudioProvider$loadLinks$1.label) {
                    case 0:
                        c = '\n';
                        c2 = 4;
                        ResultKt.throwOnFailure($result);
                        if (SubscriptionHelper.INSTANCE.isSubscribed(context)) {
                            $result2 = $result;
                        } else {
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
                                    new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.horis.cncverse.DisneyStudioProvider$$ExternalSyntheticLambda0
                                        @Override // java.lang.Runnable
                                        public final void run() {
                                            DisneyStudioProvider.loadLinks$lambda$0$0(_ctx);
                                        }
                                    });
                                }
                                $result2 = $result;
                                $this.openInExternalBrowser(new String(Base64.decode(OMG10, 0), Charsets.UTF_8));
                            }
                        }
                        String mainUrl = $this6.getMainUrl();
                        disneyStudioProvider$loadLinks$1.L$0 = $this6;
                        disneyStudioProvider$loadLinks$1.L$1 = data;
                        subtitleCallback = function1;
                        disneyStudioProvider$loadLinks$1.L$2 = subtitleCallback;
                        callback = function12;
                        disneyStudioProvider$loadLinks$1.L$3 = callback;
                        disneyStudioProvider$loadLinks$1.L$4 = $this6;
                        disneyStudioProvider$loadLinks$1.Z$0 = isCasting;
                        disneyStudioProvider$loadLinks$1.label = 1;
                        Object bypass = UtilsKt.bypass(mainUrl, disneyStudioProvider$loadLinks$1);
                        if (bypass != coroutine_suspended) {
                            data2 = data;
                            isCasting2 = isCasting;
                            obj = bypass;
                            $this2 = $this6;
                            break;
                        } else {
                            return coroutine_suspended;
                        }
                    case 1:
                        c = '\n';
                        c2 = 4;
                        boolean isCasting8 = disneyStudioProvider$loadLinks$1.Z$0;
                        callback = (Function1) disneyStudioProvider$loadLinks$1.L$3;
                        ResultKt.throwOnFailure($result);
                        $result2 = $result;
                        data2 = (String) disneyStudioProvider$loadLinks$1.L$1;
                        isCasting2 = isCasting8;
                        $this2 = (DisneyStudioProvider) disneyStudioProvider$loadLinks$1.L$0;
                        $this6 = (DisneyStudioProvider) disneyStudioProvider$loadLinks$1.L$4;
                        subtitleCallback = (Function1) disneyStudioProvider$loadLinks$1.L$2;
                        obj = $result2;
                        break;
                    case 2:
                        boolean isCasting9 = disneyStudioProvider$loadLinks$1.Z$0;
                        playlistHeaders = (Map) disneyStudioProvider$loadLinks$1.L$8;
                        cookieStr = (String) disneyStudioProvider$loadLinks$1.L$7;
                        cookies = (Map) disneyStudioProvider$loadLinks$1.L$6;
                        id = (String) disneyStudioProvider$loadLinks$1.L$5;
                        title = (String) disneyStudioProvider$loadLinks$1.L$4;
                        callback2 = (Function1) disneyStudioProvider$loadLinks$1.L$3;
                        subtitleCallback2 = (Function1) disneyStudioProvider$loadLinks$1.L$2;
                        data3 = (String) disneyStudioProvider$loadLinks$1.L$1;
                        ResultKt.throwOnFailure($result);
                        isCasting3 = isCasting9;
                        disneyStudioProvider$loadLinks$12 = disneyStudioProvider$loadLinks$1;
                        $result2 = $result;
                        obj2 = coroutine_suspended;
                        z = true;
                        $this2 = (DisneyStudioProvider) disneyStudioProvider$loadLinks$1.L$0;
                        NiceResponse this_$iv = (NiceResponse) $result;
                        ResponseParser parser = this_$iv.getParser();
                        Intrinsics.checkNotNull(parser);
                        PlayList playlist3 = (PlayList) parser.parse(this_$iv.getText(), Reflection.getOrCreateKotlinClass(PlayList.class));
                        it = playlist3.iterator();
                        data4 = data3;
                        subtitleCallback3 = subtitleCallback2;
                        Function1 subtitleCallback7 = callback2;
                        String title6 = title;
                        String id4 = id;
                        Object cookies4 = cookies;
                        String cookieStr6 = cookieStr;
                        Map playlistHeaders4 = playlistHeaders;
                        Object obj3 = obj2;
                        $this3 = $this2;
                        $completion = continuation;
                        if (!it.hasNext()) {
                            PlayListItem item4 = it.next();
                            String str = id4;
                            it3 = it;
                            coroutine_suspended = obj3;
                            callback3 = subtitleCallback7;
                            playlistHeaders2 = playlistHeaders4;
                            cookieStr3 = cookieStr6;
                            it2 = item4.getSources().iterator();
                            id2 = str;
                            cookies3 = cookies4;
                            item = item4;
                            data6 = title6;
                            disneyStudioProvider$loadLinks$13 = disneyStudioProvider$loadLinks$12;
                            playlist = playlist3;
                            isCasting5 = isCasting3;
                            if (it2.hasNext()) {
                                Source source = it2.next();
                                Continuation $completion4 = $completion;
                                String cookieStr7 = cookieStr3;
                                DisneyStudioProvider$loadLinks$1 disneyStudioProvider$loadLinks$14 = disneyStudioProvider$loadLinks$13;
                                disneyStudioProvider$loadLinks$14.L$0 = $this3;
                                disneyStudioProvider$loadLinks$14.L$1 = SpillingKt.nullOutSpilledVariable(data4);
                                disneyStudioProvider$loadLinks$14.L$2 = subtitleCallback3;
                                disneyStudioProvider$loadLinks$14.L$3 = callback3;
                                disneyStudioProvider$loadLinks$14.L$4 = SpillingKt.nullOutSpilledVariable(data6);
                                disneyStudioProvider$loadLinks$14.L$5 = SpillingKt.nullOutSpilledVariable(id2);
                                disneyStudioProvider$loadLinks$14.L$6 = SpillingKt.nullOutSpilledVariable(cookies3);
                                disneyStudioProvider$loadLinks$14.L$7 = SpillingKt.nullOutSpilledVariable(cookieStr7);
                                disneyStudioProvider$loadLinks$14.L$8 = playlistHeaders2;
                                disneyStudioProvider$loadLinks$14.L$9 = SpillingKt.nullOutSpilledVariable(playlist);
                                disneyStudioProvider$loadLinks$14.L$10 = it3;
                                disneyStudioProvider$loadLinks$14.L$11 = item;
                                disneyStudioProvider$loadLinks$14.L$12 = it2;
                                disneyStudioProvider$loadLinks$14.L$13 = SpillingKt.nullOutSpilledVariable(source);
                                disneyStudioProvider$loadLinks$14.L$14 = callback3;
                                disneyStudioProvider$loadLinks$14.L$15 = null;
                                disneyStudioProvider$loadLinks$14.L$16 = null;
                                disneyStudioProvider$loadLinks$14.L$17 = null;
                                disneyStudioProvider$loadLinks$14.L$18 = null;
                                disneyStudioProvider$loadLinks$14.L$19 = null;
                                disneyStudioProvider$loadLinks$14.Z$0 = isCasting5;
                                disneyStudioProvider$loadLinks$14.label = 3;
                                Object newExtractorLink = ExtractorApiKt.newExtractorLink($this3.getName(), source.getLabel(), $this3.getMainUrl() + source.getFile(), ExtractorLinkType.M3U8, new DisneyStudioProvider$loadLinks$3(playlistHeaders2, $this3, source, null), disneyStudioProvider$loadLinks$14);
                                if (newExtractorLink == coroutine_suspended) {
                                    return coroutine_suspended;
                                }
                                boolean z2 = isCasting5;
                                $result = newExtractorLink;
                                $completion = $completion4;
                                isCasting4 = z2;
                                disneyStudioProvider$loadLinks$13 = disneyStudioProvider$loadLinks$14;
                                title3 = data6;
                                callback4 = callback3;
                                title2 = id2;
                                cookies2 = cookies3;
                                data5 = data4;
                                subtitleCallback4 = subtitleCallback3;
                                cookieStr2 = cookieStr7;
                                callback3.invoke($result);
                                callback3 = callback4;
                                cookieStr3 = cookieStr2;
                                subtitleCallback3 = subtitleCallback4;
                                data4 = data5;
                                cookies3 = cookies2;
                                id2 = title2;
                                data6 = title3;
                                isCasting5 = isCasting4;
                                if (it2.hasNext()) {
                                    $completion2 = $completion;
                                    cookieStr4 = cookieStr3;
                                    DisneyStudioProvider$loadLinks$1 disneyStudioProvider$loadLinks$15 = disneyStudioProvider$loadLinks$13;
                                    Iterable tracks = item.getTracks();
                                    if (tracks != null) {
                                        Iterable $this$filter$iv = tracks;
                                        Collection destination$iv$iv3 = new ArrayList();
                                        for (Object element$iv$iv : $this$filter$iv) {
                                            Tracks it7 = (Tracks) element$iv$iv;
                                            Iterable $this$filter$iv2 = $this$filter$iv;
                                            DisneyStudioProvider $this7 = $this3;
                                            if (Intrinsics.areEqual(it7.getKind(), "captions")) {
                                                destination$iv$iv3.add(element$iv$iv);
                                            }
                                            $this3 = $this7;
                                            $this$filter$iv = $this$filter$iv2;
                                        }
                                        Iterable $this$map$iv3 = (List) destination$iv$iv3;
                                        int $i$f$map3 = CollectionsKt.collectionSizeOrDefault($this$map$iv3, 10);
                                        boolean z3 = isCasting5;
                                        destination$iv$iv = new ArrayList($i$f$map3);
                                        isCasting6 = z3;
                                        $i$f$map = 0;
                                        $this$map$iv = $this$map$iv3;
                                        item2 = item;
                                        subtitleCallback5 = subtitleCallback3;
                                        it4 = $this$map$iv3.iterator();
                                        $this4 = $this3;
                                        $this$mapTo$iv$iv = $this$map$iv3;
                                        disneyStudioProvider$loadLinks$1 = disneyStudioProvider$loadLinks$15;
                                        $i$f$mapTo = 0;
                                        $result3 = $result2;
                                        if (it4.hasNext()) {
                                            Object item$iv$iv2 = it4.next();
                                            Tracks track = (Tracks) item$iv$iv2;
                                            PlayList playlist4 = playlist;
                                            Iterable $this$mapTo$iv$iv3 = $this$mapTo$iv$iv;
                                            String valueOf = String.valueOf(track.getLabel());
                                            String data7 = data4;
                                            String data8 = ExtractorApiKt.httpsify(StringsKt.replace$default(String.valueOf(track.getFile()), "\\", "", false, 4, (Object) null));
                                            Object cookies5 = cookies3;
                                            String id5 = id2;
                                            disneyStudioProvider$loadLinks$1.L$0 = $this4;
                                            disneyStudioProvider$loadLinks$1.L$1 = SpillingKt.nullOutSpilledVariable(data7);
                                            disneyStudioProvider$loadLinks$1.L$2 = subtitleCallback5;
                                            disneyStudioProvider$loadLinks$1.L$3 = callback3;
                                            disneyStudioProvider$loadLinks$1.L$4 = SpillingKt.nullOutSpilledVariable(data6);
                                            disneyStudioProvider$loadLinks$1.L$5 = SpillingKt.nullOutSpilledVariable(id5);
                                            disneyStudioProvider$loadLinks$1.L$6 = SpillingKt.nullOutSpilledVariable(cookies5);
                                            disneyStudioProvider$loadLinks$1.L$7 = SpillingKt.nullOutSpilledVariable(cookieStr4);
                                            disneyStudioProvider$loadLinks$1.L$8 = playlistHeaders2;
                                            disneyStudioProvider$loadLinks$1.L$9 = SpillingKt.nullOutSpilledVariable(playlist4);
                                            disneyStudioProvider$loadLinks$1.L$10 = it3;
                                            disneyStudioProvider$loadLinks$1.L$11 = SpillingKt.nullOutSpilledVariable(item2);
                                            disneyStudioProvider$loadLinks$1.L$12 = SpillingKt.nullOutSpilledVariable($this$map$iv);
                                            disneyStudioProvider$loadLinks$1.L$13 = SpillingKt.nullOutSpilledVariable($this$mapTo$iv$iv3);
                                            disneyStudioProvider$loadLinks$1.L$14 = destination$iv$iv;
                                            Iterator it8 = it4;
                                            disneyStudioProvider$loadLinks$1.L$15 = it8;
                                            $this5 = $this4;
                                            disneyStudioProvider$loadLinks$1.L$16 = SpillingKt.nullOutSpilledVariable(item$iv$iv2);
                                            disneyStudioProvider$loadLinks$1.L$17 = SpillingKt.nullOutSpilledVariable(track);
                                            disneyStudioProvider$loadLinks$1.L$18 = subtitleCallback5;
                                            disneyStudioProvider$loadLinks$1.L$19 = destination$iv$iv;
                                            disneyStudioProvider$loadLinks$1.Z$0 = isCasting6;
                                            disneyStudioProvider$loadLinks$1.I$0 = $i$f$map;
                                            disneyStudioProvider$loadLinks$1.I$1 = $i$f$mapTo;
                                            disneyStudioProvider$loadLinks$1.I$2 = 0;
                                            disneyStudioProvider$loadLinks$1.label = 4;
                                            newSubtitleFile = MainAPIKt.newSubtitleFile(valueOf, data8, new DisneyStudioProvider$loadLinks$5$1($this4, null), disneyStudioProvider$loadLinks$1);
                                            if (newSubtitleFile == coroutine_suspended) {
                                                return coroutine_suspended;
                                            }
                                            int i = $i$f$mapTo;
                                            callback5 = callback3;
                                            $i$f$map2 = $i$f$map;
                                            item$iv$iv = cookies5;
                                            it5 = it3;
                                            $i$f$mapTo2 = i;
                                            $this$map$iv2 = $this$map$iv;
                                            isCasting7 = isCasting6;
                                            collection = destination$iv$iv;
                                            function13 = subtitleCallback5;
                                            subtitleCallback6 = function13;
                                            title4 = data6;
                                            cookieStr5 = cookieStr4;
                                            title5 = data7;
                                            id3 = id5;
                                            $result = $result3;
                                            playlist2 = playlist4;
                                            destination$iv$iv2 = collection;
                                            playlistHeaders3 = playlistHeaders2;
                                            it6 = it8;
                                            $completion3 = $completion2;
                                            item3 = item2;
                                            $this$mapTo$iv$iv2 = $this$mapTo$iv$iv3;
                                            function13.invoke(newSubtitleFile);
                                            collection.add(Unit.INSTANCE);
                                            $this$map$iv = $this$map$iv2;
                                            $result3 = $result;
                                            callback3 = callback5;
                                            destination$iv$iv = destination$iv$iv2;
                                            $i$f$mapTo = $i$f$mapTo2;
                                            $i$f$map = $i$f$map2;
                                            it4 = it6;
                                            isCasting6 = isCasting7;
                                            subtitleCallback5 = subtitleCallback6;
                                            $this$mapTo$iv$iv = $this$mapTo$iv$iv2;
                                            data4 = title5;
                                            cookies3 = item$iv$iv;
                                            item2 = item3;
                                            it3 = it5;
                                            playlist = playlist2;
                                            playlistHeaders2 = playlistHeaders3;
                                            $completion2 = $completion3;
                                            $this4 = $this5;
                                            id2 = id3;
                                            data6 = title4;
                                            cookieStr4 = cookieStr5;
                                            if (it4.hasNext()) {
                                                DisneyStudioProvider $this8 = $this4;
                                                ArrayList arrayList = (List) destination$iv$iv;
                                                playlist3 = playlist;
                                                disneyStudioProvider$loadLinks$12 = disneyStudioProvider$loadLinks$1;
                                                subtitleCallback3 = subtitleCallback5;
                                                playlistHeaders4 = playlistHeaders2;
                                                $this3 = $this8;
                                                $result2 = $result3;
                                                isCasting3 = isCasting6;
                                                subtitleCallback7 = callback3;
                                                obj3 = coroutine_suspended;
                                                it = it3;
                                                id4 = id2;
                                                title6 = data6;
                                                cookieStr6 = cookieStr4;
                                                $completion = $completion2;
                                                cookies4 = cookies3;
                                                if (!it.hasNext()) {
                                                    return Boxing.boxBoolean(z);
                                                }
                                            }
                                        }
                                    } else {
                                        isCasting3 = isCasting5;
                                        disneyStudioProvider$loadLinks$12 = disneyStudioProvider$loadLinks$15;
                                        playlist3 = playlist;
                                        playlistHeaders4 = playlistHeaders2;
                                        subtitleCallback7 = callback3;
                                        obj3 = coroutine_suspended;
                                        it = it3;
                                        id4 = id2;
                                        title6 = data6;
                                        $completion = $completion2;
                                        cookieStr6 = cookieStr4;
                                        cookies4 = cookies3;
                                        if (!it.hasNext()) {
                                        }
                                    }
                                }
                            }
                        }
                    case 3:
                        boolean isCasting10 = disneyStudioProvider$loadLinks$1.Z$0;
                        callback3 = (Function1) disneyStudioProvider$loadLinks$1.L$14;
                        Source source2 = (Source) disneyStudioProvider$loadLinks$1.L$13;
                        it2 = (Iterator) disneyStudioProvider$loadLinks$1.L$12;
                        item = (PlayListItem) disneyStudioProvider$loadLinks$1.L$11;
                        it3 = (Iterator) disneyStudioProvider$loadLinks$1.L$10;
                        playlist = (PlayList) disneyStudioProvider$loadLinks$1.L$9;
                        playlistHeaders2 = (Map) disneyStudioProvider$loadLinks$1.L$8;
                        cookieStr2 = (String) disneyStudioProvider$loadLinks$1.L$7;
                        isCasting4 = isCasting10;
                        Function1 callback6 = (Function1) disneyStudioProvider$loadLinks$1.L$3;
                        Function1 subtitleCallback8 = (Function1) disneyStudioProvider$loadLinks$1.L$2;
                        String data9 = (String) disneyStudioProvider$loadLinks$1.L$1;
                        DisneyStudioProvider $this9 = (DisneyStudioProvider) disneyStudioProvider$loadLinks$1.L$0;
                        ResultKt.throwOnFailure($result);
                        $completion = continuation;
                        disneyStudioProvider$loadLinks$13 = disneyStudioProvider$loadLinks$1;
                        $result2 = $result;
                        title2 = (String) disneyStudioProvider$loadLinks$1.L$5;
                        title3 = (String) disneyStudioProvider$loadLinks$1.L$4;
                        z = true;
                        data5 = data9;
                        callback4 = callback6;
                        $this3 = $this9;
                        cookies2 = (Map) disneyStudioProvider$loadLinks$1.L$6;
                        subtitleCallback4 = subtitleCallback8;
                        callback3.invoke($result);
                        callback3 = callback4;
                        cookieStr3 = cookieStr2;
                        subtitleCallback3 = subtitleCallback4;
                        data4 = data5;
                        cookies3 = cookies2;
                        id2 = title2;
                        data6 = title3;
                        isCasting5 = isCasting4;
                        if (it2.hasNext()) {
                        }
                        break;
                    case 4:
                        int i2 = disneyStudioProvider$loadLinks$1.I$2;
                        $i$f$mapTo2 = disneyStudioProvider$loadLinks$1.I$1;
                        int $i$f$map4 = disneyStudioProvider$loadLinks$1.I$0;
                        isCasting7 = disneyStudioProvider$loadLinks$1.Z$0;
                        Tracks tracks2 = (Tracks) disneyStudioProvider$loadLinks$1.L$17;
                        Object obj4 = disneyStudioProvider$loadLinks$1.L$16;
                        it6 = (Iterator) disneyStudioProvider$loadLinks$1.L$15;
                        Iterable $this$mapTo$iv$iv4 = (Iterable) disneyStudioProvider$loadLinks$1.L$13;
                        Iterable $this$map$iv4 = (Iterable) disneyStudioProvider$loadLinks$1.L$12;
                        item3 = (PlayListItem) disneyStudioProvider$loadLinks$1.L$11;
                        it5 = (Iterator) disneyStudioProvider$loadLinks$1.L$10;
                        playlist2 = (PlayList) disneyStudioProvider$loadLinks$1.L$9;
                        playlistHeaders3 = (Map) disneyStudioProvider$loadLinks$1.L$8;
                        String cookieStr8 = (String) disneyStudioProvider$loadLinks$1.L$7;
                        Object cookies6 = (Map) disneyStudioProvider$loadLinks$1.L$6;
                        id3 = (String) disneyStudioProvider$loadLinks$1.L$5;
                        title4 = (String) disneyStudioProvider$loadLinks$1.L$4;
                        Function1 callback7 = (Function1) disneyStudioProvider$loadLinks$1.L$3;
                        Function1 subtitleCallback9 = (Function1) disneyStudioProvider$loadLinks$1.L$2;
                        String data10 = (String) disneyStudioProvider$loadLinks$1.L$1;
                        DisneyStudioProvider $this10 = (DisneyStudioProvider) disneyStudioProvider$loadLinks$1.L$0;
                        ResultKt.throwOnFailure($result);
                        title5 = data10;
                        $this$map$iv2 = $this$map$iv4;
                        function13 = (Function1) disneyStudioProvider$loadLinks$1.L$18;
                        cookieStr5 = cookieStr8;
                        item$iv$iv = cookies6;
                        z = true;
                        callback5 = callback7;
                        $completion3 = continuation;
                        $this5 = $this10;
                        $this$mapTo$iv$iv2 = $this$mapTo$iv$iv4;
                        $i$f$map2 = $i$f$map4;
                        collection = (Collection) disneyStudioProvider$loadLinks$1.L$19;
                        subtitleCallback6 = subtitleCallback9;
                        newSubtitleFile = $result;
                        destination$iv$iv2 = (Collection) disneyStudioProvider$loadLinks$1.L$14;
                        function13.invoke(newSubtitleFile);
                        collection.add(Unit.INSTANCE);
                        $this$map$iv = $this$map$iv2;
                        $result3 = $result;
                        callback3 = callback5;
                        destination$iv$iv = destination$iv$iv2;
                        $i$f$mapTo = $i$f$mapTo2;
                        $i$f$map = $i$f$map2;
                        it4 = it6;
                        isCasting6 = isCasting7;
                        subtitleCallback5 = subtitleCallback6;
                        $this$mapTo$iv$iv = $this$mapTo$iv$iv2;
                        data4 = title5;
                        cookies3 = item$iv$iv;
                        item2 = item3;
                        it3 = it5;
                        playlist = playlist2;
                        playlistHeaders2 = playlistHeaders3;
                        $completion2 = $completion3;
                        $this4 = $this5;
                        id2 = id3;
                        data6 = title4;
                        cookieStr4 = cookieStr5;
                        if (it4.hasNext()) {
                        }
                        break;
                    default:
                        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }
                $this6.cookie_value = (String) obj;
                String text$iv = data2;
                LoadData loadData = (LoadData) UtilsKt.getJSONParser().parse(text$iv, Reflection.getOrCreateKotlinClass(LoadData.class));
                String title7 = loadData.component1();
                String id6 = loadData.component2();
                Map cookies7 = MapsKt.mapOf(new Pair[]{TuplesKt.to("t_hash_t", $this2.cookie_value), TuplesKt.to("ott", "hs"), TuplesKt.to("hd", "on")});
                String cookieStr9 = CollectionsKt.joinToString$default(cookies7.entrySet(), "; ", (CharSequence) null, (CharSequence) null, 0, (CharSequence) null, new Function1() { // from class: com.horis.cncverse.DisneyStudioProvider$$ExternalSyntheticLambda1
                    public final Object invoke(Object obj5) {
                        CharSequence loadLinks$lambda$1;
                        loadLinks$lambda$1 = DisneyStudioProvider.loadLinks$lambda$1((Map.Entry) obj5);
                        return loadLinks$lambda$1;
                    }
                }, 30, (Object) null);
                Pair[] pairArr = new Pair[13];
                pairArr[0] = TuplesKt.to("Accept", "*/*");
                z = true;
                pairArr[1] = TuplesKt.to("Accept-Language", "en-IN,en-US;q=0.9,en;q=0.8");
                pairArr[2] = TuplesKt.to("Connection", "keep-alive");
                pairArr[3] = TuplesKt.to("Cookie", cookieStr9);
                pairArr[c2] = TuplesKt.to("Referer", $this2.getMainUrl() + "/mobile/home?app=1");
                pairArr[5] = TuplesKt.to("sec-ch-ua", "\"Android WebView\";v=\"149\", \"Chromium\";v=\"149\", \"Not)A;Brand\";v=\"24\"");
                pairArr[6] = TuplesKt.to("sec-ch-ua-mobile", "?0");
                pairArr[7] = TuplesKt.to("sec-ch-ua-platform", "\"Android\"");
                pairArr[8] = TuplesKt.to("Sec-Fetch-Dest", "empty");
                pairArr[9] = TuplesKt.to("Sec-Fetch-Mode", "cors");
                pairArr[c] = TuplesKt.to("Sec-Fetch-Site", "same-origin");
                pairArr[11] = TuplesKt.to("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/149.0.7827.91 Safari/537.36 /OS.Gatu v3.0");
                pairArr[12] = TuplesKt.to("X-Requested-With", "app.netmirror.netmirrornew");
                Map playlistHeaders5 = MapsKt.mapOf(pairArr);
                disneyStudioProvider$loadLinks$1.L$0 = $this2;
                disneyStudioProvider$loadLinks$1.L$1 = SpillingKt.nullOutSpilledVariable(data2);
                disneyStudioProvider$loadLinks$1.L$2 = subtitleCallback;
                disneyStudioProvider$loadLinks$1.L$3 = callback;
                disneyStudioProvider$loadLinks$1.L$4 = SpillingKt.nullOutSpilledVariable(title7);
                disneyStudioProvider$loadLinks$1.L$5 = SpillingKt.nullOutSpilledVariable(id6);
                disneyStudioProvider$loadLinks$1.L$6 = SpillingKt.nullOutSpilledVariable(cookies7);
                disneyStudioProvider$loadLinks$1.L$7 = SpillingKt.nullOutSpilledVariable(cookieStr9);
                disneyStudioProvider$loadLinks$1.L$8 = playlistHeaders5;
                disneyStudioProvider$loadLinks$1.Z$0 = isCasting2;
                disneyStudioProvider$loadLinks$1.label = 2;
                Function1 callback8 = callback;
                isCasting3 = isCasting2;
                Function1 subtitleCallback10 = subtitleCallback;
                playlistHeaders = playlistHeaders5;
                disneyStudioProvider$loadLinks$12 = disneyStudioProvider$loadLinks$1;
                obj2 = coroutine_suspended;
                $result = Requests.get$default(UtilsKt.getApp(), $this2.getMainUrl() + "/mobile/hs/playlist.php?id=" + id6 + "&t=" + title7 + "&tm=" + APIHolder.INSTANCE.getUnixTime(), playlistHeaders, $this2.getMainUrl() + "/mobile/home?app=1", (Map) null, cookies7, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, disneyStudioProvider$loadLinks$12, 4072, (Object) null);
                if ($result != obj2) {
                    return obj2;
                }
                cookies = cookies7;
                data3 = data2;
                callback2 = callback8;
                subtitleCallback2 = subtitleCallback10;
                title = title7;
                cookieStr = cookieStr9;
                id = id6;
                NiceResponse this_$iv2 = (NiceResponse) $result;
                ResponseParser parser2 = this_$iv2.getParser();
                Intrinsics.checkNotNull(parser2);
                PlayList playlist32 = (PlayList) parser2.parse(this_$iv2.getText(), Reflection.getOrCreateKotlinClass(PlayList.class));
                it = playlist32.iterator();
                data4 = data3;
                subtitleCallback3 = subtitleCallback2;
                Function1 subtitleCallback72 = callback2;
                String title62 = title;
                String id42 = id;
                Object cookies42 = cookies;
                String cookieStr62 = cookieStr;
                Map playlistHeaders42 = playlistHeaders;
                Object obj32 = obj2;
                $this3 = $this2;
                $completion = continuation;
                if (!it.hasNext()) {
                }
            }
        }
        disneyStudioProvider$loadLinks$1 = new DisneyStudioProvider$loadLinks$1($this6, continuation);
        $result = disneyStudioProvider$loadLinks$1.result;
        Object coroutine_suspended2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
        switch (disneyStudioProvider$loadLinks$1.label) {
        }
        $this6.cookie_value = (String) obj;
        String text$iv2 = data2;
        LoadData loadData2 = (LoadData) UtilsKt.getJSONParser().parse(text$iv2, Reflection.getOrCreateKotlinClass(LoadData.class));
        String title72 = loadData2.component1();
        String id62 = loadData2.component2();
        Map cookies72 = MapsKt.mapOf(new Pair[]{TuplesKt.to("t_hash_t", $this2.cookie_value), TuplesKt.to("ott", "hs"), TuplesKt.to("hd", "on")});
        String cookieStr92 = CollectionsKt.joinToString$default(cookies72.entrySet(), "; ", (CharSequence) null, (CharSequence) null, 0, (CharSequence) null, new Function1() { // from class: com.horis.cncverse.DisneyStudioProvider$$ExternalSyntheticLambda1
            public final Object invoke(Object obj5) {
                CharSequence loadLinks$lambda$1;
                loadLinks$lambda$1 = DisneyStudioProvider.loadLinks$lambda$1((Map.Entry) obj5);
                return loadLinks$lambda$1;
            }
        }, 30, (Object) null);
        Pair[] pairArr2 = new Pair[13];
        pairArr2[0] = TuplesKt.to("Accept", "*/*");
        z = true;
        pairArr2[1] = TuplesKt.to("Accept-Language", "en-IN,en-US;q=0.9,en;q=0.8");
        pairArr2[2] = TuplesKt.to("Connection", "keep-alive");
        pairArr2[3] = TuplesKt.to("Cookie", cookieStr92);
        pairArr2[c2] = TuplesKt.to("Referer", $this2.getMainUrl() + "/mobile/home?app=1");
        pairArr2[5] = TuplesKt.to("sec-ch-ua", "\"Android WebView\";v=\"149\", \"Chromium\";v=\"149\", \"Not)A;Brand\";v=\"24\"");
        pairArr2[6] = TuplesKt.to("sec-ch-ua-mobile", "?0");
        pairArr2[7] = TuplesKt.to("sec-ch-ua-platform", "\"Android\"");
        pairArr2[8] = TuplesKt.to("Sec-Fetch-Dest", "empty");
        pairArr2[9] = TuplesKt.to("Sec-Fetch-Mode", "cors");
        pairArr2[c] = TuplesKt.to("Sec-Fetch-Site", "same-origin");
        pairArr2[11] = TuplesKt.to("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/149.0.7827.91 Safari/537.36 /OS.Gatu v3.0");
        pairArr2[12] = TuplesKt.to("X-Requested-With", "app.netmirror.netmirrornew");
        Map playlistHeaders52 = MapsKt.mapOf(pairArr2);
        disneyStudioProvider$loadLinks$1.L$0 = $this2;
        disneyStudioProvider$loadLinks$1.L$1 = SpillingKt.nullOutSpilledVariable(data2);
        disneyStudioProvider$loadLinks$1.L$2 = subtitleCallback;
        disneyStudioProvider$loadLinks$1.L$3 = callback;
        disneyStudioProvider$loadLinks$1.L$4 = SpillingKt.nullOutSpilledVariable(title72);
        disneyStudioProvider$loadLinks$1.L$5 = SpillingKt.nullOutSpilledVariable(id62);
        disneyStudioProvider$loadLinks$1.L$6 = SpillingKt.nullOutSpilledVariable(cookies72);
        disneyStudioProvider$loadLinks$1.L$7 = SpillingKt.nullOutSpilledVariable(cookieStr92);
        disneyStudioProvider$loadLinks$1.L$8 = playlistHeaders52;
        disneyStudioProvider$loadLinks$1.Z$0 = isCasting2;
        disneyStudioProvider$loadLinks$1.label = 2;
        Function1 callback82 = callback;
        isCasting3 = isCasting2;
        Function1 subtitleCallback102 = subtitleCallback;
        playlistHeaders = playlistHeaders52;
        disneyStudioProvider$loadLinks$12 = disneyStudioProvider$loadLinks$1;
        obj2 = coroutine_suspended2;
        $result = Requests.get$default(UtilsKt.getApp(), $this2.getMainUrl() + "/mobile/hs/playlist.php?id=" + id62 + "&t=" + title72 + "&tm=" + APIHolder.INSTANCE.getUnixTime(), playlistHeaders, $this2.getMainUrl() + "/mobile/home?app=1", (Map) null, cookies72, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, disneyStudioProvider$loadLinks$12, 4072, (Object) null);
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
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.horis.cncverse.DisneyStudioProvider$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                DisneyStudioProvider.showSubscriptionPopupIfNeeded$lambda$0(ctx);
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
            laterTv.setOnClickListener(new View.OnClickListener() { // from class: com.horis.cncverse.DisneyStudioProvider$$ExternalSyntheticLambda9
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    DisneyStudioProvider.showSubscriptionPopupIfNeeded$lambda$0$11($this$showSubscriptionPopupIfNeeded_u24lambda_u240_u249, $ctx, dialog, view);
                }
            });
            subscribeTv.setOnClickListener(new View.OnClickListener() { // from class: com.horis.cncverse.DisneyStudioProvider$$ExternalSyntheticLambda10
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    DisneyStudioProvider.showSubscriptionPopupIfNeeded$lambda$0$12(dialog, $ctx, view);
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
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.horis.cncverse.DisneyStudioProvider$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                DisneyStudioProvider.showTelegramPopup$lambda$0(ctx);
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
            laterTv.setOnClickListener(new View.OnClickListener() { // from class: com.horis.cncverse.DisneyStudioProvider$$ExternalSyntheticLambda11
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    dialog.dismiss();
                }
            });
            joinTv.setOnClickListener(new View.OnClickListener() { // from class: com.horis.cncverse.DisneyStudioProvider$$ExternalSyntheticLambda12
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    DisneyStudioProvider.showTelegramPopup$lambda$0$9(dialog, $ctx, view);
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
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.horis.cncverse.DisneyStudioProvider$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                DisneyStudioProvider.openInExternalBrowser$lambda$0(ctx, url);
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

    /* compiled from: DisneyStudioProvider.kt */
    @Metadata(d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\t\u0010\b\u001a\u00020\u0003HÆ\u0003J\u0013\u0010\t\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003HÆ\u0001J\u0014\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\u0001HÖ\u0083\u0004J\n\u0010\r\u001a\u00020\u000eHÖ\u0081\u0004J\n\u0010\u000f\u001a\u00020\u0003HÖ\u0081\u0004R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007¨\u0006\u0010"}, d2 = {"Lcom/horis/cncverse/DisneyStudioProvider$Id;", "", "id", "", "<init>", "(Ljava/lang/String;)V", "getId", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "hashCode", "", "toString", "CNC Verse Mobile_debug"}, k = 1, mv = {2, 3, 0}, xi = 48)
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

    /* compiled from: DisneyStudioProvider.kt */
    @Metadata(d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\n\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u0017\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003¢\u0006\u0004\b\u0005\u0010\u0006J\t\u0010\n\u001a\u00020\u0003HÆ\u0003J\t\u0010\u000b\u001a\u00020\u0003HÆ\u0003J\u001d\u0010\f\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0003HÆ\u0001J\u0014\u0010\r\u001a\u00020\u000e2\b\u0010\u000f\u001a\u0004\u0018\u00010\u0001HÖ\u0083\u0004J\n\u0010\u0010\u001a\u00020\u0011HÖ\u0081\u0004J\n\u0010\u0012\u001a\u00020\u0003HÖ\u0081\u0004R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0004\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\t\u0010\b¨\u0006\u0013"}, d2 = {"Lcom/horis/cncverse/DisneyStudioProvider$LoadData;", "", "title", "", "id", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V", "getTitle", "()Ljava/lang/String;", "getId", "component1", "component2", "copy", "equals", "", "other", "hashCode", "", "toString", "CNC Verse Mobile_debug"}, k = 1, mv = {2, 3, 0}, xi = 48)
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
