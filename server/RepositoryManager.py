import git
import errno
import ConfigManager
import DbHandler
import logging

import string
import random
from time import time

from os.path import isdir, join
from os import makedirs, listdir, chdir, getcwd


class RepositoryManager:
    __cm = None
    __db = None
    __base_dir = ""
    __git = git.Git()

    __logger = logging.getLogger(__name__)

    def __init__(self):
        try:
            self.__cm = ConfigManager.ConfigManager()
            self.__base_dir = self.__cm.get_base_dir()
            self.__base_url = self.__cm.get_base_url()
            self.__db = DbHandler.DbHandler(self.__cm.get_db_file())
        except KeyError:
            raise

    def init_repository(self, url, diff):
        id = self.generateId(int(self.__cm.get_hash_size()))
        repo_path = join(self.__base_dir, id)
        if not self.repository_exists(id):
            if isdir(self.__base_dir):
                try:
                    self.__git.clone(url, repo_path)
                    self.__db.insertData('repo', id, repo_path, url, int(time()))
                    self.__logger.info('Repository cloned to ' + repo_path + '.')
                    return ''.join([self.__base_url, '/', id])
                except OSError as exception:
                    if exception.errno == errno.EPERM:
                        self.__logger.error("Permission to " + repo_path + " denied.")
                        raise
            else:
                try:
                    makedirs(self.__base_dir, 0o755, True)
                    self.__git.clone(url, join(self.__base_dir, id))
                    self.__db.insertData('repo', id, repo_path, url, int(time()))
                    self.__logger.info('Repository cloned to ' + repo_path + '.')
                    return ''.join([self.__base_url, '/', id])
                except OSError as exception:
                    if exception.errno == errno.EPERM:
                        self.__logger.error("Permission to " + repo_path + " denied.")
                    raise
        else:
            return None

    def repository_exists(self, id):
        if isdir(join(self.__base_dir, id)):
            return True
        else:
            return False

    def list_repositories(self):
        dir_list = [[f['path'], f['url']] for f in self.__db.list('repo', 'path') if isdir(f['path'])]
        #dir_list = [f for f in listdir(self.__base_dir) if isdir(join(self.__base_dir, f))]
        return dir_list

    def list_single_repository(self, id):
        repo_path = join(self.__base_dir, id)
        if isdir(repo_path):
            file_list = [f for f in listdir(repo_path)]
            return file_list
        else:
            return None

    def cleanup_repositories(self):
        pass

    def update_repository(self, id, diff):
        repo_path = join(self.__base_dir, id)
        old_dir = getcwd()
        chdir(repo_path)
        self.__git.apply(diff)
        chdir(old_dir)

    def generateId(self, length=16, chars=string.ascii_lowercase+string.digits):
        return ''.join(random.SystemRandom().choice(chars) for _ in range(length))
