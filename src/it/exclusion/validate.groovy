try {

def file = new File(basedir, 'target')
assert !file.exists()

assert new File(basedir, "build.log").readLines().grep(~/.*excluded.*src\/it\/testsrc2\/Foobar2.scala.*/).size() == 1
assert new File(basedir, "build.log").readLines().grep(~/.*Excluded 1 file.*/).size() == 1

return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}
