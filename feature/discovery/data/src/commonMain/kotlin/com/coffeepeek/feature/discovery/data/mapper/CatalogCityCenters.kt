package com.coffeepeek.feature.discovery.data.mapper

import com.coffeepeek.feature.discovery.domain.model.Coordinates

// The city catalog exposes slugs and names. Centers are from GeoNames:
// https://www.geonames.org/BY/largest-cities-in-belarus.html and https://www.geonames.org/524901/
internal val catalogCityCenters = mapOf(
    "minsk" to Coordinates(53.900, 27.567),
    "brest" to Coordinates(52.109, 23.717),
    "grodno" to Coordinates(53.676, 23.829),
    "grodna" to Coordinates(53.676, 23.829),
    "gomel" to Coordinates(52.434, 30.975),
    "homel" to Coordinates(52.434, 30.975),
    "vitebsk" to Coordinates(55.190, 30.205),
    "vitsebsk" to Coordinates(55.190, 30.205),
    "mogilev" to Coordinates(53.909, 30.340),
    "mogilyov" to Coordinates(53.909, 30.340),
    "magilyou" to Coordinates(53.909, 30.340),
    "moscow" to Coordinates(55.752, 37.618),
    "moskva" to Coordinates(55.752, 37.618),
)
