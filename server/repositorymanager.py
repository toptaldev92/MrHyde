from sqlite3 import Error as SQLError
import logging
from time import time
from shutil import rmtree
from os.path import isdir, join, dirname, realpath
from os import makedirs, chdir, getcwd
from configparser import ParsingError as ConfigError
import threading
import shutil

import git

import dbhandler
import jekyllmanager
import filemanager
import repoutils

OWN_PATH = dirname(realpath(__file__))

logger = logging.getLogger(__name__)

class RepositoryManager:
    __fm = None
    __utils = None
    __jm = None
    __cm = None
    __base_dir = ""
    __git = git.Git()

    def __init__(self, cm):
        try:
            self.__cm = cm
            self.__base_dir = self.__cm.get_base_dir()
            self.__base_url = self.__cm.get_base_url()
            self.__fm = filemanager.FileManager(self.__cm)
            self.__utils = repoutils.RepoUtils(self.__cm)
            self.__jm = jekyllmanager.JekyllManager()
        except ConfigError as exception:
            logger.error(exception.__str__)
            raise

    def utils(self):
        return self.__utils

    def fm(self):
        return self.__fm

    def jm(self):
        return self.__jm

    def cm(self):
        return self.__cm

    def init_repository(self, url, diff):
        try:
            database = dbhandler.DbHandler(self.cm().get_db_file())
            id = self.utils().generateId(self.cm().get_hash_size())
            repo_path = join(self.__base_dir, id)
            deploy_path = ''.join([self.cm().get_deploy_base_path(), id, self.cm().get_deploy_append_path()])

            if self.utils().repository_exists(id):
                return None

            database.insertData('repo', id, repo_path, deploy_path, url, int(time()), 1)
            shutil.copytree(OWN_PATH + '/redirector/', deploy_path)

            t = threading.Thread(target=self.init_repository_async, args=(id, deploy_path, repo_path, url, diff))
            t.daemon = True
            t.start()

            return (id, ''.join(['http://', id, '.', self.cm().get_base_url(), '/poller.html']))
        except SQLError as exception:
            logger.error(exception.__str__())
            raise

    def init_repository_async(self, id, deploy_path, repo_path, url, diff):
        if not isdir(self.__base_dir):
            makedirs(self.__base_dir, 0o755, True)
        try:
            self.__git.clone(url, repo_path)
            if diff is not None and diff is not '':
                self.apply_diff(id, repo_path, diff)
            logger.info('Repository cloned to ' + repo_path + '.')
            self.jm().build(repo_path, deploy_path)

        except OSError as exception:
            logger.error(exception.strerror)
            self.fm().deploy_error_page(deploy_path,
                                        'I/O error',
                                        "An I/O error occurred, stay calm and wait for a technician!")
            with open(deploy_path + '/statuscode.txt', 'w') as outfile:
                outfile.write(str(0))
        except SQLError as exception:
            logger.error(exception.__str__())
            self.fm().deploy_error_page(deploy_path,
                                        'Database error',
                                        "We encountered an error in our database. (╯°□°）╯︵ ┻━┻")
            with open(deploy_path + '/statuscode.txt', 'w') as outfile:
                outfile.write(str(0))
        except git.GitCommandError as exception:
            logger.error(exception.__str__())
            self.fm().deploy_error_page(deploy_path,
                                        'VCS error',
                                        "We're having problems applying your changes. Have you been sacrificing some branches to the gods of git lately?")
            with open(deploy_path + '/statuscode.txt', 'w') as outfile:
                outfile.write(str(0))

    def list_repositories(self):
        dir_list = [[f['path'].split('/')[-1], f['url']] for f in self.db().list('repo') if isdir(f['path'])]
        return dir_list

    def delete_repository(self, id):
        try:
            database = dbhandler.DbHandler(self.cm().get_db_file())
            repo = database.list('repo', '', "id='%s'" % id)[0]
            rmtree(repo['path'])
            database.deleteData('repo', "id='%s'" % repo['id'])
        except OSError as exception:
            logger.error(exception.strerror)
            raise
        except SQLError as exception:
            logger.error(exception.__str__())
            raise

    # TODO update repos async
    def update_repository(self, id, diff):
        try:
            database = dbhandler.DbHandler(self.cm().get_db_file())
            repo_path = database.list('repo', 'path', "id='%s'" % id)[0]
            deploy_path = database.list('repo', 'deploy_path', "id='%s'" % id)[0]
            diff_file = self.fm().create_diff_file(id, diff)
            self.apply_diff(id, repo_path, diff_file)
            build_successful = self.jm().build(repo_path, deploy_path)
            if build_successful:
                return (id, ''.join(['https://', id, '.', self.__base_url]))
            else:
                return (id, self.jm().get_errors())
        except OSError as exception:
            logger.error(exception.strerror)
            raise
        except SQLError as exception:
            logger.error(exception.__str__())
            raise
        except git.GitCommandError as exception:
            logger.error(exception.__str__())
            raise

    def apply_diff(self, id, repo_path, diff):
        try:
            old_dir = getcwd()
            chdir(repo_path)
            diff_file = self.fm().create_diff_file(id, diff)
            self.__git.apply(diff_file)
            # self.utils().update_timestamp(id)
        except SQLError as exception:
            logger.error(exception.__str__())
            raise
        except git.GitCommandError as exception:
            logger.error(exception.__str__())
            raise
        except OSError as exception:
            logger.error(exception.strerror)
            raise
        finally:
            chdir(old_dir)
