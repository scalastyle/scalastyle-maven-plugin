try {

def file = new File(basedir, 'target')
assert !file.exists()

assert new File(basedir, "build.log").readLines().grep(~/.*warning.*Foobar2.scala message=File length exceeds.*/).size() == 1

return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}
