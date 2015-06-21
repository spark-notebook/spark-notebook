define([
    'observable'
    'knockout'
    'jquery'
    'leaflet'
    'underscore'
], (Observable, ko, $, L, _) ->
  (dataO, container, options) ->
    w = options.width||600
    h = options.height||400

    mapId = "map"+@genId
    mapDiv = $(container).append("div").css("width", w+"px").css("height", h+"px").attr("id", mapId)

    points = _.map(@dataInit, (o) -> new L.LatLng(o[options.lat], o[options.lon]))
    bounds = new L.latLngBounds(points[0])
    _.each(points, (p) -> bounds.extend(p))

    map = L.map(mapId, {
      crs:L.CRS.EPSG4326
    })

    OpenStreetMap_Mapnik = L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    })

    map.addLayer(OpenStreetMap_Mapnik)

    markers = if options.r || options.color
      _.map(_.zip(@dataInit, points), (pair) ->
        o = pair[0]
        p = pair[1]
        e = {}
        e.radius = o[options.r] if options.r
        e.fillColor = o[options.color] if options.color
        L.circleMarker(p, e)
      )
    else
      _.map(points, (o) ->
        L.marker(o)
      )

    _.each(_.zip(@dataInit, markers), (om) ->
      [o, m] = om
      m.bindPopup(JSON.stringify(o))
      m.addTo(map)
    )

    map.fitBounds(bounds)

    dataO.subscribe( (newData) =>
      console.warn("map update todo")
    )
)