import errno
from sqlite3 import Error as SQLError
import logging
from time import time
from shutil import rmtree
from os.path import isdir, join
from os import makedirs, chdir, getcwd
from configparser import ParsingError as ConfigError
import threading

import shutil

import git

import DbHandler
import JekyllManager
import FileManager
import RepoUtils


class RepositoryManager:
    __db = None
    __fm = None
    __utils = None
    __jm = None
    __base_dir = ""
    __git = git.Git()

    __logger = logging.getLogger(__name__)

    def __init__(self):
        from main import cm
        try:
            self.__base_dir = cm.get_base_dir()
            self.__base_url = cm.get_base_url()
            self.__db = DbHandler.DbHandler(cm.get_db_file())
            self.__fm = FileManager.FileManager()
            self.__utils = RepoUtils.RepoUtils()
            self.__jm = JekyllManager.JekyllManager()
        except SQLError:
            raise
        except ConfigError:
            raise

    def utils(self):
        return self.__utils

    def db(self):
        return self.__db

    def fm(self):
        return self.__fm

    def jm(self):
        return self.__jm

    def init_repository(self, url, diff):
        from main import cm
        id = self.utils().generateId(cm.get_hash_size())
        repo_path = join(self.__base_dir, id)
        deploy_path = ''.join([cm.get_deploy_base_path(), id, cm.get_deploy_append_path()])

        self.db().insertData('repo', id, repo_path, deploy_path, url, int(time()))
        shutil.copytree('redirector/', deploy_path)

        t = threading.Thread(target=self.init_repository_async, args=(id, deploy_path, repo_path, url, diff))
        t.daemon = True
        t.start()

        return (id, ''.join(['https://', id, '.', cm.get_base_url(), 'poller.html']))


    def init_repository_async(self, id, deploy_path, repo_path, url, diff):
        if not isdir(self.__base_dir):
            makedirs(self.__base_dir, 0o755, True)
        if not self.utils().repository_exists(id):
            try:
                # TODO error handling
                self.__git.clone(url, repo_path)
                if diff is not None and diff is not '':
                    self.apply_diff(id, diff)
                self.log().info('Repository cloned to ' + repo_path + '.')
                deploy_path = self.fm().setup_deployment(id)
                self.jm().build(repo_path, deploy_path)
            except OSError as exception:
                if exception.errno == errno.EPERM:
                    self.log().error("Permission to " + repo_path + " denied.")
                    raise
            except git.GitCommandError as exception:
                self.log().error(exception.__str__())
                raise
        else:
            return None

    def list_repositories(self):
        dir_list = [[f['path'].split('/')[-1], f['url']] for f in self.db().list('repo') if isdir(f['path'])]
        return dir_list

    def delete_repository(self, id):
        try:
            repo = self.db().list('repo', '', "id='%s'" % id)[0]
            rmtree(repo['path'])
            self.db().deleteData('repo', "id='%s'" % repo['id'])
            return
        except OSError as exception:
            if exception.errno == errno.ENOENT:
                self.log().error('Repository ' + id + ' not found.')
                raise
            elif exception.errno == errno.EPERM:
                self.log().error('Insufficient permissions to remove repository ' + id + '.')
                raise
        except SQLError:
            raise

    def update_repository(self, id, diff):
        try:
            repo_path = self.db().list('repo', 'path', "id='%s'" % id)[0]
            deploy_path = self.db().list('repo', 'deploy_path', "id='%s'" % id)[0]
            diff_file = self.fm().create_diff_file(id, diff)
            self.apply_diff(id, diff_file)
            build_successful = self.jm().build(repo_path, deploy_path)
            if build_successful:
                return (id, ''.join(['https://', id, '.', self.__base_url]))
            else:
                return (id, self.__jekyll.get_errors())
        except OSError as exception:
            if exception.errno == errno.ENOENT:
                self.log().error('Repository ' + repo_path['id'] + ' not found.')
                raise
            elif exception.errno == errno.EPERM:
                self.log().error('Insufficient permissions to remove repository ' + repo_path['id'] + '.')
                raise
        except SQLError:
            self.log().error('Database error.')
            raise
        except git.GitCommandError as exception:
            self.log().error(exception.__str__())
            raise

    def log(self):
        return self.__logger

    def apply_diff(self, id, diff):
        try:
            repo_path = self.db().list('repo', 'path', "id='%s'" % id)[0]
            old_dir = getcwd()
            chdir(repo_path)
            diff_file = self.fm().create_diff_file(id, diff)
            self.__git.apply(diff_file)
            self.utils().update_timestamp(id)
            chdir(old_dir)
        except SQLError:
            raise
        except git.GitCommandError:
            self.log().error('Unable to apply diff.')
            raise
        except OSError:
            raise
