name := "log-process"
version := "0.1"
scalaVersion := "2.13.0"

lazy val `log-common` = RootProject(file("../log-common"))
lazy val `log-process` = (project in file("."))
  .dependsOn(`log-common`)

lazy val runFixed = taskKey[Unit]("A task that hard codes the values to `run`")
runFixed := {
  val _ = (run in Compile).toTask(" --searchdir D:\\tmp3 --outputdir D:\\tmp4").value
  println("Done!")
}
