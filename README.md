# Dongurigaeru Bot: Ranked matchmaking for Puyo Puyo over Discord

## Specifications and plans

### General stuff
- Split code into multiple files to prevent them from getting unwieldy
- Give everything their own logger

### Discord stuff
- Proper command error handling
- Custom help command

### Ranked systems
- Player object
    - A reference to a Discord user with additional information, such as display name and Glicko-2 rating information
    - This reference could either be their ID or a fetched `discord.User`
    - Methods for setting working rating information and moving the working ratings into the final ratings
    - Players queried from the database are stored in a cache for 2-3 hours to prevent having to query data from the database every time
- Match object
    - References two Players and their scores
    - Records states such as if the match is in progress or completed
    - Stored in a RatingPeriod
    - Has various data integrity checks so things don't become inconsistent (e.g. match states)
    - Created by the Matchmaker, tracked and managed by the MatchHandler
    - In progress matches are stored in the database too
- RatingPeriod object
    - Holds completed matches and the unique Players from those matches in a dictionary of Players to a set of the matches they played in
    - Used by the Glicko-2 rating calculator to set new ratings
    - Start time is set in the config, end time is the start time + the length * the ID of the rating period, starting from 1
    - When the rating period ends, it sends all of its matches and participants to the Glicko-2 rating calculator
- Glicko2 object
    - Takes a RatingPeriod's information to calculate new ratings for every Player in the database
    - A Player that isn't in the RatingPeriod is considered a non-participant and only calculates a new RD
    - After calculating new ratings, update all Player ratings in the database accordingly
- Matchmaker object
    - Creates Players and adds them to a queue by platform to find a match
    - Dynamically matches Players through a tolerance system
    - Tolerance system takes in account the time spent in queue, rating information, previous matches to prevent rematches
    - Tracks match completion
    - Sends completed matches to the RatingPeriod
    - Saves in progress matches to the database
    - Reminds users that they're in the queue every 5 minutes or so
    - Caches queried Players for quick access

### Database
- Using SQLAlchemy and Alembic
- Tables for guild settings, Players, Matches, and RatingPeriods

### Tournaments
tbd

### Puyo Puyo utilities
- Create chains in Discord using emojis
    - Can be animated
    - Takes multiple formats for chains, like puyo.gg chains or a matrix
- Built-in wiki
  - Showcases forms and how to use them
  - Has lessons and guides
