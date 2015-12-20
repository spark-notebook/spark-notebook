define([
    'jquery'
    'observable'
    'knockout'
    'd3'
    'dimple'
], ($, Observable, ko, d3, dimple) ->
  (dataO, container, options) ->
    ulId = @genId
    $('#ul'+ulId+' a').click( (e) ->
      e.preventDefault()
      e.stopImmediatePropagation()

      $('#tab'+ulId+' div.active').removeClass('active')
      $('#ul'+ulId+' li.active').removeClass('active')
      id = $(@).attr('href')
      $(id).addClass('active')
      $(@).parent().addClass('active')
    )

    # select the first tab
    $('#ul'+ulId+' li:first a').click()

    dataO.subscribe( (newData) =>
      #notify tabs
    )
)