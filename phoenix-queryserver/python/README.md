#### install pip
`curl "https://bootstrap.pypa.io/get-pip.py" -o "get-pip.py"`
`sudo python get-pip.py`

#### from https://python-phoenixdb.readthedocs.io/en/latest/
`sudo pip install phoenixdb`

#### start queryserver then execute a python script filename.py with contents below
```
import phoenixdb
import phoenixdb.cursor

database_url = 'http://localhost:8765/'
conn = phoenixdb.connect(database_url, autocommit=True)

cursor = conn.cursor()
cursor.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, username VARCHAR)")
cursor.execute("UPSERT INTO users VALUES (?, ?)", (1, 'admin'))
cursor.execute("SELECT * FROM users")
print cursor.fetchall()

cursor = conn.cursor(cursor_factory=phoenixdb.cursor.DictCursor)
cursor.execute("SELECT * FROM users WHERE id=1")
print cursor.fetchone()['USERNAME']
```
