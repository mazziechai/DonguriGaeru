from datetime import datetime, timedelta, timezone

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from sqlalchemy_utils import create_database, drop_database

from config import LOCAL_SQL_HOSTNAME, LOCAL_SQL_PASSWORD, LOCAL_SQL_USERNAME
from donguri_gaeru.database import Base, Match, Player


@pytest.fixture(scope="module")
def dbengine():
    sql = "postgresql://{username}:{password}@{hostname}/{database}"
    url = sql.format(
        username=LOCAL_SQL_USERNAME,
        password=LOCAL_SQL_PASSWORD,
        hostname=LOCAL_SQL_HOSTNAME,
        database="test_database",
    )
    create_database(url)
    engine = create_engine(url)

    yield engine

    engine.dispose()
    drop_database(url)


@pytest.fixture(scope="function")
def dbsession(dbengine):
    Base.metadata.create_all(dbengine)
    with dbengine.connect() as connection:
        with Session(bind=connection) as session:
            yield session

        Base.metadata.drop_all(dbengine)


def test_player(dbsession):
    # A player cannot have an empty string for a name.
    with pytest.raises(AssertionError):
        dbsession.add(Player(name=""))

    # A player cannot have a name consisting of only numbers.
    with pytest.raises(AssertionError):
        dbsession.add(Player(name="12345"))

    # If a discord name is specified, it must match the pattern.
    with pytest.raises(AssertionError):
        dbsession.add(Player(name="test_player", discord=""))

    # If a created date is specified, it must be in the past.
    with pytest.raises(AssertionError):
        future = datetime.now(timezone.utc) + timedelta(days=1)
        dbsession.add(Player(name="test_player", created=future))

    # Add players to the database and verify correctness.
    test_playerA = {"name": "test_playerA", "created": datetime.now(timezone.utc)}
    test_playerB = {"name": "test_playerB", "discord": "discordname#0123"}
    test_players = [test_playerA, test_playerB]
    dbsession.add_all([Player(**test_player) for test_player in test_players])

    assert dbsession.query(Player).count() == len(test_players)

    for result, expect in zip(dbsession.query(Player), test_players):
        assert result.name == expect["name"]
        assert result.discord == expect.get("discord", None)
        assert result.created < datetime.now(timezone.utc)


def test_match(dbsession):
    test_playerA = Player(name="test_playerA")
    test_playerB = Player(name="test_playerB")
    dbsession.add_all([test_playerA, test_playerB])
    dbsession.commit()

    default_kwargs = {
        "playerA_id": test_playerA.id,
        "playerB_id": test_playerB.id,
        "scoreA": 0,
        "scoreB": 0,
    }
    test_match = lambda **kwargs: Match(**dict(default_kwargs, **kwargs))  # noqa E731

    # The match opponents may not be the same player.
    with pytest.raises(AssertionError):
        dbsession.add(test_match(playerB_id=test_playerA.id))

    # A match score must be non-negative.
    with pytest.raises(AssertionError):
        dbsession.add(test_match(scoreA=-1))

    # If a created date is specified, it must be in the past.
    with pytest.raises(AssertionError):
        future = datetime.now(timezone.utc) + timedelta(days=1)
        dbsession.add(test_match(created=future))

    # Add a match and verify default initialization.
    test_match = test_match(scoreA=4, scoreB=6)
    dbsession.add(test_match)
    dbsession.commit()

    assert test_match.games == 10
    assert test_match.active
    assert not test_match.handshake
    assert test_playerA.matches[0] == test_match
    assert test_playerB.matches[0] == test_match

    # Demonstrate active and handshake status changing.
    test_match.active = False
    test_match.handshakeA = True
    dbsession.commit()
    assert not dbsession.query(Match).first().active
    assert not dbsession.query(Match).first().handshake

    test_match.handshakeB = True
    dbsession.commit()
    assert dbsession.query(Match).first().handshake
