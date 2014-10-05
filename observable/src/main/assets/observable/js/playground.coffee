define([
    'observable'
    'knockout'
], (Observable, ko) ->
  (elem) ->
    l = (a for a in arguments)
    l.shift() # this shifts `elem`
    functions = l

    dataO = Observable.makeObservableArray(@dataId)
    # dataO.subscribe( (data) =>
    #   onData(data, svg, m)
    # )
    for f in functions
      (f.f || f).call(@, dataO, elem, f.o) 

    #dataO(@dataInit)
)
