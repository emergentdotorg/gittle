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

def keyword = "[major]"

tools.bash(tools.getDirbase(), [
    "echo PWD=\${PWD}",
    "git init --initial-branch main .",
    "git commit --allow-empty -m \'chore(release): $keyword\'",
    "git tag \'v1.0.0\'"
] as String[])
