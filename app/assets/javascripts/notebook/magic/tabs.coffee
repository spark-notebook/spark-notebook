define([
    'jquery'
    'observable'
    'knockout'
    'd3'
    'dimple'
], ($, Observable, ko, d3, dimple) ->
  (dataO, container, options) ->
    ulId = @genId

    get_cell = () ->
      # get cell object, like done in get_cell_elements in notebook.js
      cell_dom_element = $(container).parents(".cell").not('.cell .cell')[0]
      $(cell_dom_element).data("cell")

    get_saved_state = () ->
      saved_state = get_cell().metadata.presentation?.tabs_state
      JSON.parse(saved_state || "{}") || {}

    save_state = (state) ->
      cell = get_cell()
      cell.metadata.presentation = {} if not cell.metadata.presentation
      cell.metadata.presentation.tabs_state = JSON.stringify(state, undefined, 2)

    $('#ul'+ulId+' a').click( (e) ->
      e.preventDefault()
      e.stopImmediatePropagation()

      $('#tab'+ulId+' div.active').removeClass('active')
      $('#ul'+ulId+' li.active').removeClass('active')
      id = $(@).attr('href')
      $(id).addClass('active')
      $(@).parent().addClass('active')

      # console.log("tab id:", id)
      save_state({tab_id: id})
    )

    saved_tab_id = get_saved_state().tab_id
    if saved_tab_id && $('a[href=' + saved_tab_id + ']').length > 0
      # console.log("selecting a saved tab", saved_tab_id)
      $('a[href=' + saved_tab_id + ']').click()
    else
      # select the first tab
      $('#ul'+ulId+' li:first a').click()

    dataO.subscribe( (newData) =>
      #notify tabs
    )
)