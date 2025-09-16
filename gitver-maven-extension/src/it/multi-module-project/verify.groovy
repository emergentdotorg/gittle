static String s(Object o) {return String.valueOf(o)}

if (!binding.hasVariable('basedir')) {
  throw new IllegalStateException("basedir was undefined!")
}
File basedir = (File)binding.getVariable('basedir')
GroovyShell shell = new GroovyShell()
shell.setVariable('basedir', basedir)
println "scriptFile=${new File((File)binding.getVariable('basedir'), '../tools/tools.groovy')}"
def tools = shell.parse(new File((File)binding.getVariable('basedir'), '../tools/tools.groovy'))

File gitDotDir = tools.resolve(".git")
File gitverPom = tools.resolve(".gitver.pom.xml")
File buildLog = tools.resolve("build.log")
String gitverPomBody = tools.readFile(gitverPom)
String buildLogBody = tools.readFile(buildLog)
assert gitDotDir != null && gitDotDir.exists()
assert gitverPom != null && gitverPom.exists()
assert buildLog != null && buildLog.exists()

assert gitverPomBody.contains("<version>1.0.0</version>")
assert buildLogBody.contains("Building multi-module-parent 1.0.0")
