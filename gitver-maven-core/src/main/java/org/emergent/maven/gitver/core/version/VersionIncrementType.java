package org.emergent.maven.gitver.core.version;

import java.util.Locale;

public enum VersionIncrementType {
    MAJOR,
    MINOR,
    PATCH,
    COMMIT;

    public String getMojoName() {
        String name = "commit";
        if (this != COMMIT) {
            name += "-" + this.name().toLowerCase(Locale.ROOT);
        }
        return name;
    }
}
