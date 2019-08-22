# Apache Phoenix Intro

demo composite keys
```
DROP TABLE IF EXISTS TABLE1;
CREATE TABLE TABLE1 (ID BIGINT NOT NULL, COL1 VARCHAR, COL2 VARCHAR CONSTRAINT PK PRIMARY KEY(ID, COL1));
UPSERT INTO TABLE1 (ID, COL1, COL2) VALUES (1, 'test_row_1', 'col2');
UPSERT INTO TABLE1 (ID, COL1, COL2) VALUES (2, 'test_row_2', 'col2');
```

query the table
```
SELECT * FROM TABLE1;
SELECT COUNT(*) FROM TABLE1;
!help
!describe TABLE1
```

demo manual splits
```
DROP TABLE IF EXISTS USERS;
CREATE TABLE IF NOT EXISTS USERS (id INTEGER PRIMARY KEY, firstname VARCHAR) SPLIT ON ('A','J','M','R');
```

demo how Phoenix can handle splits with SALT_BUCKETS
```
DROP TABLE IF EXISTS USERS;
CREATE TABLE IF NOT EXISTS USERS (id INTEGER PRIMARY KEY, firstname VARCHAR) SALT_BUCKETS=4;
```

demo exposing DATA_BLOCK_ENCODING via Phoenix API
```
DROP TABLE IF EXISTS TEST;
CREATE TABLE TEST (MYKEY INTEGER PRIMARY KEY) DATA_BLOCK_ENCODING='FAST_DIFF';
```

Phoenix doesn't have a way to expose extended table properties currently, use `hbase shell` instead
```
{NAME => '0', BLOOMFILTER => 'NONE', VERSIONS => '1', IN_MEMORY => 'false', KEEP_DELET
ED_CELLS => 'FALSE', DATA_BLOCK_ENCODING => 'FAST_DIFF', TTL => 'FOREVER', COMPRESSION
 => 'NONE', MIN_VERSIONS => '0', BLOCKCACHE => 'true', BLOCKSIZE => '65536', REPLICATI
ON_SCOPE => '0'}
```

Phoenix has a concept for Sequences when you'd like to increment monotonously increasing numbers
```
CREATE SCHEMA IF NOT EXISTS HITS;
CREATE TABLE IF NOT EXISTS HITS.HITS (id INTEGER PRIMARY KEY) SALT_BUCKETS=4;
CREATE SEQUENCE IF NOT EXISTS HITS.HIT_SEQUENCE START 1 INCREMENT BY 1 CACHE 10;
```

now you can increment using the following statement
```
UPSERT INTO HITS.HITS(id) VALUES(NEXT VALUE FOR HITS.HIT_SEQUENCE);
SELECT MAX(ID) FROM HITS.HITS;
UPSERT INTO HITS.HITS(id) VALUES(NEXT VALUE FOR HITS.HIT_SEQUENCE);
UPSERT INTO HITS.HITS(id) VALUES(NEXT VALUE FOR HITS.HIT_SEQUENCE);
UPSERT INTO HITS.HITS(id) VALUES(NEXT VALUE FOR HITS.HIT_SEQUENCE);
SELECT MAX(ID) FROM HITS.HITS;

```            

Java example
```
git clone https://github.com/dbist/phoenix-examples
```
```
cd phoenix-examples/phoenix-java/src/main/java
javac LoadPhoenix.java
java -cp "$PHOENIX_HOME/phoenix-4.14.3-HBase-1.4-client.jar:." LoadPhoenix
```

if you get message
```
Exception in thread "main" org.apache.phoenix.exception.BatchUpdateExecution: ERROR 1106 (XCL06): Exception while executing batch.
	at org.apache.phoenix.jdbc.PhoenixStatement.executeBatch(PhoenixStatement.java:1688)
	at LoadPhoenix.main(LoadPhoenix.java:26)
Caused by: java.sql.SQLException: ERROR 730 (LIM02): MutationState size is bigger than maximum allowed number of bytes, try upserting rows in smaller batches or using autocommit on for deletes.
	at org.apache.phoenix.exception.SQLExceptionCode$Factory$1.newException(SQLExceptionCode.java:498)
	at org.apache.phoenix.exception.SQLExceptionInfo.buildException(SQLExceptionInfo.java:150)
	at org.apache.phoenix.execute.MutationState.throwIfTooBig(MutationState.java:382)
	at org.apache.phoenix.execute.MutationState.join(MutationState.java:482)
	at org.apache.phoenix.jdbc.PhoenixStatement$2.call(PhoenixStatement.java:411)
	at org.apache.phoenix.jdbc.PhoenixStatement$2.call(PhoenixStatement.java:393)
	at org.apache.phoenix.call.CallRunner.run(CallRunner.java:53)
	at org.apache.phoenix.jdbc.PhoenixStatement.executeMutation(PhoenixStatement.java:392)
	at org.apache.phoenix.jdbc.PhoenixStatement.executeMutation(PhoenixStatement.java:380)
	at org.apache.phoenix.jdbc.PhoenixPreparedStatement.execute(PhoenixPreparedStatement.java:173)
	at org.apache.phoenix.jdbc.PhoenixStatement.executeBatch(PhoenixStatement.java:1680)
	... 1 more
```

