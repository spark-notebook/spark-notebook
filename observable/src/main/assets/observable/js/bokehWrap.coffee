define([
    'observable'
    'knockout'
    #bokehjs
], (Observable, ko) ->
  (dataO, container, options) ->
    context = @dataInit[0]

    Bokeh.load_models(context.models)

    model = Bokeh.Collections(context.modelType).get(context.modelId)
    view = new model.default_view({model: model, el: container})

    dataO.subscribe( (data) =>
      view.closall()

      context = data[0]

      Bokeh.load_models(context.models)
      model = Bokeh.Collections(context.modelType).get(context.modelId)
      view = new model.default_view({model: model, el: container})
    )
)