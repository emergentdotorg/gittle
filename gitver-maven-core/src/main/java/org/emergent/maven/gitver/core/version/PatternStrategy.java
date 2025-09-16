package org.emergent.maven.gitver.core.version;

import static java.util.regex.Pattern.quote;
import static org.emergent.maven.gitver.core.Constants.GITVER_BRANCH;
import static org.emergent.maven.gitver.core.Constants.GITVER_COMMIT_NUMBER;
import static org.emergent.maven.gitver.core.Constants.GITVER_HASH;
import static org.emergent.maven.gitver.core.Constants.GITVER_HASH_SHORT;
import static org.emergent.maven.gitver.core.Constants.GITVER_VERSION;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.Mapper;
import org.emergent.maven.gitver.core.Util;

@Value
@Accessors(fluent = true)
@lombok.Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class PatternStrategy implements VersionStrategy {

    public static final String VERSION_PATTERN_DEF = "%t(-%B)(-%c)(-%S)+%h(.%d)";

    @NonNull
    GitverConfig config;
    @NonNull
    String branch;
    @NonNull
    String hash;
    @lombok.Builder.Default
    @NonNull
    String tag = "0.0.0";
    int commits;
    boolean dirty;

    @Override
    public String toVersionString() {
        String pattern = config.getVersionPattern();
        Map<String, String> values = getReplacementMap();
        return performTokenReplacements(pattern, values);
    }

    Map<String, String> getReplacementMap() {
        String devBranch = config.getReleaseBranchesSet().contains(branch) ? "" : branch;
        return Arrays.stream(PatternToken.values()).collect(Collectors.toMap(
          PatternToken::id, t -> String.valueOf(switch (t) {
              case TAG -> tag;
              case COMMIT -> commits;
              case SNAPSHOT -> commits > 0 ? "SNAPSHOT" : "";
              case BRANCH -> branch;
              case DEV_BRANCH -> devBranch;
              case HASH_SHORT -> getHashShort();
              case HASH -> hash;
              case DIRTY -> dirty ? "dirty" : "";
          })));
    }

    private static String performTokenReplacements(String versionPattern, Map<String, String> codeReplMap) {
        String codes = Arrays.stream(PatternToken.values()).map(PatternToken::code).collect(Collectors.joining());
        String tokenRegex = quote("%") + "[" + codes + "]";
        Pattern patternx = Pattern.compile("(?<uni>" + tokenRegex + ")"
          + "|\\("
          + "(?<pre>[^()%]+)?(?<mid>" + tokenRegex + ")(?<suf>[^()%]+)?"
          + "\\)");

        Matcher m = patternx.matcher(versionPattern);
        AtomicInteger priorEnd = new AtomicInteger(-1);

        String result = m.results().flatMap(r -> {
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
                  String repl = Optional.ofNullable(codeReplMap.get(tokId)).orElse("");
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

    @Override
    public Map<String, String> getPropertiesMap() {
        return Mapper.create()
                .putAll(config.toProperties())
                .put(GITVER_BRANCH, branch, "")
                .put(GITVER_HASH, hash)
                .put(GITVER_HASH_SHORT, getHashShort())
                .put(GITVER_COMMIT_NUMBER, commits, -1)
                .put(GITVER_VERSION, toVersionString())
                .toMap();
    }

    public String getHashShort() {
        return hash.substring(0, 8);
    }

    @Override
    public String toString() {
        return String.format(
                "%s [branch: %s, version: %s, hash: %s]",
                getClass().getSimpleName(), branch, toVersionString(), hash);
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
