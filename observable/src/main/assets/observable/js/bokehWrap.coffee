define([
    'observable'
    'knockout'
    #bokehjs
], (Observable, ko) ->
  (dataO, container, options) ->
    #Bokeh.set_log_level("${resources.logLevel.name}")
    spec = @dataInit[0]
    models = ${spec.models}
    modelid = "${spec.modelId}"
    modeltype = "${spec.modelType}"
    elementid = "#${spec.elementId}"
    
    Bokeh.logger.info("Realizing plot:")
    Bokeh.logger.info(" - modeltype: " + modeltype)
    Bokeh.logger.info(" - modelid:   " + modelid)
    #Bokeh.logger.info(" - elementid: " + elementid)
    Bokeh.load_models(models)
    
    model = Bokeh.Collections(modeltype).get(modelid)
    view = new model.default_view({model: model, el: container}) #elementid})

    dataO.subscribe( (data) =>
      model.set('data', data[0])
      model.trigger('change', model, {})
    )
)