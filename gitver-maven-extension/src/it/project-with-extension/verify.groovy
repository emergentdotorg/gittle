import java.nio.file.Files
import java.nio.file.Paths

def gitDotDir = Paths.get(".").resolve(".git" )
assert Files.exists(gitDotDir)
