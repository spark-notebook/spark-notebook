define([
    'observable'
    'knockout'
    'd3'
], (Observable, ko, d3) ->
  () ->
    console.log(">> This")
    console.dir(@)
    console.log(">> Arguments")
    console.dir(arguments)
)
