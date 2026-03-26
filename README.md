🌟 Zvezdo4et Telegram Bot

An assistant bot for managing a library of physical exercises.

Its main feature is generating a random set of exercises based on the number of "games" played, making it an excellent tool for gamifying workouts.


🚀 Basic functions

Exercise Library: view the full list of available activities.

Content Management: add, edit, and delete exercises (password protected).

Workout Generator: the bot takes the number of matches lost and generates a random list of exercises with a random number of repetitions.

JSON Storage: no heavy database setup required (PostgreSQL/MySQL) - everything is stored in the local files. Uses separate files for different languages (exercises_ru.json, exercises_en.json), allowing easy expansion to new locales.


💡 Key features

High Performance: in-memory caching for lightning-fast exercise retrieval and reduced disk I/O. 

Multi-language Support: full localization for UI and exercise databases (Russian and English supported out of the box).


🛠 Tech Stack

Java 17+

Spring Boot: the basis of the application.

Telegram Bots Spring Boot Starter: for interacting with the Telegram API.

Jackson: for working with JSON files.

Maven: building the project.


📋 Bot commands

Command  -  Descriprion

/start - Launch the bot and greet it

/help - List all available commands

/getAll - Show all exercises in the database

/addExr - Add a new exercise (password required)

/editExr - Change the name or description of an existing exercise (password required)

/delExr - Delete an exercise by ID (password required)

/getExr - Get a random workout based on the number of defeats

/set_ru - Изменить язык на русский

/set_en - Change language to English


📂 Project structure

- 📂 service/ - core logic, caching, and Telegram API interaction.

- 📂 model/ - data structures for Exercises and Language settings.

- 📂 resources/messages_*.properties - all UI strings for easy translation.


⚙️ Installation

1) Clone the project into your folder

2) In src/main/resources, rename "src/main/resources/application.properties.example" to "application.properties"

3) In the "application.properties" file, paste your token and bot name

4) Compile the project

5) Done! Your bot will have full functionality when launched
