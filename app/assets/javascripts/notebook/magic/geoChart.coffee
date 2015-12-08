define([
    'observable'
    'knockout'
    'jquery'
    'leaflet'
    'underscore'
    'LeafletMousePosition'
    'proj4'
    'proj4leaflet'
    'epsg'
], (Observable, ko, $, L, _, loadMousePosition, proj4, proj4leaflet, epsg) ->
  (dataO, container, options) ->
    w = options.width||600
    h = options.height||400

    mapId = "map"+@genId
    mapDiv = $(container).append("div").css("width", w+"px").css("height", h+"px").attr("id", mapId)

    map = L.map(mapId, {
      crs:L.CRS.EPSG3857 # OSM stores in epsg4326, but serves in 3857 -_-
    })

    OpenStreetMap_Mapnik = L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    })

    map.addLayer(OpenStreetMap_Mapnik)

    L.control.mousePosition().addTo(map) if L.control.mousePosition

    updateData = (data) =>
      if _.isEmpty(data)
        return

      geoms = _.map(data, (o) -> o[options.geometry])

      geojsonMarkerOptions = {
          radius: 8,
          fillColor: "#ff7800",
          color: "#000",
          weight: 1,
          opacity: 1,
          fillOpacity: 0.8
      }
      geojson = L.geoJson(geoms)

      bounds = geojson.getBounds()

      map.addLayer(geojson)

      map.fitBounds(bounds)

    updateData(@dataInit)

    dataO.subscribe( (newData) =>
      _.each(map._layers, (l) ->
        map.removeLayer(l) if l && l != OpenStreetMap_Mapnik
      )
      updateData(newData)
    )
)