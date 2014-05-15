lazy val root = (project in file(".")).enablePlugins(SbtWeb)

WebKeys.deduplicators in Assets += SbtWeb.selectFileFrom((sourceDirectory in Assets).value)
