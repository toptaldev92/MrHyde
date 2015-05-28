#!/usr/local/bin/python3.4
from os import makedirs

from sqlite3 import Error as SQLError
from configparser import Error as ConfigError
from shutil import rmtree
import sys
import ConfigManager
import DbHandler
import logging
from time import time

logging.basicConfig(filename='caretaker.log', level=logging.DEBUG)


class Caretaker:

    __html_content = """
<!doctype html>
<html>
    <head>
    <meta charset="utf-8">
    <title>MrHyde - a Jekyll scratchpad</title>
    <link rel="stylesheet" href="http://faudroid.markab.uberspace.de/jekyll/static/layout.css" type="text/css">
    <link href='http://fonts.googleapis.com/css?family=Droid+Sans:400,700' rel='stylesheet' type='text/css'>
    </head>

    <body>
        <div style="text-align: center;">
            <p><strong>Link expired!</strong></p>
            <p>Preview links expire after %s minutes.</p>
        </div>
        <p></p>
        <div align="center"><img src="http://faudroid.markab.uberspace.de/jekyll/static/ic_background.svg" alt="MrHyde logo"></div>
        <p></p>
        <footer style="text-align: center;">&copy; FauDroids 2015</footer>
    </body>
</html>
"""

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

    def deactivate_repos(self):
        timestamp = int(time()-float(self.get_config().get_cleanup_time()))
        try:
            old_repos = self.database().list('repo', '', 'last_used < %s and active > 0' % timestamp)
            for repo in old_repos:
                self.log().info('Deleting repo %s' % repo['path'])
                try:
                    rmtree(repo['path'])
                except OSError as exception:
                    self.log().error(exception.strerror)
                finally:
                    # Set repo inactive
                    self.database().updateData('repo', "id='%s'" % repo['id'], 'active = 0')

                try:
                    rmtree(repo['deploy_path'])
                except OSError as exception:
                    self.log().error(exception.strerror)

                makedirs(repo['deploy_path'], 0o755, True)
                try:
                    index_file_path = '/'.join([repo['deploy_path'], 'index.html'])
                    index_file = open(index_file_path, 'w')
                    index_file.write(self.__html_content % (int(self.get_config().get_cleanup_time())/60))
                except IOError as exception:
                    self.log().error(exception.strerror)
                finally:
                    index_file.close()
        except SQLError:
            self.log().error("Database error, we're in trouble!")
            raise

    def cleanup_subdomains(self):
        self.log().info('Cleaning up subdomains.')
        try:
            old_repos = self.database().list('repo', '', 'active < 1')
            for repo in old_repos:
                try:
                    self.log().info('Cleaning up deploy path %s' % repo['deploy_path'])
                    rmtree(repo['deploy_path'])
                except OSError as exception:
                    self.log().error(exception.strerror)
                else:
                    self.database().deleteData('repo', "id='%s'" % repo['id'])
        except SQLError:
            self.log().error("Database error, we're in trouble!")
            raise

if __name__ == '__main__':
    if len(sys.argv) < 3:
        exit('Parameters missing!\nUsage: %s <config_file> <mode=(DEACTIVATE|CLEANUP)>' % sys.argv[0])
    else:
        caretaker = Caretaker(sys.argv[1])
        if sys.argv[2] == 'DEACTIVATE':
            caretaker.deactivate_repos()
        elif sys.argv[2] == 'CLEANUP':
            caretaker.cleanup_subdomains()
        else:
            exit('Unknown command line argument!\nUsage: %s <config_file> <mode=(DEACTIVATE|CLEANUP)>' % sys.argv[0])
