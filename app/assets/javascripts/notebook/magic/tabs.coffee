define([
    'observable'
    'knockout'
    'd3'
    'dimple'
], (Observable, ko, d3, dimple) ->
  (dataO, container, options) ->
    ulId = @genId
    $('#ul'+ulId+' a').click( () ->
      $('#tab'+ulId+' div.active').removeClass('active')
      $('#ul'+ulId+' li.active').removeClass('active')
      id = $(@).attr('href')
      $(id).addClass('active')
      $(@).parent().addClass('active')
    )

    dataO.subscribe( (newData) =>
      #notify tabs
    )
)