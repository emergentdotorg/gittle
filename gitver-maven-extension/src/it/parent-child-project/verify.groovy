GroovyShell shell = new GroovyShell()
shell.setVariable('basedir', (File)binding.getVariable('basedir'))
def tools = shell.parse(new File((File)binding.getVariable('basedir'), '../tools/tools.groovy'))

static String s(Object o) {return String.valueOf(o)}

assert tools.checkNoErrors()
assert tools.checkGitDotDirExists()
File gitverPom = tools.resolve(".gitver.pom.xml")
assert gitverPom != null && gitverPom.exists()
String gitverPomBody = tools.readFile(gitverPom)

assert gitverPomBody.contains("<version>1.0.0</version>")
assert tools.verifyTextInLog("Building parent-test-pom 1.0.0")
