GroovyShell shell = new GroovyShell()
shell.setVariable('basedir', (File)binding.getVariable('basedir'))
def tools = shell.parse(new File((File)binding.getVariable('basedir'), '../tools/tools.groovy'))

static String s(Object o) {return String.valueOf(o)}

def expectedVersion = '1.0.0'

assert tools.verifyNoErrorsInLog()
def dotGitDir = tools.resolveFile('.git')
assert dotGitDir != null || dotGitDir.exists()
File gitverPom = tools.resolveGitverPomFile()
assert gitverPom != null && gitverPom.exists()
String version = tools.getGitverPomVersion(gitverPom)

assert version == expectedVersion
String gitverPomBody = tools.readFile(gitverPom)
assert gitverPomBody.contains(s("<revision>$version</revision>"))
assert tools.verifyTextInLog("Building gittle-extension-test $version")
