import argparse
import os
import re
import subprocess
import sys
from collections import defaultdict
from contextlib import suppress
from datetime import datetime, timezone
from itertools import chain
from random import choice

import networkx as nx
from community import community_louvain
from progress.bar import Bar
from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from sqlalchemy_utils import create_database, database_exists

from config import LOCAL_SQL_HOSTNAME, LOCAL_SQL_PASSWORD, LOCAL_SQL_USERNAME
from donguri_gaeru.database import Base, Match, Player

parser = argparse.ArgumentParser(
    description=(
        "Create a test database from Hiku's World Ranking dataset. "
        'Default [DBNAME] is "hikuwr".\n'
        'Command "--extract" will suffix [DBNAME] with "_puyolobbyA" and "_puyolobbyB".'
    ),
    formatter_class=argparse.RawTextHelpFormatter,
)
parser.add_argument(
    "-l",
    "--local",
    nargs="?",
    const="hikuwr",
    metavar="DBNAME",
    help="create database using the local server credentials",
)
parser.add_argument(
    "-x",
    "--extract",
    nargs="?",
    const="hikuwr",
    metavar="DBNAME",
    help="create subset-databases using the local server credentials",
)
parser.add_argument(
    "-k",
    "--heroku",
    metavar="APPNAME",
    help="create database using a heroku application",
)


def file2graph():
    # Load the dateset from the text file.
    filename = os.path.join(os.path.dirname(__file__), "source/matches_aug_2021.txt")
    with open(filename, "r", encoding="utf-8") as file:
        lines = file.readlines()

    # Parse the dataset and construct the graph.
    graph = nx.Graph()
    for line in lines:
        if datematch := re.match(r"^(\d\d/\d\d/\d\d)$", line):
            date = datetime.strptime(datematch.group(), "%d/%m/%y")
            date = date.replace(tzinfo=timezone.utc)

        if gamematch := re.match(r"^(\S+) (\d+) (\d+) (\S+)$", line):
            graph.add_node(playerA := gamematch.group(1), created=date)
            graph.add_node(playerB := gamematch.group(4), created=date)
            graph.add_edge(
                playerA,
                playerB,
                scoreA=int(gamematch.group(2)),
                scoreB=int(gamematch.group(3)),
                created=date,
            )

    return graph


def graph2database(dbname, session, graph):
    # Add the player if not already in the database, return the id.
    def player(name, created):
        player = session.query(Player).filter_by(name=name).first()
        if player is None:
            player = Player(name=name, created=created)
            session.add(player)
            session.commit()

        return player.id

    # Loop through each match and populate the database.
    bar = Bar(dbname, check_tty=False, suffix="%(percent)d%%")
    matches_bydate = sorted(graph.edges.data(), key=lambda match: match[2]["created"])
    for match in bar.iter(matches_bydate):
        with suppress(AssertionError):
            idA = player(match[0], match[2]["created"])
            idB = player(match[1], match[2]["created"])
            session.add(Match(playerA_id=idA, playerB_id=idB, **match[2]))
            session.commit()


def heroku(appname, graph):
    # Get the Heroku database access credentials via the CLI.
    cmd = "heroku config:get DATABASE_URL -a " + appname
    url = (
        subprocess.check_output(cmd, shell=True)
        .decode("utf-8")
        .strip()
        .replace("postgres", "postgresql")
    )
    while len(graph.nodes) + len(graph.edges) > 10000:
        graph.remove_edge(*choice(list(graph.edges)))

    yield appname, url, graph


def extract(dbprefix, graph):
    puyolobby_matches = [
        (match[:2])
        for match in graph.edges.data()
        if datetime(2019, 12, 1, tzinfo=timezone.utc)
        < match[2]["created"]
        < datetime(2020, 2, 1, tzinfo=timezone.utc)
    ]
    graph = graph.edge_subgraph(puyolobby_matches)

    # Repeatedly partition the graph into communities until:
    # 1. The two largest partitions contain 1/2 of all matches in the queried dataset.
    # 2. The two largest partitions have the same number of matches to within 5%.
    while True:
        part = community_louvain.best_partition(graph)
        ipart = defaultdict(list)
        [ipart[v].append(k) for k, v in part.items()]

        bysize = sorted(ipart, key=lambda k: len(ipart[k]), reverse=True)
        bysize1 = len(graph.subgraph(ipart[bysize[0]]).edges)
        bysize2 = len(graph.subgraph(ipart[bysize[1]]).edges)
        bysize_ave = (bysize1 + bysize2) / 2
        condition1 = bysize1 + bysize2 > len(graph.edges) * 0.5
        condition2 = abs(bysize1 - bysize_ave) / bysize_ave < 0.05
        if condition1 and condition2:
            break

    # The smaller partitions will be added, in order of size, to the smaller of
    # the two largest partitions (iteratively).
    for i in range(2, len(bysize)):
        if bysize1 >= bysize2:
            ipart[bysize[1]].extend(ipart[bysize[i]])
            bysize2 += len(graph.subgraph(ipart[bysize[i]]).edges)
        else:
            ipart[bysize[0]].extend(ipart[bysize[i]])
            bysize1 += len(graph.subgraph(ipart[bysize[i]]).edges)

    url = dbname2url(dbname := dbprefix + "_puyolobbyA")
    yield dbname, url, graph.subgraph(ipart[bysize[0]])

    url = dbname2url(dbname := dbprefix + "_puyolobbyB")
    yield dbname, url, graph.subgraph(ipart[bysize[1]])


def local(dbname, graph):
    url = dbname2url(dbname)
    yield dbname, url, graph


def dbname2url(dbname):
    # Get the local database access credentials via the private config.
    sql = "postgresql://{username}:{password}@{hostname}/{database}"
    url = sql.format(
        username=LOCAL_SQL_USERNAME,
        password=LOCAL_SQL_PASSWORD,
        hostname=LOCAL_SQL_HOSTNAME,
        database=dbname,
    )

    if not database_exists(url):
        create_database(url)

    return url


def start():
    if not len(sys.argv) > 1:
        parser.print_help()
        return

    args = parser.parse_args()
    graph = file2graph()
    dbiter = chain()

    if args.local is not None:
        dbiter = chain(dbiter, local(args.local, graph.copy()))
    if args.extract is not None:
        dbiter = chain(dbiter, extract(args.extract, graph.copy()))
    if args.heroku is not None:
        dbiter = chain(dbiter, heroku(args.heroku, graph.copy()))

    for dbname, url, graph in dbiter:
        engine = create_engine(url)
        Base.metadata.drop_all(engine)
        Base.metadata.create_all(engine)
        connection = engine.connect()
        session = Session(bind=connection)
        graph2database(dbname, session, graph)
        session.close()
        connection.close()
        engine.dispose()
