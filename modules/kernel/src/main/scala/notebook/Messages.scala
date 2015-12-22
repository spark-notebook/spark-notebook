package notebook.client

import notebook.util.Match

sealed trait CalcRequest

case class ExecuteRequest(cellId:String, counter: Int, code: String) extends CalcRequest

case class CompletionRequest(line: String, cursorPosition: Int) extends CalcRequest

case class ObjectInfoRequest(objName: String, position: Int) extends CalcRequest

case object InterruptRequest extends CalcRequest

case class InterruptCellRequest(cellId: String) extends CalcRequest

sealed trait CalcResponse

case class StreamResponse(data: String, name: String) extends CalcResponse

case class ExecuteResponse(outputType:String, content: String, time:String) extends CalcResponse

case class ErrorResponse(message: String, incomplete: Boolean) extends CalcResponse

// CY: With high probability, the matchedText field is the segment of the input line that could
// be sensibly replaced with (any of) the candidate.
// i.e.
//
// input: "abc".inst
// ^
// the completions would come back as List("instanceOf") and matchedText => "inst"
//
// ...maybe...
case class CompletionResponse(cursorPosition: Int, candidates: Seq[Match], matchedText: String)

/*
name
call_def
init_definition
definition
call_docstring
init_docstring
docstring
*/
case class ObjectInfoResponse(found: Boolean, name: String, callDef: String, callDocString: String)
