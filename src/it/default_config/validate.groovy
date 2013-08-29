try {

def file = new File(basedir, 'target')
assert !file.exists()

assert new File(basedir, "build.log").readLines().grep(~/.*warning.*Header does not match expected text.*/).size() == 1

return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}
