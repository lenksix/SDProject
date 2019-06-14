# SDProject
This repository is the implementation of a distributed video streaming services. 
It was developed for the Distributed Sytstems course at Unipd A.Y. 2018/19.

## Description
This project implements a Distributed service of video streaming, inspired from Youtube, Twitch and Netflix.
It was designed for implementing several functionalities. For know it manages the delivery of a video from the database servers
to the frontend side. It employs services as Cassandra, PostgreSQL and java RMI. 

## Installation
Several depencesied are required. The simpliest way to install the application is the following one.
1. Get [cassandra portable](http://cassandra.apache.org/download/).
1. Get [PostgresSQL](https://www.postgresql.org/download/) and configure it on your operating system.
1. Install the last version of [eclipse](https://www.eclipse.org/downloads/) and configure it with the [JDK 1.8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html). 
1. As last thing install [VLC](https://www.videolan.org/vlc/index.it.html) on your device.
Now you are ready to execute the application.

## Usage 
In order to use the application you will need to respect the following procedure.
1. Check that Postrgres and cassandra are running on your machine e.g. if not you can start cassandra with 
```bash
cassandra -f
```
1. Import the SDProject on eclipse. It may take a while to configure the workspace since a lot of packages are needed.
1. Check if *vlc* and *cvlc* are available from your bash/shell command line.

Once these steps are performing you just need to run the classes in the right order.
1. InitDb
1. PopulateDB
1. RDBManager
1. SessionManagerImpl 
1. CacheServicesThread
1. ManageDb
1. ServerL2
1. ManageCacheList
1. ProxyFrontend
1. ClientGui

Alternatively you may want to create a launch group on eclipse respecting the order above, so with just one run you will be able to run all those classes
