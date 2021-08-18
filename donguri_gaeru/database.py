import re
from datetime import datetime, timezone

from sqlalchemy import Boolean, Column, DateTime, ForeignKey, Integer, String
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.ext.hybrid import hybrid_property
from sqlalchemy.orm import relationship, validates
from sqlalchemy.sql import func

Base = declarative_base()


class Player(Base):
    __tablename__ = "players"
    id = Column(Integer, primary_key=True)
    name = Column(String, unique=True, nullable=False)
    discord = Column(String, default=None, unique=True, nullable=True)
    created = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    # TODO: Add username/password when creating Flask application.

    @validates("name")
    def validate_name(self, key, name):
        name = name.strip()
        assert name and not re.match(r"^\d+$", name)
        return name

    @validates("discord")
    def validate_discord(self, key, discord):
        if discord is not None:
            discord = discord.strip()
            assert re.match(r"^.+#\d\d\d\d$", discord)
        return discord

    @validates("created")
    def validate_created(self, key, created):
        assert created <= datetime.now(timezone.utc)
        return created

    @hybrid_property
    def matches(self):
        return list(
            filter(lambda match: match.active, self.matches_asA + self.matches_asB)
        )

    def __repr__(self):
        return (
            "<Player(id={player.id}, "
            "name={player.name}, "
            "discord={player.discord}, "
            "created={player.created})>".format(player=self)
        )


class Match(Base):
    __tablename__ = "matches"
    id = Column(Integer, primary_key=True)
    playerA_id = Column(Integer, ForeignKey("players.id"), nullable=False)
    playerB_id = Column(Integer, ForeignKey("players.id"), nullable=False)
    scoreA = Column(Integer, nullable=False)
    scoreB = Column(Integer, nullable=False)
    handshakeA = Column(Boolean, server_default="false", nullable=False)
    handshakeB = Column(Boolean, server_default="false", nullable=False)
    created = Column(DateTime, server_default=func.now(), nullable=False)
    active = Column(Boolean, server_default="true", nullable=False)

    playerA = relationship("Player", foreign_keys=playerA_id, backref="matches_asA")
    playerB = relationship("Player", foreign_keys=playerB_id, backref="matches_asB")

    @validates("scoreA", "scoreB")
    def validate_score(self, key, score):
        assert score >= 0
        return score

    @validates("playerA_id", "playerB_id")
    def validate_players(self, key, playerid):
        if key == "playerB_id":
            assert playerid != self.playerA_id
        return playerid

    @validates("created")
    def validate_created(self, key, created):
        assert created <= datetime.now(timezone.utc)
        return created

    @hybrid_property
    def games(self):
        return self.scoreA + self.scoreB

    @hybrid_property
    def handshake(self):
        return self.handshakeA and self.handshakeB

    def __repr__(self):
        return (
            "<Match(id={match.id}, "
            "playerA={match.playerA.name}:{match.scoreA}, "
            "playerB={match.playerB.name}:{match.scoreB}, "
            "created={match.created}, "
            "active={match.active})>".format(match=self)
        )