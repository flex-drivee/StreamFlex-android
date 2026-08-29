package com.horis.cncverse.entities;

import java.util.List;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
/* compiled from: MainPage.kt */
@Metadata(d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\u0015\u0012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003¢\u0006\u0004\b\u0005\u0010\u0006J\u000f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003HÆ\u0003J\u0019\u0010\n\u001a\u00020\u00002\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003HÆ\u0001J\u0014\u0010\u000b\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u0001HÖ\u0083\u0004J\n\u0010\u000e\u001a\u00020\u000fHÖ\u0081\u0004J\n\u0010\u0010\u001a\u00020\u0011HÖ\u0081\u0004R\u0017\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\b¨\u0006\u0012"}, d2 = {"Lcom/horis/cncverse/entities/MainPage;", "", "post", "", "Lcom/horis/cncverse/entities/PostCategory;", "<init>", "(Ljava/util/List;)V", "getPost", "()Ljava/util/List;", "component1", "copy", "equals", "", "other", "hashCode", "", "toString", "", "CNC Verse Mobile_debug"}, k = 1, mv = {2, 3, 0}, xi = 48)
/* loaded from: /sdcard/AndroidIDEProjects/Cloudstream reference/cncverse/CNC Verse Mobile/resources/classes.dex */
public final class MainPage {
    @NotNull
    private final List<PostCategory> post;

    /* JADX WARN: Multi-variable type inference failed */
    public static /* synthetic */ MainPage copy$default(MainPage mainPage, List list, int i, Object obj) {
        if ((i & 1) != 0) {
            list = mainPage.post;
        }
        return mainPage.copy(list);
    }

    @NotNull
    public final List<PostCategory> component1() {
        return this.post;
    }

    @NotNull
    public final MainPage copy(@NotNull List<PostCategory> list) {
        return new MainPage(list);
    }

    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        return (obj instanceof MainPage) && Intrinsics.areEqual(this.post, ((MainPage) obj).post);
    }

    public int hashCode() {
        return this.post.hashCode();
    }

    @NotNull
    public String toString() {
        return "MainPage(post=" + this.post + ')';
    }

    public MainPage(@NotNull List<PostCategory> list) {
        this.post = list;
    }

    @NotNull
    public final List<PostCategory> getPost() {
        return this.post;
    }
}
