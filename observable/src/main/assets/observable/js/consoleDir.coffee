define([
    'observable'
    'knockout'
    'd3'
], (Observable, ko, d3, tj) ->
  () ->
    console.log(">> This")
    console.dir(@)
    console.log(">> Arguments")
    console.dir(arguments)
)
