# gitver-maven-tools

Tools for generating and manipulating maven project versions from Git logs.

Generate the semver version using the git commit history and automatically set it to maven pom.

No more manually modifying the pom.xml to decide on the versions. You continue developing and adding commits to your
project. When it is time to release, add a commit with a message containing a specific version keyword and watch the
magic happen.

## Overview

This extension iterates over all the commit history and looks for a predefined keywords representing version changes.
It then computes the version number upto current commit.

The extension supports generating Semantic Versions `x.y.z` format. The format pattern is configurable to use
values such as Git hash, branch name etc.

See https://github.com/emergentdotorg/gitver-maven-extension-examples[manikmagar/gitver-maven-extension-examples]
 for examples of using this extension.

## Acknowledgements

This project was inspired by and borrows heavily from the following:
* [git-versioner-maven-extension](https://github.com/manikmagar/git-versioner-maven-extension).
* [maven-git-versioning-extension](https://github.com/qoomon/maven-git-versioning-extension)

## Version Keywords

*Version keywords* are the reserved words that describes which milestone of the release is this.

By default, extension supports following keywords -

- `[major]` - A Major version milestone Eg. 1.0.0 -> 2.0.0
- `[minor]` - A Minor version milestone Eg. 1.1.0 -> 1.2.
- `[patch]` - A Patch version milestone Eg. 1.1.1 -> 1.1.2

To change the keywords, see how to [Customize Version Keywords](#keyword-customization).

## Configuration

This is a maven build core extension that can -

- Participate in maven build lifecycle
- Automatically set the building project's version
- No explicit mojo executions needed to set the version
- Project's POM remain unchanged

To use as a maven build extension,

Create (or modify) `extensions.xml` file in `${project.baseDir}/.mvn/`
to have the following entry -

NOTE: The artifact id is *gitver-maven-_extension_*.

.mvn/extensions.xml

```xml

<extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0
                      http://maven.apache.org/xsd/core-extensions-1.0.0.xsd">
    <extension>
        <groupId>org.emergent.maven.plugins</groupId>
        <artifactId>gitver-maven-extension</artifactId>
        <version>${latest-version-here}</version>
    </extension>
</extensions>
```

See an example test project
at [project-with-extension](gitver-maven-extension/src/test/resources/project-with-extension/).

With just that configuration, next time your project runs any maven goals, you should see version from this module
is used by Maven reactor. Try running `mvn package` on your project.

== How to start with a different version?
It is possible that your project is already released with a certain version.
In that case, you can configure the initial version to start counting versions from.

You can add following properties to `.mvn/gitver-maven-extension.properties` file -

.Example configuration for initial version for extension mode

```properties
gitver.initial.major=1
gitver.initial.minor=3
gitver.initial.patch=4
```

With above initial version configuration, the first version calculated by this extension will be -

- Major: *2.0.0*
- Minor: 1.*4.0*
- Patch: 1.3.*5*

## Version Incrementing

Now that you have extension configured, you can continue with your regular development.

When it is time to increment version, you may use one of the following three goals
to add an *_empty commit_* with an appropriate [Version Keyword](#version-keywords) -

- `gitver:commit-major`: Adds a git commit with a commit message containing *Major* version keyword
- `gitver:commit-minor`: Adds a git commit with a commit message containing *Minor* version keyword
- `gitver:commit-patch`: Adds a git commit with a commit message containing *Patch* version keyword

CAUTION: Use `--non-recursive` flag when running commit goal in a multi-module maven project to avoid adding one commit
per included module.

The default message pattern is `chore(release): [%k]` where `[%k]` is the keyword token. To change the default message
pattern, you could pass `-Dgitver.commit.message=<message>` argument when running the goal.

NOTE: When this extension is configured, it automatically makes `gitver` plugin goals available
with *NO* any additional configuration.

.Example commit patch with a custom message

```shell
mvn gitver:commit-patch "-Dgitver.commit.message=chore: [%k] release" --non-recursive
```

Of course, you can also add commits manually with appropriate version keywords.

.Manually adding a version commit

```shell
# where `<keyword>` can be one of: major, minor, or patch.
git commit --allow-empty -m "chore: [<keyword>] release"
```

## Version Pattern Customization

The default version pattern used is `major.minor.patch(-commit)` where `(-commit)` is skipped if commit count is 0.

This pattern can be canged by setting a property in `.mvn/gitver-maven-extension.properties`.

The following example will generate versions as `major.minor.patch+shorthash`, eg. `1.2.3+a5a29f8`.

.Example configuration for version pattern in extension mode

```properties
gitver.version.pattern=%M.%m.%p+%h
```

Available Tokens for Version Pattern

| Token          | Description                    | Example                                                                                                                          |
|----------------|--------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| %M             | Major Version                  | **1**.y.z                                                                                                                        |
| %m             | Minor Version                  | x.**1**.z                                                                                                                        |
| %p             | Patch Version                  | x.y.**1**                                                                                                                        |
| %P             | Non-Zero Commit adjusted patch | Given _%M.%m.%P(-SNAPSHOT)_ with _%M=1_, _%m=2_, _%p=3_ <br/>when c == 0 -> _1.2.3_ <br/>when c > 0, = 5 -> _1.2.**4-SNAPSHOT**_ |
| %c             | Commit count                   | x.y.z-**4**                                                                                                                      |
| ([anything]%c) | Non-Zero Commit count          | Given _%M.%m.%p(-%c)_ with _%M=1_, _%m=2_, _%p=3_ <br/>when c == 0 -> _1.2.3_ <br/>when c > 0, = 5 -> _1.2.3-**5**_              |
| %b             | Branch name                    | _%M.%m.%p+%b_ -> _1.2.3+**main**_                                                                                                |
| %H             | Long Hash Ref                  | _%M.%m.%p+%H_ -> _1.2.3+**b5f600c40f362d9977132e8bf7398d2cdc745c28**_                                                            |
| %h             | Short Hash Ref                 | _%M.%m.%p+%H_ -> _1.2.3+**a5a29f8**_                                                                                             |

## Keyword Customization

The default [version keywords](#version-keywords) `[major]`, `[minor]`, and `[patch]` can be customized by overriding
the configuration. To use different keywords, you can add following properties to the
`.mvn/gitver-maven-extension.properties` file.

Example configuration for initial version for extension mode

```properties
gitver.keywords.major=[BIG]
gitver.keywords.minor=[SMALL]
gitver.keywords.patch=[FIX]
```

## Keyword Regex

You can also use regex to match version keywords. This is useful when you want to be sure that the version keyword will
only be matched when it is the first word in the commit message. So if for example you have a merge commit message
which contains the messages of the merged commits, you can use a regex to match only the first commit message.

To use regex for version keywords, you can add following properties to `.mvn/gitver-maven-extension.properties` file -

Example configuration for regex version keywords

```properties
gitver.keywords.regex=true
gitver.keywords.major=^\\[major\\].*
gitver.keywords.minor=^\\[minor\\].*
gitver.keywords.patch=^\\[patch\\].*
```

## Generated Version Access

This extension adds all version properties to *Maven properties* during build cycle -

Example of Injected maven properties (demo values)

```properties
gitver.commitNumber=0
gitver.major=0
gitver.minor=0
gitver.patch=1
gitver.version=0.0.1
gitver.branch=main
gitver.hash=67550ad6a64fe4e09bf9e36891c09b2f7bdc52f9
gitver.hash.short=67550ad
```

You may use these properties in maven pom file, for example as `${git.branch}` to access git branch name.

## Git Tag Creation

You can use `gitver:tag` goal to create a git tag for current version in local git repository.

NOTE: This does not push tag to remote repository.

Tag goal with default parameter values

```shell
mvn gitver:tag \
  "-Dtag.failWhenTagExist=true" \
  "-Dtag.messagePattern=Release version %v" \
  "-Dtag.namePattern=v%v"
```

For Tag goal, it is possible to configure pom.xml to contain the gitver plugin with required execution configuration.

Git Tag Goal with default configuration parameters

```xml
<plugin>
  <groupId>org.emergent.maven</groupId>
  <artifactId>gitver-maven-plugin</artifactId>
  <executions>
    <execution>
      <id>tag</id>
      <goals>
        <goal>tag</goal>
      </goals>
      <configuration>
        <failWhenTagExist>true</failWhenTagExist> <!-- <1> -->
        <tagNamePattern>v%v</tagNamePattern> <!-- <2> -->
        <tagMessagePattern>Release version %v</tagMessagePattern> <!-- <3> -->
      </configuration>
    </execution>
  </executions>
</plugin>
```

<1> If set to not fail, it will just log warning and skip tag creation.
<2> Tag name pattern to use. Default `v%v` will result in tags like `v1.2.3`.
<3> Tag message pattern to use. Default `Release version %v` will add tag message like `Release version 1.2.3`.

## Contributing

### Build

#### Java Support

Source code uses *Java 17* for development.

#### Installation

Install artifacts to local repository -

```shell
./mvnw install
```

#### More Examples

```shell
./mvnw -DprocessAllModules=true -DgenerateBackupPoms=false  org.codehaus.mojo:versions-maven-plugin:2.18.0:set -DnextSnapshot=true
```

```shell
./mvnw -DprocessAllModules=true -DgenerateBackupPoms=false  org.codehaus.mojo:versions-maven-plugin:2.18.0:set -DnewVersion=0.6.0
```
