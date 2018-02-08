CREATE TABLE IF NOT EXISTS EMPLOYEE (
    EMP_ID INTEGER NOT NULL,
    EMP_NAME VARCHAR ,
    CONTACT VARCHAR ,
    HIRE_DATE DATE,
    SALARY INTEGER CONSTRAINT PK PRIMARY KEY (EMP_ID)
)