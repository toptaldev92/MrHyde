#!/usr/bin/env python

from sqlite3 import Error as SQLError
from configparser import Error as ConfigError
from shutil import rmtree
import errno
import sys
import ConfigManager
import DbHandler
import logging
from time import time


class Caretaker:
    def __init__(self, config_file):
        self.__logger = logging.getLogger(__name__)
        try:
            self.__cm = ConfigManager.ConfigManager(config_file)
            self.__db = DbHandler.DbHandler(self.get_config().get_db_file())
        except SQLError:
            exit('Unable to connect to database.')
        except ConfigError:
            exit('Unable to parse config file.')

    def get_config(self):
        return self.__cm

    def database(self):
        return self.__db

    def log(self):
        return self.__logger

    def cleanup(self):
        timestamp = int(time()-(24*3600))
        try:
            old_repos = self.database().list('repo', '', 'last_used < %s' % timestamp)
            for repo in old_repos:
                rmtree(repo['path'])
                rmtree(repo['deploy_path'])
                self.database().deleteData('repo', "id='%s'" % repo['id'])
        except OSError as exception:
            if exception.errno == errno.ENOENT:
                self.log().error('Repository not found.')
                raise
            elif exception.errno == errno.EPERM:
                self.log().error('Insufficient permissions to remove repository.')
                raise
        except SQLError:
            self.log().error('Database error.')
            raise

if __name__ == '__main__':
    if len(sys.argv) < 2:
        exit('Config file missing!\nUsage: %s <config_file>' % sys.argv[0])
    else:
        caretaker = Caretaker(sys.argv[1])
        caretaker.cleanup()
