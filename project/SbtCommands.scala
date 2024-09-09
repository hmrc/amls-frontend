import sbt.Command

object SbtCommands {
  val runTestOnlyCommand: Command = Command.command("runTestOnly") { state =>
    state.globalLogging.full.info("running play using 'testOnlyDoNotUseInAppConf' routes...")
    s"""set javaOptions += "-Dplay.http.router=testOnlyDoNotUseInAppConf.Routes"""" ::
      "run" ::
      s"""set javaOptions -= "-Dplay.http.router=testOnlyDoNotUseInAppConf.Routes"""" ::
      state
  }

  val commands: Seq[Command] = Seq(
    runTestOnlyCommand
  )
}
