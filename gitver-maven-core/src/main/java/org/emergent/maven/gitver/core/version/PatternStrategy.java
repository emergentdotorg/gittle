package org.emergent.maven.gitver.core.version;

import static java.util.regex.Pattern.quote;
import static org.emergent.maven.gitver.core.Constants.RELEASE_BRANCHES_DEF;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.PropCodec;
import org.emergent.maven.gitver.core.Util;

@Value
//@Accessors(fluent = true)
@lombok.Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class PatternStrategy implements VersionStrategy<PatternStrategy> {

    public static final PatternStrategy DEFAULT = PatternStrategy.builder().build();

    public static final String VERSION_PATTERN_DEF = "%t(-%B)(-%c)(-%S)+%h(.%d)";

    @SerializedName("gitver")
    @NonNull
    @lombok.Builder.Default
    GitverConfig config = GitverConfig.builder().build();

    // @NonNull
    // @lombok.Builder.Default
    // String tagPattern = TAG_PATTERN_DEF;
    //
    // @NonNull
    // @lombok.Builder.Default
    // String versionOverride = "";
    //
    // @NonNull
    // @lombok.Builder.Default
    // String versionPattern = VERSION_PATTERN_DEF;
    //
    // @NonNull
    // @lombok.Builder.Default
    // String releaseBranches = RELEASE_BRANCHES_DEF;

    @SerializedName("gv_branch")
    @lombok.Builder.Default
    @NonNull
    String branch = "main";

    @SerializedName("gv_hash")
    @lombok.Builder.Default
    @NonNull
    String hash = "abcdef01";

    @SerializedName("gv_tagged")
    @lombok.Builder.Default
    @NonNull
    String tagged = "0.0.0";

    @SerializedName("gv_commits")
    int commits;

    @SerializedName("gv_dirty")
    boolean dirty;

//    @Override
//    public GitverConfig getConfig() {
//        return config;
//    }

    public String getHashShort() {
        return hash.substring(0, 8);
    }

    @Override
    public String toString() {
        return String.format(
                "%s [branch: %s, version: %s, hash: %s]", getClass().getSimpleName(), branch, toVersionString(), hash);
    }

    @Override
    public String toVersionString() {
        String pattern = config.getVersionPattern();
        Map<String, String> values = getReplacementMap();
        return performTokenReplacements(pattern, values);
    }

    Map<String, String> getReplacementMap() {
        String devBranch = config.getReleaseBranchesSet().contains(branch) ? "" : branch;
        return Arrays.stream(PatternToken.values())
          .collect(Collectors.toMap(
            PatternToken::id,
            t -> String.valueOf(
              switch (t) {
                  case TAG -> tagged;
                  case COMMIT -> Integer.valueOf(commits);
                  case SNAPSHOT -> commits > 0 ? "SNAPSHOT" : "";
                  case BRANCH -> branch;
                  case DEV_BRANCH -> devBranch;
                  case HASH_SHORT -> getHashShort();
                  case HASH -> hash;
                  case DIRTY -> dirty ? "dirty" : "";
              }
              )
          ));
    }

    private static String performTokenReplacements(String versionPattern, Map<String, String> codeReplMap) {
        String codes =
          Arrays.stream(PatternToken.values()).map(PatternToken::code).collect(Collectors.joining());
        String tokenRegex = quote("%") + "[" + codes + "]";
        Pattern patternx = Pattern.compile("(?<uni>" + tokenRegex + ")"
          + "|\\("
          + "(?<pre>[^()%]+)?(?<mid>" + tokenRegex + ")(?<suf>[^()%]+)?"
          + "\\)");

        Matcher m = patternx.matcher(versionPattern);
        AtomicInteger priorEnd = new AtomicInteger(-1);

        String result = m.results()
          .flatMap(r -> {
              String uni = null;
              String pre = null;
              String mid = null;
              String suf = null;
              for (int ii = 1; ii <= r.groupCount(); ii++) {
                  switch (ii) {
                      case 1:
                          uni = r.group(ii);
                          break;
                      case 2:
                          pre = r.group(ii);
                          break;
                      case 3:
                          mid = r.group(ii);
                          break;
                      case 4:
                          suf = r.group(ii);
                          break;
                  }
              }

              List<String> res = new LinkedList<>();
              String tokId = "";
              if (Util.isNotBlank(uni)) {
                  tokId = uni;
                  res.add(Optional.ofNullable(codeReplMap.get(tokId)).orElse(""));
              } else if (Util.isNotBlank(mid)) {
                  tokId = mid;
                  String repl =
                    Optional.ofNullable(codeReplMap.get(tokId)).orElse("");
                  if (!repl.isEmpty() && !"0".equals(repl)) {
                      Stream.of(pre, repl, suf).filter(Util::isNotEmpty).forEach(res::add);
                  }
              }

              int priorMatchEnd = priorEnd.getAndUpdate($ -> r.end());
              if (priorMatchEnd > -1 && priorMatchEnd < r.start()) {
                  String unmatchedPreviousSegment = versionPattern.substring(priorMatchEnd, r.start());
                  res.add(0, unmatchedPreviousSegment);
              }
              return res.stream();
          })
          .filter(Objects::nonNull)
          .collect(Collectors.joining());

        // append any remaining after the matches ran out
        int lastMatchEndIdx = priorEnd.get();
        result = lastMatchEndIdx < 0 ? result : result.concat(versionPattern.substring(lastMatchEndIdx));
        return result;
    }

    public static PatternStrategy from(Properties props) {
        return from(Util.toStringStringMap(props));
    }

    public static PatternStrategy from(Map<String, String> props) {
        Map<String, String> fixed = new LinkedHashMap<>();
        props.forEach((k, v) -> fixed.put(
                k,
//                Util.substringAfter(k, "gv."),
                v));
        return PropCodec.getInstance().fromProperties(fixed, PatternStrategy.class);
    }

    @Override
    public Properties toProperties() {
        Properties props = new Properties();
        asMap().forEach(props::setProperty);
        return props;
    }

    @Override
    public Map<String, String> asMap() {
        Map<String, String> map = PropCodec.getInstance()
                .toProperties(this, DEFAULT, this.getClass());
        return map.entrySet().stream()
                .collect(Collectors.toMap(e ->
//                        "gv." +
                                e.getKey(), Map.Entry::getValue));
    }

    @Getter
    @Accessors(fluent = true)
    public enum PatternToken {
        TAG("t"),
        COMMIT("c"),
        SNAPSHOT("S"),
        BRANCH("b"),
        DEV_BRANCH("B"),
        HASH_SHORT("h"),
        HASH("H"),
        DIRTY("d");

        private final String id;
        private final String code;

        PatternToken(String code) {
            this.id = "%" + code;
            this.code = code;
        }

        @Override
        public String toString() {
            return id();
        }
    }
}
