//import tools.Tools

//import tools.Tools

//GroovyShell shell = new GroovyShell()
//shell.setVariable('basedir', binding.getVariable('basedir'))
//tools = shell.parse(new File((File)binding.getVariable('basedir'),'../tools/tools.groovy'))
//tools.metaClass.methods.each {
//  if (it.declaringClass.getTheClass() == tools.class && Set.of('q', 's').contains(it.name)) {
//    this.metaClass."$it.name" = tools.&"$it.name"
//  }
//}

static String s(Object o) {return String.valueOf(o)}
if (!binding.hasVariable('basedir')) {
  throw new IllegalStateException("basedir was undefined!")
}
File basedir = (File) binding.getVariable('basedir')
GroovyShell shell = new GroovyShell()
shell.setVariable('basedir', basedir)
def scriptFile = new File((File) binding.getVariable('basedir'), '../tools/tools.groovy')
println "scriptFile=$scriptFile"
def tools = shell.parse(scriptFile)

//File basedir = (File) binding.getVariable('basedir')
//File basedir
//if (binding.hasVariable('basedir')) {
//  basedir = (File)binding.getVariable('basedir')
//} else {
//  basedir = new File('../../../target/it/project-with-extension').getAbsoluteFile().getCanonicalFile()
//}

//Tools tools
//if (binding.hasVariable('basedir')) {
//  tools = new Tools((File) binding.getVariable('basedir'))
//} else {
//  tools = new Tools(new File('../../../target/it/project-with-extension').getAbsoluteFile().getCanonicalFile())
//}



println "basedir=$tools.basedir"

File gitDotDir = tools.resolve(".git")
assert gitDotDir != null && gitDotDir.exists()
File gitverPom = tools.resolve(".gitver.pom.xml")
File buildLog = tools.resolve("build.log")
String gitverPomBody = tools.readFile(gitverPom)
String buildLogBody = tools.readFile(buildLog)
assert gitverPom != null && gitverPom.exists()
assert buildLog != null && buildLog.exists()

def expectedVersion = '1.0.0'
//def expectedVersion = '0.0.0-SNAPSHOT'
String version = tools.getVersion(gitverPomBody)
assert version == expectedVersion
assert gitverPomBody.contains(s("<revision>$version</revision>"))
assert buildLogBody.contains(s("Building gitver-extension-test $version"))

//
//
//// https://maven.apache.org/plugins/maven-invoker-plugin/examples/pre-post-build-script.html
//// basedir is a predefined global File for the root of the test project
//if (!binding.hasVariable('basedir')) {
//  throw new IllegalStateException("basedir was undefined!")
//}
//
//
//class Tools {
//
//  File basedir = new File('.')
//
//  Tools(File basedir) {
//    this.basedir = basedir;
//  }
//
//  File resolve(String path) {
//    return basedir.toPath().toAbsolutePath().resolve(path).toFile()
//  }
//
//  static String q(String s) {
//    return Pattern.quote(s)
//  }
//
//  String readFile(String path) {
//    return readFile(resolve(path))
//  }
//
//  static String readFile(File path) {
//    assert path != null && path.exists()
//    return Files.readString(path.toPath(), StandardCharsets.UTF_8)
//  }
//
//  static Element getXmlRecords(String xml) {
//    def builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
//    def inputStream = new ByteArrayInputStream(xml.bytes)
//    return builder.parse(inputStream).documentElement
//  }
//
//  static String processXml(String xpathQuery, String xml) {
//    def records = getXmlRecords(xml)
//    def xpath = XPathFactory.newInstance().newXPath()
//    return xpath.evaluate(xpathQuery, records)
//  }
//
//  static String getVersion(String xml) {
//    def builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
//    def inputStream = new ByteArrayInputStream(xml.bytes)
//    def records = builder.parse(inputStream).documentElement
//    def xpath = XPathFactory.newInstance().newXPath()
//    //  String version = processXml(xml, '//project/version')
//    String version = xpath.evaluate('//project/version', records)
//    if ('' == version) {
//      version = xpath.evaluate('//project/parent/version', records)
//    }
//    if ('${revision}' == version) {
//      version = xpath.evaluate('//project/properties/revision', records)
//    }
//    return version
//  }
//
//  //def loadScript(String file) {
//  //  Binding scriptBinding = new Binding()
//  //  scriptBinding.setVariable('basedir', binding.getVariable('basedir'))
//  //  def script = new GroovyShell(scriptBinding).parse(new File((File)binding.getVariable('basedir'), file))
//  //  script.metaClass.methods.each {
//  //    if (it.declaringClass.getTheClass() == script.class && !it.name.contains('$')
//  //      && it.name != 'main' && it.name != 'run') {
//  //      this.metaClass."$it.name" = script.&"$it.name"
//  //    }
//  //  }
//  //}
//  //loadScript('../tools/tools.groovy')
//
//  //@Field private CompilerConfiguration configuration
//  //configuration = new CompilerConfiguration()
//  //configuration.setScriptBaseClass('ToolsScript')
//  //GroovyShell shell = new GroovyShell(configuration)
//  //shell.setVariable('basedir', binding.getVariable('basedir'))
//  //shell.setVariable('ToolsScript', ToolsScript)
//  //tools = (ToolsScript)shell.parse(new File((File)binding.getVariable('basedir'),'../tools/tools.groovy'))
//  //tools.metaClass.methods.each {
//  //  if (it.declaringClass.getTheClass() == tools.class && Set.of('s').contains(it.name)) {
//  //    this.metaClass."$it.name" = tools.&"$it.name"
//  //  }
//  //}
//
//  //def tools = new GroovyScriptEngine( '..' ).with {
//  //  loadScriptByName('tools/tools.groovy')
//  //}
//  //this.metaClass.mixin tools
//
//  //Tools.metaClass.methods.each {
//  //  if (it.declaringClass.getTheClass() == Tools.class && Set.of('s').contains(it.name)) {
//  //    this.metaClass."$it.name" = Tools.&"$it.name"
//  //  }
//  //}
//
//}
//
//def tools = new Tools((File) binding.getVariable('basedir'))
//
//println 'hi'
//
//File gitDotDir = tools.resolve('.git')
//File gitverPom = tools.resolve('.gitver.pom.xml')
//File buildLog = tools.resolve('build.log')
//String gitverPomBody = tools.readFile(gitverPom)
//String buildLogBody = tools.readFile(buildLog)
//assert gitDotDir != null && gitDotDir.exists()
//assert gitverPom != null && gitverPom.exists()
//assert buildLog != null && buildLog.exists()

//script.method()

//def parent = getClass().getClassLoader()
//def loader = new GroovyClassLoader(parent)
//def clazz = loader.parseClass('def test() { "new class definition" }');
//def clazz = loader.parseClass(Files.readString(new File(basedir,'../tools/tools.groovy').toPath(), StandardCharsets.UTF_8));
//def tools = clazz.newInstance()
//assert tools.test() == "new class definition"


//def userProperties = context.get('userProperties')
//def server = new MockServer()
//userProperties.put('serverHost', server.getHost())
//userProperties.put('serverPort', server.getPort())

//File scriptDirFile = new File(getClass().protectionDomain.codeSource.location.path).getParentFile().getAbsoluteFile();
//def workPath = scriptDirFile.toPath()

