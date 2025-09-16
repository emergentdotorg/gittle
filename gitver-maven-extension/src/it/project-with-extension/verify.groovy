static String s(Object o) {return String.valueOf(o)}

if (!binding.hasVariable('basedir')) {
  throw new IllegalStateException("basedir was undefined!")
}
File basedir = (File)binding.getVariable('basedir')
GroovyShell shell = new GroovyShell()
shell.setVariable('basedir', basedir)
def scriptFile = new File((File)binding.getVariable('basedir'), '../tools/tools.groovy')
println "scriptFile=$scriptFile"
def tools = shell.parse(scriptFile)

File gitDotDir = tools.resolve(".git")
assert gitDotDir != null && gitDotDir.exists()
File gitverPom = tools.resolve(".gitver.pom.xml")
File buildLog = tools.resolve("build.log")
String gitverPomBody = tools.readFile(gitverPom)
String buildLogBody = tools.readFile(buildLog)
assert gitverPom != null && gitverPom.exists()
assert buildLog != null && buildLog.exists()

def expectedVersion = '1.0.0'
String version = tools.getVersion(gitverPomBody)
assert version == expectedVersion
assert gitverPomBody.contains(s("<revision>$version</revision>"))
assert buildLogBody.contains(s("Building gitver-extension-test $version"))
