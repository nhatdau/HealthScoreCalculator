# HealthScoreCalculator
This project uses Java 8 and Spring Boot to build a console application for calculating health score of projects on GitHub bases on some metrics. In current implementation, this application supports metrics: number of commits, number of contributors, average number of commits per day, average time that an issue remains opened.

## Presiquisites:
Please install Java 8, Maven on your machine.

## How to build:
* Clone source code from this repository.
* Go to source code folder. Execute command: `mvn clean package`
* This builds 1 jar file in folder **target**: `HealthScoreCalculator-0.0.1-SNAPSHOT.jar`

## How to run the application:
* At source code folder. Execute command:
    * `java -jar ./target/HealthScoreCalculator-0.0.1-SNAPSHOT.jar 2019-08-03T00:00:00Z 2019-08-05T00:00:00Z`
    * 2 input parameters are start time, end time (ISO 8601 format) for calculating health score
* Wait for some times for application finishes calculating. It'll have the message: "Finish calculating health score of projects" on console.
* Go to folder has name is timestamp when executed the application. For ex: **2019-09-19T00:28:21.495**. Check the result csv file of calculating in that folder: **results.csv**.

## Technical decisions:
* I use Spring Boot platform to build the console application. As it speeds up my development. It includes required components, libraries for 1 application: logging, assert/validation, managed services (beans), properties file...
* Moreover, I just use apis in Java 8 core: Collections, File handling... to build the solution. The idea is using simple solutions for 1 working application. And I use Jackson for parsing Json data.
* Some things I would improve for the application:
    * Speed up downloading files from GitHub by using some threads for downloading.
    * Change the approach of calculating metrics: might be don't need to populate (store) data of all repositories before calculating, find 1 way to extract required metrics of repository from data files directly.
    * In case populate data of all repositories before calculating, might be we need to find 1 data structure for storing all data is most saving in space. Since if data of repositories is very huge, it can cause "Out Of Memory" error.
    * Apply more metrics for calculating score health of project. 
