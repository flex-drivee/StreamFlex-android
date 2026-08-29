package com.horis.cncverse;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.ExtensionsKt;
import com.lagradost.api.Log;
import com.lagradost.cloudstream3.APIHolder;
import com.lagradost.nicehttp.NiceResponse;
import com.lagradost.nicehttp.Requests;
import com.lagradost.nicehttp.ResponseParser;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import kotlin.Metadata;
import kotlin.Pair;
import kotlin.ResultKt;
import kotlin.TuplesKt;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.SpillingKt;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Reflection;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.reflect.KClass;
import kotlin.text.Charsets;
import kotlin.text.StringsKt;
import kotlinx.coroutines.DelayKt;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
/* compiled from: Utils.kt */
@Metadata(d1 = {"\u00008\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010$\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u000b\u001a\"\u0010\b\u001a\u0002H\t\"\n\b\u0000\u0010\t\u0018\u0001*\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0086\b¢\u0006\u0002\u0010\r\u001a$\u0010\u000e\u001a\u0004\u0018\u0001H\t\"\n\b\u0000\u0010\t\u0018\u0001*\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0086\b¢\u0006\u0002\u0010\r\u001a\u000e\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\f\u001a\u0016\u0010\u0012\u001a\u00020\f2\u0006\u0010\u0013\u001a\u00020\fH\u0086@¢\u0006\u0002\u0010\u0014\u001a\u000e\u0010\u001d\u001a\u00020\f2\u0006\u0010\u001e\u001a\u00020\f\u001a\u000e\u0010 \u001a\u00020\fH\u0086@¢\u0006\u0002\u0010!\u001a0\u0010\"\u001a\u000e\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\f0\u00162\u0006\u0010#\u001a\u00020\f2\u0014\b\u0002\u0010$\u001a\u000e\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\f0\u0016\"\u0011\u0010\u0000\u001a\u00020\u0001¢\u0006\b\n\u0000\u001a\u0004\b\u0002\u0010\u0003\"\u0011\u0010\u0004\u001a\u00020\u0005¢\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\"\u001d\u0010\u0015\u001a\u000e\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\f0\u0016¢\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018\"\u0017\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\f0\u001a¢\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001c\"\u000e\u0010\u001f\u001a\u00020\fX\u0082\u000e¢\u0006\u0002\n\u0000¨\u0006%"}, d2 = {"JSONParser", "Lcom/lagradost/nicehttp/ResponseParser;", "getJSONParser", "()Lcom/lagradost/nicehttp/ResponseParser;", "app", "Lcom/lagradost/nicehttp/Requests;", "getApp", "()Lcom/lagradost/nicehttp/Requests;", "parseJson", "T", "", "text", "", "(Ljava/lang/String;)Ljava/lang/Object;", "tryParseJson", "convertRuntimeToMinutes", "", "runtime", "bypass", "mainUrl", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "newTvBaseHeaders", "", "getNewTvBaseHeaders", "()Ljava/util/Map;", "newTvDomains", "", "getNewTvDomains", "()Ljava/util/List;", "decodeBase64", "value", "resolvedApiUrl", "resolveApiUrl", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "buildNewTvHeaders", "ott", "extra", "CNC Verse Mobile_debug"}, k = 2, mv = {2, 3, 0}, xi = 48)
@SourceDebugExtension({"SMAP\nUtils.kt\nKotlin\n*S Kotlin\n*F\n+ 1 Utils.kt\ncom/horis/cncverse/UtilsKt\n+ 2 NiceResponse.kt\ncom/lagradost/nicehttp/NiceResponse\n+ 3 _Maps.kt\nkotlin/collections/MapsKt___MapsKt\n*L\n1#1,397:1\n62#2:398\n221#3,2:399\n*S KotlinDebug\n*F\n+ 1 Utils.kt\ncom/horis/cncverse/UtilsKt\n*L\n367#1:398\n383#1:399,2\n*E\n"})
/* loaded from: /sdcard/AndroidIDEProjects/Cloudstream reference/cncverse/CNC Verse Mobile/resources/classes.dex */
public final class UtilsKt {
    @NotNull
    private static final ResponseParser JSONParser = new ResponseParser() { // from class: com.horis.cncverse.UtilsKt$JSONParser$1
        private final ObjectMapper mapper = ExtensionsKt.jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);

        public final ObjectMapper getMapper() {
            return this.mapper;
        }

        public <T> T parse(String text, KClass<T> kClass) {
            return (T) this.mapper.readValue(text, JvmClassMappingKt.getJavaClass(kClass));
        }

        public <T> T parseSafe(String text, KClass<T> kClass) {
            try {
                return (T) this.mapper.readValue(text, JvmClassMappingKt.getJavaClass(kClass));
            } catch (Exception e) {
                return null;
            }
        }

