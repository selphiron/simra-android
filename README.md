# SimRa Android App (+ SimRa-Nav)

[![CodeFactor](https://www.codefactor.io/repository/github/simra-project/simra-android/badge)](https://www.codefactor.io/repository/github/simra-project/simra-android)

This project is part of the SimRa research project which includes the following subprojects:
- [sirma-android](https://github.com/simra-project/simra-android/): The SimRa app for Android.
- [simra-ios](https://github.com/simra-project/simra-ios): The SimRa app for iOS.
- [backend](https://github.com/simra-project/backend): The SimRa backend software.
- [dataset](https://github.com/simra-project/dataset): Result data from the SimRa project.
- [screenshots](https://github.com/simra-project/screenshots): Screenshots of both the iOS and Android app.
- [SimRa-Visualization](https://github.com/simra-project/SimRa-Visualization): Web application for visualizing the dataset.

In this project, we collect – with a strong focus on data protection and privacy – data on such near crashes to identify when and where bicyclists are especially at risk. We also aim to identify the main routes of bicycle traffic in Berlin. To obtain such data, we have developed a smartphone app that uses GPS information to track routes of bicyclists and the built-in acceleration sensors to pre-categorize near crashes. After their trip, users are asked to annotate and upload the collected data, pseudonymized per trip.
For more information see [our website](https://www.digital-future.berlin/en/research/projects/simra/).

## Instructions

The suffix used in the `clientHash` to protect the upload is not part of the source code. To compile
the project:

- copy the file `Hash-Suffix.h.sample` to `Hash-Suffix.h`
- replace the sample suffix `mcc_simra` with the suffix provided from the backend operator
- compile

## SimRa-Nav extensions

This repository is a fork of the SimRa Android app, and contains additional modifications to support
routing and navigation.

### Initial setup

To make your application work with routing, enter the simra-nav API endpoint in `local.properties`:

`NAVIGATION_ENDPOINT="http://custom-simra-routing-endpoint.com/"`

DO add a slash at the end of the url, but do not add `routing/route` at the end, as this is done
within the application.

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

### Additions to existing classes

- Main Activity: new button to navigate, long tap menu to set location as start/destination,
  displaying turn-by-turn navigation instructions
- Preferences: Weighting sliders for surface and safety scores, custom preferences regarding
  navigation

