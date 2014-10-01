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
    (f.call(@, dataO, elem) for f in functions)
    
    dataO(@dataInit)
)
