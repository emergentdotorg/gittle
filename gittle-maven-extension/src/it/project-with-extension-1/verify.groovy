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
def version = tools.getGitverPomVersion(gitverPom)
assert expectedVersion == version
assert tools.verifyTextInLog("Building gitver-extension-test $version")
