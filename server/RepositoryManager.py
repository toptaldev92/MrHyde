import git
import errno
import ConfigManager
import DbHandler
import logging

import string
import random
from time import time
from shutil import rmtree

from os.path import isdir, join
from os import makedirs, listdir, chdir, getcwd, mkdir
import subprocess


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
                    build_successful = self.start_jekyll_build(repo_path)
                    if build_successful:
                        return ''.join([self.__base_url, '/', id, '/__page'])
                    else:
                        # TODO proper error handling!
                        raise Exception
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
                    self.start_jekyll_build(repo_path)
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
        dir_list = [[f['path'], f['url']] for f in self.__db.list('repo') if isdir(f['path'])]
        return dir_list

    def list_single_repository(self, id):
        repo_path = join(self.__base_dir, id)
        if isdir(repo_path):
            file_list = [f for f in listdir(repo_path)]
            return file_list
        else:
            return None

    def cleanup_repositories(self):
        timestamp = int(time()-(24*3600))
        old_repos = self.__db.list('repo', '', 'last_used < %s' % timestamp)
        try:
            for repo in old_repos:
                rmtree(repo['path'])
                self.__db.deleteData('repo', "id='%s'" %repo['id'])
        except OSError as exception:
            if exception.errno == errno.ENOENT:
                self.__logger.error('Repository ' + repo + ' not found.')
                raise
            elif exception.errno == errno.EPERM:
                self.__logger.error('Insufficient permissions to remove repository ' + repo + '.')
                raise

    def update_repository(self, id, diff):
        repo_path = join(self.__base_dir, id)
        old_dir = getcwd()
        chdir(repo_path)
        self.__git.apply(diff)
        self.__db.updateData('repo', "id = '%s'" %id, 'last_used=%s' % int(time()))
        chdir(old_dir)
        build_successful = self.start_jekyll_build(repo_path)
        if build_successful:
            return ''.join([self.__base_url, '/', id, '/__page'])
        else:
            # TODO proper error handling!
            raise Exception

    def generateId(self, length=16, chars=string.ascii_lowercase+string.digits):
        return ''.join(random.SystemRandom().choice(chars) for _ in range(length))


    def start_jekyll_build(self, path):
        # TODO perhaps we should do this async (big pages?)
        mkdir(path+'/__page')
        pagepath = path+'/__page'
        cmd = ['jekyll', 'build', '--source', path, '--destination', pagepath]
        try:
            process = subprocess.check_output(cmd)
            return True
        except subprocess.CalledProcessError as exception:
            return False
