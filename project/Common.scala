import sbt.Keys._
import sbt.URL

object Common {
  def apply() = Seq(
    version := "0.1",
    scalaVersion := "2.12.8",
    organizationName := "Engitano",
    startYear := Some(2019),
    licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
  )
}
