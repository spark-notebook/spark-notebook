
**How it works:**

   - Javascript app sends requests to backend: e.g. load notebook, code commands to execute
   - backend processes and executes the `code`, and sends back the result DIRECTLY to javascript via `observable/websockets`
      * in Backend stuff is passed over as akka actor messages
      * all reactive widgets (e.g. `display(dataFrame)`) work like this: https://github.com/spark-notebook/spark-notebook/wiki/Reactive-outputs----widgets,-renderers,-Rx-and-knockout (this doc might be slightly old)
   - then Javascript app stores/send the notebook file which contain cell results
     * i.e. it's not the backend which stores cell outputs/results into a SNB

**Main interactions (data-flow):**
```
JavaScript [web] <-> Application.scala <-> WebSocketKernelActor (play app) <-> CalcWebSocketService   <- different JVM process -> ReplCalculator <-> Repl
```


**Code structure:**
- `conf/routes` - makes URLs to play Application backend
- `app`: is a Play-app web-backend
- `modules/`  (some sub-projects have a README.md file inside!)
  * `core` -  basic stuff, e.g. for reading/writing SNB files
  * `common` - contains [`widgets`](./modules/common/src/main/scala/notebook/front/widgets) and [`charts`](./modules/common/src/main/scala/notebook/front/widgets/charts) like `display(df)`, `PivotChart()` etc (included by most modules/JVM processes)
  * `kernel` - a REPL which is run a separate JVM process, per each notebook
    * `ReplCalculator` process the commands, and passes them to `Repl` for execution (a patched/improved spark-shell/repl for apache-spark)
  * `observable` - websockets based complex stuff used to pass over *live* messages between backend and web-app
  * `spark` - spark/scala version dependent stuff. contains low-level `Repl` implementations, and some utils
  * `subprocess` - a helper that spins up a separate JVM for REPL
  * `sbt-dependency-manager` - helpers for resolving and downloading custom dependencies
  * `git-notebook-provider`  - git storage support, used only if enabled

**P.S. IntellijIDEA Ultimate helps to orient yourself inside a large `Play! framework` project (e.g. `conf/routes` are navigatable there).**
