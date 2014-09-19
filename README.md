# README #

Android Indexing Service

(C) 2014 Dracode Software

Version: 0.5.0

Authors: Benjamin Winger

An android application that exposes a service to allow other applications to search quickly search through files on a device using the Apache Lucene search framework. This means they do not have to implement Lucene themselves or deal with indexing and also reduces application size as implementing the AIS Client Library is much smaller than implementing Apache Lucene (30KB vs ~2.2MB or more). 
It also serves as a centralised index so that the database is shared between applications and reduces redundancy from having many applications indexing individually.

## Compiling ##
Android Indexing Service is built using the Apache Maven build tool. Simply download the source and execute

```
mvn clean install
```
from inside the source directory.

Optionally, you can use

```
mvn clean build android:deploy
```
to build and install the apk directly to a device for testing.

## Current Features ##
The current version of Android Indexing service includes the following features:

* An active indexer that creates index entries for every file on the external storage directory of the device
* A passive indexer that updates the index as files are changed
* A search service that allows for boolean searches and (experimental) fuzzy searches
* A preferences screen that allows you to enable or disable indexing

## Known Issues ##

* The user interface is almost non-existant
* There is no way to customize anything using the UI
* The service has been tested on a very limited number of devices, there are no guarantees that it will work properly on every device

### Contact ###
For more information, contact Benjamin Winger (winger.benjamin@gmail.com).
