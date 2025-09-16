static String s(Object o) {return String.valueOf(o)}
if (!binding.hasVariable('basedir')) {
  throw new IllegalStateException("basedir was undefined!")
}
File basedir = (File) binding.getVariable('basedir')
GroovyShell shell = new GroovyShell()
shell.setVariable('basedir', basedir)
def scriptFile = new File((File)binding.getVariable('basedir'), '../tools/tools.groovy')
println "scriptFile=$scriptFile"
def tools = shell.parse(scriptFile)

File gitDotDir = tools.resolve(".git")
File gitverPom = tools.resolve(".gitver.pom.xml")
File buildLog = tools.resolve("build.log")
String gitverPomBody = tools.readFile(gitverPom)
String buildLogBody = tools.readFile(buildLog)
assert gitDotDir != null && gitDotDir.exists()
assert gitverPom != null && gitverPom.exists()
assert buildLog != null && buildLog.exists()

assert gitverPomBody.contains("<version>1.0.0</version>")
//assert buildLogBody.contains("gitver.version=1.0.0")
assert buildLogBody.contains("Building parent-test-pom 1.0.0")
