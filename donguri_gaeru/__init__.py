from sqlalchemy.orm.session import sessionmaker

Session = sessionmaker(expire_on_commit=False, future=True)
