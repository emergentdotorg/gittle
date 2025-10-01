GroovyShell shell = new GroovyShell()
shell.setVariable('basedir', (File)binding.getVariable('basedir'))
def tools = shell.parse(new File((File)binding.getVariable('basedir'), '../tools/tools.groovy'))

static String s(Object o) {return String.valueOf(o)}

String expectedVersion="1.0.0"

assert tools.verifyNoErrorsInLog()
def dotGitDir = tools.resolveFile('.git')
assert dotGitDir != null && dotGitDir.isDirectory()
File gitverPom = tools.resolveGitverPomFile()
assert gitverPom != null && gitverPom.isFile()
String version = tools.getGitverPomVersion(gitverPom)
assert expectedVersion == version
String gitverPomBody = tools.readFile(gitverPom)
assert gitverPomBody.contains(s("<version>$version</version>"))
assert tools.verifyTextInLog("Building multi-module-parent $version")