modify the batch i.e. 500000 to smaller, like 200000

if you get message

```
Exception in thread "main" java.sql.SQLException: ERROR 726 (43M10):  Inconsistent namespace mapping properties. Cannot initiate connection as SYSTEM:CATALOG is found but client does not have phoenix.schema.isNamespaceMappingEnabled enabled
	at org.apache.phoenix.exception.SQLExceptionCode$Factory$1.newException(SQLExceptionCode.java:498)
	at org.apache.phoenix.exception.SQLExceptionInfo.buildException(SQLExceptionInfo.java:150)
	at org.apache.phoenix.query.ConnectionQueryServicesImpl.ensureTableCreated(ConnectionQueryServicesImpl.java:1166)
```
copy `hbase-site.xml` from hbase conf directory to Java project directory

```
cp /opt/hbase/conf/hbase-site.xml phoenix-examples/phoenix-java/src/main/java
```

Skip Scans
```
!tables
SELECT COUNT(*) FROM LARGETBL;
SELECT * FROM LARGETBL LIMIT 100;

SELECT * FROM LARGETBL
WHERE ((MYKEY > 1 AND MYKEY <= 49999))
AND MYKEY IN (10, 20, 99, 1111, 12000, 14000, 15000, 20000, 33333, 40000);
```

QueryServer
```
$PHOENIX_HOME/bin/queryserver.py start &
```

enter the following below

```
from typing import List, Dict
import phoenixdb
import json

def get_ids() -> List[Dict]:
    database_url = 'http://localhost:8765/'
    conn = phoenixdb.connect(database_url, autocommit=True)
    cursor = conn.cursor()
    cursor.execute("CREATE TABLE IF NOT EXISTS USERS (id INTEGER PRIMARY KEY, firstname VARCHAR) SALT_BUCKETS=4")
    cursor.execute("UPSERT INTO USERS VALUES (?, ?)", (1, 'Artem'))
    cursor.execute("UPSERT INTO USERS VALUES (?, ?)", (2, 'Josiah'))
    cursor.execute("UPSERT INTO USERS VALUES (?, ?)", (3, 'Mac'))
    cursor.execute("UPSERT INTO USERS VALUES (?, ?)", (4, 'Ron'))
    cursor.execute('SELECT * FROM USERS')
    results = [{id: firstname} for (id, firstname) in cursor]
    cursor.close()
    conn.close()
    return results

if __name__ == '__main__':
    json.dumps({'Users:': get_ids()})
```

in another terminal open hbase shell

`/opt/hbase/bin/hbase shell`

`list`

`scan 'USERS'`

describe table in hbase shell to show significant benefits of Phoenix
```
hbase(main):001:0> describe 'USERS'
Table USERS is ENABLED
USERS, {TABLE_ATTRIBUTES => {coprocessor$1 => '|org.apache.phoenix.coprocessor.ScanRegionO
bserver|805306366|', coprocessor$2 => '|org.apache.phoenix.coprocessor.UngroupedAggregateR
egionObserver|805306366|', coprocessor$3 => '|org.apache.phoenix.coprocessor.GroupedAggreg
ateRegionObserver|805306366|', coprocessor$4 => '|org.apache.phoenix.coprocessor.ServerCac
hingEndpointImpl|805306366|', coprocessor$5 => '|org.apache.phoenix.hbase.index.Indexer|80
5306366|org.apache.hadoop.hbase.index.codec.class=org.apache.phoenix.index.PhoenixIndexCod
ec,index.builder=org.apache.phoenix.index.PhoenixIndexBuilder'}
COLUMN FAMILIES DESCRIPTION
{NAME => '0', BLOOMFILTER => 'NONE', VERSIONS => '1', IN_MEMORY => 'false', KEEP_DELETED_C
ELLS => 'FALSE', DATA_BLOCK_ENCODING => 'FAST_DIFF', TTL => 'FOREVER', COMPRESSION => 'NON
E', MIN_VERSIONS => '0', BLOCKCACHE => 'true', BLOCKSIZE => '65536', REPLICATION_SCOPE =>
'0'}
1 row(s) in 0.5100 seconds
```

