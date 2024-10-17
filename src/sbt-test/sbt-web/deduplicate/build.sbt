lazy val root = (project in file(".")).enablePlugins(SbtWeb)

Assets / WebKeys.deduplicators += SbtWeb.selectFileFrom((Assets / sourceDirectory).value)
