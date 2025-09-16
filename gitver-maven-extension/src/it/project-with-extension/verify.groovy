GroovyShell shell = new GroovyShell()
shell.setVariable('basedir', (File)binding.getVariable('basedir'))
def tools = shell.parse(new File((File)binding.getVariable('basedir'), '../tools/tools.groovy'))

static String s(Object o) {return String.valueOf(o)}

assert tools.checkNoErrors()
assert tools.checkGitDotDirExists()
File gitverPom = tools.resolve(".gitver.pom.xml")
assert gitverPom != null && gitverPom.exists()
String gitverPomBody = tools.readFile(gitverPom)

def expectedVersion = '1.0.0'
String version = tools.getVersion(gitverPomBody)
assert version == expectedVersion
assert gitverPomBody.contains(s("<revision>$version</revision>"))
assert tools.verifyTextInLog(s("Building gitver-extension-test $version"))
