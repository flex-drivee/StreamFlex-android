package com.horis.cncverse.entities;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
/* compiled from: Source.kt */
@Metadata(d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\r\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u001f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003¢\u0006\u0004\b\u0006\u0010\u0007J\t\u0010\f\u001a\u00020\u0003HÆ\u0003J\t\u0010\r\u001a\u00020\u0003HÆ\u0003J\t\u0010\u000e\u001a\u00020\u0003HÆ\u0003J'\u0010\u000f\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u0003HÆ\u0001J\u0014\u0010\u0010\u001a\u00020\u00112\b\u0010\u0012\u001a\u0004\u0018\u00010\u0001HÖ\u0083\u0004J\n\u0010\u0013\u001a\u00020\u0014HÖ\u0081\u0004J\n\u0010\u0015\u001a\u00020\u0003HÖ\u0081\u0004R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0011\u0010\u0004\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\n\u0010\tR\u0011\u0010\u0005\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\t¨\u0006\u0016"}, d2 = {"Lcom/horis/cncverse/entities/Source;", "", "file", "", "label", "type", "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "getFile", "()Ljava/lang/String;", "getLabel", "getType", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "", "toString", "CNC Verse Mobile_debug"}, k = 1, mv = {2, 3, 0}, xi = 48)
/* loaded from: /sdcard/AndroidIDEProjects/Cloudstream reference/cncverse/CNC Verse Mobile/resources/classes.dex */
public final class Source {
    @NotNull
    private final String file;
    @NotNull
    private final String label;
    @NotNull
    private final String type;

    public static /* synthetic */ Source copy$default(Source source, String str, String str2, String str3, int i, Object obj) {
        if ((i & 1) != 0) {
            str = source.file;
        }
        if ((i & 2) != 0) {
            str2 = source.label;
        }
        if ((i & 4) != 0) {
            str3 = source.type;
        }
        return source.copy(str, str2, str3);
    }

    @NotNull
    public final String component1() {
        return this.file;
    }

    @NotNull
    public final String component2() {
        return this.label;
    }

    @NotNull
    public final String component3() {
        return this.type;
    }

    @NotNull
    public final Source copy(@NotNull String str, @NotNull String str2, @NotNull String str3) {
        return new Source(str, str2, str3);
    }

    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Source) {
            Source source = (Source) obj;
            return Intrinsics.areEqual(this.file, source.file) && Intrinsics.areEqual(this.label, source.label) && Intrinsics.areEqual(this.type, source.type);
        }
        return false;
    }

    public int hashCode() {
        return (((this.file.hashCode() * 31) + this.label.hashCode()) * 31) + this.type.hashCode();
    }

    @NotNull
    public String toString() {
        return "Source(file=" + this.file + ", label=" + this.label + ", type=" + this.type + ')';
    }

    public Source(@NotNull String file, @NotNull String label, @NotNull String type) {
        this.file = file;
        this.label = label;
        this.type = type;
    }

    @NotNull
    public final String getFile() {
        return this.file;
    }

    @NotNull
    public final String getLabel() {
        return this.label;
    }

    @NotNull
    public final String getType() {
        return this.type;
    }
}
