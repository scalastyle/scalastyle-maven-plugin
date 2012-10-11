try {

assert new File(basedir, "build.log").readLines().grep(~/.*warning.*Foobar.scala message=File length exceeds.*/).size() == 1

assert new File(basedir, 'scalastyle-output.xml').exists()

return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}
