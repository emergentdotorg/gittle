import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

static ArrayList<String> exec(String[] env, String path, String execcmd, String[] subcmds) {
  Charset encoding = StandardCharsets.UTF_8;
  def outStream = new ByteArrayOutputStream()
  def errStream = new ByteArrayOutputStream()
  def proc = execcmd.execute(env, new File(path))
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

static void bash(String path, String[] subcmds) {
  def out = exec(null, path, "/bin/bash", subcmds);
  println "OUT:\n" + out[0]
  println "ERR:\n" + out[1]
}

// /target/it/multi-module-projec
//def out = exec(".", "/bin/sh", ["echo \${PWD}\n", "ls", "cd usr", "echo", "ls"] as String[])
//def out = exec(".", "/bin/bash", ["echo \${PWD}"] as String[])

def keyword = "[major]"
bash(".", [
  "git init --initial-branch main .",
  "git commit --allow-empty -m \'chore(release): $keyword\'"
] as String[])