        public String writeValueAsString(Object obj) {
            return this.mapper.writeValueAsString(obj);
        }
    };
    @NotNull
    private static final Requests app;
    @NotNull
    private static final Map<String, String> newTvBaseHeaders;
    @NotNull
    private static final List<String> newTvDomains;
    @NotNull
    private static String resolvedApiUrl;

    static {
        Requests $this$app_u24lambda_u240 = new Requests((OkHttpClient) null, (Map) null, (String) null, (Map) null, (Map) null, 0, (TimeUnit) null, 0L, JSONParser, 255, (DefaultConstructorMarker) null);
        $this$app_u24lambda_u240.setDefaultHeaders(MapsKt.mapOf(TuplesKt.to("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36")));
        app = $this$app_u24lambda_u240;
        newTvBaseHeaders = MapsKt.mapOf(new Pair[]{TuplesKt.to("Cache-Control", "no-cache, no-store, must-revalidate"), TuplesKt.to("Pragma", "no-cache"), TuplesKt.to("Expires", "0"), TuplesKt.to("X-Requested-With", "NetmirrorNewTV v1.0"), TuplesKt.to("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0 /OS.GatuNewTV v1.0"), TuplesKt.to("Accept", "application/json, text/plain, */*")});
        newTvDomains = CollectionsKt.listOf(new String[]{"aHR0cHM6Ly9tb2JpbGVkZXRlY3RzLmNvbQ==", "aHR0cHM6Ly9tb2JpbGVkZXRlY3QuYXBw", "aHR0cHM6Ly9tb2JpZGV0ZWN0LmFydA==", "aHR0cHM6Ly9tb2JpZGV0ZWN0LmNj", "aHR0cHM6Ly9tb2JpZGV0ZWN0LmNsaWNr", "aHR0cHM6Ly9tb2JpZGV0ZWN0Lmluaw==", "aHR0cHM6Ly9tb2JpZGV0ZWN0LmxpdmU=", "aHR0cHM6Ly9tb2JpZGV0ZWN0LnBybw==", "aHR0cHM6Ly9tb2JpZGV0ZWN0LnNob3A=", "aHR0cHM6Ly9tb2JpZGV0ZWN0LnNpdGU=", "aHR0cHM6Ly9tb2JpZGV0ZWN0LnNwYWNl", "aHR0cHM6Ly9tb2JpZGV0ZWN0LnN0b3Jl", "aHR0cHM6Ly9tb2JpZGV0ZWN0LnZpcA==", "aHR0cHM6Ly9tb2JpZGV0ZWN0Lndpa2k=", "aHR0cHM6Ly9tb2JpZGV0ZWN0Lnh5eg==", "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5hcnQ=", "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5jYw==", "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5pbmZv", "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5pbms=", "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5saXZl", "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5wcm8=", "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5zdG9yZQ==", "aHR0cHM6Ly9tb2JpZGV0ZWN0cy50b3A=", "aHR0cHM6Ly9tb2JpZGV0ZWN0cy54eXo="});
        resolvedApiUrl = "";
    }

    @NotNull
    public static final ResponseParser getJSONParser() {
        return JSONParser;
    }

    @NotNull
    public static final Requests getApp() {
        return app;
    }

    public static final /* synthetic */ <T> T parseJson(String text) {
        ResponseParser jSONParser = getJSONParser();
        Intrinsics.reifiedOperationMarker(4, "T");
        return (T) jSONParser.parse(text, Reflection.getOrCreateKotlinClass(Object.class));
    }

    public static final /* synthetic */ <T> T tryParseJson(String text) {
        try {
            ResponseParser jSONParser = getJSONParser();
            Intrinsics.reifiedOperationMarker(4, "T");
            return (T) jSONParser.parseSafe(text, Reflection.getOrCreateKotlinClass(Object.class));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static final int convertRuntimeToMinutes(@NotNull String runtime) {
        int minutes;
        int totalMinutes = 0;
        List<String> parts = StringsKt.split$default(runtime, new String[]{" "}, false, 0, 6, (Object) null);
        for (String part : parts) {
            if (StringsKt.endsWith$default(part, "h", false, 2, (Object) null)) {
                Integer intOrNull = StringsKt.toIntOrNull(StringsKt.trim(StringsKt.removeSuffix(part, "h")).toString());
                minutes = intOrNull != null ? intOrNull.intValue() : 0;
                totalMinutes += minutes * 60;
            } else if (StringsKt.endsWith$default(part, "m", false, 2, (Object) null)) {
                Integer intOrNull2 = StringsKt.toIntOrNull(StringsKt.trim(StringsKt.removeSuffix(part, "m")).toString());
                minutes = intOrNull2 != null ? intOrNull2.intValue() : 0;
                totalMinutes += minutes;
            }
        }
        return totalMinutes;
    }

    /* JADX WARN: Removed duplicated region for block: B:10:0x0030  */
    /* JADX WARN: Removed duplicated region for block: B:12:0x003a  */
    /* JADX WARN: Removed duplicated region for block: B:17:0x0076  */
    /* JADX WARN: Removed duplicated region for block: B:22:0x00bc  */
    /* JADX WARN: Removed duplicated region for block: B:27:0x00f6  */
    /* JADX WARN: Removed duplicated region for block: B:28:0x011e  */
    /* JADX WARN: Removed duplicated region for block: B:49:0x027d A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:50:0x027e  */
    /* JADX WARN: Removed duplicated region for block: B:62:0x02f1 A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:63:0x02f2  */
    /* JADX WARN: Removed duplicated region for block: B:70:0x037a A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:71:0x037b  */
    /* JADX WARN: Removed duplicated region for block: B:74:0x03ad A[Catch: Exception -> 0x0429, TRY_LEAVE, TryCatch #4 {Exception -> 0x0429, blocks: (B:72:0x0386, B:74:0x03ad), top: B:117:0x0386 }] */
    /* JADX WARN: Removed duplicated region for block: B:90:0x0417  */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:71:0x037b -> B:117:0x0386). Please submit an issue!!! */
    @Nullable
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static final Object bypass(@NotNull String mainUrl, @NotNull Continuation<? super String> continuation) {
        UtilsKt$bypass$1 utilsKt$bypass$1;
        String savedCookie;
        Object obj;
        String str;
        Object obj2;
        Object obj3;
        Object $result;
        String mainUrl2;
        char c;
        UtilsKt$bypass$1 utilsKt$bypass$12;
        String mainUrl3;
        Object obj4;
        long savedTimestamp;
        String addhash;
        UtilsKt$bypass$1 utilsKt$bypass$13;
        String addhash2;
        String verifyCheck;
        String savedCookie2;
        long savedTimestamp2;
        Requests requests;
        String str2;
        long savedTimestamp3;
        String mainUrl4;
        long savedTimestamp4;
        String addhash3;
        Object obj5;
        FormBody requestBody;
        int count;
        UtilsKt$bypass$1 utilsKt$bypass$14;
        Continuation $completion;
        char c2;
        NiceResponse verifyResponse;
        String verifyCheck2;
        Object obj6;
        Continuation $completion2;
        Object obj7;
        UtilsKt$bypass$1 utilsKt$bypass$15;
        Object post$default;
        Object obj8;
        Object $result2;
        String savedCookie3;
        int count2;
        String mainUrl5;
        Continuation $completion3;
        int count3;
        Continuation $completion4;
        Object obj9;
        if (continuation instanceof UtilsKt$bypass$1) {
            utilsKt$bypass$1 = (UtilsKt$bypass$1) continuation;
            if ((utilsKt$bypass$1.label & Integer.MIN_VALUE) != 0) {
                utilsKt$bypass$1.label -= Integer.MIN_VALUE;
                Object $result3 = utilsKt$bypass$1.result;
                Object coroutine_suspended = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                String str3 = "Mozilla/5.0 (Linux; Android 12; RMX2117 Build/SP1A.210812.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.55 Mobile Safari/537.36 /OS.Gatu v3.0";
                switch (utilsKt$bypass$1.label) {
                    case 0:
                        ResultKt.throwOnFailure($result3);
                        Pair<String, Long> cookie = NetflixMirrorStorage.INSTANCE.getCookie();
                        String savedCookie4 = (String) cookie.component1();
                        long savedTimestamp5 = ((Number) cookie.component2()).longValue();
                        String str4 = savedCookie4;
                        if (!(str4 == null || str4.length() == 0) && System.currentTimeMillis() - savedTimestamp5 < 54000000) {
                            Log.INSTANCE.d("NF", "savedCookie: " + savedCookie4);
                            return savedCookie4;
                        }
                        Map mapOf = MapsKt.mapOf(new Pair[]{TuplesKt.to("User-Agent", "Mozilla/5.0 (Linux; Android 12; RMX2117 Build/SP1A.210812.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.55 Mobile Safari/537.36 /OS.Gatu v3.0"), TuplesKt.to("X-Requested-With", "app.netmirror.netmirrornew")});
                        utilsKt$bypass$1.L$0 = mainUrl;
                        utilsKt$bypass$1.L$1 = SpillingKt.nullOutSpilledVariable(savedCookie4);
                        utilsKt$bypass$1.J$0 = savedTimestamp5;
                        utilsKt$bypass$1.label = 1;
                        savedCookie = savedCookie4;
                        obj = "X-Requested-With";
                        str = "Mozilla/5.0 (Linux; Android 12; RMX2117 Build/SP1A.210812.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.55 Mobile Safari/537.36 /OS.Gatu v3.0";
                        UtilsKt$bypass$1 utilsKt$bypass$16 = utilsKt$bypass$1;
                        obj2 = "User-Agent";
                        obj3 = coroutine_suspended;
                        $result = $result3;
                        mainUrl2 = "NF";
                        c = 0;
                        Object obj10 = Requests.get$default(app, mainUrl + "/mobile/home?app=1", mapOf, (String) null, (Map) null, (Map) null, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, utilsKt$bypass$16, 4092, (Object) null);
                        utilsKt$bypass$12 = utilsKt$bypass$16;
                        if (obj10 == obj3) {
                            return obj3;
                        }
                        mainUrl3 = mainUrl;
                        obj4 = obj10;
                        savedTimestamp = savedTimestamp5;
                        addhash = ((NiceResponse) obj4).getDocument().select("body").attr("data-addhash");
                        Log.INSTANCE.d(mainUrl2, "addhash: " + addhash);
                        try {
                            requests = app;
                            str2 = "https://userver.net52.cc/?jjoii=" + addhash + "&a=y&t=" + APIHolder.INSTANCE.getUnixTime();
                            utilsKt$bypass$12.L$0 = mainUrl3;
                            utilsKt$bypass$12.L$1 = SpillingKt.nullOutSpilledVariable(savedCookie);
                            utilsKt$bypass$12.L$2 = addhash;
                            utilsKt$bypass$12.J$0 = savedTimestamp;
                            utilsKt$bypass$12.label = 2;
                            savedTimestamp3 = savedTimestamp;
                            utilsKt$bypass$13 = utilsKt$bypass$12;
                            addhash2 = addhash;
                            mainUrl4 = mainUrl3;
                        } catch (Exception e) {
                            utilsKt$bypass$13 = utilsKt$bypass$12;
                            long j = savedTimestamp;
                            addhash2 = addhash;
                            verifyCheck = mainUrl3;
                            savedCookie2 = savedCookie;
                            savedTimestamp2 = j;
                        }
                        try {
                        } catch (Exception e2) {
                            verifyCheck = mainUrl4;
                            savedCookie2 = savedCookie;
                            savedTimestamp2 = savedTimestamp3;
                            savedTimestamp4 = savedTimestamp2;
                            addhash3 = addhash2;
                            String verifyCheck3 = null;
                            try {
                                obj5 = obj3;
                                requestBody = new FormBody.Builder((Charset) null, 1, (DefaultConstructorMarker) null).addEncoded("verify", String.valueOf(addhash3)).build();
                                count = 0;
                                NiceResponse verifyResponse2 = null;
                                utilsKt$bypass$14 = utilsKt$bypass$13;
                                $completion = continuation;
                                try {
                                    utilsKt$bypass$14.L$0 = verifyCheck;
                                    utilsKt$bypass$14.L$1 = SpillingKt.nullOutSpilledVariable(savedCookie2);
                                    utilsKt$bypass$14.L$2 = SpillingKt.nullOutSpilledVariable(addhash3);
                                    utilsKt$bypass$14.L$3 = SpillingKt.nullOutSpilledVariable(verifyCheck3);
                                    utilsKt$bypass$14.L$4 = SpillingKt.nullOutSpilledVariable(verifyResponse2);
                                    utilsKt$bypass$14.L$5 = requestBody;
                                    utilsKt$bypass$14.J$0 = savedTimestamp4;
                                    utilsKt$bypass$14.I$0 = count;
                                    utilsKt$bypass$14.label = 3;
                                    FormBody requestBody2 = requestBody;
                                    c2 = 1;
                                    if (DelayKt.delay(10000L, utilsKt$bypass$14) == obj5) {
                                    }
                                } catch (Exception e3) {
                                    e = e3;
                                    NetflixMirrorStorage.INSTANCE.clearCookie();
                                    throw e;
                                }
                            } catch (Exception e4) {
                                e = e4;
                                NetflixMirrorStorage.INSTANCE.clearCookie();
                                throw e;
                            }
                        }
                        if (Requests.get$default(requests, str2, (Map) null, (String) null, (Map) null, (Map) null, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, utilsKt$bypass$13, 4094, (Object) null) != obj3) {
                            return obj3;
                        }
                        verifyCheck = mainUrl4;
                        savedCookie2 = savedCookie;
                        savedTimestamp2 = savedTimestamp3;
                        savedTimestamp4 = savedTimestamp2;
                        addhash3 = addhash2;
                        String verifyCheck32 = null;
                        obj5 = obj3;
                        requestBody = new FormBody.Builder((Charset) null, 1, (DefaultConstructorMarker) null).addEncoded("verify", String.valueOf(addhash3)).build();
                        count = 0;
                        NiceResponse verifyResponse22 = null;
                        utilsKt$bypass$14 = utilsKt$bypass$13;
                        $completion = continuation;
                        utilsKt$bypass$14.L$0 = verifyCheck;
                        utilsKt$bypass$14.L$1 = SpillingKt.nullOutSpilledVariable(savedCookie2);
                        utilsKt$bypass$14.L$2 = SpillingKt.nullOutSpilledVariable(addhash3);
                        utilsKt$bypass$14.L$3 = SpillingKt.nullOutSpilledVariable(verifyCheck32);
                        utilsKt$bypass$14.L$4 = SpillingKt.nullOutSpilledVariable(verifyResponse22);
                        utilsKt$bypass$14.L$5 = requestBody;
                        utilsKt$bypass$14.J$0 = savedTimestamp4;
                        utilsKt$bypass$14.I$0 = count;
                        utilsKt$bypass$14.label = 3;
                        FormBody requestBody22 = requestBody;
                        c2 = 1;
                        if (DelayKt.delay(10000L, utilsKt$bypass$14) == obj5) {
                            return obj5;
                        }
                        verifyCheck2 = verifyCheck32;
                        verifyResponse = verifyResponse22;
                        coroutine_suspended = obj5;
                        requestBody = requestBody22;
                        try {
                            try {
                                try {
                                    Requests requests2 = app;
                                    String str5 = verifyCheck + "/mobile/verify2.php";
                                    Pair[] pairArr = new Pair[2];
                                    str3 = str;
                                    obj6 = obj2;
                                    pairArr[c] = TuplesKt.to(obj6, str3);
                                    NiceResponse verifyResponse3 = verifyResponse;
                                    pairArr[c2] = TuplesKt.to(obj7, "XMLHttpRequest");
                                    Map mapOf2 = MapsKt.mapOf(pairArr);
                                    RequestBody requestBody3 = requestBody;
                                    utilsKt$bypass$14.L$0 = verifyCheck;
                                    utilsKt$bypass$14.L$1 = SpillingKt.nullOutSpilledVariable(savedCookie2);
                                    utilsKt$bypass$14.L$2 = SpillingKt.nullOutSpilledVariable(addhash3);
                                    utilsKt$bypass$14.L$3 = SpillingKt.nullOutSpilledVariable(verifyCheck2);
                                    utilsKt$bypass$14.L$4 = SpillingKt.nullOutSpilledVariable(verifyResponse3);
                                    utilsKt$bypass$14.L$5 = requestBody;
                                    utilsKt$bypass$14.J$0 = savedTimestamp4;
                                    utilsKt$bypass$14.I$0 = count;
                                    utilsKt$bypass$14.label = 4;
                                    post$default = Requests.post$default(requests2, str5, mapOf2, (String) null, (Map) null, (Map) null, (Map) null, (List) null, (Object) null, requestBody3, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, utilsKt$bypass$15, 65276, (Object) null);
                                    if (post$default != coroutine_suspended) {
                                        return coroutine_suspended;
                                    }
                                    obj8 = post$default;
                                    $result2 = $result;
                                    savedCookie3 = savedCookie2;
                                    count2 = count;
                                    mainUrl5 = verifyCheck;
                                    $completion3 = $completion2;
                                    try {
                                        NiceResponse verifyResponse4 = (NiceResponse) obj8;
                                        String verifyCheck4 = verifyResponse4.getText();
                                        obj = obj7;
                                        Log.INSTANCE.d(mainUrl2, "verifyCheck: " + verifyCheck4);
                                        count3 = count2 + 1;
                                        if (count3 <= 7) {
                                            throw new Exception("Failed to verify cookie");
                                        }
                                        try {
                                            if (StringsKt.contains$default(verifyCheck4, "\"statusup\":\"All Done\"", false, 2, (Object) null)) {
                                                String newCookie = (String) verifyResponse4.getCookies().get("t_hash_t");
                                                if (newCookie == null) {
                                                    newCookie = "";
                                                }
                                                if (newCookie.length() > 0) {
                                                    NetflixMirrorStorage.INSTANCE.saveCookie(newCookie);
                                                }
                                                Log.INSTANCE.d(mainUrl2, "newCookie: " + newCookie);
                                                return newCookie;
                                            }
                                            String newCookie2 = mainUrl5;
                                            count = count3;
                                            verifyCheck32 = verifyCheck4;
                                            verifyCheck = newCookie2;
                                            verifyResponse22 = verifyResponse4;
                                            str = str3;
                                            obj2 = obj6;
                                            obj5 = obj9;
                                            savedCookie2 = savedCookie3;
                                            utilsKt$bypass$14 = utilsKt$bypass$15;
                                            c = 0;
                                            $result = $result2;
                                            $completion = $completion4;
                                            utilsKt$bypass$14.L$0 = verifyCheck;
                                            utilsKt$bypass$14.L$1 = SpillingKt.nullOutSpilledVariable(savedCookie2);
                                            utilsKt$bypass$14.L$2 = SpillingKt.nullOutSpilledVariable(addhash3);
                                            utilsKt$bypass$14.L$3 = SpillingKt.nullOutSpilledVariable(verifyCheck32);
                                            utilsKt$bypass$14.L$4 = SpillingKt.nullOutSpilledVariable(verifyResponse22);
                                            utilsKt$bypass$14.L$5 = requestBody;
                                            utilsKt$bypass$14.J$0 = savedTimestamp4;
                                            utilsKt$bypass$14.I$0 = count;
                                            utilsKt$bypass$14.label = 3;
                                            FormBody requestBody222 = requestBody;
                                            c2 = 1;
                                            if (DelayKt.delay(10000L, utilsKt$bypass$14) == obj5) {
                                            }
                                        } catch (Exception e5) {
                                            e = e5;
                                            NetflixMirrorStorage.INSTANCE.clearCookie();
                                            throw e;
                                        }
                                        $completion4 = $completion3;
                                        obj9 = coroutine_suspended;
                                    } catch (Exception e6) {
                                        e = e6;
                                    }
                                } catch (Exception e7) {
                                    e = e7;
                                }
                                $completion2 = $completion;
                                obj7 = obj;
                            } catch (Exception e8) {
                                e = e8;
                                NetflixMirrorStorage.INSTANCE.clearCookie();
                                throw e;
                            }
                            utilsKt$bypass$15 = utilsKt$bypass$14;
                        } catch (Exception e9) {
                            e = e9;
                        }
                        break;
                    case 1:
                        long savedTimestamp6 = utilsKt$bypass$1.J$0;
                        String mainUrl6 = (String) utilsKt$bypass$1.L$0;
                        ResultKt.throwOnFailure($result3);
                        utilsKt$bypass$12 = utilsKt$bypass$1;
                        obj3 = coroutine_suspended;
                        savedCookie = (String) utilsKt$bypass$1.L$1;
                        $result = $result3;
                        obj = "X-Requested-With";
                        str = "Mozilla/5.0 (Linux; Android 12; RMX2117 Build/SP1A.210812.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.55 Mobile Safari/537.36 /OS.Gatu v3.0";
                        obj2 = "User-Agent";
                        savedTimestamp = savedTimestamp6;
                        mainUrl2 = "NF";
                        c = 0;
                        mainUrl3 = mainUrl6;
                        obj4 = $result;
                        addhash = ((NiceResponse) obj4).getDocument().select("body").attr("data-addhash");
                        Log.INSTANCE.d(mainUrl2, "addhash: " + addhash);
                        requests = app;
                        str2 = "https://userver.net52.cc/?jjoii=" + addhash + "&a=y&t=" + APIHolder.INSTANCE.getUnixTime();
                        utilsKt$bypass$12.L$0 = mainUrl3;
                        utilsKt$bypass$12.L$1 = SpillingKt.nullOutSpilledVariable(savedCookie);
                        utilsKt$bypass$12.L$2 = addhash;
                        utilsKt$bypass$12.J$0 = savedTimestamp;
                        utilsKt$bypass$12.label = 2;
                        savedTimestamp3 = savedTimestamp;
                        utilsKt$bypass$13 = utilsKt$bypass$12;
                        addhash2 = addhash;
                        mainUrl4 = mainUrl3;
                        if (Requests.get$default(requests, str2, (Map) null, (String) null, (Map) null, (Map) null, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, utilsKt$bypass$13, 4094, (Object) null) != obj3) {
                        }
                        break;
                    case 2:
                        savedTimestamp2 = utilsKt$bypass$1.J$0;
                        String addhash4 = (String) utilsKt$bypass$1.L$2;
                        savedCookie2 = (String) utilsKt$bypass$1.L$1;
                        verifyCheck = (String) utilsKt$bypass$1.L$0;
                        try {
                            ResultKt.throwOnFailure($result3);
                            addhash2 = addhash4;
                            utilsKt$bypass$13 = utilsKt$bypass$1;
                            $result = $result3;
                            obj3 = coroutine_suspended;
                            obj = "X-Requested-With";
                            str = "Mozilla/5.0 (Linux; Android 12; RMX2117 Build/SP1A.210812.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.55 Mobile Safari/537.36 /OS.Gatu v3.0";
                            obj2 = "User-Agent";
                            mainUrl2 = "NF";
                            c = 0;
                            savedTimestamp4 = savedTimestamp2;
                            addhash3 = addhash2;
                        } catch (Exception e10) {
                            addhash2 = addhash4;
                            utilsKt$bypass$13 = utilsKt$bypass$1;
                            $result = $result3;
                            obj3 = coroutine_suspended;
                            obj = "X-Requested-With";
                            str = "Mozilla/5.0 (Linux; Android 12; RMX2117 Build/SP1A.210812.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.55 Mobile Safari/537.36 /OS.Gatu v3.0";
                            obj2 = "User-Agent";
                            mainUrl2 = "NF";
                            c = 0;
                            savedTimestamp4 = savedTimestamp2;
                            addhash3 = addhash2;
                            String verifyCheck322 = null;
                            obj5 = obj3;
                            requestBody = new FormBody.Builder((Charset) null, 1, (DefaultConstructorMarker) null).addEncoded("verify", String.valueOf(addhash3)).build();
                            count = 0;
                            NiceResponse verifyResponse222 = null;
                            utilsKt$bypass$14 = utilsKt$bypass$13;
                            $completion = continuation;
                            utilsKt$bypass$14.L$0 = verifyCheck;
                            utilsKt$bypass$14.L$1 = SpillingKt.nullOutSpilledVariable(savedCookie2);
                            utilsKt$bypass$14.L$2 = SpillingKt.nullOutSpilledVariable(addhash3);
                            utilsKt$bypass$14.L$3 = SpillingKt.nullOutSpilledVariable(verifyCheck322);
                            utilsKt$bypass$14.L$4 = SpillingKt.nullOutSpilledVariable(verifyResponse222);
                            utilsKt$bypass$14.L$5 = requestBody;
                            utilsKt$bypass$14.J$0 = savedTimestamp4;
                            utilsKt$bypass$14.I$0 = count;
                            utilsKt$bypass$14.label = 3;
                            FormBody requestBody2222 = requestBody;
                            c2 = 1;
                            if (DelayKt.delay(10000L, utilsKt$bypass$14) == obj5) {
                            }
                        }
                        String verifyCheck3222 = null;
                        obj5 = obj3;
                        requestBody = new FormBody.Builder((Charset) null, 1, (DefaultConstructorMarker) null).addEncoded("verify", String.valueOf(addhash3)).build();
                        count = 0;
                        NiceResponse verifyResponse2222 = null;
                        utilsKt$bypass$14 = utilsKt$bypass$13;
                        $completion = continuation;
                        utilsKt$bypass$14.L$0 = verifyCheck;
                        utilsKt$bypass$14.L$1 = SpillingKt.nullOutSpilledVariable(savedCookie2);
                        utilsKt$bypass$14.L$2 = SpillingKt.nullOutSpilledVariable(addhash3);
                        utilsKt$bypass$14.L$3 = SpillingKt.nullOutSpilledVariable(verifyCheck3222);
                        utilsKt$bypass$14.L$4 = SpillingKt.nullOutSpilledVariable(verifyResponse2222);
                        utilsKt$bypass$14.L$5 = requestBody;
                        utilsKt$bypass$14.J$0 = savedTimestamp4;
                        utilsKt$bypass$14.I$0 = count;
                        utilsKt$bypass$14.label = 3;
                        FormBody requestBody22222 = requestBody;
                        c2 = 1;
                        if (DelayKt.delay(10000L, utilsKt$bypass$14) == obj5) {
                        }
                        break;
                    case 3:
                        c2 = 1;
                        int count4 = utilsKt$bypass$1.I$0;
                        savedTimestamp4 = utilsKt$bypass$1.J$0;
                        RequestBody requestBody4 = (FormBody) utilsKt$bypass$1.L$5;
                        NiceResponse verifyResponse5 = (NiceResponse) utilsKt$bypass$1.L$4;
                        String verifyCheck5 = (String) utilsKt$bypass$1.L$3;
                        String addhash5 = (String) utilsKt$bypass$1.L$2;
                        String savedCookie5 = (String) utilsKt$bypass$1.L$1;
                        String mainUrl7 = (String) utilsKt$bypass$1.L$0;
                        try {
                            ResultKt.throwOnFailure($result3);
                            obj = "X-Requested-With";
                            str = "Mozilla/5.0 (Linux; Android 12; RMX2117 Build/SP1A.210812.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.55 Mobile Safari/537.36 /OS.Gatu v3.0";
                            obj2 = "User-Agent";
                            verifyResponse = verifyResponse5;
                            count = count4;
                            c = 0;
                            $result = $result3;
                            requestBody = requestBody4;
                            savedCookie2 = savedCookie5;
                            utilsKt$bypass$14 = utilsKt$bypass$1;
                            verifyCheck2 = verifyCheck5;
                            addhash3 = addhash5;
                            $completion = continuation;
                            verifyCheck = mainUrl7;
                            mainUrl2 = "NF";
                            Requests requests22 = app;
                            String str52 = verifyCheck + "/mobile/verify2.php";
                            Pair[] pairArr2 = new Pair[2];
                            str3 = str;
                            obj6 = obj2;
                            pairArr2[c] = TuplesKt.to(obj6, str3);
                            NiceResponse verifyResponse32 = verifyResponse;
                            $completion2 = $completion;
                            obj7 = obj;
                            pairArr2[c2] = TuplesKt.to(obj7, "XMLHttpRequest");
                            Map mapOf22 = MapsKt.mapOf(pairArr2);
                            RequestBody requestBody32 = requestBody;
                            utilsKt$bypass$14.L$0 = verifyCheck;
                            utilsKt$bypass$14.L$1 = SpillingKt.nullOutSpilledVariable(savedCookie2);
                            utilsKt$bypass$14.L$2 = SpillingKt.nullOutSpilledVariable(addhash3);
                            utilsKt$bypass$14.L$3 = SpillingKt.nullOutSpilledVariable(verifyCheck2);
                            utilsKt$bypass$14.L$4 = SpillingKt.nullOutSpilledVariable(verifyResponse32);
                            utilsKt$bypass$14.L$5 = requestBody;
                            utilsKt$bypass$14.J$0 = savedTimestamp4;
                            utilsKt$bypass$14.I$0 = count;
                            utilsKt$bypass$14.label = 4;
                            utilsKt$bypass$15 = utilsKt$bypass$14;
                            post$default = Requests.post$default(requests22, str52, mapOf22, (String) null, (Map) null, (Map) null, (Map) null, (List) null, (Object) null, requestBody32, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, utilsKt$bypass$15, 65276, (Object) null);
                            if (post$default != coroutine_suspended) {
                            }
                        } catch (Exception e11) {
                            e = e11;
                            NetflixMirrorStorage.INSTANCE.clearCookie();
                            throw e;
                        }
                        break;
                    case 4:
                        count2 = utilsKt$bypass$1.I$0;
                        savedTimestamp4 = utilsKt$bypass$1.J$0;
                        requestBody = (FormBody) utilsKt$bypass$1.L$5;
                        NiceResponse niceResponse = (NiceResponse) utilsKt$bypass$1.L$4;
                        String str6 = (String) utilsKt$bypass$1.L$3;
                        addhash3 = (String) utilsKt$bypass$1.L$2;
                        savedCookie3 = (String) utilsKt$bypass$1.L$1;
                        String mainUrl8 = (String) utilsKt$bypass$1.L$0;
                        try {
                            ResultKt.throwOnFailure($result3);
                            utilsKt$bypass$15 = utilsKt$bypass$1;
                            obj8 = $result3;
                            obj7 = "X-Requested-With";
                            mainUrl5 = mainUrl8;
                            mainUrl2 = "NF";
                            obj6 = "User-Agent";
                            $result2 = obj8;
                            $completion3 = continuation;
                            NiceResponse verifyResponse42 = (NiceResponse) obj8;
                            String verifyCheck42 = verifyResponse42.getText();
                            obj = obj7;
                            Log.INSTANCE.d(mainUrl2, "verifyCheck: " + verifyCheck42);
                            count3 = count2 + 1;
                            if (count3 <= 7) {
                            }
                        } catch (Exception e12) {
                            e = e12;
                            NetflixMirrorStorage.INSTANCE.clearCookie();
                            throw e;
                        }
                        break;
                    default:
                        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }
            }
        }
        utilsKt$bypass$1 = new UtilsKt$bypass$1(continuation);
        Object $result32 = utilsKt$bypass$1.result;
        Object coroutine_suspended2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
        String str32 = "Mozilla/5.0 (Linux; Android 12; RMX2117 Build/SP1A.210812.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.55 Mobile Safari/537.36 /OS.Gatu v3.0";
        switch (utilsKt$bypass$1.label) {
        }
    }

    @NotNull
    public static final Map<String, String> getNewTvBaseHeaders() {
        return newTvBaseHeaders;
    }

    @NotNull
    public static final List<String> getNewTvDomains() {
        return newTvDomains;
    }

    @NotNull
    public static final String decodeBase64(@NotNull String value) {
        return new String(Base64.getDecoder().decode(value), Charsets.UTF_8);
    }

    /* JADX WARN: Can't wrap try/catch for region: R(10:35|(1:36)|37|38|39|40|41|42|43|(1:45)(6:46|15|16|(2:21|(5:23|24|25|26|27)(3:58|33|(2:56|57)(0)))|59|(0)(0))) */
    /* JADX WARN: Can't wrap try/catch for region: R(10:35|36|37|38|39|40|41|42|43|(1:45)(6:46|15|16|(2:21|(5:23|24|25|26|27)(3:58|33|(2:56|57)(0)))|59|(0)(0))) */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x0135, code lost:
        r10 = r2;
        r2 = r22;
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x013e, code lost:
        r10 = r8;
        r2 = r2;
     */
    /* JADX WARN: Removed duplicated region for block: B:10:0x002b  */
    /* JADX WARN: Removed duplicated region for block: B:12:0x0033  */
    /* JADX WARN: Removed duplicated region for block: B:17:0x004e  */
    /* JADX WARN: Removed duplicated region for block: B:24:0x006b  */
    /* JADX WARN: Removed duplicated region for block: B:41:0x0112 A[Catch: Exception -> 0x012e, TRY_LEAVE, TryCatch #0 {Exception -> 0x012e, blocks: (B:33:0x00e1, B:35:0x0106, B:41:0x0112, B:43:0x011a), top: B:59:0x00e1 }] */
    /* JADX WARN: Removed duplicated region for block: B:47:0x0129  */
    /* JADX WARN: Removed duplicated region for block: B:57:0x015b  */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:32:0x00d9 -> B:59:0x00e1). Please submit an issue!!! */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:51:0x0135 -> B:56:0x0158). Please submit an issue!!! */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:53:0x013e -> B:56:0x0158). Please submit an issue!!! */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:55:0x014c -> B:56:0x0158). Please submit an issue!!! */
    @Nullable
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static final Object resolveApiUrl(@NotNull Continuation<? super String> continuation) {
        UtilsKt$resolveApiUrl$1 utilsKt$resolveApiUrl$1;
        UtilsKt$resolveApiUrl$1 utilsKt$resolveApiUrl$12;
        Iterator<String> it;
        Iterator<String> it2;
        String base;
        Object obj;
        Object $result;
        String str;
        boolean z;
        if (continuation instanceof UtilsKt$resolveApiUrl$1) {
            utilsKt$resolveApiUrl$1 = (UtilsKt$resolveApiUrl$1) continuation;
            if ((utilsKt$resolveApiUrl$1.label & Integer.MIN_VALUE) != 0) {
                utilsKt$resolveApiUrl$1.label -= Integer.MIN_VALUE;
                utilsKt$resolveApiUrl$12 = utilsKt$resolveApiUrl$1;
                Object $result2 = utilsKt$resolveApiUrl$12.result;
                Object $result3 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (utilsKt$resolveApiUrl$12.label) {
                    case 0:
                        ResultKt.throwOnFailure($result2);
                        if (!StringsKt.isBlank(resolvedApiUrl)) {
                            return resolvedApiUrl;
                        }
                        it = newTvDomains.iterator();
                        if (it.hasNext()) {
                            String encoded = it.next();
                            String base2 = StringsKt.trimEnd(decodeBase64(encoded), new char[]{'/'});
                            try {
                            } catch (Exception e) {
                                it2 = it;
                                utilsKt$resolveApiUrl$12 = utilsKt$resolveApiUrl$12;
                            }
                            Requests requests = app;
                            String str2 = base2 + "/checknewtv.php";
                            Map<String, String> map = newTvBaseHeaders;
                            utilsKt$resolveApiUrl$12.L$0 = it;
                            utilsKt$resolveApiUrl$12.L$1 = SpillingKt.nullOutSpilledVariable(encoded);
                            utilsKt$resolveApiUrl$12.L$2 = SpillingKt.nullOutSpilledVariable(base2);
                            utilsKt$resolveApiUrl$12.label = 1;
                            base = base2;
                            UtilsKt$resolveApiUrl$1 utilsKt$resolveApiUrl$13 = utilsKt$resolveApiUrl$12;
                            Iterator<String> it3 = it;
                            Object obj2 = Requests.get$default(requests, str2, map, (String) null, (Map) null, (Map) null, false, 0, (TimeUnit) null, 0L, (Interceptor) null, false, (ResponseParser) null, utilsKt$resolveApiUrl$13, 4092, (Object) null);
                            if (obj2 == $result3) {
                                return $result3;
                            }
                            it2 = it3;
                            obj = $result3;
                            utilsKt$resolveApiUrl$12 = utilsKt$resolveApiUrl$13;
                            $result = $result2;
                            $result2 = obj2;
                            try {
                            } catch (Exception e2) {
                                $result2 = $result;
                                $result3 = obj;
                            }
                            NiceResponse this_$iv = (NiceResponse) $result2;
                            ResponseParser parser = this_$iv.getParser();
                            Intrinsics.checkNotNull(parser);
                            NewTvTokenResponse response = (NewTvTokenResponse) parser.parse(this_$iv.getText(), Reflection.getOrCreateKotlinClass(NewTvTokenResponse.class));
                            String tokenHash = response.getToken_hash();
                            str = tokenHash;
                            if (str != null && !StringsKt.isBlank(str)) {
                                z = false;
                                if (z) {
                                    try {
                                    } catch (Exception e3) {
                                        $result2 = $result;
                                        $result3 = obj;
                                        it = it2;
                                        if (it.hasNext()) {
                                        }
                                    }
                                    resolvedApiUrl = StringsKt.trimEnd(decodeBase64(tokenHash), new char[]{'/'});
                                    return resolvedApiUrl;
                                }
                                $result2 = $result;
                                $result3 = obj;
                                it = it2;
                                if (it.hasNext()) {
                                    throw new Exception("Failed to resolve NewTV API base URL");
                                }
                            }
                            z = true;
                            if (z) {
                            }
                        }
                        break;
                    case 1:
                        String base3 = (String) utilsKt$resolveApiUrl$12.L$2;
                        String str3 = (String) utilsKt$resolveApiUrl$12.L$1;
                        it2 = (Iterator) utilsKt$resolveApiUrl$12.L$0;
                        try {
                            ResultKt.throwOnFailure($result2);
                            base = base3;
                            obj = $result3;
                            $result = $result2;
                        } catch (Exception e4) {
                            it = it2;
                            if (it.hasNext()) {
                            }
                        }
                        NiceResponse this_$iv2 = (NiceResponse) $result2;
                        ResponseParser parser2 = this_$iv2.getParser();
                        Intrinsics.checkNotNull(parser2);
                        NewTvTokenResponse response2 = (NewTvTokenResponse) parser2.parse(this_$iv2.getText(), Reflection.getOrCreateKotlinClass(NewTvTokenResponse.class));
                        String tokenHash2 = response2.getToken_hash();
                        str = tokenHash2;
                        if (str != null) {
                            z = false;
                            if (z) {
                            }
                            break;
                        }
                        z = true;
                        if (z) {
                        }
                        break;
                    default:
                        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }
            }
        }
        utilsKt$resolveApiUrl$1 = new UtilsKt$resolveApiUrl$1(continuation);
        utilsKt$resolveApiUrl$12 = utilsKt$resolveApiUrl$1;
        Object $result22 = utilsKt$resolveApiUrl$12.result;
        Object $result32 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
        switch (utilsKt$resolveApiUrl$12.label) {
        }
    }

    public static /* synthetic */ Map buildNewTvHeaders$default(String str, Map map, int i, Object obj) {
        if ((i & 2) != 0) {
            map = MapsKt.emptyMap();
        }
        return buildNewTvHeaders(str, map);
    }

    @NotNull
    public static final Map<String, String> buildNewTvHeaders(@NotNull String ott, @NotNull Map<String, String> map) {
        Map result = MapsKt.toMutableMap(newTvBaseHeaders);
        result.put("Ott", ott);
        for (Map.Entry element$iv : map.entrySet()) {
            String key = element$iv.getKey();
            String value = element$iv.getValue();
            result.put(key, value);
        }
        return result;
    }
}
