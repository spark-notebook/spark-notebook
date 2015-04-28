define([
  'jquery'
  'knockout'
  'underscore'
], ($, ko, _) ->
  viewModel = {
    tachyonPaths: ko.observableArray([
                    { path: '/' },
                    { path: '/A/' },
                    { path: '/A/B/' },
                    { path: '/A/B/C/' }
                  ])
  }
  ko.applyBindings(viewModel)
  tachyonJsRoutes.controllers.TachyonProxy.ls("/").ajax(
    success: (data) ->
      console.log("init tachyon", data)
      newData = _.map(data, (p) => {path: p})
      console.log("tachyon data", newData)
      viewModel.tachyonPaths(newData)
    error: (error) ->
      console.error("Cannot load tachyon data: " + error)
  )
);