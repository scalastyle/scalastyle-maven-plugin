try {

assert new File(basedir, "build.log").readLines().grep(~/.*warning.*Foobar.scala message=File length exceeds.*/).size() == 1

def output = new File(basedir, 'scalastyle-output.xml')

assert output.exists()

def contents = output.getText("UTF-16")
def firstLine = contents.split("[\n\r]+")[0]
assert firstLine == "<?xml version=\"1.0\" encoding=\"UTF-16\"?>"

return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}
