import git
import errno
import ConfigManager
#import DbHandler
import logging

import string
import random

from os.path import isdir, join
from os import makedirs, listdir


class RepositoryManager:
    __cm = None
    #__db = None
    __base_dir = ""
    __git = git.Git()

    __logger = logging.getLogger(__name__)

    def __init__(self):
        try:
            self.__cm = ConfigManager.ConfigManager()
            self.__base_dir = self.__cm.get_base_dir()
            #self.__db = DbHandler.DbHandler(self.__cm.get_db_file())
        except KeyError:
            raise

    def init_repository(self, url, diff):
        id = self.generateId(int(self.__cm.get_hash_size()))
        if not self.repository_exists(id):
            if isdir(self.__base_dir):
                try:
                    self.__git.clone(url, join(self.__base_dir, id))
                    self.__logger.info('Repository cloned to ' + join(self.__base_dir, id) + '.')
                    return join(self.__base_dir, id)
                except OSError as exception:
                    if exception.errno == errno.EPERM:
                        self.__logger.error("Permission to " + self.get_repo_dir(url) + " denied.")
                        raise
            else:
                try:
                    makedirs(self.__base_dir, 0o755, True)
                    self.__git.clone(url, join(self.__base_dir, id))
                    self.__logger.info('Repository cloned to ' + join(self.__base_dir, id) + '.')
                    return join(self.__base_dir, id)
                except OSError as exception:
                    if exception.errno == errno.EPERM:
                        self.__logger.error("Permission to " + self.get_repo_dir(url) + " denied.")
                    raise

    def repository_exists(self, id):
        if isdir(join(self.__base_dir, id)):
            return True
        else:
            return False

    def list_repositories(self):
        dir_list = [f for f in listdir(self.__base_dir) if isdir(join(self.__base_dir, f))]
        return dir_list

    def list_single_repository(self, repo_name):
        repo_dir = join(self.__base_dir, repo_name)
        if isdir(repo_dir):
            file_list = [f for f in listdir(repo_dir)]
            return file_list
        else:
            return None

    def get_repo_name(self, url):
        return url.split('/')[-1]

    def get_repo_dir(self, url):
        return self.get_repo_name(url).split('.')[0]

    def cleanup_repositories(self):
        pass

    def update_repository(self, id):
        pass

    def generateId(self, length=16, chars=string.ascii_lowercase+string.digits):
        return ''.join(random.SystemRandom().choice(chars) for _ in range(length))
