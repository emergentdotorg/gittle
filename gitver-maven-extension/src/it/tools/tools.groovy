import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory
import org.w3c.dom.Element

// https://maven.apache.org/plugins/maven-invoker-plugin/examples/pre-post-build-script.html
// basedir is a predefined global File for the root of the test project
if (!binding.hasVariable('basedir')) {
  //  binding.setVariable('basedir', new File('.').getAbsoluteFile())
  throw new IllegalStateException("basedir was undefined!")
}


File getBasedir() {
  return (File)binding.getVariable('basedir')
}

File resolve(String path) {
  return getBasedir().toPath().toAbsolutePath().resolve(path).toFile()
}

String readFile(String path) {
  return readFile(resolve(path))
}

static String s(Object o) {
  return String.valueOf(o)
}

static String q(String s) {
  return Pattern.quote(s)
}

static String readFile(File path) {
  assert path != null && path.exists()
  return Files.readString(path.toPath(), StandardCharsets.UTF_8)
}

static Element getXmlRecords(String xml) {
  def builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
  def inputStream = new ByteArrayInputStream(xml.bytes)
  return builder.parse(inputStream).documentElement
}

static String processXml(String xpathQuery, String xml) {
  def records = getXmlRecords(xml)
  def xpath = XPathFactory.newInstance().newXPath()
  return xpath.evaluate(xpathQuery, records)
}

static String getVersion(String xml) {
  def builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
  def inputStream = new ByteArrayInputStream(xml.bytes)
  def records = builder.parse(inputStream).documentElement
  def xpath = XPathFactory.newInstance().newXPath()
  //  String version = processXml(xml, '//project/version')
  String version = xpath.evaluate('//project/version', records)
  if ('' == version) {
    version = xpath.evaluate('//project/parent/version', records)
  }
  if ('${revision}' == version) {
    version = xpath.evaluate('//project/properties/revision', records)
  }
  return version
}

static ArrayList<String> exec(String[] env, File path, String execcmd, String[] subcmds) {
  Charset encoding = StandardCharsets.UTF_8;
  def outStream = new ByteArrayOutputStream()
  def errStream = new ByteArrayOutputStream()
  def proc = execcmd.execute(env, path)
  def inStream = proc.outputStream

  subcmds.each {cm ->
    inStream.write((cm + '\n').getBytes(encoding))
    inStream.flush()
  }

  inStream.write('exit\n'.getBytes(encoding))
  inStream.flush()
  proc.consumeProcessOutput(outStream, errStream)
  proc.waitFor()
  return [new String(outStream.toByteArray(), encoding), new String(errStream.toByteArray(), encoding)]
}

static void bash(File path, String[] subcmds) {
  def out = exec(null, path, "/usr/bin/bash", subcmds);
  println "OUT:\n" + out[0]
  println "ERR:\n" + out[1]
}

void bash(String[] subcmds) {
  bash(this.getBasedir(), subcmds)
}

//def userProperties = context.get('userProperties')
//def server = new MockServer()
//userProperties.put('serverHost', server.getHost())
//userProperties.put('serverPort', server.getPort())

//File scriptDirFile = new File(getClass().protectionDomain.codeSource.location.path).getParentFile().getAbsoluteFile();
//def workPath = scriptDirFile.toPath()
