import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

// https://maven.apache.org/plugins/maven-invoker-plugin/examples/pre-post-build-script.html
// basedir is a predefined global File for the root of the test project
if (!binding.hasVariable('basedir')) {
  throw new IllegalStateException("basedir was undefined!")
}
File dirbase = basedir;
Path basepath = dirbase.toPath().toAbsolutePath();

def gitDotDir = basepath.resolve(".git")
assert Files.exists(gitDotDir)

def gitverPom = basepath.resolve(".gitver.pom.xml")
assert Files.exists(gitverPom)
def gitverPomBody = Files.readString(gitverPom, StandardCharsets.UTF_8)
assert gitverPomBody.contains("<version>1.0.0</version>")

def buildLog = basepath.resolve("build.log")
assert Files.exists(buildLog)
def buildLogBody = Files.readString(buildLog, StandardCharsets.UTF_8)
assert buildLogBody.contains("gitver.version=1.0.0")
assert buildLogBody.contains("Building multi-module-parent 1.0.0")
