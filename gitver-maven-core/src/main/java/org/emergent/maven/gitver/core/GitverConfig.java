package org.emergent.maven.gitver.core;

import static org.emergent.maven.gitver.core.Constants.GV_RELEASE_BRANCHES;
import static org.emergent.maven.gitver.core.Constants.GV_TAG_PATTERN;
import static org.emergent.maven.gitver.core.Constants.GV_VERSION_INITIAL;
import static org.emergent.maven.gitver.core.Constants.GV_VERSION_OVERRIDE;
import static org.emergent.maven.gitver.core.Constants.GV_VERSION_PATTERN;
import static org.emergent.maven.gitver.core.Constants.RELEASE_BRANCHES_DEF;
import static org.emergent.maven.gitver.core.Constants.TAG_PATTERN_DEF;
import static org.emergent.maven.gitver.core.Constants.VERSION_INITAL_DEF;
import static org.emergent.maven.gitver.core.Constants.VERSION_PATTERN_DEF;

import java.util.Map;
import java.util.Properties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.emergent.maven.gitver.core.version.KeywordsConfig;

@Value
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@lombok.Builder(toBuilder = true, setterPrefix = "set", builderClassName = "Builder")
public class GitverConfig {

    @NonNull
    @lombok.Builder.Default
    String tagPattern = TAG_PATTERN_DEF;

    @NonNull
    @lombok.Builder.Default
    String versionOverride = "";

    @NonNull
    @lombok.Builder.Default
    String versionPattern = VERSION_PATTERN_DEF;

    @NonNull
    @lombok.Builder.Default
    String versionInitial = VERSION_INITAL_DEF;

    @NonNull
    @lombok.Builder.Default
    KeywordsConfig keywords = KeywordsConfig.DEFAULT_KEYWORDS;

    @NonNull
    @lombok.Builder.Default
    String releaseBranches = RELEASE_BRANCHES_DEF;

    public static GitverConfig from(Properties props) {
        return builder()
          .setTagPattern(props.getProperty(GV_TAG_PATTERN, TAG_PATTERN_DEF))
          .setVersionOverride(props.getProperty(GV_VERSION_OVERRIDE, ""))
          .setVersionPattern(props.getProperty(GV_VERSION_PATTERN, VERSION_PATTERN_DEF))
          .setReleaseBranches(props.getProperty(GV_RELEASE_BRANCHES, RELEASE_BRANCHES_DEF))
          .setVersionInitial(props.getProperty(GV_VERSION_INITIAL, VERSION_INITAL_DEF))
          .setKeywords(KeywordsConfig.from(props))
          .build();
    }

    public Map<String, String> toProperties() {
        Mapper m = Mapper.create()
          .put(GV_TAG_PATTERN, getTagPattern(), TAG_PATTERN_DEF)
          .put(GV_VERSION_OVERRIDE, getVersionOverride(), "")
          .put(GV_VERSION_PATTERN, getVersionPattern(), VERSION_PATTERN_DEF)
          .put(GV_RELEASE_BRANCHES, getReleaseBranches(), RELEASE_BRANCHES_DEF)
          .put(GV_VERSION_INITIAL, getVersionInitial(), VERSION_INITAL_DEF)
          .putAll(keywords.toProperties());
        return m.toMap();
    }
}

