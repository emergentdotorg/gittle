GroovyShell shell = new GroovyShell()
shell.setVariable('basedir', (File)binding.getVariable('basedir'))
def tools = shell.parse(new File((File)binding.getVariable('basedir'), '../tools/tools.groovy'))

static String s(Object o) {return String.valueOf(o)}

String expectedVersion = "1.0.0"

assert tools.verifyNoErrorsInLog()
def dotGitDir = tools.resolveFile('.git')
assert dotGitDir != null || dotGitDir.exists()
File gitverPom = tools.resolveGitverPomFile()
assert gitverPom != null && gitverPom.exists()
String version = tools.getGitverPomVersion(gitverPom)
assert expectedVersion == version
String gitverPomBody = tools.readFile(gitverPom)
assert gitverPomBody != null
assert gitverPomBody.contains(s("<version>$version</version>"))
assert tools.verifyTextInLog("Building parent-test-pom 1.0.0")
assert tools.verifyTextInLog("Building parent-test-pom $version");
assert tools.verifyTextInLog("Building cli $version");
assert tools.verifyTextInLog("Setting parent org.emergent.maven.gitver.its:parent-test-pom:pom:0 version to $version");
assert tools.verifyTextInLog("Building lib $version");