open sqlline and browse the table

`/opt/phoenix/bin/sqlline.py`

run SQL against the table


start spark shell
```
$SPARK_HOME/bin/spark-shell \
    --master yarn \
    --deploy-mode client \
    --driver-memory 512m \
    --executor-memory 512m \
    --executor-cores 1 \
    --queue default \
    --jars $PHOENIX_HOME/*.jar \
    --driver-class-path $PHOENIX_HOME/phoenix-*-HBase-*-client.jar:/etc/hbase/conf
```

Load as a DataFrame using the Data Source API
```
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import org.apache.phoenix.spark._

val sqlContext = new SQLContext(sc)

val df = sqlContext.load(
"org.apache.phoenix.spark",
  Map("table" -> "TABLE1", "zkUrl" -> "localhost:2181")
)
df.filter(df("COL1") === "test_row_1" && df("ID") === 1L).select(df("ID")).show
```

sqlline.py  // show copying data from one table to another
```
CREATE TABLE IF NOT EXISTS OUTPUTTBL (MYKEY INTEGER NOT NULL PRIMARY KEY, MYCOLUMN VARCHAR) SALT_BUCKETS = 10;
```

Saving to Phoenix
```
import org.apache.spark.SparkContext
import org.apache.spark.sql._
import org.apache.phoenix.spark._

// Load Source Table
val sqlContext = new SQLContext(sc)
val df = sqlContext.load("org.apache.phoenix.spark", Map("table" -> "LARGETBL",
  "zkUrl" -> "localhost:2181"))

// Save to Output Table
df.saveToPhoenix(Map("table" -> "OUTPUTTBL", "zkUrl" -> "localhost:2181"))
```

Query the Output Table
```
SELECT COUNT(*) FROM OUTPUTTBL;
```

Bulk Load, data generated with Mockaroo, for `id`, used Mockaroo fx `random(10000, 99999)`
Follow the bulk load example in the following [link](https://phoenix.apache.org/bulk_dataload.html).
* Note: import file cannot have a header.
```
hdfs dfs -put data.csv.gz .
```

```
 CREATE TABLE example (
    my_pk bigint not null,
    m.first_name varchar(50),
    m.last_name varchar(50) 
    CONSTRAINT pk PRIMARY KEY (my_pk));
```
```
HADOOP_CLASSPATH=/opt/hbase/hbase-1.4.10/lib/hbase-protocol-1.4.10.jar:/opt/hbase/hbase-1.4.10/conf hadoop jar /opt/phoenix/apache-phoenix-4.14.3-HBase-1.4-bin/phoenix-4.14.3-HBase-1.4-client.jar org.apache.phoenix.mapreduce.CsvBulkLoadTool --table EXAMPLE --input data.csv.gz
```

Load table while populating index
```
CREATE INDEX IDX_EXAMLE ON EXAMPLE (last_name, first_name);
```
```
HADOOP_CLASSPATH=/opt/hbase/hbase-1.4.10/lib/hbase-protocol-1.4.10.jar:/opt/hbase/hbase-1.4.10/conf hadoop jar /opt/phoenix/apache-phoenix-4.14.2-HBase-1.4-bin/phoenix-4.14.2-HBase-1.4-client.jar org.apache.phoenix.mapreduce.CsvBulkLoadTool --table EXAMPLE --input data.csv -it IDX_EXAMPLE
```

Are there any tips for optimizing Phoenix?
Use Salting to increase read/write performance Salting can significantly increase read/write performance by pre-splitting the data into multiple regions. Although Salting will yield better performance in most scenarios.

Note: Ideally for a 16 region server cluster with quad-core CPUs, choose salt buckets between 32-64 for optimal performance.

Per-split table Salting does automatic table splitting but in case you want to exactly control where table split occurs with out adding extra byte or change row key order then you can pre-split a table.
