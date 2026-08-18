GroovyShell shell = new GroovyShell()
def basedir = (File) binding.getVariable('basedir')
shell.setVariable('basedir', basedir)
def tools = shell.parse(new File(basedir, '../tools/tools.groovy'))

static String s(Object o) { return String.valueOf(o) }

assert !tools.resolveFile('.git').exists()

def keyword = "[major]"

tools.logPwd()
tools.gitInit()
tools.gitCommit(s("chore(release): $keyword"))
tools.gitTag('v1.0.0')

