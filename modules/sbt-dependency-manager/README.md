Helpers to manage, resolve and download dependencies.

This is mainly used to download dependencies declared by a Notebook. They will then be stored locally besides the project in order to create an über `jar` or a `docker` for instance.

To compile only this project, for both scala versions:

        $ sbt "project sbt-dependency-manager" "+compile"

To Run tests only for this project for both scala versions (`+`),
and with incremental recompilation (`~`) on code change, use this:

        $ sbt "project sbt-dependency-manager" "set offline := true" "~ +test"
