package org.emergent.maven.gitver.core.git;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.Util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TagProvider {

    private final GitverConfig config;
    private final Git git;
    private final Supplier<Map<ObjectId, List<ComparableVersion>>> tagMap;

    public TagProvider(GitverConfig config, Git git) {
        this.config = config;
        this.git = git;
        this.tagMap = Util.memoize(this::createTagMap);
    }

    private Map<ObjectId, List<ComparableVersion>> createTagMap() {
        // create a map of commit-refs and corresponding list of tags
        try {
            String tagPattern = config.getTagNamePattern();
            if (!tagPattern.startsWith("^")) {
                tagPattern = "^" + tagPattern;
            }
            if (!tagPattern.endsWith("$")) {
                tagPattern = tagPattern + "$";
            }
            Pattern pattern = Pattern.compile(tagPattern);

            return git.tagList().call().stream()
                    .map(ref -> {
                        String refName = ref.getLeaf().getName();
                        String tagName = StringUtils.substringAfter(refName, "refs/tags/");
                        return Map.entry(ref, pattern.matcher(tagName));
                    })
                    .filter(entry -> entry.getValue().matches())
                    .collect(Collectors.groupingBy(
                            entry -> getObjectId(entry.getKey()),
                            Collectors.mapping(
                                    entry -> new ComparableVersion(
                                            entry.getValue().group(1)),
                                    Collectors.toList())));
        } catch (GitAPIException e) {
            throw new GitverException(e);
        }
    }

    /**
     * Returns the tag with the greatest semantic version that points to this commit.
     */
    public Optional<ComparableVersion> getTag(RevCommit commit) {
        List<ComparableVersion> tags = tagMap.get().getOrDefault(commit.getId(), Collections.emptyList());
        return tags.stream().max(Comparator.naturalOrder());
    }

    public ObjectId getObjectId(Ref ref) {
        return getObjectId(resolveRevObject(ref).getLast());
    }

    public ObjectId getObjectId(ObjectId objectId) {
        return getObjectId(resolveRevObject(objectId).getLast());
    }

    public ObjectId getObjectId(RevObject revObj) {
        return Optional.ofNullable(revObj).map(RevObject::getId).orElse(null);
    }

    public LinkedList<RevObject> resolveRevObject(Ref ref) {
        ObjectId objectIdImmediate = getObjectIdImmediate(ref);
        return resolveRevObject(objectIdImmediate);
    }

    public LinkedList<RevObject> resolveRevObject(ObjectId objectId) {
        return getTargetRevObject(getRevObject(objectId));
    }

    private RevObject getRevObject(Ref ref) {
        ObjectId objectIdImmediate = getObjectIdImmediate(ref);
        return getRevObject(objectIdImmediate);
    }

    private RevObject getRevObject(ObjectId objectId) {
        try (ObjectReader reader = git.getRepository().newObjectReader()) {
            //    try {
            ObjectLoader loader = reader.open(objectId);
            int objectType = loader.getType();
            byte[] rawData = loader.getBytes();
            switch (objectType) {
                case Constants.OBJ_EXT:
                    System.out.printf("\text: %s%n", objectId.getName());
                    return null;
                case Constants.OBJ_COMMIT:
                    return RevCommit.parse(rawData);
                case Constants.OBJ_TREE:
                    System.out.printf("\ttree: %s%n", objectId.getName());
                    return null;
                case Constants.OBJ_BLOB:
                    String content = new String(rawData, StandardCharsets.UTF_8);
                    System.out.printf("\tblob: %s, content: %s%n", objectId.getName(), content);
                    return null;
                case Constants.OBJ_TAG:
                    return RevTag.parse(rawData);
                default:
                    System.out.printf("\tother: %s%n", objectId.getName());
                    return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ObjectId getObjectIdImmediate(Ref ref) {
        Util.check(ref.isPeeled() == (ref.getPeeledObjectId() != null));
        return ref.isPeeled() ? ref.getPeeledObjectId() : ref.getObjectId();
    }

    private static LinkedList<RevObject> getTargetRevObject(RevObject revObj) {
        return getTargetRevObject(revObj, new LinkedList<>());
    }

    private static LinkedList<RevObject> getTargetRevObject(RevObject revObj, LinkedList<RevObject> results) {
        return switch (revObj.getType()) {
            case Constants.OBJ_COMMIT -> {
                results.add(revObj);
                yield results;
            }
            case Constants.OBJ_TAG -> {
                // annotated tag
                results.add(revObj);
                RevObject target = ((RevTag) revObj).getObject();
                if (target == null) {
                    yield results;
                }
                yield getTargetRevObject(target, results);
            }
            default -> results;
        };
    }
}
