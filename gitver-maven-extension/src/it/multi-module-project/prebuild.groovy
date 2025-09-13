import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path

// https://maven.apache.org/plugins/maven-invoker-plugin/examples/pre-post-build-script.html
// basedir is a predefined global File for the root of the test project
if (!binding.hasVariable('basedir')) {
  throw new IllegalStateException("basedir was undefined!")
}
File dirbase = basedir;
Path basepath = dirbase.toPath().toAbsolutePath();

//def userProperties = context.get('userProperties')
//def server = new MockServer()
//userProperties.put('serverHost', server.getHost())
//userProperties.put('serverPort', server.getPort())

//File scriptDirFile = new File(getClass().protectionDomain.codeSource.location.path).getParentFile().getAbsoluteFile();
//def workPath = scriptDirFile.toPath()

static ArrayList<String> exec(String[] env, File path, String execcmd, String[] subcmds) {
  Charset encoding = StandardCharsets.UTF_8;
  def outStream = new ByteArrayOutputStream()
  def errStream = new ByteArrayOutputStream()
  def proc = execcmd.execute(env, path)
  def inStream = proc.outputStream

  subcmds.each { cm ->
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
  def out = exec(null, path, "/bin/bash", subcmds);
  println "OUT:\n" + out[0]
  println "ERR:\n" + out[1]
}

def keyword = "[major]"

bash(dirbase, [
  "echo PWD=\${PWD}",
  "git init --initial-branch main .",
  "git commit --allow-empty -m \'chore(release): $keyword\'"
] as String[])
