package org.emergent.maven.gitver.core.version;

import lombok.Builder;
import org.emergent.maven.gitver.core.Util;

@Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public record RefData(String branch, String hash) {

    public String hashShort() {
        return Util.toShortHash(hash);
    }

    public String getHashShort() {
        return hashShort();
    }
}
