# SimRa Android App (+SimRa-Nav)

This repository is a fork of the SimRa Android app, and contains additional modifications to support
routing and navigation:

- A new route calculation view where users can search for locations and calculate routes between
  them. It also displays the calculated route in different color schemes, representing surface
  quality/road safety of the segments.
- A turn by turn navigation system which is shown in the main screen, providing users with
  navigation instructions depending on their location throughout the calculated route.
- An itinerary containing all navigation steps, available when you click

## Screenshots

<p float="left">
  <img src="/screenshots/routing_detail_safety.jpg" width="200" />
  <img src="/screenshots/main_nav_running.jpg" width="200" /> 
  <img src="/screenshots/nav_instructions.jpg" width="200" />
</p>

## New adjustments for developers

Since the nav component uses a new endpoint, you need to add this line to your `local.properties`
file:

```
NAVIGATION_ENDPOINT="http://your-ip-here/"
```

The slash without the specific endpoint at the end and the http are important, as the current
configuration uses http and adds the `routing/route` REST resource by itself.

After you've added the property, build your application ("Make project" in Android Studio) and run
the app.

### New classes

We have added a set of classes that are responsible for navigation and routing:

- `NavigationActivity.java` - Main Routing activity, used for selection start and destination
  locations, calculating route
- `SimraNavService.java` - Service responsible for sending requests to routing backend and parsing
  responses
- `RoadUtil.java` - Custom utilities for navigation and UI drawing regarding calculated routes
- `SimraRoad.java` - Extension of the `Road` class with additional properties representing safety
  and surface quality scores
- `ScoreColorList.java` - Class for mapping scores along the route to specific colors
- `AddressPair.java` - Object containing an address string and its corresponding coordinates (
  GeoPoint)
- `AutocompleteAdapter.java` - Adapter to display suggestions when a user enters a location in the
  search box

### New layout files and icons

To support the new UI elements, we have added a set of corresponding layout files and icons:

- `nav_instructions_list.xml` - popup window for showing the full list of navigation instructions.
  used in Main activity
- `row_navigation.xml` - row representing a navigation instruction
- `activity_navigation.xml` - corresponding actvitiy layout of `NavigationActivity.java`
- `menu_route_visualizer_options.xml` - menu for showing different route visualization options (
  default, safety, surface)
- `ic_marker.xml`, `ic_circle.xml`, `ic_layers.xml`, `ic_nav.xml` - icons to show the route. circle
  for instruction nodes, markers for start/destination/via, `ic_nav` to start navigation activity
- `direction_....png` and `direction_icons.xml` - icons for navigation instructions (12 files in
  total)

### Additions to existing classes

- Main Activity: new button to navigate, long tap menu to set location as start/destination,
  displaying turn-by-turn navigation instructions with calculated route using location listener,
  launching Navigation activity with intent and awaiting route result
- Preferences (SharedPref, SettingsActivity): Weighting sliders for surface and safety scores,
  custom preferences regarding navigation such as recording with navigation start
- `IOutils.java` - methods for reading and stored previously queried routes

### Extension of SimRa profile

This fork also provides an extension to the SimRa user profile when uploading data. It can be found
under the git branch `profile_extension`.

The branch named `profile_extension` contains 3 new columns in the .csv file:

safetyWeighting - integer representing the user preference for the safety score weighting
surfaceWeighting - integer representing the user preference for the surface quality weighting
usesSimraSurface - boolean representing the user preference for using SimRa surface quality data

### Extension of Metadata

Metadata entries now also contain `usedNav` as a new column, representing whether navigation was
used for a recorded ride.

## Used libraries and images

No additional libraries were used for this extension. The routing instruction icons were made by
Mapbox and can be [found here](https://github.com/mapbox/directions-icons). (CC0 1.0 Universal/No
copyright)
All other newly added icons are made by [Google](https://github.com/google/material-design-icons). (
Apache License 2.0
)

## Related projects for SimRa-Nav:

- [SimRa Android App (+SimRa-Nav)](https://github.com/justdeko/simra-android): The SimRa app for
  Android with route calculation and navigation extensions. (This repository)
- [SimRa Navigation Backend](https://github.com/justdeko/simra_nav_backend): Main repository
  containing backend infrastructure and evaluation results
- [SimRa-GraphHopper](https://github.com/justdeko/graphhopper): Fork of GraphHopper engine to
  support crowdsourced scores.
- [simra-surface-analysis](https://github.com/justdeko/simra-surface-analysis)
  and [SimRaKit](https://github.com/justdeko/SimRaKit): surface analysis repositories to aggregate
  ride data

## Simra Android

[![CodeFactor](https://www.codefactor.io/repository/github/simra-project/simra-android/badge)](https://www.codefactor.io/repository/github/simra-project/simra-android)

This project is part of the SimRa research project which includes the following subprojects:

- [simra-android](https://github.com/simra-project/simra-android/): The SimRa app for Android.
- [simra-ios](https://github.com/simra-project/simra-ios): The SimRa app for iOS.
- [backend](https://github.com/simra-project/backend): The SimRa backend software.
- [dataset](https://github.com/simra-project/dataset): Result data from the SimRa project.
- [screenshots](https://github.com/simra-project/screenshots): Screenshots of both the iOS and
  Android app.
- [SimRa-Visualization](https://github.com/simra-project/SimRa-Visualization): Web application for
  visualizing the dataset.

In this project, we collect – with a strong focus on data protection and privacy – data on such near
crashes to identify when and where bicyclists are especially at risk. We also aim to identify the
main routes of bicycle traffic in Berlin. To obtain such data, we have developed a smartphone app
that uses GPS information to track routes of bicyclists and the built-in acceleration sensors to
pre-categorize near crashes. After their trip, users are asked to annotate and upload the collected
data, pseudonymized per trip. For more information
see [our website](https://www.digital-future.berlin/en/research/projects/simra/).

## Instructions

The suffix used in the `clientHash` to protect the upload is not part of the source code. To compile
the project:

- copy the file `Hash-Suffix.h.sample` to `Hash-Suffix.h`
- replace the sample suffix `mcc_simra` with the suffix provided from the backend operator
- compile
