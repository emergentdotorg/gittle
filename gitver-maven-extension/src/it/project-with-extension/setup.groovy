static String s(Object o) {return String.valueOf(o)}

if (!binding.hasVariable('basedir')) {
  throw new IllegalStateException("basedir was undefined!")
}
File basedir = (File)binding.getVariable('basedir')
GroovyShell shell = new GroovyShell()
shell.setVariable('basedir', basedir)
def tools = shell.parse(new File((File)binding.getVariable('basedir'), '../tools/tools.groovy'))
assert !tools.resolve('.git').exists()

def keyword = "[major]"

tools.bash(basedir, [
  s("echo PWD=\${PWD}"),
  s("git init --initial-branch main ."),
  s("git commit --allow-empty -m \'chore(release): $keyword\'"),
  s("git tag \'v1.0.0\'")
] as String[])
