import java.nio.file.Files
import java.nio.file.Path

// https://maven.apache.org/plugins/maven-invoker-plugin/examples/pre-post-build-script.html
// basedir is a predefined global File for the root of the test project
if (!binding.hasVariable('basedir')) {
  throw new IllegalStateException("basedir was undefined!")
}
File dirbase = basedir;
Path basepath = dirbase.toPath().toAbsolutePath();

def gitDotDir = basepath.resolve(".git" )
assert Files.exists(gitDotDir)
