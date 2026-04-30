# Gong.io Take Home Exercise

Hey! We are stoked that you are interested in joining engineering team at Gong.io!

We feel that the best place to really show us your skills is somewhere you feel comfortable. This exercise should not take you some crazy amount of time to complete.

This take home exercise is used to determine how you go about solving problems logically, as well as building out simple and high quality clean code.  
The exercise is very open to interpretation, and you will see down below that there are few hard requirements, and we will leave the rest up to you.

### What is the exercise?
You will be creating a simple calendar with one really cool feature: Given a list of people and a desired duration, find all the time slots in a day in which all persons are available to meet.

The input data is provided to you in a simple comma-separated values file (`calendar.csv`) and is structred in the following way:

```
Person name, Event subject, Event start time, Event end time
```   

### Goals

Your goal is to design and create a simple Calendar in Java, and implement the following method:

```java
List<LocalTime> findAvailableSlots(List<String> personList, Duration eventDuration);
``` 

#### Requirements:
- This calendar has only one day. So to make things simple - events have only start and end time (no dates)
- The day starts at 07:00 and ends at 19:00. Take that into consideration when finding available time slots.
- Feel free to go above and beyond if you have ideas for extra features!


### Example
Attached is an example calendar file `calendar.csv`:
```
Alice,"Morning meeting",08:00,09:30
Alice,"Lunch with Jack",13:00,14:00
Alice,"Yoga",16:00,17:00
Jack,"Morning meeting",08:00,08:50
Jack,"Sales call",09:00,09:40
Jack,"Lunch with Alice",13:00,14:00
Jack,"Yoga",16:00,17:00
Bob,"Morning meeting",08:00,09:30
Bob,"Morning meeting 2",09:30,09:40
Bob,"Q3 review",10:00,11:30
Bob,"Lunch and siesta",13:00,15:00
Bob,"Yoga",16:00,17:00
```

For this input, and for a meeting of 60 minutes which Alice & Jack should attend the following output is expected:

```
Available slot: 07:00
Available slot: 10:00
Available slot: 11:00
Available slot: 12:00
Available slot: 14:00
Available slot: 15:00
Available slot: 17:00
Available slot: 18:00
```

### Must Give high Attention to Code Quality:
Please address the following aspects in your code:
- Clean coding, if you're familiar with [SOLID Principals](https://en.wikipedia.org/wiki/SOLID) try to work by it as much as possible.
- Meaningfull naming for your classes and APIs.
- Clear and understood relationship between your classes.
- Object oriented design
- Error handling
- Validations
- logging (System.out.print is good enough as well)
- Testing, Testing Testing
 
### Getting Started

You will need [Maven](https://maven.apache.org/) installed to run the commands below.

You will have to run `mvn clean install` inside the directory to download and install required dependencies.

We have created the application's entry point for you. The entry point file is `src/main/java/io/gong/App.java`.
To execute the app, you can run `mvn compile exec:java`

