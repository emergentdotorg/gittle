def userProperties = context.get('userProperties')
def server = new MockServer()
userProperties.put('serverHost', server.getHost())
userProperties.put('serverPort', server.getPort())

//def sout = new StringBuilder(), serr = new StringBuilder()
//def proc = 'ls /badDir'.execute()

//envVars = ["P4PORT=p4server:2222", "P4USER=user", "P4PASSWD=pass", "P4CLIENT=p4workspace"];

envVars = [];
workDir = new File(".");
//cmd = "bash -c \"p4 change -o 1234\"";

//initcmd = "git init .";
//cmd3 = "git commit --allow-empty -m $message"


def sout = new StringBuilder(), serr = new StringBuilder()
def proc = "git init .".execute(envVars, workDir);
proc.consumeProcessOutput(sout, serr)
proc.waitForOrKill(1000)
println "out> $sout\nerr> $serr"
envVars = ["P4PORT=p4server:2222", "P4USER=user", "P4PASSWD=pass", "P4CLIENT=p4workspace"];
