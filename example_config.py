import logging

LOG_LEVEL = logging.DEBUG

# Database

DB_USERNAME = "postgres"
DB_PASSWORD = None
DB_HOSTNAME = "localhost"
DB_NAME = "puyodb"

# Discord

DEBUG = False
TOKEN = None  # Set this
TEST_GUILDS = (
    None  # Set this to a list of guild IDs for testing the bot's slash commands
)
