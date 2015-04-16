import sqlite3
import sys
import logging


class DbHandler:
    """A class for communication with an sqlite3 database"""

    __logger = logging.getLogger(__name__)
    __db = None
    __cursor = None

    def __init__(self, dbFile=""):
        """Initializes the object.
        If a database file is passed as argument, it'll be
        opened and used as current database
        """

        self.__dbFile = dbFile

        if dbFile:
            self.__logger.info("Loading %s" % self.__dbFile)
            try:
                self.__db = sqlite3.connect(self.__dbFile)
                self.__db.row_factory = sqlite3.Row
                self.__cursor = self.__db.cursor()
            except sqlite3.Error as exception:
                self.__logger.error("Error %s occured!" % exception.args[0])
                raise

    def __del__(self):
        if self.__db:
            self.__db.close()

    def comm(self, dbString, data=None):
        """Sends a SQL query"""
        try:
            if data:
                self.__cursor.execute(dbString, data)
            else:
                self.__cursor.execute(dbString)
            self.__db.commit()
        except sqlite3.Error as exception:
            self.__logger.error("Error %s occured!" % exception.args[0])
            raise

    def fetch(self):
        """Fetches the output of a SQL query"""
        try:
            return self.__cursor.fetchall()
        except sqlite3.Error as exception:
            self.__logger.error("Error %s occured!" % exception.args[0])
            raise

    def list(self, table, column="", where=""):
        """List entrys in a table"""
        if where:
            dbString = "SELECT * FROM '%s' WHERE %s;" % (table, where)
        else:
            dbString = "SELECT * FROM '%s';" % table

        self.comm(dbString)
        if column:
            return [c[column] for c in self.fetch()]
        else:
            return self.fetch()

    def createDatabase(self, dbFile):
        """Create a new database file"""
        try:
            self.__dbFile = dbFile
            self.__db = sqlite3.connect(self.__dbFile)
            self.__db.row_factory = sqlite3.Row
            self.__cursor = self.__db.cursor()

        except sqlite3.Error as exception:
            self.__logger.error("SQL error %s occured!" % exception.args[0])

    def useDatabase(self, dbFile):
        """Change database file"""
        if self.__db:
            self.__db.close()

        try:
            self.__dbFile = dbFile
            self.__db = sqlite3.connect(self.__dbFile)
            self.__db.row_factory = sqlite3.Row
            self.__cursor = self.__db.cursor()

        except sqlite3.Error as exception:
            self.__logger.error("SQL error %s occured!" % exception.args[0])

    def createTable(self, table, *args):
        """Creates a new table. Elements are passed as arguments.
        Example: ("number","INTEGER","string","TEXT")"""

        dbString = "CREATE TABLE IF NOT EXISTS '%s' (" % table
        for elem in range(0, len(args) - 1, 2):
            dbString = "".join([dbString, "'%s' '%s'" % (args[elem], args[elem + 1])])
            if elem < (len(args) - 2):
                dbString = "".join([dbString, ","])

        dbString = "".join([dbString, ");"])

        self.comm(dbString)

    def insertData(self, table, *args):
        """Adds data to selected table"""
        if args:
            dbString = "INSERT INTO '%s' VALUES (" % table
            for elem in range(len(args)):
                dbString = "".join([dbString, "?"])
                if elem < (len(args) - 1):
                    dbString = "".join([dbString, ","])

            dbString = "".join([dbString, ");"])
            self.comm(dbString, args)
        else:
            self.__logger.error("Error! No values to insert!")

    def updateData(self, table, where="", *args):
        """Changes data in table.
           If no specific criterion is needed,
           the 'where' parameter is an empty string."""
        if args:
            dbString = "UPDATE %s SET " % table
            for elem in range(len(args)):
                dbString = "".join([dbString, "%s" % args[elem]])
                if elem < (len(args) - 1):
                    dbString = "".join([dbString, ","])

            if where:
                dbString = "".join([dbString, " WHERE %s;" % where])

            print(dbString)

            self.comm(dbString)

    def deleteData(self, table, where):
        """Delete data from table"""
        if where:
            dbString = "DELETE FROM '%s' WHERE %s;" % (table, where)
            self.comm(dbString)
        else:
            sys.stderr.write("Nothing selected!")

    def showTables(self):
        """Shows tables of a database"""
        if(self.__db):
            dbString = "SELECT name FROM sqlite_master WHERE type='table';"
            self.comm(dbString)
            entrys = self.fetch()
            for elem in entrys:
                print("%s" % elem["name"])

        else:
            self.__logger.error("Error! No database connection!")

    def deleteTable(self, table):
        """Deletes an existing table"""
        dbString = "DROP TABLE IF EXISTS %s;" % table
        self.comm(dbString)

    def listCols(self, table):
        """Lists columns of a table"""
        self.comm("SELECT * FROM '%s';" % table)
        output = self.__cursor.description
        buf = ""
        for elem in range(len(output)):
            buf = "".join([buf, ("%s" % output[elem][0])])
            if elem < (len(output) - 1):
                buf = "".join([buf, ", "])

        buf = "".join([buf, ""])

        return buf

    def addCol(self, table, name, typ):
        """Adds a column to the used database"""
        dbString = "ALTER TABLE '%s' ADD '%s' '%s';" % (table, name, typ)
        self.comm(dbString)

    def renameTable(self, tableOld, tableNew):
        """Renames a table in the used database"""
        dbString = "ALTER TABLE '%s' RENAME TO '%s';" % (tableOld, tableNew)
        self.comm(dbString)
