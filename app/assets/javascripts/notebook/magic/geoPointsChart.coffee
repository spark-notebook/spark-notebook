define([
    'observable'
    'knockout'
    'jquery'
    'leaflet'
    'underscore'
    'LeafletMousePosition'
], (Observable, ko, $, L, _, loadMousePosition) ->
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

    updatePoints = (data) =>
      if _.isEmpty(data)
        return
      points = _.map(data, (o) -> new L.LatLng(o[options.lat], o[options.lon]))
      bounds = new L.latLngBounds(points[0])
      _.each(points, (p) -> bounds.extend(p))


      markers = if options.r || options.color
        _.map(_.zip(data, points), (pair) ->
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

      _.each(_.zip(data, markers), (om) ->
        [o, m] = om
        m.bindPopup(JSON.stringify(o))
        m.addTo(map)
      )

      map.fitBounds(bounds)

    updatePoints(@dataInit)

    dataO.subscribe( (newData) =>
      _.each(map._layers, (l) ->
        map.removeLayer(l) if l instanceof L.Marker
      )
      updatePoints(newData)
    )
)