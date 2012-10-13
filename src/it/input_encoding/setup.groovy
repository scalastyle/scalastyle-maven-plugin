try {

def file = new File(basedir, 'src')
if (!file.exists()) file.mkdir()

assert file.exists()

def src = new File(file, 'Foobar.scala')
src.write("""package org.scalastyle.maven.testsrc;

class Foobar {

}""", "UTF-16");

return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}
