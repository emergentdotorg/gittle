package org.emergent.maven.gitver.core.version;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.PropCodec;
import org.emergent.maven.gitver.core.Util;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.regex.Pattern.quote;

@Value
@Accessors(fluent = true)
//@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Builder(toBuilder = true, builderClassName = "Builder")
public class ResolvedData implements PropCodec.Codable<ResolvedData> {

    @NonNull
    @lombok.Builder.Default
    String branch = "";
    @NonNull
    @lombok.Builder.Default
    String hash = "";
    @NonNull
    @lombok.Builder.Default
    String tagged = "0.0.0";
    int commits;
    boolean dirty;

    public static Builder builder() {
        return new Builder();
    }

    public String getHashShort() {
        return Optional.ofNullable(hash).map(s -> s.substring(0, Math.min(8, s.length()))).orElse("");
    }

    public static ResolvedData from(Map<String, String> props) {
        return PropCodec.fromProperties(props, ResolvedData.class);
    }

    public Map<String, String> asMap() {
        return PropCodec.toProperties(this);
    }

    public static class Builder {

    }
}
