package org.emergent.maven.gitver.core.git;

import static org.emergent.maven.gitver.core.Util.VERSION_REGEX;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.emergent.maven.gitver.core.Util;
import org.emergent.maven.gitver.core.version.BasicVersion;

public class TagProvider {

    private static final Predicate<String> TAG_REF_NAME_PREDICATE = VERSION_REGEX.asMatchPredicate();
    private static final Predicate<Ref> TAG_REF_PREDICATE =
            ref -> TAG_REF_NAME_PREDICATE.test(ref.getLeaf().getName());

    private final Repository repository;
    private final Map<ObjectId, List<Ref>> tagMap;

    public TagProvider(Git git) throws GitAPIException {
        this.repository = git.getRepository();
        // create a map of commit-refs and corresponding list of tags
        this.tagMap = git.tagList().call().stream()
                .filter(TAG_REF_PREDICATE)
                .collect(Collectors.groupingBy(this::getObjectId, Collectors.toList()));
    }

    /**
     * Returns a map of descending (highest to lowest) tags pointing to this commit.
     */
    public LinkedList<BasicVersion> getDescendingTags(RevCommit commit) {
        return tagMap.getOrDefault(commit.getId(), Collections.emptyList()).stream()
                .map(ref -> ref.getLeaf().getName())
                .map(BasicVersion::parse)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(BasicVersion.COMPARATOR.reversed())
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private BasicVersion getSimpleVer(Ref tag) {
        BasicVersion.Builder builder = BasicVersion.builder();
        Matcher matcher = VERSION_REGEX.matcher(tag.getLeaf().getName());
        if (matcher.matches()) {
            builder.setMajor(Integer.parseInt(matcher.group("major")))
                    .setMinor(Integer.parseInt(matcher.group("minor")))
                    .setPatch(Integer.parseInt(matcher.group("patch")));
        }
        return builder.build();
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
        try (ObjectReader reader = repository.newObjectReader()) {
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
